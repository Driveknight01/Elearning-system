from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime, timezone

from app.core.config import settings
from app.dkt.router import router as dkt_router
from app.ocr.router import router as ocr_router
from app.chatbot.router import router as chatbot_router

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="SmartFlash AI/ML Microservice for DKT, OCR and AI Chatbot Tutor",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS Middleware (cho phép Spring Boot và Frontend Web gọi sang)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Đăng ký các module routers
app.include_router(dkt_router)
app.include_router(ocr_router)
app.include_router(chatbot_router)


@app.get("/health", tags=["Health"])
async def health_check():
    """Endpoint kiểm tra trạng thái hoạt động của AI/ML Service."""
    return {
        "status": "healthy",
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "environment": settings.APP_ENV,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


@app.get("/", tags=["Health"])
async def root():
    return {
        "message": f"Welcome to {settings.APP_NAME} v{settings.APP_VERSION}",
        "docs": "/docs",
        "health": "/health",
    }
