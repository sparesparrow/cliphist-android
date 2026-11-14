#!/bin/bash
# Import the Tasker project "Claude Assistant": scene "Claude Panel" (status, what you said,
# Claude's reply; Talk / New / Stop / Type / close), its tasks, and profile "Claude Panel
# Gesture" (Assistance Request -> open the panel; a second press while a turn runs stops it).
# Listening on the gesture stays with profile "Claude Listen" (import-claude-listen.sh).
# The generated XML holds the trigger token: it lives in a mode-600 temp file and in
# /sdcard/Download only until Tasker has saved it. Prints no token.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=tasker-import.sh
. "$HERE/tasker-import.sh"
TOKEN_FILE=${TOKEN_FILE:-/root/voice-assistant/state/http-token}

tmpd=$(mktemp -d); chmod 700 "$tmpd"
cleanup_local() { rm -rf "$tmpd"; }
trap 'cleanup_local; _tasker_adb shell "rm -f ${TASKER_IMPORT_DEST:-/nonexistent}" >/dev/null 2>&1 || true' EXIT
python3 -I "$HERE/build-claude-panel.py" "$tmpd/ClaudeAssistant.prj.xml" --token-file "$TOKEN_FILE"
tasker_import "$tmpd/ClaudeAssistant.prj.xml" ClaudeAssistant.prj.xml
trap - EXIT
cleanup_local
echo "Open the panel: long-press power, or run task 'CP Open' (Quick Settings tile / MCP run_tasker_task)."
