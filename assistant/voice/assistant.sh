#!/usr/bin/env bash
# Phone voice assistant: speech file -> ElevenLabs STT -> Claude Code -> ElevenLabs TTS -> mp3.
#
#   assistant.sh <audio-file>        # e.g. the m4a Tasker recorded
#   assistant.sh --text "question"   # skip STT (testing)
#   assistant.sh --new ...           # start a new conversation instead of continuing
#   assistant.sh --speak ...         # play the reply on the phone speaker while it streams in:
#                                    # the first sentence as soon as Claude has written it
#
# stdout: exactly one line, the path of the reply mp3 (Termux:Tasker passes it back as %stdout;
# with --speak, the last part played). Everything else goes to logs/assistant.log and stderr.
# Timing per turn goes to logs/latency.log (ms since VA_T0_MS, the end of the recording; no text).
# --speak takes state/turn.lock unless the caller holds it (VA_TURN_LOCKED=1); exit 4 = busy.
#
# Claude runs isolated: --permission-mode dontAsk denies every tool not in ALLOWED_TOOLS, and
# --strict-mcp-config loads only mcp.json (android-assistant, mcp-prompts), not the user's
# claude.ai connectors or plugins; --setting-sources project,local skips user settings;
# --tools leaves only WebSearch and WebFetch of the built-in tools.
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ENV_FILE=${ELEVENLABS_ENV_FILE:-/root/mia/apps/rpi-backend/.env.local}
OUT_DIR=${VA_OUT_DIR:-/sdcard/VoiceAssistant}
VOICE_ID=${ELEVENLABS_VOICE_ID:-JBFqnCBsd6RMkjVDRZzb}
TTS_MODEL=${ELEVENLABS_TTS_MODEL:-eleven_flash_v2_5}
STT_MODEL=${ELEVENLABS_STT_MODEL:-scribe_v2}
PROMPT_FILE=${VA_PROMPT_FILE:-/root/mcp-prompts/data/prompts/voice-intelligence/phone-voice-assistant.json}
SESSION_TTL=${VA_SESSION_TTL:-600}   # seconds a conversation stays open for follow-ups
API=https://api.elevenlabs.io/v1

mkdir -p "$HERE/logs" "$HERE/state" "$OUT_DIR"
LOG=$HERE/logs/assistant.log
SESSION_FILE=$HERE/state/session
log() { printf '%s %s\n' "$(date '+%F %T')" "$*" >>"$LOG"; printf '%s\n' "$*" >&2; }

export PATH="$HOME/.local/bin:$PATH"
PY=/root/mia-venv/bin/python
ms() { date +%s%3N; }
T0=${VA_T0_MS:-$(ms)}   # listen.sh passes the time the recording ended
T_STT="" T_AUDIO="" CLAUDE_MODEL="" SPEAK=0 out=""
TMP=$(mktemp -d); trap 'rm -rf "$TMP"; [ "$SPEAK" = 1 ] && phase idle; true' EXIT
# Stage for the Tasker panel (GET /status): one word in state/phase, replaced atomically.
PHASE_FILE=${VA_PHASE_FILE:-$HERE/state/phase}
phase() { printf '%s\n' "$1" >"$PHASE_FILE.$$" && mv -f "$PHASE_FILE.$$" "$PHASE_FILE" || true; }
# One line per turn in logs/latency.log, also on failure. No transcript, no secrets.
latency() {  # $1 = outcome: ok | tts_fallback | claude_failed | error
  local tok rel_t
  tok=$(jq -r '.t_first_token // empty' <<<"$out" 2>/dev/null || true)
  rel_t() { [ -n "$1" ] && echo $(( $1 - T0 )) || echo -; }
  printf '%s outcome=%s model=%s mode=%s t_rec_end=0 t_stt_done=%s t_first_token=%s t_first_audio=%s t_done=%s\n' \
    "$(date '+%F %T')" "$1" "${CLAUDE_MODEL:--}" "$([ "$SPEAK" = 1 ] && echo speak || echo mp3)" \
    "$(rel_t "$T_STT")" "${tok:--}" "${T_AUDIO:--}" "$(rel_t "$(ms)")" >>"$HERE/logs/latency.log"
}
die() { log "ERROR: $*"; latency error; exit 1; }

