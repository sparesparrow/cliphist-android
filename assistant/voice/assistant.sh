#!/usr/bin/env bash
# Phone voice assistant: speech file -> ElevenLabs STT -> Claude Code -> ElevenLabs TTS -> mp3.
#
#   assistant.sh <audio-file>        # e.g. the m4a Tasker recorded
#   assistant.sh --text "question"   # skip STT (testing)
#   assistant.sh --new ...           # start a new conversation instead of continuing
#
# stdout: exactly one line, the path of the reply mp3 (Termux:Tasker passes it back as %stdout).
# Everything else goes to logs/assistant.log and stderr.
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ENV_FILE=${ELEVENLABS_ENV_FILE:-/root/mia/apps/rpi-backend/.env.local}
OUT_DIR=${VA_OUT_DIR:-/sdcard/VoiceAssistant}
VOICE_ID=${ELEVENLABS_VOICE_ID:-JBFqnCBsd6RMkjVDRZzb}
TTS_MODEL=${ELEVENLABS_TTS_MODEL:-eleven_flash_v2_5}
STT_MODEL=${ELEVENLABS_STT_MODEL:-scribe_v2}
CLAUDE_MODEL=${VA_CLAUDE_MODEL:-sonnet}
PROMPT_FILE=${VA_PROMPT_FILE:-/root/mcp-prompts/data/prompts/voice-intelligence/phone-voice-assistant.json}
SESSION_TTL=${VA_SESSION_TTL:-600}   # seconds a conversation stays open for follow-ups
API=https://api.elevenlabs.io/v1

mkdir -p "$HERE/logs" "$HERE/state" "$OUT_DIR"
LOG=$HERE/logs/assistant.log
SESSION_FILE=$HERE/state/session
log() { printf '%s %s\n' "$(date '+%F %T')" "$*" >>"$LOG"; printf '%s\n' "$*" >&2; }
die() { log "ERROR: $*"; exit 1; }

export PATH="$HOME/.local/bin:$PATH"

# --- args -------------------------------------------------------------------
NEW=0 TEXT="" AUDIO=""
while [ $# -gt 0 ]; do
  case $1 in
    --new) NEW=1 ;;
    --text) shift; TEXT=${1:-} ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) AUDIO=$1 ;;
  esac
  shift
done
[ -n "$TEXT" ] || [ -n "$AUDIO" ] || die "usage: assistant.sh <audio-file> | --text \"...\""

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
fi

# --- 2. Claude ----------------------------------------------------------------
# No Bash: phone actions go through the android-assistant MCP server, which has no raw shell
# and asks for spoken confirmation before Tasker tasks and non-light MQTT topics.
ALLOWED_TOOLS=(
  mcp__android-assistant__phone_status mcp__android-assistant__screen mcp__android-assistant__act
  mcp__android-assistant__open mcp__android-assistant__notify
  mcp__android-assistant__run_tasker_task mcp__android-assistant__mqtt_publish
  mcp__mcp-prompts__list_prompts mcp__mcp-prompts__get_prompt mcp__mcp-prompts__apply_template
  mcp__cliphist-android__search_clipboard mcp__cliphist-android__get_clipboard_history
  WebSearch WebFetch
)
ask_claude() {  # $1 = resume session id or empty; prints claude's JSON
  local sys now args=()
  now=$(date '+%A %-d %B %Y, %H:%M')
  sys=$(jq -r '.content' "$PROMPT_FILE")
  sys=${sys//\{\{current_time\}\}/$now}
  [ -n "$1" ] && args+=(--resume "$1")
  (cd /root && claude -p "$TEXT" "${args[@]}" \
      --model "$CLAUDE_MODEL" --output-format json \
      --append-system-prompt "$sys" \
      --allowedTools "${ALLOWED_TOOLS[@]}" \
      --disallowedTools Bash Edit Write NotebookEdit \
      </dev/null 2>>"$LOG")
}

if [ -z "$TEXT" ]; then
  REPLY="Sorry, I didn't catch that."
else
  sid=""
  if [ "$NEW" = 0 ] && [ -f "$SESSION_FILE" ] && \
     [ $(( $(date +%s) - $(stat -c %Y "$SESSION_FILE") )) -lt "$SESSION_TTL" ]; then
    sid=$(cat "$SESSION_FILE")
  fi
  out=$(ask_claude "$sid") || { [ -n "$sid" ] && log "resume failed, starting new session" && out=$(ask_claude ""); } || out=""
  REPLY=$(jq -r 'select(.is_error != true) | .result // empty' <<<"$out" 2>/dev/null || true)
  new_sid=$(jq -r '.session_id // empty' <<<"$out" 2>/dev/null || true)
  [ -n "$new_sid" ] && printf '%s' "$new_sid" >"$SESSION_FILE"
  if [ -z "$REPLY" ]; then
    log "claude failed: $(head -c 500 <<<"$out")"
    REPLY="Sorry, I couldn't get an answer from Claude."
  fi
fi
# Strip markdown that would be read out loud.
REPLY=$(sed -E 's/[*_`#>|]+//g; s/^[[:space:]]*-[[:space:]]+//; s/\[([^]]*)\]\([^)]*\)/\1/g' <<<"$REPLY")
log "reply: $REPLY"

# --- 3. text to speech -------------------------------------------------------
MP3="$OUT_DIR/reply-$(date +%Y%m%d-%H%M%S).mp3"
printf '%s' "$REPLY" | "$ELEVEN" tts "$MP3" 2>>"$LOG" || { rm -f "$MP3"; die "text-to-speech failed (see log)"; }

printf '%s\n%s\n' "$TEXT" "$REPLY" >"$OUT_DIR/last.txt"
ls -1t "$OUT_DIR"/reply-*.mp3 2>/dev/null | tail -n +11 | xargs -r rm -f   # keep the last 10
echo "$MP3"
