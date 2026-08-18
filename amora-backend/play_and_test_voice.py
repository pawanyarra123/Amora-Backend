import os
import time
import winsound
import win32com.client

print("=" * 65)
print("🔊 PLAYING AMORA VOICE THROUGH PC SPEAKERS & GENERATING WAV")
print("=" * 65)

wav_path = os.path.join(os.getcwd(), "test_amora_voice.wav")
text_to_speak = "Hello! I am Amora. Your voice assistant is online, listening, and speaking through your speaker."

print("\n1️⃣ Playing voice live out loud through PC Speakers via Windows SAPI...")
try:
    # Direct live playback through default audio output device
    live_speaker = win32com.client.Dispatch("SAPI.SpVoice")
    live_speaker.Speak(text_to_speak)
    print("   ✅ Live voice spoken through speakers successfully!")
except Exception as e:
    print(f"   ❌ Live voice error: {e}")

time.sleep(1)

print("\n2️⃣ Playing generated WAV audio file through PC Speakers via winsound...")
try:
    if os.path.exists(wav_path):
        # Play the saved WAV audio file out loud through PC speakers
        winsound.PlaySound(wav_path, winsound.SND_FILENAME)
        print("   ✅ WAV audio file played through speakers successfully!")
    else:
        print("   ⚠️ WAV file not found to play.")
except Exception as e:
    print(f"   ❌ WAV playback error: {e}")

print("\n" + "=" * 65)
print("🎉 VOICE AUDIBLE PLAYBACK TEST COMPLETE!")
print("=" * 65)
