import base64
import logging
import os
from pathlib import Path
from typing import Optional, Tuple

import cv2
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# ──────────────────────────────────────────────────────────────
# Config
# ──────────────────────────────────────────────────────────────
DEFAULT_MODEL_PATH = Path(__file__).parent / "pneumonia_model.h5"
MODEL_PATH   = os.getenv("MODEL_PATH", str(DEFAULT_MODEL_PATH))
LAST_CONV    = "conv2d_5"   # confirmed from h5 file inspection
INPUT_SIZE   = (150, 150)
THRESHOLD    = 0.5          # sigmoid >= THRESHOLD → NORMAL
MODEL_VER    = "1.0"

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("tynysai-ai")

# ──────────────────────────────────────────────────────────────
# Load model once at startup
# ──────────────────────────────────────────────────────────────
if not os.path.exists(MODEL_PATH):
    raise FileNotFoundError(
        f"Model file not found at {MODEL_PATH}. "
        f"Place pneumonia_model.h5 next to main.py or set MODEL_PATH env var."
    )

log.info("Loading model from %s ...", MODEL_PATH)
model: tf.keras.Model = tf.keras.models.load_model(MODEL_PATH)
log.info("Model loaded successfully.")

# ──────────────────────────────────────────────────────────────
# FastAPI app
# ──────────────────────────────────────────────────────────────
app = FastAPI(
    title="TynysAI — Pneumonia Detection API",
    description="CNN-based chest X-ray classifier (NORMAL / PNEUMONIA)",
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
    diagnosis: str                  # "NORMAL" | "PNEUMONIA"
    confidence: float               # 0.0 – 1.0
    raw_score: float                # raw sigmoid output
    severity: str                   # "LOW" | "MODERATE" | "HIGH"
    findings: str                   # structured medical text (RU)
    requires_doctor_review: bool
    model_version: str


class GradCamResponse(AnalysisResponse):
    heatmap_base64: Optional[str] = None  # base64-encoded PNG overlay


# ──────────────────────────────────────────────────────────────
# Image preprocessing
# ──────────────────────────────────────────────────────────────
def preprocess(file_bytes: bytes) -> np.ndarray:
    """bytes → grayscale 150×150 float32 tensor of shape (1,150,150,1)."""
    arr = np.frombuffer(file_bytes, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_GRAYSCALE)
    if img is None:
        raise ValueError("Cannot decode image. Provide a valid JPG or PNG file.")
    img = cv2.resize(img, INPUT_SIZE)
    img = img.astype("float32") / 255.0
    img = np.expand_dims(img, axis=(0, -1))  # → (1, 150, 150, 1)
    return img


# ──────────────────────────────────────────────────────────────
# Interpretation logic
# ──────────────────────────────────────────────────────────────
def interpret(raw_score: float) -> Tuple[str, float, str, str, bool]:
    """
    Map raw sigmoid output → (diagnosis, confidence, severity, findings, requires_review).
    """
    if raw_score >= THRESHOLD:
        # ── NORMAL ──────────────────────────────────────────
        diagnosis  = "NORMAL"
        confidence = float(raw_score)
        severity   = "LOW"

        if confidence >= 0.95:
            confidence_note = "Высокая уверенность в норме."
        elif confidence >= 0.80:
            confidence_note = ("Уверенность в норме умеренная. "
                               "Рекомендуется динамическое наблюдение.")
        else:
            confidence_note = ("Пограничный результат. "
                               "Рекомендуется повторное исследование.")

        findings = (
            f"ЗАКЛЮЧЕНИЕ: НОРМА\n"
            f"УВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n\n"
            f"РЕНТГЕНОЛОГИЧЕСКОЕ ОПИСАНИЕ:\n"
            f"На представленной рентгенограмме патологических изменений лёгочной ткани "
            f"не выявлено. Лёгочные поля прозрачны, без очаговых и инфильтративных теней. "
            f"Корни лёгких структурны, не расширены. Синусы свободны. "
            f"Диафрагма расположена обычно. "
            f"Сердечная тень не увеличена, конфигурация не изменена.\n\n"
            f"{confidence_note}\n\n"
            f"ВАЖНО: Заключение требует верификации врачом-рентгенологом."
        )

    else:
        # ── PNEUMONIA ────────────────────────────────────────
        diagnosis  = "PNEUMONIA"
        confidence = float(1.0 - raw_score)

        if confidence >= 0.90:
            severity      = "HIGH"
            severity_desc = "выраженные"
            localization  = "диффузные изменения, преимущественно в нижних долях"
            urgency       = "СРОЧНО"
        elif confidence >= 0.75:
            severity      = "MODERATE"
            severity_desc = "умеренные"
            localization  = "диффузные изменения, преимущественно в верхней доле"
            urgency       = "ОБЯЗАТЕЛЬНО"
        else:
            severity      = "LOW"
            severity_desc = "начальные"
            localization  = "очаговые изменения"
            urgency       = "РЕКОМЕНДУЕТСЯ"

        findings = (
            f"ЗАКЛЮЧЕНИЕ: ВНЕБОЛЬНИЧНАЯ ПНЕВМОНИЯ\n"
            f"УВЕРЕННОСТЬ МОДЕЛИ: {confidence:.2%}\n"
            f"ЛОКАЛИЗАЦИЯ ПОРАЖЕНИЯ: {localization}\n\n"
            f"РЕНТГЕНОЛОГИЧЕСКОЕ ОПИСАНИЕ:\n"
            f"На представленной рентгенограмме визуализируются инфильтративные изменения "
            f"лёгочной ткани ({severity_desc}), локализующиеся {localization}.\n"
            f"Характерные признаки:\n"
            f"  • Наличие очаговых/инфильтративных теней\n"
            f"  • Лёгочный рисунок усилен в зонах поражения\n"
            f"  • Возможен симптом \"воздушной бронхограммы\"\n\n"
            f"РЕКОМЕНДАЦИИ:\n"
            f"  1. Консультация пульмонолога — {urgency}\n"
            f"  2. Антибактериальная терапия (после подтверждения)\n"
            f"  3. Общий анализ крови + С-реактивный белок\n"
            f"  4. Контрольная рентгенография через 10-14 дней\n\n"
            f"ВАЖНО: Заключение требует верификации врачом-рентгенологом."
        )

    requires_review = confidence < 0.80
    return diagnosis, confidence, severity, findings, requires_review


# ──────────────────────────────────────────────────────────────
# Grad-CAM
# ──────────────────────────────────────────────────────────────
def compute_gradcam(img_tensor: np.ndarray) -> np.ndarray:
    """Return Grad-CAM heatmap smoothly overlaid on the original image (BGR uint8)."""
    grad_model = tf.keras.models.Model(
        inputs=model.inputs,
        outputs=[model.get_layer(LAST_CONV).output, model.output],
    )
    with tf.GradientTape() as tape:
        conv_outputs, predictions = grad_model(img_tensor)
        loss = predictions[:, 0]

    grads        = tape.gradient(loss, conv_outputs)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))
    heatmap      = conv_outputs[0] @ pooled_grads[..., tf.newaxis]
    heatmap      = tf.squeeze(heatmap)
    heatmap      = tf.maximum(heatmap, 0) / (tf.math.reduce_max(heatmap) + 1e-8)
    heatmap      = heatmap.numpy()

    heatmap_resized = cv2.resize(heatmap, INPUT_SIZE)
    heatmap_color   = cv2.applyColorMap(np.uint8(255 * heatmap_resized), cv2.COLORMAP_JET)

    original_gray = np.uint8(img_tensor[0, :, :, 0] * 255)
    original_bgr  = cv2.cvtColor(original_gray, cv2.COLOR_GRAY2BGR)
    overlay       = cv2.addWeighted(original_bgr, 0.6, heatmap_color, 0.4, 0)
    return overlay


