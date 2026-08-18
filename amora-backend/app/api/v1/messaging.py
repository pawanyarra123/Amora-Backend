from fastapi import APIRouter
from pydantic import BaseModel
from app.services.grammar_service import grammar_service

router = APIRouter()

class MessageCorrectionRequest(BaseModel):
    raw_message: str
    recipient: str

@router.post("/messaging/correct")
async def correct_message(request: MessageCorrectionRequest):
    return await grammar_service.correct_message(request.raw_message, request.recipient)
