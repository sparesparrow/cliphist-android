"""Tests for the voice pieces: MCP tools speak/listen (subprocess stubbed), the shared turn
lock, model routing (voice/route.py) and stream-json first-sentence speaking
(voice/stream_speak.py, fixture captured from a real `claude -p --output-format stream-json
--verbose --include-partial-messages 'say hi'`, init event trimmed). No phone, no network.
Run:  .venv/bin/python -m pytest -q
"""
import importlib
import io
import os
import json
import subprocess
import sys
from pathlib import Path

import pytest

HERE = Path(__file__).parent
VOICE = HERE.parent / "voice"
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(VOICE))
FIXTURE = (HERE / "fixtures" / "claude-stream-sayhi.jsonl").read_text().splitlines()

import route  # noqa: E402
import stream_speak as ss  # noqa: E402
import turnlock  # noqa: E402


# ---------------------------------------------------------------- MCP speak / listen
@pytest.fixture()
def srv(tmp_path, monkeypatch):
    monkeypatch.setenv("ASSISTANT_STATE", str(tmp_path))
    monkeypatch.setenv("VA_LOCK_FILE", str(tmp_path / "turn.lock"))
    import server
    server = importlib.reload(server)
    calls, script = [], {}

    def fake(cmd, timeout=60, inp=None):
        calls.append((cmd, timeout, inp))
        key = Path(cmd[0]).name if cmd[0] != "timeout" else cmd[2]
        if key == "eleven.py":
            key += " " + cmd[1]
        return script.get(key, (0, "", ""))

    monkeypatch.setattr(server, "_run_split", fake)
    server.calls, server.script = calls, script
    return server


def _cmds(srv):
    return [c[0] for c in srv.calls]


def test_speak_synthesizes_then_plays(srv):
    assert srv.speak("Speak tool test") == "spoke 15 characters"
    (tts, _, inp), (play, _, _) = srv.calls
    assert tts[0].endswith("voice/eleven.py") and tts[1] == "tts" and inp == "Speak tool test"
    assert play[:2] == ["play", "-q"] and play[2] == tts[2]


def test_speak_caps_text(srv):
    out = srv.speak("x" * 1500)
    assert out == "spoke 1000 characters (cut to 1000)"
    assert len(srv.calls[0][2]) == srv.SPEAK_MAX == 1000


def test_speak_empty(srv):
    assert srv.speak("  \n ") == "nothing to say" and not srv.calls


def test_speak_and_listen_busy_while_a_turn_holds_the_lock(srv):
    with turnlock.turn_lock(0) as got:  # what http_trigger.run_locked holds during a turn
        assert got
        assert srv.speak("hello") == "busy"
        assert srv.listen() == "busy"
    assert not srv.calls
    assert srv.speak("hello") == "spoke 5 characters"  # released afterwards


def test_speak_tts_error_is_one_line_without_key(srv):
    srv.script["eleven.py tts"] = (1, "", "Traceback noise\ntts failed: ApiError status=401: "
                                   '{"detail": "invalid api key sk_0123456789abcdef0123456789abcdef"}\n')
    out = srv.speak("hi")
    assert out.startswith("tts failed: tts failed: ApiError status=401")
    assert "\n" not in out and "sk_0123" not in out and "[redacted]" in out
    assert len(srv.calls) == 1  # nothing played


def test_speak_tts_error_without_stderr(srv):
    srv.script["eleven.py tts"] = (127, "", "")
    assert srv.speak("hi") == "tts failed: rc=127"


def test_speak_playback_error(srv):
    srv.script["play"] = (2, "", "play FAIL formats: can't open output file `default': Connection refused\n")
    assert srv.speak("hi") == "playback failed: play FAIL formats: can't open output file `default': Connection refused"


def _rec_cmd(srv):
    return next(c for c in srv.calls if c[0][0] == "timeout")


