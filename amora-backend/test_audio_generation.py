import os
import sys
import wave

print("=" * 60)
print("🔊 TESTING VOICE SYNTHESIS & WAV AUDIO GENERATION")
print("=" * 60)

output_wav = os.path.join(os.getcwd(), "test_amora_voice.wav")
text_to_speak = "Hello! I am Amora. Your voice assistant is online and working."

sapi_success = False

# Method 1: Windows SAPI.SpVoice (Native Windows Speech Synthesizer)
try:
    import win32com.client
    speaker = win32com.client.Dispatch("SAPI.SpVoice")
    stream = win32com.client.Dispatch("SAPI.SpFileStream")
    
    # 3 = SSFMCreateForWrite
    stream.Open(output_wav, 3, False)
    speaker.AudioOutputStream = stream
    speaker.Speak(text_to_speak)
    stream.Close()
    
    if os.path.exists(output_wav) and os.path.getsize(output_wav) > 1000:
        sapi_success = True
        print(f"✅ SAPI.SpVoice generated WAV audio successfully!")
        print(f"   File Path : {output_wav}")
        print(f"   File Size : {os.path.getsize(output_wav):,} bytes")
except Exception as e:
    print(f"⚠️ SAPI.SpVoice not available: {e}")

# Method 2: Check pyttsx3 fallback if SAPI didn't run
if not sapi_success:
    try:
        import importlib
        pyttsx3 = importlib.import_module("pyttsx3")
        engine = pyttsx3.init()
        engine.save_to_file(text_to_speak, output_wav)
        engine.runAndWait()
        if os.path.exists(output_wav) and os.path.getsize(output_wav) > 1000:
            sapi_success = True
            print(f"✅ pyttsx3 generated WAV audio successfully!")
            print(f"   File Path : {output_wav}")
            print(f"   File Size : {os.path.getsize(output_wav):,} bytes")
    except Exception as e:
        print(f"⚠️ pyttsx3 not available: {e}")

# Inspect WAV file properties
if os.path.exists(output_wav):
    try:
        with wave.open(output_wav, 'rb') as wf:
            channels = wf.getnchannels()
            sample_width = wf.getsampwidth()
            framerate = wf.getframerate()
            frames = wf.getnframes()
            duration = frames / float(framerate)
            
            print("\n📊 Generated Audio Properties:")
            print(f"   Duration      : {duration:.2f} seconds")
            print(f"   Sample Rate   : {framerate} Hz")
            print(f"   Channels      : {channels} ({'Stereo' if channels == 2 else 'Mono'})")
            print(f"   Sample Width  : {sample_width * 8}-bit")
            print(f"   Total Frames  : {frames:,}")
            print("\n✅ VOICE AUDIO GENERATION TEST PASSED!")
    except Exception as e:
        print(f"❌ Error reading WAV headers: {e}")
else:
    print("\n❌ VOICE AUDIO GENERATION FAILED (No WAV file created)")

print("=" * 60)
