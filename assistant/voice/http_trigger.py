#!/usr/bin/env python3
"""Local HTTP trigger for the voice assistant, so Tasker can call it with its built-in
HTTP Request action (no Termux:Tasker plugin needed).

  GET  /health                         -> "ok"
  POST /ask   [?new=1]                 -> body: {"audio": "/sdcard/VoiceAssistant/in.m4a"}
                                                 or {"text": "question"}
                                          reply (text/plain): path of the reply mp3
  POST /listen [?new=1]                -> hands-free turn on the phone mic + speaker
                                          (listen.sh); reply: the spoken answer as text
  POST /ask, /listen with ?async=1     -> 202 "started" at once (or 503 busy); the turn runs
                                          in the background, GET /status shows how it went
  /ask body {"speak": true}            -> play the reply on the phone speaker (assistant.sh --speak)
  GET  /status                         -> JSON {phase, busy, heard, reply, last, session_age_s}
                                          phase: idle | listening | thinking | speaking
  POST /stop                           -> end the running turn (its process group): "stopped" | "idle"

Claude Panel router (../router, mcp-prompts templates -> Claude Code here -> draft PR):
  POST /dictate?async=1                -> record + speech-to-text only (listen.sh --transcribe-only);
                                          the transcript becomes the draft's request
  GET  /templates, GET /repos          -> JSON lists for the panel's dropdowns
  POST /draft {transcript?, template?, repo?} -> the re-filled draft (JSON)
  POST /dispatch                       -> 202: router agent, then Claude Code in a worktree of the
                                          repo -> a pushed branch + public draft PR (or a spoken
                                          assistant turn); see /status
  GET  /status also has draft, dispatch and jobs; a finished job is spoken + notified

Listens on 127.0.0.1 only. Every request except /health needs header X-Token with the
value in state/http-token (created on first start, mode 600). One turn runs at a time:
the lock (turnlock.py, state/turn.lock) is shared with the MCP tools speak/listen.
"""
import json, os, secrets, signal, subprocess, sys, threading, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)  # started with python -I: the script dir is not on sys.path
from turnlock import turn_lock  # noqa: E402
ROUTER_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..", "router")
sys.path.insert(0, os.path.normpath(ROUTER_DIR))
import router  # noqa: E402
PORT = int(os.environ.get("VA_HTTP_PORT", "8780"))
AUDIO_DIR = os.path.realpath(os.environ.get("VA_OUT_DIR", "/sdcard/VoiceAssistant"))
STATE = os.environ.get("VA_STATE_DIR") or os.path.join(HERE, "state")
LOGS = os.environ.get("VA_LOG_DIR") or os.path.join(HERE, "logs")
TOKEN_FILE = os.path.join(STATE, "http-token")
SESSION_FILE = os.path.join(STATE, "session")
TIMEOUT = 300
STOP_GRACE = 2.0     # seconds between SIGTERM and SIGKILL on /stop
TEXT_CAP = 2000      # characters of heard/reply in /status

# The running turn: its process group, so /stop ends claude, rec, play ... and nothing else.
_turn_mu = threading.Lock()
_turn = {"pgid": None}
_last = {"result": None, "code": None, "at": None}   # outcome of the latest turn
_dispatch = {"state": "idle", "result": "", "at": None}  # idle | routing | working | done | failed
_dispatch_mu = threading.Lock()
ADB_SERIAL = os.environ.get("DROID_SERIAL", "127.0.0.1:5555")


def phase_file():
    """Read per call (VA_PHASE_FILE), so tests and scripts agree on the path."""
    return os.environ.get("VA_PHASE_FILE") or os.path.join(STATE, "phase")


def set_phase(name):
    path = phase_file()
    try:
        tmp = f"{path}.{os.getpid()}"
        with open(tmp, "w") as f:
            f.write(name + "\n")
        os.replace(tmp, path)
    except OSError:
        pass


def read_phase():
    try:
        with open(phase_file()) as f:
            return f.read().strip() or "idle"
    except OSError:
        return "idle"


def lock_busy():
    """True while any voice turn (this server, or the MCP speak/listen tools) holds the lock."""
    with turn_lock(0) as got:
        return not got


def last_exchange(path=None, window=32768):
    """The latest `heard [..]: ...` and `reply: ...` from assistant.log (tail only)."""
    path = path or os.path.join(LOGS, "assistant.log")
    heard = reply = ""
    try:
        with open(path, "rb") as f:
            f.seek(0, os.SEEK_END)
            f.seek(max(0, f.tell() - window))
            lines = f.read().decode("utf-8", "replace").splitlines()
    except OSError:
        return heard, reply
    for line in reversed(lines):
        body = line[20:] if len(line) > 20 and line[4] == "-" and line[19] == " " else line
        if not reply and body.startswith("reply: "):
            reply = body[7:]
        elif not heard and body.startswith("heard ["):
            heard = body.split("]: ", 1)[-1] if "]: " in body else ""
        if heard and reply:
            break
    return heard[:TEXT_CAP], reply[:TEXT_CAP]


