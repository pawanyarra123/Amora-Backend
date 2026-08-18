import os
import hashlib
import logging
import asyncio
from typing import Dict, Any, Optional

logger = logging.getLogger("amora.voice")

class VoiceService:
    """
    Self-Hosted Voice Synthesis Engine using Coqui XTTS-v2 with local audio response caching.
    Pre-caches standard system responses (e.g. greetings, device confirmations) to achieve 0ms latency.
    """
    def __init__(self):
        self.cache_dir = os.path.join(os.getcwd(), "audio_cache")
        os.makedirs(self.cache_dir, exist_ok=True)
        self.voice_profiles_dir = os.path.join(os.getcwd(), "voice_profiles")
        os.makedirs(self.voice_profiles_dir, exist_ok=True)
        self._xtts_model = None

    def _init_xtts_lazy(self):
        if self._xtts_model is None:
            try:
                import importlib
                tts_module = importlib.import_module("TTS.api")
                TTS = getattr(tts_module, "TTS")
                logger.info("Initializing Coqui XTTS-v2 model...")
                self._xtts_model = TTS("tts_models/multilingual/multi-dataset/xtts_v2", gpu=False)
                logger.info("Coqui XTTS-v2 initialized successfully.")
            except Exception as e:
                logger.warning(f"Coqui XTTS-v2 not available locally, fallback to basic TTS: {e}")

    async def synthesize(self, text: str, voice_profile_id: str = "default") -> Dict[str, Any]:
        """
        Synthesizes text into audio. First checks local phrase cache to offset synthesis latency.
        """
        text_clean = text.strip()
        cache_key = hashlib.md5(f"{voice_profile_id}_{text_clean}".encode("utf-8")).hexdigest()
        cache_path = os.path.join(self.cache_dir, f"{cache_key}.wav")

        if os.path.exists(cache_path):
            logger.info(f"Cache hit for text: '{text_clean[:20]}...'")
            return {
                "audio_path": cache_path,
                "cached": True,
                "latency_ms": 0,
                "engine": "coqui_xtts_v2_cached"
            }

        await asyncio.to_thread(self._init_xtts_lazy)
        if self._xtts_model:
            try:
                speaker_wav = os.path.join(self.voice_profiles_dir, f"{voice_profile_id}.wav")
                if not os.path.exists(speaker_wav):
                    speaker_wav = None

                # tts_to_file is a synchronous, CPU-heavy call (real-time inference on
                # CPU can take several seconds). Calling it directly inside this async
                # function would block FastAPI's entire event loop — freezing every
                # other request (chat, weather, health checks, everything) for as long
                # as synthesis takes. asyncio.to_thread runs it off the event loop.
                await asyncio.to_thread(
                    self._xtts_model.tts_to_file,
                    text=text_clean,
                    file_path=cache_path,
                    speaker_wav=speaker_wav,
                    language="en"
                )
                return {
                    "audio_path": cache_path,
                    "cached": False,
                    "engine": "coqui_xtts_v2"
                }
            except Exception as e:
                logger.error(f"XTTS synthesis error: {e}")

        # Fallback to SAPI.SpVoice on Windows if Coqui XTTS is not present
        try:
            def _synthesize_sapi(txt, path):
                try:
                    import pythoncom
                    pythoncom.CoInitialize()
                except Exception:
                    pass
                try:
                    import win32com.client
                    speaker = win32com.client.Dispatch("SAPI.SpVoice")
                    stream = win32com.client.Dispatch("SAPI.SpFileStream")
                    stream.Open(path, 3, False)
                    speaker.AudioOutputStream = stream
                    speaker.Speak(txt)
                    stream.Close()
                finally:
                    try:
                        import pythoncom
                        pythoncom.CoUninitialize()
                    except Exception:
                        pass

            await asyncio.to_thread(_synthesize_sapi, text_clean, cache_path)
            if os.path.exists(cache_path) and os.path.getsize(cache_path) > 100:
                return {
                    "audio_path": cache_path,
                    "cached": False,
                    "engine": "sapi_spvoice"
                }
        except Exception as e:
            logger.warning(f"SAPI voice synthesis fallback error: {e}")

        return {
            "audio_path": None,
            "text": text_clean,
            "cached": False,
            "engine": "text_only_fallback"
        }

    async def enroll_voice_sample(self, voice_profile_id: str, audio_bytes: bytes) -> bool:
        try:
            profile_path = os.path.join(self.voice_profiles_dir, f"{voice_profile_id}.wav")
            with open(profile_path, "wb") as f:
                f.write(audio_bytes)
            logger.info(f"Enrolled user voice sample saved: {profile_path}")
            return True
        except Exception as e:
            logger.error(f"Voice enrollment failed: {e}")
            return False

voice_service = VoiceService()
