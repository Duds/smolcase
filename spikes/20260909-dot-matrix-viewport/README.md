---
id: 20260909-002
title: "SMOLCASE Dot Matrix Viewport"
type: spike
status: active
timebox: "1 day"
related: "[[spikes/20260909-smolcase-face/README]]"
---

# SMOLCASE Dot Matrix Viewport

## Question

Can the dot-matrix display be implemented as a true viewport translation layer, rather than as a mask over a blur?

This spike was split out of [[spikes/20260909-smolcase-face/README|the face sphere spike]] after review found that the original CSS mask was a sampling mask, not a translation layer.

## Scope

### In scope

- Render the source face to an offscreen render target.
- Quantise that target into a discrete dot grid.
- Compute one luminance value per dot from the region it represents.
- Render each dot as an emitter, with core, glow, and halation.
- Expose the optical parameters that matter for an appliance display.

### Out of scope

- The sphere gaze model itself, which lives in the face spike.
- Android implementation.
- Persistence or production abstractions.

## Architecture

```text
Source scene (sphere + eyes + key light)
  ↓ render
Offscreen WebGLRenderTarget
  ↓ quantise
Per-dot luminance, sampled as a 5-tap mean per cell
  ↓ emit
Fullscreen shader: core disc + glow + halation + ghost floor
  ↓
Screen
```

The source never reaches the screen directly. There is a debug toggle to bypass the viewport and inspect the raw source.

## Why this is a translation layer

The previous approach was:

```text
sphere → CSS backdrop blur → CSS mask with punched holes
```

That blurs the whole image uniformly, then samples it sparsely. It cannot express per-dot optics.

This approach computes a value per dot, then renders each dot. Blur is expressed as per-dot glow, not as a uniform screen blur.

## Constraints

- Runs locally as an HTML file.
- Uses Three.js, including a custom `ShaderMaterial`.
- No build step.
- Prototype code is throwaway.

## Parameters

| Group | Parameters |
|---|---|
| Grid | columns, rows, dot radius, edge softness, cell fill |
| Emitter optics | glow, glow spread, halation, ghost floor, exposure |
| Source model | face height, yaw range, pitch range, eye size |
| Debug | bypass viewport, show source sphere |

Three one-click recipes are provided: Crisp appliance, Soft dreamy, Blown neon. They set the optics parameters only and leave the grid and source model alone.

The `Copy parameters` button dumps the full parameter set as JSON and copies it to the clipboard. `Apply parameters` restores a pasted set, so a look you like can be saved into `findings.md` and rebuilt later. `Save PNG` writes the current canvas.

## Success criteria

- [ ] Source renders offscreen and reaches the screen only through the shader.
- [ ] Dot grid is visible and matches the configured columns and rows.
- [ ] Dots over the eyes are measurably brighter than dots over unlit sphere.
- [ ] Per-dot glow and halation respond to their parameters.
- [ ] Ghost floor makes unlit dots faintly visible.
- [ ] Findings record whether this translation layer is worth carrying into the real implementation.

## Locked defaults

The prototype ships with these defaults. Paste this JSON into the parameter box to restore them.

```json
{
  "cols": 38,
  "rows": 68,
  "dotRadius": 0.36,
  "dotSoftness": 0.3,
  "cellFill": 0.82,
  "glow": 1.8,
  "glowSpread": 0.5,
  "halation": 0.6,
  "ghostFloor": 0.03,
  "exposure": 4,
  "anchor": 2.8,
  "yaw": 50,
  "pitch": 50,
  "eye": 0.52,
  "lightIntensity": 1,
  "falloff": 0.1,
  "fillIntensity": 0.3,
  "ambientIntensity": 0,
  "sphereLift": 1.3,
  "showSource": false,
  "showSphere": true
}
```

This is the Blown neon optics profile with a dimmer key, a small amount of falloff, and a lifted albedo.

## Run

```bash
python3 -m http.server 8000 --directory .
```

Then open `http://localhost:8000/spikes/20260909-dot-matrix-viewport/`.

## Files

- `index.html`, runnable prototype.
- `findings.md`, observation log and verdict.
