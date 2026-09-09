---
id: 20260909-001
title: "SMOLCASE Face Sphere Architecture"
type: spike
status: active
timebox: "1 day"
---

# SMOLCASE Face Sphere Architecture

## Question

Can SMOLCASE's face be modelled as a 3D sphere carrying animated eyes, with gaze expressed by moving the sphere and expression expressed by animating the eyes in place?

The eventual dot-matrix display is a viewport translation layer. It is deliberately **not implemented in this spike**. This spike tests the underlying spatial and animation architecture before display translation.

## Scope

### In scope

- A Three.js sphere as the face's underlying spatial model.
- Awake state only.
- A centre-pinned sphere that rotates around its centre to express gaze direction.
- Eyes that animate in place while remaining attached to the rotating sphere.
- A seam where a future dot-matrix viewport can consume the rendered face.
- A runnable local HTML prototype.
- A portrait screen wrapper using Pixel 10a-style 1080 x 2424 logical screen dimensions.

### Out of scope

- Dot-matrix rendering, LED quantisation, aperture masks, or viewport sampling. Moved to [[spikes/20260909-dot-matrix-viewport/README|the dot matrix viewport spike]] after review.
- CSS blur layers and CSS mask layers. Removed from this spike so it answers one question only.
- Complex moods and expressions beyond the awake baseline.
- Android implementation.
- Persistence, production abstractions, or performance optimisation.

## Constraints

- Runs locally as an HTML file.
- Uses Three.js.
- Must be easy to run without a build step.
- Prototype code is throwaway and must not be treated as production face code.

## Conceptual model

```text
Face state
  ├── gaze target ──> sphere rotation around centre pin
  └── expression ──> eye animation in place
                              │
                              ▼
                    future dot-matrix viewport
                              │
                              ▼
                    physical display output
```

The prototype intentionally shows the sphere. In the final system, the user sees only the sphere as translated through the dot-matrix viewport.

The current wrapper uses a 1080 x 2424 portrait aspect ratio. It is a CSS viewport model, not a physical bezel or hardware measurement.

## Live parameters

The prototype exposes live controls for face height (default 2.8), yaw range, pitch range, eye size, and the key light's X, Y, Z position and intensity. Layer toggles control the source sphere faces and sphere edges. These controls are exploratory only and are not persisted.

## Snapshot

A pre-review snapshot, taken while the spike still carried the CSS mask and blur layers, is preserved in `snapshots/20260909-pre-review/`. Restore it with:

```bash
cp spikes/20260909-smolcase-face/snapshots/20260909-pre-review/index.html \
   spikes/20260909-smolcase-face/index.html
```

## Success criteria

- [ ] Awake face loads in a browser.
- [ ] Sphere remains pinned at its centre while rotating smoothly in response to gaze input.
- [ ] Eyes remain spatially attached to the rotating sphere while animating independently.
- [ ] The architecture makes the future viewport translation seam explicit.
- [ ] Findings record whether the concept is worth carrying into the dot-matrix implementation.

## Run

Open `index.html` directly in a browser, or serve the folder:

```bash
python3 -m http.server 8000 --directory .
```

Then open `http://localhost:8000/spikes/20260909-smolcase-face/`.

## Files

- `index.html`, runnable Three.js prototype.
- `findings.md`, observation log and final verdict.
- `review.md`, critical review of objective and code, with 12 blockers and recommended actions.
- `snapshots/20260909-pre-review/`, frozen pre-review baseline of all of the above.
