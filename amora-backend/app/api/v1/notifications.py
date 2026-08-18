from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Dict, Any
from app.services.notification_service import notification_service

router = APIRouter()

class NotificationsBatchRequest(BaseModel):
    notifications: List[Dict[str, Any]]

@router.post("/notifications/summarize")
async def summarize_notifications(request: NotificationsBatchRequest):
    return await notification_service.process_notifications(request.notifications)
