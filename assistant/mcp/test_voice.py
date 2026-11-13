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
    with turnlock.turn_lock(0) as got:  # what http_trigger.run_turn holds during a turn
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
    # same silence-trim settings as voice/listen.sh (VA_REC_SILENCE in voice/audio.env)
    assert cmd[2:] == ["rec", "-q", "-c1", "-r16000", cmd[6], "silence", "1", "0.1", "2%", "1", "1.3", "2%",
                       "trim", "0", str(used)]
    assert srv.calls[0][0] == ["play", "-q", "-n", "synth", "0.12", "sine", "880", "vol", "0.35"]


def test_listen_beep_failure_does_not_record(srv):
    srv.script["play"] = (1, "", "play FAIL sox: Sorry, there is no default audio device configured\n")
    assert srv.listen() == "beep failed, not recording: play FAIL sox: Sorry, there is no default audio device configured"
    assert [c[0] for c in _cmds(srv)] == ["play"]


def test_audio_env_is_the_one_source():
    import server
    env = server._audio_env()
    assert server.PULSE_ENV == {"PULSE_SERVER": "127.0.0.1", "PULSE_COOKIE": "/root/phone/pa/cookie",
                                "AUDIODRIVER": "pulseaudio"}
    assert env["VA_REC_SILENCE"] == "silence 1 0.1 2% 1 1.3 2%" and env["VA_BEEP"] == "0.12 880 0.35"
    for f in ("listen.sh", "assistant.sh"):
        assert '. "$HERE/audio.env"' in (VOICE / f).read_text()
    assert "$VA_REC_SILENCE trim 0" in (VOICE / "listen.sh").read_text()


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


@pytest.mark.parametrize("stdout,want", [("not json", "stt failed: unexpected output"),
                                         ('{"text": "", "language_code": "eng"}', "no speech heard")])
def test_listen_stt_bad_or_empty_output(srv, stdout, want):
    srv.script["soxi"] = (0, "2.0\n", "")
    srv.script["eleven.py stt"] = (0, stdout, "")
    assert srv.listen() == want


def test_tool_annotations(srv):
    tools = {t.name: t for t in srv.mcp._tool_manager.list_tools()}
    assert tools["speak"].annotations.read_only_hint is False
    assert tools["speak"].annotations.destructive_hint is False
    assert tools["listen"].annotations.read_only_hint is False  # records and uploads audio
    assert tools["listen"].annotations.open_world_hint is True
    assert "upload" in tools["listen"].description


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


@pytest.mark.parametrize("text", ["ztlum hudbu", "zavři aplikaci", "spusť Spotify", "pusť rádio",
                                  "rozsviť v kuchyni", "zhasni", "otevři mapy", "napiš Petrovi ahoj"])
def test_route_czech_actions_go_to_default(text):
    assert route.choose_model(text, {}) == "sonnet"


def test_route_word_limit_boundary():
    eight = "what is the capital city of the Czechia"
    assert len(eight.split()) == 8 == route.SHORT_WORDS
    assert route.choose_model(eight + "?", {}) == "haiku"
    assert route.choose_model(eight + " today?", {}) == "sonnet"  # 9 words


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
    ("Dr. Novák is in. Then", ("Dr. Novák is in.", "Then")),             # abbreviations
    ("Mr. Smith, e.g. him. Ok", ("Mr. Smith, e.g. him.", "Ok")),
    ("Use i.e. this. Ok", ("Use i.e. this.", "Ok")),
    ("Třeba např. Petr, tj. on. A", ("Třeba např. Petr, tj. on.", "A")),
    ("Jablka atd. a hrušky apod. Ano. Dál", ("Jablka atd. a hrušky apod. Ano.", "Dál")),
    ("It is 3.5 degrees. Then", ("It is 3.5 degrees.", "Then")),       # number
    ("**Done.** Next", ("**Done.**", "Next")),                         # markdown after the end
    ("(Fine!) Next", ("(Fine!)", "Next")),
    ("Dr. ", None),
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
    """tts/play for Speaker: records calls; texts in fail_tts / parts in fail_play fail."""

    def __init__(self, fail_tts=(), fail_play=()):
        self.fail_tts, self.fail_play, self.said, self.played = set(fail_tts), set(fail_play), [], []

    def tts(self, text, mp3):
        self.said.append((text, Path(mp3).name.rsplit("-", 1)[-1]))
        return "tts failed: ApiError status=500" if text in self.fail_tts else None

    def play(self, mp3, started):
        part = Path(mp3).name.rsplit("-", 1)[-1]
        started()
        self.played.append(part)
        return part not in self.fail_play


