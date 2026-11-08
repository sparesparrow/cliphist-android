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
