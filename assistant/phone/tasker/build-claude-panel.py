#!/usr/bin/env python3
"""Build the Tasker project "Claude Assistant": scene "Claude Panel" plus its tasks and a
gesture profile, as one .prj.xml for Tasker's own importer (import-claude-panel.sh).

  build-claude-panel.py OUT.prj.xml [--token-file F]   real build (the file holds the token)
  build-claude-panel.py --dry-run                        fake token, XML to stdout

The panel talks to voice/http_trigger.py on 127.0.0.1:8780:
  GET /status (polled while the panel is open), POST /listen?async=1[&new=1],
  POST /ask?async=1 {"text", "speak": true}, POST /stop.
Every request goes through Tasker's JavaScriptlet shell() + toybox nc. (A WebView
XMLHttpRequest with the X-Token header would need a CORS preflight, which the trigger does not
answer.) Typed text never reaches a shell command line: it goes into a body file that nc
reads with cat.

Router (../../router): Dictate records + transcribes only (POST /dictate?async=1), Template ▾
and Repo ▾ pick an mcp-prompts template and a repo (GET /templates, /repos -> List Dialog ->
POST /draft), the Prompt row previews the filled template and where it would be posted, and
Send (POST /dispatch) lets the router agent hand it to GitHub @claude or the phone assistant.
The Jobs row follows the issue to its draft PR.

Gesture: this project's profile "Claude Panel Gesture" (event 201, Assistance Request) only
opens the panel; the existing profile "Claude Listen" (import-claude-listen.sh) starts the
turn. Long-press power therefore opens the panel and listens. A second gesture while a turn
runs stops it.

Formats follow Tasker 6.7.6 as exported on this phone: action 129 = JavaScriptlet,
360 = Input Dialog (sets %input), 378 = List Dialog (items from a variable, sets
%ld_selected); TextElement arg2 size, arg4 colour, arg7 1 = top-left,
arg8 1 = long text; ButtonElement clickTask + arg1 label.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

SCENE = "Claude Panel"
PROJECT = "Claude Assistant"
PORT = 8780
W, H = 1000, 1380
BODY_FILE = "/sdcard/Tasker/.claude-panel-body.json"
TOKEN_RE = re.compile(r"^[A-Za-z0-9_-]{16,128}$")
DEFAULT_TOKEN_FILE = Path("/root/voice-assistant/state/http-token")

# ----------------------------------------------------------------------------- JavaScript
# Shared helper, prepended to every JavaScriptlet. __TOKEN__ / __PORT__ / __SCENE__ /
# __BODY__ are substituted with JSON string literals.
JS_LIB = r"""
var CP = {token: __TOKEN__, port: __PORT__, scene: __SCENE__, body: __BODY__};
function cpReq(method, path, obj) {
  var data = obj === undefined ? "" : JSON.stringify(obj);
  var len = unescape(encodeURIComponent(data)).length;
  var head = method + " " + path + " HTTP/1.0\\r\\nX-Token: " + CP.token +
             "\\r\\nContent-Type: application/json\\r\\nContent-Length: " + len + "\\r\\n\\r\\n";
  var cmd = "printf '" + head + "'";
  if (data) { writeFile(CP.body, data, false); cmd = "{ " + cmd + "; cat '" + CP.body + "'; rm -f '" + CP.body + "'; }"; }
  var out = "";
  try { out = shell(cmd + " | nc -q 3 127.0.0.1 " + CP.port, false, 15) || ""; } catch (e) { out = ""; }
  // Tasker's shell() may hand back the output with the CRs stripped: split on \r?\n\r?\n.
  var sep = out.match(/\r?\n\r?\n/), m = out.match(/^HTTP\/1\.\d (\d{3})/);
  return {code: m ? parseInt(m[1], 10) : 0,
          text: (sep ? out.substring(sep.index + sep[0].length) : "").replace(/\s+$/, "")};
}
function cpTry(f) { try { f(); } catch (e) {} }
function cpSay(msg) { cpTry(function () { flash(msg); }); }
function cpOpen() {
  if (global("CP_OPEN") == "1") return;
  setGlobal("CP_OPEN", "1"); setGlobal("CP_T0", String(Date.now())); setGlobal("CP_SIG", "");
  cpShow();
  performTask("CP Poll", 4, "", "");
}
function cpShow() { showScene(CP.scene, "OverBlocking", 0, 22, false, false); }
function cpPick(path, key) {
  var r = cpReq("GET", path), items = [];
  try { items = JSON.parse(r.text).map(function (x) { return String(x[key]).replace(/,/g, " "); }); } catch (e) {}
  if (!items.length) cpSay("Claude: no list (" + (r.code || "trigger not reachable") + ")");
  setLocal("cp_items", items.join(","));
  if (global("CP_OPEN") == "1") cpTry(function () { hideScene(CP.scene); });
}
function cpPicked(field) {
  var sel = "";
  try { sel = local("ld_selected") || ""; } catch (e) {}
  if (sel && sel.charAt(0) != "%") {
    var body = {}; body[field] = sel;
    var r = cpReq("POST", "/draft", body);
    if (r.code != 200) cpSay("Claude: " + (r.code ? r.code + " " + r.text : "trigger not reachable"));
  }
  setGlobal("CP_SIG", "");
  if (global("CP_OPEN") == "1") cpTry(cpShow);
}
function cpStart(path, obj) {
  var r = cpReq("POST", path, obj);
  if (r.code == 503) cpSay("Claude is busy - Stop first");
  else if (r.code != 202) cpSay("Claude: " + (r.code ? r.code + " " + r.text : "trigger not reachable"));
}
"""

JS = {
    "CP Open": "cpOpen();",
    # One poll cycle per run of this action. The task loops with Tasker's own Wait + Goto
    # (see build()), because Tasker runs one task's actions at a time: a JS action that looped
    # here would starve the button tasks until it ended.
    "CP Poll": r"""
