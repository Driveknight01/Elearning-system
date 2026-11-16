from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing import List, Dict

router = APIRouter(prefix="/dkt", tags=["Deep Knowledge Tracing"])


class DktInteraction(BaseModel):
    card_id: int = Field(..., description="ID của thẻ flashcard")
    is_correct: bool = Field(..., description="Kết quả trả lời: True nếu nhớ, False nếu quên")


class DktPredictRequest(BaseModel):
    user_id: int
    set_id: int
    history: List[DktInteraction] = Field(default_factory=list)


class DktPredictResponse(BaseModel):
    user_id: int
    set_id: int
    mastery_scores: Dict[int, float] = Field(
        default_factory=dict,
        description="Xác suất làm đúng (0.0 - 1.0) cho từng thẻ",
    )
    model_version: str = "dkt-lstm-v1-skeleton"


@router.post("/predict", response_model=DktPredictResponse)
async def predict_mastery(request: DktPredictRequest):
    """
    Endpoint dự đoán độ thành thạo kiến thức (DKT).
    Sẽ được hoàn thiện ở task T5.2 bằng PyTorch LSTM.
    """
    # Placeholder trả về xác suất mặc định cho skeleton
    scores = {item.card_id: 0.85 if item.is_correct else 0.40 for item in request.history}
    return DktPredictResponse(
        user_id=request.user_id,
        set_id=request.set_id,
        mastery_scores=scores,
        model_version="dkt-lstm-v1-skeleton",
    )
