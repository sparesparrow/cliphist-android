"""router/: draft filling (mcp-prompts server and the JSON-file fallback), route parsing,
the local Claude Code job (worktree, tools, commit, push, draft PR). git, gh and claude are fakes; nothing is posted."""
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

import pytest

ROUTER = Path(__file__).resolve().parents[1] / "router"
sys.path.insert(0, str(ROUTER))
import prompts  # noqa: E402
import router  # noqa: E402

FIX = Path(__file__).parent / "fixtures" / "panel-prompts"
SERVER = shutil.which("mcp-prompts") or "/usr/local/bin/mcp-prompts"
has_server = pytest.mark.skipif(not os.access(SERVER, os.X_OK), reason="mcp-prompts not installed")


@pytest.fixture(params=["files", "server"])
def env(request, tmp_path, monkeypatch):
    if request.param == "server" and not os.access(SERVER, os.X_OK):
        pytest.skip("mcp-prompts not installed")
    pdir = tmp_path / "prompts" / "panel"
    shutil.copytree(FIX, pdir)
    monkeypatch.setattr(prompts, "PROMPTS_DIR", str(tmp_path / "prompts"))
    monkeypatch.setattr(prompts, "SERVER", SERVER if request.param == "server" else "/nonexistent/mcp-prompts")
    monkeypatch.setenv("VA_STATE_DIR", str(tmp_path / "state"))
    return tmp_path


class Fake:
    """Stands in for subprocess.run: answers by argv prefix, records every call."""
    def __init__(self, answers=None):
        self.calls, self.answers = [], answers or {}

    def __call__(self, argv, **kw):
        assert isinstance(argv, list) and not kw.get("shell")
        self.calls.append((argv, kw))
        for prefix, ans in self.answers.items():
            if " ".join(argv).startswith(prefix):
                if isinstance(ans, Exception):
                    raise ans
                rc, out = ans
                return subprocess.CompletedProcess(argv, rc, out, "boom" if rc else "")
        return subprocess.CompletedProcess(argv, 0, "", "")


def test_templates_hide_the_router(env):
    ids = [t["id"] for t in router.templates()]
    assert ids == ["panel-bug-fix", "panel-coding-task", "panel-repo-question"]


def test_draft_fills_request_and_repo(env):
    router.update_draft(transcript="  add a  comment\nto the README ")
    d = router.update_draft(repo_name="cliphist-android")
    assert d["slug"] == "sparesparrow/cliphist-android" and d["template"] == "panel-coding-task"
    assert "> add a comment to the README" in d["filled"] and "in cliphist-android" in d["filled"]
    assert "{{" not in d["filled"]
    d = router.update_draft(template="panel-bug-fix")
    assert "Fix this bug in cliphist-android" in d["filled"]
    assert json.loads((env / "state" / "draft.json").read_text())["template"] == "panel-bug-fix"


def test_draft_rejects_unknown_choices(env):
    for kw in ({"template": "panel-router"}, {"template": "nope"}, {"repo_name": "evil/repo"}):
        with pytest.raises(ValueError):
            router.update_draft(**kw)
    assert not (env / "state" / "draft.json").exists()


def test_draft_needs_all_three_to_fill(env):
    assert router.update_draft(transcript="hi")["filled"] == ""


@pytest.mark.parametrize("out,want", [
    ("ROUTE: coding-agent\nREASON: code work", ("coding-agent", "code work")),
    ("```\nROUTE: phone-assistant\nREASON: weather\n```", ("phone-assistant", "weather")),
    ("ROUTE: rm-rf\nREASON: x", ("none", "router answer not understood")),
    ("I think github", ("none", "router answer not understood")),
])
def test_route_parses_the_answer(env, out, want):
    router.update_draft(transcript="fix it", repo_name="mia")
    fake = Fake({"claude": (0, out)})
    assert router.route(router.draft(), run=fake) == want
    argv, kw = fake.calls[0]
    assert argv[:5] == ["claude", "-p", "--model", "haiku", "--tools"] and argv[5] == ""
    assert "Request (speech-to-text): fix it" in kw["input"] and "{{" not in kw["input"]


def test_route_timeout_failure_and_empty_draft(env):
    assert router.route(router.draft(), run=Fake()) == ("none", "nothing to send yet")
    router.update_draft(transcript="x", repo_name="mia")
    t = Fake({"claude": subprocess.TimeoutExpired("claude", 60)})
    assert router.route(router.draft(), run=t) == ("none", "router timed out")
    assert router.route(router.draft(), run=Fake({"claude": (1, "")}))[0] == "none"