var LOOK = {idle: ["\u25CF Ready", "#FF9E9E9E"], listening: ["\u25CF Listening\u2026", "#FF66BB6A"],
            thinking: ["\u25CF Thinking\u2026", "#FFFFCA28"], speaking: ["\u25CF Speaking\u2026", "#FF42A5F5"],
            transcribing: ["\u25CF Transcribing\u2026", "#FF66BB6A"]};
var DSP = {routing: "\u23F3 Routing\u2026", posting: "\u23F3 Posting\u2026", failed: "\u26A0 ", done: "\u2714 "};
var t0 = parseInt(global("CP_T0") || "0", 10);
if (!t0 || Date.now() - t0 > 600000) {   // open for 10 min: close the panel
  setGlobal("CP_OPEN", "");
  cpTry(function () { destroyScene(CP.scene); });
} else {
  var r = cpReq("GET", "/status"), st, label, col, s = null;
  if (r.code == 200) { try { s = JSON.parse(r.text); } catch (e) { s = null; } }
  if (s) {
    var look = LOOK[s.phase] || LOOK.idle;
    label = look[0]; col = look[1];
    var last = s.last || {};
    if (s.phase == "idle" && last.result && last.at && Date.now() / 1000 - last.at < 20 &&
        last.result != s.reply) label += " \u2014 " + last.result;
    var d = s.draft || {}, prompt;
    if (!d.transcript) prompt = "📄 " + (d.template || "-") + " \u00B7 📦 " + (d.repo || "pick a repo") + "\nDictate a request first.";
    else if (!d.repo) prompt = "📄 " + d.template + " \u00B7 📦 pick a repo\n\u201C" + d.transcript + "\u201D";
    else prompt = "📄 " + d.template + " \u00B7 📦 " + d.repo + "\nSend will post to github.com/" + d.slug +
                  " (public)\n" + (d.filled || "").replace(/\s+/g, " ").substring(0, 260);
    var jl = (s.jobs || []).slice(-2).reverse().map(function (j) { return j.line; });
    var ds = s.dispatch || {};
    if (ds.state && ds.state != "idle" && ds.at && Date.now() / 1000 - ds.at < 120)
      jl.unshift((DSP[ds.state] || "") + (ds.result || ""));
    st = [label, col, "You: " + (s.heard || "\u2026"), "Claude: " + (s.reply || "\u2026"),
          prompt, jl.length ? jl.join("\n") : "Jobs: none yet"];
  } else {
    st = [r.code == 200 ? "\u25CF Bad status reply" : "\u25CF Offline (" + (r.code || "no answer") + ")",
          "#FFEF5350", "", "", "", ""];
  }
  var sig = st.join("\n");
  if (sig != global("CP_SIG")) {
    setGlobal("CP_SIG", sig);
    cpTry(function () { elemText(CP.scene, "Status", "repl", st[0]); });
    cpTry(function () { elemTextColour(CP.scene, "Status", st[1]); });
    if (st[2]) cpTry(function () { elemText(CP.scene, "Heard", "repl", st[2]); });
    if (st[3]) cpTry(function () { elemText(CP.scene, "Reply", "repl", st[3]); });
    if (st[4]) cpTry(function () { elemText(CP.scene, "Prompt", "repl", st[4]); });
    if (st[5]) cpTry(function () { elemText(CP.scene, "Jobs", "repl", st[5]); });
  }
}
""",
    "CP Talk": 'cpStart("/listen?async=1");',
    "CP New": 'cpStart("/listen?async=1&new=1");',
    "CP Stop": 'var r = cpReq("POST", "/stop"); if (r.code != 200) cpSay("Stop: " + (r.code || "trigger not reachable"));',
    # Input Dialog is cancelled at once while the blocking panel overlay is up: hide the
    # panel (step before the dialog), then send and show it again (step after).
    "CP Type hide": 'if (global("CP_OPEN") == "1") cpTry(function () { hideScene(CP.scene); });',
    "CP Type": r"""
