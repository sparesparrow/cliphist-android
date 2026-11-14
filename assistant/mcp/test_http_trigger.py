"""voice/http_trigger.py: /status, /stop, async turns, /ask speak, auth. Real HTTP server on a
random port; listen.sh / assistant.sh are replaced by small fake scripts in tmp."""
import importlib
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

import pytest

VOICE = Path(__file__).resolve().parents[1] / "voice"
sys.path.insert(0, str(VOICE))


@pytest.fixture()
def trig(tmp_path, monkeypatch):
    state, logs, here = tmp_path / "state", tmp_path / "logs", tmp_path / "bin"
    for d in (state, logs, here):
        d.mkdir()
    (state / "http-token").write_text("T0K3N")
    monkeypatch.setenv("VA_STATE_DIR", str(state))
    monkeypatch.setenv("VA_LOG_DIR", str(logs))
    monkeypatch.setenv("VA_LOCK_FILE", str(state / "turn.lock"))
    monkeypatch.setenv("VA_PHASE_FILE", str(state / "phase"))
    import http_trigger
    m = importlib.reload(http_trigger)
    m.HERE = str(here)
    m.STOP_GRACE = 0.3
    srv = m.ThreadingHTTPServer(("127.0.0.1", 0), m.Handler)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    m.base = f"http://127.0.0.1:{srv.server_address[1]}"
    m.bin, m.state, m.logs, m.tmp = here, state, logs, tmp_path

    def script(name, body):
        p = here / name
        p.write_text("#!/bin/bash\n" + body)
        p.chmod(0o755)
    m.script = script
    yield m
    srv.shutdown()


def call(m, method, path, body=None, token="T0K3N"):
    data = json.dumps(body).encode() if body is not None else (b"" if method == "POST" else None)
    req = urllib.request.Request(m.base + path, data=data, method=method)
    if token:
        req.add_header("X-Token", token)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, r.read().decode().strip()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode().strip()


def status(m):
    code, body = call(m, "GET", "/status")
    assert code == 200
    return json.loads(body)


def wait_for(pred, timeout=5.0):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if pred():
            return True
        time.sleep(0.05)
    return False


def alive(pid):
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    # a zombie still answers kill 0; check its state
    try:
        return Path(f"/proc/{pid}/stat").read_text().split(")")[1].split()[0] != "Z"
    except OSError:
        return False


def test_health_is_open_everything_else_needs_the_token(trig):
    assert call(trig, "GET", "/health", token=None) == (200, "ok")
    assert call(trig, "GET", "/status", token=None)[0] == 403
    assert call(trig, "GET", "/status", token="wrong")[0] == 403
    assert call(trig, "POST", "/stop", token=None)[0] == 403
    assert call(trig, "POST", "/listen?async=1", token=None)[0] == 403
    assert call(trig, "GET", "/nope")[0] == 404


def test_status_idle_with_last_exchange_from_the_log(trig):
    (trig.logs / "assistant.log").write_text(
        "2026-11-13 10:00:00 heard [eng]: old question\n"
        "2026-11-13 10:00:05 reply: old answer\n"
        "stream_speak: init {...}\n"
        "2026-11-13 10:01:00 heard [ces]: Kolik je hodin?\n"
        "2026-11-13 10:01:09 reply: Je deset hodin.\n")
    s = status(trig)
    assert s["phase"] == "idle" and s["busy"] is False
    assert s["heard"] == "Kolik je hodin?" and s["reply"] == "Je deset hodin."
    assert s["session_age_s"] is None and s["last"]["result"] is None


def test_status_caps_text_and_survives_a_missing_log(trig):
    assert status(trig)["heard"] == ""
    (trig.logs / "assistant.log").write_text("2026-11-13 10:01:09 reply: " + "x" * 5000 + "\n")
    assert len(status(trig)["reply"]) == trig.TEXT_CAP


def test_stale_phase_reads_idle_when_no_turn_runs(trig):
    (trig.state / "phase").write_text("thinking\n")
    assert status(trig)["phase"] == "idle"


def test_sync_listen_reply_and_no_speech(trig):
    trig.script("listen.sh", 'echo "env=$VA_TURN_LOCKED args=$*"\necho "It is ten."\n')
    assert call(trig, "POST", "/listen?new=1") == (200, "It is ten.")
    trig.script("listen.sh", "exit 3\n")
    assert call(trig, "POST", "/listen") == (200, "no speech heard")
    assert status(trig)["last"]["result"] == "no speech heard"


