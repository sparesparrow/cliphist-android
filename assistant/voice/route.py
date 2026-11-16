#!/root/mia-venv/bin/python
"""Pick the Claude model for one voice turn.

  route.py "<transcript>"   -> prints the model name (haiku | sonnet | $VA_CLAUDE_MODEL)

VA_CLAUDE_MODEL always wins. Otherwise a short question (<= SHORT_WORDS words and no
phone/home action verb) goes to the fast model; anything else to the default model.
"""
import os
import re
import sys

FAST_MODEL = "haiku"
DEFAULT_MODEL = "sonnet"
SHORT_WORDS = 8
# Words that mean "do something on the phone, in the home or car" (English and Czech).
ACTION_WORDS = {
    "open", "launch", "start", "stop", "close", "send", "text", "message", "call", "dial",
    "tap", "click", "press", "type", "swipe", "scroll", "turn", "switch", "set", "dim",
    "light", "lights", "lock", "unlock", "play", "pause", "run", "remind", "notify",
    "post", "write", "reply", "share", "navigate", "take", "screenshot", "screen", "copy",
    "paste", "delete", "install", "enable", "disable", "mute", "volume",
    "otevři", "otevri", "pošli", "posli", "zavolej", "napiš", "napis", "zapni", "vypni",
    "spusť", "spust", "nastav", "rozsviť", "rozsvit", "zhasni", "pusť", "pust", "zamkni",
    "odemkni", "připomeň", "pripomen", "ztlum", "zavři", "zavri",
}


def choose_model(text: str, env: dict | None = None) -> str:
    env = os.environ if env is None else env
    override = (env.get("VA_CLAUDE_MODEL") or "").strip()
    if override:
        return override
    words = re.findall(r"\w+", text.lower())
    if words and len(words) <= SHORT_WORDS and not ACTION_WORDS.intersection(words):
        return FAST_MODEL
    return DEFAULT_MODEL


if __name__ == "__main__":
    print(choose_model(" ".join(sys.argv[1:])))
