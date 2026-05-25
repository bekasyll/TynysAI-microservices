import base64
import logging
import os
from pathlib import Path
from typing import List

import cv2
import matplotlib.cm as cm_mpl
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from tensorflow.keras.applications.densenet import preprocess_input as densenet_preprocess

# ──────────────────────────────────────────────────────────────
# Config
# ──────────────────────────────────────────────────────────────
DEFAULT_MODEL_PATH = Path(__file__).parent / "DenseNet121_best_model.keras"
MODEL_PATH = os.getenv("MODEL_PATH", str(DEFAULT_MODEL_PATH))
NORMAL_REF_PATH = Path(__file__).parent / "normal_reference.jpeg"
IMG_SIZE = 128
MODEL_VER = "2.0"
CLASS_LABELS = ["COVID19", "INVALID", "NORMAL", "PNEUMONIA", "TUBERCULOSIS"]

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("tynysai-ai")

# ──────────────────────────────────────────────────────────────
# Load model
# ──────────────────────────────────────────────────────────────
if not os.path.exists(MODEL_PATH):
    raise FileNotFoundError(
        f"Model file not found at {MODEL_PATH}. "
        f"Place DenseNet121_best_model.keras next to main.py or set MODEL_PATH env var."
    )


def load_model_safe(path: str) -> tf.keras.Model:
    """Rebuild model as Functional API and load weights from .keras archive.
    Works around Keras 3 bug with Sequential + nested Functional model."""
    import zipfile
    import tempfile

    inputs = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
    base = tf.keras.applications.DenseNet121(include_top=False, weights=None, input_shape=(IMG_SIZE, IMG_SIZE, 3))
    x = base(inputs)
    x = tf.keras.layers.Flatten()(x)
    x = tf.keras.layers.Dense(256, activation="relu")(x)
    x = tf.keras.layers.Dense(len(CLASS_LABELS), activation="softmax")(x)
    m = tf.keras.Model(inputs=inputs, outputs=x)

    with zipfile.ZipFile(path, "r") as z:
        with tempfile.TemporaryDirectory() as tmp:
            z.extractall(tmp)
            weights_path = os.path.join(tmp, "model.weights.h5")
            m.load_weights(weights_path)
    return m


log.info("Loading DenseNet121 from %s ...", MODEL_PATH)
model: tf.keras.Model = load_model_safe(MODEL_PATH)
log.info("DenseNet121 loaded successfully.")


# ──────────────────────────────────────────────────────────────
# Grad-CAM utilities
# ──────────────────────────────────────────────────────────────
def find_base_model(m: tf.keras.Model) -> tf.keras.Model:
    """Find the nested DenseNet121 functional model."""
    for layer in m.layers:
        if isinstance(layer, tf.keras.Model) and len(layer.layers) > 10:
            return layer
    return m


BASE_MODEL = find_base_model(model)
log.info("Grad-CAM: using base model '%s' output shape %s", BASE_MODEL.name, BASE_MODEL.output.shape)


def compute_gradcam(img_array: np.ndarray, target_class_idx: int) -> np.ndarray:
    """Grad-CAM using DenseNet base output as conv features."""
    feature_extractor = tf.keras.Model(inputs=BASE_MODEL.input, outputs=BASE_MODEL.output)

    dense_w, dense_b = model.get_layer("dense").get_weights()
    out_w, out_b = model.get_layer("dense_1").get_weights()

    img_tensor = tf.cast(img_array, tf.float32)

    with tf.GradientTape() as tape:
        conv_outputs = feature_extractor(img_tensor, training=False)
        tape.watch(conv_outputs)
        x = tf.reshape(conv_outputs, [tf.shape(conv_outputs)[0], -1])
        x = tf.nn.relu(x @ dense_w + dense_b)
        predictions = tf.nn.softmax(x @ out_w + out_b)
        class_channel = predictions[:, target_class_idx]

    grads = tape.gradient(class_channel, conv_outputs)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))
    heatmap = conv_outputs[0] @ pooled_grads[..., tf.newaxis]
    heatmap = tf.squeeze(heatmap)
    heatmap = tf.maximum(heatmap, 0) / (tf.math.reduce_max(heatmap) + 1e-8)
    return heatmap.numpy()


