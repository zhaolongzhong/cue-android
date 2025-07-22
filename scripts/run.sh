#!/bin/bash

# CUE Android - Run in Simulator Script
# This script builds and runs the Android app in the simulator

set -e  # Exit on any error

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

echo "🚀 Starting CUE Android build and run..."

# Check if Android SDK is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: Android SDK not found. Please ensure Android SDK is installed and in PATH."
    exit 1
fi

# Check if emulator is running
RUNNING_EMULATORS=$(adb devices | grep -E "emulator-[0-9]+" | wc -l)
if [ "$RUNNING_EMULATORS" -eq 0 ]; then
    echo "⚠️  No Android emulator detected. Attempting to start default emulator..."
    echo "💡 You can also start an emulator manually from Android Studio or run: emulator -avd <your_avd_name>"
    
    # Try to start the first available AVD
    if command -v emulator &> /dev/null; then
        FIRST_AVD=$(emulator -list-avds | head -n 1)
        if [ -n "$FIRST_AVD" ]; then
            echo "🔄 Starting emulator: $FIRST_AVD"
            emulator -avd "$FIRST_AVD" &
            echo "⏳ Waiting for emulator to boot..."
            adb wait-for-device
            sleep 10  # Give emulator time to fully boot
        else
            echo "❌ No AVDs found. Please create an Android Virtual Device first."
            exit 1
        fi
    else
        echo "❌ Emulator command not found. Please start an emulator manually."
        exit 1
    fi
else
    echo "✅ Android emulator is running"
fi

# Clean logs from previous run
echo "🧹 Cleaning previous logs..."
rm -f "$LOGS_DIR"/*.log

# Build and install the app
echo "🔨 Building and installing the app..."
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
echo "📱 Launching CUE app..."
adb shell am start -n com.example.cue/.MainActivity

echo "✅ App launched successfully!"
echo ""
echo "📋 Useful commands:"
echo "   Pull logs from device: ./scripts/pull-logs.sh"
echo "   View logs: tail -f $LOGS_DIR/cue-$(date +%Y-%m-%d).log"
echo "   App logcat: adb logcat | grep 'CueApplication\\|MainActivity\\|AppViewModel'"
echo "   Clear app data: adb shell pm clear com.example.cue"
echo "   Restart app: adb shell am start -n com.example.cue/.MainActivity"
echo ""
echo "🎉 Development workflow ready!"
echo "💡 Note: Logs are stored on device. Use './scripts/pull-logs.sh' to retrieve them."
