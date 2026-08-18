import logging
from typing import Dict, Any
from app.services.llm_service import llm_service

logger = logging.getLogger("amora.grammar")

class GrammarService:
    """
    Grammar & Phrasing Correction Service.
    When a user asks Amora to send a message, this service refines the tone/grammar,
    presenting original vs. corrected versions for explicit user confirmation.
    """
    async def correct_message(self, raw_message: str, recipient: str) -> Dict[str, Any]:
        prompt = (
            f"Correct the grammar and improve the tone of this message intended for '{recipient}'. "
            f"Do not change the underlying meaning. Output ONLY a JSON object with keys 'corrected_text' and 'changes_summary'.\n\n"
            f"Message: \"{raw_message}\""
        )
        result = await llm_service.process_chat(prompt)
        reply = result.get("reply", "")

        # Try to parse JSON output from LLM, or fallback to simple refinement
        try:
            import json
            parsed = json.loads(reply)
            return {
                "recipient": recipient,
                "original_text": raw_message,
                "corrected_text": parsed.get("corrected_text", raw_message),
                "changes_summary": parsed.get("changes_summary", "Refined grammar and tone.")
            }
        except Exception:
            return {
                "recipient": recipient,
                "original_text": raw_message,
                "corrected_text": reply.strip('" \n'),
                "changes_summary": "Polished phrasing for clarity."
            }

grammar_service = GrammarService()
