---
id: 20260829-002
title: "Fix conversation quality regressions"
type: wayfinder:task
status: open
blocked-by: []
---

## Question

Resolve the remaining conversation quality issues from the Settings Expansion rollout. The core features work but there are regressions in the conversation loop that degrade the user experience.

**Specific issues to fix:**
1. **Echo loop**: The creature may be re-processing its own TTS output — verify the cooldown mechanism and check logcat for ignored audio segments.
2. **Prompt repetition**: The conversation prompt no longer repeats "directives" in replies — verify the fix is working and add a regression test.
3. **Gemma latency**: 7-8 seconds per reply is too slow for conversational use — investigate whether this is a model issue, a prompt context overflow, or a LiteRT-LM runtime issue.
4. **Cloud TTS provider label**: Settings field for the cloud TTS provider label needs live-device testing.
5. **Field + button contrast**: Verify WCAG AA contrast on actual Pixel 8 display.
6. `.gitignore` `models/*.xnnpack_cache` files to prevent build artifacts from being tracked.

**Resolution:** All six items verified and fixed, or documented as known limitations with workarounds in AGENTS.md.