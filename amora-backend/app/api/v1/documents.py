from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from app.services.document_service import document_service

router = APIRouter()

class DocumentProcessRequest(BaseModel):
    content: str
    action: str  # summarize, translate, explain
    target_language: Optional[str] = "en"

@router.post("/documents/process")
async def process_document(request: DocumentProcessRequest):
    return await document_service.process_document(
        text_content=request.content,
        action=request.action,
        target_language=request.target_language
    )
