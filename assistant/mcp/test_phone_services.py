"""Tests for assistant/phone/phone-services (bash) with stub commands on PATH. No phone needed.

The stubs replace `timeout` (which every probe goes through, including the /dev/tcp port check),
`adb`, `pactl`, `curl`, `ssh` and `sleep`. Each stub appends its argv to calls.log.
Run:  .venv/bin/python -m pytest -q
"""
import subprocess
import time
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parent.parent / "phone" / "phone-services"

STUBS = {
    # timeout DUR CMD...: emulate port probes from STUB_PORTS, run everything else
    "timeout": r'''echo "timeout $*" >>"$STUB_DIR/calls.log"; shift
if [ "$1" = bash ] && [ "$2" = -c ]; then
  port=${3##*/}; case " $STUB_PORTS " in *" $port "*) exit 0;; *) exit 1;; esac
fi
exec "$@"''',
    # adb: `shell true` succeeds once $STUB_DIR/adb-up exists; `connect` creates it if STUB_CONNECT_WORKS=1
    "adb": r'''echo "adb $*" >>"$STUB_DIR/calls.log"
case "$*" in
  connect*) [ "${STUB_CONNECT_WORKS:-0}" = 1 ] && touch "$STUB_DIR/adb-up"; echo "connected"; exit 0;;
  *"shell true") [ -f "$STUB_DIR/adb-up" ];;
  *) exit 0;;
esac''',
    "pactl": r'''echo "pactl $*" >>"$STUB_DIR/calls.log"; [ "${STUB_PA_UP:-0}" = 1 ] && echo "0	OpenSL_ES_source	module-sles-source.c"; exit 0''',
    "curl": r'''echo "curl $*" >>"$STUB_DIR/calls.log"; [ "${STUB_VA_UP:-0}" = 1 ]''',
    "ssh": r'''echo "ssh $*" >>"$STUB_DIR/calls.log"; exit 0''',
    "sleep": r'''exit 0''',
}


@pytest.fixture()
def ps(tmp_path):
    stub = tmp_path / "bin"
    stub.mkdir()
    for name, body in STUBS.items():
        f = stub / name
        f.write_text("#!/bin/bash\n" + body + "\n")
        f.chmod(0o755)
    key = tmp_path / "key"
    key.write_text("not a real key\n")
    base_env = {
        "PATH": f"{stub}:/usr/bin:/bin",
        "STUB_DIR": str(tmp_path),
        "PHONE_DIR": str(tmp_path / "phone"),
        "VA_DIR": str(tmp_path / "va"),
        "PHONE_SSH_KEY": str(key),
        "HOME": str(tmp_path),
    }

    def run(*args, adb_up=False, **env):
        if adb_up:
            (tmp_path / "adb-up").touch()
        p = subprocess.run(["bash", str(SCRIPT), *args], capture_output=True, text=True,
                           timeout=30, env={**base_env, **env})
        return p

    def calls():
        log = tmp_path / "calls.log"
        return log.read_text().splitlines() if log.exists() else []

    run.calls = calls
    run.dir = tmp_path
    return run


def test_syntax():
    assert subprocess.run(["bash", "-n", str(SCRIPT)]).returncode == 0


def test_status_all_up(ps):
    p = ps("status", adb_up=True, STUB_PORTS="4713 8022", STUB_PA_UP="1", STUB_VA_UP="1")
    out = p.stdout.splitlines()
    assert out[0].startswith("adb      : OK (127.0.0.1:5555)")
    assert out[1].startswith("mic      : OK")
    assert out[2].startswith("va-http  : OK")
    assert out[3] == "native   : sshd on 8022, key login OK"


def test_status_all_down(ps):
    out = ps("status", STUB_PORTS="").stdout.splitlines()
    assert out == ["adb      : DOWN (port 5555 closed)", "mic      : DOWN",
                   "va-http  : DOWN", "native   : sshd not running"]


def test_status_reports_open_port_when_adb_down(ps):
    assert ps("status", STUB_PORTS="5555").stdout.splitlines()[0] == "adb      : DOWN (port 5555 open)"


def test_status_does_not_adb_connect(ps):
    ps("status", STUB_PORTS="5555")
    assert not [c for c in ps.calls() if c.startswith("adb connect")]


def test_ensure_skips_adb_connect_when_link_is_up(ps):
    ps("ensure", adb_up=True, STUB_PORTS="4713", STUB_PA_UP="1", STUB_VA_UP="1")
    assert not [c for c in ps.calls() if c.startswith("adb connect")]
    log = (ps.dir / "phone" / "logs" / "ensure.log").read_text()
    assert "adb ok" in log and "mic ok" in log and "va-http ok" in log


def test_ensure_reconnects_when_link_is_down_and_port_open(ps):
    ps("ensure", STUB_PORTS="5555 4713", STUB_PA_UP="1", STUB_VA_UP="1", STUB_CONNECT_WORKS="1")
    assert [c for c in ps.calls() if c.startswith("adb connect")] == ["adb connect 127.0.0.1:5555"]
    assert "adb ok" in (ps.dir / "phone" / "logs" / "ensure.log").read_text()


def test_ensure_does_not_connect_when_port_closed(ps):
    ps("ensure", STUB_PORTS="4713", STUB_PA_UP="1", STUB_VA_UP="1", STUB_CONNECT_WORKS="1")
    assert not [c for c in ps.calls() if c.startswith("adb connect")]
    assert "adb down" in (ps.dir / "phone" / "logs" / "ensure.log").read_text()


def test_ensure_is_single_instance(ps):
    logs = ps.dir / "phone" / "logs"
    logs.mkdir(parents=True)
    lock = str(logs / ".lock")
    locker = subprocess.Popen(["flock", "-x", lock, "-c", "read _"], stdin=subprocess.PIPE)
    try:
        for _ in range(100):  # wait until the other holder has the lock
            if subprocess.run(["flock", "-n", lock, "true"]).returncode:
                break
            time.sleep(0.05)
        p = ps("ensure", adb_up=True, STUB_PORTS="4713", STUB_PA_UP="1", STUB_VA_UP="1")
    finally:
        locker.communicate(b"\n", timeout=5)
    assert p.returncode == 0 and not (logs / "ensure.log").exists() and not ps.calls()


def test_va_stop_uses_pid_file_not_pkill(ps):
    va_state = ps.dir / "va" / "state"
    va_state.mkdir(parents=True)
    sleeper = subprocess.Popen(["/bin/sleep", "30"])
    (va_state / "http.pid").write_text(f"{sleeper.pid}\n")
    ps("va-restart", STUB_PORTS="")  # no http_trigger.py in VA_DIR: stop succeeds, start fails
    assert sleeper.wait(timeout=5) != 0  # killed by signal
    assert not (va_state / "http.pid").exists()


def test_unknown_command_prints_usage(ps):
    p = ps("nonsense")
    assert p.returncode == 2 and "phone-services ensure" in p.stdout


def test_native_without_key_fails_cleanly(ps):
    p = ps("native", "true", PHONE_SSH_KEY=str(ps.dir / "missing"))
    assert p.returncode == 1 and "no key" in p.stderr
    assert not [c for c in ps.calls() if c.startswith("ssh")]