def _speak_fixture(fake, tmp_path, lines=FIXTURE):
    sp = ss.Speaker(str(tmp_path), 0, tts=fake.tts, play=fake.play, clock=_clock())
    result, _ = ss.parse(lines, sp.first)
    sp.rest(result["result"])
    return sp


def test_speaker_plays_first_sentence_then_rest(tmp_path):
    fake = FakeAudio()
    sp = _speak_fixture(fake, tmp_path)
    assert fake.said == [("Hi!", "a.mp3"), ("What can I help you with today?", "b.mp3")]
    assert fake.played == ["a.mp3", "b.mp3"] and sp.first_audio == 1000 and sp.unspoken == ""
    assert sp.last_mp3.endswith("-b.mp3")


def test_speaker_synthesizes_rest_while_first_sentence_plays(tmp_path):
    import threading
    order, gate = [], threading.Event()

    def tts(text, mp3):
        order.append("tts " + text)
        if text != "Hi!":
            gate.set()  # rest is being synthesized ...
        return None

    def play(mp3, started):
        started()
        if mp3.endswith("-a.mp3"):
            assert gate.wait(5)  # ... while the first sentence is still playing
        order.append("play " + mp3[-5:])
        return True
    sp = ss.Speaker(str(tmp_path), 0, tts=tts, play=play)
    result, _ = ss.parse(FIXTURE, sp.first)
    sp.rest(result["result"])
    assert order[-2:] == ["play a.mp3", "play b.mp3"] and sp.unspoken == ""


def test_speaker_first_tts_failure_leaves_whole_reply_unspoken(tmp_path):
    fake = FakeAudio(fail_tts={"Hi!"})
    sp = _speak_fixture(fake, tmp_path)
    # nothing was heard: assistant.sh synthesizes the whole reply once (no double playback)
    assert fake.played == [] and sp.unspoken == "Hi! What can I help you with today?"
    assert sp.first_audio is None


def test_speaker_rest_tts_failure_leaves_only_the_remainder(tmp_path):
    fake = FakeAudio(fail_tts={"What can I help you with today?"})
    sp = _speak_fixture(fake, tmp_path)
    assert fake.played == ["a.mp3"] and sp.unspoken == "What can I help you with today?"


def test_speaker_rest_playback_failure_leaves_the_remainder(tmp_path):
    fake = FakeAudio(fail_play={"b.mp3"})
    sp = _speak_fixture(fake, tmp_path)
    assert sp.unspoken == "What can I help you with today?" and sp.last_mp3.endswith("-a.mp3")


def test_speaker_rest_tts_timeout(tmp_path, monkeypatch):
    calls = []

    def run(cmd, **kw):
        calls.append(kw["input"])
        if kw["input"] != "Hi!":
            raise subprocess.TimeoutExpired(cmd, kw["timeout"])
        Path(cmd[2]).write_text("mp3")
        return subprocess.CompletedProcess(cmd, 0, "", "")
    monkeypatch.setattr(ss.subprocess, "run", run)
    fake = FakeAudio()
    sp = ss.Speaker(str(tmp_path), 0, play=fake.play)
    result, _ = ss.parse(FIXTURE, sp.first)
    sp.rest(result["result"])
    assert calls == ["Hi!", "What can I help you with today?"]
    assert fake.played == ["a.mp3"] and sp.unspoken == "What can I help you with today?"


@pytest.mark.parametrize("exc", [FileNotFoundError(2, "No such file"), PermissionError(13, "denied")])
def test_tts_cannot_run(exc, monkeypatch):
    monkeypatch.setattr(ss.subprocess, "run", lambda *a, **k: (_ for _ in ()).throw(exc))
    assert ss.Speaker._tts("hi", "/tmp/x.mp3") == f"tts could not run: {type(exc).__name__}"