def apply_colormap(heatmap, img_resized, alpha, threshold, cmap_name):
    h = heatmap.copy()
    h[h < threshold] = 0
    h_resized = cv2.resize(h, (img_resized.shape[1], img_resized.shape[0]))
    h_uint8 = np.uint8(255 * h_resized)
    cmap = cm_mpl.get_cmap(cmap_name)
    colored = cmap(np.arange(256))[:, :3][h_uint8]
    overlay = colored * alpha + img_resized.astype(np.float32) / 255.0 * (1 - alpha)
    active_percent = float((h_resized > threshold).sum() / h_resized.size * 100)
    return np.clip(overlay, 0, 1), colored, active_percent


def ndarray_to_b64(arr: np.ndarray) -> str:
    if arr.dtype != np.uint8:
        arr = (arr * 255).astype(np.uint8)
    _, buf = cv2.imencode(".jpg", cv2.cvtColor(arr, cv2.COLOR_RGB2BGR))
    return base64.b64encode(buf).decode("utf-8")


def load_normal_reference():
    if NORMAL_REF_PATH.exists():
        img = cv2.imread(str(NORMAL_REF_PATH))
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img = cv2.resize(img, (IMG_SIZE, IMG_SIZE))
        return ndarray_to_b64(img)
    return None


NORMAL_REF_B64 = load_normal_reference()

# ──────────────────────────────────────────────────────────────
# Preprocessing
# ──────────────────────────────────────────────────────────────
ALLOWED_TYPES = {"image/jpeg", "image/jpg", "image/png"}


def preprocess_image_bytes(file_bytes: bytes) -> tuple[np.ndarray, np.ndarray]:
    """Returns (preprocessed tensor for model, resized RGB image for gradcam)."""
    arr = np.frombuffer(file_bytes, np.uint8)
    img_bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img_bgr is None:
        raise ValueError("Cannot decode image. Provide a valid JPG or PNG file.")
    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    img_resized = cv2.resize(img_rgb, (IMG_SIZE, IMG_SIZE))
    input_img = densenet_preprocess(img_resized.astype("float32"))
    input_img = np.expand_dims(input_img, axis=0)
    return input_img, img_resized


# ──────────────────────────────────────────────────────────────
# Severity / findings logic
# ──────────────────────────────────────────────────────────────
SEVERITY_MAP = {
    "NORMAL": "NONE",
    "INVALID": "NONE",
    "COVID19": "HIGH",
    "PNEUMONIA": "HIGH",
    "TUBERCULOSIS": "HIGH",
}

