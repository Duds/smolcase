# Findings

## Observation log

| Test | Result | Notes |
|---|---|---|
| Awake face loads | Pass | Sphere renders inside the 1080 x 2424 wrapper. |
| Centre-pinned rotation | Pass | Sphere rotates around its centre on mouse move and WASD. Translation was removed after the first iteration. |
| Eyes animate in place | Pass | Blink runs while the sphere rotates. Eyes stay attached to the sphere. |
| Eyes depth-correct | Pass after fix | Eyes were forced in front with `depthTest: false`. They now sit just outside the sphere surface, so depth testing is honest. |
| Layer toggles | Pass | Playwright verified sphere and edges on and off, no console errors. |
| Viewport seam understandable | Superseded | The CSS mask and blur layers were removed and moved to [[spikes/20260909-dot-matrix-viewport/README\|the viewport spike]]. |

## Verdict

The sphere-plus-eyes model is viable as a source representation. Centre-pinned rotation reads correctly and is easy to reason about.

Two corrections were needed to get there:

1. Gaze must be rotation only. The first implementation translated the sphere, which broke the centre pin.
2. The eyes were geometrically inside the opaque sphere. They were made visible by disabling depth testing, which hid the real problem. They now sit just outside the surface.

## Open

- The spike no longer contains any viewport layer, so this verdict covers the source model only.
- See [[review|the review]] for the 12 blockers raised, and which of them this split resolves.
