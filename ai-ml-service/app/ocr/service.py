import base64
import io
import logging
import os
import shutil
from typing import List, Optional, Tuple

import httpx
from fastapi import HTTPException, UploadFile, status

from app.core.config import settings
from app.ocr.card_parser import parse_cards_from_text, parse_llm_json_payload
from app.ocr.schemas import ExtractedCardItem, OcrExtractResponse

logger = logging.getLogger(__name__)

ALLOWED_MIME_TYPES = {
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp",
    "image/bmp",
    "image/tiff",
}

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"}

_PLACEHOLDER_KEYS = {"", "your_gemini_api_key_here", "your_openai_api_key_here", "changeme"}


def _is_configured_key(api_key: Optional[str]) -> bool:
    return bool(api_key and api_key.strip() and api_key.strip().lower() not in _PLACEHOLDER_KEYS)


def _infer_mime_type(filename: str, content_type: Optional[str]) -> str:
    if content_type and content_type.strip().lower() in ALLOWED_MIME_TYPES:
        normalized = content_type.strip().lower()
        return "image/jpeg" if normalized == "image/jpg" else normalized

    ext = os.path.splitext(filename.lower())[1]
    mapping = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".webp": "image/webp",
        ".bmp": "image/bmp",
        ".tiff": "image/tiff",
    }
    return mapping.get(ext, "image/jpeg")


async def validate_image_upload(file: UploadFile) -> Tuple[bytes, str, str]:
    """Validate uploaded image file type and size; return (bytes, filename, mime_type)."""
    if file is None or not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Vui lòng chọn tệp ảnh hợp lệ để quét OCR.",
        )

    filename = file.filename
    ext = os.path.splitext(filename.lower())[1]
    content_type = (file.content_type or "").strip().lower()

    is_valid_mime = content_type in ALLOWED_MIME_TYPES
    is_valid_ext = ext in ALLOWED_EXTENSIONS
    if not (is_valid_mime or (content_type in ("", "application/octet-stream") and is_valid_ext)):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Định dạng tệp không hợp lệ. Chỉ hỗ trợ ảnh JPG, PNG, WEBP, BMP hoặc TIFF.",
        )

    image_bytes = await file.read()
    if not image_bytes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Tệp ảnh tải lên bị rỗng.",
        )

    max_bytes = settings.OCR_MAX_IMAGE_BYTES
    if len(image_bytes) > max_bytes:
        max_mb = max_bytes / (1024 * 1024)
        raise HTTPException(
            status_code=getattr(status, "HTTP_413_CONTENT_TOO_LARGE", 413),
            detail=f"Dung lượng ảnh vượt quá giới hạn tối đa {max_mb:.0f}MB.",
        )

    return image_bytes, filename, _infer_mime_type(filename, content_type)


def _build_vision_prompt(source_lang: Optional[str], target_lang: Optional[str]) -> str:
    lang_hint = ""
    if source_lang or target_lang:
        lang_hint = f" Ngôn ngữ thuật ngữ ưu tiên: {source_lang or 'auto'}, định nghĩa: {target_lang or 'auto'}."
    return (
        "Hãy đọc toàn bộ văn bản trong ảnh tài liệu học tập này và tách thành danh sách cặp flashcard "
        "(term - definition)." + lang_hint + " "
        "Trả về DUY NHẤT JSON hợp lệ theo đúng schema sau (không thêm markdown hay giải thích): "
        '{"raw_text": "...", "cards": [{"term": "...", "definition": "..."}]}'
    )


async def _extract_with_gemini_vision(
    image_bytes: bytes,
    mime_type: str,
    source_lang: Optional[str],
    target_lang: Optional[str],
) -> Optional[Tuple[str, List[ExtractedCardItem]]]:
    if not _is_configured_key(settings.GEMINI_API_KEY):
        return None

    url = (
        f"https://generativelanguage.googleapis.com/v1beta/models/"
        f"{settings.GEMINI_MODEL}:generateContent?key={settings.GEMINI_API_KEY}"
    )
    payload = {
        "contents": [
            {
                "parts": [
                    {"text": _build_vision_prompt(source_lang, target_lang)},
                    {
                        "inline_data": {
                            "mime_type": mime_type,
                            "data": base64.b64encode(image_bytes).decode("ascii"),
                        }
                    },
                ]
            }
        ],
        "generationConfig": {"temperature": 0.1},
    }

    try:
        async with httpx.AsyncClient(timeout=12.0) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            body = resp.json()
        text = (
            body.get("candidates", [{}])[0]
            .get("content", {})
            .get("parts", [{}])[0]
            .get("text", "")
        )
        return parse_llm_json_payload(text)
    except Exception as exc:
        logger.warning("Gemini Vision OCR failed, falling back: %s", exc)
        return None


async def _extract_with_openai_vision(
    image_bytes: bytes,
    mime_type: str,
    source_lang: Optional[str],
    target_lang: Optional[str],
) -> Optional[Tuple[str, List[ExtractedCardItem]]]:
    if not _is_configured_key(settings.OPENAI_API_KEY):
        return None

    b64_image = base64.b64encode(image_bytes).decode("ascii")
    payload = {
        "model": settings.OPENAI_MODEL,
        "temperature": 0.1,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": _build_vision_prompt(source_lang, target_lang)},
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:{mime_type};base64,{b64_image}"},
                    },
                ],
            }
        ],
    }

    try:
        async with httpx.AsyncClient(timeout=12.0) as client:
            resp = await client.post(
                "https://api.openai.com/v1/chat/completions",
                headers={"Authorization": f"Bearer {settings.OPENAI_API_KEY}"},
                json=payload,
            )
            resp.raise_for_status()
            body = resp.json()
        text = body.get("choices", [{}])[0].get("message", {}).get("content", "")
        return parse_llm_json_payload(text)
    except Exception as exc:
        logger.warning("OpenAI Vision OCR failed, falling back: %s", exc)
        return None


