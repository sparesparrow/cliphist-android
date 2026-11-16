#!/usr/bin/env bash
# One hands-free turn: beep, record from the phone mic until you stop talking, run
# assistant.sh (ElevenLabs STT -> Claude -> ElevenLabs TTS), play the reply on the phone speaker.
#
#   listen.sh [--new]             stdout: the reply text (one line); exit 3 if no speech was heard
#   listen.sh [--new] --wav FILE  same turn, but FILE stands in for the recording (latency tests)
#   listen.sh --transcribe-only [--wav FILE]   record + speech-to-text only (the panel's Dictate):
#                                 stdout: the transcript; no Claude, nothing is played back
#   exit 4 "busy" if another voice turn (HTTP trigger, MCP speak/listen) holds state/turn.lock
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
set -a; . "$HERE/audio.env"; set +a   # PULSE_*, AUDIODRIVER, VA_REC_SILENCE, VA_BEEP
mkdir -p "$OUT" "$HERE/logs" "$HERE/state"
beep() { play -q -n synth "$1" sine "$2" vol "${VA_BEEP##* }" 2>/dev/null || true; }
ms() { date +%s%3N; }
# Stage for the Tasker panel (GET /status): one word in state/phase, replaced atomically.
PHASE_FILE=${VA_PHASE_FILE:-$HERE/state/phase}
phase() { printf '%s\n' "$1" >"$PHASE_FILE.$$" && mv -f "$PHASE_FILE.$$" "$PHASE_FILE" || true; }
ARGS=() WAV="" STT_ONLY=0
while [ $# -gt 0 ]; do
  case $1 in
    --transcribe-only) STT_ONLY=1 ;;
    --wav) shift; WAV=${1:-}; [ -s "$WAV" ] || { echo "--wav: no such file: $WAV" >&2; exit 2; } ;;
    *) ARGS+=("$1") ;;
  esac
  shift
done

# One turn at a time. http_trigger.py already holds the lock for us (VA_TURN_LOCKED=1).
if [ -z "${VA_TURN_LOCKED:-}" ]; then
  exec 9>"${VA_LOCK_FILE:-$HERE/state/turn.lock}"
  flock -n 9 || { echo "busy" >&2; exit 4; }
  export VA_TURN_LOCKED=1
fi

/root/phone/phone-services ensure 9>&-   # daemons it starts must not inherit the lock
trap 'phase idle' EXIT
if [ -n "$WAV" ]; then
  IN=$WAV
else
  read -r b_s b_hz _ <<<"$VA_BEEP"; beep "$b_s" "$b_hz"
  phase listening
  rm -f "$IN"
  # $VA_REC_SILENCE (audio.env) ends the recording on a pause; cap at $MAX s.
  # shellcheck disable=SC2086
  rc=0; timeout $((WAIT + MAX)) rec -q -c1 -r16000 "$IN" $VA_REC_SILENCE trim 0 "$MAX" 2>"$HERE/logs/rec.err" || rc=$?
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
if [ "$STT_ONLY" = 1 ]; then
  phase transcribing
  rc=0; "$HERE/assistant.sh" --transcribe-only "$IN" || rc=$?
  wait; exit "$rc"
fi
"$HERE/assistant.sh" --speak "${ARGS[@]}" "$IN" >/dev/null
wait
sed -n 2p "$OUT/last.txt"
