# Plan: Factory Reset / Reset to Scratch

**Spec:** [[docs/06-specs/2026-08-30-reset-to-scratch-design]]
**Started:** 2026-08-30

## Tasks

### Task 1 — MemoryStore: soul clear
- [ ] Add `clearAll()` to MemoryStore — `prefs.edit().clear().apply()`
- [ ] Add companion tests: verify `totalSessions == 0` and `lastSessionEndAt == 0L` after clear

### Task 2 — TarsFaceView: resetState()
- [ ] Add `resetState()` to TarsFaceView — resets lastFaceAt, gaze, saccade, blink timers, expressionState to fresh NEUTRAL
- [ ] Verify mood, blink, and drift timers all restart cleanly

### Task 3 — CreatureBrain: reset()
- [ ] Add `reset()` — call `stop()`, re-create MemoryStore, clear sessionActive/lastFaceEventAt
- [ ] Verify fresh MemoryStore returns `isFirstEver = true` on next `beginSession()`

### Task 4 — Reset orchestration
- [ ] Create `resetToScratch(context, eyesView, brain)` helper in MainActivity
- [ ] Settings button: new "Creature" section → "Reset to Scratch" with confirmation dialog
- [ ] Debug cycle: add RESET entry to the mood cycle after THINKING

### Task 5 — Tests
- [ ] Unit: `MemoryStore.clearAll()` wipes all keys
- [ ] Unit: `TarsFaceView.resetState()` restores defaults
- [ ] Regression: `:app:testDebugUnitTest` green

### Task 6 — Build & deploy
- [ ] `assembleDebug` builds clean
- [ ] Deploy, execute reset, verify fresh BIRTH greeting on next face arrival