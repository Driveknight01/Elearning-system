from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing import List, Optional

router = APIRouter(prefix="/chatbot", tags=["AI Chatbot & Tutor"])


class CardItem(BaseModel):
    term: str
    definition: str


class SuggestedFlashcardSet(BaseModel):
    title: str
    description: Optional[str] = None
    tags: List[str] = Field(default_factory=list)
    cards: List[CardItem] = Field(default_factory=list)


class ChatMessageRequest(BaseModel):
    session_id: Optional[str] = None
    user_id: Optional[int] = None
    current_set_id: Optional[int] = None
    user_prompt: str


class ChatMessageResponse(BaseModel):
    reply: str
    intent: str = Field(
        default="GENERAL_CHAT",
        description="'GENERAL_CHAT' hoặc 'GENERATE_FLASHCARD_SET'",
    )
    suggested_set: Optional[SuggestedFlashcardSet] = None


@router.post("/chat", response_model=ChatMessageResponse)
async def chat_with_tutor(request: ChatMessageRequest):
    """
    Endpoint gia sư AI nhận ngữ cảnh và sinh bộ thẻ theo JSON schema.
    Sẽ được hoàn thiện ở task T4.1.
    """
    return ChatMessageResponse(
        reply="Xin chào! Tôi là trợ lý học tập SmartFlash AI. Tôi có thể giải thích bài học hoặc tạo bộ thẻ ôn tập cho bạn.",
        intent="GENERAL_CHAT",
        suggested_set=None,
    )
