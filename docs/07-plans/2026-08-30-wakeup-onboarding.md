# Plan: First-Start Wakeup / Onboarding Routine

**Spec:** [[docs/06-specs/2026-08-30-wakeup-onboarding-design]]
**Started:** 2026-08-30
**Blocked by:** [[docs/07-plans/2026-08-30-reset-to-scratch]] (wakeup_complete key defined in soul schema)

## Tasks

### Task 1 — MemoryStore: wakeup keys + methods
- [ ] Add `KEY_CREATURE_NAME`, `KEY_OWNER_NAME`, `KEY_WAKEUP_COMPLETE` constants
- [ ] Add `isWakeupComplete()` — reads `KEY_WAKEUP_COMPLETE`, default false
- [ ] Add `completeWakeup(name, owner)` — atomically write all three keys
- [ ] Add `creatureName()` — returns name or "SMOLCASE"
- [ ] Add `ownerName()` — returns owner name or "friend"
- [ ] Tests: round-trip, wakeup gate logic

### Task 2 — WakeupHandler.kt (NEW)
- [ ] Implement `WakeupHandler` with Stage enum (NAME, OWNER, HONESTY, COMPLETE)
- [ ] `parseName(text)` — clean the first significant word as name
- [ ] `parseOwner(text)` — extract a human-readable name from utterance
- [ ] `parseHonesty(text)` — extract 1-10 from number words or key phrases ("brutal"=10, "gentle"=1)
- [ ] 15s timeout per prompt with retry logic (2 retries → default)
- [ ] Pure JVM tests for all parsing

### Task 3 — CreatureBrain: BIRTH branch update
- [ ] Modify `greet()` — when BIRTH and `!memory.isWakeupComplete()`, start WakeupHandler instead of standard greeting
- [ ] Wire `onGreetingLine` to WakeupHandler's `onStageAdvance`

### Task 4 — ConversationEngine: wakeup routing
- [ ] In `handle()` — if `!memory.isWakeupComplete()`, route to WakeupHandler instead of IntentRouter + LLM backends
- [ ] Wakeup mode is not a backend setting — it's a transient state that automatically ends after `completeWakeup()`

### Task 5 — TarsFaceView: boot animation
- [ ] Add `animateBoot(onComplete: () -> Unit)` — matrix column sweep-in over ~3s, then slow blink, then ALERT state, then pulse telemetry pips
- [ ] Non-blocking — uses existing `tick()` frame loop with a `bootPhase` state machine

### Task 6 — Integration
- [ ] Wire everything: wakeup detected → animation plays → dialogue flow runs → completeWakeup() → normal mode
- [ ] Test full flow on-device

### Task 7 — Tests
- [ ] Unit: all parser edge cases (silence, numbers, garbage, partial matches)
- [ ] Unit: stage transitions and timeouts
- [ ] Regression: `:app:testDebugUnitTest` green
- [ ] Deployment: `assembleDebug`, wireless adb, full onboarding walkthrough