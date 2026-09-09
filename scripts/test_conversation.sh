#!/usr/bin/env bash
# scripts/test_conversation.sh
#
# Build, stamp, sideload, launch SMOLCASE, and tail the conversation log.
# When you're done testing, Ctrl+C to stop.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."
ADB="${ADB:-$HOME/android-sdk/platform-tools/adb}"
DEVICE="${DEVICE:-192.168.0.236:43007}"

export ADB DEVICE
"$SCRIPT_DIR/build-and-sideload.sh"
sleep 2

echo ""
echo "=== Tailing conversation log (Ctrl+C to stop) ==="
echo "     logcat:  adb -s $DEVICE logcat SmolcaseMain:V SmolcaseEars:V SmolcaseVoice:V SmolcaseLLM:V *:S"
echo "     pulls:   adb -s $DEVICE shell run-as com.smolcase.companion cat /data/data/com.smolcase.companion/files/logs/conversations.jsonl"
echo ""
$ADB -s "$DEVICE" shell run-as com.smolcase.companion tail -n +1 -f /data/data/com.smolcase.companion/files/logs/conversations.jsonl 2>&1 || true
echo ""
echo "=== Done ==="