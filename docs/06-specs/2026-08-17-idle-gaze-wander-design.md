# Spec: Idle Gaze Wander for TarsFaceView

**Date:** 2026-08-17
**Status:** Draft — pending owner review
**Scope:** Android companion app, `TarsFaceView.kt` internal attention behaviour only. No public API changes. Explicitly excluded: `FaceTracker`, gait/motion code, telemetry feed content, persona/voice.

## Background

The TARS face spec (2026-08-16) defines TRACKING (eased pupil glide + micro-saccades on `onFaceSeen`), DROWSY (no face 30 s), and SLEEPING (120 s) — but says nothing about the interval between *face lost* and *30 s*. As implemented, the pupil holds the last-seen face position: a frozen stare, which reads as a hung render, not a live instrument.

Research note `docs/01-research/SMOLCASE-Robot-Eye-Libraries-Review.md` (RoboEyes/IrisOLED review) identified the field-standard pattern for this gap: **idle gaze wander** — on a jittered timer, the eyes reposition to random points with eased motion, conveying "alert but unoccupied." This spec adapts that pattern to the glyph face, staying within the TARS register: restrained, instrumental, deadpan.

## Design

### 1. Attention state machine (internal to `TarsFaceView`)

Four states, priority-ordered. Higher-priority entries always win:

| Priority | State | Enter when | Pupil behaviour |
|---------:|-------|-----------|-----------------|
| 1 | TRACKING | `onFaceSeen` within last ~0.5 s | Eased glide toward face position + micro-saccades (existing) |
| 2 | REACTING | `express()` one-shot playing | One-shot overlay runs; underlying state resumes on completion (existing pattern, reaffirmed) |
| 3 | **WANDER (new)** | No face for 0.5–30 s, not speaking | Periodic eased drift to random anchors (§2) |
| 4 | DROWSY / SLEEPING | No face 30 s / 120 s (existing) | Unchanged |

Transitions:

- WANDER → TRACKING: face seen → ease to face position fast (~120 ms), not a snap. TARS notices you without flinching.
- TRACKING → WANDER: face lost → hold current position ~1.5 s (a beat of "where did you go"), then first wander move.
- WANDER → DROWSY: existing 30 s no-face timer; uninterrupted. Wander never resets the drowsy clock.
- `setSpeaking(true)`: wander amplitude halves and interval doubles — the face attends to the conversation partner (last-seen or default-centre anchor) rather than roaming. Speaking never triggers wander.

### 2. Wander behaviour

- **Timer:** next move at `2.5 s ± 1.5 s` uniform jitter (i.e. 1.0–4.0 s). Jittered, never fixed-period — fixed cadence reads as mechanical (RoboEyes lesson 1).
- **Anchors:** uniform-random target cell within each eye's grid, biased toward the central 60% of the field (edge stares feel alarmed; TARS is unbothered). Both eyes move to *the same* anchor in their own grids — the eyes are related instruments but never disagree about where attention points.
- **Motion:** eased glide ~200–300 ms (ease-out), then 1–2 micro-saccades (±1 cell, ~40 ms) on arrival, matching the existing tracking texture (lesson 3). Gaze beyond the edge still clamps per existing rule.
- **Dwell:** ~15% of moves are "double-takes": a second move to a nearby cell within 400 ms. Rare, so it reads as intentional.
- **Blink independence:** the existing random blink (3–7 s, jittered) runs on its own timer and never synchronizes with wander moves. Coincident blink+move is fine and desirable.
- **Grid interaction:** telemetry feed continues scrolling; pupil still displaces characters. Wander does not pause the feed.

### 3. Constraints reaffirmed (from research lessons)

- **Every transition eased.** No state change pops, including entering/exiting WANDER.
- **Non-blocking clock.** All timers on the render frame clock (Choreographer), never tied to BLE, LLM, or sensor callbacks. The face keeps breathing while the brain is busy.
- **Silent failure.** If `FaceTracker` stalls or reports garbage, behaviour degrades to wander → drowsy → sleep on the existing timers; no error state on the face.
- **Restraint.** No new expressions, no cartoon affordances, no emoji. Wander amplitude and rate must survive the arm's-length desk test: at a glance it should read as "quiet instrumentation," not "looking around nervously."

### 4. Tunables (constants, not settings)

| Constant | Default | Notes |
|----------|--------:|-------|
| `WANDER_ENTER_DELAY_MS` | 1500 | Hold after face lost before first move |
| `WANDER_INTERVAL_MIN_MS` | 1000 | Jitter lower bound |
| `WANDER_INTERVAL_MAX_MS` | 4000 | Jitter upper bound |
| `WANDER_GLIDE_MS` | 250 | Eased move duration |
| `WANDER_CENTER_BIAS` | 0.6 | Fraction of grid the anchor distribution favours |
| `DOUBLE_TAKE_PROB` | 0.15 | |
| `SPEAKING_INTERVAL_MULT` | 2.0 | Wander slows while speaking |
| `SPEAKING_AMPLITUDE_MULT` | 0.5 | Wander range shrinks while speaking |

Not user-facing dials — Humor/Honesty stay the only persona settings. If desk-testing shows these need per-user tuning, that becomes a separate spec.

## Out of scope

- Changes to `FaceTracker`, blink timing, DROWSY/SLEEPING thresholds, telemetry content, voice, dials.
- Body/leg expression of attention (servo motion on face-lost) — would need its own spec against ADR-003/ADR-005 and the behaviour curriculum.

## Testing

- **Unit:** anchor distribution stays within grid bounds and centre bias; jittered interval always within [min, max]; state priority (face seen during wander → TRACKING; express during wander → REACTING → returns to WANDER); drowsy timer unaffected by wander.
- **Desk test:** sit with the face for 10 min out of frame — does it read as alive-but-calm, or fidgety? Tune §4 constants once, from observation, then freeze them.
- **Regression:** tracking, blink, drowsy (30 s), sleep (120 s), tap-to-mute, reminders per the TARS plan checklist.
