import httpx
import logging
from typing import Dict, Any
from sqlalchemy import text
from app.core.config import settings
from app.db.session import engine

logger = logging.getLogger("amora.health")

class HealthService:
    """
    Nightly Health Check Service.
    Checks API key validity (Groq API), database connectivity, and returns status indicator payload
    for the Home dashboard ("Connected / Not Connected").
    """
    async def check_health(self) -> Dict[str, Any]:
        groq_status = False
        db_status = False

        if settings.GROQ_API_KEY:
            try:
                async with httpx.AsyncClient(timeout=5.0) as client:
                    resp = await client.get(
                        "https://api.groq.com/openai/v1/models",
                        headers={"Authorization": f"Bearer {settings.GROQ_API_KEY}"}
                    )
                    if resp.status_code == 200:
                        groq_status = True
            except Exception as e:
                # str(e) can come back empty for some httpx/httpcore-wrapped SSL/connect
                # errors — logging repr() and the underlying cause makes the real problem
                # visible instead of a blank message.
                logger.error(f"Groq API health check failed: {type(e).__name__}: {e!r} (cause: {e.__cause__!r})")

        # This used to be hardcoded to True with a "Local DB connection ok" comment —
        # it never actually checked anything, so the app's "Test Connection" button
        # (and the Dashboard status) could say "Connected" even while the database
        # was completely unreachable (e.g. the Neon DNS failure this was hit by).
        try:
            async with engine.connect() as conn:
                await conn.execute(text("SELECT 1"))
            db_status = True
        except Exception as e:
            logger.warning(f"Database health check failed: {e}")

        overall_connected = db_status  # Connected to backend API

        return {
            "status": "Connected" if overall_connected else "Not Connected",
            "groq_api_valid": groq_status,
            "database_connected": db_status,
            "environment": settings.ENVIRONMENT,
            "backend_version": "1.0.0"
        }

health_service = HealthService()
