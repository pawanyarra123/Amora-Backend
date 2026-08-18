import logging
from typing import List, Dict, Any

logger = logging.getLogger("amora.notifications")

class NotificationService:
    """
    Notification Summarization & Prioritization Engine.
    Groups incoming Android notifications, filters noise, and provides high-level AI summaries.
    """
    async def process_notifications(self, notifications: List[Dict[str, Any]]) -> Dict[str, Any]:
        if not notifications:
            return {"summary": "No notifications to summarize.", "urgent": [], "normal": []}

        urgent = []
        normal = []
        low_priority = []

        for notif in notifications:
            package = notif.get("package_name", "").lower()
            title = notif.get("title", "")
            text = notif.get("text", "")
            
            # Simple heuristic priority classification
            if any(w in (title + text).lower() for w in ["urgent", "sos", "alert", "emergency", "otp", "code", "call"]):
                urgent.append(notif)
            elif any(w in package for w in ["whatsapp", "telegram", "messages", "gmail", "slack"]):
                normal.append(notif)
            else:
                low_priority.append(notif)

        summary_text = (
            f"You have {len(urgent)} urgent, {len(normal)} standard messages, "
            f"and {len(low_priority)} promotional notifications."
        )

        return {
            "summary": summary_text,
            "urgent": urgent,
            "normal": normal,
            "low_priority_count": len(low_priority)
        }

notification_service = NotificationService()
