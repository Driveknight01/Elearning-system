from typing import Optional

from fastapi import APIRouter, File, Form, UploadFile

from app.ocr.schemas import OcrExtractResponse
from app.ocr.service import process_ocr_extraction

router = APIRouter(prefix="/ocr", tags=["OCR Extraction"])


@router.post("/extract", response_model=OcrExtractResponse)
async def extract_flashcards_from_image(
    file: UploadFile = File(..., description="Ảnh chụp tài liệu / sách / bảng từ vựng"),
    source_lang: Optional[str] = Form(default=None, description="Ngôn ngữ thuật ngữ (tùy chọn)"),
    target_lang: Optional[str] = Form(default=None, description="Ngôn ngữ định nghĩa (tùy chọn)"),
):
    """
    Nhận file ảnh tài liệu (multipart/form-data), trích xuất văn bản bằng Vision LLM hoặc OCR cục bộ
    và phân tích thành mảng cặp thuật ngữ - định nghĩa `[{term, definition}]`.
    """
    return await process_ocr_extraction(file=file, source_lang=source_lang, target_lang=target_lang)