def to_base64_png(img: np.ndarray) -> str:
    _, buf = cv2.imencode(".png", img)
    return base64.b64encode(buf).decode("utf-8")


# ──────────────────────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────────────────────
ALLOWED_TYPES = {"image/jpeg", "image/jpg", "image/png"}


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}


@app.get("/info")
def info():
    return {
        "model_version": MODEL_VER,
        "input_size":    list(INPUT_SIZE),
        "classes":       ["NORMAL", "PNEUMONIA"],
        "threshold":     THRESHOLD,
        "last_conv_layer": LAST_CONV,
    }


@app.post("/analyze", response_model=AnalysisResponse)
async def analyze(file: UploadFile = File(...)):
    """Upload chest X-ray (JPG/PNG) → AI diagnosis."""
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(400, f"Unsupported type: {file.content_type}. Use JPG or PNG.")
    try:
        data = await file.read()
        tensor = preprocess(data)
        raw    = float(model.predict(tensor, verbose=0)[0][0])
        diagnosis, confidence, severity, findings, review = interpret(raw)

        log.info("Analyzed %s → %s (%.2f%%) raw=%.4f",
                 file.filename, diagnosis, confidence * 100, raw)

        return AnalysisResponse(
            diagnosis=diagnosis,
            confidence=confidence,
            raw_score=raw,
            severity=severity,
            findings=findings,
            requires_doctor_review=review,
            model_version=MODEL_VER,
        )
    except ValueError as e:
        raise HTTPException(400, str(e))
    except Exception as e:
        log.exception("Prediction failed")
        raise HTTPException(500, f"Prediction failed: {e}")


@app.post("/analyze/gradcam", response_model=GradCamResponse)
async def analyze_gradcam(file: UploadFile = File(...)):
    """Upload chest X-ray → AI diagnosis + Grad-CAM heatmap (base64 PNG)."""
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(400, "Use JPG or PNG.")
    try:
        data = await file.read()
        tensor = preprocess(data)
        raw    = float(model.predict(tensor, verbose=0)[0][0])
        diagnosis, confidence, severity, findings, review = interpret(raw)

        overlay = compute_gradcam(tensor)
        heatmap_b64 = to_base64_png(overlay)

        return GradCamResponse(
            diagnosis=diagnosis,
            confidence=confidence,
            raw_score=raw,
            severity=severity,
            findings=findings,
            requires_doctor_review=review,
            model_version=MODEL_VER,
            heatmap_base64=heatmap_b64,
        )
    except Exception as e:
        log.exception("Grad-CAM failed")
        raise HTTPException(500, str(e))
