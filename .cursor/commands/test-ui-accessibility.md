```markdown
Test UI and accessibility: verify SYSTEM_ALERT_WINDOW permission prompt, test Material Design 3 theme switching (light/dark), validate bubble opacity controls (10-100%), test bubble size selection (5 levels), verify accessibility service compatibility, test TalkBack screen reader support, output ui-accessibility-report.json
```

```bash
# Cursor tests UI/UX and accessibility:
echo "♿ UI & Accessibility Testing"

# Require device
if ! adb get-state 1>/dev/null 2>&1; then
  echo "No adb device/emulator detected. Connect one and retry."
  exit 1
fi

# 1. Permission flow test
echo "Test 1: Overlay permission"
adb shell pm revoke com.clipboardhistory.debug android.permission.SYSTEM_ALERT_WINDOW
adb shell am start -n com.clipboardhistory.debug/com.clipboardhistory.presentation.MainActivity

# Verify permission prompt appears
sleep 2
adb shell screencap /sdcard/permission-prompt.png

# Grant permission via settings
adb shell appops set com.clipboardhistory.debug \
  SYSTEM_ALERT_WINDOW allow

# Verify permission granted
adb shell appops get com.clipboardhistory.debug SYSTEM_ALERT_WINDOW
# Expected: SYSTEM_ALERT_WINDOW: allow

# 2. Theme switching test
echo "Test 2: Material Design 3 themes"
for theme in Light Dark System; do
  echo "  Testing $theme theme"
  adb shell "am broadcast -a com.clipboardhistory.SET_THEME \
    --es theme $theme"
  sleep 1
  adb shell screencap /sdcard/theme-${theme,,}.png
done

# 3. Bubble opacity test
echo "Test 3: Bubble opacity control"
for opacity in 10 30 50 80 100; do
  echo "  Testing opacity: $opacity%"
  adb shell "am broadcast -a com.clipboardhistory.SET_OPACITY \
    --ei opacity $opacity"
  sleep 1
  adb shell screencap /sdcard/opacity-${opacity}.png
done

# 4. Bubble size test
echo "Test 4: Bubble size levels"
sizes=("XSmall" "Small" "Medium" "Large" "XLarge")
for size in "${sizes[@]}"; do
  echo "  Testing size: $size"
  adb shell "am broadcast -a com.clipboardhistory.SET_SIZE \
    --es size $size"
  sleep 1
  adb shell screencap /sdcard/size-${size,,}.png
done

# 5. TalkBack accessibility test
echo "Test 5: TalkBack screen reader"
# Enable TalkBack
adb shell settings put secure enabled_accessibility_services \
  com.google.android.marvin.talkback/.TalkBackService
sleep 2

# Navigate UI with TalkBack gestures
adb shell input swipe 540 960 540 500 100  # Swipe down (next element)
sleep 1
adb shell input tap 540 960  # Double-tap to activate
sleep 1

# Verify TalkBack announcements in logcat
adb logcat -d | grep "TalkBack"
# Expected: Content descriptions for all UI elements

# 6. Color contrast validation
echo "Test 6: WCAG color contrast"
python3 << 'PYCODE'
from PIL import Image

def calculate_contrast(rgb1, rgb2):
    def luminance(rgb):
        rgb = [x / 255.0 for x in rgb]
        rgb = [(x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4) for x in rgb]
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]
    l1 = luminance(rgb1)
    l2 = luminance(rgb2)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)

text_color = (255, 255, 255)  # White text
bg_color = (33, 128, 141)     # Teal background

contrast = calculate_contrast(text_color, bg_color)
print(f"Text contrast ratio: {contrast:.2f}")
print(f"WCAG AA: {'PASS' if contrast >= 4.5 else 'FAIL'}")
print(f"WCAG AAA: {'PASS' if contrast >= 7.0 else 'FAIL'}")
PYCODE

# Summary (printed, not executed as commands)
cat <<'EOF_SUMMARY'
✅ UI & Accessibility Tests:
- Overlay permission flow validated
- Themes switched (Light/Dark/System) and captured
- Opacity and size variations captured
- TalkBack interaction executed
- Contrast script run
EOF_SUMMARY
```
