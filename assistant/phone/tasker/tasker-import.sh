# Sourced by import-*.sh: hand one .prf/.tsk/.prj.xml file to Tasker's own importer.
#
#   tasker_import LOCAL_FILE NAME.prj.xml
#
# Pushes the file to /sdcard/Download, starts ActivityImportTaskerDataFromXml on it (Tasker shows
# "Import Data" with the monitors and actions; the user taps YES), waits until Tasker's
# autobackup.xml changes, and deletes the pushed copy only then: the importer re-reads the file
# after the first YES (a task import also asks for the project). The caller's EXIT trap must
# delete $TASKER_IMPORT_DEST too (see import-claude-panel.sh), so a failed run leaves no copy.
# Tasker's IntentHandler file:// handler only copies files into Tasker/profiles; it does not import.

TASKER_SERIAL=${DROID_SERIAL:-127.0.0.1:5555}
TASKER_PKG=net.dinglisch.android.taskerm
TASKER_BACKUP=/data/data/$TASKER_PKG/files/autobackup.xml

_tasker_adb() { adb -s "$TASKER_SERIAL" "$@"; }
_tasker_mtime() { _tasker_adb shell "su -c 'stat -c %Y $TASKER_BACKUP'" 2>/dev/null | tr -d '\r'; }

tasker_import() {
  local src=$1 name=$2 dest before now
  case $name in *.prf.xml|*.tsk.xml|*.prj.xml) ;; *) echo "tasker_import: $name: need .prf/.tsk/.prj.xml" >&2; return 2 ;; esac
  dest=/sdcard/Download/$name
  TASKER_IMPORT_DEST=$dest
  before=$(_tasker_mtime)
  _tasker_adb push "$src" "$dest" >/dev/null
  _tasker_adb shell "am start -a android.intent.action.VIEW -t text/xml -d file://$dest \
    -n $TASKER_PKG/com.joaomgcd.taskerm.datashare.import.ActivityImportTaskerDataFromXml" >/dev/null
  echo "Tasker shows 'Import Data'. Check the list of actions, tap YES (and YES to enable)."
  echo "Waiting up to ${TASKER_IMPORT_WAIT:-90} s for Tasker to save its config..."
  for _ in $(seq "${TASKER_IMPORT_WAIT:-90}"); do
    sleep 1
    now=$(_tasker_mtime)
    if [ -n "$now" ] && [ "$now" != "$before" ]; then
      _tasker_adb shell "rm -f $dest" >/dev/null 2>&1 || true
      echo "imported (Tasker config saved)"
      return 0
    fi
  done
  echo "no import seen; the pushed file is deleted on exit, run again when ready" >&2
  return 1
}
