# ADR-003: CPG vs MuJoCo vs TFLite — Three-Layer Control Stack

## Status
Accepted — 2026-08-06

## Context
Three distinct approaches exist for legged locomotion control. We needed to understand each and decide how they fit together in SMOLCASE.

## The Three Approaches

### 1. CPG (Central Pattern Generator)

**What it is**: Biologically-inspired rhythmic oscillators that generate periodic motor patterns without a central brain. Think of how a decapitated insect can still walk — the rhythm is distributed.

**How it works in SMOLCASE**:
- Coupled Hopf oscillators produce sinusoidal reference trajectories for each joint
- Phase coupling ensures leg coordination (legs in phase to hop, anti-phase to alternate)
- Parameters: frequency, amplitude, phase offsets, coupling weights
- Runs on the Pixel 8 in real-time (or ESP32 for lower latency)

**Pros**:
- Extremely fast and stable — no inference needed
- Naturally smooth, rhythmic gaits
- Robust to perturbations (the oscillator self-corrects)
- Low compute (can run on ESP32)

**Cons**:
- Fixed patterns — can't learn new behaviours
- Difficult to tune for non-periodic behaviours (recovering from falls, turning)
- Emerges from biology but doesn't exploit modern ML

**Where it fits**: **Reflex layer** — provides a safe rhythmic baseline that keeps the robot moving even if the ML policy fails. Also used for emergency behaviours (e.g., "shake off" reflex).

---

### 2. MuJoCo (Physics Simulation + RL)

**What it is**: A high-fidelity physics simulator used to train reinforcement learning policies in simulation before transferring to the real robot.

**How it works in SMOLCASE**:
- Robot body modeled in XML with joints, mass, inertia, collision geometry
- RL agent (PPO) explores actions in simulation, rewarded for forward velocity, stability, energy efficiency
- Policy learns to map sensor observations → joint torques/angles
- Trained policies are exported to TFLite for on-device inference

**Pros**:
- Can train dangerous behaviours safely (falling, tumbling) in sim
- Millions of training steps in hours (not days on real hardware)
- Exact repeatability — same initial conditions for debugging
- Can use domain randomization to improve sim-to-real transfer

**Cons**:
- Sim-to-real gap — physics approximations mean policies may fail on real robot
- Requires accurate URDF/MJCF model of the robot
- No real-world sensor noise, friction variation, etc. unless explicitly modeled
- Computationally expensive training (GPU recommended)

**Where it fits**: **Training environment** — all policies are trained here first. MuJoCo is the gym, not the runtime.

---

### 3. TFLite (TensorFlow Lite On-Device Inference)

**What it is**: Google's lightweight ML inference engine optimized for mobile and edge devices. Runs trained neural networks on the Pixel 8.

**How it works in SMOLCASE**:
- Trained policies (from MuJoCo + PPO) are converted to `.tflite` format
- TFLite interpreter loads the model, runs inference in ~1-5ms on Pixel 8 GPU/NPU
- Input: IMU readings + joint angles → Output: target joint angles
- One model active at a time, selected by the behaviour arbiter

**Pros**:
- Runs on Pixel 8 — no cloud latency for movement decisions
- Fast inference (GPU delegate on Tensor G3)
- Small model size (~50-150KB per policy)
- Quantization can reduce size further with minimal accuracy loss

**Cons**:
- Still slower than CPG (inference vs. oscillator math)
- Requires training pipeline (MuJoCo → stable-baselines3 → ONNX → TFLite)
- Quantization can degrade performance for precise control

**Where it fits**: **Runtime policy layer** — the "brain" that decides how to move, sitting above CPG.

---

## How They Stack

```
┌──────────────────────────────────────┐
│  High-Level Intent (Kimi LLM)        │
│  "Walk forward and greet the human"  │
└──────────────┬───────────────────────┘
               │
┌──────────────▼───────────────────────┐
│  Behaviour Arbiter                   │
│  Selects: WalkForward + Bow TFLite   │
└──────────────┬───────────────────────┘
               │
┌──────────────▼───────────────────────┐
│  TFLite Policy (active model)        │
│  Observations → Joint targets        │
└──────────────┬───────────────────────┘
               │
┌──────────────▼───────────────────────┐
│  CPG Reflex Layer                    │
│  Adds rhythmic baseline, blends      │
│  Emergency reflexes override         │
└──────────────┬───────────────────────┘
               │ BLE
┌──────────────▼───────────────────────┐
│  ESP32 → PWM → Servos                │
└──────────────────────────────────────┘
```

## Related
- ADR-002: Hierarchical Policy Architecture
- `sim/src/smolcase_train.py`