def test_play_missing_or_failing(tmp_path, monkeypatch):
    started = []
    monkeypatch.setenv("PATH", str(tmp_path))  # no `play` here
    assert ss.Speaker._play("x.mp3", lambda: started.append(1)) is False and started == []
    (tmp_path / "play").write_text("#!/bin/sh\nexit 2\n")
    (tmp_path / "play").chmod(0o755)
    assert ss.Speaker._play("x.mp3", lambda: started.append(1)) is False and started == [1]
    (tmp_path / "play").write_text("#!/bin/sh\nexit 0\n")
    assert ss.Speaker._play("x.mp3", lambda: started.append(1)) is True


def test_main_summary_and_error_result(tmp_path, monkeypatch, capsys):
    monkeypatch.setattr(sys, "stdin", io.StringIO("\n".join(FIXTURE)))
    ss.main(["--t0-ms", "0", "--out-dir", str(tmp_path)])
    s = json.loads(capsys.readouterr().out)
    assert s["result"] == "Hi! What can I help you with today?" and s["is_error"] is False
    assert s["session_id"] == "3e627f94-06d6-4b71-9fac-78d700b5aaaa"
    assert s["t_first_token"] > 0 and s["t_first_audio"] is None
    assert s["unspoken"] == "Hi! What can I help you with today?"

    err = json.dumps({"type": "result", "subtype": "error_during_execution", "is_error": True,
                      "result": "No conversation found", "session_id": "x"})
    monkeypatch.setattr(sys, "stdin", io.StringIO(err))
    fake = FakeAudio()
    monkeypatch.setattr(ss.Speaker, "_tts", staticmethod(fake.tts))
    monkeypatch.setattr(ss.Speaker, "_play", staticmethod(fake.play))
    ss.main(["--t0-ms", "0", "--play", "--out-dir", str(tmp_path)])
    s = json.loads(capsys.readouterr().out)
    assert s["is_error"] is True and s["result"] == "" and fake.said == []  # nothing spoken on error


def test_main_always_prints_summary(tmp_path, monkeypatch, capsys):
    def boom(*a, **k):
        raise RuntimeError("unexpected")
    monkeypatch.setattr(ss, "parse", boom)
    monkeypatch.setattr(sys, "stdin", io.StringIO(""))
    ss.main(["--t0-ms", "0", "--play", "--out-dir", str(tmp_path)])
    s = json.loads(capsys.readouterr().out)
    assert s["is_error"] is True and s["t_first_audio"] is None


def test_main_logs_init_tools(monkeypatch, capsys):
    monkeypatch.setattr(sys, "stdin", io.StringIO(json.dumps(INIT) + "\n"))
    ss.main(["--t0-ms", "0"])
    err = capsys.readouterr().err
    line = json.loads(err.split("stream_speak: init ", 1)[1])
    assert line["mcp_servers"] == ["android-assistant:connected", "mcp-prompts:connected"]


# ---------------------------------------------------------------- voice turn isolation (A)
INIT = json.loads((HERE / "fixtures" / "claude-init-voice.json").read_text())


def _allowed_tools():
    text = (VOICE / "assistant.sh").read_text()
    block = text.split("ALLOWED_TOOLS=(", 1)[1].split(")", 1)[0]
    return block.split()


def test_real_init_has_only_the_voice_servers_and_no_connectors():
    # fields of the system/init event of a live voice turn with assistant.sh's flags, as
    # stream_speak.py logged them (mcp_servers "name:status" expanded back to objects)
    assert INIT["type"] == "system" and INIT["subtype"] == "init"
    assert sorted(t for t in INIT["tools"] if not t.startswith("mcp__")) == ["WebFetch", "WebSearch"]  # --tools
    assert INIT["permissionMode"] == "dontAsk"
    assert sorted(m["name"] for m in INIT["mcp_servers"]) == ["android-assistant", "mcp-prompts"]
    servers = {t.split("__")[1] for t in INIT["tools"] if t.startswith("mcp__")}
    assert servers == {"android-assistant", "mcp-prompts"}  # no claude.ai connectors
    assert not any(t.startswith("mcp__claude_ai") for t in INIT["tools"])
    for tool in _allowed_tools():  # everything the turn may use exists
        assert tool in INIT["tools"], tool


def test_allowed_tools_exclude_voice_and_unconfigured_tools():
    allowed = _allowed_tools()
    assert not any("cliphist" in t for t in allowed)
    assert "mcp__android-assistant__speak" not in allowed and "mcp__android-assistant__listen" not in allowed