var q = (typeof input === "undefined" || !input || String(input).charAt(0) == "%") ? "" : String(input).trim();
if (q) cpStart("/ask?async=1", {text: q, speak: true});
if (global("CP_OPEN") == "1") cpTry(cpShow);
""",
    "CP Dictate": 'cpStart("/dictate?async=1");',
    "CP Send": r"""
var r = cpReq("GET", "/status"), d = null;
try { d = JSON.parse(r.text).draft; } catch (e) {}
if (!d || !d.transcript) cpSay("Dictate a request first");
else if (!d.repo) cpSay("Pick a repo first");
else cpStart("/dispatch");
""",
    # Template / Repo: fetch the list (and hide the panel, see CP Type), List Dialog, then
    # save the pick in the draft and show the panel again.
    "CP Template list": 'cpPick("/templates", "id");',
    "CP Repo list": 'cpPick("/repos", "name");',
    "CP Template pick": 'cpPicked("template");',
    "CP Repo pick": 'cpPicked("repo");',
    "CP Close": 'setGlobal("CP_OPEN", ""); cpTry(function () { destroyScene(CP.scene); });',
    "CP Gesture": r"""
var r = cpReq("GET", "/status"), busy = false;
try { busy = r.code == 200 && JSON.parse(r.text).busy; } catch (e) {}
if (global("CP_OPEN") == "1" && busy) cpReq("POST", "/stop");
else cpOpen();
""",
}
JS_TIMEOUT = {"CP Type": 30}
POLL_WAIT_MS = 500

# ----------------------------------------------------------------------------- XML helpers
TIME = "1794600000000"


def sub(parent, tag, text=None, **attrs):
    e = ET.SubElement(parent, tag, {k.rstrip("_"): str(v) for k, v in attrs.items()})
    if text is not None:
        e.text = str(text)
    return e


def s_arg(parent, n, value=None):
    return sub(parent, "Str", value, sr=f"arg{n}", ve="3")


def i_arg(parent, n, value):
    return sub(parent, "Int", sr=f"arg{n}", val=value)


def javascriptlet(task, n, code, timeout):
    a = sub(task, "Action", sr=f"act{n}", ve="7")
    sub(a, "code", "129")
    s_arg(a, 0, code)
    s_arg(a, 1)
    i_arg(a, 2, 1)          # auto exit
    i_arg(a, 3, timeout)    # seconds


def wait_ms(task, n, ms):
    """Action 30 Wait (arg0 ms, arg1 s, arg2 min, arg3 h, arg4 days). Other tasks run meanwhile."""
    a = sub(task, "Action", sr=f"act{n}", ve="7")
    sub(a, "code", "30")
    for i, v in enumerate((ms, 0, 0, 0, 0)):
        i_arg(a, i, v)


def goto_if_matches(task, n, action_no, var, pattern):
    """Action 135 Goto action number `action_no` (1-based) If var ~ pattern (op 2 = matches)."""
    a = sub(task, "Action", sr=f"act{n}", ve="7")
    sub(a, "code", "135")
    cl = sub(a, "ConditionList", sr="if")
    c = sub(cl, "Condition", sr="c0", ve="3")
    sub(c, "op", "2")
    sub(c, "lhs", var)
    sub(c, "rhs", pattern)
    i_arg(a, 0, 0)
    i_arg(a, 1, action_no)
    s_arg(a, 2)


def input_dialog(task, n, title, text, timeout=120):
    """Action 360 Input Dialog, same argument shape as the user's own export; sets %input."""
    a = sub(task, "Action", sr=f"act{n}", ve="7")
    sub(a, "code", "360")
    s_arg(a, 1, title)
    s_arg(a, 2, text)
    s_arg(a, 3)
    i_arg(a, 4, timeout)
    s_arg(a, 5)
    i_arg(a, 6, 0)
    i_arg(a, 7, 1)
    s_arg(a, 8)


