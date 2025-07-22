#!/bin/bash

# CUE Android - Screenshot Capture Script
# Captures current app screenshot for debugging UI issues

set -e

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCREENSHOTS_DIR="$PROJECT_ROOT/logs/screenshots"

echo "📸 Capturing app screenshot..."

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

# Create screenshots directory
mkdir -p "$SCREENSHOTS_DIR"

# Capture screenshot
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
SCREENSHOT_FILE="screenshot_${TIMESTAMP}.png"

echo "🔄 Taking screenshot..."
adb shell screencap -p /sdcard/temp_screenshot.png
adb pull /sdcard/temp_screenshot.png "$SCREENSHOTS_DIR/$SCREENSHOT_FILE"
adb shell rm /sdcard/temp_screenshot.png

echo "✅ Screenshot saved: $SCREENSHOTS_DIR/$SCREENSHOT_FILE"
echo "📱 Current app screen captured for debugging"

# Also create a "latest" symlink for easy access
cd "$SCREENSHOTS_DIR"
ln -sf "$SCREENSHOT_FILE" latest.png

echo "💡 View with: open $SCREENSHOTS_DIR/latest.png"
