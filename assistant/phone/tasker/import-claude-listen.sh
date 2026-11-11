#!/bin/bash
# Import the Tasker profile "Claude Listen" without navigating Tasker's UI.
#
#   Event 201 "Assistance Request" (Tasker is the default digital assistant, so
#   long-press power / the assistant gesture fires it)
#   -> JavaScriptlet: shell() pipes a POST /listen with X-Token to 127.0.0.1:8780 via toybox nc.
#
# How: the XML (it contains the token) goes to /sdcard briefly, Tasker's own importer
# ActivityImportTaskerDataFromXml is started on it, the user taps YES on Tasker's
# "Import Data" dialog (it lists monitors and actions), then the file is deleted.
# Tasker's IntentHandler file:// handler only copies the file into Tasker/profiles; it does
# not import. Prints no token.
set -euo pipefail
SERIAL=${DROID_SERIAL:-127.0.0.1:5555}
TOKEN_FILE=${TOKEN_FILE:-/root/voice-assistant/state/http-token}
PORT=${VA_PORT:-8780}
DEST=/sdcard/Download/ClaudeListen.prf.xml
A=(adb -s "$SERIAL")

[ -s "$TOKEN_FILE" ] || { echo "no token at $TOKEN_FILE (start the trigger first: phone-services va-restart)" >&2; exit 1; }
token=$(tr -dc 'A-Za-z0-9_-' <"$TOKEN_FILE")
[ -n "$token" ] || { echo "token has unexpected characters" >&2; exit 1; }

tmp=$(mktemp); chmod 600 "$tmp"
cleanup() { rm -f "$tmp"; "${A[@]}" shell "rm -f $DEST" >/dev/null 2>&1 || true; }
trap cleanup EXIT

js="shell(\"printf 'POST /listen HTTP/1.0\\\\r\\\\nX-Token: $token\\\\r\\\\nContent-Length: 0\\\\r\\\\n\\\\r\\\\n' | nc -q 1 127.0.0.1 $PORT &gt;/dev/null 2&gt;&amp;1\", false, 10);"
cat >"$tmp" <<EOF
<TaskerData sr="" dvi="1" tv="6.7.6-beta">
	<Profile sr="prof9001" ve="2">
		<cdate>1794200000000</cdate>
		<edate>1794200000000</edate>
		<flags>10</flags>
		<id>9001</id>
		<mid0>9002</mid0>
		<nme>Claude Listen</nme>
		<Event sr="con0" ve="2">
			<code>201</code>
			<App sr="arg0"/>
			<Str sr="arg1" ve="3"/>
			<Str sr="arg2" ve="3"/>
			<Str sr="arg3" ve="3"/>
		</Event>
	</Profile>
	<Task sr="task9002">
		<cdate>1794200000000</cdate>
		<edate>1794200000000</edate>
		<id>9002</id>
		<nme>Claude Listen</nme>
		<pri>100</pri>
		<Action sr="act0" ve="7">
			<code>129</code>
			<Str sr="arg0" ve="3">$js</Str>
			<Str sr="arg1" ve="3"/>
			<Int sr="arg2" val="1"/>
			<Int sr="arg3" val="20"/>
		</Action>
	</Task>
</TaskerData>
EOF

"${A[@]}" push "$tmp" "$DEST" >/dev/null
"${A[@]}" shell "am start -a android.intent.action.VIEW -t text/xml -d file://$DEST \
  -n net.dinglisch.android.taskerm/com.joaomgcd.taskerm.datashare.import.ActivityImportTaskerDataFromXml" >/dev/null
echo "Tasker shows 'Import Data: Claude Listen'. Tap YES, then YES to enable it."
echo "Waiting up to 60 s for Tasker to save its config..."
before=$("${A[@]}" shell 'su -c "stat -c %Y /data/data/net.dinglisch.android.taskerm/files/autobackup.xml"' | tr -d '\r')
for _ in $(seq 60); do
  sleep 1
  now=$("${A[@]}" shell 'su -c "stat -c %Y /data/data/net.dinglisch.android.taskerm/files/autobackup.xml"' | tr -d '\r')
  if [ "$now" != "$before" ]; then echo "imported (Tasker config saved)"; exit 0; fi
done
echo "no import seen; the file is deleted on exit, so run again when ready" >&2
exit 1
