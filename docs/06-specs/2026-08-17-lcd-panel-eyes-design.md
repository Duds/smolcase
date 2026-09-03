# Spec: LCD Panel Eyes — Legibility Revision for TarsFaceView

**Date:** 2026-08-17
**Status:** Draft — pending owner review
**Amends:** [[docs/06-specs/2026-08-16-tars-face-persona-design]] §1 (The Face). All other sections (telemetry sources, voice, dials, rollout) unchanged.
**Scope:** `TarsFaceView.kt` rendering only. Public API, state timing (AWAKE/DROWSY/SLEEPING), telemetry feed content, and the idle-gaze-wander spec all untouched.
**Superseded by:** [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]]
**Implementation plan:** [[docs/07-plans/2026-08-17-lcd-panel-eyes]]
**Related:** [[docs/06-specs/2026-08-17-idle-gaze-wander-design]], [[docs/01-research/SMOLCASE-Robot-Eye-Libraries-Review]]

## Background — field test result

First live user test (2026-08-17, owner's 14-year-old son, unstructured play): **the subject did not perceive the eyes at all** and could not locate the pupils even after extended viewing, until they were pointed out. The face read as "scrolling green text," not "a robot looking at me."

Root cause (confirmed against `TarsFaceView.kt`):

1. **Negative-space pupil.** The pupil is a 2×2 block of cells painted black *into* the green text field — a hole, not an object. Figure-ground works against recognition.
2. **No eye boundary.** The eye region is an unbounded patch of text in a black void; nothing encloses it as a shape.
3. **Uniform salience.** Text, filler, and pupil compete at equal visual weight.

Heuristic framing (Nielsen): the current design demands **recall** ("interpret this gap in text as a pupil") instead of **recognition**. **Jakob's Law** compounds it: the reference class for "robot eyes" (Cozmo/Vector, Tamagotchi, RoboEyes-type OLED builds, LCD toys) is universally *dark pupil on a lighter bounded field*. The design violated the pattern users arrive with.

Owner direction: keep the text idiom if possible; increase eye text density; shift palette toward liquid-crystal display colour; if that fails, fall back to fully pixelated LCD style. **TARS persona constraint stands: no cartoon eyes, no rounded friendly blobs.**

## Design — Option A: LCD panel eyes (preferred)

Each eye becomes a **bounded LCD instrument panel**: a light liquid-crystal field carrying dark text segments, with the pupil as the darkest contiguous region on the panel. Text is fully retained — it becomes the panel's segment noise.

### 1. Palette (classic STN/calculator LCD)

| Element | Colour | Notes |
|---------|--------|-------|
| Screen void (lower 2/3, margins) | `#000000` | Unchanged — TARS restraint |
| Panel field | `#B8C4A9` | Grey-green LCD glass; slight warm tint |
| Text segments | `#2E3A28` at ~55% alpha | Dark olive; visible up close, texture at arm's length |
| Pupil | `#1A2214` at 100% | Near-black olive — always the darkest thing on the panel |
| Glint | `#E8F0DC` | Small light dot on pupil, upper-left (existing, retained — strong eye cue) |
| Panel bezel | 1 px `#5A6850` | Bounds the eye as a shape |

Phosphor `#33FF66` is retired from the face. If the owner wants a phosphor throwback, it becomes a debug-mode skin, not the default.

### 2. Eye structure

- **Panel:** one rounded-rect LCD window per eye (corner radius ~6% of panel width — instrument glass, not cartoon), positioned as today (upper third, two panels).
- **Density:** grid increases from 14×7 to **18 cols × 9 rows** per panel; text size shrinks accordingly. Denser, smaller text reads as LCD segment texture rather than "a document."
- **Pupil:** positive-space block, **5 cols × 4 rows** (~28% of panel width), rendered as solid pupil colour. Glint retained at upper-left of pupil.
- **Quiet ring (sclera cue):** text cells within 1 cell of the pupil render at half the normal segment alpha. Guarantees a luminance step around the pupil at all times — the recognition signal — regardless of what the feed is doing.
- **Salience invariant (testable):** at every frame, the pupil must be the largest darkest contiguous region on its panel. Enforced structurally: segment alpha cap + quiet ring; nothing else may render at pupil darkness.

### 3. Behaviour mapping (all existing logic carries over)

| Existing behaviour | LCD rendering |
|--------------------|---------------|
| Blink (rows collapse 140 ms) | Lid masks panel rows top+bottom; revealed area is screen void, not panel |
| Happy squint | Unchanged mechanism (bottom rows hidden) |
| DROWSY / SLEEPING | Unchanged timing; panel dims (field alpha → 60% / 25% + breathing) instead of row collapse alone |
| Speaking shimmer | Dark column-scan sweeps the panel (same phase logic, inverted polarity) |
| Telemetry feed | Unchanged content/scrolling/phase offset; rendered as panel segments |
| Idle gaze wander (2026-08-17 spec) | Unchanged — pupil block glides, quiet ring follows |
| Face tracking / micro-saccades | Unchanged |
| Triple-tap debug modes | Unchanged; add optional phosphor-skin mode if trivial |

### 4. Acceptance test — the son protocol

Recognition is the requirement, so recognition is the test:

1. Show the face to the same subject with no explanation. **Pass = subject identifies the eyes unprompted** (or on "what do you see?").
2. Ask **"where is it looking?"** while the face tracks. **Pass = subject can point to the gaze direction** (left/right/you). Gaze-direction legibility is the real functional requirement for a face-following robot.
3. Arm's-length desk check: panels read as instruments, pupil position readable at a glance.

If Option A fails test 1 or 2, escalate to **Option B (fallback):** pupil becomes a chunky pixelated LCD block (drawn pixels, no text within pupil ±2 cells; text demoted to a telemetry band below the panels). Option B is pre-approved as fallback direction by this spec but gets its own short spec if triggered.

## Out of scope

- Voice, dials, persona prompt, telemetry content, FaceTracker, servo/body behaviour.
- Any change to the monochrome single-hue constraint — LCD olive is one hue family; no multicolour UI.

## Testing

- **Unit:** salience invariant (no segment renders darker than threshold near pupil); quiet-ring alpha math; grid resize (18×9) doesn't break feed wrapping, pupil clamping, or the wander anchors from the idle-gaze spec.
- **Field:** the son protocol above, before and after. Record result in the design-thinking log.
- **Regression:** blink, drowsy (30 s), sleep (120 s), speaking shimmer, tap-to-mute, debug modes.