def status():
    running = _turn["pgid"] is not None
    busy = running or lock_busy()
    phase = read_phase()
    if not busy and phase != "idle":   # a turn died without its exit trap
        phase = "idle"
    heard, reply = last_exchange()
    try:
        age = int(time.time() - os.path.getmtime(SESSION_FILE))
    except OSError:
        age = None
    d = router.draft()
    d["filled"] = d.get("filled", "")[:600]
    jobs = [dict(j, line=router.summary(j)) for j in router.jobs()[-3:]]
    return {"phase": phase, "busy": busy, "heard": heard, "reply": reply,
            "last": dict(_last), "session_age_s": age, "draft": d,
            "dispatch": dict(_dispatch), "jobs": jobs}


def dictate_args():
    return [os.path.join(HERE, "listen.sh"), "--transcribe-only"]


def finish_dictation(p, err):
    """After listen.sh --transcribe-only: the transcript goes into the draft."""
    if err or p is None:
        return (503 if err == "busy" else 504), err or "failed"
    if p.returncode == 3:
        return 200, "no speech heard"
    if p.returncode != 0:
        return summarize(p, None, True)
    text = (p.stdout.strip().splitlines() or [""])[-1]
    router.update_draft(transcript=text)
    return 200, "dictated: " + text


def set_dispatch(state, result=""):
    _dispatch.update(state=state, result=result[:TEXT_CAP], at=int(time.time()))


def dispatch():
    """Route the draft and act on it. Runs in a thread; the outcome lands in /status dispatch."""
    try:
        d = router.draft()
        set_dispatch("routing")
        where, reason = router.route(d)
        if where == "coding-agent":
            job = router.new_job(d)
            set_dispatch("working", f"{job['id']}: Claude Code working in {d['repo']} ({reason})")
            job = router.run_job(d, job)
            set_dispatch("failed" if job["state"] == "failed" else "done", router.summary(job))
            announce(job)
        elif where == "phone-assistant":
            set_dispatch("done", f"asking the phone assistant ({reason})")
            p, err = run_turn([os.path.join(HERE, "assistant.sh"), "--speak", "--text", d["transcript"]],
                              lock_timeout=5)
            record(*summarize(p, err, False))
        else:
            set_dispatch("failed", f"not sent: {reason}")
    except (router.AgentError, ValueError, KeyError, OSError, subprocess.SubprocessError) as e:
        set_dispatch("failed", str(e))
    finally:
        _dispatch_mu.release()


def notify(title, text):
    q = lambda v: "'" + v.replace("'", "'\\''") + "'"  # noqa: E731 - adb shell quoting
    tag = "claude-panel-" + secrets.token_hex(2)
    subprocess.run(["adb", "-s", ADB_SERIAL, "shell",
                    f"cmd notification post -S bigtext -t {q(title[:80])} {tag} {q(text[:1000])}"],
                   capture_output=True, timeout=20)


def announce(job):
    line, title, text = router.announce(job)
    try:
        notify(title, text)
    except (OSError, subprocess.SubprocessError):
        pass
    run_turn([os.path.join(os.path.normpath(ROUTER_DIR), "say.sh"), line], lock_timeout=120)


