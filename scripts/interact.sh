#!/bin/bash

# CUE Android - Interaction Script
# Allows tapping on screen coordinates and UI interaction

set -e

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

# Function to display usage
usage() {
    echo "Usage: $0 <command> [arguments]"
    echo ""
    echo "Commands:"
    echo "  tap <x> <y>              - Tap at coordinates"
    echo "  swipe <x1> <y1> <x2> <y2> [duration] - Swipe from point to point"
    echo "  text <text>              - Input text (auto-finds and focuses text field)"
    echo "  send                     - Send message (tap send button)"
    echo "  message <text>           - Complete message flow (input + send)"
    echo "  back                     - Press back button"
    echo "  home                     - Press home button"
    echo "  screenshot               - Take screenshot and show coordinates"
    echo "  ui-dump                  - Dump UI hierarchy"
    echo "  find <text>              - Find UI element containing text"
    echo ""
    echo "Examples:"
    echo "  $0 tap 500 1000"
    echo "  $0 text 'Hello World'"
    echo "  $0 find 'Login'"
    exit 1
}

# Check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo "❌ No Android device connected"
        exit 1
    fi
}

# Get screen dimensions
get_screen_size() {
    SIZE=$(adb shell wm size | grep "Physical size:" | sed 's/Physical size: //')
    WIDTH=$(echo $SIZE | cut -d'x' -f1)
    HEIGHT=$(echo $SIZE | cut -d'x' -f2)
    echo "Screen size: ${WIDTH}x${HEIGHT}"
}

