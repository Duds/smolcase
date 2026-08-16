# SMOLCASE — Project Index

> Living document. Last updated: 2026-08-16 (v0.5-tars)
>
> **Purpose**: Central index for all discoveries, decisions, plans, and status. When you find something in this project, it should be findable from here.

---

## 🗺️ Where to Find Things (5S Sustain Guide)

| If you want to... | Go to... |
|-------------------|----------|
| Understand the project | `README.md` (root) |
| See this index | `PROJECT_INDEX.md` (root) |
| Agent/convention rules | `AGENTS.md` (root) |
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
| Work on CAD | `mech/` |
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
| ESP32 firmware | ⚪ Not started | `firmware/README.md` placeholder |
| Android app | 🟢 v0.5-tars | `android/` — TARS glyph face, telemetry feed, humor/honesty dials, LLM layer |
| TFLite inference on Pixel 8 | ⚪ Not started | Need to verify TFLite + GPU delegate |
| Kimi LLM integration | ⚪ Not started | Future personality layer |
| Behaviour arbiter | ⚪ Not started | State machine / rules engine |

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

## 🏛️ Architecture Decisions

| ADR | Decision | Date | Status |
|-----|----------|------|--------|
| [ADR-001](docs/03-decisions/001-pixel8-brain.md) | Pixel 8 as brain (not Pi Zero) | 2026-08-06 | Accepted |
| [ADR-002](docs/03-decisions/002-hierarchical-policies.md) | Hierarchical policy architecture (~20 TFLite models) | 2026-08-06 | Accepted |
| [ADR-003](docs/03-decisions/003-cpg-mujoco-tflite.md) | Three-layer control: CPG + MuJoCo + TFLite | 2026-08-06 | Accepted |
| [ADR-004](docs/03-decisions/004-esp32-ble-bridge.md) | ESP32 BLE bridge for servo control | 2026-08-06 | Accepted |
| [ADR-005](docs/03-decisions/005-case-architecture-2leg.md) | Case architecture: 2-leg CASE, 2x 360° serial bus servos | 2026-08-14 | Accepted |

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

1. **Train Walk Backward policy** — Next behaviour in Tier 1
2. **Verify TFLite export** — Ensure `.tflite` loads and runs on Pixel 8
3. **Select servos** — Unlock mechanical design and BOM
4. **Design body in Fusion 360** — After servo dimensions known
5. **Prototype ESP32 BLE bridge** — Flash firmware, test PWM output
6. **Build Android inference app** — Load TFLite, stream servo commands
7. **Integrate Kimi LLM** — Personality layer over behaviour arbiter

---

## 📝 How to Update This Index

When you add something new to the project:

1. **New document?** Add it to the "Where to Find Things" table above
2. **New decision?** Write an ADR in `docs/03-decisions/` and link it here
3. **Status change?** Update the status board tables
4. **New question?** Add to "Research Questions"
5. **Next action complete?** Move it to status board, update priority queue

> **Rule**: If it's not in this index, it doesn't exist.
