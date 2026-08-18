from fastapi import APIRouter, WebSocket, WebSocketDisconnect, HTTPException
from pydantic import BaseModel
from typing import Optional, List
import logging
from app.services.llm_service import llm_service
from app.services.voice_service import voice_service
from app.services.training_service import training_service

logger = logging.getLogger("amora.chat")
router = APIRouter()

class ChatRequest(BaseModel):
    message: str
    language: Optional[str] = "en"
    voice_profile_id: Optional[str] = "default"
    synthesize_audio: Optional[bool] = True
    context_memory: Optional[List[str]] = None

class TrainRequest(BaseModel):
    question: str
    answer: str

class CallSummarizeRequest(BaseModel):
    caller: str
    reason: str
    is_emergency: Optional[bool] = False

@router.post("/calls/summarize")
async def summarize_call_endpoint(request: CallSummarizeRequest):
    """
    Generates a 1-sentence AI summary and extracts 1-click action items from screened calls.
    """
    return await llm_service.summarize_call(
        caller=request.caller,
        reason=request.reason,
        is_emergency=request.is_emergency
    )

@router.post("/chat")
async def handle_chat(request: ChatRequest):
    """
    Main conversational reasoning endpoint. Returns text reply, intent commands, and synthesized voice URL/metadata.
    Flow: Groq LLM -> trained DB fallback -> generic fallback
    """
    if not request.message.strip():
        raise HTTPException(status_code=400, detail="Message cannot be empty")

    chat_result = await llm_service.process_chat(
        user_message=request.message,
        language=request.language,
        context_memory=request.context_memory
    )

    reply_text = chat_result.get("reply", "")
    audio_meta = None

    if request.synthesize_audio and reply_text:
        audio_meta = await voice_service.synthesize(
            text=reply_text,
            voice_profile_id=request.voice_profile_id
        )

    return {
        "reply": reply_text,
        "intent": chat_result.get("intent"),
        "language": chat_result.get("language"),
        "source": chat_result.get("source", "unknown"),
        "audio": audio_meta
    }


@router.get("/chat/trained")
async def search_trained(q: str):
    """
    Search the trained conversation database for a matching answer.
    Used by the Android app when it wants to query learned Q&A pairs.
    Returns the best match or a clear 'not found' message.
    """
    if not q.strip():
        raise HTTPException(status_code=400, detail="Query 'q' cannot be empty")

    answer = await training_service.search(q)
    if answer:
        return {"found": True, "answer": answer}
    return {
        "found": False,
        "answer": "I don't have a trained answer for that. Ask me when connected to the internet for a full response."
    }


@router.post("/chat/train")
async def manual_train(request: TrainRequest):
    """
    Manually add a Q&A training pair (e.g., from Settings screen).
    Lets you teach Amora specific facts about yourself or your preferences.
    """
    if not request.question.strip() or not request.answer.strip():
        raise HTTPException(status_code=400, detail="Both question and answer are required")

    saved = await training_service.save_pair(request.question, request.answer)
    return {
        "saved": saved,
        "message": "Training pair saved successfully." if saved else "Similar entry already exists — skipped."
    }


@router.get("/chat/training-data")
async def list_training_data():
    """List all saved training Q&A pairs (for Settings screen)."""
    pairs = await training_service.list_all()
    return {"count": len(pairs), "pairs": pairs}


@router.delete("/chat/training-data/{pair_id}")
async def delete_training_pair(pair_id: int):
    """Delete a specific training pair by ID."""
    deleted = await training_service.delete_pair(pair_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"Training pair {pair_id} not found")
    return {"deleted": True, "id": pair_id}


@router.websocket("/ws/chat")
async def chat_websocket(websocket: WebSocket):
    """
    WebSocket endpoint for real-time streaming text & audio reasoning.
    """
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_json()
            user_msg = data.get("message", "")
            lang = data.get("language", "en")

            if user_msg:
                result = await llm_service.process_chat(user_msg, language=lang)
                await websocket.send_json(result)
    except WebSocketDisconnect:
        pass
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        try:
            await websocket.send_json({"error": str(e)})
        except Exception:
            pass
        await websocket.close()

