#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Default arguments
RUN_UNIT_TESTS=true
RUN_UI_TESTS=false

# Print usage
function print_usage() {
    echo "Usage: $0 [-u] [-i] [-a] [-h]"
    echo "  -u: Run unit tests only (default)"
    echo "  -i: Run instrumented tests only"
    echo "  -a: Run all tests (both unit and instrumented)"
    echo "  -h: Show this help message"
}

# Check for Android SDK and adb
function check_android_sdk() {
    # Try to locate adb from ANDROID_HOME or ANDROID_SDK_ROOT
    if [ -n "${ANDROID_HOME:-}" ]; then
        ADB="$ANDROID_HOME/platform-tools/adb"
    elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
    else
        echo "Error: ANDROID_HOME or ANDROID_SDK_ROOT environment variable is not set"
        echo "Please set one of these to your Android SDK location by running:"
        echo ""
        echo "  echo 'export ANDROID_HOME=\$HOME/Library/Android/sdk' >> ~/.bash_profile"
        echo "  echo 'export PATH=\$PATH:\$ANDROID_HOME/platform-tools' >> ~/.bash_profile"
        echo "  source ~/.bash_profile"
        echo ""
        echo "Note: If you're using zsh, replace .bash_profile with .zshrc"
        exit 1
    fi

    if [ ! -f "$ADB" ]; then
        echo "Error: adb not found at $ADB"
        echo "Please make sure Android SDK platform-tools are installed:"
        echo ""
        echo "1. Open Android Studio"
        echo "2. Go to Tools -> SDK Manager"
        echo "3. Select SDK Tools tab"
        echo "4. Check 'Android SDK Platform-Tools'"
        echo "5. Click Apply and wait for installation"
        exit 1
    fi
}

# Parse arguments
while getopts ":uiah" opt; do
    case $opt in
        i)
            RUN_UI_TESTS=true
            RUN_UNIT_TESTS=false
            ;;
        u)
            RUN_UNIT_TESTS=true
            RUN_UI_TESTS=false
            ;;
        a)
            RUN_UI_TESTS=true
            RUN_UNIT_TESTS=true
            ;;
        h)
            print_usage
            exit 0
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            print_usage
            exit 1
            ;;
    esac
done

# Run unit tests
if [ "$RUN_UNIT_TESTS" = true ]; then
    echo "Running unit tests..."
    ./gradlew testDebugUnitTest || {
        echo "Unit tests failed"
        exit 1
    }
    echo "Unit tests completed successfully"
fi

# Run instrumented tests
if [ "$RUN_UI_TESTS" = true ]; then
    echo "Running instrumented tests..."

    # Check for Android SDK before proceeding
    check_android_sdk

    # Check if device/emulator is connected
    if ! "$ADB" devices | grep -q "device$"; then
        echo "No Android device or emulator found. Please connect a device or start an emulator."
        exit 1
    fi

    ./gradlew connectedAndroidTest || {
        echo "Instrumented tests failed"
        exit 1
    }
    echo "Instrumented tests completed successfully"
fi

# Show test results location
if [ "$RUN_UNIT_TESTS" = true ]; then
    echo "Unit test results available at: app/build/reports/tests/testDebugUnitTest/index.html"
fi

if [ "$RUN_UI_TESTS" = true ]; then
    echo "Instrumented test results available at: app/build/reports/androidTests/connected/index.html"
fi