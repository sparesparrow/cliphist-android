#!/bin/bash
# Build a self-contained PulseAudio bundle from Termux's pulseaudio package, to run on the
# phone as the adb shell user (uid 2000). That user holds RECORD_AUDIO; Termux does not
# declare it in its manifest, so module-sles-source can never open the mic inside Termux.
# Re-run after `pkg upgrade pulseaudio` in Termux, then `phone-services pa-restart --push`.
set -eu
T=/data/data/com.termux/files/usr
HERE=$(cd "$(dirname "$0")" && pwd)
B=$HERE/bundle
M=$T/lib/pulseaudio/modules
rm -rf "$B"; mkdir -p "$B/lib" "$B/mod" "$B/bin"
cp "$T/bin/pulseaudio" "$B/bin/"
for m in module-sles-source module-sles-sink module-native-protocol-tcp; do cp "$M/$m.so" "$B/mod/"; done
cp "$HERE/mic.pa" "$HERE/run.sh" "$B/"
# Auth cookie: loopback is shared by every Android app, so the mic server must not be anonymous.
# Clients (PRoot) read it via PULSE_COOKIE; on the phone it is mode 600, owned by uid 2000.
[ -s "$HERE/cookie" ] || { (umask 077; head -c 256 /dev/urandom > "$HERE/cookie"); }
cp "$HERE/cookie" "$B/cookie"; chmod 600 "$B/cookie"
# Android system libraries the bundle may link against (provided by the OS, not copied).
SYS=" libc.so libdl.so libm.so liblog.so libOpenSLES.so libandroid.so libaaudio.so libz.so "
queue="$B/bin/pulseaudio $(echo "$B"/mod/*.so)"; seen=" "
while [ -n "${queue// /}" ]; do
  set -- $queue; f=$1; shift; queue="$*"
  for n in $(readelf -d "$f" | sed -n 's/.*NEEDED.*\[\(.*\)\]/\1/p'); do
    case "$SYS$seen" in *" $n "*) continue;; esac
    seen="$seen$n "
    src=$(ls "$T/lib/pulseaudio/$n" "$T/lib/$n" "$M/$n" 2>/dev/null | head -1 || true)
    [ -n "$src" ] || { echo "MISSING $n (needed by $f)" >&2; exit 1; }
    cp -L "$src" "$B/lib/"; queue="$queue $B/lib/$n"
  done
done
echo "bundle: $B ($(du -sh "$B" | cut -f1), $(ls "$B/lib" | wc -l) libs)"
