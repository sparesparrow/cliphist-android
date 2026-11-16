#!/bin/bash
# Store the ElevenLabs API key for the voice assistant without echoing it.
# Run in a terminal (not via Claude):   debian -- /root/phone/set-elevenlabs-key.sh
set -eu
F=/root/mia/apps/rpi-backend/.env.local
read -rsp "Paste ElevenLabs API key (hidden), then Enter: " K; echo
K=$(printf '%s' "$K" | tr -d '[:space:]')
[ -n "$K" ] || { echo "empty, nothing saved"; exit 1; }
touch "$F"; chmod 600 "$F"
sed -i '/^[[:space:]]*\(export[[:space:]]\+\)\?ELEVENLABS_API_KEY=/d' "$F"
printf 'ELEVENLABS_API_KEY=%s\n' "$K" >> "$F"
echo "saved (${#K} characters) to $F"
