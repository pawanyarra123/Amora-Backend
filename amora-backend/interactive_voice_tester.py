import urllib.request
import json
import sys
import time

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
    print("=" * 70)
    print("🎙️ AMORA INTERACTIVE VOICE COMMAND & SPEAKER TESTER")
    print("=" * 70)
    print("Type any voice command spoken by user to test response & speaker audio.")
    print("Examples:")
    print("  • Hey Amora turn off wifi")
    print("  • Hey Amora turn on flashlight")
    print("  • Amora what is the weather today?")
    print("  • Hey Amora open Chrome")
    print("Type 'exit' or 'q' to stop.\n")

    while True:
        try:
            user_input = input("🗣️ Enter Voice Command > ").strip()
            if not user_input:
                continue
            if user_input.lower() in ["exit", "q", "quit"]:
                print("Exiting voice tester. Goodbye!")
                break

            print(f"\n📡 Processing request to backend: '{user_input}'...")
            res = post_json("/v1/chat", {"message": user_input, "synthesize_audio": True})
            
            reply = res.get("reply", "")
            intent = res.get("intent")
            source = res.get("source", "unknown")

            print("=" * 50)
            print(f"💬 AMORA Reply : \"{reply}\"")
            print(f"🎯 Action Intent: {intent}")
            print(f"⚙️ Reply Source: {source}")
            print("=" * 50)

            if reply:
                print("🔊 Speaking response live through PC speakers...")
                speak_out_loud(reply)
                print("✅ Spoken.\n")
            else:
                print("⚠️ No reply text generated.\n")

        except KeyboardInterrupt:
            print("\nExiting voice tester.")
            break
        except Exception as e:
            print(f"❌ Error communicating with backend: {e}\n")

if __name__ == "__main__":
    main()
