#!/usr/bin/env bash
# Say one line on the phone speaker (ElevenLabs TTS + SoX through the mic server). Used by the
# panel's job watcher; the caller holds state/turn.lock (http_trigger.run_turn).
#   say.sh "text"
set -euo pipefail
VOICE=$(cd "$(dirname "$0")/../voice" && pwd)
set -a; . "$VOICE/audio.env"; set +a
ENV_FILE=${ELEVENLABS_ENV_FILE:-/root/mia/apps/rpi-backend/.env.local}
if [ -z "${ELEVENLABS_API_KEY:-}" ] && [ -f "$ENV_FILE" ]; then   # read, never printed
  ELEVENLABS_API_KEY=$(sed -n 's/^[[:space:]]*\(export[[:space:]]\+\)\?ELEVENLABS_API_KEY=//p' "$ENV_FILE" | tail -n1 | tr -d "\r\"'")
fi
[ -n "${ELEVENLABS_API_KEY:-}" ] || { echo "ELEVENLABS_API_KEY is not set" >&2; exit 1; }
export ELEVENLABS_API_KEY
[ -n "${1:-}" ] || exit 0
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
printf '%s' "${1:0:600}" | "$VOICE/eleven.py" tts "$tmp/say.mp3"
play -q "$tmp/say.mp3"