def test_mcp_json_matches_init_and_has_no_secrets():
    cfg = json.loads((VOICE / "mcp.json").read_text())["mcpServers"]
    assert sorted(cfg) == sorted(m["name"] for m in INIT["mcp_servers"])
    envs = {k for c in cfg.values() for k in c.get("env", {})}
    assert envs <= {"PROMPTS_DIR"}


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
# The first sentence and the rest are synthesized concurrently: compare TTS calls unordered.
# STUB_CLAUDE_RC != 0: print only STUB_PARTIAL lines of the stream (if set), then fail.
# STUB_FAIL_ONCE: fail only on the first call (e.g. --resume of an expired session).
STUB_CLAUDE = """#!/bin/sh
printf '%s\\n' "$*" >>"$STUB_LOG/claude"
n=$(wc -l <"$STUB_LOG/claude")
if [ "${STUB_CLAUDE_RC:-0}" != 0 ] && { [ -z "${STUB_FAIL_ONCE:-}" ] || [ "$n" = 1 ]; }; then
  [ -n "${STUB_PARTIAL:-}" ] && head -n "$STUB_PARTIAL" "$STUB_STREAM"
  exit "$STUB_CLAUDE_RC"
fi
cat "$STUB_STREAM"
"""
STUB_PLAY = """#!/bin/sh
for a; do :; done; echo "${a##*-}" >>"$STUB_LOG/play"
"""


@pytest.fixture()
def va(tmp_path):
    """A copy of voice/ with eleven.py, claude and play stubbed (HOME/.local/bin comes first
    on assistant.sh's PATH). Logs, state and the turn lock stay in tmp_path."""
    v = tmp_path / "voice"
    v.mkdir()
    for f in ("assistant.sh", "route.py", "stream_speak.py", "audio.env", "mcp.json"):
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
        latf, lastf = v / "logs" / "latency.log", tmp_path / "out" / "last.txt"
        lat = latf.read_text().splitlines() if latf.exists() else []
        last = lastf.read_text().splitlines() if lastf.exists() else []
        return p, stub, lat, last
    run.wav = str(tmp_path / "in.wav")
    run.dir = v
    return run


def _fields(line):
    return dict(kv.split("=", 1) for kv in line.split()[2:])


def test_assistant_speak_streams_first_sentence_and_logs_latency(va):
    p, stub, lat, last = va("--speak", va.wav, VA_T0_MS="1000")
    assert p.returncode == 0, p.stderr
    assert p.stdout.strip().endswith("-b.mp3")
    assert sorted(stub("tts")) == sorted(["Hi!", "What can I help you with today?"]) and stub("play") == ["a.mp3", "b.mp3"]
    args = stub("claude")[0]
    assert "--output-format stream-json --verbose" in args and "--model haiku" in args
    assert last == ["What time is it?", "Hi! What can I help you with today?"]
    assert len(lat) == 1 and "What time" not in lat[0] and "dummy" not in lat[0]
    f = _fields(lat[0])
    assert f["outcome"] == "ok" and f["model"] == "haiku" and f["mode"] == "speak" and f["t_rec_end"] == "0"
    for k in ("t_stt_done", "t_first_token", "t_first_audio", "t_done"):
        assert int(f[k]) > 0


def test_assistant_claude_runs_isolated(va):
    p, stub, lat, last = va("--text", "hi")
    args = stub("claude")[0]
    for flag in ("--permission-mode dontAsk", "--strict-mcp-config",
                 f"--mcp-config {va.dir}/mcp.json", "--setting-sources project,local",
                 "--tools WebSearch,WebFetch", "--disallowedTools Bash Edit Write NotebookEdit"):
        assert flag in args, flag
    allowed = args.split("--allowedTools ", 1)[1].split(" --", 1)[0].split()
    assert allowed == _allowed_tools()


def test_assistant_speak_rest_tts_failure_synthesizes_only_the_remainder(va):
    p, stub, lat, last = va("--speak", "--text", "What time is it?",
                            STUB_TTS_FAIL="What can I help you with today?")
    assert p.returncode != 0  # the stub fails that text every time: assistant.sh's retry fails too
    assert sorted(stub("tts")) == sorted(["Hi!", "What can I help you with today?", "What can I help you with today?"])
    assert stub("play") == ["a.mp3"] and len(stub("claude")) == 1
    assert _fields(lat[0])["outcome"] == "error"


