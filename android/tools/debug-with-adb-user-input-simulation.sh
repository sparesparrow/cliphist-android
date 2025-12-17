#!/bin/bash

# ============================================
# Android Debug Script with ADB User Input Simulation
# ============================================
# This script helps debug the Clipboard History app by simulating
# various user interactions via ADB commands.

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
PACKAGE_NAME="com.clipboardhistory.debug"
MAIN_ACTIVITY="com.clipboardhistory.presentation.MainActivity"
SERVICE_CLASS="com.clipboardhistory.presentation.services.FloatingBubbleService"
SCREENSHOT_DIR="./debug-screenshots"
LOG_DIR="./debug-logs"

# Functions
print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Check ADB connection
check_adb() {
    if ! command -v adb &> /dev/null; then
        print_error "ADB not found. Please install Android SDK platform-tools."
        exit 1
    fi
    
    if ! adb get-state 1>/dev/null 2>&1; then
        print_error "No ADB device/emulator detected. Connect one and retry."
        exit 1
    fi
    
    print_success "ADB device connected: $(adb devices | grep -v List | grep device | awk '{print $1}')"
}

# Check if app is installed
check_app_installed() {
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        print_error "App $PACKAGE_NAME is not installed."
        print_info "Install it with: ./gradlew installDebug"
        exit 1
    fi
    print_success "App $PACKAGE_NAME is installed"
}

# Get screen dimensions
get_screen_size() {
    adb shell wm size | awk '{print $3}' | tr -d '\r'
}

# Take screenshot
take_screenshot() {
    local name=$1
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local filename="${SCREENSHOT_DIR}/${name}_${timestamp}.png"
    
    mkdir -p "$SCREENSHOT_DIR"
    adb shell screencap -p /sdcard/screenshot.png
    adb pull /sdcard/screenshot.png "$filename" > /dev/null 2>&1
    adb shell rm /sdcard/screenshot.png > /dev/null 2>&1
    
    print_info "Screenshot saved: $filename"
    echo "$filename"
}

# Simulate tap at coordinates
tap() {
    local x=$1
    local y=$2
    print_info "Tapping at ($x, $y)"
    adb shell input tap "$x" "$y"
    sleep 0.5
}

# Simulate swipe gesture
swipe() {
    local x1=$1
    local y1=$2
    local x2=$3
    local y2=$4
    local duration=${5:-300}
    print_info "Swiping from ($x1, $y1) to ($x2, $y2)"
    adb shell input swipe "$x1" "$y1" "$x2" "$y2" "$duration"
    sleep 0.5
}

# Simulate text input
input_text() {
    local text=$1
    print_info "Inputting text: $text"
    adb shell input text "$(echo "$text" | sed "s/ /%s/g")"
    sleep 0.5
}

# Simulate key event
key_event() {
    local key=$1
    print_info "Pressing key: $key"
    adb shell input keyevent "$key"
    sleep 0.3
}

# Launch app
launch_app() {
    print_header "🚀 Launching App"
    adb shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"
    sleep 3
    print_success "App launched"
    take_screenshot "01_app_launched"
}

# Test clipboard capture
test_clipboard_capture() {
    print_header "📋 Testing Clipboard Capture"
    
    local test_texts=(
        "Hello World"
        "https://github.com/sparesparrow/cliphist-android"
        "+420777123456"
        "test@example.com"
    )
    
    for i in "${!test_texts[@]}"; do
        local text="${test_texts[$i]}"
        print_info "Test $((i+1)): Copying '$text'"
        
        # Copy to clipboard via broadcast
        adb shell am broadcast \
            -a android.intent.action.SEND \
            --es android.intent.extra.TEXT "$text"
        sleep 2
        
        take_screenshot "02_clipboard_${i}_${text//[^a-zA-Z0-9]/_}"
    done
    
    print_success "Clipboard capture tests completed"
}