def ready(env, template=None):
    router.update_draft(transcript="add a one-line comment", repo_name="cliphist-android")
    if template:
        router.update_draft(template=template)
    return router.draft()


class Agent(Fake):
    """Fake git/gh/claude for a local job; `status` is what git status --porcelain prints."""
    def __init__(self, status=" M README.md\n", agent_out="Changed README.\nTested: n/a", **over):
        answers = {"gh repo view": (0, "main\n"), "gh pr create": (0, "https://github.com/sparesparrow/cliphist-android/pull/77\n"),
                   "claude": (0, agent_out)}
        answers.update(over)
        super().__init__(answers)
        self.status = status

    def __call__(self, argv, **kw):
        if argv[0] == "git" and "status" in argv:
            self.calls.append((argv, kw))
            return subprocess.CompletedProcess(argv, 0, self.status, "")
        return super().__call__(argv, **kw)

    def find(self, *words):
        return [(a, kw) for a, kw in self.calls if all(w in a for w in words)]


def test_new_job_records_a_working_job(env):
    job = router.new_job(ready(env), now=1794600000)
    assert job["state"] == "working" and job["mode"] == "change" and job["branch"].startswith("panel/")
    assert router.jobs() == [job]
    with pytest.raises(router.AgentError):
        router.new_job({"repo": "", "filled": ""})


def test_job_runs_claude_code_in_a_worktree_then_opens_a_draft_pr(env, tmp_path, monkeypatch):
    monkeypatch.setattr(router, "WORK_ROOT", str(tmp_path / "work"))
    d = ready(env)
    job = router.new_job(d)
    fake = Agent()
    job = router.run_job(d, job, run=fake)
    assert (job["state"], job["url"]) == ("pr", "https://github.com/sparesparrow/cliphist-android/pull/77")
    wt = job["worktree"]
    assert fake.find("worktree", "add", job["branch"], wt, "origin/main")
    (claude, kw), = fake.find("claude")
    assert kw["cwd"] == wt and "--permission-mode" in claude and claude[claude.index("--permission-mode") + 1] == "dontAsk"
    tools = claude[claude.index("--allowedTools") + 1].split(",")
    assert "Edit" in tools and not any(t.startswith(("Bash(git commit", "Bash(git push", "Bash(gh")) for t in tools)
    assert "Bash" not in tools and "--strict-mcp-config" in claude
    assert "do NOT commit, push" in kw["input"] and "> add a one-line comment" in kw["input"]
    (commit, _), = fake.find("commit")
    assert "user.name=sparesparrow" in commit
    assert fake.find("push", "origin", job["branch"])
    (pr, _), = fake.find("gh", "pr", "create")
    assert "--draft" in pr and pr[pr.index("--head") + 1] == job["branch"] and "Changed README." in pr[pr.index("--body") + 1]
    assert fake.find("worktree", "remove")
    assert router.jobs()[-1]["state"] == "pr"
    assert all(not kw.get("shell") for _, kw in fake.calls)


def test_job_without_changes_pushes_nothing(env, tmp_path, monkeypatch):
    monkeypatch.setattr(router, "WORK_ROOT", str(tmp_path / "work"))
    d = ready(env)
    fake = Agent(status="", agent_out="Nothing to do: the comment is already there.")
    job = router.run_job(d, router.new_job(d), run=fake)
    assert job["state"] == "no-changes" and "already there" in job["note"]
    assert not fake.find("push") and not fake.find("gh", "pr", "create")


@pytest.mark.parametrize("path", [".env", "voice/state/http-token", "a/cookie", "logs/x.log", "keys/id_ed25519"])
def test_job_refuses_to_push_secret_looking_files(env, tmp_path, monkeypatch, path):
    monkeypatch.setattr(router, "WORK_ROOT", str(tmp_path / "work"))
    d = ready(env)
    fake = Agent(status=f"?? {path}\n M README.md\n")
    job = router.run_job(d, router.new_job(d), run=fake)
    assert job["state"] == "failed" and "secrets" in job["note"]
    assert not fake.find("add", "-A") and not fake.find("push")
    assert not fake.find("worktree", "remove")   # kept for inspection


