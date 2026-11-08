"""Unit tests for the android-assistant MCP server: confirmation tokens and input validation.
No phone needed: subprocess calls are stubbed.   Run:  .venv/bin/python -m pytest -q
"""
import importlib
import sys
import time
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent))


@pytest.fixture()
def srv(tmp_path, monkeypatch):
    monkeypatch.setenv("ASSISTANT_STATE", str(tmp_path))
    import server
    server = importlib.reload(server)
    calls = []

    def fake_run(cmd, timeout=30, inp=None):
        calls.append(cmd)
        return 0, "ok"

    monkeypatch.setattr(server, "_run", fake_run)
    monkeypatch.setattr(server.shutil, "which", lambda name: "/usr/bin/" + name)
    monkeypatch.setattr(server.time, "sleep", lambda s: None)
    server.calls = calls
    return server


def _token(msg: str) -> str:
    return msg.split('confirm="')[1].split('"')[0]


def test_lights_publish_without_confirmation(srv):
    assert srv.mqtt_publish("mia/lights/mood", "calm") == "published to mia/lights/mood"
    assert srv.calls[-1][:2] == ["mosquitto_pub", "-h"]


def test_other_mia_topic_needs_confirmation_then_publishes(srv):
    first = srv.mqtt_publish("mia/car/climate", "22")
    assert first.startswith("NEEDS CONFIRMATION") and not srv.calls
    assert srv.mqtt_publish("mia/car/climate", "22", confirm=_token(first)) == "published to mia/car/climate"


def test_token_is_single_use_and_bound_to_params(srv):
    tok = _token(srv.mqtt_publish("mia/car/lock", "on"))
    assert "invalid" in srv.mqtt_publish("mia/car/lock", "off", confirm=tok)  # different payload
    assert "invalid" in srv.mqtt_publish("mia/car/lock", "on", confirm=tok)   # already consumed


def test_token_expires(srv, monkeypatch):
    tok = _token(srv.run_tasker_task("Claude Listen"))
    real = time.time
    monkeypatch.setattr(srv.time, "time", lambda: real() + srv.CONFIRM_TTL + 1)
    assert "invalid" in srv.run_tasker_task("Claude Listen", confirm=tok)


def test_token_survives_new_process(srv):
    """claude -p starts a new server per voice turn: tokens must persist on disk."""
    tok = _token(srv.run_tasker_task("Claude Listen"))
    srv2 = importlib.reload(srv)
    srv2._run = lambda cmd, timeout=30, inp=None: (0, "ok")
    assert srv2.run_tasker_task("Claude Listen", confirm=tok) == "started Tasker task 'Claude Listen'"


def test_disallowed_topic_refused(srv):
    assert srv.mqtt_publish("homeassistant/lock/front/set", "UNLOCK").startswith("topic not allowed")
    assert not srv.calls


def test_tasker_name_is_shell_quoted(srv):
    tok = _token(srv.run_tasker_task("x'; reboot; '"))
    srv.run_tasker_task("x'; reboot; '", confirm=tok)
    shell_cmd = srv.calls[-1][-1]
    assert "'x'\\''; reboot; '\\'''" in shell_cmd


@pytest.mark.parametrize("bad", ["foo bar", "rm -rf /", "a;b", ""])
def test_open_rejects_non_package_non_url(srv, bad):
    assert srv.open(bad) == "target must be a package name or a URL"


def test_act_rejects_unknown_key_and_action(srv):
    assert srv.act("key", "reboot").startswith("unknown key")
    assert srv.act("sh", "id").startswith("unknown action")


PS_OUT = """\
  PID  PPID    STIME     ELAPSED NAME                        ARGS
    1     0 07:03:58    11:32:10 init                        init second_stage
16471     1 18:22:24       18:50 pulseaudio                  pulseaudio -n --dl-search-path=/data/local/tmp/pa/mod -F /data/local/tmp/pa/mic.pa
18064     1 18:35:07       06:07 adbd                        adbd --root_seclabel=u:r:su:s0
20001 18064 18:40:00       00:01 pulseaudio                  pulseaudio --start
"""


def _status_with_ps(srv, monkeypatch, ps_out, ps_rc=0):
    def fake_run(cmd, timeout=30, inp=None):
        srv.calls.append(cmd)
        if cmd[-1] == srv.PS_CMD:
            return ps_rc, ps_out
        return 0, "ok"
    monkeypatch.setattr(srv, "_run", fake_run)
    return srv.phone_status().splitlines()


def test_phone_status_reports_mic_server_and_adbd(srv, monkeypatch):
    lines = _status_with_ps(srv, monkeypatch, PS_OUT)
    assert "mic-server: pid 16471 ppid 1 uptime 18:50 (started 18:22:24)" in lines
    assert "adbd: pid 18064 started 18:35:07 (uptime 06:07)" in lines
    assert ["adb", "-s", srv.ADB_SERIAL, "shell", srv.PS_CMD] in srv.calls


def test_phone_status_warns_when_mic_server_is_adbd_child(srv, monkeypatch):
    ps = PS_OUT.replace("16471     1 18:22:24", "16471 18064 18:22:24")
    mic = [l for l in _status_with_ps(srv, monkeypatch, ps) if l.startswith("mic-server:")]
    assert mic and "ppid 18064" in mic[0] and "WARNING: child of adbd" in mic[0]


def test_phone_status_without_processes_or_ps(srv, monkeypatch):
    lines = _status_with_ps(srv, monkeypatch, PS_OUT.splitlines()[0] + "\n")
    assert "mic-server: no process" in lines and "adbd: no process" in lines
    assert any(l.startswith("processes: ps failed") for l in _status_with_ps(srv, monkeypatch, "err", ps_rc=1))


def test_parse_ps_ignores_header_and_short_lines(srv):
    rows = srv._parse_ps(PS_OUT + "garbage\n")
    assert [r["pid"] for r in rows] == [1, 16471, 18064, 20001]
    assert rows[2] == {"pid": 18064, "ppid": 1, "stime": "18:35:07", "elapsed": "06:07",
                       "name": "adbd", "args": "adbd --root_seclabel=u:r:su:s0"}
