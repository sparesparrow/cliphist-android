# Operations

## Health and recovery
| Symptom | Check | Fix |
|---|---|---|
| Anything | `phone-services status` | `phone-services ensure` (the watchdog runs it every 60 s) |
| adb DOWN | port 5555 closed? | the Magisk boot script `/data/adb/service.d/99-droid-adb.sh` restarts adbd at boot; manually: `phone-services native 'sudo sh /data/adb/service.d/99-droid-adb.sh'` |
| mic DOWN | `phone-services status` | `phone-services pa-restart` (`--push` after a Termux pulseaudio upgrade) |
| mic indicator always on | `pactl list short sources` shows OpenSL_ES_source IDLE, not SUSPENDED | `mic.pa` must load `module-suspend-on-idle`; `phone-services pa-restart --push` |
| mic up but silence | `phone-services test-mic 3` | max amplitude around 0.001 means silence; speech gives roughly 0.1 or more |
| va-http DOWN | `curl 127.0.0.1:8780/health` | `phone-services va-restart`; log in `voice/logs/http.log` |
| native sshd down | port 8022 | open a new Termux session (its `.bashrc` starts sshd), or reboot (boot script) |
| `/voice` in Claude Code fails | `echo $AUDIODRIVER $PULSE_COOKIE` | start Claude from a new Debian login (`cc --continue` in Termux) |

## Why things are built this way
- Termux does not declare RECORD_AUDIO. The mic server therefore runs as the adb shell user
  (uid 2000) from `/data/local/tmp/pa`, served over TCP with a cookie. Loopback is shared by all
  Android apps, so never make it anonymous or use `auth-ip-acl`.
- PRoot cannot see Magisk's su (`/debug_ramdisk`) or the termux-am socket (`files/apps`). Root
  goes through `adb shell "su -c ..."` or the native Termux shell (`phone-services native`).
- A server started through Magisk su runs in another SELinux domain; uid 2000 cannot signal it.
  `phone-services pa-stop` falls back to su.
- `pkill -f <pattern>` matches the caller's own command line. Use pid files or `'[/]path'`.
- Long-lived processes started from `ensure` must not inherit its lock fds (`8>&- 9>&-`).

## Startup
- Phone boot: Magisk `/data/adb/service.d/98-phone-services.sh` starts the mic server and sshd.
- Debian login: `/etc/profile.d/phone-services.sh` exports the audio variables and starts
  `phone-services watch`.
- Claude starts the MCP servers itself (`claude mcp get android-assistant`).

## Why adbd restarts
**Conclusion.** adbd restarts each time the keyguard (lock screen) is shown or dismissed. It does
not restart when the screen merely turns on or off. The logs below show this directly. The
mechanism is a hypothesis. On every keyguard change, `UsbDeviceManager` switches the USB
functions: none plus `sys.usb.secure_mode` true when the lock screen shows, `mtp` plus secure
mode false when it is dismissed. Init probably restarts adbd on each `sys.usb.config` change,
which would take the TCP listener on 5555 down with it. The init side is not logged (logcat has
no `init` lines here), and the OEM setting behind `sys.usb.secure_mode` has not been found.

Evidence from 2026-11-08 (`logcat -d -b all -v time`, `ps -A -o PID,PPID,STIME,NAME`). An adbd
start is the `Read failed` line (the old adbd is gone) followed about 2 s later by
`Received connected key message`. ps STIME can differ from the log time by about 1 s.

