"""Claude Panel router: the draft (dictated request + mcp-prompts template + repo), the router
agent that decides where it goes, and the local coding agent.

  draft   state/draft.json  {transcript, template, repo, slug, filled, updated}
  route   panel-router prompt -> `claude -p --model haiku` (no tools, 150 s) -> ROUTE / REASON
  agent   Claude Code here in PRoot (the owner's own login, no API key): a git worktree of the
          repo on branch panel/<stamp>, `claude -p` with file tools only (it cannot commit,
          push or reach the phone), then this module commits, pushes and opens a DRAFT PR.
          panel-repo-question only answers (read-only tools, no branch is pushed).
  jobs    state/jobs.json: working -> pr | answered | no-changes | failed

Every external command goes through `run` (argv lists, never a shell), so tests inject fakes.
The repos are public: the branch and draft PR are published when the agent has finished.
"""
import json, os, re, subprocess, time

import prompts

HERE = os.path.dirname(os.path.abspath(__file__))
REPOS_FILE = os.environ.get("PANEL_REPOS_FILE") or os.path.join(HERE, "repos.json")
ROUTES = ("coding-agent", "phone-assistant", "none")
ROUTER_PROMPT = "panel-router"
DEFAULT_TEMPLATE = "panel-coding-task"
ANSWER_TEMPLATES = ("panel-repo-question",)
WORK_ROOT = os.environ.get("PANEL_WORK_DIR", "/root/panel-work")
AGENT_TIMEOUT = int(os.environ.get("PANEL_AGENT_TIMEOUT", "1800"))
AGENT_MODEL = os.environ.get("PANEL_AGENT_MODEL", "")      # empty: Claude Code's default
GIT_ID = ["-c", "user.name=sparesparrow", "-c", "user.email=72402690+sparesparrow@users.noreply.github.com"]
READ_TOOLS = ["Read", "Glob", "Grep", "Bash(git status:*)", "Bash(git diff:*)", "Bash(git log:*)",
              "Bash(git show:*)", "Bash(ls:*)"]
EDIT_TOOLS = READ_TOOLS + ["Edit", "Write", "Bash(python3 -m pytest:*)", "Bash(pytest:*)",
                           "Bash(npm test:*)", "Bash(pnpm test:*)", "Bash(./gradlew test:*)"]
SECRET_NAMES = re.compile(r"(^|/)(\.env[^/]*|.*cookie.*|.*token.*|.*\.pem|.*\.key|id_[a-z0-9]+)$|(^|/)(state|logs)/", re.I)
TRANSCRIPT_CAP = 4000
_run = subprocess.run


def state_dir():
    return os.environ.get("VA_STATE_DIR") or os.path.join(os.path.dirname(HERE), "voice", "state")


def _path(name):
    return os.path.join(state_dir(), name)


def _load(name, default):
    try:
        with open(_path(name)) as f:
            return json.load(f)
    except (OSError, ValueError):
        return default


def _save(name, data):
    os.makedirs(state_dir(), exist_ok=True)
    tmp = _path(f"{name}.{os.getpid()}")
    with open(tmp, "w") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    os.replace(tmp, _path(name))


# --- repos and templates ----------------------------------------------------
def repos():
    with open(REPOS_FILE) as f:
        return json.load(f)


def repo(name):
    return next((r for r in repos() if r["name"] == name), None)


def templates():
    """Panel templates the user can pick (the router's own prompt is not one of them)."""
    return [t for t in prompts.templates("panel") if t["id"] != ROUTER_PROMPT]


# --- draft ------------------------------------------------------------------
def draft():
    d = _load("draft.json", {})
    d.setdefault("transcript", "")
    d.setdefault("template", DEFAULT_TEMPLATE)
    d.setdefault("repo", "")
    d.setdefault("slug", "")
    d.setdefault("filled", "")
    return d


def update_draft(transcript=None, template=None, repo_name=None):
    """Change any of the three inputs and re-fill the template. Raises ValueError on an
    unknown template or repo (nothing is saved then)."""
    d = draft()
    if transcript is not None:
        d["transcript"] = " ".join(str(transcript).split())[:TRANSCRIPT_CAP]
    if template is not None:
        if template not in {t["id"] for t in templates()}:
            raise ValueError(f"unknown template: {template}")
        d["template"] = template
    if repo_name is not None:
        r = repo(repo_name)
        if r is None:
            raise ValueError(f"unknown repo: {repo_name}")
        d["repo"], d["slug"] = r["name"], r["slug"]
    d["filled"] = ""
    if d["transcript"] and d["template"] and d["repo"]:
        d["filled"] = prompts.apply(d["template"], {"request": d["transcript"], "repo": d["repo"]})
    d["updated"] = int(time.time())
    _save("draft.json", d)
    return d


