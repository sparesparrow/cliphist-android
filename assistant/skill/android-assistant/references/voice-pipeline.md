# Voice pipeline

```
assistant gesture / Quick Settings tile / widget
  └─ Tasker task "Claude Listen": HTTP Request POST 127.0.0.1:8780/listen (X-Token)
       └─ voice/http_trigger.py ─► voice/listen.sh
            beep ─► rec (pulse 4713, until 1.3 s of silence) ─► earcon ─► voice/assistant.sh --speak
              1. eleven.py stt            ElevenLabs scribe_v2
              2. route.py                 model: $VA_CLAUDE_MODEL, else haiku for a short
                                          question (<= 8 words, no action verb), else sonnet
              3. claude -p --resume --output-format stream-json --verbose --include-partial-messages
                                          prompt `phone-voice-assistant` (mcp-prompts),
                                          tools: android-assistant (not speak/listen),
                                          mcp-prompts, WebSearch, WebFetch
              4. stream_speak.py          first complete sentence ─► eleven.py tts ─► play
                                          while Claude goes on; then the rest of the reply
            (fallback: if streamed TTS fails, the whole reply is synthesized and played)
       ◄─ HTTP 200, body = reply text (Tasker: %http_data)
```

One turn at a time: `http_trigger.py`, `listen.sh`, `assistant.sh --speak` and the MCP tools
`speak`/`listen` share a flock on `voice/state/turn.lock` (`voice/turnlock.py`); the tools
answer "busy" instead of waiting. Audio settings (pulse env, silence trim, beep) live in
`voice/audio.env`, read by the scripts and by the MCP server.

Isolation: the turn's `claude -p` gets `--permission-mode dontAsk` (every tool not in
`ALLOWED_TOOLS` is denied, whatever the user's default mode is), `--strict-mcp-config
--mcp-config voice/mcp.json` (only android-assistant and mcp-prompts; no claude.ai connectors,
no other servers), `--setting-sources project,local` (skips user settings and plugins) and
`--tools WebSearch,WebFetch` (no other built-in tools). The init event's servers and tools go
to `logs/assistant.log` (`stream_speak: init`). Clipboard history is not wired in yet.

Latency: each turn appends one line to `voice/logs/latency.log`, also when it fails:
`outcome` (ok, tts_fallback, claude_failed, error), model, mode and
`t_rec_end t_stt_done t_first_token t_first_audio t_done` in ms since the recording ended
(`-` = did not happen). `t_first_token` is Claude's first text token, which comes after any
tool calls; `t_first_audio` is when the first player process started. No transcript, no
secrets. `listen.sh --wav FILE` runs a turn from a file instead of the mic, for measuring.
Measured 2026-11-08 with a 1.1 s "What time is it?" WAV, time to first audio:
64.1 s old pipeline (sonnet, whole reply then TTS); 59.3 s streamed with haiku; 38.9 s with the
isolation flags above (fewer servers, plugins and tools to load). About 30 s of that is still
`claude -p` starting up before the first token.

If streamed TTS or playback fails, `assistant.sh` synthesizes only the part nobody heard. It
runs Claude a second time only when Claude itself failed on a resumed session and no audio has
played, so tool actions are never repeated. One turn at a time also from the shell:
`listen.sh` and `assistant.sh --speak` take the same lock (exit 4 "busy") unless their caller
holds it (`VA_TURN_LOCKED=1`, set by `http_trigger.py`). On timeout the trigger kills the
turn's whole process group.

Follow-ups within 10 minutes continue the same Claude session. That is how "shall I?" → "yes"
confirmations reach the same conversation. `?new=1` starts a fresh one.

## Tasker setup
Tasker is the default digital assistant: `settings put secure assistant` and
`settings put secure voice_interaction_service` with
`net.dinglisch.android.taskerm/com.joaomgcd.taskerm.assistant.ServiceVoiceInteractionTasker`.
The previous values are in `/root/phone/logs/assistant-before.txt`.

Import the profile with `assistant/phone/tasker/import-claude-listen.sh`, then tap YES twice
on Tasker's own "Import Data" dialog. The script writes the XML (it holds the token), starts
Tasker's `ActivityImportTaskerDataFromXml` on it, waits for `autobackup.xml` to change, and
deletes the file. What gets imported:
- Profile **Claude Listen**: Event 201 "Assistance Request". Long-press power fires it
  (`mLongPressOnPowerBehavior=5`).
- Task **Claude Listen**: one JavaScriptlet that does `shell("printf 'POST /listen …X-Token…' | nc -q 1 127.0.0.1 8780")`.
  It fires and forgets, so Tasker needs no HTTP Request action and no plugins. The trigger
  ignores the early hang-up.

Gotchas:
- Opening a `.prf.xml` through `IntentHandler` with `file://` only copies it into `Tasker/profiles`.
  It does not import it.
- `input keyevent KEYCODE_ASSIST` is dropped when the press is under 150 ms. To test, use
  `input keyevent --duration 900 KEYCODE_POWER`; with behavior 5 that opens the assistant.
  Event 2093 "assistant_action" is the Google App Actions path, not the gesture.
- Other profiles on event 201 (old ChatGPT projects) fire on the same gesture. Disable them in Tasker.
- Profiles that need AutoInput, AutoNotification, AutoApps or AutoCast log "non-existing plugin
  code" warnings when those apps are missing. The warnings don't affect Claude Listen.

Quick Settings (optional): Tasker ▸ Preferences ▸ Action ▸ Quick Setting Tasks → Claude Listen.

## Claude Panel router (mcp-prompts → GitHub @claude)
Scene **Claude Panel** (`phone/tasker/build-claude-panel.py`, imported by
`import-claude-panel.sh`) is a frontend to mcp-prompts. Dictate → Template ▾ → Repo ▾ → Send.
The trigger endpoints are `/dictate`, `/templates`, `/repos`, `/draft` and `/dispatch`;
`/status` also carries `draft`, `dispatch` and `jobs`.

- The code is in `assistant/router/`: `prompts.py` (JSON-RPC over stdio to `mcp-prompts`,
  falling back to the JSON files), `router.py`, `repos.json`, `claude-workflow.yml`,
  `setup-github-claude.sh` and `say.sh`.
- The templates are in mcp-prompts `data/prompts/panel/`. Tasker XML knowledge is in
  `cross-platform/tasker-scene-xml`.
- The router prompt is `panel-router`, run with `claude -p --model haiku --tools ""`. Startup
  takes 30–95 s here, so the timeout is 150 s (`PANEL_ROUTE_TIMEOUT`).
- Nothing reaches GitHub unless the repo already has `.github/workflows/claude.yml`.
- Writing that file needs the gh token's `workflow` scope.
- The Tasker dropdowns use List Dialog (378): a JS action hides the scene and puts the items
  in `%cp_items`, then the List Dialog runs, then JS posts `%ld_selected` to `/draft` and shows
  the scene again. A blocking overlay cancels dialogs, so the scene has to be hidden first.

## Later: realtime (phase 3)
An ElevenLabs Agent session from the Python SDK with a pulse AudioInterface. The agent's own
fast LLM handles small talk and barge-in. A client tool `ask_claude(request)` runs `claude -p`
locally for anything that touches the phone, home or car, so no public webhook is needed.
Costs agent minutes.
