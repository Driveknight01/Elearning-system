from fastapi import APIRouter, UploadFile, File
from pydantic import BaseModel, Field
from typing import List

router = APIRouter(prefix="/ocr", tags=["OCR Extraction"])


class ExtractedCard(BaseModel):
    term: str
    definition: str


class OcrExtractResponse(BaseModel):
    success: bool = True
    raw_text: str = ""
    cards: List[ExtractedCard] = Field(default_factory=list)


@router.post("/extract", response_model=OcrExtractResponse)
async def extract_flashcards_from_image(file: UploadFile = File(...)):
    """
    Endpoint nhận file ảnh tài liệu và trích xuất danh sách cặp thuật ngữ - định nghĩa.
    Sẽ được hoàn thiện ở task T3.5.
    """
    return OcrExtractResponse(
        success=True,
        raw_text=f"Skeleton OCR extracted from: {file.filename}",
        cards=[
            ExtractedCard(term="Sample Term 1", definition="Sample Definition 1"),
            ExtractedCard(term="Sample Term 2", definition="Sample Definition 2"),
        ],
    )
