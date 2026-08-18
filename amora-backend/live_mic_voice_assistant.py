import sys
import time
import json
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

def speak_out_loud(text):
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

def main():
    recognizer = sr.Recognizer()
    recognizer.energy_threshold = 300
    recognizer.dynamic_energy_threshold = True

    print("=" * 70)
    print("🎙️ LIVE MICROPHONE VOICE ASSISTANT TESTER FOR AMORA")
    print("=" * 70)
    print("Speak any command into your PC microphone!")
    print("Examples to try:")
    print("  👉 'Hey Amora turn off wifi'")
    print("  👉 'Hey Amora turn on flashlight'")
    print("  👉 'Amora what is the weather today?'")
    print("  👉 'Hey Amora remember my meeting is at 3 PM'")
    print("\nPress Ctrl+C at any time to stop.\n")

    with sr.Microphone() as source:
        print("🎤 Calibrating microphone for ambient room noise... (1 sec)")
        recognizer.adjust_for_ambient_noise(source, duration=1.0)
        print("✅ Microphone Ready & Armed!\n")

        while True:
            try:
                print("🎧 Listening to your microphone... Speak now!")
                audio = recognizer.listen(source, timeout=8.0, phrase_time_limit=8.0)
                print("⏳ Processing your spoken voice...")

                # Speech to text via Google STT API
                spoken_text = recognizer.recognize_google(audio)
                print(f"\n🗣️ You Spoke into Mic: \"{spoken_text}\"")

                # Send to AMORA backend reasoning API
                print("📡 Sending command to AMORA backend...")
                res = post_json("/v1/chat", {"message": spoken_text, "synthesize_audio": True})

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
                    print("✅ Spoken!\n")
                else:
                    print("⚠️ No reply text generated.\n")

            except sr.WaitTimeoutError:
                print("⏱️ Listening timed out (no speech detected). Listening again...\n")
            except sr.UnknownValueError:
                print("❓ Could not understand audio. Please speak clearly into your mic.\n")
            except sr.RequestError as e:
                print(f"❌ Speech Recognition network error: {e}\n")
            except KeyboardInterrupt:
                print("\n🛑 Stopping voice assistant tester. Goodbye!")
                break
            except Exception as e:
                print(f"❌ Error: {e}\n")

if __name__ == "__main__":
    main()