@pytest.mark.parametrize("asked,used", [(0, 1), (-5, 1), (1, 1), (8, 8), (30, 30), (99, 30)])
def test_listen_clamps_max_s(srv, asked, used):
    srv.script["soxi"] = (0, "1.5\n", "")
    srv.script["eleven.py stt"] = (0, '{"text": "hello", "language_code": "eng"}\n', "")
    assert json.loads(srv.listen(asked)) == {"text": "hello", "language": "eng"}
    cmd, timeout, _ = _rec_cmd(srv)
    assert cmd[1] == str(srv.LISTEN_WAIT + used) and cmd[-1] == str(used)
    # same silence-trim settings as voice/listen.sh
    assert cmd[2:] == ["rec", "-q", "-c1", "-r16000", cmd[6], "silence", "1", "0.1", "2%", "1", "1.3", "2%",
                       "trim", "0", str(used)]
    assert "silence 1 0.1 2% 1 1.3 2% trim 0" in (VOICE / "listen.sh").read_text()


def test_listen_no_speech(srv):
    srv.script["soxi"] = (0, "0.2\n", "")
    assert srv.listen() == "no speech heard"
    assert not any(c[0][0].endswith("eleven.py") for c in srv.calls)


def test_listen_timeout_without_speech_then_no_file(srv):
    srv.script["rec"] = (124, "", "")
    srv.script["soxi"] = (1, "", "soxi FAIL formats: can't open input file")
    assert srv.listen() == "no speech heard"


def test_listen_soxi_garbage(srv):
    srv.script["soxi"] = (0, "n/a\n", "")
    assert srv.listen() == "no speech heard"


def test_listen_recording_error(srv):
    srv.script["rec"] = (1, "", "rec FAIL formats: can't open input `default': Connection refused\n")
    assert srv.listen().startswith("recording failed: rec FAIL formats")


def test_listen_stt_error_is_one_line_without_key(srv):
    srv.script["soxi"] = (0, "2.0\n", "")
    srv.script["eleven.py stt"] = (1, "", "stt failed: ApiError status=401: xi-api-key=abcdefgh rejected\n")
    out = srv.listen()
    assert out == "stt failed: stt failed: ApiError status=401: [redacted] rejected"


@pytest.mark.parametrize("stdout", ["not json", '{"text": "", "language_code": "eng"}'])
def test_listen_stt_bad_or_empty_output(srv, stdout):
    srv.script["soxi"] = (0, "2.0\n", "")
    srv.script["eleven.py stt"] = (0, stdout, "")
    assert srv.listen() in ("stt failed: unexpected output", "no speech heard")


def test_tool_annotations(srv):
    tools = {t.name: t for t in srv.mcp._tool_manager.list_tools()}
    assert tools["speak"].annotations.read_only_hint is False
    assert tools["speak"].annotations.destructive_hint is False
    assert tools["listen"].annotations.read_only_hint is True


def test_run_split_reports_missing_binary_and_timeout(srv):
    real = importlib.reload(sys.modules["server"])
    assert real._run_split(["/nonexistent/bin"])[0] == 127
    rc, _, err = real._run_split(["sleep", "5"], timeout=0.2)
    assert rc == 124 and "timed out" in err
    rc, out, _ = real._run_split(["sh", "-c", "echo $PULSE_SERVER $AUDIODRIVER"])
    assert out.strip() == "127.0.0.1 pulseaudio"


# ---------------------------------------------------------------- turn lock
def test_turn_lock_excludes_other_holders_and_times_out(tmp_path, monkeypatch):
    monkeypatch.setenv("VA_LOCK_FILE", str(tmp_path / "s" / "turn.lock"))
    with turnlock.turn_lock(0) as a:
        assert a
        with turnlock.turn_lock(0.3) as b:  # same process, other open file: still excluded
            assert b is False
    with turnlock.turn_lock(0) as c:
        assert c


def test_turn_lock_held_by_another_process(tmp_path, monkeypatch):
    lock = tmp_path / "turn.lock"
    monkeypatch.setenv("VA_LOCK_FILE", str(lock))
    p = subprocess.Popen(["flock", str(lock), "sleep", "3"])
    try:
        import time
        time.sleep(0.5)
        with turnlock.turn_lock(0) as got:
            assert got is False
    finally:
        p.kill()
        p.wait()