def list_dialog(task, n, title, items_var, timeout=60):
    """Action 378 List Dialog, shaped like the user's own exports (items from a variable,
    single choice); the pick lands in %ld_selected."""
    a = sub(task, "Action", sr=f"act{n}", ve="7")
    sub(a, "code", "378")
    i_arg(a, 1, 1)
    s_arg(a, 2, title)
    s_arg(a, 3, items_var)
    s_arg(a, 4)
    s_arg(a, 5)
    s_arg(a, 6, "OK")
    s_arg(a, 7, "cancel")
    s_arg(a, 8)
    i_arg(a, 9, timeout)
    for k in (10, 11, 12):
        i_arg(a, k, 0)
    s_arg(a, 13)


def text_el(scene, n, name, text, geom, size, colour="#FFFFFFFF", align=1, long_text=0):
    e = sub(scene, "TextElement", sr=f"elements{n}", ve="3")
    s_arg(e, 0, name)
    s_arg(e, 1, text)
    i_arg(e, 2, size)
    i_arg(e, 3, 100)
    s_arg(e, 4, colour)
    s_arg(e, 5)
    i_arg(e, 6, 0)
    i_arg(e, 7, align)
    i_arg(e, 8, long_text)
    sub(e, "geom", ",".join(map(str, geom)) + ",-1,-1,-1,-1")
    sub(e, "flags", "4")


def button_el(scene, n, name, label, geom, task_id, size=20):
    e = sub(scene, "ButtonElement", sr=f"elements{n}", ve="3")
    sub(e, "clickTask", task_id)
    s_arg(e, 0, name)
    s_arg(e, 1, label)
    i_arg(e, 2, size)
    i_arg(e, 3, 100)
    s_arg(e, 4, "#FFFFFFFF")
    s_arg(e, 5)
    i_arg(e, 6, 0)
    sub(e, "Img", sr="arg7", ve="2")
    sub(e, "geom", ",".join(map(str, geom)) + ",-1,-1,-1,-1")
    sub(e, "flags", "4")


