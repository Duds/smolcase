# SMOLCASE

**S**mall **M**obile **O**perating **L**ogic / **C**ybernetic **A**utomated **S**ervo **E**nsemble

> A phone-brain biped robot. Inspired by [GrowBot](https://growbot.dev), built with a Pixel 8, MuJoCo simulation, and ~20 specialist TFLite policies.

---

## What is SMOLCASE?

SMOLCASE is a small biped robot whose brain is a **Google Pixel 8**. The phone's screen is its face. Its sensors (IMU, cameras, microphone) are its senses. An **ESP32** bridges BLE commands to hobby servos. A suite of trained neural policies, running via TensorFlow Lite, controls its gait.

Unlike monolithic robot platforms, SMOLCASE is **hierarchical and modular**:
- **Kimi LLM** provides personality and high-level intent
- **Behaviour Arbiter** selects the active movement policy
- **TFLite models** (~20 specialists) handle specific behaviours
- **CPG reflex layer** provides safe rhythmic baseline and emergency reflexes
- **ESP32 bridge** executes servo commands in real time

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  PIXEL 8 — Brain, Sensors, Face                         │
│  ┌─────────────┐  ┌──────────────────────────────────┐  │
│  │  Kimi LLM   │  │  Behaviour Arbiter               │  │
│  │  (persona)  │  │  (selects active TFLite model)   │  │
│  └──────┬──────┘  └──────────────┬───────────────────┘  │
│         │                        │                       │
│  ┌──────┴────────────────────────┴──────┐                │
│  │  Active TFLite Policy                 │                │
│  │  (walk, turn, recover, bow, etc.)     │                │
│  └──────────────┬────────────────────────┘                │
│                 │                                       │
│  ┌──────────────┴────────────────────────┐                │
│  │  CPG Reflex Layer                     │                │
│  │  (rhythmic baseline + safety reflex)  │                │
│  └──────────────┬────────────────────────┘                │
│                 │ BLE 5.0                               │
└─────────────────┼───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│  ESP32 — Dumb Command Executor                          │
│  BLE → UART → 2x 360° Serial Bus Servos                │
└─────────────────────────────────────────────────────────┘
```

---

## Project Status

| Subsystem | Status | Next Action |
|-----------|--------|-------------|
| **Simulation** | 🟢 Active | MuJoCo env running, Walk Forward trained |
| **ML Training** | 🟡 In Progress | 1/30 behaviours trained |
| **Mechanical Design** | ⚪ Not Started | Awaiting servo selection → Fusion 360 |
| **Firmware** | ⚪ Not Started | Awaiting ESP32 + servo specs |
| **Android App** | ⚪ Not Started | After simulation curriculum complete |
| **Kimi Integration** | ⚪ Not Started | Future personality layer |

---

## Quick Start — Training

```bash
# 1. Install dependencies
cd sim/
pip install -r requirements.txt

# 2. Verify MuJoCo loads the body
python src/test_xml_load.py

# 3. Train Walk Forward (takes ~30 min on CPU)
python src/smolcase_train.py --mode train \
  --policy-name smolcase_gait \
  --timesteps 500000

# 4. Evaluate
python src/smolcase_train.py --mode eval \
  --model models/smolcase_gait.zip \
  --episodes 5

# 5. Export to TFLite (for Pixel 8)
python src/smolcase_train.py --mode export \
  --model models/smolcase_gait.zip
```

View training progress:
```bash
tensorboard --logdir logs/
```

---

## Directory Map

```
SMOLCASE/
├── README.md                  ← You are here
├── PROJECT_INDEX.md           ← Living project index & status board
├── docs/
│   ├── 01-research/           ← GrowBot reverse-engineering, sources
│   ├── 02-behaviour-training/ ← 30-behaviour training curriculum
│   ├── 03-decisions/          ← Architecture Decision Records (ADRs)
│   └── 04-hardware/           ← BOM, servo specs, power budget
├── sim/
│   ├── assets/                ← MuJoCo body XML (GrowBot + future SMOLCASE)
│   ├── src/                   ← Training pipeline, environment, tests
│   └── requirements.txt       ← Python dependencies
├── models/
│   ├── checkpoints/           ← Periodic training saves
│   ├── exports/               ← Final .tflite files for Pixel 8
│   └── README.md              ← Model manifest
├── firmware/                  ← ESP32 BLE bridge code
├── android/                   ← Pixel 8 companion app
├── mech/                      ← Fusion 360 CAD, STL files
└── logs/                      ← TensorBoard events
```

---

## Key Decisions

| Decision | Rationale | Document |
|----------|-----------|----------|
| Pixel 8 instead of Pi Zero | Full sensor suite, screen as face, Kimi integration | [ADR-001](docs/03-decisions/001-pixel8-brain.md) |
| Hierarchical policies | Modular, interpretable, failure-isolated | [ADR-002](docs/03-decisions/002-hierarchical-policies.md) |
| CPG + MuJoCo + TFLite stack | Physics sim for training, TFLite for runtime, CPG for safety | [ADR-003](docs/03-decisions/003-cpg-mujoco-tflite.md) |
| ESP32 BLE bridge | Wireless, cheap, proven phone-robot pattern | [ADR-004](docs/03-decisions/004-esp32-ble-bridge.md) |

---

## Behaviour Training Curriculum

30 behaviours across 5 tiers, from basic locomotion to expressive personality:

| Tier | Behaviours |
|------|-----------|
| **1 — Locomotion** | Walk Forward, Walk Backward, Turn Left, Turn Right |
| **2 — Stability** | Stand, Sit, Recover Fall (back), Recover Fall (front) |
| **3 — Agility** | Trot, Bound, Side-step, Pivot |
| **4 — Expressive** | Bow, Greet, Cower, Shake, Head Tilt |
| **5 — Advanced** | Climb Slope, Navigate Obstacle, Push Object, Follow |

Full curriculum with reward functions and hyperparameters: [`docs/02-behaviour-training/`](docs/02-behaviour-training/)

---

## Research & Sources

Deep reverse-engineering of GrowBot hardware, software, phone-creature architecture, and monetization analysis:

- [`docs/01-research/SMOLCASE-GrowBot-Reverse-Engineering.md`](docs/01-research/SMOLCASE-GrowBot-Reverse-Engineering.md)

Key external references:
- [growbot.dev](https://growbot.dev) — GrowBot website (retired)
- [britcruise9/GrowBot](https://github.com/britcruise9/GrowBot) — Source repo
- [Art of the Problem — GrowBot Preview](https://artoftheproblem.com/pages/growbot-preview)
- [YouTube: GrowBot Preview](https://youtu.be/S67z2aekBrI)
- [YouTube: GrowBot Deep Dive](https://youtu.be/mIfmUHiMN3U)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Simulation | MuJoCo 3.11.0, Gymnasium 1.3.0 |
| RL Training | Stable-Baselines3 (PPO) |
| Model Export | TensorFlow → TFLite |
| On-Device Runtime | TensorFlow Lite (Pixel 8 GPU delegate) |
| Phone OS | Android 15 (Pixel 8) |
| Servo Bridge | ESP32 (Arduino core) |
| Communication | BLE 5.0 |
| CAD | Autodesk Fusion 360 |

---

## License

TBD — This is a personal research and robotics project.

## Acknowledgements

- [GrowBot](https://growbot.dev) by britcruise9 — the original phone-brain robot that inspired this project
- [MuJoCo](https://mujoco.org/) by DeepMind — physics simulation
- [Stable-Baselines3](https://stable-baselines3.readthedocs.io/) — RL training framework