def test_async_listen_starts_reports_busy_and_records_the_outcome(trig):
    trig.script("listen.sh",
                f'printf "listening\\n" > "{trig.state}/phase"\nsleep 1\necho "Done."\n')
    assert call(trig, "POST", "/listen?async=1") == (202, "started")
    assert wait_for(lambda: status(trig)["phase"] == "listening")
    s = status(trig)
    assert s["busy"] is True
    assert call(trig, "POST", "/listen?async=1") == (503, "busy")
    assert call(trig, "POST", "/ask?async=1", {"text": "hi"}) == (503, "busy")
    assert wait_for(lambda: status(trig)["last"]["result"] == "Done.")
    s = status(trig)
    assert s["busy"] is False and s["phase"] == "idle" and s["last"]["code"] == 200


def test_stop_when_idle(trig):
    assert call(trig, "POST", "/stop") == (200, "idle")


def test_stop_ends_the_whole_turn_process_group(trig):
    pids = trig.tmp / "pids"
    trig.script("listen.sh", f'sleep 30 &\necho "$$ $!" > "{pids}"\nwait\n')
    assert call(trig, "POST", "/listen?async=1")[0] == 202
    assert wait_for(pids.exists)
    shell_pid, child = map(int, pids.read_text().split())
    assert call(trig, "POST", "/stop") == (200, "stopped")
    assert wait_for(lambda: not alive(shell_pid) and not alive(child))
    assert wait_for(lambda: status(trig)["last"]["result"] == "stopped")
    assert status(trig)["busy"] is False


def test_stop_escalates_to_sigkill(trig):
    pids = trig.tmp / "pids"
    trig.script("listen.sh", f"trap '' TERM\necho $$ > \"{pids}\"\nwhile :; do sleep 0.1; done\n")
    assert call(trig, "POST", "/listen?async=1")[0] == 202
    assert wait_for(pids.exists)
    pid = int(pids.read_text())
    assert call(trig, "POST", "/stop") == (200, "stopped")
    assert alive(pid)   # ignored SIGTERM ...
    assert wait_for(lambda: not alive(pid), timeout=3)   # ... SIGKILL after STOP_GRACE


def test_stop_never_touches_processes_outside_the_turn(trig):
    import subprocess
    bystander = subprocess.Popen(["sleep", "30"])   # same name as the turn's child
    try:
        pids = trig.tmp / "pids"
        trig.script("listen.sh", f'sleep 30 &\necho "$$ $!" > "{pids}"\nwait\n')
        call(trig, "POST", "/listen?async=1")
        assert wait_for(pids.exists)
        call(trig, "POST", "/stop")
        assert wait_for(lambda: status(trig)["busy"] is False)
        assert bystander.poll() is None
    finally:
        bystander.kill()
        bystander.wait()


def test_ask_speak_flag_and_text_args(trig):
    trig.script("assistant.sh", 'echo "$*"\n')
    assert call(trig, "POST", "/ask", {"text": "  what time  ", "speak": True, "new": True}) == \
        (200, "--speak --new --text what time")
    assert call(trig, "POST", "/ask", {"text": "x", "speak": "yes"}) == (200, "--text x")


def test_ask_rejects_bad_bodies(trig):
    assert call(trig, "POST", "/ask", ["text"])[0] == 400
    assert call(trig, "POST", "/ask", {})[0] == 400
    assert call(trig, "POST", "/ask", {"audio": "/etc/passwd"})[0] == 400


def test_busy_when_another_process_holds_the_lock(trig):
    import turnlock
    trig.script("listen.sh", "echo never\n")
    with turnlock.turn_lock(0) as got:
        assert got
        assert status(trig)["busy"] is True
        assert call(trig, "POST", "/listen?async=1") == (503, "busy")


# --- Claude Panel router endpoints ------------------------------------------
FIXP = Path(__file__).parent / "fixtures" / "panel-prompts"


@pytest.fixture()
def rtrig(trig, tmp_path, monkeypatch):
    import prompts
    pdir = tmp_path / "prompts" / "panel"
    import shutil
    shutil.copytree(FIXP, pdir)
    monkeypatch.setattr(prompts, "PROMPTS_DIR", str(tmp_path / "prompts"))
    monkeypatch.setattr(prompts, "SERVER", "/nonexistent/mcp-prompts")   # file fallback: fast
    return trig


def test_router_endpoints_need_the_token(rtrig):
    for method, path in (("GET", "/templates"), ("GET", "/repos"), ("POST", "/draft"),
                         ("POST", "/dictate"), ("POST", "/dispatch")):
        assert call(rtrig, method, path, token="nope")[0] == 403, path


def test_templates_and_repos_lists(rtrig):
    code, body = call(rtrig, "GET", "/templates")
    assert code == 200
    assert [t["id"] for t in json.loads(body)] == ["panel-bug-fix", "panel-coding-task", "panel-repo-question"]
    code, body = call(rtrig, "GET", "/repos")
    assert code == 200 and json.loads(body)[0]["slug"] == "sparesparrow/cliphist-android"


