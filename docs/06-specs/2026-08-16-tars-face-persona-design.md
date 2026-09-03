# Spec: TARS Face & Persona for SMOLCASE Companion

**Date:** 2026-08-16
**Status:** Approved design (sections 1–4 approved in dialogue), pending written-spec review
**Scope:** Android companion app only — face rendering, voice, persona. Explicitly excluded: gait/motion vocabulary (MuJoCo/ML curriculum stays; SMOLCASE never does "the wheel").
**Superseded by:** [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]] (visual system; persona/dials remain)
**Implementation plan:** [[docs/07-plans/2026-08-16-tars-face-persona]]
**Related:** [[docs/03-decisions/001-pixel8-brain\|ADR-001]], [[docs/06-specs/2026-08-17-idle-gaze-wander-design]], [[docs/06-specs/2026-08-17-lcd-panel-eyes-design]]

## Background

Owner directive: SMOLCASE is based on TARS/CASE from Interstellar. The current cute-creature face (round cartoon eyes) and high-pitched voice don't deliver the TARS vibe. Research notes:

- TARS (Wired, 2014-11): monolithic articulated slab; **no face, just screens**; personality "between a marine company commander and a gym teacher" (Bill Irwin); personality as **settings** (humor 75%, honesty 90%).
- Interstellar's **CASE** performs the asterisk/"wheel" tumble — mechanically analogous to SMOLCASE's 360° rotating-leg design (ADR-005). Name resonance confirmed.
- Charlie Diaz TARS replica: Hackster guide (Pi 3B+, PCA9685), CAD/code via GPTARS_Interstellar and forks (e.g. pyrater/TARS-AI, CC-BY-NU — attribution required, no monetization). GPTARS precedent: humor setting adjustable by dialogue.
- Grounding check: ADR-001 committed to "screen as face" but never to cartoon eyes; ADR-002's specialist-policy stack treats motion as discrete vocabulary (already TARS-compatible).

## Design

### 1. The Face — `TarsFaceView.kt` (NEW)

Canvas glyph-grid rendering (approved Approach 1; TextView and WebView approaches rejected for jank and weight).

- Portrait, phone flat, screen up. **Two eye fields in the upper 1/3** of the screen; lower 2/3 stays black void (TARS restraint).
- Each eye: monospace glyph grid ~14 cols × 7 rows, phosphor green (~#33FF66) on black.
- **Pupil:** 2×2 block of `█` glyphs, gliding within the grid, eased tracking + micro-saccades (ported from EyesView). (Block over gap chosen for arm's-length visibility; switchable.)
- **Blink:** rows collapse symmetrically top+bottom toward middle over ~140 ms, hold, reopen. Random every 3–7 s.
- **Happy squint:** bottom rows rise (^^ in text).
- **DROWSY** (no face 30 s): grid at half rows. **SLEEPING** (120 s): single dim middle row, slow scroll, breathing brightness.
- **Speaking:** column-scan shimmer sweeps the grid (replaces cute smile-pulse).
- Same public API as EyesView (`onFaceSeen`, `express`, `farewell`, `setSpeaking`, `setMicMuted`) — drop-in replacement; MainActivity/CreatureBrain/CreatureVoice wiring unchanged.

### 2. The Text — `TelemetryFeed.kt` (NEW, hybrid real + filler)

- Real telemetry lines rotate through the grids: soul file (`SESS 0142 · TOGETHER 3.7H`), reminders (`REM: CALL MUM`), creature state, battery, clock/next-meeting, dial values (`HUMOR 75 · HONESTY 90`), IMU (when body exists).
- Lines enter bottom row, scroll up ~2 rows/s; pupil displaces characters (text parts around it).
- Two eyes run the same feed at independent phase offsets — related instruments, not mirrored.
- Filler (weighted `▓▒░:·` + hex, low density) fills empty queue so idle looks like quiet instrumentation.
- SLEEPING: feed reduces to clock + slow filler.
- Every source fails silently to filler; the face never errors.
- Triple-tap cycles debug modes: normal (hybrid) → all-filler → all-telemetry → frozen frame → back to normal.

### 3. The Voice & The Dials

- `CreatureVoice` retune: pitch 0.85, rate 0.92 — low, measured, deadpan. Remove smile-pulse.
- New persona prompt (interpolates dials + soul context): marine-commander/gym-teacher register; 1–2 short sentences; no emoji; never breaks character; never mentions being an AI.
- **Dials:** Humor % / Honesty % (defaults 75/90), persisted, sliders in SettingsActivity, injected into every LLM prompt.
- **Dial voice commands** in IntentRouter (offline-capable): "set humor to 90", "honesty 50 percent" → in-character confirmation. Out-of-range rejected in character.
- Greeting lines re-voiced (BIRTH: "Systems nominal. So this is the desk."; GOOD_MORNING: "Morning. You're N minutes later than usual."; BACK_ALREADY: "That was fast."). Expressions become subtler (smaller widen, fewer blinks).

### 4. Architecture / Rollout / Testing

- Untouched: `MainActivity`, `FaceTracker`, `VoiceEars`, `ConversationEngine`, `MemoryStore` (except line sources reading it), MuJoCo curriculum, ADR-005, mech spec.
- LLM failure → IntentRouter fallback (existing behavior).
- Testing: triple-tap legibility check; desk-test day (does it read as TARS at arm's length; does deadpan survive a day); dial A/B test (humor 10 vs 90, same question); regression pass on tracking/blink/sleep/tap-mute/reminders.
- Rollout: single build v0.5-tars, wireless adb deploy.

## Attribution note

TARS/CASE are Warner Bros./Paramount IP; Charlie Diaz's CAD is CC-BY-NU. This spec borrows *design language only* — no Diaz CAD or code is used. Personal non-commercial project; add credit line to README acknowledgements if any community assets are ever imported.