# ---------------------------------------------------------------- model routing
@pytest.mark.parametrize("text", ["What time is it?", "Who wrote Hamlet?", "Kolik je hodin?"])
def test_route_short_question_goes_to_haiku(text):
    assert route.choose_model(text, {}) == "haiku"


@pytest.mark.parametrize("text", ["Open WhatsApp", "Turn the lights blue", "Send Anna a message saying hi",
                                  "zapni světla", "Pošli zprávu Petrovi",
                                  "Can you tell me the history of the Roman Empire in a few words please"])
def test_route_actions_and_long_requests_go_to_default(text):
    assert route.choose_model(text, {}) == "sonnet"


def test_route_empty_goes_to_default():
    assert route.choose_model("", {}) == "sonnet"


def test_route_override_wins():
    assert route.choose_model("What time is it?", {"VA_CLAUDE_MODEL": "opus"}) == "opus"
    assert route.choose_model("Open WhatsApp", {"VA_CLAUDE_MODEL": "haiku"}) == "haiku"
    assert route.choose_model("What time is it?", {"VA_CLAUDE_MODEL": "  "}) == "haiku"


def test_route_cli():
    out = subprocess.run([sys.executable, str(VOICE / "route.py"), "What", "time", "is", "it?"],
                         capture_output=True, text=True, env={"PATH": "/usr/bin:/bin"})
    assert out.stdout.strip() == "haiku"


# ---------------------------------------------------------------- stream-json speaking
def test_fixture_is_real_partial_stream():
    types = [json.loads(l)["type"] for l in FIXTURE]
    assert types[0] == "system" and types[-1] == "result" and "stream_event" in types and "assistant" in types


@pytest.mark.parametrize("buf,want", [
    ("Hi! What", ("Hi!", "What")),
    ("Hi!", None),                                   # might still be "Hi!!" or "Hi!\"": wait
    ("It is 3.5 degrees", None),                     # decimal point is not a sentence end
    ("1. First, open it. Then", ("1. First, open it.", "Then")),
    ('He said "yes." Then', ('He said "yes."', "Then")),
    ("Hotovo. A dál", ("Hotovo.", "A dál")),
    ("", None),
])
def test_split_first_sentence(buf, want):
    assert ss.split_first_sentence(buf) == want


def _clock():
    t = iter(range(1000, 100000, 10))
    return lambda: next(t)


def test_parse_real_stream_speaks_first_sentence_early():
    spoken, seen = [], []

    def lines():
        for l in FIXTURE:
            seen.append(json.loads(l).get("event", {}).get("type") or json.loads(l)["type"])
            yield l
    result, first_token = ss.parse(lines(), lambda s: spoken.append((s, list(seen))), clock=_clock())
    assert result["result"] == "Hi! What can I help you with today?" and result["is_error"] is False
    assert first_token == 1000
    assert len(spoken) == 1 and spoken[0][0] == "Hi!"
    # spoken right after the first text delta, before the message (and the result) is complete
    assert spoken[0][1][-1] == "content_block_delta" and "result" not in spoken[0][1]


def test_parse_without_partial_messages_uses_assistant_events():
    lines = [l for l in FIXTURE if json.loads(l)["type"] != "stream_event"]
    spoken = []
    result, first_token = ss.parse(lines, spoken.append, clock=_clock())
    assert spoken == ["Hi!"] and first_token == 1000 and result["session_id"]


