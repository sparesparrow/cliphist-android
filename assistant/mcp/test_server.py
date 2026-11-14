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


PS_HEAD = "  PID  PPID USER         LABEL                          STIME     ELAPSED ARGS"
MIC = "16471     1 shell        u:r:magisk:s0                  18:22:23       41:07 pulseaudio -n --dl-search-path=/data/local/tmp/pa/mod -F /data/local/tmp/pa/mic.pa"
ADBD = "20461     1 shell        u:r:adbd:s0                    18:45:33       17:57 adbd --root_seclabel=u:r:su:s0"
INIT = "    1     0 root         u:r:init:s0                    07:03:59    11:59:32 init second_stage"
KTHREAD = "    2     0 root         u:r:kernel:s0                  07:03:59    11:59:32 [kthreadd]"
BIN = "/data/local/tmp/pa/bin/pulseaudio"


def _ps(*rows, exes=None):
    exes = {16471: BIN} if exes is None else exes
    return "\n".join([PS_HEAD, *rows, "---exe---", *(f"{p} {e}" for p, e in exes.items())]) + "\n"


def _status_with_ps(srv, monkeypatch, ps_out, ps_rc=0):
    def fake_run(cmd, timeout=30, inp=None):
        srv.calls.append(cmd)
        if cmd[-1] == srv.PS_CMD:
            return ps_rc, ps_out
        return 0, "ok"
    monkeypatch.setattr(srv, "_run", fake_run)
    return srv.phone_status().splitlines()


def _mic(lines):
    return [l for l in lines if l.startswith("mic-server:")]


def test_phone_status_reports_mic_server_and_adbd(srv, monkeypatch):
    lines = _status_with_ps(srv, monkeypatch, _ps(INIT, KTHREAD, MIC, ADBD))
    assert ("mic-server: pid 16471 ppid 1 user shell label u:r:magisk:s0 uptime 41:07 "
            "(started 18:22:23)") in lines
    assert "adbd: pid 20461 started 18:45:33 (uptime 17:57)" in lines
    assert ["adb", "-s", srv.ADB_SERIAL, "shell", srv.PS_CMD] in srv.calls
    assert "ps -A -w -o PID,PPID,USER,LABEL,STIME,ELAPSED,ARGS" in srv.PS_CMD


def test_phone_status_warns_when_mic_server_is_adb_spawned(srv, monkeypatch):
    # Reparented to PPID 1 but still in the shell domain: dies on the next adbd restart.
    mic = _mic(_status_with_ps(srv, monkeypatch, _ps(MIC.replace("u:r:magisk:s0", "u:r:shell:s0 "), ADBD)))
    assert len(mic) == 1 and "ppid 1" in mic[0] and "WARNING: adb-spawned (u:r:shell:s0)" in mic[0]


def test_phone_status_warns_on_unexpected_user(srv, monkeypatch):
    mic = _mic(_status_with_ps(srv, monkeypatch, _ps(MIC.replace("shell   ", "u0_a525 "), ADBD)))
    assert "WARNING: unexpected user u0_a525" in mic[0]


def test_phone_status_warns_on_two_mic_servers(srv, monkeypatch):
    second = MIC.replace("16471", "16999")
    mic = _mic(_status_with_ps(srv, monkeypatch, _ps(MIC, second, ADBD, exes={16471: BIN, 16999: BIN})))
    assert len(mic) == 3 and mic[-1] == f"mic-server: WARNING: 2 processes run {BIN}"


def test_spoofed_pulseaudio_is_not_the_mic_server(srv, monkeypatch):
    # Named pulseaudio, path only in later args, real exe elsewhere: must not match.
    spoof = ("16471     1 u0_a525      u:r:untrusted_app:s0           18:22:23       41:07 "
             "pulseaudio -F /data/local/tmp/pa/bin/pulseaudio")
    lines = _status_with_ps(srv, monkeypatch, _ps(spoof, ADBD, exes={16471: "/data/data/x/pulseaudio"}))
    assert _mic(lines) == ["mic-server: no process"]


def test_adbd_matched_by_argv0_only(srv, monkeypatch):
    fake = ADBD.replace("adbd --root", "sh -c adbd --root")
    assert "adbd: no process" in _status_with_ps(srv, monkeypatch, _ps(MIC, fake))
    apex = ADBD.replace("adbd --root", "/apex/com.android.adbd/bin/adbd --root")
    assert "adbd: pid 20461 started 18:45:33 (uptime 17:57)" in _status_with_ps(srv, monkeypatch, _ps(MIC, apex))


def test_phone_status_without_processes_or_ps(srv, monkeypatch):
    lines = _status_with_ps(srv, monkeypatch, _ps(exes={}))
    assert "mic-server: no process" in lines and "adbd: no process" in lines
    assert any(l.startswith("processes: ps failed") for l in _status_with_ps(srv, monkeypatch, "err", ps_rc=1))


def test_parse_ps_tolerates_odd_rows(srv):
    rows, exes = srv._parse_ps(_ps(
        INIT, KTHREAD,
        "  777  ???  root  u:r:x:s0  07:03:59  01:00 bad-ppid",          # non-numeric PPID: skipped
        "  abc    1  root  u:r:x:s0  07:03:59  01:00 bad-pid",            # non-numeric PID: skipped
        "  888    1  root  u:r:x:s0  Nov 07    1-02:03:04 old --day",     # STIME > 24 h, day ELAPSED
        "garbage",
    ))
    assert [r["pid"] for r in rows] == [1, 2, 888]
    assert rows[1]["argv0"] == "[kthreadd]"
    assert rows[2] == {"pid": 888, "ppid": 1, "user": "root", "label": "u:r:x:s0", "stime": "Nov 07",
                       "elapsed": "1-02:03:04", "argv0": "old", "args": "old --day"}
    assert exes == {16471: BIN}
