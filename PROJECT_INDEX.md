# SMOLCASE — Project Index

> Living document. Last updated: 2026-08-30 (v0.7-wayfinder)
>
> **Purpose**: Central index for all discoveries, decisions, plans, status, and active wayfinding frontiers.
> Navigation: [[README]], [[AGENTS]], [[GOAL]], [[TASKS]], [[docs/07-plans/wayfinder-map]]
> See the [`.workspace-scratchpad`](.workspace-scratchpad.md) for raw ideas and cross-branch context.

---

## 🧭 Active Wayfinding & Frontiers

> **Current Destination**: Ship a desk-ready SMOLCASE robot that walks, talks, and responds autonomously.
> See the [Wayfinder Map](docs/07-plans/wayfinder-map.md) for the full decision ticket board.

- **Destination Contract**: [`GOAL.md`](GOAL.md)
- **Wayfinder Map (decision tickets)**: [`docs/07-plans/wayfinder-map.md`](docs/07-plans/wayfinder-map.md)
- **Execution Task Board**: [`TASKS.md`](TASKS.md)
- **Fog of War (Knowns / Unknowns / Unknown Unknowns)**:
  - **Knowns**: Pixel 8 screen-as-face works; LiteRT-LM Gemma 4 E2B runs on-device; Cozmo SDF rasterizer + mood state machine implemented; Settings & cloud intelligence complete; wiki-link cross-refs done; reset-to-scratch and wakeup specs drafted.
  - **Unknowns**: Exact serial-bus servo torque/speed profile for 300g load; ESP32 BLE-to-serial latency under load; SC-CASE final mass/inertia for accurate MuJoCo simulation.
  - **Unknown Unknowns**: Thermal dissipation of Pixel 8 running continuous LLM inference inside closed 3D-printed `SC-CASE`; center-of-mass stability during dynamic PPO gait transitions; sim-to-real transfer gap for 3D-printed body.

---

## 🗺️ Where to Find Things (5S Sustain Guide)

| If you want to... | Go to... |
|-------------------|----------|
| Understand the project | `README.md` (root) |
| See this index | `PROJECT_INDEX.md` (root) |
| Active tasks & status | `TASKS.md` (root) |
| Decision tickets & frontier | `docs/07-plans/wayfinder-map.md` + `_tasks/` |
| Agent/convention rules | `AGENTS.md` (root) |
| Long-running goal protocol | `GOAL.md` (root) |
| Read GrowBot research | `docs/01-research/` |
| Read training curriculum | `docs/02-behaviour-training/` |
| Read architecture decisions | `docs/03-decisions/ADR-###.md` |
| Design thinking log & roadmap | `docs/05-design-thinking/SMOLCASE-Design-Thinking-Log.md` |
| Read approved specs | `docs/06-specs/` |
| Read implementation plans | `docs/07-plans/` |
| Check hardware specs | `docs/04-hardware/` |
| Run simulation / training | `sim/src/` |
| Check trained models | `models/` |
| View body XML | `sim/assets/` |
| Check model manifest | `models/README.md` |
| Work on ESP32 code | `firmware/` |
| Work on Android app | `android/` |
| Work on CAD | `mech/` — [face appliance dot matrix SVG](mech/face-appliance-dots.svg) |
| View training logs | `logs/` |

---

## ✅ Status Board

### Simulation & Training

| Item | Status | Evidence |
|------|--------|----------|
| MuJoCo environment | 🟢 Running | `sim/src/smolcase_train.py` loads GrowBot XML |
| CPG baseline | 🟢 Implemented | Integrated into env, provides rhythmic reference |
| PPO training pipeline | 🟢 Working | `smolcase_train.py` — train/eval/export modes |
| Walk Forward policy | 🟢 Trained | 500K steps, +7.32m vs CPG -1.13m |
| TensorBoard logging | 🟢 Active | `logs/PPO_1/` |
| Model checkpoints | 🟢 Saving | Every 50K steps in `models/checkpoints/` |
| TFLite export | 🟡 Partial | Code exists, not yet verified on-device |
| Walk Backward policy | ⚪ Queued | Next training target |
| Full 30-behaviour suite | ⚪ Planned | See `docs/02-behaviour-training/` |

### Hardware

| Item | Status | Notes |
|------|--------|-------|
| Servo selection | ⚪ Not started | Need spec: torque, speed, voltage, size |
| ESP32 board selection | ⚪ Not started | DevKit-C or similar |
| Battery selection | ⚪ Not started | LiPo vs 18650, capacity vs weight |
| Frame design | ⚪ Not started | Fusion 360, after servo selection |
| Phone mount | ⚪ Not started | Custom 3D print |
| Power management | ⚪ Not started | Buck converter, shared ground |
| BOM | ⚪ Not started | `docs/04-hardware/README.md` placeholder |

