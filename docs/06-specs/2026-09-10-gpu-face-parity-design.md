---
id: 20260910-001
title: GPU face parity with the expression spike
type: feature
status: approved
---

# GPU face parity

Approved by Dale's request to implement the two-pass GPU recommendation.

## User story

As the robot owner, I want the Android face to use the captured spike parameters and rendering equations, so the sphere, expressions, colour and emitter optics match the browser reference.

## Design

- Native OpenGL ES 3 renderer hosted inside TarsFaceView, retaining its app API.
- Source RGB pass: exact expression/sphere fragment shader from the spike.
- Dot pass: exact shared dot-matrix fragment shader. Keep colour space and framebuffer format explicit, verified against the browser, not adjusted by eye.
- Generate Android shader assets from the spike at build time. No separately hand-tuned shader copy.
- One packaged captured JSON object supplies all eye, sphere, optical, animation and toggle settings. Strict parsing and range validation. Runtime app dials/moods remain supported as explicit overrides.
- Gaze uses positive-up coordinates at the render boundary. Gaze rotates the sphere only; saccades offset the eye pattern independently.
- Frame-rate-independent expression/gaze damping and spike blink/saccade timing.
- Telemetry is a separate overlay, excluded from deterministic parity captures.
- Debug-only comparison activity: no camera, microphone, conversation or random animation; fixed parameters, gaze, blink and viewport. Does not alter user settings/data.

## Acceptance criteria and test boundaries

1. Given the captured JSON, when loaded and round-tripped, then every parameter is retained, invalid values are rejected and all renderer controls are connected.
2. Given a fixed mood/gaze/blink and viewport, when building a frame, then repeated output is deterministic, geometry uses screen aspect and the spike's resolved dial values.
3. Given animated frames, when elapsed time is equal at different refresh rates, then damping converges equivalently.
4. Given the browser and Android renderer with identical inputs, when captured, then raw and dot images agree within measured rasterisation/texture precision tolerance. Verify neutral, HEART, nonzero yaw/pitch, and changes to sphere and optics. Record metrics, not just passing Kotlin tests.
5. Given surface recreation or app pause/resume, when drawing resumes, then GL resources are recreated and the face renders without a leak or crash.
6. Given normal app use, when face/voice/touch/telemetry callbacks fire, then existing APIs and privacy controls still operate.

## Related

- [[docs/07-plans/2026-09-10-gpu-face-parity]]
- [[spikes/20260909-face-expressions/README]]
- [[spikes/20260909-dot-matrix-viewport/README]]
- [[docs/03-decisions/001-pixel8-brain]]
- [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]]
- [[PROJECT_INDEX]]
