#!/root/mia-venv/bin/python
"""ElevenLabs speech-to-text and text-to-speech for the voice assistant (official Python SDK).

  eleven.py stt <audio-file>          -> JSON {"text": ..., "language_code": ...} on stdout
  eleven.py tts <out.mp3> < text      -> writes the mp3

The SDK reads ELEVENLABS_API_KEY from the environment. If it is not set, the key is loaded
from ELEVENLABS_ENV_FILE (default /root/mia/apps/rpi-backend/.env.local). Never printed.
"""
import json
import os
import sys

from dotenv import load_dotenv

if not os.environ.get("ELEVENLABS_API_KEY"):
    load_dotenv(os.environ.get("ELEVENLABS_ENV_FILE", "/root/mia/apps/rpi-backend/.env.local"))

from elevenlabs import ElevenLabs  # noqa: E402  (after the key is loaded)

STT_MODEL = os.environ.get("ELEVENLABS_STT_MODEL", "scribe_v2")
TTS_MODEL = os.environ.get("ELEVENLABS_TTS_MODEL", "eleven_flash_v2_5")
VOICE_ID = os.environ.get("ELEVENLABS_VOICE_ID", "JBFqnCBsd6RMkjVDRZzb")


def stt(client, path):
    with open(path, "rb") as f:
        r = client.speech_to_text.convert(file=f, model_id=STT_MODEL, tag_audio_events=False)
    print(json.dumps({"text": (r.text or "").strip(), "language_code": r.language_code}, ensure_ascii=False))


def tts(client, out):
    text = sys.stdin.read().strip()
    if not text:
        sys.exit("tts: empty text")
    audio = client.text_to_speech.convert(
        text=text, voice_id=VOICE_ID, model_id=TTS_MODEL, output_format="mp3_44100_128"
    )
    tmp = out + ".part"
    with open(tmp, "wb") as f:
        for chunk in audio:
            f.write(chunk)
    os.replace(tmp, out)


def main():
    if len(sys.argv) != 3 or sys.argv[1] not in ("stt", "tts"):
        sys.exit(__doc__)
    client = ElevenLabs()
    try:
        (stt if sys.argv[1] == "stt" else tts)(client, sys.argv[2])
    except Exception as e:  # API errors: one line on stderr, no key material
        status = getattr(e, "status_code", None)
        body = getattr(e, "body", None)
        detail = json.dumps(body, ensure_ascii=False)[:400] if body is not None else str(e)[:300]
        sys.exit(f"{sys.argv[1]} failed: {type(e).__name__} status={status}: {detail}")


if __name__ == "__main__":
    main()
