# Voice pipeline

```
assistant gesture / Quick Settings tile / widget
  └─ Tasker task "Claude Listen": HTTP Request POST 127.0.0.1:8780/listen (X-Token)
       └─ voice/http_trigger.py ─► voice/listen.sh
            beep ─► rec (pulse 4713, until 1.3 s of silence) ─► voice/assistant.sh
              1. eleven.py stt            ElevenLabs scribe_v2
              2. claude -p --resume       prompt `phone-voice-assistant` (mcp-prompts),
                                          tools: android-assistant, mcp-prompts, cliphist, web
              3. eleven.py tts            ElevenLabs eleven_flash_v2_5 ─► mp3
            play mp3 on the phone speaker (pulse sink "speaker")
       ◄─ HTTP 200, body = reply text (Tasker: %http_data)
```

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
