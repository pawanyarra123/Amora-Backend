import os
import time
import logging
from typing import Dict, Any
from sqlalchemy import select, delete
from app.core.config import settings
from app.db.session import AsyncSessionLocal
from app.db.models import CallSummaryModel, IntruderLogModel

logger = logging.getLogger("amora.retention")

class RetentionService:
    """
    Automated Data Retention & Purge Manager.
    Automatically purges expired intruder logs (>30 days), call summaries (>90 days),
    and system logs (>7 days) on schedule.
    """
    async def run_daily_retention_purge(self) -> Dict[str, Any]:
        now = time.time()
        purged_counts = {"photos": 0, "call_summaries": 0, "logs": 0}

        # Transient audio cache files older than TTL_LOGS_DAYS
        cache_dir = os.path.join(os.getcwd(), "audio_cache")
        if os.path.exists(cache_dir):
            for fname in os.listdir(cache_dir):
                fpath = os.path.join(cache_dir, fname)
                if os.path.isfile(fpath):
                    file_age_days = (now - os.path.getmtime(fpath)) / (24 * 3600)
                    if file_age_days > settings.TTL_LOGS_DAYS:
                        try:
                            os.remove(fpath)
                            purged_counts["logs"] += 1
                        except Exception as e:
                            logger.error(f"Error removing expired file {fpath}: {e}")

        # Call summaries older than TTL_CALL_SUMMARIES_DAYS — this used to be described
        # in the docstring above but never actually implemented; the DB rows just grew
        # forever.
        call_summary_cutoff = now - (settings.TTL_CALL_SUMMARIES_DAYS * 24 * 3600)
        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(
                    delete(CallSummaryModel).where(CallSummaryModel.created_at < call_summary_cutoff)
                )
                await session.commit()
                purged_counts["call_summaries"] = result.rowcount or 0
            except Exception as e:
                await session.rollback()
                logger.error(f"Error purging expired call summaries: {e}")

        # Intruder photos older than TTL_INTRUDER_PHOTOS_DAYS — remove both the DB row
        # and the actual photo file on disk (photo_path), then delete the row.
        photo_cutoff = now - (settings.TTL_INTRUDER_PHOTOS_DAYS * 24 * 3600)
        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(
                    select(IntruderLogModel).where(IntruderLogModel.created_at < photo_cutoff)
                )
                expired_logs = result.scalars().all()
                for log_row in expired_logs:
                    if log_row.photo_path and os.path.exists(log_row.photo_path):
                        try:
                            os.remove(log_row.photo_path)
                        except Exception as e:
                            logger.error(f"Error removing intruder photo {log_row.photo_path}: {e}")
                    await session.delete(log_row)
                await session.commit()
                purged_counts["photos"] = len(expired_logs)
            except Exception as e:
                await session.rollback()
                logger.error(f"Error purging expired intruder logs: {e}")

        logger.info(f"Daily retention purge completed: {purged_counts}")
        return {
            "status": "completed",
            "purged": purged_counts,
            "ttl_photos_days": settings.TTL_INTRUDER_PHOTOS_DAYS,
            "ttl_summaries_days": settings.TTL_CALL_SUMMARIES_DAYS,
            "ttl_logs_days": settings.TTL_LOGS_DAYS
        }

retention_service = RetentionService()
