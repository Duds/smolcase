# Snapshot: Pre-review state

Captured: 2026-09-09, before implementing the review recommendations.

## Purpose

Preserve the working prototype exactly as reviewed, so the recommendations in `review.md` can be implemented from a known-good baseline and diffed afterwards.

## Contents

| File | State |
|---|---|
| `index.html` | Full working prototype with sphere model, key light, CSS blur or fog modes, dot-matrix mask, and live parameter controls |
| `README.md` | Spike scope, constraints, conceptual model, success criteria, parameters |
| `findings.md` | Observation log, still unfilled, verdict pending |
| `review.md` | Full critical review with 12 blockers and recommended next actions |

## Confirmed working at snapshot time

- Sphere renders inside the Pixel 10a-style 1080 x 2424 wrapper.
- Centre-pinned rotation on mouse move and WASD.
- Eyes blink in place, render in front via `depthTest: false`.
- Full-screen CSS blur layer, toggleable.
- 38 x 68 dot-matrix mask with punched circular apertures.
- Live controls for blur, blur mode, fog density, aperture, grid size, face height, yaw, pitch, eye size, light position, intensity, and four layer toggles.
- Playwright verification of all layer toggle states, no console errors.

## Known issues carried forward

See `review.md`. The headline problems are:

1. Dot-matrix substrate is black on black, so it is invisible by construction.
2. Blur is applied before the mask, so it is not per-dot optics.
3. No luminance quantisation, so it is a sampling mask rather than a viewport translation layer.
4. Eyes bypass depth testing rather than being geometrically correct.
5. Spike has drifted from its single-question objective.
6. `findings.md` was never filled in.

## Session transcript

The full session transcript for this work is at:

```text
~/.pi/agent/sessions/--Users-dalerogers-00-PERSONAL-projects-SMOLCASE--/2026-09-08T20-54-02-150Z_01a082cc-9aa4-7233-90bb-fa9112f5003b.jsonl
```

Not copied into the repo. Retrieve it from that path if full context is needed.

## How to restore

```bash
cp spikes/20260909-smolcase-face/snapshots/20260909-pre-review/index.html \
   spikes/20260909-smolcase-face/index.html
```
