#!/bin/bash

# CUE Android - Debug Session Script
# Complete debugging workflow with all feedback loops

set -e

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"
SCREENSHOTS_DIR="$LOGS_DIR/screenshots"

echo "🐛 Starting debug session..."

# Check if device is connected first
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "💡 Start an emulator or connect a device before debugging"
    exit 1
fi

# 1. Build and deploy
echo "🔨 Building and deploying app..."
if ! ./scripts/run.sh > /tmp/build_output.log 2>&1; then
    echo "❌ Build failed!"
    echo "📄 Build output:"
    cat /tmp/build_output.log
    echo ""
    echo "💡 Try: ./gradlew clean assembleDebug"
    exit 1
fi

# 2. Wait for app to initialize
echo "⏳ Waiting for app initialization..."
sleep 3

# 3. Capture initial screenshot
echo "📸 Capturing app screenshot..."
./scripts/screenshot.sh

# 4. Pull logs
echo "📥 Pulling app logs..."
./scripts/pull-logs.sh

# 5. Check for errors in logs
echo "🔍 Checking for errors..."
LATEST_LOG="$LOGS_DIR/cue-$(date +%Y-%m-%d).log"
if [ -f "$LATEST_LOG" ]; then
    ERROR_COUNT=$(grep -c "ERROR" "$LATEST_LOG" 2>/dev/null || echo "0")
    WARN_COUNT=$(grep -c "WARN" "$LATEST_LOG" 2>/dev/null || echo "0")
    
    echo "📊 Log Summary:"
    echo "   - Errors: $ERROR_COUNT"
    echo "   - Warnings: $WARN_COUNT"
    
    if [ "$ERROR_COUNT" -gt 0 ]; then
        echo "⚠️  Found errors in logs:"
        grep "ERROR" "$LATEST_LOG" | tail -3
    fi
fi

# 6. Show current app state
echo ""
echo "📱 Current App State:"
echo "   - Screenshot: $SCREENSHOTS_DIR/latest.png"
echo "   - Logs: $LATEST_LOG"
echo "   - Build output: /tmp/build_output.log"

echo ""
echo "🎯 Debug Commands:"
echo "   View screenshot: open $SCREENSHOTS_DIR/latest.png"
echo "   View logs: tail -f $LATEST_LOG"
echo "   Restart app: adb shell am start -n com.example.cue/.MainActivity"
echo "   New screenshot: ./scripts/screenshot.sh"

echo ""
echo "✅ Debug session ready!"
