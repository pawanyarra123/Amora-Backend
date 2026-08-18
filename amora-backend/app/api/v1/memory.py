from fastapi import APIRouter
from pydantic import BaseModel
from app.services.memory_service import memory_service

router = APIRouter()

class AddMemoryRequest(BaseModel):
    item: str

@router.get("/memory")
async def get_memories():
    return {"memories": await memory_service.get_memories()}

@router.post("/memory")
async def add_memory(request: AddMemoryRequest):
    success = await memory_service.add_memory(request.item)
    return {"status": "added" if success else "exists"}

@router.delete("/memory/wipe")
async def wipe_all_data():
    """
    Executes immediate complete wipe of all user memory, call summaries, intruder photos, and audio caches.
    """
    return await memory_service.wipe_all_data()
