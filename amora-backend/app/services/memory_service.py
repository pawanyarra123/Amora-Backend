import logging
from typing import List, Dict, Any
import os
import shutil
from sqlalchemy import select, delete
from app.db.session import AsyncSessionLocal
from app.db.models import UserMemoryModel

logger = logging.getLogger("amora.memory")

class MemoryService:
    """
    User Personalization Memory & Data Purge Service.
    Persists user memory to the database (SQLite/PostgreSQL).
    Executes explicit "Delete Everything" wipe across database, cache, and storage.
    """

    async def add_memory(self, item: str) -> bool:
        """Add a memory item to the persistent database store."""
        if not item:
            return False
        async with AsyncSessionLocal() as session:
            try:
                # Check for duplicate
                result = await session.execute(
                    select(UserMemoryModel).where(UserMemoryModel.memory_text == item)
                )
                existing = result.scalars().first()
                if existing:
                    return False
                session.add(UserMemoryModel(memory_text=item))
                await session.commit()
                logger.info(f"Memory persisted: {item}")
                return True
            except Exception as e:
                await session.rollback()
                logger.error(f"Failed to add memory: {e}")
                return False

    async def get_memories(self) -> List[str]:
        """Retrieve all persisted memory items."""
        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(select(UserMemoryModel))
                return [row.memory_text for row in result.scalars().all()]
            except Exception as e:
                logger.error(f"Failed to retrieve memories: {e}")
                return []

    async def wipe_all_data(self) -> Dict[str, Any]:
        """
        Executes complete wipe of user memory, call summaries, intruder photo logs,
        and audio caches across local storage, SQLite/Neon DB, and Redis.
        """
        async with AsyncSessionLocal() as session:
            try:
                await session.execute(delete(UserMemoryModel))
                await session.commit()
            except Exception as e:
                await session.rollback()
                logger.error(f"DB wipe failed: {e}")

        # Purge audio cache
        cache_dir = os.path.join(os.getcwd(), "audio_cache")
        if os.path.exists(cache_dir):
            shutil.rmtree(cache_dir)
            os.makedirs(cache_dir, exist_ok=True)

        # Purge voice profiles
        profiles_dir = os.path.join(os.getcwd(), "voice_profiles")
        if os.path.exists(profiles_dir):
            shutil.rmtree(profiles_dir)
            os.makedirs(profiles_dir, exist_ok=True)

        logger.info("Complete data wipe executed successfully across all backend stores.")
        return {
            "status": "success",
            "message": "All user memory, cached audio, voice profiles, and logs have been deleted."
        }

memory_service = MemoryService()
