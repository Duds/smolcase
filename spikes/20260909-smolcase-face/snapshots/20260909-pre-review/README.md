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
- A full-screen CSS backdrop-blur layer filling the phone viewport as a display experiment.
- A black 38 x 68 CSS dot-matrix substrate in front of the blur, with circular apertures punched through as the display viewport.

### Out of scope

- Dot-matrix rendering, LED quantisation, aperture masks, or viewport sampling.
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

The prototype exposes live controls for blur strength, blur mode (CSS layer or Three.js 3D fog), fog density, aperture size, dot-grid columns and rows, face height (default 2.8), gaze rotation range, eye size, and the key light's X, Y, Z position and intensity. Layer toggles control the source sphere faces, sphere edges, blur layer, and dot-matrix layer. These controls are exploratory only and are not persisted.

## Success criteria

- [ ] Awake face loads in a browser.
- [ ] Sphere remains pinned at its centre while rotating smoothly in response to gaze input.
- [ ] Eyes remain spatially attached to the rotating sphere while animating independently.
- [ ] The architecture makes the future viewport translation seam explicit.
- [ ] Findings record whether the concept is worth carrying into the dot-matrix implementation.

## Run

Open `index.html` directly in a browser, or serve the folder:

```bash
python3 -m http.server 8000 --directory spikes/20260909-smolcase-face
```

Then open `http://localhost:8000`.

## Files

- `index.html`, runnable Three.js prototype.
- `findings.md`, observation log and final verdict.
