#!/system/bin/sh
# Runs on the phone as the adb shell user (uid 2000), from /data/local/tmp/pa.
# Serves the phone microphone (OpenSL_ES_source) over PulseAudio TCP on 127.0.0.1:4713.
D=/data/local/tmp/pa
export LD_LIBRARY_PATH=$D/lib HOME=$D TMPDIR=$D/tmp XDG_RUNTIME_DIR=$D/run PULSE_RUNTIME_PATH=$D/run PULSE_STATE_PATH=$D/state
mkdir -p $D/tmp $D/run $D/state
pkill -f $D/bin/pulseaudio 2>/dev/null
exec $D/bin/pulseaudio -n --daemonize=no --exit-idle-time=-1 --disallow-exit --use-pid-file=no \
  --dl-search-path=$D/mod -F $D/mic.pa --log-target=file:$D/pa.log