# --- router agent -----------------------------------------------------------
def parse_route(text):
    m = re.search(r"^\s*ROUTE:\s*([a-z-]+)", text or "", re.M)
    reason = re.search(r"^\s*REASON:\s*(.+)$", text or "", re.M)
    r = m.group(1) if m and m.group(1) in ROUTES else None
    return r, (reason.group(1).strip() if reason else "")


ROUTE_TIMEOUT = int(os.environ.get("PANEL_ROUTE_TIMEOUT", "150"))   # claude -p starts in 30-95 s here


def route(d, run=None, timeout=None):
    """(route, reason). Empty draft -> none without asking; a failed or garbled answer -> none."""
    if not d.get("transcript") or not d.get("filled"):
        return "none", "nothing to send yet"
    text = prompts.apply(ROUTER_PROMPT, {"request": d["transcript"], "repo": d["repo"],
                                         "template": d["template"], "routes": ", ".join(ROUTES)})
    argv = ["claude", "-p", "--model", "haiku", "--tools", "", "--strict-mcp-config",
            "--setting-sources", "project,local"]
    try:
        p = (run or _run)(argv, input=text, capture_output=True, text=True, timeout=timeout or ROUTE_TIMEOUT,
                          cwd=state_dir())
    except subprocess.TimeoutExpired:
        return "none", "router timed out"
    except OSError as e:
        return "none", f"router failed: {e}"
    if p.returncode != 0:
        return "none", "router failed"
    r, reason = parse_route(p.stdout)
    return (r, reason) if r else ("none", "router answer not understood")


# --- local coding agent (Claude Code in PRoot) -------------------------------
class AgentError(Exception):
    pass


def _ok(p, what):
    if p.returncode != 0:
        raise AgentError(f"{what} failed: " + ((p.stderr or p.stdout or "").strip().splitlines() or ["?"])[-1][:200])
    return (p.stdout or "").strip()


def _git(args, run=None, cwd=None, timeout=120):
    return (run or _run)(["git", *GIT_ID, *args], capture_output=True, text=True, timeout=timeout, cwd=cwd)


def _gh(args, run=None, timeout=60, cwd=None):
    return (run or _run)(["gh", *args], capture_output=True, text=True, timeout=timeout, cwd=cwd)


def issue_title(transcript):
    t = transcript.strip().rstrip(".")
    return (t[:67] + "...") if len(t) > 70 else (t or "Voice panel request")


def jobs():
    return _load("jobs.json", [])


def save_jobs(js):
    _save("jobs.json", js[-20:])


def save_job(job):
    js = [j for j in jobs() if j["id"] != job["id"]] + [job]
    save_jobs(js)


def new_job(d, now=None):
    r = repo(d.get("repo", ""))
    if r is None or not d.get("filled"):
        raise AgentError("pick a template and a repo first")
    stamp = time.strftime("%Y%m%d-%H%M%S", time.localtime(now or time.time()))
    job = {"id": f"{r['name']}@{stamp[9:13]}", "repo": r["name"], "slug": r["slug"],
           "template": d["template"], "branch": f"panel/{stamp}",
           "mode": "answer" if d["template"] in ANSWER_TEMPLATES else "change",
           "title": issue_title(d["transcript"]), "created": int(now or time.time()),
           "state": "working", "url": "", "note": ""}
    save_job(job)
    return job


def agent_prompt(d, job):
    if job["mode"] == "answer":
        rules = ("You run headless in a read-only checkout. Do not change files. Answer in plain text; "
                 "start with two sentences that can be read aloud.")
    else:
        rules = (f"You run headless in a git worktree of {d['repo']} on branch {job['branch']}. Edit files "
                 "only: do NOT commit, push or open pull requests; the runner commits your changes, pushes "
                 "and opens a DRAFT pull request. Never touch secrets, .env files, logs or runtime state. "
                 "End your reply with the pull request description: what changed, how it was tested, "
                 "and what is unverified.")
    return d["filled"] + "\n\n## Runner\n" + rules


def checkout(job, run=None):
    """A fresh worktree for the job (from the local clone in repos.json, else a clone in WORK_ROOT)."""
    r = repo(job["repo"])
    src = r.get("path") or os.path.join(WORK_ROOT, "_clones", r["name"])
    if not os.path.isdir(os.path.join(src, ".git")):
        os.makedirs(os.path.dirname(src), exist_ok=True)
        _ok(_gh(["repo", "clone", r["slug"], src, "--", "-q"], run, timeout=600), "clone")
    base = _ok(_gh(["repo", "view", r["slug"], "--json", "defaultBranchRef", "-q", ".defaultBranchRef.name"], run),
               "default branch") or "main"
    _ok(_git(["-C", src, "fetch", "-q", "origin", base], run, timeout=300), "fetch")
    wt = os.path.join(WORK_ROOT, job["branch"].replace("/", "-") + "-" + r["name"])
    _ok(_git(["-C", src, "worktree", "add", "-q", "-b", job["branch"], wt, f"origin/{base}"], run), "worktree")
    job.update(src=src, base=base, worktree=wt)
    return wt