# --- args -------------------------------------------------------------------
NEW=0 TEXT="" AUDIO=""
while [ $# -gt 0 ]; do
  case $1 in
    --new) NEW=1 ;;
    --text) shift; TEXT=${1:-} ;;
    --speak) SPEAK=1 ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) AUDIO=$1 ;;
  esac
  shift
done
[ -n "$TEXT" ] || [ -n "$AUDIO" ] || die "usage: assistant.sh <audio-file> | --text \"...\""
if [ "$SPEAK" = 1 ]; then
  set -a; . "$HERE/audio.env"; set +a   # PULSE_SERVER, PULSE_COOKIE, AUDIODRIVER
  if [ -z "${VA_TURN_LOCKED:-}" ]; then  # one voice turn at a time (http_trigger, listen.sh hold it)
    exec 9>"${VA_LOCK_FILE:-$HERE/state/turn.lock}"
    flock -n 9 || { echo "busy" >&2; exit 4; }
    export VA_TURN_LOCKED=1
  fi
  phase thinking
fi

# --- key (read from the env file, never printed) ----------------------------
if [ -z "${ELEVENLABS_API_KEY:-}" ] && [ -f "$ENV_FILE" ]; then
  ELEVENLABS_API_KEY=$(sed -n 's/^[[:space:]]*\(export[[:space:]]\+\)\?ELEVENLABS_API_KEY=//p' "$ENV_FILE" | tail -n1 | tr -d "\r\"'")
fi
[ -n "${ELEVENLABS_API_KEY:-}" ] || die "ELEVENLABS_API_KEY is not set (looked in $ENV_FILE)"
export ELEVENLABS_API_KEY ELEVENLABS_STT_MODEL="$STT_MODEL" ELEVENLABS_TTS_MODEL="$TTS_MODEL" ELEVENLABS_VOICE_ID="$VOICE_ID"
ELEVEN="$HERE/eleven.py"   # ElevenLabs SDK: speech-to-text and text-to-speech

