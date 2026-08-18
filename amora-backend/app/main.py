from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging
from app.core.config import settings
from app.api.v1 import chat, voice, messaging, notifications, documents, health, memory, weather, news
from app.core.scheduler import start_scheduler, stop_scheduler
from app.db.session import init_db

logger = logging.getLogger("amora.main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: initialize database tables, then start background scheduler.
    # A DB outage (wrong URL, DNS issue, cloud DB paused, etc.) used to crash the
    # entire backend on startup — every endpoint (chat, weather, health) would go
    # down even though most of them don't touch the database. Log and continue
    # instead, so a temporary DB problem doesn't take down features that don't need it.
    try:
        await init_db()
    except Exception as e:
        logger.error(
            f"Database initialization failed — starting anyway, but endpoints that "
            f"touch the DB (memory, call summaries, retention) will fail until this "
            f"is fixed. Error: {e}"
        )
    start_scheduler()
    yield
    # Shutdown: stop background scheduler
    stop_scheduler()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version="1.0.0",
    lifespan=lifespan
)

import os

app.add_middleware(
    CORSMiddleware,
    # In production, set CORS_ORIGINS env var to your specific origin(s)
    # e.g. CORS_ORIGINS=https://yourapp.example.com
    allow_origins=os.getenv("CORS_ORIGINS", "*").split(","),
    allow_credentials=os.getenv("CORS_ORIGINS", "*") != "*",
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register V1 Routers
app.include_router(health.router, prefix=settings.API_V1_STR, tags=["Health"])
app.include_router(chat.router, prefix=settings.API_V1_STR, tags=["Chat & WS"])
app.include_router(voice.router, prefix=settings.API_V1_STR, tags=["Voice Studio"])
app.include_router(messaging.router, prefix=settings.API_V1_STR, tags=["Messaging"])
app.include_router(notifications.router, prefix=settings.API_V1_STR, tags=["Notifications"])
app.include_router(documents.router, prefix=settings.API_V1_STR, tags=["Documents"])
app.include_router(memory.router, prefix=settings.API_V1_STR, tags=["Memory"])
app.include_router(weather.router, prefix=settings.API_V1_STR, tags=["Weather"])
app.include_router(news.router, prefix=settings.API_V1_STR, tags=["News"])

@app.get("/")
async def root():
    return {
        "message": "AMORA AI Companion is active",
        "status": "online",
        "version": "1.0.0"
    }
