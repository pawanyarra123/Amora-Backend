import logging
from typing import Dict, Any
from app.services.llm_service import llm_service

logger = logging.getLogger("amora.documents")

class DocumentService:
    """
    Files & Documents Assistant Service.
    Summarizes, translates, explains, and organizes document content.
    """
    async def process_document(self, text_content: str, action: str, target_language: str = "en") -> Dict[str, Any]:
        if action == "summarize":
            prompt = f"Summarize the following document content clearly and concisely:\n\n{text_content[:3000]}"
        elif action == "translate":
            prompt = f"Translate the following text accurately into {target_language}:\n\n{text_content[:3000]}"
        elif action == "explain":
            prompt = f"Explain the main concepts and key takeaways in simple terms from this text:\n\n{text_content[:3000]}"
        else:
            prompt = f"Analyze the following document:\n\n{text_content[:3000]}"

        result = await llm_service.process_chat(prompt, language=target_language)
        return {
            "action": action,
            "result": result.get("reply", "Analysis complete."),
            "character_count": len(text_content)
        }

document_service = DocumentService()