def rect_el(scene, n, name, geom, colour):
    e = sub(scene, "RectElement", sr=f"elements{n}")
    s_arg(e, 0, name)
    i_arg(e, 1, 0)
    s_arg(e, 2, colour)
    s_arg(e, 3)
    i_arg(e, 4, 0)
    s_arg(e, 5, colour)
    i_arg(e, 6, 0)
    i_arg(e, 7, 0)
    sub(e, "geom", ",".join(map(str, geom)) + ",-1,-1,-1,-1")
    sub(e, "flags", "4")


# ----------------------------------------------------------------------------- build
TASK_IDS = {name: 9101 + i for i, name in enumerate(
    ["CP Open", "CP Poll", "CP Talk", "CP New", "CP Stop", "CP Type", "CP Close", "CP Gesture",
     "CP Dictate", "CP Template", "CP Repo", "CP Send"])}
PICKERS = {"CP Template": "Prompt template", "CP Repo": "Repository"}
PROFILE_ID = 9150


def js_for(name, token):
    lib = (JS_LIB.replace("__TOKEN__", json.dumps(token)).replace("__PORT__", str(PORT))
           .replace("__SCENE__", json.dumps(SCENE)).replace("__BODY__", json.dumps(BODY_FILE)))
    return lib.strip() + "\n" + JS[name].strip()