# Test bubble interactions
test_bubble_interactions() {
    print_header "🎈 Testing Bubble Interactions"
    
    # Get screen center
    local screen_size=$(get_screen_size)
    local width=$(echo "$screen_size" | cut -d'x' -f1)
    local height=$(echo "$screen_size" | cut -d'x' -f2)
    local center_x=$((width / 2))
    local center_y=$((height / 2))
    
    print_info "Screen size: ${width}x${height}"
    print_info "Center: ($center_x, $center_y)"
    
    # Tap bubble (assuming it's near center)
    print_info "Tapping bubble"
    tap "$center_x" "$center_y"
    sleep 1
    take_screenshot "03_bubble_tapped"
    
    # Swipe to move bubble
    print_info "Swiping bubble"
    swipe "$center_x" "$center_y" "$((center_x + 100))" "$((center_y + 100))" 500
    sleep 1
    take_screenshot "04_bubble_moved"
    
    print_success "Bubble interaction tests completed"
}

# Test settings
test_settings() {
    print_header "⚙️  Testing Settings"
    
    # Open settings (assuming there's a settings button or menu)
    # This is a placeholder - adjust coordinates based on your UI
    key_event "KEYCODE_MENU"  # Open menu
    sleep 1
    take_screenshot "05_settings_menu"
    
    # Try to find and tap settings
    # Adjust coordinates based on your actual UI layout
    local screen_size=$(get_screen_size)
    local width=$(echo "$screen_size" | cut -d'x' -f1)
    local height=$(echo "$screen_size" | cut -d'x' -f2)
    
    # Tap somewhere that might be settings (adjust as needed)
    tap "$((width - 100))" "100"
    sleep 1
    take_screenshot "06_settings_opened"
    
    # Go back
    key_event "KEYCODE_BACK"
    sleep 1
    
    print_success "Settings tests completed"
}

# Test service status
test_service_status() {
    print_header "🔄 Testing Service Status"
    
    print_info "Checking FloatingBubbleService status..."
    adb shell dumpsys activity services | grep -i "FloatingBubbleService" || print_warning "Service not found in dumpsys"
    
    print_info "Checking memory usage..."
    adb shell dumpsys meminfo "$PACKAGE_NAME" | grep -E "TOTAL PSS|TOTAL PRIVATE DIRTY" || true
    
    print_info "Checking running processes..."
    adb shell ps | grep "$PACKAGE_NAME" || print_warning "Process not found"
    
    print_success "Service status check completed"
}

# Test different content types
test_content_types() {
    print_header "📝 Testing Different Content Types"
    
    local content_types=(
        "Plain text: This is a test message"
        "URL: https://www.example.com/path?query=value"
        "Email: user@example.com"
        "Phone: +1-555-123-4567"
        "Code: function test() { return true; }"
        "JSON: {\"key\": \"value\", \"number\": 123}"
    )
    
    for content in "${content_types[@]}"; do
        local label=$(echo "$content" | cut -d':' -f1)
        local text=$(echo "$content" | cut -d':' -f2- | xargs)
        
        print_info "Testing: $label"
        adb shell am broadcast \
            -a android.intent.action.SEND \
            --es android.intent.extra.TEXT "$text"
        sleep 2
        
        take_screenshot "07_content_${label// /_}"
    done
    
    print_success "Content type tests completed"
}

# Collect logs
collect_logs() {
    print_header "📋 Collecting Logs"
    
    mkdir -p "$LOG_DIR"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    
    print_info "Collecting logcat..."
    adb logcat -d > "${LOG_DIR}/logcat_${timestamp}.log" 2>&1 || true
    
    print_info "Collecting dumpsys..."
    adb shell dumpsys activity > "${LOG_DIR}/dumpsys_activity_${timestamp}.log" 2>&1 || true
    adb shell dumpsys meminfo "$PACKAGE_NAME" > "${LOG_DIR}/dumpsys_meminfo_${timestamp}.log" 2>&1 || true
    
    print_success "Logs saved to $LOG_DIR"
}

