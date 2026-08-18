import urllib.request
import json
import sys

BASE_URL = "http://127.0.0.1:8000"


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

def test_wakeword_and_voice():
    print("=" * 65)
    print("🧪 AMORA WAKE-WORD RECOGNITION & VOICE SYNTHESIS TEST SUITE")
    print("=" * 65)

    test_cases = [
        {
            "name": "Direct Voice Command — Flashlight Toggle",
            "input": "Hey Amora turn on flashlight",
            "expected_intent": "TOGGLE_FLASHLIGHT"
        },
        {
            "name": "Direct Voice Command — Weather Query",
            "input": "Amora what is the weather in Tokyo today?",
            "expected_intent": None
        },
        {
            "name": "Direct Voice Command — Memory Storage",
            "input": "Hey Amora remember that my garage code is 9876",
            "expected_intent": "UPDATE_MEMORY"
        },
        {
            "name": "Direct Voice Command — WhatsApp Message",
            "input": "Hey Amora send a whatsapp message to Sarah saying I am running 5 mins late",
            "expected_intent": "SEND_WHATSAPP"
        }
    ]

    passed_count = 0

    for i, tc in enumerate(test_cases, 1):
        print(f"\n[Test {i}/{len(test_cases)}] {tc['name']}")
        print(f"   Input Voice Phrase : \"{tc['input']}\"")
        try:
            res = post_json("/v1/chat", {"message": tc['input'], "synthesize_audio": True})
            
            reply = res.get("reply", "")
            intent = res.get("intent")
            audio = res.get("audio")
            source = res.get("source", "unknown")

            print(f"   Response AI Reply  : \"{reply}\"")
            print(f"   Extracted Intent   : {intent}")
            print(f"   Response Source    : {source}")
            print(f"   Audio Metadata     : {audio}")

            # Verify intent matching if expected
            intent_ok = True
            if tc["expected_intent"]:
                act = intent.get("action") if intent else None
                if act != tc["expected_intent"]:
                    print(f"   ❌ Intent mismatch! Expected: {tc['expected_intent']}, Got: {act}")
                    intent_ok = False
                else:
                    print(f"   ✅ Intent correctly extracted: {act}")
            
            # Verify reply and audio
            reply_ok = bool(reply.strip())
            audio_ok = audio is not None and "engine" in audio

            if reply_ok and audio_ok and intent_ok:
                print(f"   STATUS: ✅ PASSED (Voice recognized & response generated)")
                passed_count += 1
            else:
                print(f"   STATUS: ❌ FAILED (reply_ok={reply_ok}, audio_ok={audio_ok}, intent_ok={intent_ok})")

        except Exception as e:
            print(f"   ❌ Error calling endpoint: {e}")

    print("\n" + "-" * 65)
    print("📢 TESTING VOICE SYNTHESIS DIRECT ENDPOINT (/v1/voice/synthesize)")
    print("-" * 65)

    try:
        synth_res = post_json("/v1/voice/synthesize", {
            "text": "Good afternoon! AMORA voice engine is online and ready.",
            "voice_profile_id": "default"
        })
        print(f"   Synthesize Output  : {synth_res}")
        if synth_res and "engine" in synth_res:
            print("   STATUS: ✅ PASSED (Voice synthesis engine online & giving response)")
            passed_count += 1
        else:
            print("   STATUS: ❌ FAILED")
    except Exception as e:
        print(f"   ❌ Error in voice synthesis test: {e}")

    total_tests = len(test_cases) + 1
    print("\n" + "=" * 65)
    print(f"📊 SUMMARY: {passed_count}/{total_tests} Tests Passed!")
    print("=" * 65)

if __name__ == "__main__":
    test_wakeword_and_voice()