def test_assistant_speak_rest_fallback_plays_remainder(va, tmp_path):
    # rest TTS fails once in stream_speak, then works in assistant.sh: only the remainder is said again
    (va.dir / "eleven.py").write_text(STUB_ELEVEN.replace(
        '[ "$text" = "$STUB_TTS_FAIL" ]; then',
        '[ "$text" = "$STUB_TTS_FAIL" ] && [ ! -e "$STUB_LOG/failed" ]; then touch "$STUB_LOG/failed";'))
    p, stub, lat, last = va("--speak", "--text", "What time is it?",
                            STUB_TTS_FAIL="What can I help you with today?")
    assert p.returncode == 0, p.stderr
    assert sorted(stub("tts")) == sorted(["Hi!", "What can I help you with today?", "What can I help you with today?"])
    plays = stub("play")
    assert len(plays) == 2 and plays[0] == "a.mp3" and plays[1] != "b.mp3"
    assert p.stdout.strip().endswith(plays[1]) and len(stub("claude")) == 1
    f = _fields(lat[0])
    assert f["outcome"] == "tts_fallback" and int(f["t_first_audio"]) >= 0
    assert last[1] == "Hi! What can I help you with today?"


def test_assistant_speak_first_sentence_tts_failure_plays_reply_once(va):
    p, stub, lat, last = va("--speak", "--text", "What time is it?", STUB_TTS_FAIL="Hi!")
    assert p.returncode == 0, p.stderr
    # stream_speak: "Hi!" failed and the rest was synthesized ahead but not played;
    # assistant.sh says the whole reply once.
    assert sorted(stub("tts")) == sorted(["Hi!", "What can I help you with today?", "Hi! What can I help you with today?"])
    assert len(stub("play")) == 1 and stub("play")[0] not in ("a.mp3", "b.mp3")
    assert len(stub("claude")) == 1 and _fields(lat[0])["outcome"] == "tts_fallback"


def test_assistant_play_missing_does_not_rerun_claude(va, tmp_path):
    (tmp_path / "home" / ".local" / "bin" / "play").write_text("#!/bin/sh\nexit 127\n")
    p, stub, lat, last = va("--speak", "--text", "What time is it?")
    assert p.returncode == 0, p.stderr
    assert len(stub("claude")) == 1
    assert last[1] == "Hi! What can I help you with today?"


def test_assistant_mp3_mode_keeps_old_output(va):
    p, stub, lat, last = va("--text", "Open WhatsApp")
    assert p.returncode == 0, p.stderr
    assert "/out/reply-" in p.stdout and p.stdout.count("\n") == 1 and stub("play") == []
    assert stub("tts") == ["Hi! What can I help you with today?"]
    assert "--model sonnet" in stub("claude")[0]
    f = _fields(lat[0])
    assert f["outcome"] == "ok" and f["mode"] == "mp3" and f["model"] == "sonnet" and f["t_first_audio"] == "-"


def test_assistant_claude_failure_speaks_apology(va):
    p, stub, lat, last = va("--speak", "--text", "hi", STUB_CLAUDE_RC="1", VA_CLAUDE_MODEL="opus")
    assert p.returncode == 0, p.stderr
    assert last[1] == "Sorry, I couldn't get an answer from Claude."
    assert stub("tts") == [last[1]] and len(stub("play")) == 1
    f = _fields(lat[0])
    assert f["outcome"] == "claude_failed" and f["model"] == "opus" and f["t_first_token"] == "-"


def _session(va):
    (va.dir / "state").mkdir(exist_ok=True)
    (va.dir / "state" / "session").write_text("old-session")


def test_assistant_resume_failure_retries_when_nothing_was_heard(va):
    _session(va)
    p, stub, lat, last = va("--speak", "--text", "hi", STUB_CLAUDE_RC="1", STUB_FAIL_ONCE="1")
    assert p.returncode == 0, p.stderr
    calls = stub("claude")
    assert len(calls) == 2 and "--resume old-session" in calls[0] and "--resume" not in calls[1]
    assert last[1] == "Hi! What can I help you with today?" and stub("play") == ["a.mp3", "b.mp3"]