# Interactive mode
interactive_mode() {
    print_header "🎮 Interactive Mode"
    print_info "Enter commands to simulate user input"
    print_info "Commands: tap <x> <y>, swipe <x1> <y1> <x2> <y2>, text <text>, key <keycode>, screenshot, exit"
    print_info "Example: tap 500 800"
    
    while true; do
        read -p "adb> " cmd
        
        case "$cmd" in
            exit|quit|q)
                print_info "Exiting interactive mode"
                break
                ;;
            screenshot|screen)
                take_screenshot "interactive"
                ;;
            tap\ *)
                local coords=($cmd)
                if [ ${#coords[@]} -eq 3 ]; then
                    tap "${coords[1]}" "${coords[2]}"
                else
                    print_error "Usage: tap <x> <y>"
                fi
                ;;
            swipe\ *)
                local coords=($cmd)
                if [ ${#coords[@]} -ge 5 ]; then
                    swipe "${coords[1]}" "${coords[2]}" "${coords[3]}" "${coords[4]}" "${coords[5]:-300}"
                else
                    print_error "Usage: swipe <x1> <y1> <x2> <y2> [duration]"
                fi
                ;;
            text\ *)
                local text="${cmd#text }"
                input_text "$text"
                ;;
            key\ *)
                local keycode="${cmd#key }"
                key_event "$keycode"
                ;;
            help|h)
                echo "Commands:"
                echo "  tap <x> <y>              - Tap at coordinates"
                echo "  swipe <x1> <y1> <x2> <y2> [duration] - Swipe gesture"
                echo "  text <text>              - Input text"
                echo "  key <keycode>            - Press key (e.g., KEYCODE_BACK)"
                echo "  screenshot              - Take screenshot"
                echo "  exit                    - Exit interactive mode"
                ;;
            "")
                continue
                ;;
            *)
                print_warning "Unknown command: $cmd (type 'help' for commands)"
                ;;
        esac
    done
}

# Show help
show_help() {
    cat << EOF
Android Debug Script with ADB User Input Simulation

Usage: $0 [COMMAND] [OPTIONS]

COMMANDS:
  all              Run all automated tests
  launch           Launch the app
  clipboard        Test clipboard capture
  bubble           Test bubble interactions
  settings         Test settings
  content          Test different content types
  service          Check service status
  logs             Collect logs
  interactive      Enter interactive mode
  help             Show this help message

OPTIONS:
  --screenshot-dir DIR    Directory for screenshots (default: ./debug-screenshots)
  --log-dir DIR           Directory for logs (default: ./debug-logs)
  --package NAME          Package name (default: com.clipboardhistory.debug)

EXAMPLES:
  $0 all                  # Run all tests
  $0 interactive          # Enter interactive mode
  $0 clipboard            # Test clipboard capture only
  $0 --package com.clipboardhistory.debug launch

KEYCODES (for interactive mode):
  KEYCODE_BACK            - Back button
  KEYCODE_HOME            - Home button
  KEYCODE_MENU            - Menu button
  KEYCODE_DPAD_UP         - D-pad up
  KEYCODE_DPAD_DOWN       - D-pad down
  KEYCODE_DPAD_LEFT       - D-pad left
  KEYCODE_DPAD_RIGHT      - D-pad right
  KEYCODE_ENTER           - Enter key
  KEYCODE_DEL              - Delete key

For more keycodes, see: https://developer.android.com/reference/android/view/KeyEvent
EOF
}

# Main execution
main() {
    local command="${1:-help}"
    
    # Parse options
    while [[ $# -gt 0 ]]; do
        case $1 in
            --screenshot-dir)
                SCREENSHOT_DIR="$2"
                shift 2
                ;;
            --log-dir)
                LOG_DIR="$2"
                shift 2
                ;;
            --package)
                PACKAGE_NAME="$2"
                shift 2
                ;;
            *)
                command="$1"
                shift
                ;;
        esac
    done
    
    # Check prerequisites
    check_adb
    check_app_installed
    
    # Execute command
    case "$command" in
        all)
            launch_app
            test_clipboard_capture
            test_bubble_interactions
            test_content_types
            test_service_status
            collect_logs
            print_header "🎉 All Tests Completed"
            print_info "Screenshots: $SCREENSHOT_DIR"
            print_info "Logs: $LOG_DIR"
            ;;
        launch)
            launch_app
            ;;
        clipboard)
            launch_app
            test_clipboard_capture
            ;;
        bubble)
            launch_app
            test_bubble_interactions
            ;;
        settings)
            launch_app
            test_settings
            ;;
        content)
            launch_app
            test_content_types
            ;;
        service)
            test_service_status
            ;;
        logs)
            collect_logs
            ;;
        interactive|i)
            interactive_mode
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
