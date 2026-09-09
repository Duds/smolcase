---
id: 20260910-001
title: Implement and verify GPU face parity
type: task
status: active
---

# GPU face parity execution

User story and Given/When/Then acceptance criteria: [[docs/06-specs/2026-09-10-gpu-face-parity-design]].

1. Test captured parameter loading, implement strict parameter model and asset.
2. Test deterministic frame resolution and elapsed-time animation, implement frame model.
3. Generate GLES-compatible shaders directly from spike sources; build two-pass FBO renderer with checked shader/link/FBO status and explicit colour handling.
4. Replace active TarsFaceView CPU rasterisation, preserve interaction APIs and separate telemetry.
5. Add debug comparison activity and browser/device capture harness with identical viewport, fixed state and parameter sweeps.
6. Run JVM tests, build, compare raw/dot output on Pixel 8; inspect shader errors and pause/resume. Fix measured mismatches before claiming parity.
7. Install with `adb -s <verified serial> install -r`, never uninstall or clear app data. Restore normal app after comparison.

## Evidence

Pending implementation. Captures and metrics will be written under `/tmp/smolcase-gpu-parity/`; record results here before closing.

Related: [[TASKS]], [[PROJECT_INDEX]], [[docs/07-plans/wayfinder-map]].
