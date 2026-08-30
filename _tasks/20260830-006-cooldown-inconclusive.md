---
id: 20260830-006
title: "Echo loop cooldown sometimes lets utterances through within 1-2s"
type: bug
severity: low
reported: 2026-08-30
---

### 🐛 [BUG]: Echo loop cooldown sometimes lets utterances through within 1-2s
*Origin: System Logs*

#### 🚨 Current vs. Expected Behaviour
- **Current Behaviour:** At timestamps `23:21:42`, `23:21:46`, and `23:21:50`, three utterances were processed within ~4s of each other. The second at `46.923` may be a recognition retry, but the cooldown behaviour is not clearly blocking re-triggers.
- **Expected Behaviour:** The 2.5s cooldown should consistently block re-triggering after TTS playback. Successive utterances within the cooldown window should be ignored with a log line.

#### 🎬 Replication Steps
1. Speak to the robot
2. Wait for TTS reply to complete
3. Speak again immediately after TTS finishes
4. Check `logcat` for `"📝 ignored (cooldown)"` lines
5. Observe whether utterances within the 2.5s window are blocked

#### 🎯 Failure Boundary (BDD-style)
- [ ] **Scenario: Regression Verification**
  - **Given:** The conversation cooldown mechanism is active (2.5s window)
  - **When:** A new utterance arrives within 2.5s of the previous one
  - **Then:** The utterance is ignored and logged as `"📝 ignored (cooldown)"`
  - **Status:** Inconclusive — needs controlled silent test to verify
- [ ] **Scenario: Resolution Validation (Fix Verification)**
  - **Given:** The conversation cooldown is active
  - **When:** A new utterance arrives within the cooldown window
  - **Then:** It is consistently ignored with a log line; no utterances leak through

#### 🔍 Diagnostic & Contextual Data
- **Impacted Subsystems:** `VoiceEars` / `ConversationEngine` — cooldown timer mechanism
- **Error Logs / Stack Traces:**
  ```text
  - 23:21:42.943 — "small cases a small" (heard)
  - 23:21:46.923 — "that is the designation" (heard — may be onResults retry)
  - 23:21:50.336 — "is a small" (heard)
  ```
- **Environmental Factors:** On-device Pixel 8, only when TTS is active