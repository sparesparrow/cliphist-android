"""phone/tasker/build-claude-panel.py: XML wiring, token handling, and the JavaScript the
Tasker tasks run (syntax, and cpReq parsing the way Tasker's shell() returns output)."""
import importlib.util
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

import pytest

SRC = Path(__file__).resolve().parents[1] / "phone" / "tasker" / "build-claude-panel.py"
spec = importlib.util.spec_from_file_location("build_claude_panel", SRC)
bp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bp)
TOKEN = "TestToken_0123456789abcdef"
node = pytest.mark.skipif(not shutil.which("node"), reason="node not installed")


def root():
    return ET.fromstring(bp.to_xml(bp.build(TOKEN)))


def js_of(r, task):
    t = next(t for t in r.iter("Task") if t.findtext("nme") == task)
    a = next(a for a in t.iter("Action") if a.findtext("code") == "129")
    return next(s.text for s in a.findall("Str") if s.get("sr") == "arg0")


def test_project_lists_its_scene_profile_and_tasks():
    r = root()
    p = r.find("Project")
    assert p.findtext("name") == "Claude Assistant" and p.findtext("scenes") == "Claude Panel"
    tids = {t.findtext("id") for t in r.iter("Task")}
    assert set(p.findtext("tids").split(",")) == tids
    prof = r.find("Profile")
    assert p.findtext("pids") == prof.findtext("id")
    assert prof.find("Event").findtext("code") == "201"
    assert prof.findtext("mid0") in tids


def test_every_button_runs_an_existing_task():
    r = root()
    names = {t.findtext("id"): t.findtext("nme") for t in r.iter("Task")}
    got = {b.find("Str").text: names[b.findtext("clickTask")] for b in r.iter("ButtonElement")}
    assert got == {"TalkBtn": "CP Talk", "NewBtn": "CP New", "StopBtn": "CP Stop",
                   "TypeBtn": "CP Type", "CloseBtn": "CP Close", "DictateBtn": "CP Dictate",
                   "TemplateBtn": "CP Template", "RepoBtn": "CP Repo", "SendBtn": "CP Send"}


def test_poll_targets_existing_elements():
    r = root()
    els = {e.find("Str").text for e in r.find("Scene") if e.tag.endswith("Element") and e.find("Str") is not None}
    for name in ("Status", "Heard", "Reply", "Prompt", "Jobs"):
        assert name in els and f'"{name}"' in js_of(r, "CP Poll")


def test_type_asks_then_posts():
    r = root()
    t = next(t for t in r.iter("Task") if t.findtext("nme") == "CP Type")
    assert [a.findtext("code") for a in t.iter("Action")] == ["129", "360", "129"]
    js = [next(x.text for x in a.findall("Str") if x.get("sr") == "arg0") for a in t.iter("Action") if a.findtext("code") == "129"]
    assert "hideScene" in js[0] and "cpShow" in js[1] and "/ask?async=1" in js[1]


def test_token_only_inside_javascript():
    xml = bp.to_xml(bp.build(TOKEN))
    r = ET.fromstring(xml)
    scripts = [next(x.text for x in a.findall("Str") if x.get("sr") == "arg0")
               for a in r.iter("Action") if a.findtext("code") == "129"]
    assert xml.count(TOKEN) == sum(js.count(TOKEN) for js in scripts) == len(scripts)


@pytest.mark.parametrize("bad", ["", "short", "has space 0123456789", "quote'0123456789abc", 'q"0123456789abcdef'])
def test_rejects_odd_tokens(bad):
    with pytest.raises(ValueError):
        bp.build(bad)


def test_cli_dry_run_and_out_file(tmp_path):
    out = subprocess.run(["python3", "-I", str(SRC), "--dry-run"], capture_output=True, text=True)
    assert out.returncode == 0 and ET.fromstring(out.stdout).tag == "TaskerData"
    tok = tmp_path / "tok"
    tok.write_text(TOKEN + "\n")
    dest = tmp_path / "x.prj.xml"
    p = subprocess.run(["python3", "-I", str(SRC), str(dest), "--token-file", str(tok)],
                       capture_output=True, text=True)
    assert p.returncode == 0 and TOKEN not in p.stdout + p.stderr
    assert dest.stat().st_mode & 0o777 == 0o600
    bad = subprocess.run(["python3", "-I", str(SRC), str(tmp_path / "x.xml"), "--token-file", str(tok)],
                         capture_output=True, text=True)
    assert bad.returncode != 0


