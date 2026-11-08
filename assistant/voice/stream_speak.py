#!/root/mia-venv/bin/python
"""Read `claude -p --output-format stream-json --verbose --include-partial-messages` on stdin
and, with --play, speak the first complete sentence on the phone speaker as soon as it has
streamed in; when the final result arrives, speak the rest of the reply.

  claude -p ... | stream_speak.py --t0-ms MS [--play --out-dir DIR]

stdout: one JSON line {result, is_error, session_id, t_first_token, t_first_audio, last_mp3,
tts_failed}; times are ms since --t0-ms (the end of the recording), null if it never happened.
On an error result nothing more is spoken; assistant.sh retries or speaks its own fallback.
TTS goes through eleven.py (ElevenLabs SDK), playback through SoX `play` (PULSE_SERVER env).
Without partial messages, whole `assistant` events are used instead of text deltas.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ELEVEN = os.path.join(HERE, "eleven.py")
_SENT_END = re.compile(r"[.!?…][\"')\]]*\s+")


def now_ms() -> int:
    return int(time.time() * 1000)


def clean(text: str) -> str:
    """Python port of assistant.sh's markdown strip (sed, line by line):
    s/[*_`#>|]+//g; s/^[[:space:]]*-[[:space:]]+//; s/\\[([^]]*)\\]\\([^)]*\\)/\\1/g"""
    out = []
    for line in text.split("\n"):
        line = re.sub(r"[*_`#>|]+", "", line)
        line = re.sub(r"^\s*-\s+", "", line, count=1)
        line = re.sub(r"\[([^]]*)\]\([^)]*\)", r"\1", line)
        out.append(line)
    return "\n".join(out)


def split_first_sentence(buf: str) -> tuple[str, str] | None:
    """(first sentence, rest) once `buf` holds a sentence end followed by whitespace and the
    sentence has a letter in it ("1. " alone is not a sentence); None while incomplete."""
    for m in _SENT_END.finditer(buf):
        head = buf[:m.end()].strip()
        if re.search(r"[^\W\d_]", head):
            return head, buf[m.end():]
    return None


def remainder(reply: str, spoken: str | None) -> str:
    """What is left to say of the cleaned final reply after `spoken` (cleaned) was played.
    If the reply does not start with it (it was interim text before a tool call), say it all."""
    reply = reply.strip()
    if spoken and reply.startswith(spoken):
        return reply[len(spoken):].strip()
    return reply


def parse(lines, on_first_sentence, clock=now_ms):
    """Walk stream-json events. Calls on_first_sentence(text) once, as early as possible.
    Returns (result_event or None, t_first_token or None)."""
    buf, spoken, first_token, saw_delta, result = "", False, None, False, None
    for line in lines:
        try:
            ev = json.loads(line)
        except ValueError:
            continue
        if not isinstance(ev, dict) or ev.get("parent_tool_use_id"):
            continue  # subagent output is not the reply
        t, flush = ev.get("type"), False
        if t == "stream_event":
            e = ev.get("event") or {}
            d = e.get("delta") or {}
            if e.get("type") == "content_block_delta" and d.get("type") == "text_delta":
                saw_delta = True
                first_token = first_token or clock()
                buf += d.get("text", "")
            elif e.get("type") == "content_block_stop":
                flush = True
        elif t == "assistant" and not saw_delta:  # no partial messages: whole message at once
            for block in (ev.get("message") or {}).get("content") or []:
                if block.get("type") == "text" and block.get("text"):
                    first_token = first_token or clock()
                    buf += block["text"] + "\n"
            flush = True
        elif t == "result":
            result = ev
        if not spoken:
            s = split_first_sentence(buf)
            if s is None and flush and buf.strip():
                s = (buf.strip(), "")
            if s:
                spoken = True
                on_first_sentence(s[0])
        if flush:
            buf = ""
    return result, first_token


class Speaker:
    """TTS + playback. tts(text, mp3) -> error string or None; play(mp3) -> None."""

    def __init__(self, out_dir, t0, tts=None, play=None, clock=now_ms):
        self.out_dir, self.t0, self.clock = out_dir, t0, clock
        self.tts = tts or self._tts
        self.play = play or self._play
        self.first_audio = None
        self.spoken = None        # cleaned text actually played first
        self.last_mp3 = None
        self.failed = False       # True if the reply was not played to the end
        self._thread = None
        self._stamp = time.strftime("%Y%m%d-%H%M%S")

    @staticmethod
    def _tts(text, mp3):
        p = subprocess.run([ELEVEN, "tts", mp3], input=text, capture_output=True, text=True, timeout=60)
        return None if p.returncode == 0 else ((p.stderr.strip().splitlines() or ["tts failed"])[-1])

    @staticmethod
    def _play(mp3):
        subprocess.run(["play", "-q", mp3], capture_output=True, timeout=300)

    def _say(self, text, part):
        mp3 = os.path.join(self.out_dir, f"reply-{self._stamp}-{part}.mp3")
        err = self.tts(text, mp3)
        if err:
            print(f"stream_speak: {err}", file=sys.stderr)
            return False
        self.first_audio = self.first_audio or self.clock()
        self.last_mp3 = mp3
        self.play(mp3)
        return True

    def first(self, sentence):
        text = clean(sentence).strip()
        if not text:
            return

        def run():
            if self._say(text, "a"):
                self.spoken = text
        self._thread = threading.Thread(target=run, daemon=True)
        self._thread.start()

    def wait(self):
        if self._thread:
            self._thread.join()

    def rest(self, reply):
        self.wait()  # the first sentence finishes playing; also tells what was spoken
        rest = remainder(clean(reply), self.spoken)
        # If the first sentence failed, rest is the whole reply: played once, not failed.
        self.failed = not self._say(rest, "b") if rest else self.spoken is None


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--t0-ms", type=int, default=now_ms())
    ap.add_argument("--play", action="store_true")
    ap.add_argument("--out-dir", default=os.environ.get("VA_OUT_DIR", "/sdcard/VoiceAssistant"))
    a = ap.parse_args(argv)
    sp = Speaker(a.out_dir, a.t0_ms)
    result, first_token = parse(sys.stdin, sp.first if a.play else (lambda s: None))
    result = result or {}
    reply = "" if result.get("is_error") else (result.get("result") or "")
    if a.play:
        if reply:
            sp.rest(reply)
        else:
            sp.wait()  # error: assistant.sh retries or speaks its fallback itself
    rel = lambda t: None if t is None else t - a.t0_ms  # noqa: E731
    print(json.dumps({
        "result": reply, "is_error": bool(result.get("is_error")) or not reply,
        "session_id": result.get("session_id"), "t_first_token": rel(first_token),
        "t_first_audio": rel(sp.first_audio), "last_mp3": sp.last_mp3, "tts_failed": sp.failed,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