def test_assistant_resume_failure_after_spoken_sentence_does_not_rerun(va):
    _session(va)
    # Claude streams "Hi! What" (first sentence spoken), then dies without a result.
    p, stub, lat, last = va("--speak", "--text", "hi", STUB_CLAUDE_RC="1", STUB_FAIL_ONCE="1", STUB_PARTIAL="6")
    assert p.returncode == 0, p.stderr
    assert len(stub("claude")) == 1  # no second answer, no repeated tool actions
    assert stub("play")[0] == "a.mp3" and last[1] == "Sorry, I couldn't get an answer from Claude."
    assert _fields(lat[0])["outcome"] == "claude_failed"


def test_assistant_die_writes_latency_line(va):
    p, stub, lat, last = va("--text", "Open WhatsApp", STUB_TTS_FAIL="Hi! What can I help you with today?")
    assert p.returncode == 1 and "text-to-speech failed" in p.stderr
    f = _fields(lat[0])
    assert f["outcome"] == "error" and f["model"] == "sonnet" and f["t_done"] != "-"


def test_assistant_speak_busy_and_inherited_lock(va):
    lock = va.dir / "state" / "turn.lock"
    lock.parent.mkdir(exist_ok=True)
    holder = subprocess.Popen(["flock", str(lock), "sleep", "10"])
    try:
        import time
        time.sleep(0.5)
        p, stub, lat, last = va("--speak", "--text", "hi")
        assert p.returncode == 4 and p.stderr.strip() == "busy" and stub("claude") == []
        # a caller that holds the lock (http_trigger, listen.sh) says so: no deadlock
        p, stub, lat, last = va("--speak", "--text", "hi", VA_TURN_LOCKED="1")
        assert p.returncode == 0, p.stderr
        # mp3 mode (Tasker /ask) is serialized by http_trigger alone
    finally:
        holder.kill()
        holder.wait()


def test_listen_sh_wav_must_exist(tmp_path):
    p = subprocess.run([str(VOICE / "listen.sh"), "--wav", str(tmp_path / "nope.wav")],
                       capture_output=True, text=True, timeout=10)
    assert p.returncode == 2 and "no such file" in p.stderr


def test_listen_sh_busy(tmp_path):
    lock, wav = tmp_path / "turn.lock", tmp_path / "q.wav"
    wav.write_bytes(b"RIFF")
    holder = subprocess.Popen(["flock", str(lock), "sleep", "10"])
    try:
        import time
        time.sleep(0.5)
        p = subprocess.run([str(VOICE / "listen.sh"), "--wav", str(wav)], capture_output=True, text=True,
                           timeout=10, env={**os.environ, "VA_LOCK_FILE": str(lock), "VA_OUT_DIR": str(tmp_path)})
        assert p.returncode == 4 and p.stderr.strip() == "busy"
    finally:
        holder.kill()
        holder.wait()


def test_http_trigger_busy_while_a_turn_holds_the_lock(tmp_path, monkeypatch):
    monkeypatch.setenv("VA_LOCK_FILE", str(tmp_path / "turn.lock"))
    import http_trigger
    monkeypatch.setattr(http_trigger, "TIMEOUT", 0.3)
    with turnlock.turn_lock(0):
        assert http_trigger.run_turn(["true"]) == (None, "busy")
    p, err = http_trigger.run_turn(["sh", "-c", "echo locked=$VA_TURN_LOCKED"])
    assert err is None and p.returncode == 0 and p.stdout.strip() == "locked=1"


def test_http_trigger_timeout_kills_the_process_group(tmp_path, monkeypatch):
    monkeypatch.setenv("VA_LOCK_FILE", str(tmp_path / "turn.lock"))
    import http_trigger
    monkeypatch.setattr(http_trigger, "TIMEOUT", 1)
    pidf = tmp_path / "grandchild.pid"
    p, err = http_trigger.run_turn(["sh", "-c", f"sleep 60 & echo $! >{pidf}; wait"])
    assert (p, err) == (None, "assistant timed out")
    pid = int(pidf.read_text())
    import time
    for _ in range(20):
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            break
        time.sleep(0.1)
    else:
        pytest.fail("grandchild sleep survived the timeout")
