#!/bin/bash
set -e

echo "=== Building Circle Popper for iOS (macOS required) ==="
echo ""

if [[ "$(uname)" != "Darwin" ]]; then
    echo "❌ This script must be run on macOS."
    echo "   Use build_for_ios_github.sh to build via GitHub Actions instead."
    exit 1
fi

export JAVA_HOME="${JAVA_HOME:-/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"

echo "Building KMP iOS framework..."
./gradlew :app:linkDebugFrameworkIosArm64 --no-daemon

echo ""
echo "✅ iOS framework built: app/build/bin/iosArm64/debugFramework/shared.framework"
echo ""
echo "Next steps:"
echo "  1. Open iosApp/iosApp.xcodeproj in Xcode"
echo "  2. Select your team for code signing"
echo "  3. Product → Archive → Distribute App"
echo "  4. Export IPA and sideload with iloader"
