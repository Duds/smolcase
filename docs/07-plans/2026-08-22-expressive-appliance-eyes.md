# Plan: Expressive Appliance Dot Matrix Eyes & Emotions Refactor

**Date:** 2026-08-22  
**Status:** In Progress  
**Spec Reference:** [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]]
**Supersedes:** [[docs/07-plans/2026-08-16-tars-face-persona]], [[docs/07-plans/2026-08-17-lcd-panel-eyes]]
**Related:** [[docs/01-research/SMOLCASE-Robot-Eye-Libraries-Review]], [[docs/06-specs/2026-08-17-idle-gaze-wander-design]]

---

## Phase 1: Core Dot Matrix & Procedural Eye Engine
- [ ] Implement `ApplianceMatrixCanvas`: full-screen LED/VFD dot grid with unlit ghost dots and sub-dot anti-aliasing.
- [ ] Implement `CozmoEyeRenderer`: procedural parametric eye model with SDF boundary, top/bottom lid deformation, slant angle, squash/stretch, and upper-half clamping ($Y \le 0.5$).
- [ ] Implement ambient breathing wave generator for unlit matrix dots.
- [ ] Add unit tests for eye coordinate projection, lid clipping, and matrix buffer rasterization.

## Phase 2: Expression & Reaction State Machine
- [ ] Implement `EyeExpressionState`: data models for emotional presets (Neutral, Skeptical, Happy, Curious, Drowsy, Surprised, Thinking).
- [ ] Implement spring-damper interpolator for continuous morphing between moods and gaze positions.
- [ ] Implement momentary reaction overlays (e.g., Heart icon morphing, Thinking radar scan).
- [ ] Wire TARS Honesty and Humour dials into emotive deformation parameters (e.g. lid asymmetry, bounce intensity).

## Phase 3: Telemetry & Face View Integration
- [ ] Implement bottom-edge appliance telemetry pips (battery level, BLE/Wi-Fi connection indicator).
- [ ] Refactor `TarsFaceView.kt` / `CompanionActivity` to use the unified full-screen matrix renderer.
- [ ] Remove deprecated `GlyphGrid.kt`, on-screen text transcripts, sliders, and old pupil/glint rendering code.
- [ ] Wire touch / tap gestures and IMU inertial lag into eye gaze.

## Phase 4: Verification & Polish
- [ ] Execute Android unit and instrumentation tests.
- [ ] Build and verify debug APK compilation (`gradle assembleDebug`).
- [ ] Test on-device rendering fidelity and frame rates.