STUBS = """
var calls = [], globals = {}, reply = "";
function shell(cmd) { calls.push(cmd); return reply; }
function writeFile(p, d) { calls.push("write " + p + " " + d); }
function global(n) { return globals[n] || ""; } function setGlobal(n, v) { globals[n] = v; }
function flash() {} function showScene() {} function performTask() {} function destroyScene() {}
function elemText() {} function elemTextColour() {} function wait() {} function hideScene() {}
var locals = {}; function setLocal(n, v) { locals[n] = v; } function local(n) { return locals[n] || ""; }
"""


def run_js(code):
    p = subprocess.run(["node", "-e", code], capture_output=True, text=True, timeout=30)
    assert p.returncode == 0, p.stderr
    return json.loads(p.stdout)


@node
def test_all_task_scripts_parse():
    r = root()
    for t in r.iter("Task"):
        code = js_of(r, t.findtext("nme"))
        p = subprocess.run(["node", "--check", "-"], input="function __t(){\n" + code + "\n}\n",
                           capture_output=True, text=True)
        assert p.returncode == 0, (t.findtext("nme"), p.stderr)


@node
@pytest.mark.parametrize("eol", ["\r\n", "\n"])   # Tasker's shell() drops the CRs
def test_cpreq_parses_status_with_or_without_cr(eol):
    lib = js_of(root(), "CP Stop").split("\nvar r = cpReq")[0]
    resp = eol.join(["HTTP/1.0 200 OK", "Content-Type: application/json", "", '{"phase": "idle"}', ""])
    out = run_js(STUBS + lib + f"\nreply = {json.dumps(resp)};\n"
                 "console.log(JSON.stringify(cpReq('GET', '/status')));")
    assert out == {"code": 200, "text": '{"phase": "idle"}'}


@node
def test_cpreq_body_goes_through_a_file_not_the_command_line():
    lib = js_of(root(), "CP Stop").split("\nvar r = cpReq")[0]
    evil = "ř'\"`$(touch /tmp/x)`;|&"
    out = run_js(STUBS + lib + f"\ncpReq('POST', '/ask?async=1', {{text: {json.dumps(evil)}}});"
                 "console.log(JSON.stringify(calls));")
    write, cmd = out
    assert write.startswith("write " + bp.BODY_FILE)
    assert "touch" not in cmd and "Content-Length: " + str(len(json.dumps({"text": evil}, ensure_ascii=False, separators=(",", ":")).encode())) in cmd
    assert cmd.endswith("| nc -q 3 127.0.0.1 8780") and f"rm -f '{bp.BODY_FILE}'" in cmd


@node
def test_cpreq_no_answer_is_code_zero():
    lib = js_of(root(), "CP Stop").split("\nvar r = cpReq")[0]
    out = run_js(STUBS + lib + "\nreply = '';\nconsole.log(JSON.stringify(cpReq('GET', '/status')));")
    assert out == {"code": 0, "text": ""}


def test_poll_loops_with_tasker_wait_and_goto_not_inside_js():
    r = root()
    t = next(t for t in r.iter("Task") if t.findtext("nme") == "CP Poll")
    acts = list(t.iter("Action"))
    assert [a.findtext("code") for a in acts] == ["129", "30", "135"]
    assert "while" not in js_of(r, "CP Poll") and "wait(" not in js_of(r, "CP Poll")
    goto = acts[2]
    cond = goto.find("ConditionList/Condition")
    assert (cond.findtext("lhs"), cond.findtext("op"), cond.findtext("rhs")) == ("%CP_OPEN", "2", "1")
    assert [i.get("val") for i in goto.findall("Int")] == ["0", "1"]   # action number 1


def poll_once(reply, t0="Date.now()"):
    code = js_of(root(), "CP Poll")
    return run_js(STUBS + f"globals.CP_OPEN = '1'; globals.CP_T0 = String({t0}); reply = {json.dumps(reply)};\n"
                  "var texts = []; elemText = function (s, e, m, t) { texts.push([e, t]); };\n"
                  + code + "\nconsole.log(JSON.stringify({texts: texts, open: globals.CP_OPEN}));")


@node
def test_poll_survives_a_garbled_status_body():
    assert poll_once("HTTP/1.0 200 OK\n\nnot json")["texts"] == [["Status", "\u25CF Bad status reply"]]


@node
def test_poll_shows_phase_and_exchange_and_skips_unchanged_repaints():
    body = json.dumps({"phase": "thinking", "busy": True, "heard": "hi", "reply": "yo", "last": {}})
    out = poll_once("HTTP/1.0 200 OK\n\n" + body)
    assert out["texts"][:3] == [["Status", "\u25CF Thinking\u2026"], ["Heard", "You: hi"], ["Reply", "Claude: yo"]]
    assert out["texts"][3] == ["Prompt", "📄 - · 📦 pick a repo\nDictate a request first."]
    assert out["texts"][4] == ["Jobs", "Jobs: none yet"]
    assert out["open"] == "1"


