# Operations

## Health and recovery
| Symptom | Check | Fix |
|---|---|---|
| Anything | `phone-services status` | `phone-services ensure` (the watchdog runs it every 60 s) |
| adb DOWN | port 5555 closed? | the Magisk boot script `/data/adb/service.d/99-droid-adb.sh` restarts adbd at boot; manually: `phone-services native 'sudo sh /data/adb/service.d/99-droid-adb.sh'` |
| mic DOWN | `phone-services status` | `phone-services pa-restart` (`--push` after a Termux pulseaudio upgrade) |
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
**Conclusion (supported by logs, not yet confirmed by a controlled lock/unlock test):** adbd is
restarted by Android itself every time the screen locks or unlocks, not by our tooling. On lock,
`UsbDeviceManager` switches the USB functions to none and sets the OEM USB secure mode; on unlock
it switches back to `mtp`. Init restarts adbd on every `sys.usb.config` change, and the TCP
listener on 5555 goes down with it. Everything started as a child of adbd dies.

Evidence from 2026-11-08 (`logcat -d -b all -v time`; adbd PIDs 16641, 17115, 18064 from
`ps -A -o PID,PPID,STIME,NAME`). Each adbd start matches a keyguard change within 0.2 s:
```
18:22:39.124 I/wm_set_keyguard_shown( 1851): [0,1,0,0,0,setKeyguardShown]    <- screen locked
18:22:39.131 I/UsbDeviceManager( 1851): Setting USB config to
18:22:39.222 E/UsbDeviceManager( 1851): Set USB secure = true by sys.usb.secure_mode
18:22:39.244 W/AdbDebuggingManager( 1851): Read failed with count -1         <- old adbd gone
18:22:41.250 D/AdbDebuggingManager( 1851): Received connected key message: ...@localhost
18:25:11.514 I/PowerManagerService( 1851): Waking up from Asleep (... WAKE_REASON_BIOMETRIC)
18:25:11.880 I/UsbDeviceManager( 1851): Setting USB config to mtp             <- unlocked
18:25:11.928 E/UsbDeviceManager( 1851): Set USB secure = false by sys.usb.secure_mode
18:25:11.960 W/AdbDebuggingManager( 1851): Read failed with count -1
18:35:05.906 I/PowerManagerService( 1851): Going to sleep due to power_button
18:35:06.894 I/UsbDeviceManager( 1851): Setting USB config to
18:35:06.995 W/AdbDebuggingManager( 1851): Read failed with count -1         <- adbd 18064, STIME 18:35:06
18:35:07.714 I/UsbDeviceManager( 1851): Setting USB config to mtp
```
The 18:19:40 restart falls just before the start of the system/events buffers; the first event
logged is an unlock (`18:19:41.960 wm_set_keyguard_shown [0,0,0,1,0]`), which fits the pattern.
Properties: `init.svc.adbd=running`, `persist.sys.usb.config=adb`, `sys.usb.config=mtp,adb`,
`sys.usb.secure_mode=0` (while unlocked), `service.adb.tcp.port=5555`, `adb_wifi_enabled=0`.

Our tooling is ruled out as the trigger:
- No AdbDebuggingManager disconnect or adbd start appears without a USB config change next to it.
- droid's `adb connect` and `phone-services`' `adb_connect` are client-side calls to the adb
  server (PID 15637, running since 07:12). They do not restart adbd on the phone.
  `phone-services` only calls `adb connect` when `adb shell true` fails.
- `/data/adb/service.d/99-droid-adb.sh` (`stop adbd; start adbd`) runs only at boot.
- Nothing in the tooling runs `adb kill-server` or writes `adb_enabled`.

Consequences and mitigation:
- Long-lived processes must not be children of adbd. The mic server is started through Magisk
  su with PPID 1, and it survived all three restarts above (pid 16471, started 18:22:24).
  `phone_status` prints the mic server's pid, ppid and uptime, plus adbd's start time. It warns if
  the mic server's ppid is adbd's pid.
- Expect `adb -s 127.0.0.1:5555` to fail for 1 to 3 s after each lock or unlock. The
  `phone-services watch` loop reconnects.
- To confirm: note the adbd PID, lock the screen with the power button, unlock it, and check the
  PID again (`ps -A -o PID,STIME,NAME | grep adbd`, `logcat -b events | grep keyguard`). A new
  PID after each lock and none while the screen stays locked confirms the cause. The OEM USB
  secure-mode setting that drives `sys.usb.secure_mode` has not been identified yet.
