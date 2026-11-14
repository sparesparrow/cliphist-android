"""Claude Panel router: the draft (dictated request + mcp-prompts template + repo), the router
agent that decides where it goes, and the hand-off to the GitHub @claude agent.

  draft   state/draft.json  {transcript, template, repo, slug, filled, updated}
  route   panel-router prompt -> `claude -p --model haiku` (no tools, 150 s) -> ROUTE / REASON
  github  gh issue create -R <slug> --label voice-panel, body "@claude" + the filled template;
          only if the repo has .github/workflows/claude.yml (else nothing is posted)
  jobs    state/jobs.json; poll_job() follows an issue until a PR (or Claude's answer) shows up

Every external command goes through `run` (argv lists, never a shell), so tests inject fakes.
The repos are public: an issue is published the moment dispatch_github() runs.
"""
import json, os, re, subprocess, time

import prompts

HERE = os.path.dirname(os.path.abspath(__file__))
REPOS_FILE = os.environ.get("PANEL_REPOS_FILE") or os.path.join(HERE, "repos.json")
ROUTES = ("github-claude", "phone-assistant", "none")
ROUTER_PROMPT = "panel-router"
DEFAULT_TEMPLATE = "panel-coding-task"
LABEL = "voice-panel"
JOB_MAX_S = 2 * 3600
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


class DispatchError(Exception):
    pass


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


# --- GitHub @claude ---------------------------------------------------------
def _gh(args, run=None, timeout=60):
    return (run or _run)(["gh", *args], capture_output=True, text=True, timeout=timeout)


def workflow_exists(slug, run=None):
    p = _gh(["api", f"repos/{slug}/contents/.github/workflows/claude.yml", "--silent"], run)
    return p.returncode == 0


def issue_title(transcript):
    t = transcript.strip().rstrip(".")
    return (t[:67] + "...") if len(t) > 70 else (t or "Voice panel request")


def dispatch_github(d, run=None):
    """Open the @claude issue. Returns the new job; raises DispatchError without posting
    anything when the repo is unknown or has no @claude workflow."""
    r = repo(d.get("repo", ""))
    if r is None or not d.get("filled"):
        raise DispatchError("pick a template and a repo first")
    if not workflow_exists(r["slug"], run):
        raise DispatchError(f"@claude not set up in {r['name']}")
    _gh(["label", "create", LABEL, "-R", r["slug"], "--force", "--color", "5319E7",
         "--description", "Sent from the Claude Panel on the phone"], run)
    p = _gh(["issue", "create", "-R", r["slug"], "--title", issue_title(d["transcript"]),
             "--label", LABEL, "--body", "@claude\n\n" + d["filled"]], run)
    url = (p.stdout or "").strip().splitlines()[-1:] or [""]
    m = re.search(r"/issues/(\d+)$", url[0])
    if p.returncode != 0 or not m:
        raise DispatchError("gh issue create failed: " + ((p.stderr or "").strip().splitlines() or ["?"])[-1])
    job = {"id": f"{r['name']}#{m.group(1)}", "repo": r["name"], "slug": r["slug"],
           "issue": int(m.group(1)), "issue_url": url[0], "template": d["template"],
           "created": int(time.time()), "state": "waiting", "url": "", "title": "", "note": ""}
    jobs = _load("jobs.json", [])
    jobs.append(job)
    _save("jobs.json", jobs[-20:])
    return job


def jobs():
    return _load("jobs.json", [])


def save_jobs(js):
    _save("jobs.json", js[-20:])


def _is_claude(login):
    """Claude's comments: the Claude GitHub App, or github-actions when the workflow runs the
    action with its own GITHUB_TOKEN (claude-workflow.yml)."""
    return login.startswith("claude") or login in ("github-actions", "github-actions[bot]", "app/github-actions")


def poll_job(job, run=None, now=None):
    """Advance one waiting job. Returns True if its state changed:
    waiting -> pr (a PR mentions the issue) | branch (Claude pushed a branch and left a
    'Create PR' link) | answered (Claude finished without a branch) | timeout (2 h)."""
    if job.get("state") != "waiting":
        return False
    now = now or time.time()
    num, slug = job["issue"], job["slug"]
    p = _gh(["pr", "list", "-R", slug, "--state", "all", "--search", f"{num} in:body",
             "--json", "url,title,isDraft,state,body"], run)
    if p.returncode == 0:
        try:
            prs = json.loads(p.stdout or "[]")
        except ValueError:
            prs = []
        for pr in prs:
            if re.search(rf"#{num}\b|/issues/{num}\b", pr.get("body") or ""):
                job.update(state="pr", url=pr["url"], title=pr.get("title", ""),
                           note="draft PR ready" if pr.get("isDraft") else "PR ready")
                return True
    p = _gh(["issue", "view", str(num), "-R", slug, "--json", "comments"], run)
    if p.returncode == 0:
        try:
            comments = json.loads(p.stdout or "{}").get("comments", [])
        except ValueError:
            comments = []
        for c in reversed(comments):
            if not _is_claude((c.get("author") or {}).get("login", "")):
                continue
            body = c.get("body") or ""
            if "finished" not in body.lower():
                break        # Claude's tracking comment is still "working"
            link = re.search(r"https://github\.com/[^\s)\]]+/compare/[^\s)\]]+", body)
            if link:
                job.update(state="branch", url=link.group(0), note="branch pushed, open the PR")
            else:
                answer = re.sub(r"\s+", " ", re.sub(r"[*#`>]|\[.*?\]\(.*?\)", "", body)).strip()
                job.update(state="answered", url=job["issue_url"], note=answer[:300])
            return True
    if now - job["created"] > JOB_MAX_S:
        job.update(state="timeout", note="no answer from @claude in 2 h")
        return True
    return False


def announce(job):
    """(spoken line, notification title, notification text) for a finished job."""
    where = f"{job['repo']} issue {job['issue']}"
    if job["state"] == "pr":
        return (f"{job['note'][:1].upper() + job['note'][1:]} in {job['repo']}: {job['title']}",
                f"Claude: {job['note']} ({job['repo']})", f"{job['title']}\n{job['url']}")
    if job["state"] == "branch":
        return (f"Claude pushed a branch for {where}. Open the link to create the pull request.",
                f"Claude: branch ready ({job['repo']})", job["url"])
    if job["state"] == "answered":
        return (f"Claude answered {where}. {job['note'][:200]}",
                f"Claude answered ({job['repo']})", f"{job['note']}\n{job['url']}")
    return (f"No answer from Claude on {where} yet.", f"Claude: no answer ({job['repo']})",
            job["issue_url"])


def summary(job):
    """One line for the panel's jobs row."""
    states = {"waiting": "waiting for @claude", "pr": job.get("note") or "PR ready",
              "branch": "branch pushed", "answered": "answered", "timeout": "no answer (2 h)"}
    return f"{job['id']} → {states.get(job['state'], job['state'])}"
