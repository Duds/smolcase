# TASKS.md — SMOLCASE Task Registry

> Living task board. Grouped by workstream and status.
> Linked to specs in `docs/06-specs/` and plans in `docs/07-plans/`.

---

## 🟢 In Progress

(None — all Android tasks complete)

---

## 🟡 Ready to Pick Up

### Settings Expansion & Cloud Intelligence

See: `docs/06-specs/2026-08-29-settings-expansion-design.md`

#### Phase 0: Foundation
- [ ] P0-1 Create `ui/SettingsTheme.kt` with WCAG AA helper constants
- [ ] P0-2 Create `ui/SettingsSection.kt` collapsible section component
- [ ] P0-3 Add `AGENT` backend to `LlmSettings` with backward-compat migration
- [ ] P0-4 Rename `KimiBackend` → `AgentBackend`, generalise request construction

#### Phase 1: Settings UI Overhaul
- [x] P1-1 Scrollable layout with collapsible sections
- [x] P1-2 Apply WCAG AA: contentDescription, 48dp min targets, contrast
- [x] P1-3 Back-press warn on unsaved changes
- [x] P1-4 Replace Kimi-specific config with reusable `AgentEndpointForm`

#### Phase 2: Cloud TTS
- [x] P2-1 Create `cloud/CloudTtsBackend.kt`
- [x] P2-2 Add cloud TTS fields to `LlmSettings`
- [x] P2-3 Cloud TTS section UI in Settings
- [x] P2-4 Wire `CreatureVoice` → try cloud first, fall back to local

#### Phase 3: Cloud Vision
- [x] P3-1 Create `cloud/CloudVision.kt` (CameraX frame → base64 → endpoint)
- [x] P3-2 Add cloud Vision fields to `LlmSettings`
- [x] P3-3 Cloud Vision section UI in Settings
- [x] P3-4 Wire vision into `ConversationEngine` (rate-limited)

#### Phase 4: Cloud AI Reply Generation
- [x] P4-1 Add independent cloud reply gen fields to `LlmSettings`
- [x] P4-2 Cloud reply gen section UI in Settings
- [x] P4-3 Wire `ConversationEngine.reply()` through cloud endpoint

#### Phase 5: Sensors
- [x] P5-1 Create `sensors/SensorConfig.kt`
- [x] P5-2 Create `sensors/CreatureSenses.kt` (background HandlerThread)
- [x] P5-3 Sensor toggles + live readout UI in Settings
- [x] P5-4 Wire sensor state into creature awareness

#### Phase 6: Polish + Test
- [x] P6-1 Unit tests: AgentBackend, LlmSettings migration, SensorConfig
- [x] P6-2 Unit tests: CloudVision URL construction, CreatureSenses lifecycle
- [x] P6-3 Manual QA: all sections, save/load, back-button, offline fallback
- [x] P6-4 Final build: assembleDebug passes, deploy & smoke test

#### Phase 6: Polish + Test
- [ ] P6-1 Unit tests: AgentBackend, LlmSettings migration, SensorConfig
- [ ] P6-2 Unit tests: CloudVision URL construction, CreatureSenses lifecycle
- [ ] P6-3 Manual QA: all sections, save/load, back-button, offline fallback
- [ ] P6-4 Final build: assembleDebug passes, deploy & smoke test

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
