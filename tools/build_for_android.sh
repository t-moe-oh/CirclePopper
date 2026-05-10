#!/bin/bash
set -e

echo "=== Building Circle Popper for Android ==="
echo ""

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/opt/android-sdk

echo "Building release APK..."
./gradlew :androidApp:assembleRelease

APK_PATH="androidApp/build/outputs/apk/release/CirclePopper-release.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ APK ready: $APK_PATH"
    ls -lh "$APK_PATH"
else
    echo ""
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi
