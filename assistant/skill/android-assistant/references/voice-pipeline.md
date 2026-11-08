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
                                          tools: android-assistant, mcp-prompts, cliphist, web
              4. stream_speak.py          first complete sentence ─► eleven.py tts ─► play
                                          while Claude goes on; then the rest of the reply
            (fallback: if streamed TTS fails, the whole reply is synthesized and played)
       ◄─ HTTP 200, body = reply text (Tasker: %http_data)
```

One turn at a time: `http_trigger.py` and the MCP tools `speak`/`listen` share a flock on
`voice/state/turn.lock` (`voice/turnlock.py`); the tools answer "busy" instead of waiting.

Latency: each turn appends one line to `voice/logs/latency.log`: model, mode and
`t_rec_end t_stt_done t_first_token t_first_audio t_done` in ms since the recording ended
(`-` = did not happen). No transcript, no secrets. `listen.sh --wav FILE` runs a turn from a
file instead of the mic, for measuring.
Measured 2026-11-08, "What time is it?" WAV: time to first audio 64.1 s with the old pipeline
(sonnet, whole reply then TTS), 59.3 s with this one (haiku, streamed). About 50 s of either is
`claude -p` starting up before the first token; that is the next thing to cut.

Follow-ups within 10 minutes continue the same Claude session. That is how "shall I?" → "yes"
confirmations reach the same conversation. `?new=1` starts a fresh one.

## Tasker setup
1. Task **Claude Listen**, one action: Net ▸ HTTP Request.
   - Method POST. URL `http://127.0.0.1:8780/listen`.
   - Headers `X-Token:<contents of /root/voice-assistant/state/http-token>`. Timeout 300.
   - Optional: Alert ▸ Flash `%http_data`.
2. Assistant gesture: Tasker is the default digital assistant. To set it, run
   `settings put secure assistant` and `settings put secure voice_interaction_service` with
   `net.dinglisch.android.taskerm/com.joaomgcd.taskerm.assistant.ServiceVoiceInteractionTasker`.
   The previous values are in `/root/phone/logs/assistant-before.txt`. Add a profile on
   Event ▸ Tasker ▸ Assistant (the name differs by version) → Claude Listen.
3. Quick Settings: Tasker ▸ Preferences ▸ Action ▸ Quick Setting Tasks → Claude Listen.

## Later: realtime (phase 3)
An ElevenLabs Agent session from the Python SDK with a pulse AudioInterface. The agent's own
fast LLM handles small talk and barge-in. A client tool `ask_claude(request)` runs `claude -p`
locally for anything that touches the phone, home or car, so no public webhook is needed.
Costs agent minutes.