def run_agent(d, job, run=None):
    tools = READ_TOOLS if job["mode"] == "answer" else EDIT_TOOLS
    argv = ["claude", "-p", "--permission-mode", "dontAsk", "--allowedTools", ",".join(tools),
            "--strict-mcp-config", "--setting-sources", "project,local"]
    if AGENT_MODEL:
        argv += ["--model", AGENT_MODEL]
    try:
        p = (run or _run)(argv, input=agent_prompt(d, job), capture_output=True, text=True,
                          timeout=AGENT_TIMEOUT, cwd=job["worktree"])
    except subprocess.TimeoutExpired:
        raise AgentError(f"Claude Code did not finish in {AGENT_TIMEOUT // 60} min")
    return _ok(p, "Claude Code")


def publish(job, summary, run=None):
    """Commit what the agent changed, push the branch, open a draft PR. Returns the PR url, or
    "" when nothing changed. Refuses files that look like secrets or runtime state."""
    wt = job["worktree"]
    changed = [l[3:] for l in _ok(_git(["-C", wt, "status", "--porcelain", "-uall"], run), "status").splitlines() if l]
    if not changed:
        return ""
    bad = [f for f in changed if SECRET_NAMES.search(f.split(" -> ")[-1])]
    if bad:
        raise AgentError("refusing to push files that look like secrets or runtime state: " + ", ".join(bad[:3]))
    _ok(_git(["-C", wt, "add", "-A"], run), "git add")
    msg = (f"{job['title']}\n\nRequested by voice on the Claude Panel ({job['template']}); "
           "written by Claude Code on the phone.\n\nCo-Authored-By: Claude <noreply@anthropic.com>")
    _ok(_git(["-C", wt, "commit", "-q", "-m", msg], run), "git commit")
    _ok(_git(["-C", wt, "push", "-q", "-u", "origin", job["branch"]], run, timeout=300), "git push")
    body = (f"Requested by voice on the Claude Panel (template `{job['template']}`); written by Claude Code "
            f"running locally on the phone.\n\n{summary[-3000:]}\n\n"
            "\U0001F916 Generated with [Claude Code](https://claude.com/claude-code)")
    url = _ok(_gh(["pr", "create", "-R", job["slug"], "--draft", "--base", job["base"], "--head", job["branch"],
                   "--title", job["title"], "--body", body], run, cwd=wt), "gh pr create")
    return url.splitlines()[-1]


def cleanup(job, run=None):
    if job.get("worktree") and job.get("src"):
        _git(["-C", job["src"], "worktree", "remove", "--force", job["worktree"]], run)
        if job.get("state") in ("answered", "no-changes"):
            _git(["-C", job["src"], "branch", "-D", job["branch"]], run)


def run_job(d, job, run=None):
    """The whole local job; updates and saves `job`, never raises."""
    try:
        checkout(job, run)
        out = run_agent(d, job, run)
        if job["mode"] == "answer":
            job.update(state="answered", note=re.sub(r"\s+", " ", out)[:300])
        else:
            url = publish(job, out, run)
            if url:
                job.update(state="pr", url=url, note="draft PR ready")
            else:
                job.update(state="no-changes", note=re.sub(r"\s+", " ", out)[:300])
        cleanup(job, run)   # kept on failure, so the work can be inspected
    except (AgentError, OSError, subprocess.SubprocessError) as e:
        job.update(state="failed", note=str(e)[:300])
    save_job(job)
    return job


def announce(job):
    """(spoken line, notification title, notification text) for a finished job."""
    if job["state"] == "pr":
        return (f"Draft PR ready in {job['repo']}: {job['title']}",
                f"Claude: draft PR ready ({job['repo']})", f"{job['title']}\n{job['url']}")
    if job["state"] == "answered":
        return (f"Claude answered about {job['repo']}. {job['note'][:200]}",
                f"Claude answered ({job['repo']})", job["note"])
    if job["state"] == "no-changes":
        return (f"Claude made no changes in {job['repo']}. {job['note'][:150]}",
                f"Claude: no changes ({job['repo']})", job["note"])
    return (f"The job in {job['repo']} failed. {job['note'][:150]}",
            f"Claude: job failed ({job['repo']})", job["note"])


def summary(job):
    """One line for the panel's jobs row."""
    states = {"working": "Claude Code working\u2026", "pr": "draft PR ready", "answered": "answered",
              "no-changes": "no changes", "failed": "failed: " + job.get("note", "")[:60]}
    return f"{job['id']} → {states.get(job['state'], job['state'])}"
