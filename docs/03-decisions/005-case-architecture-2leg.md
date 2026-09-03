# ADR-005: Case Architecture — 2-Leg CASE

## Status
Accepted — 2026-08-14

## Context

Early project docs (root README, hardware README, mech README, ADR-004 packet draft) described a quadruped with 8–12 servos. That was a stale authoring assumption — it never matched the GrowBot research (2-servo biped, 1-piece legs, Feetech SCS0009 serial bus) or the simulation (`smolcase_train.py` and the MuJoCo XML are already a 2-leg, 2-actuator body). All records were corrected on 2026-08-14.

The owner confirmed the actual mechanical concept (2026-08-14):

- **Three primary components: CASE, LEFT LEG, RIGHT LEG.**
- Legs are simple one-piece parts, each directly driven by a 360° servo.
- Legs rotate a full 360° beside the case (GrowBot-style tumble/windmill gait).
- Servo hinge point is at ~2/3 of the case height.
- Leg length ≈ case height → leg tip reaches ~1/3 of case height below the case bottom when pointing straight down.
- Secondary components live inside CASE only: sub-frames, covers, PCB mount brackets.

Full ideation trail: [`docs/04-hardware/SMOLCASE-Case-Design-Brainstorm.md`](../04-hardware/SMOLCASE-Case-Design-Brainstorm.md).

## Decision

### 1. Mechanical architecture

- **CASE** is the single structural chassis: it mounts the phone, both servos, the PCB, and the battery. Secondary parts (brackets, cover) exist only where printability or serviceability forces them.
- **LEG-L / LEG-R**: one-piece printed legs, one servo each, no knee/elbow joints.
- Servo pods (**HIP POD-L/R**) are integrated into the CASE side walls at 2/3 height, not separate brackets (unless printability forces it).

### 1a. CASE construction (owner decision, 2026-08-14)

- **Monolithic tub + back lid (SHELL).** The tub is one continuous structural print carrying the phone channel (JAW latch) and both servo pods; SHELL is a thin, non-structural lid closing the electronics/battery cavity (2–4 screws into heat-set inserts). The clamshell (mid-plane split) was rejected: its seam lands exactly on the tumble impact line, splits screw bosses into thin half-walls, and reduces cradle rigidity (IMU coupling).
- **Servos load internally.** Each servo inserts into its pod from inside the cavity before SHELL closes; the horn shaft passes through the pod wall to the leg outside. Consequence: servo replacement = remove SHELL, disconnect bus, swap — phone never comes out.
- Assembly order: servos → RIBCAGE (PCB) → BELLY (battery) → wire/bus → SHELL; phone slides into the front channel last (or anytime).
- Print orientation: tub open-side-up, no supports; lid flat.

### 2. Naming

| Part | Name | Code |
|------|------|------|
| Phone case / main body | CASE | `SC-CASE` |
| Left / right leg | LEG-L / LEG-R | `SC-LEG-L`, `SC-LEG-R` |
| Servo pockets in CASE walls | HIP POD-L / HIP POD-R | `SC-POD-L/R` |
| Internal PCB bracket | RIBCAGE | `SC-RIB` |
| Battery tray / retainer | BELLY | `SC-BLY` |
| Back cover / service panel | SHELL | `SC-SHL` |
| Phone retention latch | JAW | `SC-JAW` |

Rule: Fusion 360 components, BOM lines, firmware constants, and ADRs share these names exactly.

### 3. Servos

- Class: **360° positional serial bus servo** — magnetic encoder, absolute position across full rotation, torque/temp/voltage feedback, half-duplex UART daisy-chain.
- Candidate: **Feetech STS3215-class** (~19–30 kg·cm, ~55 g, ~$20). Final model selection is open (brainstorm Q1); SCS0009 (GrowBot's part) is the proven fallback but likely underpowered at this scale.
- Explicitly rejected: continuous-rotation servos (speed control only — gait impossible) and 270°/300° PWM servos (dead zone blocks full-circle tumble).
- Torque basis: worst case ~107 mm leg lifting ~600 g → ~6.5 kg·cm static, ~15–20 kg·cm with dynamic/tumbling margin.

### 4. Layout and dimensions (targets, refined in CAD)

- CASE: ~162 × 82 × 26–32 mm (anchored to Pixel 8: 150.5 × 70.8 × 8.9 mm, 187 g).
- Legs: ~162 mm; pivot ~108 mm from case bottom; ~54 mm ground clearance on leg tips.
- Battery (2S 18650) centered behind the phone — the robot rotates around its own body, so CoG symmetry about the tumble axis is a primary design constraint.
- Servo rail and logic rail on the RIBCAGE board; SHELL back cover as the only service panel; JAW slide-in phone latch.
- All-up mass target: ~570–600 g.

### 5. Comms and power

- BLE remains the primary phone↔ESP32 command channel (ADR-004 unchanged) — a tumbling robot cannot have external cables.
- RIBCAGE provides an **internal** short USB-C lead (5 V/2 A) so the robot battery charges the docked phone.
- ESP32-S3 native USB is reserved as a wired data fallback if measured BLE loop jitter proves inadequate.
- Firmware servo interface is **UART serial bus** (half-duplex), superseding the `ledc` PWM note in ADR-004's firmware stack section.

## Rationale

- **Minimal part count = minimal iteration cost.** Each part is print time, assembly time, and a failure joint. Three primary parts is the floor.
- **The gait is the phase relationship of two oscillators.** Two independent 360° servos map directly onto a 2-oscillator CPG (ADR-003) — phase-locked control is a natural primitive.
- **Sim already matches.** The MuJoCo body is 2-leg; no architecture rewrite needed, only re-measured SMOLCASE geometry.
- **Feedback-capable servos close the sim-to-real loop.** Position/torque/temperature telemetry lets the CPG and safety reflexes run on real state, not open-loop assumptions.

## Consequences

- BOM drops to ~$95–120 (phone excluded); servos and cells dominate.
- The leg is the foot at every rotation angle → symmetric cross-section with TPU pads at both ends.
- Leg sweep clearance (legs vs. case, vs. each other, vs. table edges) must be validated in a Fusion 360 motion study before any print.
- Behaviour curriculum gait names (trot, bound) need reinterpretation for a 2-leg tumbler — handled in the training plan, not here.
- Open questions deferred to the brainstorm doc (Phase 4): final servo model, phone-charging in v1 vs v1.1, 18650 vs LiPo pouch, fixed vs replaceable leg tips, battery-behind-phone vs below-phone, leg-crossing gaits.

## Related

- [[docs/03-decisions/001-pixel8-brain|ADR-001: Pixel 8 as Brain]]
- [[docs/03-decisions/002-hierarchical-policies|ADR-002: Hierarchical Policies]]
- [[docs/03-decisions/003-cpg-mujoco-tflite|ADR-003: CPG + MuJoCo + TFLite]] (2-oscillator CPG)
- [[docs/03-decisions/004-esp32-ble-bridge|ADR-004: ESP32 BLE Bridge]] (UART servo interface supersedes its PWM note)
- [[docs/04-hardware/SMOLCASE-Case-Design-Brainstorm]] — ideation source
- [[docs/01-research/SMOLCASE-GrowBot-Reverse-Engineering]] — 2-servo biped evidence
- [[mech/SMOLCASE-CASE-Layout-Spec]] — Fusion 360 datum layout
