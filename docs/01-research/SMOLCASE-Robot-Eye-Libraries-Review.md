# SMOLCASE Research: Robot Eye / Gaze Libraries Review

> **Date:** 2026-08-17
> **Subject:** Review of four eye-related open-source libraries for conceptual fit with SMOLCASE — FluxGarage/RoboEyes, orji123/IrisOLED, HanZhang-psych/pupeyes, pymovements.
> **Verdict:** Two rejected outright (wrong problem domain). Two rejected as code (wrong layer, wrong persona) but their **behavioural animation patterns are worth stealing** for `TarsFaceView.kt`.

---

## 1. Context

SMOLCASE's face is the Pixel 8 screen rendered by the Android companion app (ADR-001), currently the TARS glyph face (`TarsFaceView.kt`, spec `docs/06-specs/2026-08-16-tars-face-persona-design.md`). The app already does **face following**: `FaceTracker.kt` feeds a 2×2 glyph pupil that glides within the text grid with eased tracking + micro-saccades, and escalates DROWSY (no face 30 s) → SLEEPING (120 s).

Persona constraint (AGENTS.md + TARS spec): monochrome phosphor-green **text face** — no cute cartoon eyes, no emoji.

The ESP32 (ADR-004) is purely a BLE→serial-servo bridge; it drives no display.

---

## 2. Libraries Reviewed

| Library | What it is | Fit |
|---------|-----------|-----|
| [FluxGarage/RoboEyes](https://github.com/FluxGarage/RoboEyes) | Arduino lib: smoothly animated robot eyes on OLED (Adafruit GFX). Moods, autoblinker, idle wander, one-shot reactions, eased transitions | ⚠️ Patterns yes, code no |
| [orji123/IrisOLED](https://github.com/orji123/IrisOLED) | Arduino lib: 32 static eye-expression bitmaps (SSD1306/SH1106) + non-blocking frame player (MIT) | ⚠️ Weaker version of the same idea |
| [HanZhang-psych/pupeyes](https://github.com/HanZhang-psych/pupeyes) | Python: preprocessing/visualization of *human* eye-tracking data (pupillometry, fixations; EyeLink-oriented, GPL-3.0) | ❌ Wrong domain |
| [pymovements](https://pymovements.readthedocs.io/en/stable/) | Python: gaze data processing, event detection (I-VT fixations, microsaccades), public dataset library | ❌ Wrong domain |

### 2.1 Why pupeyes and pymovements are out

Both answer "where was a *human* looking" — offline/real-time analysis of recorded gaze data from research eye-trackers. SMOLCASE's need is the inverse: a robot *expressing* attention, plus simple real-time face detection (already handled by `FaceTracker` via on-device vision, not pupillometry). No BOM eye-tracker, no path into Android/ESP32/sim stacks.

### 2.2 Why RoboEyes and IrisOLED can't be used as code

1. **Wrong layer.** They are Arduino/C++ for tiny I²C OLEDs. SMOLCASE renders its face in Kotlin on the Pixel 8; the ESP32 drives servos only.
2. **Wrong persona.** Both are the rounded-rectangle cartoon-eye idiom the TARS spec explicitly rejected.

---

## 3. Lessons Worth Stealing (from RoboEyes, partially IrisOLED)

These map directly onto the existing `TarsFaceView` / `CreatureBrain` architecture — none require cartoon eyes; all translate to glyph-face behaviour:

1. **Autoblinker with randomized interval** — `setAutoblinker(interval, variation)`: blink every `interval ± random(variation)` seconds. RoboEyes' insight is that *fixed-period* blinking reads as mechanical; jittered timing reads as alive. Applies to the glyph pupil's blink/dim cycle.

2. **Idle gaze wander** — `setIdleMode(interval, variation)`: when nothing is happening, the eyes reposition randomly on a jittered timer. This is the missing middle state between active face-tracking and DROWSY — the pupil shouldn't freeze on the last-seen face position; it should drift, re-check, settle.

3. **Micro-saccade texture during tracking** — RoboEyes eases all motion and overlays small flickers; `TarsFaceView` already has "eased tracking + micro-saccades" (ported from EyesView). Validation that this pattern is the field-standard trick for believable gaze: big eased movements + small fast corrective hops.

4. **One-shot reaction animations as a separate channel** — `anim_confused()`, `anim_laugh()`, `blink()` are fire-and-forget overlays on top of the base state, not states themselves. Good pattern for `express()`: reactions should interrupt-and-return, never replace the underlying attention state machine.

5. **Moods as continuous modifiers, not discrete faces** — RoboEyes' TIRED/ANGRY/HAPPY adjust the *same* eyes (height, lid angle) rather than swapping in a different bitmap set (IrisOLED's approach). Modifier-over-base survives transitions smoothly; bitmap-swap pops. Matches the TARS spec's "expressions deliberately subtler" direction.

6. **Every transition eased, always** — "All state changes have smooth transitions" is the library's core design rule. Popping is what makes robot faces feel cheap. No exceptions, even for error/fallback states.

7. **Non-blocking frame advancement** — IrisOLED's `update()` advances animation on `millis()` without `delay()`, so expression never stalls the control loop. The Android analog: face animation on the render/Choreographer clock, never tied to BLE or LLM latency. The face must keep breathing while the brain is busy.

8. **Non-face glyphs earn their keep** — IrisOLED ships battery/signal/warning icons alongside eyes. Supports the TARS spec's telemetry-line / instrumentation-filler direction: status iconography in the same phosphor idiom is a legitimate, on-persona use of "eye library" thinking.

---

## 4. Open Threads

- None blocking. If `TarsFaceView` idle behaviour is ever revisited, item 2 (idle gaze wander) is the first candidate — spec it in `docs/06-specs/` before touching code (house rule 5).
