#!/bin/bash

# CUE Android - Pull Logs Script
# This script pulls log files from the Android device to the local logs folder

set -e

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

echo "📥 Pulling logs from Android device..."

# Create local logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

# Get the app's internal storage path
APP_PACKAGE="com.example.cue"
DEVICE_LOGS_PATH="/data/data/$APP_PACKAGE/files/logs"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

# Pull all log files from device
echo "🔄 Pulling log files from device storage..."
adb shell "run-as $APP_PACKAGE find files/logs -name '*.log' 2>/dev/null" | while read -r file; do
    if [ -n "$file" ]; then
        filename=$(basename "$file")
        echo "   📄 Pulling $filename"
        adb shell "run-as $APP_PACKAGE cat $file" > "$LOGS_DIR/$filename"
    fi
done

# List pulled logs
echo ""
echo "✅ Logs pulled successfully:"
ls -la "$LOGS_DIR/"

echo ""
echo "📋 View logs with:"
echo "   tail -f $LOGS_DIR/cue-$(date +%Y-%m-%d).log"
echo "   cat $LOGS_DIR/cue-$(date +%Y-%m-%d).log"