### Software

| Item | Status | Notes |
|------|--------|-------|
| Training pipeline | 🟢 Working | `sim/src/smolcase_train.py` |
| BLE protocol spec | 🟡 Draft | `docs/03-decisions/004-esp32-ble-bridge.md` |
| ESP32 firmware | ⚪ Not started | Blocked by servo + ESP32 selection |
| Android app | 🟢 v0.7-wayfinder | Dot matrix eyes, TARS persona, cloud intelligence, Gemma 4 E2B — all shipping |
| TFLite policy export | 🟡 Code exists | Not yet verified on-device; blocked by trained Walk Backward policy |
| Behaviour arbiter | ⚪ Not started | Blocked by TFLite pipeline + BLE bridge |
| Conversation quality | 🟡 In progress | Echo loop, prompt repetition, Gemma latency — tracked as bugs 20260830-001 through 006 |
| Voice system — audio fix | 🟢 Complete | Voice Step 1: per-track volume fade, decouple stream muting, invert voice preference |
| Voice system — Piper TTS | 🔴 Not started | Voice Step 2: replace Android TTS with Piper via sherpa-onnx |
| Voice system — streaming | 🔴 Not started | Voice Step 3: token streaming from LLM to speaker, sentence buffer |
| Voice system — TARS voice | ⚪ Deferred | Voice Step 4: zero-shot voice cloning, Pocket TTS evaluation |
| Cross-document wiki-links | 🟢 Complete | 100+ `[[wiki-links]]` across 33 files; conventions in AGENTS.md §10 |
| Reset-to-scratch spec | 🟡 Drafted | [[docs/06-specs/2026-08-30-reset-to-scratch-design\|Spec]] + [[docs/07-plans/2026-08-30-reset-to-scratch\|plan]] |
| Wakeup/onboarding spec | 🟡 Drafted | [[docs/06-specs/2026-08-30-wakeup-onboarding-design\|Spec]] + [[docs/07-plans/2026-08-30-wakeup-onboarding\|plan]] |

---

## 📚 Discovery Archive

### GrowBot Reverse-Engineering

Full 23,000-word analysis covering:
- Hardware: Pi Zero 2 W, 2x serial bus servos, camera module
- Software: ROS-based stack, gait models, TFLite runtime
- Phone-Creature Architecture: screen-as-face, sensor fusion, personality layer
- Monetization analysis: subscription model, hardware kit margins

📄 [`docs/01-research/SMOLCASE-GrowBot-Reverse-Engineering.md`](docs/01-research/SMOLCASE-GrowBot-Reverse-Engineering.md)

**Key findings**:
- GrowBot's gait model was **not open-sourced** — kept private for monetization
- Used monolithic end-to-end policy (not hierarchical)
- No Discord or public collaborator community found in videos
- Phone-as-brain architecture is sound and replicable

### Robot Eye / Gaze Libraries Review

Review of 4 eye-related libraries for SMOLCASE fit. pupeyes & pymovements rejected (human gaze-analysis, wrong domain); RoboEyes & IrisOLED rejected as code (Arduino/OLED, cartoon persona) but 8 reusable behavioural patterns captured: jittered autoblink, idle gaze wander, micro-saccade texture, one-shot reaction channel, moods-as-modifiers, always-eased transitions, non-blocking animation clock, status iconography.

📄 [`docs/01-research/SMOLCASE-Robot-Eye-Libraries-Review.md`](docs/01-research/SMOLCASE-Robot-Eye-Libraries-Review.md)

### Behaviour Training Plan

Full 25,000-word curriculum for 30+ behaviours across 5 tiers:
1. **Locomotion** — walk, trot, bound, turn
2. **Stability** — stand, sit, recover from falls
3. **Agility** — side-step, pivot, reverse
4. **Expressive** — bow, greet, cower, shake, head tilt
5. **Advanced** — climb, navigate, push, follow

Each behaviour includes: reward function, observation space, hyperparameters, training notes, sim-to-real transfer tips.

📄 [`docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan.md`](docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan.md)

---

### Specs

| Spec | Topic | Date | Status |
|------|-------|------|--------|
| [[docs/06-specs/2026-08-16-tars-face-persona-design\|2026-08-16 TARS Face & Persona]] | Glyph face, telemetry feed, voice & dials | 2026-08-16 | Approved — superseded by Appliance Eyes (visual system) |
| [[docs/06-specs/2026-08-17-idle-gaze-wander-design\|2026-08-17 Idle Gaze Wander]] | WANDER attention state between face-lost and DROWSY | 2026-08-17 | Draft |
| [[docs/06-specs/2026-08-17-lcd-panel-eyes-design\|2026-08-17 LCD Panel Eyes]] | Legibility revision: bounded LCD panels, positive-space pupil (field-test driven) | 2026-08-17 | Draft — superseded by Appliance Eyes |
| [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design\|2026-08-22 Expressive Appliance Eyes]] | Full-screen dot matrix, Cozmo-style kawaii expressive eyes (no pupils/glint), appliance telemetry | 2026-08-22 | Approved |
| [[docs/06-specs/2026-08-29-settings-expansion-design\|2026-08-29 Settings Expansion]] | WCAG AA settings UI, agnostic agent endpoints, cloud TTS/Vision/ReplyGen, Pixel sensors | 2026-08-29 | Draft |
| [[docs/06-specs/2026-08-17-gemma-backend-design\|2026-08-17 Gemma 4 E2B Backend]] | On-device Gemma LLM via LiteRT-LM, CPU backend, sideloaded model | 2026-08-17 | Approved — shipping |
| [[docs/06-specs/2026-08-30-reset-to-scratch-design\|2026-08-30 Reset to Scratch]] | Factory reset: wipe soul, dials, conversation log, restart fresh | 2026-08-30 | Draft — [[docs/07-plans/2026-08-30-reset-to-scratch\|plan]] |
| [[docs/06-specs/2026-08-30-wakeup-onboarding-design\|2026-08-30 Wakeup / Onboarding]] | First-start multi-turn dialogue: name, owner, honesty dial calibration | 2026-08-30 | Draft — [[docs/07-plans/2026-08-30-wakeup-onboarding\|plan]] |

---

## 🏛️ Architecture Decisions

| ADR | Decision | Date | Status |
|-----|----------|------|--------|
| [[docs/03-decisions/001-pixel8-brain\|ADR-001]] | Pixel 8 as brain (not Pi Zero) | 2026-08-06 | Accepted |
| [[docs/03-decisions/002-hierarchical-policies\|ADR-002]] | Hierarchical policy architecture (~20 TFLite models) | 2026-08-06 | Accepted |
| [[docs/03-decisions/003-cpg-mujoco-tflite\|ADR-003]] | Three-layer control: CPG + MuJoCo + TFLite | 2026-08-06 | Accepted |
| [[docs/03-decisions/004-esp32-ble-bridge\|ADR-004]] | ESP32 BLE bridge for servo control | 2026-08-06 | Accepted |
| [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]] | Case architecture: 2-leg CASE, 2x 360° serial bus servos | 2026-08-14 | Accepted |

---

## 🔬 Research Questions (Open)

| Question | Priority | Notes |
|----------|----------|-------|
| Can TFLite run quantized policies fast enough on Pixel 8? | High | Need to benchmark inference latency |
| What's the sim-to-real gap for our body? | High | Depends on accurate mass/inertia in MJCF |
| Did GrowBot have public collaborators with training data? | Low | Searched — no evidence found |
| Can CPG + TFLite blend smoothly without jitter? | Medium | Needs implementation testing |
| What's the optimal servo torque for ~300g robot? | High | Blocked on servo selection |

---

## 🎯 Next Actions (Priority Order)

> See the [Wayfinder Map](docs/07-plans/wayfinder-map.md) for the full decision dependency chain.

1. **Fix conversation bugs 20260830-001 through 005** — directives repetition, latency degradation, memory recall, dials ineffectiveness, multi-turn context
2. **Select servos** — Unlock mechanical design and BOM (wayfinder:research, ticket 20260829-003)
3. **Reset-to-scratch implementation** — spec written, ready for code (ticket 20260830-008)
4. **Wakeup/onboarding implementation** — spec written, ready for code (ticket 20260830-009, blocked by #3)
5. **Design SC-CASE in CAD** — After servo dimensions known (wayfinder:prototype, ticket 20260829-004)
6. **Select ESP32 & battery** — After servo power budget known (blocked by #5)
7. **Train Walk Backward policy** — After CAD geometry in MuJoCo (blocked by #5)
8. **Build ESP32 BLE bridge** — After servo + ESP32 selected (blocked by #6)
9. **TFLite export + on-device pipeline** — After Walk Backward policy (blocked by #7)
10. **Behaviour arbiter integration** — Brain → BLE → servo pipeline (blocked by #8)

---

## 📝 How to Update This Index

When you add something new to the project:

1. **New document?** Add it to the "Where to Find Things" table above
2. **New decision?** Write an ADR in `docs/03-decisions/` and link it here
3. **Status change?** Update the status board tables
4. **New question?** Add to "Research Questions"
5. **Next action complete?** Move it to status board, update priority queue
6. **New wiki-links?** Follow the conventions in [[AGENTS]] §10

> **Rule**: If it's not in this index, it doesn't exist.