def test_draft_endpoint_fills_and_validates(rtrig):
    code, body = call(rtrig, "POST", "/draft", {"transcript": "add tests", "repo": "mia"})
    d = json.loads(body)
    assert code == 200 and d["slug"] == "sparesparrow/mia" and "> add tests" in d["filled"]
    assert call(rtrig, "POST", "/draft", {"template": "panel-router"})[0] == 400
    assert call(rtrig, "POST", "/draft", {"repo": "someone/else"})[0] == 400
    assert call(rtrig, "POST", "/draft", ["x"])[0] == 400
    s = status(rtrig)
    assert s["draft"]["repo"] == "mia" and len(s["draft"]["filled"]) <= 600
    assert s["dispatch"]["state"] == "idle" and s["jobs"] == []


def test_dictate_puts_the_transcript_in_the_draft(rtrig):
    rtrig.script("listen.sh", '[ "$1" = --transcribe-only ] || exit 9\necho "make the panel blue"\n')
    assert call(rtrig, "POST", "/dictate?async=1") == (202, "started")
    assert wait_for(lambda: status(rtrig)["draft"]["transcript"] == "make the panel blue")
    assert wait_for(lambda: status(rtrig)["last"]["result"] == "dictated: make the panel blue")


def test_dictate_no_speech_keeps_the_draft(rtrig):
    call(rtrig, "POST", "/draft", {"transcript": "keep me"})
    rtrig.script("listen.sh", "echo 'no speech heard'; exit 3\n")
    assert call(rtrig, "POST", "/dictate") == (200, "no speech heard")
    assert status(rtrig)["draft"]["transcript"] == "keep me"


def test_dispatch_routes_then_posts(rtrig, monkeypatch):
    import router
    posted = []
    monkeypatch.setattr(router, "route", lambda d: ("github-claude", "code work"))
    monkeypatch.setattr(router, "dispatch_github",
                        lambda d: posted.append(d) or {"id": "mia#3"})
    call(rtrig, "POST", "/draft", {"transcript": "x", "repo": "mia"})
    assert call(rtrig, "POST", "/dispatch") == (202, "started")
    assert wait_for(lambda: status(rtrig)["dispatch"]["state"] == "done")
    assert status(rtrig)["dispatch"]["result"] == "mia#3: waiting for @claude (code work)"
    assert posted[0]["repo"] == "mia"


def test_dispatch_not_set_up_and_none(rtrig, monkeypatch):
    import router
    def no(d):
        raise router.DispatchError("@claude not set up in mia")
    monkeypatch.setattr(router, "route", lambda d: ("github-claude", "r"))
    monkeypatch.setattr(router, "dispatch_github", no)
    call(rtrig, "POST", "/dispatch")
    assert wait_for(lambda: status(rtrig)["dispatch"]["state"] == "failed")
    assert status(rtrig)["dispatch"]["result"] == "@claude not set up in mia"
    monkeypatch.setattr(router, "route", lambda d: ("none", "unclear"))
    call(rtrig, "POST", "/dispatch")
    assert wait_for(lambda: status(rtrig)["dispatch"]["result"] == "not sent: unclear")


def test_dispatch_phone_route_runs_a_spoken_turn(rtrig, monkeypatch):
    import router
    monkeypatch.setattr(router, "route", lambda d: ("phone-assistant", "weather"))
    rtrig.script("assistant.sh", 'echo "$@" > "$(dirname "$0")/args"; echo ok\n')
    call(rtrig, "POST", "/draft", {"transcript": "is it raining", "repo": "mia"})
    call(rtrig, "POST", "/dispatch")
    args = rtrig.bin / "args"
    assert wait_for(lambda: args.exists() and args.read_text().strip())
    assert args.read_text().strip() == "--speak --text is it raining"


def test_watch_once_saves_and_announces(rtrig, monkeypatch):
    import router
    router.save_jobs([{"id": "mia#1", "state": "waiting"}, {"id": "mia#2", "state": "pr"}])
    def poll(j):
        if j["state"] != "waiting":
            return False
        j.update(state="pr", repo="mia", issue=1, title="T", url="u", note="draft PR ready", issue_url="i")
        return True
    said = []
    monkeypatch.setattr(router, "poll_job", poll)
    monkeypatch.setattr(rtrig, "notify", lambda t, x: said.append(("notify", t)))
    monkeypatch.setattr(rtrig, "run_turn", lambda args, lock_timeout=None: said.append(("say", args[-1])) or (None, None))
    changed = rtrig.watch_once()
    assert [j["id"] for j in changed] == ["mia#1"]
    assert [j["state"] for j in router.jobs()] == ["pr", "pr"]
    assert said == [("notify", "Claude: draft PR ready (mia)"), ("say", "Draft PR ready in mia: T")]
    assert status(rtrig)["jobs"][0]["line"] == "mia#1 → draft PR ready"