# Main command processing
case "$1" in
    tap)
        check_device
        if [ -z "$2" ] || [ -z "$3" ]; then
            echo "❌ Error: tap requires x and y coordinates"
            usage
        fi
        echo "👆 Tapping at ($2, $3)"
        adb shell input tap "$2" "$3"
        echo "✅ Tap executed"
        ;;
        
    swipe)
        check_device
        if [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ] || [ -z "$5" ]; then
            echo "❌ Error: swipe requires x1 y1 x2 y2"
            usage
        fi
        DURATION=${6:-300}
        echo "👆 Swiping from ($2, $3) to ($4, $5) over ${DURATION}ms"
        adb shell input swipe "$2" "$3" "$4" "$5" "$DURATION"
        echo "✅ Swipe executed"
        ;;
        
    text)
        check_device
        if [ -z "$2" ]; then
            echo "❌ Error: text requires input string"
            usage
        fi
        echo "⌨️  Typing: $2"
        # First find and focus the text field
        echo "🎯 Finding text input field..."
        adb shell uiautomator dump /sdcard/window_dump.xml > /dev/null 2>&1
        adb pull /sdcard/window_dump.xml /tmp/ui_dump.xml > /dev/null 2>&1
        
        # Look for EditText element
        EDITTEXT=$(grep -o 'class="android.widget.EditText"[^>]*bounds="\[[0-9,]*\]\[[0-9,]*\]"' /tmp/ui_dump.xml | head -1)
        
        if [ -n "$EDITTEXT" ]; then
            # Extract bounds and calculate center
            BOUNDS=$(echo "$EDITTEXT" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]"/\1 \2 \3 \4/')
            X1=$(echo $BOUNDS | cut -d' ' -f1)
            Y1=$(echo $BOUNDS | cut -d' ' -f2)
            X2=$(echo $BOUNDS | cut -d' ' -f3)
            Y2=$(echo $BOUNDS | cut -d' ' -f4)
            CENTER_X=$(( (X1 + X2) / 2 ))
            CENTER_Y=$(( (Y1 + Y2) / 2 ))
            
            echo "📍 Tapping text field at ($CENTER_X, $CENTER_Y)"
            adb shell input tap "$CENTER_X" "$CENTER_Y"
            sleep 0.5
        fi
        
        # Clear existing text and input new text
        adb shell input keyevent KEYCODE_CTRL_A > /dev/null 2>&1 || true
        sleep 0.2
        ESCAPED_TEXT=$(echo "$2" | sed 's/ /\\ /g')
        adb shell input text "$ESCAPED_TEXT"
        echo "✅ Text entered: $2"
        
        # Cleanup
        adb shell rm /sdcard/window_dump.xml > /dev/null 2>&1
        rm -f /tmp/ui_dump.xml
        ;;
        
    send)
        check_device
        echo "📤 Sending message..."
        # Find send button by content description
        adb shell uiautomator dump /sdcard/window_dump.xml > /dev/null 2>&1
        adb pull /sdcard/window_dump.xml /tmp/ui_dump.xml > /dev/null 2>&1
        
        # Look for send button (by content-desc="Send message" or icon)
        SEND_BTN=$(grep -o 'content-desc="Send message"[^>]*bounds="\[[0-9,]*\]\[[0-9,]*\]"' /tmp/ui_dump.xml | head -1)
        
        if [ -n "$SEND_BTN" ]; then
            # Extract bounds and calculate center
            BOUNDS=$(echo "$SEND_BTN" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]"/\1 \2 \3 \4/')
            X1=$(echo $BOUNDS | cut -d' ' -f1)
            Y1=$(echo $BOUNDS | cut -d' ' -f2)
            X2=$(echo $BOUNDS | cut -d' ' -f3)
            Y2=$(echo $BOUNDS | cut -d' ' -f4)
            CENTER_X=$(( (X1 + X2) / 2 ))
            CENTER_Y=$(( (Y1 + Y2) / 2 ))
            
            echo "📍 Tapping send button at ($CENTER_X, $CENTER_Y)"
            adb shell input tap "$CENTER_X" "$CENTER_Y"
            echo "✅ Message sent"
        else
            echo "❌ Send button not found"
        fi
        
        # Cleanup
        adb shell rm /sdcard/window_dump.xml > /dev/null 2>&1
        rm -f /tmp/ui_dump.xml
        ;;
        
    message)
        check_device
        if [ -z "$2" ]; then
            echo "❌ Error: message requires text"
            usage
        fi
        echo "💬 Sending complete message: $2"
        
        # Input text
        echo "⌨️  Entering text..."
        adb shell uiautomator dump /sdcard/window_dump.xml > /dev/null 2>&1
        adb pull /sdcard/window_dump.xml /tmp/ui_dump.xml > /dev/null 2>&1
        
        EDITTEXT=$(grep -o 'class="android.widget.EditText"[^>]*bounds="\[[0-9,]*\]\[[0-9,]*\]"' /tmp/ui_dump.xml | head -1)
        
        if [ -n "$EDITTEXT" ]; then
            BOUNDS=$(echo "$EDITTEXT" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]"/\1 \2 \3 \4/')
            X1=$(echo $BOUNDS | cut -d' ' -f1)
            Y1=$(echo $BOUNDS | cut -d' ' -f2)
            X2=$(echo $BOUNDS | cut -d' ' -f3)
            Y2=$(echo $BOUNDS | cut -d' ' -f4)
            CENTER_X=$(( (X1 + X2) / 2 ))
            CENTER_Y=$(( (Y1 + Y2) / 2 ))
            
            adb shell input tap "$CENTER_X" "$CENTER_Y"
            sleep 0.5
        fi
        
        ESCAPED_TEXT=$(echo "$2" | sed 's/ /\\ /g')
        adb shell input text "$ESCAPED_TEXT"
        
        # Send message
        echo "📤 Sending message..."
        sleep 0.5
        SEND_BTN=$(grep -o 'content-desc="Send message"[^>]*bounds="\[[0-9,]*\]\[[0-9,]*\]"' /tmp/ui_dump.xml | head -1)
        
        if [ -n "$SEND_BTN" ]; then
            BOUNDS=$(echo "$SEND_BTN" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]"/\1 \2 \3 \4/')
            X1=$(echo $BOUNDS | cut -d' ' -f1)
            Y1=$(echo $BOUNDS | cut -d' ' -f2)
            X2=$(echo $BOUNDS | cut -d' ' -f3)
            Y2=$(echo $BOUNDS | cut -d' ' -f4)
            CENTER_X=$(( (X1 + X2) / 2 ))
            CENTER_Y=$(( (Y1 + Y2) / 2 ))
            
            adb shell input tap "$CENTER_X" "$CENTER_Y"
            echo "✅ Message sent: $2"
        else
            echo "❌ Send button not found"
        fi
        
        adb shell rm /sdcard/window_dump.xml > /dev/null 2>&1
        rm -f /tmp/ui_dump.xml
        ;;
        
    back)
        check_device
        echo "🔙 Pressing back button"
        adb shell input keyevent KEYCODE_BACK
        echo "✅ Back pressed"
        ;;
        
    home)
        check_device
        echo "🏠 Pressing home button"
        adb shell input keyevent KEYCODE_HOME
        echo "✅ Home pressed"
        ;;
        
    screenshot)
        check_device
        ./scripts/screenshot.sh
        get_screen_size
        echo "💡 Use coordinates from screenshot to tap specific locations"
        ;;
        
    ui-dump)
        check_device
        echo "📋 Dumping UI hierarchy..."
        DUMP_FILE="$LOGS_DIR/ui_dump.xml"
        adb shell uiautomator dump /sdcard/window_dump.xml
        adb pull /sdcard/window_dump.xml "$DUMP_FILE"
        adb shell rm /sdcard/window_dump.xml
        echo "✅ UI hierarchy saved to: $DUMP_FILE"
        echo "💡 Use this to find clickable elements and their bounds"
        ;;
        
    find)
        check_device
        if [ -z "$2" ]; then
            echo "❌ Error: find requires search text"
            usage
        fi
        echo "🔍 Finding UI element with text: $2"
        
        # Dump UI and search
        adb shell uiautomator dump /sdcard/window_dump.xml > /dev/null 2>&1
        adb pull /sdcard/window_dump.xml /tmp/ui_dump.xml > /dev/null 2>&1
        
        # Search for element
        ELEMENT=$(grep -o "text=\"[^\"]*$2[^\"]*\"[^>]*bounds=\"\[[0-9,]*\]\[[0-9,]*\]\"" /tmp/ui_dump.xml | head -1)
        
        if [ -n "$ELEMENT" ]; then
            # Extract bounds
            BOUNDS=$(echo "$ELEMENT" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]"/\1 \2 \3 \4/')
            X1=$(echo $BOUNDS | cut -d' ' -f1)
            Y1=$(echo $BOUNDS | cut -d' ' -f2)
            X2=$(echo $BOUNDS | cut -d' ' -f3)
            Y2=$(echo $BOUNDS | cut -d' ' -f4)
            
            # Calculate center point
            CENTER_X=$(( (X1 + X2) / 2 ))
            CENTER_Y=$(( (Y1 + Y2) / 2 ))
            
            echo "✅ Found element!"
            echo "   Text: $(echo "$ELEMENT" | grep -o 'text="[^"]*"')"
            echo "   Bounds: [$X1,$Y1][$X2,$Y2]"
            echo "   Center: ($CENTER_X, $CENTER_Y)"
            echo ""
            echo "💡 To tap this element, run:"
            echo "   $0 tap $CENTER_X $CENTER_Y"
        else
            echo "❌ Element with text '$2' not found"
            echo "💡 Try: $0 ui-dump to see all available elements"
        fi
        
        # Cleanup
        adb shell rm /sdcard/window_dump.xml > /dev/null 2>&1
        rm -f /tmp/ui_dump.xml
        ;;
        
    *)
        echo "❌ Unknown command: $1"
        usage
        ;;
esac
