from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.core.config import settings
from app.ocr.card_parser import parse_cards_from_text, parse_llm_json_payload
from app.ocr.schemas import ExtractedCardItem, OcrTextBox
from main import app

client = TestClient(app)

# Minimal valid-looking PNG header bytes for upload tests
FAKE_PNG_BYTES = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde"


def test_parse_cards_from_text_supports_multiple_separators():
    raw_text = (
        "1. algorithm - a step-by-step procedure for solving a problem\n"
        "2) recursion: a function calling itself\n"
        "- polymorphism\tability of objects to take many forms\n"
        "• encapsulation = bundling data and methods together\n"
        "heuristic -> practical problem-solving shortcut\n"
        "Noise line without separator\n"
    )

    cards, warnings = parse_cards_from_text(raw_text)

    assert len(cards) == 5
    assert cards[0] == ExtractedCardItem(
        term="algorithm",
        definition="a step-by-step procedure for solving a problem",
    )
    assert cards[1] == ExtractedCardItem(
        term="recursion",
        definition="a function calling itself",
    )
    assert cards[2].term == "polymorphism"
    assert cards[3].term == "encapsulation"
    assert cards[4].term == "heuristic"
    assert any("1 dòng không khớp" in w for w in warnings)


def test_parse_llm_json_payload_handles_markdown_fences():
    fenced = '```json\n{"raw_text": "hello - xin chao", "cards": [{"term": "hello", "definition": "xin chao"}]}\n```'
    parsed = parse_llm_json_payload(fenced)
    assert parsed is not None
    raw_text, cards = parsed
    assert raw_text == "hello - xin chao"
    assert len(cards) == 1
    assert cards[0].term == "hello"


def test_ocr_extract_uses_tesseract_fallback_and_parses_cards():
    sample_text = "photosynthesis - quá trình quang hợp\nmitochondria: nhà máy năng lượng của tế bào"

    with (
        patch("app.ocr.service._extract_with_gemini_vision", new=AsyncMock(return_value=None)),
        patch("app.ocr.service._extract_with_openai_vision", new=AsyncMock(return_value=None)),
        patch("app.ocr.service._extract_with_paddleocr", return_value=None),
        patch("app.ocr.service._extract_with_rapidocr", return_value=None),
        patch("app.ocr.service._extract_with_tesseract", return_value=sample_text),
    ):
        response = client.post(
            "/ocr/extract",
            files={"file": ("vocab.png", FAKE_PNG_BYTES, "image/png")},
            data={"source_lang": "en", "target_lang": "vi"},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["engine"] == "tesseract"
    assert body["confidence"] > 0.5
    assert len(body["cards"]) == 2
    assert body["cards"][0]["term"] == "photosynthesis"
    assert body["cards"][1]["term"] == "mitochondria"


def test_ocr_extract_uses_gemini_vision_when_available():
    mocked_cards = [
        ExtractedCardItem(term="ubiquitous", definition="có mặt ở khắp mọi nơi"),
        ExtractedCardItem(term="ephemeral", definition="ngắn ngủi, phù du"),
    ]

    with (
        patch(
            "app.ocr.service._extract_with_gemini_vision",
            new=AsyncMock(return_value=("ubiquitous - có mặt ở khắp mọi nơi\nephemeral - ngắn ngủi, phù du", mocked_cards)),
        ),
        patch("app.ocr.service._extract_with_rapidocr", return_value=None),
    ):
        response = client.post(
            "/ocr/extract",
            files={"file": ("book.jpg", FAKE_PNG_BYTES, "image/jpeg")},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["engine"] == "gemini_vision"
    assert body["confidence"] >= 0.9
    assert len(body["cards"]) == 2
    assert body["cards"][0]["term"] == "ubiquitous"


def test_ocr_extract_uses_rapidocr_and_returns_word_boxes():
    mock_boxes = [
        OcrTextBox(id="box_0_0", text="routine", x=0.4, y=0.3, width=0.15, height=0.05, confidence=0.98),
        OcrTextBox(id="box_1_0", text="thói", x=0.5, y=0.4, width=0.08, height=0.04, confidence=0.95),
        OcrTextBox(id="box_1_1", text="quen", x=0.59, y=0.4, width=0.09, height=0.04, confidence=0.96),
    ]
    with (
        patch("app.ocr.service._extract_with_gemini_vision", new=AsyncMock(return_value=None)),
        patch("app.ocr.service._extract_with_openai_vision", new=AsyncMock(return_value=None)),
        patch("app.ocr.service._extract_with_paddleocr", return_value=None),
        patch("app.ocr.service._extract_with_rapidocr", return_value=("routine - thói quen", mock_boxes)),
    ):
        response = client.post(
            "/ocr/extract",
            files={"file": ("card.png", FAKE_PNG_BYTES, "image/png")},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["engine"] == "rapidocr"
    assert len(body["boxes"]) == 3
    assert body["boxes"][0]["text"] == "routine"
    assert len(body["cards"]) == 1


def test_ocr_extract_graceful_fallback_when_no_engine_or_key():
    with (
        patch("app.ocr.service._extract_with_gemini_vision", new=AsyncMock(return_value=None)),
        patch("app.ocr.service._extract_with_openai_vision", new=AsyncMock(return_value=None)),
        patch("app.ocr.service._extract_with_paddleocr", return_value=None),
        patch("app.ocr.service._extract_with_rapidocr", return_value=None),
        patch("app.ocr.service._extract_with_tesseract", return_value=None),
    ):
        response = client.post(
            "/ocr/extract",
            files={"file": ("notes.png", FAKE_PNG_BYTES, "image/png")},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["engine"] == "fallback"
    assert body["cards"] == []
    assert body["confidence"] == 0.0
    assert len(body["warnings"]) >= 1


def test_ocr_extract_rejects_invalid_file_type():
    response = client.post(
        "/ocr/extract",
        files={"file": ("notes.txt", b"plain text", "text/plain")},
    )
    assert response.status_code == 400


def test_ocr_extract_rejects_empty_file():
    response = client.post(
        "/ocr/extract",
        files={"file": ("empty.png", b"", "image/png")},
    )
    assert response.status_code == 400


def test_ocr_extract_rejects_oversized_file(monkeypatch):
    monkeypatch.setattr(settings, "OCR_MAX_IMAGE_BYTES", 10)
    response = client.post(
        "/ocr/extract",
        files={"file": ("big.png", FAKE_PNG_BYTES, "image/png")},
    )
    assert response.status_code == 413