# --- 1. speech to text ------------------------------------------------------
if [ -z "$TEXT" ]; then
  [ -s "$AUDIO" ] || die "audio file missing or empty: $AUDIO"
  stt=$("$ELEVEN" stt "$AUDIO" 2>>"$LOG") || die "speech-to-text failed (see log)"
  TEXT=$(jq -r '.text // empty' <<<"$stt" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  log "heard [$(jq -r '.language_code // "?"' <<<"$stt")]: $TEXT"
  T_STT=$(ms)
fi
CLAUDE_MODEL=$("$PY" -I "$HERE/route.py" "$TEXT")   # VA_CLAUDE_MODEL wins; short question -> haiku

# --- 2. Claude ----------------------------------------------------------------
# No Bash: phone actions go through the android-assistant MCP server, which has no raw shell
# and asks for spoken confirmation before Tasker tasks and non-light MQTT topics.
ALLOWED_TOOLS=(
  mcp__android-assistant__phone_status mcp__android-assistant__screen mcp__android-assistant__act
  mcp__android-assistant__open mcp__android-assistant__notify
  mcp__android-assistant__run_tasker_task mcp__android-assistant__mqtt_publish
  mcp__mcp-prompts__list_prompts mcp__mcp-prompts__get_prompt mcp__mcp-prompts__apply_template
  WebSearch WebFetch
)
ask_claude() {  # $1 = resume session id or empty; summary JSON -> $TMP/summary, rc -> $TMP/claude.rc
  local sys now args=() ss=(--t0-ms "$T0" --out-dir "$OUT_DIR")
  [ "$SPEAK" = 1 ] && ss+=(--play)
  now=$(date '+%A %-d %B %Y, %H:%M')
  sys=$(jq -r '.content' "$PROMPT_FILE")
  sys=${sys//\{\{current_time\}\}/$now}
  [ -n "$1" ] && args+=(--resume "$1")
  { rc=0; (cd /root && claude -p "$TEXT" "${args[@]}" \
      --model "$CLAUDE_MODEL" --output-format stream-json --verbose --include-partial-messages \
      --permission-mode dontAsk --strict-mcp-config --mcp-config "$HERE/mcp.json" \
      --setting-sources project,local --tools "WebSearch,WebFetch" \
      --append-system-prompt "$sys" \
      --allowedTools "${ALLOWED_TOOLS[@]}" \
      --disallowedTools Bash Edit Write NotebookEdit \
      </dev/null 2>>"$LOG") || rc=$?; echo "$rc" >"$TMP/claude.rc"; } \
    | "$PY" -I "$HERE/stream_speak.py" "${ss[@]}" >"$TMP/summary" 2>>"$LOG" || true
  out=$(cat "$TMP/summary" 2>/dev/null || true)
  CLAUDE_RC=$(cat "$TMP/claude.rc" 2>/dev/null || echo 1)
}
heard() { [ -n "$(jq -r '.t_first_audio // empty' <<<"$out" 2>/dev/null)" ]; }

OUTCOME=ok
if [ -z "$TEXT" ]; then
  REPLY="Sorry, I didn't catch that."
else
  sid=""
  if [ "$NEW" = 0 ] && [ -f "$SESSION_FILE" ] && \
     [ $(( $(date +%s) - $(stat -c %Y "$SESSION_FILE") )) -lt "$SESSION_TTL" ]; then
    sid=$(cat "$SESSION_FILE")
  fi
  ask_claude "$sid"
  # Run Claude again only if Claude itself failed on a resumed session and nobody heard
  # anything yet: never because of TTS or playback, never twice after audio (no repeated actions).
  if [ "$CLAUDE_RC" != 0 ] && [ -n "$sid" ] && ! heard; then
    log "resume failed (rc=$CLAUDE_RC), starting new session"
    ask_claude ""
  fi
  REPLY=$(jq -r 'select(.is_error != true) | .result // empty' <<<"$out" 2>/dev/null || true)
  new_sid=$(jq -r '.session_id // empty' <<<"$out" 2>/dev/null || true)
  [ -n "$new_sid" ] && printf '%s' "$new_sid" >"$SESSION_FILE"
  if [ -z "$REPLY" ]; then
    log "claude failed (rc=$CLAUDE_RC): $(head -c 500 <<<"$out")"
    REPLY="Sorry, I couldn't get an answer from Claude."
    OUTCOME=claude_failed
  fi
fi
# Strip markdown that would be read out loud.
# (stream_speak.py's clean() is a Python port of this sed, for the parts it speaks early.)
REPLY=$(sed -E 's/[*_`#>|]+//g; s/^[[:space:]]*-[[:space:]]+//; s/\[([^]]*)\]\([^)]*\)/\1/g' <<<"$REPLY")
log "reply: $REPLY"

# --- 3. text to speech -------------------------------------------------------
# With --speak, stream_speak.py has played what it could; synthesize only what nobody heard
# (all of it on a Claude failure). Without --speak, the whole reply becomes one mp3 as before.
T_AUDIO=$(jq -r '.t_first_audio // empty' <<<"$out" 2>/dev/null || true)
SAY=$REPLY MP3=""
if [ "$SPEAK" = 1 ] && [ "$OUTCOME" = ok ]; then
  SAY=$(jq -r '.unspoken // empty' <<<"$out" 2>/dev/null || true)
  MP3=$(jq -r '.last_mp3 // empty' <<<"$out" 2>/dev/null || true)
  [ -z "$SAY" ] || OUTCOME=tts_fallback
fi
if [ -n "$SAY" ]; then
  MP3="$OUT_DIR/reply-$(date +%Y%m%d-%H%M%S).mp3"
  printf '%s' "$SAY" | "$ELEVEN" tts "$MP3" 2>>"$LOG" || { rm -f "$MP3"; die "text-to-speech failed (see log)"; }
  if [ "$SPEAK" = 1 ]; then
    [ -n "$T_AUDIO" ] || T_AUDIO=$(( $(ms) - T0 ))   # as play starts
    phase speaking
    play -q "$MP3" 2>>"$LOG" || log "playback failed: $MP3"
  fi
fi
latency "$OUTCOME"

printf '%s\n%s\n' "$TEXT" "$REPLY" >"$OUT_DIR/last.txt"
ls -1t "$OUT_DIR"/reply-*.mp3 2>/dev/null | tail -n +11 | xargs -r rm -f   # keep the last 10
echo "$MP3"
