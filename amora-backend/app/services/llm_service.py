import json
import logging
from typing import Dict, Any, List, Optional
from app.core.config import settings
from app.services.training_service import training_service
from app.db.session import AsyncSessionLocal
from app.db.models import CallSummaryModel

logger = logging.getLogger("amora.llm")

class LLMService:
    def __init__(self):
        self.groq_api_key = settings.GROQ_API_KEY
        self.model = settings.GROQ_MODEL
        self._client = None
        if self.groq_api_key:
            try:
                from groq import AsyncGroq
                self._client = AsyncGroq(api_key=self.groq_api_key)
            except Exception as e:
                logger.warning(f"Failed to initialize Groq client: {e}")

    async def process_chat(self, user_message: str, language: str = "en", context_memory: Optional[List[str]] = None) -> Dict[str, Any]:
        """
        Processes conversation message, extracts device action intents (if any),
        handles voice-managed Automations, Skills, Memory, and returns conversational response.
        Operates in 'Hey Google' voice assistant mode.
        """
        # Clean leading wake word prefixes if present (e.g. "Hey Amora, turn on the flashlight")
        clean_msg = user_message.strip()
        lower_msg = clean_msg.lower()
        wake_prefixes = ["hey amora", "amora", "hey mora", "hey amore", "ok amora", "okay amora", "a mora", "amor"]
        for prefix in wake_prefixes:
            if lower_msg.startswith(prefix):
                clean_msg = clean_msg[len(prefix):].strip(" ,!?.")
                break
        
        effective_message = clean_msg if clean_msg else user_message

        system_prompt = (
            "You are AMORA, a smart AI voice companion operating like Google Assistant / Hey Google. "
            "Respond concisely, directly, and naturally in 1-2 short sentences suited for voice assistants. "
            "Support English and Telugu as requested. "
            "If the user asks to control the device (flashlight, volume, brightness, wifi, bluetooth, app open) "
            "or manage automations, skills, or memory, include an 'intent' object in JSON."
        )

        intent = self._rule_based_intent_extractor(effective_message)

        if not self._client:
            # No Groq client — try trained data first
            trained = await training_service.search(effective_message)
            if trained:
                return {"reply": trained, "intent": intent, "language": language, "source": "trained"}
            reply = self._generate_fallback_reply(effective_message, language, intent)
            return {"reply": reply, "intent": intent, "language": language, "source": "fallback"}

        try:
            messages = [
                {"role": "system", "content": system_prompt},
            ]
            if context_memory:
                messages.append({"role": "system", "content": f"User memory context: {'; '.join(context_memory)}"})
            messages.append({"role": "user", "content": effective_message})

            response = await self._client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.7,
                max_tokens=256
            )
            reply_text = response.choices[0].message.content or ""
            # Strip any raw markdown JSON block or intent fragments if LLM appended them in conversational reply
            if "intent" in reply_text.lower() or "```" in reply_text:
                lines = [
                    line for line in reply_text.splitlines()
                    if not line.strip().startswith("```")
                    and not line.strip().startswith("{")
                    and not line.strip().startswith("}")
                    and not line.strip().startswith('"intent"')
                    and not line.strip().startswith('intent:')
                ]
                reply_text = "\n".join(lines).strip()

            if not reply_text:
                reply_text = self._generate_fallback_reply(effective_message, language, intent)

            # Grammar Polishing & Voice Confirmation for Messaging Intents
            if intent and intent.get("action") in ["SEND_WHATSAPP", "SEND_SMS"]:
                raw_msg = intent.get("message", "")
                recipient = intent.get("recipient", "Recipient")
                if raw_msg:
                    polish_prompt = f"Fix spelling and polish grammar of this message spoken by user to be natural and correct. Return ONLY the polished message text without quotes: '{raw_msg}'"
                    try:
                        polish_resp = await self._client.chat.completions.create(
                            model=self.model,
                            messages=[{"role": "user", "content": polish_prompt}],
                            temperature=0.3,
                            max_tokens=64
                        )
                        polished_text = polish_resp.choices[0].message.content.strip().strip('"\'')
                        if polished_text:
                            intent["message"] = polished_text
                    except Exception:
                        pass

                intent["requires_confirmation"] = True
                reply_text = f"I've polished your message for {recipient}: \"{intent['message']}\". Would you like me to send it now?"

            # ✅ Auto-save every successful Groq Q&A to training DB
            await training_service.save_pair(user_message, reply_text)

            return {"reply": reply_text, "intent": intent, "language": language, "source": "groq"}
        except Exception as e:
            logger.error(f"Groq API error: {e}")
            # Try trained data as secondary fallback
            trained = await training_service.search(user_message)
            if trained:
                logger.info("Groq failed — answered from trained data")
                return {"reply": trained, "intent": intent, "language": language, "source": "trained"}
            reply = self._generate_fallback_reply(user_message, language, intent)
            return {"reply": reply, "intent": intent, "language": language, "source": "fallback"}

    async def summarize_call(self, caller: str, reason: str, is_emergency: bool = False) -> Dict[str, Any]:
        """
        Generates a 1-sentence AI call summary, detects language (Telugu/Hindi/English),
        translates if needed, and extracts 1-click action tasks. Persists the result to
        CallSummaryModel so retention_service actually has real rows to purge by TTL —
        previously this returned a summary but never saved it anywhere, so the "purge call
        summaries older than 90 days" retention claim had nothing to act on.
        """
        prompt = (
            f"Caller: {caller}\nReason/Transcript: {reason}\nEmergency: {is_emergency}\n"
            "Understand any local slang, idioms, or colloquial expressions (e.g. in Telugu: 'lite teeso/లైట్ తీస్కో' = take it easy/don't worry, 'mama/macha' = bro/friend, 'scene enti' = what's up, 'bunk' = skip class). "
            "Detect the language. If non-English, translate accurately considering slang meanings into natural English. "
            "Generate a JSON object with fields:\n"
            "1. 'detected_language': e.g. 'Telugu (Colloquial)', 'Hindi', 'English'\n"
            "2. 'summary': A clear 1-sentence English summary.\n"
            "3. 'translated_transcript': Natural English translation of what they meant.\n"
            "4. 'tasks': A list of 1 to 2 short actionable tasks.\n"
            "Return ONLY JSON."
        )
        if not self._client:
            result = {
                "detected_language": "English",
                "summary": f"{caller} called: {reason}",
                "translated_transcript": reason,
                "tasks": [f"Follow up with {caller}"]
            }
            await self._persist_call_summary(caller, reason, is_emergency, result["summary"])
            return result
        try:
            response = await self._client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
                response_format={"type": "json_object"}
            )
            content = response.choices[0].message.content
            data = json.loads(content)
            result = {
                "detected_language": data.get("detected_language", "English"),
                "summary": data.get("summary", f"{caller} called: {reason}"),
                "translated_transcript": data.get("translated_transcript", reason),
                "tasks": data.get("tasks", [f"Follow up with {caller}"])
            }
            await self._persist_call_summary(caller, reason, is_emergency, result["summary"])
            return result
        except Exception as e:
            logger.error(f"Call summarize error: {e}")
            result = {
                "detected_language": "Unknown",
                "summary": f"{caller} called regarding: {reason}",
                "translated_transcript": reason,
                "tasks": [f"Call {caller} back"]
            }
            await self._persist_call_summary(caller, reason, is_emergency, result["summary"])
            return result

    async def _persist_call_summary(self, caller: str, reason: str, is_emergency: bool, summary_text: str) -> None:
        """Save the summary to CallSummaryModel so retention_service has real rows to purge by TTL."""
        try:
            async with AsyncSessionLocal() as session:
                session.add(CallSummaryModel(
                    caller_name=caller,
                    phone_number=caller,
                    reason=reason,
                    urgency="emergency" if is_emergency else "normal",
                    summary_text=summary_text
                ))
                await session.commit()
        except Exception as e:
            logger.error(f"Failed to persist call summary: {e}")

    def _rule_based_intent_extractor(self, text: str) -> Optional[Dict[str, Any]]:
        lower = text.lower()
        if "flashlight" in lower:
            if any(w in lower for w in ["on", "enable", "start"]):
                return {"action": "TOGGLE_FLASHLIGHT", "state": True}
            elif any(w in lower for w in ["off", "disable", "stop"]):
                return {"action": "TOGGLE_FLASHLIGHT", "state": False}
        elif "wifi" in lower or "wi-fi" in lower:
            if any(w in lower for w in ["on", "enable", "connect"]):
                return {"action": "TOGGLE_WIFI", "state": True}
            elif any(w in lower for w in ["off", "disable", "disconnect"]):
                return {"action": "TOGGLE_WIFI", "state": False}
        elif "bluetooth" in lower:
            if any(w in lower for w in ["on", "enable", "connect"]):
                return {"action": "TOGGLE_BLUETOOTH", "state": True}
            elif any(w in lower for w in ["off", "disable", "disconnect"]):
                return {"action": "TOGGLE_BLUETOOTH", "state": False}
        elif "increase volume" in lower or "volume up" in lower:
            return {"action": "ADJUST_VOLUME", "delta": +15}
        elif "decrease volume" in lower or "volume down" in lower:
            return {"action": "ADJUST_VOLUME", "delta": -15}
        elif "mute" in lower:
            return {"action": "SET_VOLUME", "level": 0}
        elif "open " in lower:
            app_name = lower.split("open ", 1)[1].strip()
            return {"action": "OPEN_APP", "app_name": app_name}
        elif "remember that" in lower:
            memory_item = text.split("remember that", 1)[1].strip()
            return {"action": "UPDATE_MEMORY", "item": memory_item}
        elif "create automation" in lower:
            rule_item = text.split("create automation", 1)[1].strip()
            return {"action": "CREATE_AUTOMATION", "rule": rule_item}
        elif "send a whatsapp message to" in lower or "whatsapp message to" in lower or "whatsapp to" in lower or "send whatsapp to" in lower:
            clean = lower.replace("send a whatsapp message to", "").replace("whatsapp message to", "").replace("send whatsapp to", "").replace("whatsapp to", "").strip()
            if " that " in clean:
                recipient, msg = clean.split(" that ", 1)
            elif " saying " in clean:
                recipient, msg = clean.split(" saying ", 1)
            else:
                parts = clean.split(" ", 1)
                recipient = parts[0]
                msg = parts[1] if len(parts) > 1 else ""
            return {
                "action": "SEND_WHATSAPP",
                "recipient": recipient.strip().title(),
                "message": msg.strip().capitalize()
            }
        elif "send sms to" in lower or "send message to" in lower or "text to" in lower:
            clean = lower.replace("send sms to", "").replace("send message to", "").replace("text to", "").strip()
            if " that " in clean:
                recipient, msg = clean.split(" that ", 1)
            elif " saying " in clean:
                recipient, msg = clean.split(" saying ", 1)
            else:
                parts = clean.split(" ", 1)
                recipient = parts[0]
                msg = parts[1] if len(parts) > 1 else ""
            return {
                "action": "SEND_SMS",
                "recipient": recipient.strip().title(),
                "message": msg.strip().capitalize()
            }
        elif "enable skill" in lower:
            skill = text.split("enable skill", 1)[1].strip()
            return {"action": "TOGGLE_SKILL", "skill": skill, "enabled": True}
        return None

    def _generate_fallback_reply(self, message: str, language: str, intent: Optional[Dict[str, Any]]) -> str:
        if language == "te":
            if intent:
                return f"సరే, {intent.get('action')} ప్రక్రియను నిర్వహిస్తున్నాను."
            return "నమస్కారం! నేను AMORA. మీకు ఏవిధంగా సహాయపడగలను?"
        else:
            if intent:
                action = intent.get("action")
                if action == "TOGGLE_FLASHLIGHT":
                    return "Turning on the flashlight." if intent.get("state") else "Turning off the flashlight."
                elif action == "TOGGLE_WIFI":
                    return "Opening Wi-Fi panel to turn on Wi-Fi." if intent.get("state") else "Opening Wi-Fi panel to turn off Wi-Fi."
                elif action == "TOGGLE_BLUETOOTH":
                    return "Opening Bluetooth panel to turn on Bluetooth." if intent.get("state") else "Opening Bluetooth panel to turn off Bluetooth."
                elif action == "UPDATE_MEMORY":
                    return f"Saved to memory: {intent.get('item', 'your note')}."
                elif action == "CREATE_AUTOMATION":
                    return "Automation created successfully."
                elif action == "ADJUST_VOLUME" or action == "SET_VOLUME":
                    return "Adjusted device volume."
                elif action == "OPEN_APP":
                    return f"Opening {intent.get('app_name', 'app')}."
                return f"Certainly! Executing {action} right away."
            return "Hello! I am Amora. How can I assist you today?"

llm_service = LLMService()
