import sys

print("Checking installed speech recognition modules in Python...")

try:
    import speech_recognition as sr
    print("✅ speech_recognition module found!")
except ImportError:
    print("⚠️ speech_recognition module NOT found.")

try:
    import pyaudio
    print("✅ pyaudio module found!")
except ImportError:
    print("⚠️ pyaudio module NOT found.")

try:
    import win32com.client
    rec = win32com.client.Dispatch("SAPI.SpSharedRecognizer")
    print("✅ Windows SAPI.SpSharedRecognizer found!")
except Exception as e:
    print(f"⚠️ SAPI recognizer check: {e}")
