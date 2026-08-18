from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
from app.services.voice_service import voice_service

router = APIRouter()

class SynthesizeRequest(BaseModel):
    text: str
    voice_profile_id: str = "default"

@router.post("/voice/synthesize")
async def synthesize_voice(request: SynthesizeRequest):
    if not request.text:
        raise HTTPException(status_code=400, detail="Text required for synthesis")
    return await voice_service.synthesize(request.text, request.voice_profile_id)

@router.post("/voice/enroll")
async def enroll_voice(voice_profile_id: str = Form("default"), file: UploadFile = File(...)):
    audio_bytes = await file.read()
    success = await voice_service.enroll_voice_sample(voice_profile_id, audio_bytes)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to save voice profile sample")
    return {"status": "success", "voice_profile_id": voice_profile_id}
