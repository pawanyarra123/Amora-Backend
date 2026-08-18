from fastapi import APIRouter
from app.services.health_service import health_service

router = APIRouter()

@router.get("/health")
async def check_health():
    """
    Health check endpoint consumed by the Android Dashboard Home screen
    to display "Backend: Connected / Not Connected".
    """
    return await health_service.check_health()
