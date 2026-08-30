# Spec: First-Start Wakeup / Onboarding Routine

**Date:** 2026-08-30
**Status:** Draft
**Scope:** Android companion app — replaces the single-line BIRTH greeting with a multi-turn dialogue onboarding flow when the creature meets its human for the first time.

---

## 1. Problem

SMOLCASE's BIRTH greeting currently says "Systems nominal. So this is the desk." — a single throwaway line. The creature has no name, doesn't know who it answers to, and doesn't calibrate its personality dials with the human. The wakeup routine is the creature's first and most important impression — it should establish identity, relationship, and personality baseline.

## 2. Design

### 2.1 Trigger

The wakeup routine fires when `Arrival.isFirstEver == true` **and** `wakeup_complete` is `false`.

Normal BIRTH greeting continues to fire if `isFirstEver == true` but `wakeup_complete == true` (e.g. after a factory reset that was tested mid-cycle, or a re-onboarding scenario).

### 2.2 Animation chain

1. **Boot sequence:** Matrix dot sweep-in — all dots fade up column-by-column left-to-right over ~3s (like an old CRT terminal powering on)
2. **Eyes blink open:** Slow ~600ms farewell-style blink (the creature "waking up")
3. **Eyes settle:** `ALERT` mood — wide, curious, scanning
4. **Self-test pips:** Bottom telemetry pips pulse sequentially left-to-right (battery → BLE → mute) — a hardware self-test visual
5. **Gaze find:** Eyes drift slowly toward the detected face position, then lock on

### 2.3 Dialogue flow

All dialogue delivered in TARS register — low, measured, deadpan. The creature never fakes excitement during onboarding; it's a systems-check, not a birthday party.

```
CREATURE:  Systems nominal. Blank slate. I don't have a name yet.
           What should I call myself?

USER:      "SMOLCASE" / "Case" / "TARS Jr" / etc.

CREATURE:  [pauses 1s — considering]
           [NAME]. Noted. And who do I answer to?

USER:      "Dale" / "my name is X"

CREATURE:  [pauses 0.5s]
           Logged: [OWNER NAME]. One more thing — how direct should I be?
           Scale of one to ten, where ten is brutal honesty.

USER:      "Seven" / "ten" / "be gentle" / etc.

CREATURE:  [pauses 0.5s]
           [parsed value]. Consider my honesty dial calibrated.
           I'm ready when you are.
```

The creature then transitions to its normal idle state (WANDER gaze), awaiting the user's first real interaction.

### 2.4 Wakeup Mode — Conversation Routing

During onboarding, speech input is NOT routed through IntentRouter or LLM backends. Instead, a transient `WakeupHandler` parses answers:

| User input | Parsed as | Action |
|------------|-----------|--------|
| Any non-empty utterance during name prompt | Creature name | Store `creature_name` in `smolcase_soul` |
| Any utterance containing a recognised name during owner prompt | Owner name | Store `owner_name` in `smolcase_soul` |
| A number (1-10), or phrases like "brutal" / "gentle" / "direct" during dial prompt | Honesty dial value | Set `PersonalityDials.honesty` |
| Silence / timeout (15s per prompt) | Default values | Name → "SMOLCASE", Owner → "friend", Honesty → 90 |

The creature repeats one re-prompt per question if unrecognised, then accepts the default. Timeout per prompt: 15s, then advance to next question with default.

### 2.5 State persistence

| Key | Prefs file | Type | Default | Set by |
|-----|------------|------|---------|--------|
| `creature_name` | `smolcase_soul` | String | "SMOLCASE" | Name prompt |
| `owner_name` | `smolcase_soul` | String | "friend" | Owner prompt |
| `wakeup_complete` | `smolcase_soul` | Boolean | `false` | After dial calibration |

### 2.6 Wakeup completion

After the last dialogue exchange:
1. `MemoryStore.completeWakeup(name, owner)` persists all three keys atomically
2. `MemoryStore.beginSession()` is called with the current time (this IS session 1)
3. `ConversationLog.startSession()` is called (session 1)
4. Conversation routing switches to normal mode (IntentRouter + LLM backends)

## 3. Changes required

| File | Change |
|------|--------|
| `MemoryStore.kt` | Add keys: `KEY_CREATURE_NAME`, `KEY_OWNER_NAME`, `KEY_WAKEUP_COMPLETE` |
| | Add methods: `isWakeupComplete()`, `completeWakeup(name, owner)`, `creatureName()`, `ownerName()` |
| `CreatureBrain.kt` | Modify `greet()` — BIRTH branch checks `isWakeupComplete()`, delegates to WakeupHandler if not |
| `ConversationEngine.kt` | Add WakeupHandler transient mode — if `!memory.isWakeupComplete()`, route to WakeupHandler instead of IntentRouter + LLM |
| `TarsFaceView.kt` | Add boot animation: `animateBoot(onComplete: () -> Unit)` — matrix sweep-in → blink open → self-test pips |
| `CreatureVoice.kt` | No changes needed — existing TTS pipeline handles onboarding dialogue |
| New: `WakeupHandler.kt` | Parses name/owner/dial responses from speech input, orchestrates wakeup flow |

## 4. WakeupHandler.kt

```kotlin
package com.smolcase.companion

/**
 * Transient handler for the first-start onboarding dialogue.
 * Routes speech input to name/owner/dial parsing instead of normal
 * IntentRouter + LLM dispatch. Self-destructs after wakeup completes.
 */
class WakeupHandler(
    private val memory: MemoryStore,
    private val dials: PersonalityDials,
    private val onStageAdvance: (String) -> Unit,  // speak a line
    private val onComplete: () -> Unit
) {
    enum class Stage { NAME, OWNER, HONESTY, COMPLETE }
    private var stage = Stage.NAME
    private var retries = 0
    private val maxRetries = 2

    fun start() {
        stage = Stage.NAME
        retries = 0
        onStageAdvance("Systems nominal. Blank slate. I don't have a name yet. What should I call myself?")
    }

    fun handleInput(text: String) {
        when (stage) {
            Stage.NAME -> {
                val name = parseName(text)
                if (name != null || retries >= maxRetries) {
                    memory.persistName(name ?: "SMOLCASE")
                    advance(Stage.OWNER)
                } else {
                    retries++
                    onStageAdvance("I didn't catch that. What should I call myself?")
                }
            }
            Stage.OWNER -> {
                val owner = parseOwner(text)
                if (owner != null || retries >= maxRetries) {
                    memory.persistOwner(owner ?: "friend")
                    advance(Stage.HONESTY)
                } else {
                    retries++
                    onStageAdvance("And who do I answer to?")
                }
            }
            Stage.HONESTY -> {
                val honesty = parseHonesty(text)
                if (honesty != null || retries >= maxRetries) {
                    dials.honesty = honesty ?: 90
                    memory.completeWakeup()
                    advance(Stage.COMPLETE)
                } else {
                    retries++
                    onStageAdvance("Scale of one to ten — how direct should I be?")
                }
            }
            Stage.COMPLETE -> { /* no-op */ }
        }
    }
}
```

## 5. Risks

| Risk | Mitigation |
|------|------------|
| User says nothing during onboarding | 15s timeout per prompt → advance with default values |
| Speech recognition fails on name | Re-prompt twice, then accept default |
| User expects to skip onboarding | Not supported in v1 — all creatures get a name and owner. Can add skip in v2. |
| Wakeup interrupted by crash | `wakeup_complete` is only written at end — next launch re-runs from beginning |