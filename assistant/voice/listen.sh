#!/usr/bin/env bash
# One hands-free turn: beep, record from the phone mic until you stop talking, run
# assistant.sh (ElevenLabs STT -> Claude -> ElevenLabs TTS), play the reply on the phone speaker.
#
#   listen.sh [--new]             stdout: the reply text (one line); exit 3 if no speech was heard
#   listen.sh [--new] --wav FILE  same turn, but FILE stands in for the recording (latency tests)
#
# The reply starts playing as soon as Claude's first sentence is synthesized (assistant.sh
# --speak); per-turn timing goes to logs/latency.log.
# Audio goes through the mic server (/root/phone, PulseAudio as the adb shell user).
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=${VA_OUT_DIR:-/sdcard/VoiceAssistant}
IN=$OUT/listen.wav
MAX=${VA_LISTEN_MAX:-20}        # seconds of speech at most
WAIT=${VA_LISTEN_WAIT:-8}       # seconds to wait for speech to start
export PULSE_SERVER=127.0.0.1 PULSE_COOKIE=/root/phone/pa/cookie AUDIODRIVER=pulseaudio
mkdir -p "$OUT"
beep() { play -q -n synth "$1" sine "$2" vol 0.35 2>/dev/null || true; }
ms() { date +%s%3N; }
ARGS=() WAV=""
while [ $# -gt 0 ]; do
  case $1 in
    --wav) shift; WAV=${1:-}; [ -s "$WAV" ] || { echo "--wav: no such file: $WAV" >&2; exit 2; } ;;
    *) ARGS+=("$1") ;;
  esac
  shift
done

/root/phone/phone-services ensure
if [ -n "$WAV" ]; then
  IN=$WAV
else
  beep 0.12 880
  rm -f "$IN"
  # Start on sound above 2 %, stop after 1.3 s below 2 %, cap at $MAX s.
  rc=0; timeout $((WAIT + MAX)) rec -q -c1 -r16000 "$IN" silence 1 0.1 2% 1 1.3 2% trim 0 "$MAX" 2>"$HERE/logs/rec.err" || rc=$?
  if [ "$rc" != 0 ] && [ "$rc" != 124 ]; then   # 124 = timeout (no speech) is fine
    beep 0.4 220
    echo "recording failed: $(tail -1 "$HERE/logs/rec.err")" >&2; exit 1
  fi
  dur=$(soxi -D "$IN" 2>/dev/null || echo 0)
  if awk -v d="$dur" 'BEGIN{exit !(d < 0.4)}'; then
    beep 0.25 330
    echo "no speech heard"; exit 3
  fi
fi
export VA_T0_MS; VA_T0_MS=$(ms)   # recording ended: latency.log times count from here
beep 0.08 660 &                   # earcon "heard you", in parallel with speech-to-text
"$HERE/assistant.sh" --speak "${ARGS[@]}" "$IN" >/dev/null
wait
sed -n 2p "$OUT/last.txt"