FINDINGS_TEMPLATES = {
    "COVID19": {
        "ru": "ЗАКЛЮЧЕНИЕ: COVID-19\nУВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЧЕСКОЕ ОПИСАНИЕ:\nНа рентгенограмме выявлены признаки, характерные для COVID-19: двусторонние периферические затемнения по типу «матового стекла», преимущественно в нижних и средних долях лёгких.\n\nРЕКОМЕНДАЦИИ:\n  1. ПЦР-тест на COVID-19\n  2. КТ грудной клетки\n  3. Консультация инфекциониста\n\nВАЖНО: Заключение требует верификации врачом-рентгенологом.",
        "en": "CONCLUSION: COVID-19\nMODEL CONFIDENCE: {confidence:.2%}\n\nRADIOLOGICAL DESCRIPTION:\nThe X-ray shows bilateral peripheral ground-glass opacities predominantly in the lower and middle lung lobes, characteristic of COVID-19.\n\nRECOMMENDATIONS:\n  1. PCR test for COVID-19\n  2. Chest CT scan\n  3. Infectious disease specialist consultation\n\nIMPORTANT: This conclusion requires verification by a radiologist.",
        "kk": "ҚОРЫТЫНДЫ: COVID-19\nМОДЕЛЬ СЕНІМДІЛІГІ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЯЛЫҚ СИПАТТАМА:\nРентгенограммада COVID-19-ға тән белгілер анықталды: екі жақты шеткі «матовое стекло» түріндегі қараюлар.\n\nҰСЫНЫСТАР:\n  1. COVID-19 ПЦР-тесті\n  2. Кеуде КТ\n  3. Инфекционист дәрігермен кеңесу\n\nМАҢЫЗДЫ: Қорытындыны рентгенолог дәрігер тексеруі қажет.",
    },
    "NORMAL": {
        "ru": "ЗАКЛЮЧЕНИЕ: НОРМА\nУВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЧЕСКОЕ ОПИСАНИЕ:\nЛёгочные поля прозрачны, без очаговых и инфильтративных изменений. Сосудистый рисунок не усилен. Синусы свободны. Патологических изменений не выявлено.\n\nПрофилактический осмотр рекомендуется раз в год.\n\nВАЖНО: Заключение требует верификации врачом-рентгенологом.",
        "en": "CONCLUSION: NORMAL\nMODEL CONFIDENCE: {confidence:.2%}\n\nRADIOLOGICAL DESCRIPTION:\nLung fields are clear, without focal or infiltrative changes. Vascular pattern is not enhanced. Sinuses are free. No pathological changes detected.\n\nPreventive examination is recommended once a year.\n\nIMPORTANT: This conclusion requires verification by a radiologist.",
        "kk": "ҚОРЫТЫНДЫ: ҚАЛЫПТЫ\nМОДЕЛЬ СЕНІМДІЛІГІ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЯЛЫҚ СИПАТТАМА:\nӨкпе өрістері мөлдір, ошақты және инфильтративті өзгерістерсіз. Тамырлық сурет күшейтілмеген. Синустар бос. Патологиялық өзгерістер анықталмады.\n\nПрофилактикалық тексеру жылына бір рет ұсынылады.\n\nМАҢЫЗДЫ: Қорытындыны рентгенолог дәрігер тексеруі қажет.",
    },
    "PNEUMONIA": {
        "ru": "ЗАКЛЮЧЕНИЕ: ПНЕВМОНИЯ\nУВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЧЕСКОЕ ОПИСАНИЕ:\nНа снимке определяются очаговые затемнения с нечёткими контурами, преимущественно в нижних долях лёгких. Признаки воспалительной инфильтрации лёгочной ткани.\n\nРЕКОМЕНДАЦИИ:\n  1. Срочная консультация пульмонолога\n  2. Антибактериальная терапия (после подтверждения)\n  3. Общий анализ крови + С-реактивный белок\n  4. Контрольная рентгенография через 10-14 дней\n\nВАЖНО: Заключение требует верификации врачом-рентгенологом.",
        "en": "CONCLUSION: PNEUMONIA\nMODEL CONFIDENCE: {confidence:.2%}\n\nRADIOLOGICAL DESCRIPTION:\nFocal opacities with ill-defined margins are visible, predominantly in the lower lung lobes. Signs of inflammatory infiltration of lung tissue.\n\nRECOMMENDATIONS:\n  1. Urgent pulmonologist consultation\n  2. Antibiotic therapy (after confirmation)\n  3. Complete blood count + C-reactive protein\n  4. Follow-up X-ray in 10-14 days\n\nIMPORTANT: This conclusion requires verification by a radiologist.",
        "kk": "ҚОРЫТЫНДЫ: ПНЕВМОНИЯ\nМОДЕЛЬ СЕНІМДІЛІГІ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЯЛЫҚ СИПАТТАМА:\nСуретте негізінен өкпенің төменгі бөліктерінде бұлыңғыр шектері бар ошақты қараюлар анықталады. Өкпе тінінің қабыну инфильтрациясының белгілері.\n\nҰСЫНЫСТАР:\n  1. Пульмонологпен шұғыл кеңесу\n  2. Антибактериялық терапия\n  3. Жалпы қан анализі + С-реактивті белок\n  4. 10-14 күнде бақылау рентгенографиясы\n\nМАҢЫЗДЫ: Қорытындыны рентгенолог дәрігер тексеруі қажет.",
    },
    "TUBERCULOSIS": {
        "ru": "ЗАКЛЮЧЕНИЕ: ТУБЕРКУЛЁЗ\nУВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЧЕСКОЕ ОПИСАНИЕ:\nВыявлены специфические изменения: очаговые тени в верхних долях лёгких, возможные каверны и фиброзные изменения. Картина подозрительна на туберкулёз лёгких.\n\nРЕКОМЕНДАЦИИ:\n  1. Срочная консультация фтизиатра\n  2. Микробиологическое исследование мокроты\n  3. Проба Манту/диаскинтест\n\nВАЖНО: Заключение требует верификации врачом-рентгенологом.",
        "en": "CONCLUSION: TUBERCULOSIS\nMODEL CONFIDENCE: {confidence:.2%}\n\nRADIOLOGICAL DESCRIPTION:\nSpecific changes detected: focal shadows in the upper lung lobes, possible cavities and fibrotic changes. Suspicious for pulmonary tuberculosis.\n\nRECOMMENDATIONS:\n  1. Urgent phthisiologist consultation\n  2. Sputum microbiological examination\n  3. Mantoux/diaskintest\n\nIMPORTANT: This conclusion requires verification by a radiologist.",
        "kk": "ҚОРЫТЫНДЫ: ТУБЕРКУЛЕЗ\nМОДЕЛЬ СЕНІМДІЛІГІ: {confidence:.2%}\n\nРЕНТГЕНОЛОГИЯЛЫҚ СИПАТТАМА:\nАрнайы өзгерістер анықталды: өкпенің жоғарғы бөліктеріндегі ошақты көлеңкелер, мүмкін каверналар және фиброздық өзгерістер.\n\nҰСЫНЫСТАР:\n  1. Фтизиатрмен шұғыл кеңесу\n  2. Қақырықты микробиологиялық зерттеу\n  3. Манту/диаскинтест\n\nМАҢЫЗДЫ: Қорытындыны рентгенолог дәрігер тексеруі қажет.",
    },
    "INVALID": {
        "ru": "ЗАКЛЮЧЕНИЕ: НЕ ЯВЛЯЕТСЯ РЕНТГЕНОМ\nУВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n\nЗагруженное изображение не распознано как рентгенограмма грудной клетки. Пожалуйста, загрузите корректный рентгеновский снимок лёгких в формате JPG или PNG.",
        "en": "CONCLUSION: NOT AN X-RAY\nMODEL CONFIDENCE: {confidence:.2%}\n\nThe uploaded image was not recognized as a chest X-ray. Please upload a valid lung X-ray image in JPG or PNG format.",
        "kk": "ҚОРЫТЫНДЫ: РЕНТГЕН ЕМЕС\nМОДЕЛЬ СЕНІМДІЛІГІ: {confidence:.2%}\n\nЖүктелген сурет кеуде рентгенограммасы ретінде танылмады. Өкпенің дұрыс рентген суретін JPG немесе PNG форматында жүктеңіз.",
    },
}

