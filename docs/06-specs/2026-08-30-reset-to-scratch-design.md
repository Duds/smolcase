# Spec: Factory Reset / Reset to Scratch

**Date:** 2026-08-30
**Status:** Draft
**Scope:** Android companion app — settings/developer path to wipe SMOLCASE back to a fresh soul while keeping a starter personality (TARS defaults).

---

## 1. Problem

There is currently no way to reset SMOLCASE's persistent state:
- Soul data (`smolcase_soul` SharedPreferences) accumulates forever — session count, total time together, reminders, first-seen dates, usual arrival time
- Personality dials (`smolcase_dials` SharedPreferences) must be manually reset via sliders
- Conversation log (`conversations.jsonl`) grows unboundedly — no archive/rotate/clear exists
- No factory reset means testing conversation bugs requires manual state manipulation or fresh installs

The creature needs a deliberate full-reset path for debugging, re-onboarding, and fresh-start scenarios.

## 2. Design

### 2.1 What gets wiped

| Data | Storage | Reset action |
|------|---------|-------------|
| Session count, total time together, last session end, first-seen date, usual arrival time, reminders | `smolcase_soul` SharedPreferences | `edit().clear().apply()` |
| Humor/honesty dials, voice name | `smolcase_dials` SharedPreferences | `edit().clear().apply()` → next read returns defaults (HUMOR=75, HONESTY=90) |
| Conversation log (JSONL) | `context.filesDir/logs/conversations.jsonl` | `File.delete()` |
| Log session counter | `smolcase_log` SharedPreferences (`log_session`) | `remove("log_session")` → resets to 0 |
| Wakeup complete flag | `smolcase_soul` SharedPreferences (`wakeup_complete`) | Cleared by soul wipe |
| Creature name, owner name | `smolcase_soul` SharedPreferences (`creature_name`, `owner_name`) | Cleared by soul wipe |
| In-memory eye mood | `EyeExpressionState` | Reset to `NEUTRAL` |
| In-memory face state | `TarsFaceView` | Reset `lastFaceAt`, gaze, saccade, blink timers |
| In-memory brain state | `CreatureBrain` | Session ended, fresh MemoryStore |

### 2.2 What does NOT get wiped

- LLM backend config (`LlmSettings`) — keep agent endpoints, provider, model
- Cloud TTS/Vision config — keep API keys and provider settings
- Sensor config — keep which sensors are active
- BLE bindings
- System settings (adb, permissions)

### 2.3 Invocation paths

**Path A — Settings button (primary):**
- Long-press face → Settings → new section "Creature" → "Reset to Scratch" button
- Confirmation dialog: "This will erase all memories, conversations, and personality settings. SMOLCASE will restart as if meeting you for the first time. Continue?"
- On confirm: run reset sequence, then restart the brain

**Path B — Hidden developer shortcut:**
- Triple-tap debug cycle adds "R" (Reset) to the mood cycle after THINKING
- `eyesView.cycleDebugMode()` → NEUTRAL → HAPPY → SKEPTICAL → CURIOUS → HEART → THINKING → **RESET** → back to NEUTRAL
- On RESET: same confirmation dialog, same sequence

### 2.4 Reset sequence (atomic)

```kotlin
fun resetToScratch(context: Context, eyesView: TarsFaceView, brain: CreatureBrain) {
    // 1. End current session gracefully
    brain.stop()

    // 2. Wipe soul file
    context.getSharedPreferences("smolcase_soul", Context.MODE_PRIVATE)
        .edit().clear().apply()

    // 3. Wipe dials (next PersonalityDials read returns defaults)
    context.getSharedPreferences("smolcase_dials", Context.MODE_PRIVATE)
        .edit().clear().apply()

    // 4. Reset log session counter
    context.getSharedPreferences("smolcase_log", Context.MODE_PRIVATE)
        .edit().remove("log_session").apply()

    // 5. Delete conversation history
    File(context.filesDir, "logs/conversations.jsonl").delete()

    // 6. Reset in-memory face state
    eyesView.resetState()

    // 7. Start fresh brain session
    brain.reset()
    brain.start()

    // 8. Start fresh conversation log at session 1
    val freshLog = ConversationLog(context)
    freshLog.startSession()
}
```

### 2.5 New methods required

**TarsFaceView:**
```kotlin
fun resetState() {
    lastFaceAt = SystemClock.uptimeMillis()
    faceTarget = null
    gaze.set(0f, 0f)
    saccade.set(0f, 0f)
    expressionState = EyeExpressionState()  // fresh — mood=NEUTRAL, humor=75, honesty=90
    pendingBlinks = 0
    blinkStartedAt = 0L
    nextBlinkAt = now + Random.nextLong(2_000, 5_000)
    nextSaccadeAt = now + Random.nextLong(1_500, 4_000)
}
```

**CreatureBrain:**
```kotlin
fun reset() {
    stop()
    this.memory = MemoryStore(context)  // fresh, empty soul
    sessionActive = false
    lastFaceEventAt = 0L
}
```

## 3. Risks

| Risk | Mitigation |
|------|------------|
| Accidental tap triggers reset | Confirmation dialog with "Cancel" as default. No auto-confirm timer. |
| Reset mid-conversation | Brain stop() ends the session first; any in-flight TTS is abandoned. |
| Reset clears useful data the user wanted to keep | This is by design — it's a full factory reset. The word "scratch" is in the name. |

## 4. Out of scope

- Selective memory wipe (keep sessions but clear reminders, etc.) — would be a separate spec
- Reset-triggered backup before wipe (adb pull is the manual escape hatch)
- Cloud-synced state restore after reset