def test_parse_flushes_block_without_sentence_end_and_skips_noise():
    ev = lambda d: json.dumps({"type": "stream_event", "parent_tool_use_id": None, "event": d})  # noqa: E731
    lines = [
        "not json at all",
        json.dumps({"type": "stream_event", "parent_tool_use_id": "toolu_1",
                    "event": {"type": "content_block_delta", "delta": {"type": "text_delta", "text": "Sub. agent."}}}),
        ev({"type": "content_block_delta", "delta": {"type": "text_delta", "text": "Done"}}),
        ev({"type": "content_block_stop"}),
        ev({"type": "content_block_delta", "delta": {"type": "text_delta", "text": "Later. More."}}),
    ]
    spoken = []
    result, _ = ss.parse(lines, spoken.append)
    assert spoken == ["Done"] and result is None


SED = r's/[*_`#>|]+//g; s/^[[:space:]]*-[[:space:]]+//; s/\[([^]]*)\]\([^)]*\)/\1/g'


@pytest.mark.parametrize("text", ["**Bold** and `code`", "- item one\n  - item two", "See [the docs](https://x.y/z).",
                                  "# Title\n> quote | pipe", "plain - dash - text", "snake_case_name"])
def test_clean_matches_assistant_sh_sed(text):
    sed = subprocess.run(["sed", "-E", SED], input=text, capture_output=True, text=True).stdout
    assert ss.clean(text) == sed.rstrip("\n")
    assert SED in (VOICE / "assistant.sh").read_text()


def test_remainder():
    assert ss.remainder("Hi! What can I help you with today?", "Hi!") == "What can I help you with today?"
    assert ss.remainder("Hi!", "Hi!") == ""
    assert ss.remainder("It is 6 pm.", "Let me check.") == "It is 6 pm."  # interim text before a tool call
    assert ss.remainder("It is 6 pm.", None) == "It is 6 pm."


class FakeAudio:
    def __init__(self, fail=()):
        self.fail, self.said, self.played = set(fail), [], []

    def tts(self, text, mp3):
        self.said.append((text, Path(mp3).name.rsplit("-", 1)[-1]))
        return "tts failed: ApiError status=500" if text in self.fail else None

    def play(self, mp3):
        self.played.append(Path(mp3).name.rsplit("-", 1)[-1])


def _speak_fixture(fake, tmp_path, lines=FIXTURE):
    sp = ss.Speaker(str(tmp_path), 0, tts=fake.tts, play=fake.play, clock=_clock())
    result, _ = ss.parse(lines, sp.first)
    sp.rest(result["result"])
    return sp


def test_speaker_plays_first_sentence_then_rest(tmp_path):
    fake = FakeAudio()
    sp = _speak_fixture(fake, tmp_path)
    assert fake.said == [("Hi!", "a.mp3"), ("What can I help you with today?", "b.mp3")]
    assert fake.played == ["a.mp3", "b.mp3"] and sp.first_audio == 1000 and not sp.failed
    assert sp.last_mp3.endswith("-b.mp3")


def test_speaker_first_tts_failure_falls_back_to_whole_reply(tmp_path):
    fake = FakeAudio(fail={"Hi!"})
    sp = _speak_fixture(fake, tmp_path)
    assert fake.said[-1] == ("Hi! What can I help you with today?", "b.mp3")
    assert fake.played == ["b.mp3"] and not sp.failed  # whole reply played once: no re-synthesis


def test_speaker_rest_tts_failure_is_reported(tmp_path):
    fake = FakeAudio(fail={"What can I help you with today?"})
    sp = _speak_fixture(fake, tmp_path)
    assert fake.played == ["a.mp3"] and sp.failed


def test_main_summary_and_error_result(tmp_path, monkeypatch, capsys):
    monkeypatch.setattr(sys, "stdin", io.StringIO("\n".join(FIXTURE)))
    ss.main(["--t0-ms", "0", "--out-dir", str(tmp_path)])
    s = json.loads(capsys.readouterr().out)
    assert s["result"] == "Hi! What can I help you with today?" and s["is_error"] is False
    assert s["session_id"] == "3e627f94-06d6-4b71-9fac-78d700b5aaaa"
    assert s["t_first_token"] > 0 and s["t_first_audio"] is None and s["tts_failed"] is False

    err = json.dumps({"type": "result", "subtype": "error_during_execution", "is_error": True,
                      "result": "No conversation found", "session_id": "x"})
    monkeypatch.setattr(sys, "stdin", io.StringIO(err))
    fake = FakeAudio()
    monkeypatch.setattr(ss.Speaker, "_tts", staticmethod(fake.tts))
    monkeypatch.setattr(ss.Speaker, "_play", staticmethod(fake.play))
    ss.main(["--t0-ms", "0", "--play", "--out-dir", str(tmp_path)])
    s = json.loads(capsys.readouterr().out)
    assert s["is_error"] is True and s["result"] == "" and fake.said == []  # nothing spoken on error


