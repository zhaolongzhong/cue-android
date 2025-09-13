#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

BUILD_TYPE="debug"
OUTPUT_DIR="$PROJECT_ROOT/build/outputs"

while [[ $# -gt 0 ]]; do
    case $1 in
        -r|--release)
            BUILD_TYPE="release"
            shift
            ;;
        -d|--debug)
            BUILD_TYPE="debug"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  -d, --debug      Build debug APK (default)"
            echo "  -r, --release    Build release APK/AAB (requires keystore)"
            echo "  -h, --help       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "🚀 Building CUE Android App (${BUILD_TYPE})"
echo ""

cd "$PROJECT_ROOT"

if [ "$BUILD_TYPE" = "release" ] && [ ! -f "keystore.properties" ]; then
    echo "⚠️  Warning: keystore.properties not found"
    echo ""
    echo "For release builds, you need to:"
    echo "1. Copy keystore.properties.sample to keystore.properties"
    echo "2. Update it with your keystore credentials"
    echo "3. Generate a keystore if you don't have one:"
    echo ""
    echo "   keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA \\"
    echo "           -keysize 2048 -validity 10000 -alias upload-keystore"
    echo ""
    echo "Falling back to unsigned release build..."
    echo ""
fi

echo "🧹 Cleaning previous builds..."
./gradlew clean

if [ "$BUILD_TYPE" = "release" ]; then
    BUILD_TASK="assembleRelease"
    BUNDLE_TASK="bundleRelease"
    BUILD_TYPE_CAP="Release"
else
    BUILD_TASK="assembleDebug"
    BUNDLE_TASK=""
    BUILD_TYPE_CAP="Debug"
fi

if [ "$BUILD_TYPE" = "release" ]; then
    APK_PATH="app/build/outputs/apk/${BUILD_TYPE}/app-${BUILD_TYPE}-unsigned.apk"
    AAB_PATH="app/build/outputs/bundle/${BUILD_TYPE}/app-${BUILD_TYPE}.aab"
else
    APK_PATH="app/build/outputs/apk/${BUILD_TYPE}/app-${BUILD_TYPE}.apk"
    AAB_PATH=""
fi
APP_ID="ai.plusonelabs.app.dev"

if [ "$BUILD_TYPE" = "debug" ]; then
    APP_ID="${APP_ID}.debug"
fi

echo "📦 Building ${BUILD_TYPE} APK..."
./gradlew $BUILD_TASK

if [ "$BUILD_TYPE" = "release" ]; then
    echo "📦 Building release AAB..."
    ./gradlew $BUNDLE_TASK
fi

mkdir -p "$OUTPUT_DIR"

if [ -f "$APK_PATH" ]; then
    OUTPUT_APK="$OUTPUT_DIR/app-${BUILD_TYPE}.apk"
    cp "$APK_PATH" "$OUTPUT_APK"
    echo "✅ ${BUILD_TYPE_CAP} APK: $OUTPUT_APK"
fi

if [ "$BUILD_TYPE" = "release" ] && [ -f "$AAB_PATH" ]; then
    cp "$AAB_PATH" "$OUTPUT_DIR/"
    echo "✅ Release Bundle: $OUTPUT_DIR/app-release.aab"
fi

echo ""
echo "📊 Build Summary:"
echo "  • Build Type: ${BUILD_TYPE_CAP}"
echo "  • Application ID: $APP_ID"
echo "  • Output Directory: $OUTPUT_DIR"

if [ "$BUILD_TYPE" = "release" ] && [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    echo ""
    echo "⚠️  Note: APK is unsigned. Configure keystore.properties for signed builds."
fi

if [ "$BUILD_TYPE" = "debug" ]; then
    echo ""
    echo "📱 To install on device:"
    echo "  adb install $OUTPUT_DIR/app-debug.apk"
fi

echo ""
echo "🎉 Build completed successfully!"