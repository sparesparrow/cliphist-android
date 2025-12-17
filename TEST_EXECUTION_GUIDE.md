# Quick Test Execution Guide

## 🚀 Run Full Test Suite

### Step 1: Connect Android Device
```bash
# Verify ADB connection
adb devices

# Should show your device/emulator
# List of devices attached
# emulator-5554	device
```

### Step 2: Install App
```bash
./gradlew installDebug
```

### Step 3: Run Complete Test Suite
```bash
# This will run all 6 test categories and generate reports
./run-full-test-suite.sh
```

### Step 4: View Results
```bash
# Open the comprehensive test report
xdg-open test-results/reports/full-test-suite-report.html

# Check screenshots
ls -la test-results/screenshots/

# View detailed logs
ls -la test-results/logs/
```

## 🧪 Individual Test Commands

If you want to run specific tests:

```bash
# Clipboard monitoring
/test-clipboard-capture

# Bubble types and rendering
/test-floating-bubble-types

# Smart actions (drag & drop)
/test-smart-actions-drag-drop

# Security and encryption
/test-encryption-security

# Service stability
/test-service-persistence-memory

# UI and accessibility
/test-ui-accessibility
```

## 📊 Expected Test Results

- **Total Tests:** 6 categories
- **Duration:** ~15-20 minutes
- **Screenshots:** 40+ UI validation images
- **Coverage:** Clipboard, bubbles, security, performance, accessibility

## 🎯 Success Criteria

✅ All 6 test categories pass
✅ Memory usage < 50MB
✅ Startup time < 5 seconds  
✅ Clipboard capture latency < 200ms
✅ 100% service recovery rate
✅ WCAG accessibility compliance

**The app is now ready for comprehensive validation testing!**

