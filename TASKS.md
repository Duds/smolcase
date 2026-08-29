# TASKS.md — SMOLCASE Task Registry

> Living task board. Grouped by workstream and status.
> Linked to specs in `docs/06-specs/` and plans in `docs/07-plans/`.

---

## 🟢 In Progress

(None — all Android tasks complete)

---

## 🟡 Ready to Pick Up

### Appliance Face Dot Matrix — SVG Export & Surface Integration
- [ ] **Review & edit `mech/face-appliance-dots.svg`**
  - Check layer separation (bg / LEDs / Light Baffle / Light Mask / Sample Eyes / Eyes Glow) in an editor
  - Verify dot aperture positions against real Pixel 8 active area + dot pitch
  - Adjust physical panel dims / hole sizes for manufacture (print-ready)
- [ ] **Add SVG → Kotlin transform**
  - Bring the exported LED aperture map into the app as a layout/mask source
  - Transform dot-grid coordinates into `ApplianceMatrixCanvas` buffer coords (`setDot` / `blendDot`)
  - Sync shader constants (dot radius, ghost alpha, glow falloff) with the SVG source geometry

### Simulation & Policy Training
- [ ] **Design SC-CASE in CAD** (prerequisite for all below)
  - Define chassis geometry, leg attachment points, mass distribution
  - Export dimensions to MuJoCo XML for training
- [ ] **Train Walk Backward policy** (after CAD)
  - Update MuJoCo XML with new body dimensions, then train
  - Target: >5m backward displacement
- [ ] **TFLite Policy Export** (after training)
  - Export trained policies to `.tflite` and verify quantization fidelity

*Note: Training paused until CAD design is complete. CPG baseline verified working — fine-tuning on new geometry will be faster than training from scratch.*

### Hardware & Power Selection
- [ ] **Select Serial-Bus Servos**
  - Benchmark torque, speed, dimensions, and voltage requirements for ~300g robot (ADR-005)
  - Select 2x 360° serial-bus servos
- [ ] **Select ESP32 & Battery Pack**
  - Select ESP32 DevKit / module for BLE bridge
  - Select LiPo vs 18650 cell + buck converter

---

## 🔴 Blocked / Waiting on Dependencies

### Mechanical / CAD (mech/)
- [ ] **Design SC-CASE, SC-LEG-L, SC-LEG-R in Fusion 360**
  - *Blocked by:* Servo selection (need exact physical dimensions and mounting points)

### Firmware (firmware/)
- [ ] **Implement ESP32 BLE-to-Serial-Bus Bridge**
  - *Blocked by:* ESP32 hardware and servo selection (ADR-004)

### Policy Arbiter & On-Device Control
- [ ] **Android TFLite Inference Runtime & BLE Command Stream**
  - *Blocked by:* ESP32 BLE protocol verification & TFLite model validation

---

## 🏁 Completed

- [x] **Gemma 4 E2B On-Device Backend** — LiteRT-LM 0.16.0, CPU backend, sideloaded .litertlm, verified live inference on Pixel 8
- [x] **Expressive Appliance Dot Matrix Eyes** — Phases 1–4 complete (SDF eyes, expression state machine, telemetry pips, 20/20 tests, debug APK built)
- [x] ADR-001 through ADR-005 architecture definitions
- [x] MuJoCo simulation environment & CPG baseline
- [x] PPO training pipeline & Walk Forward policy (+7.32m)
- [x] TARS face v0.5 (glyph text face, telemetry feed, voice dials)
- [x] Empirical eye analysis & photometric study (`docs/01-research/analysis/`)
