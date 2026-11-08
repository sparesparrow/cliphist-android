#!/usr/bin/env python3
"""Local HTTP trigger for the voice assistant, so Tasker can call it with its built-in
HTTP Request action (no Termux:Tasker plugin needed).

  GET  /health                         -> "ok"
  POST /ask   [?new=1]                 -> body: {"audio": "/sdcard/VoiceAssistant/in.m4a"}
                                                 or {"text": "question"}
                                          reply (text/plain): path of the reply mp3
  POST /listen [?new=1]                -> hands-free turn on the phone mic + speaker
                                          (listen.sh); reply: the spoken answer as text

Listens on 127.0.0.1 only. Every request except /health needs header X-Token with the
value in state/http-token (created on first start, mode 600). One request runs at a time.
"""
import json, os, secrets, subprocess, threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

HERE = os.path.dirname(os.path.abspath(__file__))
PORT = int(os.environ.get("VA_HTTP_PORT", "8780"))
AUDIO_DIR = os.path.realpath(os.environ.get("VA_OUT_DIR", "/sdcard/VoiceAssistant"))
TOKEN_FILE = os.path.join(HERE, "state", "http-token")
TIMEOUT = 300
LOCK = threading.Lock()


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
    def reply(self, code, text):
        body = (text + "\n").encode()
        self.send_response(code)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if urlparse(self.path).path == "/health":
            return self.reply(200, "ok")
        self.reply(404, "not found")

    def run_locked(self, args):
        if not LOCK.acquire(timeout=TIMEOUT):
            return None, "busy"
        try:
            return subprocess.run(args, capture_output=True, text=True, timeout=TIMEOUT), None
        except subprocess.TimeoutExpired:
            return None, "assistant timed out"
        finally:
            LOCK.release()

    def do_POST(self):
        url = urlparse(self.path)
        if url.path not in ("/ask", "/listen"):
            return self.reply(404, "not found")
        if not secrets.compare_digest(self.headers.get("X-Token", ""), TOKEN):
            return self.reply(403, "bad token")
        if url.path == "/listen":  # hands-free: record on the phone mic, speak the reply
            args = [os.path.join(HERE, "listen.sh")]
            if parse_qs(url.query).get("new") == ["1"]:
                args.append("--new")
            p, err = self.run_locked(args)
            if err:
                return self.reply(503 if err == "busy" else 504, err)
            out = (p.stdout.strip().splitlines() or [""])[-1]
            if p.returncode == 0:
                return self.reply(200, out)
            if p.returncode == 3:
                return self.reply(200, "no speech heard")
            return self.reply(500, (p.stderr.strip().splitlines() or ["listen failed"])[-1])
        try:
            n = int(self.headers.get("Content-Length", "0"))
            data = json.loads(self.rfile.read(min(n, 65536)) or b"{}")
        except (ValueError, json.JSONDecodeError):
            return self.reply(400, "body must be JSON")
        args = [os.path.join(HERE, "assistant.sh")]
        if parse_qs(url.query).get("new") == ["1"] or data.get("new"):
            args.append("--new")
        if isinstance(data.get("text"), str) and data["text"].strip():
            args += ["--text", data["text"].strip()]
        elif isinstance(data.get("audio"), str):
            audio = os.path.realpath(data["audio"])
            if not audio.startswith(AUDIO_DIR + os.sep):
                return self.reply(400, f"audio must be under {AUDIO_DIR}")
            args.append(audio)
        else:
            return self.reply(400, 'need "audio" or "text"')
        p, err = self.run_locked(args)
        if err:
            return self.reply(503 if err == "busy" else 504, err)
        out = p.stdout.strip().splitlines()
        if p.returncode == 0 and out:
            return self.reply(200, out[-1])
        err = (p.stderr.strip().splitlines() or ["assistant failed"])[-1]
        self.reply(500, err)

    def log_message(self, fmt, *a):
        with open(os.path.join(HERE, "logs", "http.log"), "a") as f:
            f.write("%s %s\n" % (self.log_date_time_string(), fmt % a))


if __name__ == "__main__":
    os.makedirs(os.path.join(HERE, "logs"), exist_ok=True)
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