| adbd PID | ps STIME | log: old adbd gone | Keyguard event just before it |
|---|---|---|---|
| (before 16641) | ~18:19:40 | before the buffer start | missing: the system/events buffers begin at 18:19:41. The first event logged is an unlock, `18:19:41.960 wm_set_keyguard_shown [0,0,0,1,0]` |
| 16641 | 18:22:4x | 18:22:39.244 | `18:22:39.124 wm_set_keyguard_shown [0,1,0,0,0,setKeyguardShown]` (lock), `18:22:39.131 UsbDeviceManager: Setting USB config to` (empty), `18:22:39.222 Set USB secure = true` |
| 17115 | 18:25:1x | 18:25:11.960 | `18:25:11.514 Waking up ... WAKE_REASON_BIOMETRIC`, `18:25:11.871 wm_set_keyguard_shown [0,0,0,1,0]` (unlock), `18:25:11.880 Setting USB config to mtp`, `18:25:11.928 Set USB secure = false` |
| 18064 | 18:35:06 | 18:35:06.995 | `18:35:05.906 Going to sleep due to power_button`, `18:35:06.863 setKeyguardShown [0,1,0,0,0]` (lock), `18:35:06.894 Setting USB config to` (empty) |
| 20461 | 18:45:33 | 18:45:34.409 | `18:45:29.289 Going to sleep due to timeout`, `18:45:34.360 setKeyguardShown [0,1,0,0,0]` (lock), `18:45:34.362 Setting USB config to` (empty) |
| 25599 | 19:09:30 | 19:09:30.823 | `19:09:30.737 setKeyguardShown [0,0,0,1,0]` (unlock), `19:09:30.747 Setting USB config to mtp`, `19:09:30.780 Set USB secure = false` |

Two observations support the keyguard explanation:
- **18:35.** The phone was locked and then unlocked within one second
  (`18:35:06.894 Setting USB config to`, then `18:35:07.714 Setting USB config to mtp`). The log
  shows only one adbd drop (`Read failed` at 18:35:06.995) and one reconnect (18:35:09.010).
  Probably the unlock arrived while the new adbd was still starting, so the two restarts merged
  into one. This is not verified.
- **19:03 and 19:04.** The screen turned on and off twice while it stayed locked
  (`19:03:53 Waking up ... POWER_BUTTON`, `19:03:57 Going to sleep`, `19:04:11 Waking up`,
  `19:04:16 Going to sleep`). There was no keyguard change and no USB config change, and adbd
  20461 survived. It was replaced only at the unlock at 19:09:30.

Properties: `init.svc.adbd=running`, `persist.sys.usb.config=adb`, `sys.usb.config=mtp,adb`,
`sys.usb.secure_mode=0` (unlocked), `service.adb.tcp.port=5555`, global `adb_wifi_enabled=0`.

**Our tooling is ruled out as the trigger (hypothesis, supported by logs):**
- Every adbd drop above has a keyguard and USB config change within 0.2 s of it.
- No drop occurs at any other time, although droid and `phone-services watch` call adb all the
  time.
- `adb connect` from droid or `phone-services` talks to the local adb server (PID 15637, running
  since 07:12). Nothing in the logs suggests it restarts adbd.
- `/data/adb/service.d/99-droid-adb.sh` (`stop adbd; start adbd`) runs only at boot.
- The tooling never runs `adb kill-server` and never writes `adb_enabled`.

**What keeps the mic server alive.** The mic server survives because of its SELinux domain, not
its PPID. When it is started through Magisk su (at boot, or by `phone-services pa-restart`), it runs
as user shell with label `u:r:magisk:s0`, outside adbd's session. It survived every restart above:
pid 16471, started 18:22:23. A server started by the plain `adb shell nohup` fallback runs in
`u:r:shell:s0`. It dies at the next adbd restart even when it has been reparented to PPID 1.
`phone_status` prints the mic server's pid, ppid, user, label and uptime, plus adbd's start time.
It warns on label `u:r:shell:s0`, on a user other than shell or root, and on more than one
process running `/data/local/tmp/pa/bin/pulseaudio`. The mic server is identified by
`/proc/<pid>/exe`, because pulseaudio rewrites its argv[0].

**Recovery.** The adb link drops for about 2 s at each lock or unlock. An adb-spawned mic server
would not come back until the next `phone-services watch` pass. That loop runs every 60 s, so
recovery can take up to about 60 s plus restart time.

**How to confirm the mechanism:** note the adbd PID, lock the screen, check it, unlock, and
check again (`ps -A -o PID,STIME,NAME | grep adbd`; `logcat -b events | grep keyguard`). Expect a
new PID after each lock and each unlock, and none while the screen only turns on and off. Reading
init's log (`dmesg` as root, or `logcat -b kernel`) at the moment of a restart would show whether
init stops adbd because `sys.usb.config` changed.