# ---------------------------------------------------------------- assistant.sh / listen.sh (stubbed)
STUB_ELEVEN = """#!/bin/sh
case $1 in
  stt) echo '{"text": "What time is it?", "language_code": "eng"}' ;;
  tts) text=$(cat); printf '%s\\n' "$text" >>"$STUB_LOG/tts"
       if [ -n "${STUB_TTS_FAIL:-}" ] && [ "$text" = "$STUB_TTS_FAIL" ]; then
         echo "tts failed: ApiError status=500" >&2; exit 1; fi
       echo mp3 >"$2" ;;
esac
"""
STUB_CLAUDE = """#!/bin/sh
printf '%s\\n' "$*" >>"$STUB_LOG/claude"
[ "${STUB_CLAUDE_RC:-0}" = 0 ] || exit "$STUB_CLAUDE_RC"
cat "$STUB_STREAM"
"""
STUB_PLAY = """#!/bin/sh
for a; do :; done; echo "${a##*-}" >>"$STUB_LOG/play"
"""


@pytest.fixture()
def va(tmp_path):
    """A copy of voice/ with eleven.py, claude and play stubbed (HOME/.local/bin comes first
    on assistant.sh's PATH). Logs and state stay in tmp_path."""
    v = tmp_path / "voice"
    v.mkdir()
    for f in ("assistant.sh", "route.py", "stream_speak.py"):
        (v / f).write_bytes((VOICE / f).read_bytes())
        (v / f).chmod(0o755)
    bindir = tmp_path / "home" / ".local" / "bin"
    bindir.mkdir(parents=True)
    for path, body in ((v / "eleven.py", STUB_ELEVEN), (bindir / "claude", STUB_CLAUDE), (bindir / "play", STUB_PLAY)):
        path.write_text(body)
        path.chmod(0o755)
    (tmp_path / "prompt.json").write_text('{"content": "Be brief. {{current_time}}"}')
    (tmp_path / "in.wav").write_bytes(b"RIFF")
    (tmp_path / "stub").mkdir()
    (tmp_path / "stream.jsonl").write_text("\n".join(FIXTURE) + "\n")
    env = {"PATH": os.environ["PATH"], "HOME": str(tmp_path / "home"), "ELEVENLABS_API_KEY": "dummy",
           "VA_OUT_DIR": str(tmp_path / "out"), "VA_PROMPT_FILE": str(tmp_path / "prompt.json"),
           "STUB_LOG": str(tmp_path / "stub"), "STUB_STREAM": str(tmp_path / "stream.jsonl")}

    def run(*args, **extra):
        p = subprocess.run([str(v / "assistant.sh"), *args], capture_output=True, text=True,
                           env={**env, **extra}, timeout=60)
        stub = lambda n: ((tmp_path / "stub" / n).read_text().splitlines()  # noqa: E731
                          if (tmp_path / "stub" / n).exists() else [])
        lat = (v / "logs" / "latency.log").read_text().splitlines()
        last = (tmp_path / "out" / "last.txt").read_text().splitlines()
        return p, stub, lat, last
    run.wav = str(tmp_path / "in.wav")
    return run


def _fields(line):
    return dict(kv.split("=", 1) for kv in line.split()[2:])


