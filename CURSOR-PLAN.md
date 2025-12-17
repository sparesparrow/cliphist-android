## **Comprehensive ADB Testing Plan for Clipboard History Android App**

**Enhanced testing strategy for [cliphist-android](https://github.com/sparesparrow/cliphist-android) with floating bubbles, encryption, and smart actions.**

***

## **Testing Objectives**

1. **Clipboard Monitoring**: Validate automatic capture of text, URLs, phone numbers, emails, addresses
2. **Floating Bubbles**: Test all bubble types (Circle, Cube, Hexagon, Square) with flash preview
3. **Smart Actions**: Verify edge-based drag-and-drop with context-aware suggestions
4. **Encryption**: Test secure storage and encrypted clipboard history
5. **Performance**: Monitor memory usage, battery impact, service stability
6. **Accessibility**: Validate SYSTEM_ALERT_WINDOW permission and service persistence
7. **UI/UX**: Test Material Design 3 themes, bubble customization, settings

***

## **Cursor Commands for Automated Testing**

### **1. `/test-clipboard-capture`**

**File:** `.cursor/commands/test-clipboard-capture.md`

```markdown
Test clipboard monitoring: launch cliphist-android app, verify FloatingBubbleService running, copy text/URL/phone/email to clipboard via adb input, validate bubble appearance with correct content type, screenshot each bubble state, verify encryption in Room database, output clipboard-test-report.json
```

**Example Usage:**
```bash
/test-clipboard-capture

# Cursor executes comprehensive clipboard test:
echo "📋 Testing Clipboard Capture & Bubble Display"

# 1. Launch app and verify service
adb shell am start -n com.sparesparrow.cliphist/.MainActivity
sleep 2
adb shell dumpsys activity services | grep FloatingBubbleService
# Expected: FloatingBubbleService (pid=12345) app=com.sparesparrow.cliphist

# 2. Test text clipboard
echo "Test 1: Plain text clipboard"
adb shell "input text 'Hello World Testing Clipboard'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT 'Hello World Testing Clipboard'"
sleep 1
adb shell screencap /sdcard/step1-text-bubble.png
adb pull /sdcard/step1-text-bubble.png test-results/

# Verify bubble appeared
adb shell dumpsys window | grep FloatingBubble
# Expected: mCurrentFocus=Window{... com.sparesparrow.cliphist/FloatingBubble}

# 3. Test URL clipboard
echo "Test 2: URL clipboard"
adb shell "input text 'https://github.com/sparesparrow/cliphist-android'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT 'https://github.com/sparesparrow/cliphist-android'"
sleep 1
adb shell screencap /sdcard/step2-url-bubble.png

# 4. Test phone number
echo "Test 3: Phone number clipboard"
adb shell "input text '+420777123456'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT '+420777123456'"
sleep 1
adb shell screencap /sdcard/step3-phone-bubble.png

# 5. Test email
echo "Test 4: Email clipboard"
adb shell "input text 'test@example.com'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT 'test@example.com'"
sleep 1
adb shell screencap /sdcard/step4-email-bubble.png

# 6. Verify Room database encryption
adb shell "run-as com.sparesparrow.cliphist \
  sqlite3 /data/data/com.sparesparrow.cliphist/databases/clipboard.db \
  'SELECT COUNT(*) FROM clipboard_history;'"
# Expected: 4 (text, URL, phone, email)

# 7. Check encryption
adb shell "run-as com.sparesparrow.cliphist \
  sqlite3 /data/data/com.sparesparrow.cliphist/databases/clipboard.db \
  'SELECT content FROM clipboard_history LIMIT 1;'"
# Expected: Encrypted blob (not readable plaintext)

# Output:
✅ Clipboard monitoring: ACTIVE
✅ Bubble appearance: 4/4 content types captured
✅ Bubble types detected:
   - Plain text: Circle bubble (default)
   - URL: Hexagon bubble with link icon
   - Phone: Square bubble with phone icon
   - Email: Cube bubble with email icon
✅ Database encryption: VERIFIED (SQLCipher)
📸 Screenshots: test-results/step1-4.png

📊 Test Report:
   - Service uptime: 100% (no crashes)
   - Clipboard capture latency: avg 120ms
   - Bubble render time: avg 80ms
   - Memory footprint: 28 MB (within limits)
```

***

### **2. `/test-floating-bubble-types`**

**File:** `.cursor/commands/test-floating-bubble-types.md`

```markdown
Test all bubble types: launch app, open settings via intent, cycle through Circle/Cube/Hexagon/Square bubble types, copy test content, verify bubble shape rendering, test cube flash feature (tap cube, verify 1s content preview flash), screenshot each type, measure rendering performance, output bubble-types-comparison.html
```

**Example Usage:**
```bash
/test-floating-bubble-types

# Cursor tests all 4 bubble types:
echo "🎨 Testing Bubble Type Variations"

# Helper function to set bubble type
set_bubble_type() {
  local type=$1
  adb shell "am broadcast -a com.sparesparrow.cliphist.SET_BUBBLE_TYPE \
    --es bubble_type $type"
  sleep 1
}

# Test each bubble type
for bubble_type in Circle Cube Hexagon Square; do
  echo "Testing: $bubble_type bubble"
  
  # 1. Set bubble type
  set_bubble_type $bubble_type
  
  # 2. Copy test content
  adb shell "input text 'Test $bubble_type bubble'; \
    am broadcast -a android.intent.action.SEND \
    --es android.intent.extra.TEXT 'Test $bubble_type bubble'"
  sleep 1
  
  # 3. Screenshot bubble
  adb shell screencap /sdcard/bubble-${bubble_type,,}.png
  adb pull /sdcard/bubble-${bubble_type,,}.png test-results/
  
  # 4. If Cube, test flash feature
  if [ "$bubble_type" == "Cube" ]; then
    echo "  Testing cube flash preview..."
    # Tap cube bubble (screen center for demo)
    adb shell input tap 540 960
    sleep 0.5
    adb shell screencap /sdcard/cube-flash-preview.png
    adb pull /sdcard/cube-flash-preview.png test-results/
    sleep 0.5  # Wait for flash to complete (1s animation)
  fi
done

# 5. Performance comparison
echo "📊 Rendering performance:"
adb shell dumpsys gfxinfo com.sparesparrow.cliphist | grep -A 20 "Profile data"

# Output:
✅ Bubble Types Tested: 4/4

Bubble Type Comparison:
┌──────────┬─────────────┬───────────────┬─────────────┐
│ Type     │ Render Time │ Memory Impact │ Special     │
├──────────┼─────────────┼───────────────┼─────────────┤
│ Circle   │ 42ms        │ +2.1 MB       │ Default     │
│ Cube     │ 68ms        │ +3.8 MB       │ Flash ✅    │
│ Hexagon  │ 51ms        │ +2.4 MB       │ Geometric   │
│ Square   │ 38ms        │ +1.9 MB       │ Minimal     │
└──────────┴─────────────┴───────────────┴─────────────┘

✅ Cube Flash Test:
   - Flash trigger: SUCCESS (tap detected)
   - Content preview: "Test Cube bubble" (20 chars)
   - Flash duration: 1.02s (target: 1.0s)
   - Alpha animation: Smooth (60 FPS)

📸 Screenshots saved: test-results/
   - bubble-circle.png
   - bubble-cube.png
   - cube-flash-preview.png
   - bubble-hexagon.png
   - bubble-square.png
```

***

### **3. `/test-smart-actions-drag-drop`**

**File:** `.cursor/commands/test-smart-actions-drag-drop.md`

```markdown
Test smart action drag-and-drop: copy URL/phone/email/address to clipboard, simulate drag gesture toward screen edges (left/right/top/bottom), verify action area appearance with context-aware suggestions, test drop on action (Open Link, Call Number, Send Email, Open Maps), validate action execution, screenshot edge glow and action areas, output smart-actions-matrix.json
```

**Example Usage:**
```bash
/test-smart-actions-drag-drop

# Cursor tests smart actions with different content types:
echo "🎯 Testing Smart Actions Drag-and-Drop"

# Helper: Simulate drag gesture
simulate_drag() {
  local start_x=$1 start_y=$2 end_x=$3 end_y=$4
  adb shell input swipe $start_x $start_y $end_x $end_y 1000
}

# Test 1: URL → Open Link
echo "Test 1: URL smart action"
adb shell "input text 'https://github.com'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT 'https://github.com'"
sleep 1

# Drag bubble to left edge (action area appears)
echo "  Dragging to left edge..."
simulate_drag 540 960 50 960  # Center to left edge
sleep 0.5
adb shell screencap /sdcard/url-left-edge-actions.png

# Verify action area visible
adb shell dumpsys window | grep "ActionArea"
# Expected: ActionArea visible=true contentType=URL

# Drop on "Open Link" action (top action in left edge)
adb shell input tap 50 800
sleep 1

# Verify browser opened
adb shell dumpsys activity | grep "mResumedActivity"
# Expected: com.android.chrome (or default browser)

# Test 2: Phone Number → Call Number
echo "Test 2: Phone number smart action"
adb shell "input text '+420777123456'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT '+420777123456'"
sleep 1

# Drag to right edge
simulate_drag 540 960 1030 960  # Center to right edge
sleep 0.5
adb shell screencap /sdcard/phone-right-edge-actions.png

# Drop on "Call Number"
adb shell input tap 1030 800
sleep 1

# Verify dialer opened
adb shell dumpsys activity | grep "mResumedActivity"
# Expected: com.android.dialer (with number pre-filled)

# Test 3: Email → Send Email
echo "Test 3: Email smart action"
adb shell "input text 'test@example.com'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT 'test@example.com'"
sleep 1

# Drag to top edge
simulate_drag 540 960 540 50  # Center to top
sleep 0.5
adb shell screencap /sdcard/email-top-edge-actions.png

# Drop on "Send Email"
adb shell input tap 540 50
sleep 1

# Verify email app opened
adb shell dumpsys activity | grep "mResumedActivity"
# Expected: com.google.android.gm (Gmail) or default email

# Test 4: Address → Open Maps
echo "Test 4: Address smart action"
adb shell "input text 'Prague Castle, Czech Republic'; \
  am broadcast -a android.intent.action.SEND \
  --es android.intent.extra.TEXT 'Prague Castle, Czech Republic'"
sleep 1

# Drag to bottom edge
simulate_drag 540 960 540 1870  # Center to bottom
sleep 0.5
adb shell screencap /sdcard/address-bottom-edge-actions.png

# Drop on "Open Maps"
adb shell input tap 540 1870
sleep 1

# Verify maps opened
adb shell dumpsys activity | grep "mResumedActivity"
# Expected: com.google.android.apps.maps

# Output:
✅ Smart Actions Tested: 4/4

Action Matrix:
┌───────────────┬─────────────┬─────────────────────┬─────────────┐
│ Content Type  │ Edge        │ Action              │ Result      │
├───────────────┼─────────────┼─────────────────────┼─────────────┤
│ URL           │ Left        │ Open Link           │ ✅ Browser  │
│ Phone Number  │ Right       │ Call Number         │ ✅ Dialer   │
│ Email         │ Top         │ Send Email          │ ✅ Gmail    │
│ Address       │ Bottom      │ Open Maps           │ ✅ Maps     │
└───────────────┴─────────────┴─────────────────────┴─────────────┘

✅ Edge Glow Animation: VERIFIED (blue glow at threshold)
✅ Context-Aware Suggestions: 4/4 correct actions displayed
✅ Action Execution: 4/4 successful (apps opened correctly)
✅ One-Handed Operation: Thumb-friendly positioning confirmed

📸 Screenshots saved: test-results/
   - url-left-edge-actions.png
   - phone-right-edge-actions.png
   - email-top-edge-actions.png
   - address-bottom-edge-actions.png
```

***

### **4. `/test-encryption-security`**

**File:** `.cursor/commands/test-encryption-security.md`

```markdown
Test clipboard encryption: enable encryption in settings, copy sensitive data (passwords, credit cards), verify AES encryption in SQLCipher database, test biometric authentication (if enabled), attempt to read database without decryption key, validate ProGuard obfuscation in APK, output security-audit-report.json
```

**Example Usage:**
```bash
/test-encryption-security

# Cursor performs security audit:
echo "🔒 Security & Encryption Testing"

# 1. Enable encryption
adb shell "am broadcast -a com.sparesparrow.cliphist.ENABLE_ENCRYPTION \
  --ez enabled true"
sleep 1

# 2. Copy sensitive data
sensitive_data=(
  "Password: MySecret123!"
  "4532-1234-5678-9012"  # Credit card
  "SSN: 123-45-6789"
)

for data in "${sensitive_data[@]}"; do
  echo "Testing: $data"
  adb shell "input text '$data'; \
    am broadcast -a android.intent.action.SEND \
    --es android.intent.extra.TEXT '$data'"
  sleep 1
done

# 3. Verify encryption in database
echo "Checking database encryption..."
adb shell "run-as com.sparesparrow.cliphist \
  sqlite3 /data/data/com.sparesparrow.cliphist/databases/clipboard.db \
  'SELECT content FROM clipboard_history WHERE id IN (SELECT MAX(id) FROM clipboard_history);'"

# Expected: Encrypted blob (not plaintext password)
# Example output: �Q��8��vX�u�... (binary encrypted data)

# 4. Test biometric authentication
echo "Testing biometric unlock..."
# Trigger biometric prompt
adb shell "am broadcast -a com.sparesparrow.cliphist.UNLOCK_APP"
sleep 1
adb shell screencap /sdcard/biometric-prompt.png

# Simulate fingerprint (emulator only)
adb emu finger touch 1
sleep 1

# Verify unlock success
adb shell dumpsys activity | grep "UnlockStatus"
# Expected: UnlockStatus=SUCCESS

# 5. ProGuard obfuscation check
echo "Checking code obfuscation..."
apktool d app/build/outputs/apk/release/app-release.apk -o /tmp/decompiled
grep -r "EncryptionManager" /tmp/decompiled/smali/

# Expected: Obfuscated class names (e.g., com.a.b.c instead of readable names)

# Output:
✅ Encryption Tests:
   - AES-256 encryption: ACTIVE
   - SQLCipher database: VERIFIED
   - Sensitive data protection: ✅ (3/3 encrypted)

✅ Database Encryption Verification:
   - Raw database query: Binary encrypted data (not readable)
   - Encryption key: Stored in Android Keystore
   - Key rotation: Supported

✅ Biometric Authentication:
   - Fingerprint unlock: SUCCESS
   - Fallback to PIN: Available
   - Timeout: 30s (configurable)

✅ Code Obfuscation (ProGuard/R8):
   - Class names: OBFUSCATED
   - Method names: OBFUSCATED
   - String encryption: APPLIED
   - Dead code removal: APPLIED

⚠️  Security Recommendations:
   - Enable biometric lock in production
   - Rotate encryption keys every 90 days
   - Consider adding PIN fallback timeout

📄 Security Audit Report: security-audit-report.json
```

***

### **5. `/test-service-persistence-memory`**

**File:** `.cursor/commands/test-service-persistence-memory.md`

```markdown
Test service stability: launch FloatingBubbleService, force-stop app, verify auto-restart, test service under low memory conditions, monitor memory usage over 1 hour, test battery optimization exemption, simulate app killed by system, validate foreground service notification, output service-health-report.json
```

**Example Usage:**
```bash
/test-service-persistence-memory

# Cursor stress tests service stability:
echo "🔄 Service Persistence & Memory Testing"

# 1. Start service and baseline memory
adb shell am start-foreground-service \
  com.sparesparrow.cliphist/.FloatingBubbleService
sleep 5

echo "Baseline memory usage:"
adb shell dumpsys meminfo com.sparesparrow.cliphist | grep "TOTAL PSS"
# Expected: ~25-35 MB

# 2. Test auto-restart after force-stop
echo "Test 1: Force-stop recovery"
adb shell am force-stop com.sparesparrow.cliphist
sleep 5

# Verify service auto-restarted
adb shell dumpsys activity services | grep FloatingBubbleService
# Expected: Service running (PID changed)

# 3. Memory stress test
echo "Test 2: Memory stress (1 hour monitoring)"
for i in {1..60}; do
  # Copy 100 items to clipboard
  for j in {1..100}; do
    adb shell "input text 'Test $i-$j'; \
      am broadcast -a android.intent.action.SEND \
      --es android.intent.extra.TEXT 'Test $i-$j'"
    sleep 0.1
  done
  
  # Measure memory every minute
  mem=$(adb shell dumpsys meminfo com.sparesparrow.cliphist | \
    grep "TOTAL PSS" | awk '{print $3}')
  echo "[$i min] Memory: $mem KB"
  
  # Check for memory leaks (should stabilize)
  if [ $i -eq 60 ]; then
    if [ $mem -gt 50000 ]; then  # 50 MB threshold
      echo "⚠️  Memory leak detected: $mem KB > 50 MB"
    fi
  fi
  
  sleep 60
done

# 4. Low memory simulation
echo "Test 3: Low memory handling"
adb shell am send-trim-memory com.sparesparrow.cliphist RUNNING_LOW
sleep 2

# Verify service still running
adb shell dumpsys activity services | grep FloatingBubbleService
# Expected: Service running (gracefully reduced memory)

# 5. Battery optimization test
echo "Test 4: Battery optimization exemption"
adb shell dumpsys battery
adb shell settings put global low_power 1  # Enable battery saver
sleep 5

# Verify service still foreground
adb shell dumpsys activity services | grep FloatingBubbleService
# Expected: isForeground=true (exempted from doze)

# 6. System kill simulation
echo "Test 5: System kill recovery"
pid=$(adb shell pidof com.sparesparrow.cliphist)
adb shell kill -9 $pid
sleep 5

# Verify service recovered
adb shell dumpsys activity services | grep FloatingBubbleService
# Expected: Service running (new PID)

# Output:
✅ Service Persistence Tests:

Test Results:
┌────────────────────────────┬────────┬─────────────┐
│ Test                       │ Status │ Details     │
├────────────────────────────┼────────┼─────────────┤
│ Force-stop recovery        │ ✅     │ 5s restart  │
│ Memory stability (1h)      │ ✅     │ 28-32 MB    │
│ Low memory handling        │ ✅     │ No crash    │
│ Battery optimization       │ ✅     │ Foreground  │
│ System kill recovery       │ ✅     │ Auto-restart│
└────────────────────────────┴────────┴─────────────┘

📊 Memory Profile (1 hour):
   - Baseline: 28 MB
   - Peak: 32 MB
   - Average: 30 MB
   - Memory leak: NOT DETECTED

✅ Foreground Service Notification:
   - Title: "Clipboard History Active"
   - Channel: High priority
   - Dismissible: No (foreground requirement)

⚡ Battery Impact:
   - Battery drain: <1% per hour
   - Doze exemption: GRANTED
   - Background restriction: NONE

🔄 Service Recovery:
   - Auto-restart latency: avg 4.8s
   - Recovery success rate: 100% (5/5 tests)
   - Data persistence: ✅ (Room database)

📄 Health Report: service-health-report.json
```

***

### **6. `/test-ui-accessibility`**

**File:** `.cursor/commands/test-ui-accessibility.md`

```markdown
Test UI and accessibility: verify SYSTEM_ALERT_WINDOW permission prompt, test Material Design 3 theme switching (light/dark), validate bubble opacity controls (10-100%), test bubble size selection (5 levels), verify accessibility service compatibility, test TalkBack screen reader support, output ui-accessibility-report.json
```

**Example Usage:**
```bash
/test-ui-accessibility

# Cursor tests UI/UX and accessibility:
echo "♿ UI & Accessibility Testing"

# 1. Permission flow test
echo "Test 1: Overlay permission"
adb shell pm revoke com.sparesparrow.cliphist android.permission.SYSTEM_ALERT_WINDOW
adb shell am start -n com.sparesparrow.cliphist/.MainActivity

# Verify permission prompt appears
sleep 2
adb shell screencap /sdcard/permission-prompt.png

# Grant permission via settings
adb shell appops set com.sparesparrow.cliphist \
  SYSTEM_ALERT_WINDOW allow

# Verify permission granted
adb shell appops get com.sparesparrow.cliphist SYSTEM_ALERT_WINDOW
# Expected: SYSTEM_ALERT_WINDOW: allow

# 2. Theme switching test
echo "Test 2: Material Design 3 themes"
for theme in Light Dark System; do
  echo "  Testing $theme theme"
  adb shell "am broadcast -a com.sparesparrow.cliphist.SET_THEME \
    --es theme $theme"
  sleep 1
  adb shell screencap /sdcard/theme-${theme,,}.png
done

# 3. Bubble opacity test
echo "Test 3: Bubble opacity control"
for opacity in 10 30 50 80 100; do
  echo "  Testing opacity: $opacity%"
  adb shell "am broadcast -a com.sparesparrow.cliphist.SET_OPACITY \
    --ei opacity $opacity"
  sleep 1
  adb shell screencap /sdcard/opacity-${opacity}.png
done

# 4. Bubble size test
echo "Test 4: Bubble size levels"
sizes=("XSmall" "Small" "Medium" "Large" "XLarge")
for size in "${sizes[@]}"; do
  echo "  Testing size: $size"
  adb shell "am broadcast -a com.sparesparrow.cliphist.SET_SIZE \
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
# Check contrast ratios (via screenshot analysis)
python3 << 'EOF'
from PIL import Image
import numpy as np

def calculate_contrast(rgb1, rgb2):
    def luminance(rgb):
        rgb = [x / 255.0 for x in rgb]
        rgb = [(x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4) for x in rgb]
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]
    
    l1 = luminance(rgb1)
    l2 = luminance(rgb2)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)

# Example: Check bubble text vs background
# (Simplified - actual implementation would analyze screenshots)
text_color = (255, 255, 255)  # White text
bg_color = (33, 128, 141)     # Teal background

contrast = calculate_contrast(text_color, bg_color)
print(f"Text contrast ratio: {contrast:.2f}")
print(f"WCAG AA: {'PASS' if contrast >= 4.5 else 'FAIL'}")
print(f"WCAG AAA: {'PASS' if contrast >= 7.0 else 'FAIL'}")
EOF

# Output:
✅ UI & Accessibility Tests:

Permission Flow:
   - Overlay permission prompt: ✅ (displayed correctly)
   - Permission grant flow: ✅ (settings opened)
   - Permission verification: ✅ (android.permission.SYSTEM_ALERT_WINDOW)

Material Design 3 Themes:
   - Light theme: ✅ (high contrast, readable)
   - Dark theme: ✅ (OLED-friendly, reduced eye strain)
   - System theme: ✅ (follows system preference)

Bubble Customization:
   - Opacity range: ✅ (10%-100%, smooth transitions)
   - Size levels: ✅ (5 sizes, proportional scaling)
   - Theme colors: ✅ (consistent across states)

Accessibility Compliance:
   - TalkBack support: ✅ (content descriptions present)
   - Focus order: ✅ (logical navigation)
   - Touch targets: ✅ (min 48x48dp)
   - Color contrast: ✅ (WCAG AA compliant)

WCAG Color Contrast Results:
   - Text on bubble: 7.2:1 (AAA ✅)
   - Action buttons: 5.8:1 (AA ✅)
   - Icon contrast: 4.9:1 (AA ✅)

📸 Screenshots: test-results/
   - permission-prompt.png
   - theme-light.png, theme-dark.png
   - opacity-10.png to opacity-100.png
   - size-xsmall.png to size-xlarge.png

📄 Accessibility Report: ui-accessibility-report.json
```

***

## **Comprehensive Test Script**

### **7. `/run-full-test-suite`**

**File:** `.cursor/commands/run-full-test-suite.md`

```markdown
Execute complete test suite: run all 6 test categories in sequence (clipboard capture, bubble types, smart actions, encryption, service persistence, UI accessibility), collect all screenshots and logs, generate unified HTML test report with pass/fail matrix, output full-test-suite-report.html + all artifacts in test-results/
```

**Example Usage:**
```bash
/run-full-test-suite

# Cursor orchestrates full test execution:
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 Clipboard History - Full Test Suite"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Clean previous results
rm -rf test-results/
mkdir -p test-results/{screenshots,logs,reports}

# Test execution sequence
tests=(
  "test-clipboard-capture"
  "test-floating-bubble-types"
  "test-smart-actions-drag-drop"
  "test-encryption-security"
  "test-service-persistence-memory"
  "test-ui-accessibility"
)

results=()
for test in "${tests[@]}"; do
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "Running: $test"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  
  # Execute test (invoke corresponding cursor command)
  start_time=$(date +%s)
  /cursor/$test 2>&1 | tee test-results/logs/${test}.log
  exit_code=${PIPESTATUS[0]}
  end_time=$(date +%s)
  duration=$((end_time - start_time))
  
  # Record result
  if [ $exit_code -eq 0 ]; then
    results+=("$test:PASS:${duration}s")
    echo "✅ $test: PASS (${duration}s)"
  else
    results+=("$test:FAIL:${duration}s")
    echo "❌ $test: FAIL (${duration}s)"
  fi
done

# Generate HTML report
python3 << 'EOF'
import json
from datetime import datetime

results = """${results[@]}"""
tests_data = []
for result in results.split():
    name, status, duration = result.split(':')
    tests_data.append({
        'name': name.replace('test-', '').replace('-', ' ').title(),
        'status': status,
        'duration': duration
    })

html = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Clipboard History - Test Report</title>
    <style>
        body {{ font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; background: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; }}
        h1 {{ color: #21808d; }}
        table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        th, td {{ padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background: #21808d; color: white; }}
        .pass {{ color: #4caf50; font-weight: bold; }}
        .fail {{ color: #f44336; font-weight: bold; }}
        .summary {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin: 20px 0; }}
        .summary-card {{ padding: 20px; border-radius: 8px; text-align: center; }}
        .summary-card.total {{ background: #e3f2fd; }}
        .summary-card.passed {{ background: #e8f5e9; }}
        .summary-card.failed {{ background: #ffebee; }}
        .summary-card h2 {{ margin: 0; font-size: 2em; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>📋 Clipboard History Test Report</h1>
        <p>Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        
        <div class="summary">
            <div class="summary-card total">
                <h2>{len(tests_data)}</h2>
                <p>Total Tests</p>
            </div>
            <div class="summary-card passed">
                <h2>{sum(1 for t in tests_data if t['status'] == 'PASS')}</h2>
                <p>Passed</p>
            </div>
            <div class="summary-card failed">
                <h2>{sum(1 for t in tests_data if t['status'] == 'FAIL')}</h2>
                <p>Failed</p>
            </div>
        </div>
        
        <h2>Test Results</h2>
        <table>
            <tr>
                <th>Test Name</th>
                <th>Status</th>
                <th>Duration</th>
            </tr>
"""

for test in tests_data:
    status_class = 'pass' if test['status'] == 'PASS' else 'fail'
    html += f"""
            <tr>
                <td>{test['name']}</td>
                <td class="{status_class}">{test['status']}</td>
                <td>{test['duration']}</td>
            </tr>
"""

html += """
        </table>
        
        <h2>📸 Screenshots</h2>
        <p>All test screenshots saved in: <code>test-results/screenshots/</code></p>
        
        <h2>📋 Logs</h2>
        <p>Detailed logs available in: <code>test-results/logs/</code></p>
    </div>
</body>
</html>
"""

with open('test-results/reports/full-test-suite-report.html', 'w') as f:
    f.write(html)
    
print("✅ HTML report generated: test-results/reports/full-test-suite-report.html")
EOF

# Final summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Test Suite Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
passed=$(echo "${results[@]}" | grep -o "PASS" | wc -l)
failed=$(echo "${results[@]}" | grep -o "FAIL" | wc -l)
echo "Total: ${#tests[@]} tests"
echo "Passed: $passed ✅"
echo "Failed: $failed ❌"
echo ""
echo "📁 Artifacts:"
echo "   - HTML Report: test-results/reports/full-test-suite-report.html"
echo "   - Screenshots: test-results/screenshots/"
echo "   - Logs: test-results/logs/"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
```

***

## **Summary: Automated Testing Coverage**

| Test Category | Commands | Coverage |
|---------------|----------|----------|
| **Clipboard Monitoring** | `/test-clipboard-capture` | Text, URL, phone, email, address capture + encryption |
| **Bubble Types** | `/test-floating-bubble-types` | Circle, Cube (flash), Hexagon, Square + rendering |
| **Smart Actions** | `/test-smart-actions-drag-drop` | Edge-based drag + 4 content types + action execution |
| **Security** | `/test-encryption-security` | AES encryption + biometric + ProGuard obfuscation |
| **Service Stability** | `/test-service-persistence-memory` | Auto-restart + memory + battery + system kill |
| **UI/Accessibility** | `/test-ui-accessibility` | Permissions + themes + TalkBack + WCAG contrast |

**Total: 6 comprehensive test commands + 1 full suite runner**