ABNORMALITIES_MAP = {
    "COVID19": {
        "ru": ["Затемнения по типу «матового стекла»", "Двусторонняя периферическая консолидация"],
        "en": ["Ground-glass opacities", "Bilateral peripheral consolidation"],
        "kk": ["«Матовое стекло» түріндегі қараюлар", "Екі жақты шеткі консолидация"],
    },
    "PNEUMONIA": {
        "ru": ["Лёгочная консолидация", "Повышенная плотность", "Воздушная бронхограмма"],
        "en": ["Pulmonary consolidation", "Increased opacity", "Air bronchogram"],
        "kk": ["Өкпе консолидациясы", "Тығыздық артуы", "Ауа бронхограммасы"],
    },
    "TUBERCULOSIS": {
        "ru": ["Каверны в верхних долях", "Фиброзные изменения", "Увеличение лимфоузлов корней лёгких"],
        "en": ["Apical cavitary lesions", "Fibrotic changes", "Hilar lymphadenopathy"],
        "kk": ["Жоғарғы бөліктердегі каверналар", "Фиброздық өзгерістер", "Өкпе түбірі лимфа түйіндерінің ұлғаюы"],
    },
    "NORMAL": {"ru": [], "en": [], "kk": []},
    "INVALID": {"ru": [], "en": [], "kk": []},
}


def interpret(preds: np.ndarray, lang: str = "ru") -> dict:
    pred_idx = int(np.argmax(preds))
    result_label = CLASS_LABELS[pred_idx]
    confidence = float(preds[pred_idx])

    severity = SEVERITY_MAP.get(result_label, "MODERATE")
    if result_label not in ("NORMAL", "INVALID"):
        if confidence >= 0.90:
            severity = "HIGH"
        elif confidence >= 0.70:
            severity = "MODERATE"
        else:
            severity = "LOW"

    template = FINDINGS_TEMPLATES.get(result_label, {})
    findings_text = template.get(lang, template.get("ru", ""))
    findings = findings_text.format(confidence=confidence) if findings_text else ""
    requires_review = confidence < 0.80 or result_label not in ("NORMAL", "INVALID")

    probabilities = {CLASS_LABELS[i]: float(preds[i]) for i in range(len(CLASS_LABELS))}

    return {
        "diagnosis": result_label,
        "confidence": confidence,
        "raw_score": confidence,
        "severity": severity,
        "findings": findings,
        "requires_doctor_review": requires_review,
        "probabilities": probabilities,
        "abnormalities": ABNORMALITIES_MAP.get(result_label, {}).get(lang, []),
        "pred_idx": pred_idx,
    }


