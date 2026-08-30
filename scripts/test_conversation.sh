#!/usr/bin/env bash
# scripts/test_conversation.sh
#
# Build APK, install on device, launch SMOLCASE, and tail conversation log.
# When you're done testing, Ctrl+C to stop.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."
ADB="${ADB:-$HOME/android-sdk/platform-tools/adb}"
GRADLE="${GRADLE:-$HOME/toolchains/gradle-8.7/bin/gradle}"
DEVICE="${DEVICE:-192.168.0.236:43007}"
JAVA_HOME="${JAVA_HOME:-$HOME/toolchains/jdk-17/Contents/Home}"
ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"

echo "=== Building APK ==="
export JAVA_HOME ANDROID_HOME
cd "$PROJECT_DIR/android"
$GRADLE assembleDebug --console=plain

echo ""
echo "=== Installing on device ($DEVICE) ==="
$ADB -s "$DEVICE" shell am force-stop com.smolcase.companion 2>/dev/null || true
$ADB -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "=== Starting app ==="
$ADB -s "$DEVICE" shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1 1>/dev/null
sleep 2

echo ""
echo "=== Tailing conversation log (Ctrl+C to stop) ==="
echo "     logcat:  adb -s $DEVICE logcat SmolcaseMain:V SmolcaseEars:V SmolcaseVoice:V SmolcaseLLM:V *:S"
echo "     pulls:   adb -s $DEVICE shell run-as com.smolcase.companion cat /data/data/com.smolcase.companion/files/logs/conversations.jsonl"
echo ""
$ADB -s "$DEVICE" shell run-as com.smolcase.companion tail -n +1 -f /data/data/com.smolcase.companion/files/logs/conversations.jsonl 2>&1 || true
echo ""
echo "=== Done ==="