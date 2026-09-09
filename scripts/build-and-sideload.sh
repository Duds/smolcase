#!/usr/bin/env bash
# Build, stamp, sideload, and launch the SMOLCASE Android app.
# The build number advances only after the APK has been installed successfully.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."
ANDROID_DIR="$PROJECT_DIR/android"
BUILD_NUMBER_FILE="$ANDROID_DIR/build-number"
LOCK_DIR="$PROJECT_DIR/.build-number.lock"
ADB="${ADB:-$HOME/android-sdk/platform-tools/adb}"
GRADLE="${GRADLE:-$HOME/toolchains/gradle-8.7/bin/gradle}"
DEVICE="${DEVICE:-192.168.0.236:43007}"
JAVA_HOME="${JAVA_HOME:-$HOME/toolchains/jdk-17/Contents/Home}"
ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
APK="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"

cleanup() {
    rmdir "$LOCK_DIR" 2>/dev/null || true
}

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    echo "Another build-and-sideload is already running." >&2
    exit 1
fi
trap cleanup EXIT

if [[ ! -f "$BUILD_NUMBER_FILE" ]]; then
    echo "Missing build number file: $BUILD_NUMBER_FILE" >&2
    exit 1
fi

current_number="$(tr -d '[:space:]' < "$BUILD_NUMBER_FILE")"
if [[ ! "$current_number" =~ ^[0-9]+$ ]] || (( current_number < 1 )); then
    echo "Invalid build number in $BUILD_NUMBER_FILE: $current_number" >&2
    exit 1
fi
next_number=$((current_number + 1))

export JAVA_HOME ANDROID_HOME

echo "=== Building APK ${next_number} ==="
(
    cd "$ANDROID_DIR"
    "$GRADLE" -PbuildNumber="$next_number" assembleDebug --console=plain
)

echo ""
echo "=== Installing build ${next_number} on ${DEVICE} ==="
"$ADB" -s "$DEVICE" shell am force-stop com.smolcase.companion 2>/dev/null || true
"$ADB" -s "$DEVICE" install -r "$APK"

printf '%s\n' "$next_number" > "$BUILD_NUMBER_FILE"
echo "Build ${next_number} installed and recorded in ${BUILD_NUMBER_FILE}."

echo ""
echo "=== Starting app ==="
"$ADB" -s "$DEVICE" shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1 >/dev/null