def run_turn(args, lock_timeout=None):
    """Run one voice turn under the shared lock. Returns (CompletedProcess, None) or (None, err)."""
    with turn_lock(TIMEOUT if lock_timeout is None else lock_timeout) as got:
        if not got:
            return None, "busy"
        # The child knows we hold the lock (no deadlock in listen.sh / assistant.sh --speak)
        # and gets its own process group, so a timeout or /stop also ends claude, play, rec ...
        p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
                             env={**os.environ, "VA_TURN_LOCKED": "1"}, start_new_session=True)
        with _turn_mu:
            _turn["pgid"] = p.pid
        try:
            out, err = p.communicate(timeout=TIMEOUT)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(p.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            p.communicate()
            return None, "assistant timed out"
        finally:
            with _turn_mu:
                _turn["pgid"] = None
            set_phase("idle")
        return subprocess.CompletedProcess(args, p.returncode, out, err), None


def stop_turn():
    """SIGTERM the running turn's process group, SIGKILL it after STOP_GRACE s if still there."""
    with _turn_mu:
        pgid = _turn["pgid"]
    if pgid is None:
        return "idle"
    try:
        os.killpg(pgid, signal.SIGTERM)
    except ProcessLookupError:
        return "idle"

    def reap():
        time.sleep(STOP_GRACE)
        with _turn_mu:
            same = _turn["pgid"] == pgid
        if same:
            try:
                os.killpg(pgid, signal.SIGKILL)
            except ProcessLookupError:
                pass
    threading.Thread(target=reap, daemon=True).start()
    set_phase("idle")
    return "stopped"


def summarize(p, err, listen):
    """(http code, text) for a finished turn, shared by the sync reply and /status `last`."""
    if err:
        return (503 if err == "busy" else 504), err
    lines = p.stdout.strip().splitlines()
    if p.returncode == 0 and (lines or listen):
        return 200, (lines or [""])[-1]
    if listen and p.returncode == 3:
        return 200, "no speech heard"
    if p.returncode < 0:
        return 200, "stopped"
    return 500, (p.stderr.strip().splitlines() or ["assistant failed"])[-1]


def record(code, text):
    _last.update(result=text[:TEXT_CAP], code=code, at=int(time.time()))


def load_token():
    os.makedirs(os.path.dirname(TOKEN_FILE), exist_ok=True)
    if not os.path.exists(TOKEN_FILE):
        fd = os.open(TOKEN_FILE, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as f:
            f.write(secrets.token_urlsafe(24))
    with open(TOKEN_FILE) as f:
        return f.read().strip()


TOKEN = load_token()


class Handler(BaseHTTPRequestHandler):
    def reply(self, code, text, ctype="text/plain"):
        body = (text + "\n").encode()
        try:
            self.send_response(code)
            self.send_header("Content-Type", f"{ctype}; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            # Tasker's fire-and-forget `nc -q 1` hangs up before a turn ends.
            pass

    def authorized(self):
        if secrets.compare_digest(self.headers.get("X-Token", ""), TOKEN):
            return True
        self.reply(403, "bad token")
        return False

    def json_body(self):
        """The request body as a JSON object, or None after replying 400."""
        try:
            n = int(self.headers.get("Content-Length", "0"))
            data = json.loads(self.rfile.read(min(n, 65536)) or b"{}")
        except (ValueError, json.JSONDecodeError):
            self.reply(400, "body must be JSON")
            return None
        if not isinstance(data, dict):
            self.reply(400, "body must be a JSON object")
            return None
        return data

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/health":
            return self.reply(200, "ok")
        if path in ("/status", "/templates", "/repos"):
            if not self.authorized():
                return
            try:
                data = {"/status": status, "/templates": router.templates, "/repos": router.repos}[path]()
            except (OSError, ValueError) as e:
                return self.reply(500, f"{path[1:]}: {e}")
            return self.reply(200, json.dumps(data, ensure_ascii=False), "application/json")
        self.reply(404, "not found")

    def do_POST(self):
        url = urlparse(self.path)
        if url.path not in ("/ask", "/listen", "/stop", "/dictate", "/draft", "/dispatch"):
            return self.reply(404, "not found")
        if not self.authorized():
            return
        if url.path == "/stop":
            return self.reply(200, stop_turn())
        query = parse_qs(url.query)
        if url.path == "/draft":
            data = self.json_body()
            if data is None:
                return
            try:
                d = router.update_draft(transcript=data.get("transcript"), template=data.get("template"),
                                        repo_name=data.get("repo"))
            except (ValueError, KeyError) as e:
                return self.reply(400, str(e))
            except OSError as e:
                return self.reply(500, f"draft: {e}")
            return self.reply(200, json.dumps(d, ensure_ascii=False), "application/json")
        if url.path == "/dispatch":
            if not _dispatch_mu.acquire(blocking=False):
                return self.reply(503, "busy")
            threading.Thread(target=dispatch, daemon=True).start()
            return self.reply(202, "started")
        if url.path == "/dictate":
            if _turn["pgid"] is not None or lock_busy():
                return self.reply(503, "busy")
            if query.get("async") == ["1"]:
                def work_d():
                    record(*finish_dictation(*run_turn(dictate_args(), lock_timeout=2)))
                threading.Thread(target=work_d, daemon=True).start()
                return self.reply(202, "started")
            code, text = finish_dictation(*run_turn(dictate_args()))
            record(code, text)
            return self.reply(code, text)
        listen = url.path == "/listen"
        if listen:  # hands-free: record on the phone mic, speak the reply
            args = [os.path.join(HERE, "listen.sh")]
            if query.get("new") == ["1"]:
                args.append("--new")
        else:
            data = self.json_body()
            if data is None:
                return
            args = [os.path.join(HERE, "assistant.sh")]
            if data.get("speak") is True:
                args.append("--speak")
            if query.get("new") == ["1"] or data.get("new"):
                args.append("--new")
            if isinstance(data.get("text"), str) and data["text"].strip():
                args += ["--text", data["text"].strip()[:4000]]
            elif isinstance(data.get("audio"), str):
                audio = os.path.realpath(data["audio"])
                if not audio.startswith(AUDIO_DIR + os.sep):
                    return self.reply(400, f"audio must be under {AUDIO_DIR}")
                args.append(audio)
            else:
                return self.reply(400, 'need "audio" or "text"')
        if query.get("async") == ["1"]:
            if _turn["pgid"] is not None or lock_busy():
                return self.reply(503, "busy")

            def work():
                p, err = run_turn(args, lock_timeout=2)
                record(*summarize(p, err, listen))
            threading.Thread(target=work, daemon=True).start()
            return self.reply(202, "started")
        p, err = run_turn(args)
        code, text = summarize(p, err, listen)
        record(code, text)
        self.reply(code, text)

    def log_message(self, fmt, *a):
        with open(os.path.join(LOGS, "http.log"), "a") as f:
            f.write("%s %s\n" % (self.log_date_time_string(), fmt % a))


if __name__ == "__main__":
    os.makedirs(LOGS, exist_ok=True)
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
