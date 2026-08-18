import logging
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from app.services.health_service import health_service
from app.services.retention_service import retention_service

logger = logging.getLogger("amora.scheduler")
scheduler = AsyncIOScheduler()

def start_scheduler():
    """Starts APScheduler background jobs for nightly health check and daily data retention purge."""
    try:
        # Schedule daily data retention purge at 02:00 AM
        scheduler.add_job(
            retention_service.run_daily_retention_purge,
            'cron',
            hour=2,
            minute=0,
            id="daily_retention_purge"
        )

        # Schedule nightly server health check at 00:00 AM
        scheduler.add_job(
            health_service.check_health,
            'cron',
            hour=0,
            minute=0,
            id="nightly_health_check"
        )

        scheduler.start()
        logger.info("APScheduler initialized: Nightly health & retention jobs active.")
    except Exception as e:
        logger.error(f"Scheduler failed to start: {e}")

def stop_scheduler():
    if scheduler.running:
        scheduler.shutdown()
        logger.info("APScheduler stopped.")
