#!/system/bin/sh
# Run ONCE as root in native Termux:   sudo sh /data/data/com.termux/files/home/phone/setup-boot.sh
# (or from PRoot: phone-services native 'sudo sh ~/phone/setup-boot.sh')
# Installs a Magisk boot script that, after boot, starts:
#   - the mic PulseAudio server as the adb shell user (uid 2000, which holds RECORD_AUDIO)
#   - Termux's sshd on 127.0.0.1:8022 (key auth only) as the Termux user
# Undo: rm /data/adb/service.d/98-phone-services.sh
set -e
SVC=/data/adb/service.d/98-phone-services.sh
TUID=$(stat -c %u /data/data/com.termux)
mkdir -p /data/adb/service.d
cat > "$SVC" <<EOF
#!/system/bin/sh
until [ "\$(getprop sys.boot_completed)" = 1 ]; do sleep 2; done
sleep 15
if [ -x /data/local/tmp/pa/run.sh ]; then
  su 2000 -c 'nohup sh /data/local/tmp/pa/run.sh >/dev/null 2>&1 &'
fi
P=/data/data/com.termux/files/usr
su $TUID -c "HOME=/data/data/com.termux/files/home PREFIX=\$P PATH=\$P/bin LD_LIBRARY_PATH=\$P/lib \
  \$P/bin/sshd -p 8022 -o ListenAddress=127.0.0.1 -o PasswordAuthentication=no -o KbdInteractiveAuthentication=no"
EOF
chmod 755 "$SVC"
echo "OK: installed $SVC (runs at next boot)"
