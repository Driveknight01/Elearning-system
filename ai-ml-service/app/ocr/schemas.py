from typing import List
from pydantic import BaseModel, Field


class ExtractedCardItem(BaseModel):
    """A single flashcard term-definition pair extracted from an OCR image."""

    term: str = Field(..., min_length=1, description="Thuật ngữ / mặt trước của thẻ")
    definition: str = Field(..., min_length=1, description="Định nghĩa / mặt sau của thẻ")


class OcrTextBox(BaseModel):
    """Normalized (0.0 - 1.0) bounding box for a recognized text region on the image."""

    id: str = Field(..., description="Unique box identifier")
    text: str = Field(..., description="Recognized text inside this bounding box")
    x: float = Field(..., ge=0.0, le=1.0, description="Normalized left coordinate")
    y: float = Field(..., ge=0.0, le=1.0, description="Normalized top coordinate")
    width: float = Field(..., ge=0.0, le=1.0, description="Normalized box width")
    height: float = Field(..., ge=0.0, le=1.0, description="Normalized box height")
    angle: float = Field(default=0.0, description="Clockwise rotation angle in degrees")
    confidence: float = Field(default=1.0, ge=0.0, le=1.0)


class OcrExtractResponse(BaseModel):
    """Standardized response payload returned by POST /ocr/extract."""

    success: bool = True
    raw_text: str = Field(default="", description="Văn bản thô trích xuất được từ ảnh")
    cards: List[ExtractedCardItem] = Field(
        default_factory=list,
        description="Danh sách cặp thuật ngữ - định nghĩa đã phân tích",
    )
    boxes: List[OcrTextBox] = Field(
        default_factory=list,
        description="Danh sách bounding box chuẩn hóa (0..1) phục vụ vuốt chọn text trên ảnh",
    )
    confidence: float = Field(
        default=0.0,
        ge=0.0,
        le=1.0,
        description="Độ tin cậy ước lượng của kết quả trích xuất (0.0 - 1.0)",
    )
    engine: str = Field(
        default="fallback",
        description="Engine thực tế đã xử lý: gemini_vision | openai_vision | paddleocr | rapidocr | tesseract | fallback",
    )
    warnings: List[str] = Field(
        default_factory=list,
        description="Cảnh báo nếu thiếu engine, ảnh mờ hoặc không tách được cặp thẻ",
    )
