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
