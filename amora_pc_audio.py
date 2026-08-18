import sys
import time
import socket
import numpy as np

try:
    import pyaudiowpatch as pyaudio
except ImportError:
    print("[AMORA] Error: 'pyaudiowpatch' module not found.")
    print("[AMORA] Install it using: pip install pyaudiowpatch")
    sys.exit(1)

# Pycaw for mute/unmute of laptop speaker
try:
    from ctypes import cast, POINTER
    from comtypes import CLSCTX_ALL
    from pycaw.pycaw import AudioUtilities, IAudioEndpointVolume
    HAS_PYCAW = True
except Exception:
    HAS_PYCAW = False

PAIR_PORT = 5149       # Handshake port
AUDIO_PORT = 5150      # Streaming UDP port
SAMPLE_RATE = 48000
CHANNELS = 2
CHUNK_SIZE = 480       # 10 ms at 48 kHz

def set_laptop_mute(muted: bool = True):
    """Mutes or unmutes default laptop speakers."""
    if HAS_PYCAW:
        try:
            devices = AudioUtilities.GetSpeakers()
            interface = devices.Activate(IAudioEndpointVolume._iid_, CLSCTX_ALL, None)
            volume = cast(interface, POINTER(IAudioEndpointVolume))
            volume.SetMute(1 if muted else 0, None)
            return
        except Exception:
            pass

    try:
        import ctypes
        VK_VOLUME_MUTE = 0xAD
        KEYEVENTF_KEYUP = 0x0002
        ctypes.windll.user32.keybd_event(VK_VOLUME_MUTE, 0, 0, 0)
        ctypes.windll.user32.keybd_event(VK_VOLUME_MUTE, 0, KEYEVENTF_KEYUP, 0)
    except Exception:
        pass

def discover_wasapi_loopback_device(p):
    """Find default system output speakers WASAPI loopback device."""
    try:
        wasapi_info = p.get_host_api_info_by_type(pyaudio.paWASAPI)
        default_speakers = p.get_device_info_by_index(wasapi_info["defaultOutputDevice"])
        
        # Match loopback device corresponding to default speakers
        for dev in p.get_device_info_generator():
            if dev.get("isLoopbackDevice") and default_speakers["name"] in dev["name"]:
                return dev
        
        # Fallback to any active WASAPI loopback device
        for dev in p.get_device_info_generator():
            if dev.get("isLoopbackDevice"):
                return dev

        return default_speakers
    except Exception as e:
        print(f"[AMORA] Error discovering WASAPI loopback device: {e}")
        return None

def pair_with_phone(phone_ip: str, pair_code: str) -> bool:
    print(f"\n[AMORA] 🔗 Pairing with {phone_ip}:{PAIR_PORT} (Code: {pair_code})...")
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(2.0)

    msg = f"AMORA_HELLO:{pair_code}".encode()

    for attempt in range(1, 4):
        try:
            print(f"[AMORA]    → Sent HELLO (attempt {attempt}/3)")
            sock.sendto(msg, (phone_ip, PAIR_PORT))
            data, addr = sock.recvfrom(1024)
            resp = data.decode(errors="ignore").strip()
            if resp.startswith("AMORA_ACK"):
                print(f"[AMORA] ✅ Paired successfully with {phone_ip}!")
                sock.close()
                return True
        except socket.timeout:
            pass
        except Exception as e:
            print(f"[AMORA]    Pairing attempt error: {e}")

    print(f"[AMORA] ⚠️ No reply from phone. Proceeding to stream anyway...")
    sock.close()
    return True

def stream_audio(phone_ip: str, loopback_device):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 16384)

    p = pyaudio.PyAudio()
    device_index = loopback_device["index"]
    device_name = loopback_device["name"]
    sample_rate = int(loopback_device.get("defaultSampleRate", SAMPLE_RATE))
    # Use the loopback device's actual channel count instead of a hardcoded 2 —
    # some outputs (e.g. 5.1 speakers) aren't stereo, which would otherwise
    # make PyAudio fail to open the stream or produce garbled/silent audio.
    channels = int(loopback_device.get("maxInputChannels", CHANNELS)) or CHANNELS

    if channels != CHANNELS:
        print(f"[AMORA] ⚠️  Loopback device reports {channels} channel(s), but the phone expects "
              f"stereo (2ch). Audio may sound wrong — try setting your PC's default output to a "
              f"stereo device in Windows Sound Settings.")

    print(f"\n[AMORA] 🔊 Starting System Audio Loopback Stream")
    print(f"[AMORA]    Device     : {device_name} (Index: {device_index})")
    print(f"[AMORA]    Target     : {phone_ip}:{AUDIO_PORT}")
    print(f"[AMORA]    Format     : {sample_rate} Hz · {channels}ch · int16")
    print(f"[AMORA]    Latency    : ~10 ms (Real-Time)")
    print(f"[AMORA]    Laptop     : 🔇 Muted (Sound plays from phone only)")
    print(f"[AMORA]    Press Ctrl+C to stop.\n")

    set_laptop_mute(True)

    try:
        stream = p.open(
            format=pyaudio.paInt16,
            channels=channels,
            rate=sample_rate,
            input=True,
            input_device_index=device_index,
            frames_per_buffer=CHUNK_SIZE
        )

        print(f"[AMORA] ⚡ Streaming live PC system audio to phone {phone_ip}... (Ctrl+C to stop)")
        packet_count = 0

        while True:
            raw_pcm = stream.read(CHUNK_SIZE, exception_on_overflow=False)
            if raw_pcm:
                sock.sendto(raw_pcm, (phone_ip, AUDIO_PORT))
                packet_count += 1

    except KeyboardInterrupt:
        print("\n[AMORA] 🛑 Stopping PC audio stream...")
    except Exception as e:
        print(f"\n[AMORA] Stream error: {e}")
    finally:
        try:
            stream.stop_stream()
            stream.close()
            p.terminate()
            sock.close()
        except Exception:
            pass
        print("[AMORA] 🔊 Restoring laptop speakers sound...")
        set_laptop_mute(False)
        print("[AMORA] Done.")

def main():
    if len(sys.argv) < 3:
        print("\n[AMORA PC Speaker Audio Sender]")
        print("Usage: python amora_pc_audio.py <PHONE_IP> <PAIR_CODE>")
        print("Example: python amora_pc_audio.py 10.205.103.141 4625\n")
        sys.exit(1)

    phone_ip = sys.argv[1]
    pair_code = sys.argv[2]

    p = pyaudio.PyAudio()
    loopback_device = discover_wasapi_loopback_device(p)
    
    if not loopback_device:
        print("[AMORA] Error: No WASAPI system audio loopback device found.")
        sys.exit(1)

    print(f"[AMORA] 🎧 Selected Loopback Device: {loopback_device['name']} (Index: {loopback_device['index']})")

    pair_with_phone(phone_ip, pair_code)
    stream_audio(phone_ip, loopback_device)

if __name__ == "__main__":
    main()