# ──────────────────────────────────────────────────────────────
# FastAPI app
# ──────────────────────────────────────────────────────────────
app = FastAPI(
    title="TynysAI - PulmoScan AI Service",
    description="DenseNet121 chest X-ray classifier (COVID19 / NORMAL / PNEUMONIA / TUBERCULOSIS / INVALID) + Grad-CAM",
    version=MODEL_VER,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ──────────────────────────────────────────────────────────────
# Response schemas
# ──────────────────────────────────────────────────────────────
class AnalysisResponse(BaseModel):
    diagnosis: str
    confidence: float
    raw_score: float
    severity: str
    findings: str
    requires_doctor_review: bool
    model_version: str
    probabilities: dict[str, float]
    abnormalities: List[str]
    original_b64: str | None = None
    normal_ref_b64: str | None = None


class GradcamRequest(BaseModel):
    image_b64: str
    target_class: str
    alpha: float = 0.5
    threshold: float = 0.3
    colormap: str = "jet"


class GradcamResponse(BaseModel):
    heatmap_b64: str
    overlay_b64: str
    active_percent: float
    target_class: str
    model_used: str


# ──────────────────────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}


@app.get("/info")
def info():
    return {
        "model_version": MODEL_VER,
        "model_name": "DenseNet121",
        "input_size": [IMG_SIZE, IMG_SIZE],
        "classes": CLASS_LABELS,
    }


@app.post("/analyze", response_model=AnalysisResponse)
async def analyze(file: UploadFile = File(...), lang: str = Form("ru")):
    """Upload chest X-ray (JPG/PNG) -> AI diagnosis with DenseNet121."""
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(400, f"Unsupported type: {file.content_type}. Use JPG or PNG.")
    try:
        data = await file.read()
        input_img, img_resized = preprocess_image_bytes(data)
        preds = model.predict(input_img, verbose=0)[0]
        result = interpret(preds, lang)

        log.info("Analyzed %s -> %s (%.2f%%)",
                 file.filename, result["diagnosis"], result["confidence"] * 100)

        return AnalysisResponse(
            diagnosis=result["diagnosis"],
            confidence=result["confidence"],
            raw_score=result["raw_score"],
            severity=result["severity"],
            findings=result["findings"],
            requires_doctor_review=result["requires_doctor_review"],
            model_version=MODEL_VER,
            probabilities=result["probabilities"],
            abnormalities=result["abnormalities"],
            original_b64=ndarray_to_b64(img_resized),
            normal_ref_b64=NORMAL_REF_B64,
        )
    except ValueError as e:
        raise HTTPException(400, str(e))
    except Exception as e:
        log.exception("Prediction failed")
        raise HTTPException(500, f"Prediction failed: {e}")


@app.post("/gradcam", response_model=GradcamResponse)
async def gradcam(req: GradcamRequest):
    """Generate Grad-CAM heatmap for a given class. Call after /analyze."""
    try:
        img_bytes = base64.b64decode(req.image_b64)
        nparr = np.frombuffer(img_bytes, np.uint8)
        img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img_bgr is None:
            raise HTTPException(400, "Cannot decode image from base64")

        img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
        img_resized = cv2.resize(img_rgb, (IMG_SIZE, IMG_SIZE))
        input_img = densenet_preprocess(img_resized.astype("float32"))
        input_img = np.expand_dims(input_img, axis=0)

        if req.target_class not in CLASS_LABELS:
            raise HTTPException(400, f"Unknown class: {req.target_class}")

        target_idx = CLASS_LABELS.index(req.target_class)
        heatmap = compute_gradcam(input_img, target_idx)
        overlay, colored, active_percent = apply_colormap(
            heatmap, img_resized, req.alpha, req.threshold, req.colormap
        )

        return GradcamResponse(
            heatmap_b64=ndarray_to_b64(colored),
            overlay_b64=ndarray_to_b64(overlay),
            active_percent=round(active_percent, 1),
            target_class=req.target_class,
            model_used="DenseNet121",
        )
    except HTTPException:
        raise
    except Exception as e:
        log.exception("Grad-CAM failed")
        raise HTTPException(500, f"Grad-CAM failed: {e}")
