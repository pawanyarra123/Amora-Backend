import os
import sys
import time
import json
import winsound
import urllib.request

try:
    import speech_recognition as sr
except ImportError:
    print("❌ Error: 'speech_recognition' module not found. Install with: pip install SpeechRecognition")
    sys.exit(1)

try:
    import win32com.client
    HAS_SAPI = True
except Exception:
    HAS_SAPI = False

BASE_URL = "http://127.0.0.1:8000"

def play_chime():
    """Play a short, pleasant wake-up chime tone through speakers."""
    try:
        winsound.Beep(880, 120)  # 880 Hz A5 note for 120 ms
        winsound.Beep(1320, 160) # 1320 Hz E6 note for 160 ms
    except Exception:
        pass

def speak_out_loud(text):
    """Speak text out loud through PC speakers using Windows SAPI."""
    if HAS_SAPI and text:
        try:
            speaker = win32com.client.Dispatch("SAPI.SpVoice")
            speaker.Speak(text)
        except Exception as e:
            print(f"⚠️ Speaker output error: {e}")

def post_json(endpoint, data):
    url = f"{BASE_URL}{endpoint}"
    req_data = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=req_data,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))

def extract_wake_word(phrase):
    """
    Checks if phrase contains 'amora' or 'hey amora'.
    Returns (is_detected: bool, extracted_command: str or None)
    """
    lower = phrase.lower().strip()
    wake_words = ["hey amora", "amora", "hey mora", "hey amore", "ok amora", "okay amora"]
    
    for w in wake_words:
        if w in lower:
            idx = lower.find(w)
            after = phrase[idx + len(w):].strip(" ,!?.")
            return True, (after if after else None)
            
    return False, None

def process_command_and_respond(command):
    print(f"\n📡 Sending Command to AMORA Backend: \"{command}\"")
    try:
        res = post_json("/v1/chat", {"message": command, "synthesize_audio": True})
        
        reply = res.get("reply", "")
        intent = res.get("intent")
        source_engine = res.get("source", "unknown")

        print("=" * 60)
        print(f"🤖 AMORA AI Reply  : \"{reply}\"")
        print(f"🎯 Action Intent   : {intent}")
        print(f"⚙️ Reason Engine   : {source_engine}")
        print("=" * 60)

        if reply:
            print("🔊 Speaking AMORA response out loud through your speakers...")
            speak_out_loud(reply)
            print("✅ Response Complete.\n")
        else:
            print("⚠️ No reply text generated.\n")

    except Exception as e:
        print(f"❌ Error communicating with backend: {e}\n")

def main():
    recognizer = sr.Recognizer()
    recognizer.energy_threshold = 300
    recognizer.dynamic_energy_threshold = True

    print("=" * 75)
    print("🔔 HEY AMORA — WAKE WORD & VOICE RESPONSE TESTER (HEY GOOGLE MODE)")
    print("=" * 75)
    print("Operates strictly like 'Hey Google':")
    print("  1. AMORA remains SILENT and ignores background noise until you say 'Hey Amora'.")
    print("  2. When you say 'Hey Amora' + command (e.g. 'Hey Amora turn off wifi'):")
    print("     → Plays wake chime 🔔, extracts command, and speaks answer out loud!")
    print("  3. When you say ONLY 'Hey Amora':")
    print("     → Plays wake chime 🔔 and listens for your follow-up voice command.")
    print("\nPress Ctrl+C at any time to stop.\n")

    with sr.Microphone() as source:
        print("🎤 Calibrating microphone for ambient room noise... (1 sec)")
        recognizer.adjust_for_ambient_noise(source, duration=1.0)
        print("✅ Microphone Calibrated & Passive Gate Armed!\n")

        while True:
            try:
                print("💤 [PASSIVE MONITORING] Listening for 'Amora' / 'Hey Amora'...")
                audio = recognizer.listen(source, timeout=6.0, phrase_time_limit=6.0)

                try:
                    phrase = recognizer.recognize_google(audio)
                    is_wake_word, command = extract_wake_word(phrase)

                    if not is_wake_word:
                        print(f"   [Ignored non-wake speech: \"{phrase}\"] — No response.")
                        continue

                    # 🔔 WAKE WORD DETECTED!
                    print(f"\n🔔 WAKE WORD DETECTED! Phrase: \"{phrase}\"")
                    play_chime()

                    if command:
                        # User spoke wake word + command together: "Hey Amora turn off wifi"
                        print(f"⚡ Extracted Direct Voice Command: \"{command}\"")
                        process_command_and_respond(command)
                    else:
                        # User spoke wake word only: "Hey Amora"
                        print("🎙️ Listening for follow-up voice command... Speak now!")
                        try:
                            cmd_audio = recognizer.listen(source, timeout=5.0, phrase_time_limit=6.0)
                            captured_cmd = recognizer.recognize_google(cmd_audio)
                            print(f"🗣️ Captured Command: \"{captured_cmd}\"")
                            process_command_and_respond(captured_cmd)
                        except sr.WaitTimeoutError:
                            print("⏱️ Follow-up listening timed out — returning to passive mode.\n")
                        except sr.UnknownValueError:
                            print("❓ Could not understand command — returning to passive mode.\n")

                except sr.UnknownValueError:
                    # Ambient noise / unclarified sound — ignore silently
                    pass
                except sr.RequestError as e:
                    print(f"❌ Speech Recognition network error: {e}")

            except sr.WaitTimeoutError:
                pass
            except KeyboardInterrupt:
                print("\n🛑 Stopping Hey Amora Assistant. Goodbye!")
                break
            except Exception as e:
                print(f"❌ Unexpected error: {e}\n")

if __name__ == "__main__":
    main()
