from typing import List
from pydantic import BaseModel, Field


class ExtractedCardItem(BaseModel):
    """A single flashcard term-definition pair extracted from an OCR image."""

    term: str = Field(..., min_length=1, description="Thuật ngữ / mặt trước của thẻ")
    definition: str = Field(..., min_length=1, description="Định nghĩa / mặt sau của thẻ")


class OcrExtractResponse(BaseModel):
    """Standardized response payload returned by POST /ocr/extract."""

    success: bool = True
    raw_text: str = Field(default="", description="Văn bản thô trích xuất được từ ảnh")
    cards: List[ExtractedCardItem] = Field(
        default_factory=list,
        description="Danh sách cặp thuật ngữ - định nghĩa đã phân tích",
    )
    confidence: float = Field(
        default=0.0,
        ge=0.0,
        le=1.0,
        description="Độ tin cậy ước lượng của kết quả trích xuất (0.0 - 1.0)",
    )
    engine: str = Field(
        default="fallback",
        description="Engine thực tế đã xử lý: gemini_vision | openai_vision | paddleocr | tesseract | fallback",
    )
    warnings: List[str] = Field(
        default_factory=list,
        description="Cảnh báo nếu thiếu engine, ảnh mờ hoặc không tách được cặp thẻ",
    )
