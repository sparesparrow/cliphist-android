"""router/: draft filling (mcp-prompts server and the JSON-file fallback), route parsing,
the GitHub @claude dispatch argv, the job watcher. gh and claude are fakes; nothing is posted."""
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
    ("ROUTE: github-claude\nREASON: code work", ("github-claude", "code work")),
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


def ready(env):
    router.update_draft(transcript="add a one-line comment", repo_name="cliphist-android")
    return router.draft()


def test_dispatch_posts_issue_with_claude_mention(env):
    fake = Fake({"gh issue create": (0, "https://github.com/sparesparrow/cliphist-android/issues/42\n")})
    job = router.dispatch_github(ready(env), run=fake)
    argvs = [a for a, _ in fake.calls]
    assert argvs[0][:3] == ["gh", "api", "repos/sparesparrow/cliphist-android/contents/.github/workflows/claude.yml"]
    assert argvs[1][:4] == ["gh", "label", "create", "voice-panel"]
    issue = argvs[2]
    assert issue[:5] == ["gh", "issue", "create", "-R", "sparesparrow/cliphist-android"]
    body = issue[issue.index("--body") + 1]
    assert body.startswith("@claude\n\n") and "add a one-line comment" in body
    assert issue[issue.index("--title") + 1] == "add a one-line comment"
    assert job["id"] == "cliphist-android#42" and job["state"] == "waiting"
    assert router.jobs() == [job]


def test_dispatch_without_workflow_posts_nothing(env):
    fake = Fake({"gh api": (1, "")})
    with pytest.raises(router.DispatchError, match="@claude not set up in cliphist-android"):
        router.dispatch_github(ready(env), run=fake)
    assert len(fake.calls) == 1 and router.jobs() == []


def test_dispatch_failed_issue_create(env):
    with pytest.raises(router.DispatchError, match="gh issue create failed"):
        router.dispatch_github(ready(env), run=Fake({"gh issue create": (1, "")}))
    assert router.jobs() == []


def test_issue_title_is_capped():
    assert len(router.issue_title("x" * 200)) == 70 and router.issue_title("") == "Voice panel request"


def job(**kw):
    return {"id": "mia#7", "repo": "mia", "slug": "sparesparrow/mia", "issue": 7,
            "issue_url": "https://github.com/sparesparrow/mia/issues/7", "created": 1000,
            "state": "waiting", "url": "", "title": "", "note": "", **kw}


def comments(*cs):
    return json.dumps({"comments": [{"author": {"login": a}, "body": b} for a, b in cs]})


def test_watch_finds_the_draft_pr():
    prs = [{"url": "u1", "title": "other", "isDraft": True, "state": "OPEN", "body": "Closes #70"},
           {"url": "u2", "title": "Add comment", "isDraft": True, "state": "OPEN", "body": "Closes #7"}]
    j = job()
    assert router.poll_job(j, run=Fake({"gh pr list": (0, json.dumps(prs))}), now=1100)
    assert (j["state"], j["url"], j["note"]) == ("pr", "u2", "draft PR ready")
    assert router.announce(j)[0] == "Draft PR ready in mia: Add comment"
    assert not router.poll_job(j, run=Fake(), now=1200)   # finished jobs are left alone


def test_watch_waits_while_claude_is_working():
    f = Fake({"gh pr list": (0, "[]"), "gh issue view": (0, comments(("claude", "Claude Code is working…")))})
    j = job()
    assert not router.poll_job(j, run=f, now=1100) and j["state"] == "waiting"


def test_watch_branch_link_and_answer():
    body = "**Claude finished @sparesparrow's task** [Create PR ➔](https://github.com/sparesparrow/mia/compare/main...claude/issue-7)"
    j = job()
    assert router.poll_job(j, run=Fake({"gh pr list": (0, "[]"), "gh issue view": (0, comments(("claude", body)))}), now=1100)
    assert j["state"] == "branch" and j["url"].endswith("/compare/main...claude/issue-7")
    j = job()
    ans = "**Claude finished** The panel polls `/status` every 2 s."
    assert router.poll_job(j, run=Fake({"gh pr list": (0, "[]"), "gh issue view": (0, comments(("sparesparrow", "x"), ("claude", ans)))}), now=1100)
    assert j["state"] == "answered" and "polls /status every 2 s" in j["note"]


def test_watch_accepts_github_actions_as_claude():
    f = Fake({"gh pr list": (0, "[]"), "gh issue view": (0, comments(("github-actions", "**Claude finished** It is fine.")))})
    j = job()
    assert router.poll_job(j, run=f, now=1100) and j["state"] == "answered"


def test_watch_ignores_other_people_and_times_out():
    f = Fake({"gh pr list": (0, "[]"), "gh issue view": (0, comments(("mallory", "Claude finished: merged!")))})
    j = job()
    assert not router.poll_job(j, run=f, now=1100)
    assert router.poll_job(j, run=f, now=1000 + router.JOB_MAX_S + 1) and j["state"] == "timeout"


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