def _extract_with_paddleocr(image_bytes: bytes) -> Optional[str]:
    try:
        from paddleocr import PaddleOCR  # type: ignore
        import numpy as np  # type: ignore
        from PIL import Image  # type: ignore
    except ImportError:
        return None

    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        ocr = PaddleOCR(use_angle_cls=True, lang="en", show_log=False)
        result = ocr.ocr(np.array(image), cls=True)
        lines: List[str] = []
        for page in result or []:
            for line in page or []:
                if len(line) >= 2 and isinstance(line[1], (list, tuple)) and line[1]:
                    lines.append(str(line[1][0]))
        return "\n".join(lines).strip()
    except Exception as exc:
        logger.warning("PaddleOCR failed, falling back: %s", exc)
        return None


def _extract_with_tesseract(image_bytes: bytes) -> Optional[str]:
    try:
        import pytesseract  # type: ignore
        from PIL import Image  # type: ignore
    except ImportError:
        return None

    tesseract_cmd = settings.TESSERACT_CMD or "tesseract"
    if not os.path.isabs(tesseract_cmd) and shutil.which(tesseract_cmd) is None:
        return None
    if os.path.isabs(tesseract_cmd) and not os.path.exists(tesseract_cmd):
        return None

    try:
        pytesseract.pytesseract.tesseract_cmd = tesseract_cmd
        image = Image.open(io.BytesIO(image_bytes))
        text = pytesseract.image_to_string(image)
        return text.strip()
    except Exception as exc:
        logger.warning("Tesseract OCR failed, falling back: %s", exc)
        return None


async def process_ocr_extraction(
    file: UploadFile,
    source_lang: Optional[str] = None,
    target_lang: Optional[str] = None,
) -> OcrExtractResponse:
    """
    Execute the multi-tier OCR + Card Parser pipeline with graceful fallbacks.

    Order:
    1. Gemini Vision (if GEMINI_API_KEY configured)
    2. OpenAI Vision (if OPENAI_API_KEY configured)
    3. PaddleOCR (if installed)
    4. Tesseract OCR (if Pillow + pytesseract + tesseract binary available)
    5. Graceful fallback (returns empty cards with informative warnings, never crashes)
    """
    image_bytes, filename, mime_type = await validate_image_upload(file)
    warnings: List[str] = []

    # Tier 1: Vision LLMs (direct text + structured cards)
    gemini_result = await _extract_with_gemini_vision(image_bytes, mime_type, source_lang, target_lang)
    if gemini_result is not None:
        raw_text, cards = gemini_result
        if not cards and raw_text:
            cards, parse_warnings = parse_cards_from_text(raw_text)
            warnings.extend(parse_warnings)
        confidence = 0.92 if cards else 0.45
        return OcrExtractResponse(
            success=True,
            raw_text=raw_text,
            cards=cards,
            confidence=confidence,
            engine="gemini_vision",
            warnings=warnings,
        )

    openai_result = await _extract_with_openai_vision(image_bytes, mime_type, source_lang, target_lang)
    if openai_result is not None:
        raw_text, cards = openai_result
        if not cards and raw_text:
            cards, parse_warnings = parse_cards_from_text(raw_text)
            warnings.extend(parse_warnings)
        confidence = 0.90 if cards else 0.45
        return OcrExtractResponse(
            success=True,
            raw_text=raw_text,
            cards=cards,
            confidence=confidence,
            engine="openai_vision",
            warnings=warnings,
        )

    # Tier 2: Local OCR Engines + Deterministic Card Parser
    paddle_text = _extract_with_paddleocr(image_bytes)
    if paddle_text is not None:
        cards, parse_warnings = parse_cards_from_text(paddle_text)
        warnings.extend(parse_warnings)
        confidence = 0.80 if cards else 0.35
        return OcrExtractResponse(
            success=True,
            raw_text=paddle_text,
            cards=cards,
            confidence=confidence,
            engine="paddleocr",
            warnings=warnings,
        )

    tesseract_text = _extract_with_tesseract(image_bytes)
    if tesseract_text is not None:
        cards, parse_warnings = parse_cards_from_text(tesseract_text)
        warnings.extend(parse_warnings)
        confidence = 0.75 if cards else 0.30
        return OcrExtractResponse(
            success=True,
            raw_text=tesseract_text,
            cards=cards,
            confidence=confidence,
            engine="tesseract",
            warnings=warnings,
        )

    # Tier 3: Graceful Fallback when no engine/key is installed locally
    warnings.append(
        f"Đã nhận ảnh '{filename}' nhưng chưa có engine OCR (Tesseract/PaddleOCR) hoặc API key "
        "(GEMINI_API_KEY / OPENAI_API_KEY) được cấu hình trên máy. "
        "Hệ thống trả về kết quả rỗng an toàn để bạn nhập hoặc cấu hình engine sau."
    )
    return OcrExtractResponse(
        success=True,
        raw_text="",
        cards=[],
        confidence=0.0,
        engine="fallback",
        warnings=warnings,
    )
