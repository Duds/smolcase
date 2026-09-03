# Wayfinder Map: Ship Walking SMOLCASE Robot

## Destination

Ship a desk-ready SMOLCASE robot that walks, talks, and responds autonomously — integrating the Android brain/face with a physical 3D-printed body, 2x 360° serial-bus servos, and an ESP32 BLE bridge.

## Notes

- **Domain**: Hardware + firmware + Android + MuJoCo RL integration
- **Skills to consult**: `tdd`, `code-standards`, `grill-me`, `living-map`, `7s-code`
- **Key references**: [[AGENTS]] (gotchas, architecture), [[docs/03-decisions/]] (ADR-001–005), [[docs/06-specs/]] (approved specs)
- **Safety Rule**: Snapshot current state before modifying any existing hardware paths or configs
- **Architecture**: Exactly 2 legs, 2x 360° serial-bus servos per [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]]. Never design for 4 legs or multi-jointed limbs.
- **Software side is shipping**: [[docs/06-specs/2026-08-17-gemma-backend-design|Gemma 4 E2B backend]], [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design|dot matrix eyes]], [[docs/06-specs/2026-08-29-settings-expansion-design|settings/cloud features]] are all complete and tested. No rework needed.

## Decisions so far

- [[docs/03-decisions/001-pixel8-brain\|ADR-001: Pixel 8 as brain]]: Phone is the onboard computer; no separate SBC required.
- [[docs/03-decisions/002-hierarchical-policies\|ADR-002: Hierarchical policies]]: ~20 small TFLite models instead of one monolithic policy.
- [[docs/03-decisions/003-cpg-mujoco-tflite\|ADR-003: CPG + MuJoCo + TFLite]]: Three-layer control architecture with CPG baseline, MuJoCo simulation, TFLite on-device inference.
- [[docs/03-decisions/004-esp32-ble-bridge\|ADR-004: ESP32 BLE bridge]]: ESP32 as wireless bridge; Pixel 8 sends BLE commands, ESP32 translates to serial-bus servo protocol.
- [[docs/03-decisions/005-case-architecture-2leg\|ADR-005: 2-leg CASE architecture]]: Two 360° serial-bus servos, `SC-CASE` chassis, `SC-LEG-L`/`SC-LEG-R` legs hinged at ~2/3 case height.
- [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design|Expressive Appliance Dot Matrix Eyes completed]]: SDF eye rasterizer, mood state machine, telemetry pips, 20/20 tests, debug APK built.
- [[docs/06-specs/2026-08-29-settings-expansion-design|Settings Expansion & Cloud Intelligence completed]]: WCAG AA settings, cloud TTS/Vision/ReplyGen, Pixel sensor integration, all tests passing.
- [[docs/06-specs/2026-08-17-gemma-backend-design|Gemma 4 E2B on-device inference verified]]: LiteRT-LM 0.16.0, CPU backend, sideloaded `.litertlm`, live inference on Pixel 8.

## Frontier

- [20260830-001 through 006: Conversation bug fixes](_tasks/20260830-001-directives-repetition.md) (wayfinder:task — replaces 20260829-002)
- [20260829-003: Decide serial-bus servo model for ~300g robot](_tasks/20260829-003-servo-selection.md) (wayfinder:research)
- [20260829-004: Design SC-CASE chassis and leg geometry in CAD](_tasks/20260829-004-sc-case-cad-design.md) (wayfinder:prototype)
- [20260830-008: Spec — Factory reset / reset-to-scratch](_tasks/20260830-008-reset-to-scratch.md) (wayfinder:spec)
- [20260830-009: Spec — First-start wakeup / onboarding](_tasks/20260830-009-wakeup-routine.md) (wayfinder:spec, blocked by 008)

## Blocked Tickets

- [20260829-005: Decide ESP32 module and battery architecture](_tasks/20260829-005-esp32-battery.md) (wayfinder:research)
  - *Blocked by:* 20260829-003 (servo dimensions affect power budget and physical layout)
- [20260829-006: Train Walk Backward PPO policy with final body geometry](_tasks/20260829-006-walk-backward-training.md) (wayfinder:task)
  - *Blocked by:* 20260829-004 (final CAD geometry needed for accurate MuJoCo mass/inertia)
- [20260829-007: Implement ESP32 BLE-to-serial-bus bridge firmware](_tasks/20260829-007-esp32-ble-bridge-firmware.md) (wayfinder:prototype)
  - *Blocked by:* 20260829-003, 20260829-005 (servo protocol + ESP32 model)
- [20260829-008: TFLite policy export and on-device inference pipeline](_tasks/20260829-008-tflite-on-device-pipeline.md) (wayfinder:task)
  - *Blocked by:* 20260829-006 (requires trained policy to export)
- [20260829-009: Integrate behaviour arbiter on Android (brain → BLE → servo)](_tasks/20260829-009-behaviour-arbiter-integration.md) (wayfinder:prototype)
  - *Blocked by:* 20260829-007, 20260829-008 (needs firmware + TFLite pipeline)
- [20260829-010: Full 30-behaviour training suite](_tasks/20260829-010-30-behaviour-suite.md) (wayfinder:task)
  - *Blocked by:* 20260829-006 (Walk Backward is prerequisite; remaining behaviours build on validated body model)

## Not yet specified

- *Thermal dissipation characterisation*: Pixel 8 running continuous LLM inference inside closed 3D-printed `SC-CASE` — may need vent channels or passive heatsink.
- *Sim-to-real transfer validation*: How closely does the MuJoCo model match the physical prototype? May need tuning iterations after first build.
- *Gait transition smoothness*: Can CPG + TFLite blend between walk forward/backward/turn without jitter or stutter? Depends on behaviour arbiter design.

## Out of scope

- Cloud dependency for core locomotion (must work fully offline)
- Multi-robot coordination or swarm behaviour
- Phone-replacement SBC architecture (rejected by ADR-001)
- 4+ leg or multi-jointed leg designs (rejected by ADR-005)