@node
def test_poll_closes_the_panel_after_ten_minutes():
    out = poll_once("HTTP/1.0 200 OK\n\n{}", t0="Date.now() - 601000")
    assert out == {"texts": [], "open": ""}


def all_js(r, task):
    t = next(t for t in r.iter("Task") if t.findtext("nme") == task)
    return [next(x.text for x in a.findall("Str") if x.get("sr") == "arg0")
            for a in t.iter("Action") if a.findtext("code") == "129"]


@pytest.mark.parametrize("task,path,field", [("CP Template", "/templates", "template"), ("CP Repo", "/repos", "repo")])
def test_pickers_hide_list_dialog_then_save_and_show(task, path, field):
    r = root()
    t = next(t for t in r.iter("Task") if t.findtext("nme") == task)
    acts = list(t.iter("Action"))
    assert [a.findtext("code") for a in acts] == ["129", "378", "129"]
    ld = {x.get("sr"): (x.text if x.tag == "Str" else x.get("val")) for x in acts[1] if x.get("sr")}
    assert ld["arg1"] == "1" and ld["arg3"] == "%cp_items" and ld["arg6"] == "OK" and ld["arg9"] == "60"
    first, last = all_js(r, task)
    assert first.endswith(f'cpPick("{path}", "{"id" if field == "template" else "name"}");')
    assert last.endswith(f'cpPicked("{field}");')


@node
def test_cppick_lists_ids_and_hides_the_panel():
    lib = js_of(root(), "CP Stop").split("\nvar r = cpReq")[0]
    body = json.dumps([{"id": "panel-bug-fix"}, {"id": "a,b"}])
    out = run_js(STUBS + lib + f"\nreply = {json.dumps('HTTP/1.0 200 OK' + chr(10) + chr(10) + body)};"
                 "var hidden = 0; hideScene = function () { hidden++; }; globals.CP_OPEN = '1';"
                 "cpPick('/templates', 'id'); console.log(JSON.stringify([locals.cp_items, hidden]));")
    assert out == ["panel-bug-fix,a b", 1]


@node
@pytest.mark.parametrize("sel,posted", [("cliphist-android", True), ("%ld_selected", False), ("", False)])
def test_cppicked_posts_the_choice_and_shows_the_panel(sel, posted):
    lib = js_of(root(), "CP Stop").split("\nvar r = cpReq")[0]
    out = run_js(STUBS + lib + f"\nlocals.ld_selected = {json.dumps(sel)}; reply = 'HTTP/1.0 200 OK\\n\\n{{}}';"
                 "var shown = 0; showScene = function () { shown++; }; globals.CP_OPEN = '1';"
                 "cpPicked('repo'); console.log(JSON.stringify([calls, shown]));")
    calls, shown = out
    assert shown == 1
    assert any('"repo":"cliphist-android"' in c for c in calls) == posted


@node
def test_poll_previews_the_prompt_and_where_it_goes():
    body = json.dumps({"phase": "idle", "busy": False, "heard": "h", "reply": "r", "last": {},
                       "draft": {"transcript": "add x", "template": "panel-coding-task", "repo": "mia",
                                 "slug": "sparesparrow/mia", "filled": "Implement   this"},
                       "dispatch": {"state": "done", "result": "mia#4: waiting", "at": 9e12},
                       "jobs": [{"line": "mia#3 \u2192 draft PR ready"}]})
    out = poll_once("HTTP/1.0 200 OK\n\n" + body)
    texts = dict(out["texts"])
    assert texts["Prompt"] == "📄 panel-coding-task · 📦 mia\nSend will post to github.com/sparesparrow/mia (public)\nImplement this"
    assert texts["Jobs"] == "\u2714 mia#4: waiting\nmia#3 \u2192 draft PR ready"


@node
@pytest.mark.parametrize("draft,said", [({}, "Dictate a request first"), ({"transcript": "x"}, "Pick a repo first"),
                                        ({"transcript": "x", "repo": "mia"}, None)])
def test_send_checks_the_draft_first(draft, said):
    code = js_of(root(), "CP Send")
    body = json.dumps({"draft": draft})
    out = run_js(STUBS + "var flashes = []; flash = function (m) { flashes.push(m); };"
                 f"reply = {json.dumps('HTTP/1.0 200 OK' + chr(10) + chr(10) + body)};\n" + code +
                 "\nconsole.log(JSON.stringify([flashes, calls.length]));")
    flashes, n = out
    if said:
        assert flashes == [said] and n == 1
    else:
        assert n == 2   # GET /status, then POST /dispatch
