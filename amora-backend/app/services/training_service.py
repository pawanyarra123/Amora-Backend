import logging
import re
from typing import Optional
from sqlalchemy import select
from app.db.session import AsyncSessionLocal
from app.db.models import ConversationTrainingModel

logger = logging.getLogger("amora.training")

# Words to ignore when building keywords (stop words)
_STOP_WORDS = {
    "a", "an", "the", "is", "it", "in", "on", "at", "to", "do", "be",
    "are", "was", "were", "for", "of", "and", "or", "but", "i", "you",
    "my", "me", "can", "what", "how", "why", "when", "where", "who",
    "please", "hey", "amora", "tell", "me", "about", "could", "would",
    "should", "will", "your", "that", "this", "with", "have", "has",
}

MATCH_THRESHOLD = 0.35  # minimum keyword overlap score to count as a match


def _extract_keywords(text: str) -> set:
    """Tokenize text and return meaningful keywords."""
    tokens = re.findall(r"[a-z0-9]+", text.lower())
    return {t for t in tokens if t not in _STOP_WORDS and len(t) > 1}


def _score(query_kws: set, stored_kws: set) -> float:
    """Jaccard-style overlap score between 0 and 1."""
    if not query_kws or not stored_kws:
        return 0.0
    intersection = query_kws & stored_kws
    union = query_kws | stored_kws
    return len(intersection) / len(union)


class TrainingService:
    """Saves every real Q&A pair and searches them for trained answers."""

    async def save_pair(self, question: str, answer: str) -> bool:
        """
        Auto-save a Q&A pair from a successful Groq conversation.
        If a very similar question already exists, skip to avoid duplicates.
        """
        if not question.strip() or not answer.strip():
            return False

        q_kws = _extract_keywords(question)
        kw_str = " ".join(sorted(q_kws))

        async with AsyncSessionLocal() as session:
            try:
                # Check for near-duplicate (score > 0.8 -> too similar, skip)
                result = await session.execute(select(ConversationTrainingModel))
                rows = result.scalars().all()
                for row in rows:
                    stored_kws = set(row.keywords.split())
                    if _score(q_kws, stored_kws) > 0.8:
                        # Update use_count on the existing match instead
                        row.use_count += 1
                        await session.commit()
                        logger.debug("Duplicate training pair skipped (updated use_count)")
                        return False

                session.add(ConversationTrainingModel(
                    question=question,
                    answer=answer,
                    keywords=kw_str,
                ))
                await session.commit()
                logger.info(f"Training pair saved: '{question[:60]}'")
                return True
            except Exception as e:
                await session.rollback()
                logger.error(f"Failed to save training pair: {e}")
                return False

    async def search(self, question: str) -> Optional[str]:
        """
        Search saved Q&A pairs for the best matching answer.
        Returns the answer string if score >= MATCH_THRESHOLD, else None.
        """
        q_kws = _extract_keywords(question)
        if not q_kws:
            return None

        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(select(ConversationTrainingModel))
                rows = result.scalars().all()

                best_score = 0.0
                best_row = None
                for row in rows:
                    stored_kws = set(row.keywords.split()) if row.keywords else set()
                    score = _score(q_kws, stored_kws)
                    if score > best_score:
                        best_score = score
                        best_row = row

                if best_row and best_score >= MATCH_THRESHOLD:
                    logger.info(
                        f"Trained match (score={best_score:.2f}): '{best_row.question[:50]}'"
                    )
                    best_row.use_count += 1
                    await session.commit()
                    return best_row.answer

                logger.info(f"No trained match for: '{question[:60]}' (best={best_score:.2f})")
                return None
            except Exception as e:
                logger.error(f"Training search failed: {e}")
                return None

    async def list_all(self):
        """Return all saved training pairs ordered by use count."""
        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(
                    select(ConversationTrainingModel).order_by(
                        ConversationTrainingModel.use_count.desc()
                    )
                )
                rows = result.scalars().all()
                return [
                    {
                        "id": r.id,
                        "question": r.question,
                        "answer": r.answer,
                        "use_count": r.use_count,
                        "created_at": r.created_at,
                    }
                    for r in rows
                ]
            except Exception as e:
                logger.error(f"Failed to list training data: {e}")
                return []

    async def delete_pair(self, pair_id: int) -> bool:
        """Delete a specific training pair by ID."""
        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(
                    select(ConversationTrainingModel).where(
                        ConversationTrainingModel.id == pair_id
                    )
                )
                row = result.scalars().first()
                if not row:
                    return False
                await session.delete(row)
                await session.commit()
                return True
            except Exception as e:
                await session.rollback()
                logger.error(f"Failed to delete training pair {pair_id}: {e}")
                return False


training_service = TrainingService()