def test_question_template_answers_read_only(env, tmp_path, monkeypatch):
    monkeypatch.setattr(router, "WORK_ROOT", str(tmp_path / "work"))
    d = ready(env, "panel-repo-question")
    fake = Agent(agent_out="The panel polls /status.\nDetails...")
    job = router.run_job(d, router.new_job(d), run=fake)
    assert job["mode"] == "answer" and job["state"] == "answered" and job["note"].startswith("The panel polls")
    (claude, kw), = fake.find("claude")
    tools = claude[claude.index("--allowedTools") + 1].split(",")
    assert "Edit" not in tools and "Write" not in tools and "Do not change files" in kw["input"]
    assert not fake.find("commit") and not fake.find("push")
    assert fake.find("branch", "-D", job["branch"])


def test_job_agent_failure_and_timeout(env, tmp_path, monkeypatch):
    monkeypatch.setattr(router, "WORK_ROOT", str(tmp_path / "work"))
    d = ready(env)
    job = router.run_job(d, router.new_job(d), run=Agent(claude=(1, "")))
    assert job["state"] == "failed" and job["note"].startswith("Claude Code failed")
    job = router.run_job(d, router.new_job(d), run=Agent(claude=subprocess.TimeoutExpired("claude", 1)))
    assert job["state"] == "failed" and "did not finish" in job["note"]


def test_job_clones_a_repo_without_a_local_path(env, tmp_path, monkeypatch):
    monkeypatch.setattr(router, "WORK_ROOT", str(tmp_path / "work"))
    router.update_draft(transcript="x", repo_name="mcp-router")
    d = router.draft()
    fake = Agent()
    router.run_job(d, router.new_job(d), run=fake)
    (clone, _), = fake.find("gh", "repo", "clone")
    assert clone[3:5] == ["sparesparrow/mcp-router", str(tmp_path / "work" / "_clones" / "mcp-router")]


def test_announce_and_summary_lines():
    j = {"repo": "mia", "title": "Add x", "url": "u", "note": "n", "id": "mia@1503"}
    assert router.announce(dict(j, state="pr"))[0] == "Draft PR ready in mia: Add x"
    assert router.summary(dict(j, state="working")) == "mia@1503 \u2192 Claude Code working\u2026"
    assert router.summary(dict(j, state="failed", note="boom")) == "mia@1503 \u2192 failed: boom"


def test_repos_config_is_valid():
    rs = router.repos()
    assert [r["name"] for r in rs] == ["cliphist-android", "mcp-prompts", "mia", "elevenlabs-agents", "mcp-router"]
    assert all(r["slug"] == "sparesparrow/" + r["name"] for r in rs)


@has_server
def test_fixtures_match_mcp_prompts_checkout():
    src = Path("/root/mcp-prompts/data/prompts/panel")
    if not src.is_dir():
        pytest.skip("no mcp-prompts checkout")
    for f in FIX.glob("*.json"):
        if (src / f.name).exists():
            assert json.loads(f.read_text()) == json.loads((src / f.name).read_text()), f.name


def test_assistant_transcribe_only_prints_text_without_claude(tmp_path):
    """assistant.sh --transcribe-only: STT, one line on stdout, exit 3 on silence; no claude."""
    voice = Path(__file__).resolve().parents[1] / "voice"
    d = tmp_path / "voice"
    d.mkdir()
    shutil.copy(voice / "assistant.sh", d)
    shutil.copy(voice / "route.py", d)
    (d / "eleven.py").write_text('#!/bin/bash\ncat "$FAKE_STT"\n')
    (d / "eleven.py").chmod(0o755)
    bin_ = tmp_path / "bin"
    bin_.mkdir()
    (bin_ / "claude").write_text("#!/bin/bash\ntouch \"$CLAUDE_RAN\"; exit 1\n")
    (bin_ / "claude").chmod(0o755)
    audio = tmp_path / "in.wav"
    audio.write_bytes(b"RIFF")
    stt = tmp_path / "stt.json"
    env = {**os.environ, "PATH": f"{bin_}:{os.environ['PATH']}", "ELEVENLABS_API_KEY": "k",
           "FAKE_STT": str(stt), "CLAUDE_RAN": str(tmp_path / "ran"), "VA_OUT_DIR": str(tmp_path / "out")}

    def run():
        return subprocess.run([str(d / "assistant.sh"), "--transcribe-only", str(audio)],
                              capture_output=True, text=True, env=env, timeout=60)
    stt.write_text('{"text": " make it\\nblue ", "language_code": "eng"}')
    p = run()
    assert (p.returncode, p.stdout) == (0, "make it blue\n"), p.stderr
    stt.write_text('{"text": ""}')
    assert run().returncode == 3
    assert not (tmp_path / "ran").exists()
