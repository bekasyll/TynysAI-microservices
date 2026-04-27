# TynysAI - Pneumonia Detection Service

Python FastAPI микросервис с CNN-моделью (Keras/TensorFlow) для классификации
рентгеновских снимков грудной клетки: **NORMAL** vs **PNEUMONIA**.

Вызывается из `xray-service` (Java) по HTTP через `POST /analyze`.

## Требования

- Python 3.11
- `pneumonia_model.h5` - файл весов модели (бинарная классификация, вход 150×150 grayscale)

## Установка

```bash
cd xray-service/ai_service

python3.11 -m venv venv
source venv/bin/activate

pip install -r requirements.txt
```

## Размещение модели

Положи файл `pneumonia_model.h5` рядом с `main.py`:

```
xray-service/ai_service/
├── main.py
├── pneumonia_model.h5   ← сюда
├── requirements.txt
└── README.md
```

Либо укажи путь через переменную окружения:

```bash
export MODEL_PATH=/absolute/path/to/pneumonia_model.h5
```

## Запуск

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

Сервис поднимется на `http://localhost:8000` - этот URL настроен в
`xray-service/src/main/resources/application.yaml`:

```yaml
app:
  ai:
    service-url: http://localhost:8000
    enabled: true
    confidence-threshold: 0.70
```

## Эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/health` | Health check |
| `GET` | `/info` | Информация о модели (версия, классы, порог) |
| `POST` | `/analyze` | Анализ снимка (multipart `file=@image.jpg`) |
| `POST` | `/analyze/gradcam` | Анализ + Grad-CAM heatmap (base64 PNG) |

### Пример

```bash
curl -X POST http://localhost:8000/analyze \
  -F "file=@chest.jpg"
```

Ответ:

```json
{
  "diagnosis": "PNEUMONIA",
  "confidence": 0.92,
  "raw_score": 0.08,
  "severity": "HIGH",
  "findings": "ЗАКЛЮЧЕНИЕ: ВНЕБОЛЬНИЧНАЯ ПНЕВМОНИЯ\n...",
  "requires_doctor_review": false,
  "model_version": "1.0"
}
```

## Связь с xray-service

Java-клиент `AiAnalysisService` (`xray-service/.../service/AiAnalysisService.java`)
отправляет файл на `POST /analyze` и маппит ответ в `DiseaseType` enum:

- `PNEUMONIA` → `BACTERIAL_PNEUMONIA`
- `NORMAL` → `NORMAL`

Если этот Python-сервис не запущен или упал, Java-клиент откатывается на
stub-результат (`NORMAL, 0.87, requiresReview=true`) и пишет `WARN` в лог -
`xray-service` продолжит работать.

## Как модель получается

Модель - бинарный CNN-классификатор. Её нет в репозитории (git-ignore).
Получить можно одним из способов:

1. **Обучить самостоятельно** на датасете
   [Kaggle chest-xray-pneumonia](https://www.kaggle.com/datasets/paultimothymooney/chest-xray-pneumonia)
   - архитектура ожидает последний conv-слой с именем `conv2d_5`
2. Скопировать готовые веса из монолита, если они у тебя уже есть локально
3. Запросить у владельца проекта

Параметры модели (из `main.py`):
- Вход: 150×150 grayscale, нормализация `/255.0`
- Выход: sigmoid
- Порог: 0.5 (`>= 0.5` → NORMAL, `< 0.5` → PNEUMONIA)
- Последний conv-слой: `conv2d_5` (для Grad-CAM)