def build(token: str) -> ET.Element:
    if not TOKEN_RE.match(token):
        raise ValueError("token has unexpected characters or length")
    root = ET.Element("TaskerData", sr="", dvi="1", tv="6.7.6-beta")

    proj = sub(root, "Project", sr="proj0", ve="2")
    sub(proj, "cdate", TIME)
    sub(proj, "id", str(uuid.uuid5(uuid.NAMESPACE_URL, "claude-assistant-tasker-project")))
    sub(proj, "name", PROJECT)
    sub(proj, "pids", str(PROFILE_ID))
    sub(proj, "scenes", SCENE)
    sub(proj, "tids", ",".join(str(i) for i in TASK_IDS.values()))

    prof = sub(root, "Profile", sr=f"prof{PROFILE_ID}", ve="2")
    for tag, val in (("cdate", TIME), ("edate", TIME), ("flags", "10"), ("id", PROFILE_ID),
                     ("mid0", TASK_IDS["CP Gesture"]), ("nme", "Claude Panel Gesture")):
        sub(prof, tag, val)
    ev = sub(prof, "Event", sr="con0", ve="2")
    sub(ev, "code", "201")
    sub(ev, "App", sr="arg0")
    for n in (1, 2, 3):
        s_arg(ev, n)

    sc = sub(root, "Scene", sr=f"scene{SCENE}")
    rect_el(sc, 0, "Panel", (0, 0, W, H), "#FF202124")   # opaque backdrop behind every element
    rect_el(sc, 10, "Divider", (40, 150, W - 80, 4), "#33FFFFFF")
    text_el(sc, 1, "Title", "Claude", (40, 30, 520, 110), 30)
    text_el(sc, 2, "Status", "● …", (480, 40, W - 520, 100), 18, "#FF9E9E9E")
    text_el(sc, 3, "Heard", "You: …", (40, 170, W - 80, 120), 16, "#FFB0BEC5", long_text=1)
    text_el(sc, 4, "Reply", "Claude: …", (40, 295, W - 80, 270), 18, long_text=1)
    rect_el(sc, 11, "Divider2", (40, 575, W - 80, 4), "#33FFFFFF")
    text_el(sc, 12, "Prompt", "\U0001F4C4 …", (40, 595, W - 80, 300), 15, "#FFFFE082", long_text=1)
    text_el(sc, 13, "Jobs", "Jobs: …", (40, 905, W - 80, 110), 15, "#FF80CBC4", long_text=1)
    gap = 20
    rows = [(1030, [("DictateBtn", "\U0001F399 Dictate", "CP Dictate"), ("TemplateBtn", "\U0001F4C4 Template ▾", "CP Template"),
                    ("RepoBtn", "\U0001F4E6 Repo ▾", "CP Repo"), ("SendBtn", "\U0001F680 Send", "CP Send")]),
            (1180, [("TalkBtn", "\U0001F3A4 Talk", "CP Talk"), ("NewBtn", "➕ New", "CP New"),
                    ("StopBtn", "⏹ Stop", "CP Stop"), ("TypeBtn", "⌨ Type", "CP Type"), ("CloseBtn", "✕", "CP Close")])]
    k = 20
    for y, buttons in rows:
        bw = (W - 80 - (len(buttons) - 1) * gap) // len(buttons)
        for i, (name, label, task) in enumerate(buttons):
            button_el(sc, k, name, label, (40 + i * (bw + gap), y, bw, 130), TASK_IDS[task], 17)
            k += 1
    sub(sc, "widthLand", "-1")
    sub(sc, "widthPort", W)
    sub(sc, "heightLand", "-1")
    sub(sc, "heightPort", H)
    sub(sc, "nme", SCENE)
    sub(sc, "cdate", TIME)
    sub(sc, "edate", TIME)
    props = sub(sc, "PropertiesElement", sr="props")
    i_arg(props, 0, 1)
    i_arg(props, 1, 0)
    s_arg(props, 2, "#F0202124")   # panel background
    i_arg(props, 3, 0)
    s_arg(props, 4, SCENE)
    s_arg(props, 5)
    sub(props, "Img", sr="arg6", ve="2")
    s_arg(props, 7)

    for name, tid in TASK_IDS.items():
        t = sub(root, "Task", sr=f"task{tid}")
        for tag, val in (("cdate", TIME), ("edate", TIME), ("id", tid), ("nme", name), ("pri", "100")):
            sub(t, tag, val)
        n = 0
        if name in PICKERS:
            javascriptlet(t, 0, js_for(name + " list", token), 20)
            list_dialog(t, 1, PICKERS[name], "%cp_items")
            javascriptlet(t, 2, js_for(name + " pick", token), 20)
            continue
        if name == "CP Type":
            javascriptlet(t, 0, js_for("CP Type hide", token), 10)
            input_dialog(t, 1, "Ask Claude", "Type a question; Claude answers out loud.")
            n = 2
        javascriptlet(t, n, js_for(name, token), JS_TIMEOUT.get(name, 20))
        if name == "CP Poll":   # loop while the panel is open
            wait_ms(t, n + 1, POLL_WAIT_MS)
            goto_if_matches(t, n + 2, 1, "%CP_OPEN", "1")
    return root


def to_xml(root: ET.Element) -> str:
    ET.indent(root, "\t")
    return ET.tostring(root, encoding="unicode") + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("out", nargs="?", help="output .prj.xml (mode 600)")
    ap.add_argument("--token-file", type=Path, default=DEFAULT_TOKEN_FILE)
    ap.add_argument("--dry-run", action="store_true", help="fake token, print the XML")
    a = ap.parse_args(argv)
    if a.dry_run:
        sys.stdout.write(to_xml(build("DRY-RUN-TOKEN-0123456789")))
        return 0
    if not a.out or not a.out.endswith(".prj.xml"):
        ap.error("need OUT ending in .prj.xml (Tasker's importer goes by the extension)")
    try:
        token = a.token_file.read_text().strip()
    except OSError as e:
        sys.exit(f"no token: {a.token_file}: {e.strerror} (start the trigger: phone-services va-restart)")
    out = Path(a.out)
    out.touch(mode=0o600)
    out.chmod(0o600)
    out.write_text(to_xml(build(token)))
    print(f"wrote {out} ({len(TASK_IDS)} tasks, scene '{SCENE}', profile 'Claude Panel Gesture')")
    return 0


if __name__ == "__main__":
    sys.exit(main())
