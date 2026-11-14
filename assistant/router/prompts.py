"""mcp-prompts access for the Claude Panel: talk JSON-RPC over stdio to the same mcp-prompts
server the session uses (no SDK needed: the trigger runs under mia-venv), and fall back to
reading data/prompts/**/*.json with the same plain {{var}} replacement if it can't start."""
import glob, json, os, subprocess

PROMPTS_DIR = os.environ.get("PROMPTS_DIR", "/root/mcp-prompts/data/prompts")
SERVER = os.environ.get("MCP_PROMPTS_BIN", "/usr/local/bin/mcp-prompts")


class Server:
    def __init__(self, timeout=20):
        self.p = subprocess.Popen([SERVER], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                  stderr=subprocess.DEVNULL, text=True,
                                  env={**os.environ, "PROMPTS_DIR": PROMPTS_DIR})
        self.n = 0
        self.rpc("initialize", {"protocolVersion": "2025-06-18", "capabilities": {},
                                "clientInfo": {"name": "claude-panel", "version": "1"}})
        self._send({"jsonrpc": "2.0", "method": "notifications/initialized"})

    def _send(self, m):
        self.p.stdin.write(json.dumps(m) + "\n")
        self.p.stdin.flush()

    def rpc(self, method, params):
        self.n += 1
        self._send({"jsonrpc": "2.0", "id": self.n, "method": method, "params": params})
        while True:
            line = self.p.stdout.readline()
            if not line:
                raise OSError("mcp-prompts exited")
            try:
                m = json.loads(line)
            except json.JSONDecodeError:
                continue   # log lines
            if m.get("id") == self.n:
                if "error" in m or m.get("result", {}).get("isError"):
                    raise OSError(f"{method}: {m.get('error') or m['result']}")
                return m["result"]

    def call(self, tool, args):
        return self.rpc("tools/call", {"name": tool, "arguments": args})["content"][0]["text"]

    def close(self):
        self.p.kill()
        self.p.wait()


def _files():
    out = {}
    for f in glob.glob(os.path.join(PROMPTS_DIR, "**", "*.json"), recursive=True):
        try:
            d = json.load(open(f))
        except (OSError, ValueError):
            continue
        if isinstance(d, dict) and "id" in d:
            out[d["id"]] = d
    return out


def fill(content, variables):
    for k, v in variables.items():
        content = content.replace("{{" + k + "}}", str(v))
    return content


def get(pid):
    """The prompt as a dict (server first, files as fallback)."""
    try:
        s = Server()
        try:
            return json.loads(s.call("get_prompt", {"id": pid}))
        finally:
            s.close()
    except (OSError, ValueError, KeyError):
        d = _files().get(pid)
        if d is None:
            raise KeyError(pid)
        return d


def templates(tag="panel"):
    """[{id, name, variables}] of template prompts carrying `tag` (from the JSON files the
    server also loads; list_prompts answers in Markdown, so files are the stable source)."""
    return sorted(({"id": d["id"], "name": d.get("name", d["id"]), "variables": d.get("variables", [])}
                   for d in _files().values() if tag in d.get("tags", []) and d.get("isTemplate")),
                  key=lambda t: t["id"])


def apply(pid, variables):
    try:
        s = Server()
        try:
            return s.call("apply_template", {"id": pid, "variables": variables})
        finally:
            s.close()
    except (OSError, ValueError, KeyError):
        d = _files().get(pid)
        if d is None:
            raise KeyError(pid)
        return fill(d["content"], variables)