def test_assistant_speak_streams_first_sentence_and_logs_latency(va):
    p, stub, lat, last = va("--speak", va.wav, VA_T0_MS="1000")
    assert p.returncode == 0, p.stderr
    assert p.stdout.strip().endswith("-b.mp3")
    assert stub("tts") == ["Hi!", "What can I help you with today?"] and stub("play") == ["a.mp3", "b.mp3"]
    args = stub("claude")[0]
    assert "--output-format stream-json --verbose" in args and "--model haiku" in args
    assert last == ["What time is it?", "Hi! What can I help you with today?"]
    assert len(lat) == 1 and "What time" not in lat[0] and "dummy" not in lat[0]
    f = _fields(lat[0])
    assert f["model"] == "haiku" and f["mode"] == "speak" and f["t_rec_end"] == "0"
    for k in ("t_stt_done", "t_first_token", "t_first_audio", "t_done"):
        assert int(f[k]) > 0


def test_assistant_speak_falls_back_to_whole_reply_when_streamed_tts_fails(va):
    # The rest of the reply failed: assistant.sh re-synthesizes the whole reply and plays it.
    p, stub, lat, last = va("--speak", "--text", "What time is it?",
                            STUB_TTS_FAIL="What can I help you with today?")
    assert p.returncode == 0, p.stderr
    assert stub("tts") == ["Hi!", "What can I help you with today?", "Hi! What can I help you with today?"]
    plays = stub("play")
    assert len(plays) == 2 and plays[0] == "a.mp3" and plays[1] != "b.mp3"  # b failed: never played
    assert p.stdout.strip().endswith(plays[1])  # the re-synthesized reply-<time>.mp3
    f = _fields(lat[0])
    assert f["t_stt_done"] == "-" and int(f["t_first_audio"]) >= 0


def test_assistant_speak_first_sentence_tts_failure_plays_reply_once(va):
    p, stub, lat, last = va("--speak", "--text", "What time is it?", STUB_TTS_FAIL="Hi!")
    assert p.returncode == 0, p.stderr
    assert stub("tts") == ["Hi!", "Hi! What can I help you with today?"]
    assert stub("play") == ["b.mp3"] and p.stdout.strip().endswith("-b.mp3")


def test_assistant_mp3_mode_keeps_old_output(va):
    p, stub, lat, last = va("--text", "Open WhatsApp")
    assert p.returncode == 0, p.stderr
    assert "/out/reply-" in p.stdout and p.stdout.count("\n") == 1 and stub("play") == []
    assert stub("tts") == ["Hi! What can I help you with today?"]
    assert "--model sonnet" in stub("claude")[0]
    f = _fields(lat[0])
    assert f["mode"] == "mp3" and f["model"] == "sonnet" and f["t_first_audio"] == "-"


def test_assistant_claude_failure_speaks_apology(va):
    p, stub, lat, last = va("--speak", "--text", "hi", STUB_CLAUDE_RC="1", VA_CLAUDE_MODEL="opus")
    assert p.returncode == 0, p.stderr
    assert last[1] == "Sorry, I couldn't get an answer from Claude."
    assert stub("tts") == [last[1]] and len(stub("play")) == 1
    f = _fields(lat[0])
    assert f["model"] == "opus" and f["t_first_token"] == "-"


def test_listen_sh_wav_must_exist(tmp_path):
    p = subprocess.run([str(VOICE / "listen.sh"), "--wav", str(tmp_path / "nope.wav")],
                       capture_output=True, text=True, timeout=10)
    assert p.returncode == 2 and "no such file" in p.stderr


def test_http_trigger_busy_while_a_turn_holds_the_lock(tmp_path, monkeypatch):
    monkeypatch.setenv("VA_LOCK_FILE", str(tmp_path / "turn.lock"))
    import http_trigger
    monkeypatch.setattr(http_trigger, "TIMEOUT", 0.3)
    with turnlock.turn_lock(0):
        assert http_trigger.Handler.run_locked(None, ["true"]) == (None, "busy")
    p, err = http_trigger.Handler.run_locked(None, ["true"])
    assert err is None and p.returncode == 0
