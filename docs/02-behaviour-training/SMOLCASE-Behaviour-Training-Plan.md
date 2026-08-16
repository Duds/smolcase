# SMOLCASE Physical Behaviour Suite — Complete Training Plan

> **Version:** 1.0  
> **Date:** 2026-08-06  
> **Platform:** 2-servo biped (Feetech SCS0009) + Pixel 8 brain  
> **Physics:** MuJoCo `growbot_current_body.xml`  
> **Training:** PPO via stable-baselines3  

---

## 1. Philosophy

SMOLCASE is not just a walking robot. It is a **physical character** — a creature with moods, intentions, and a body that expresses them. Every behaviour must serve the fiction: this is a small, curious, sometimes frightened, sometimes joyful being that lives in your phone and wants to explore the world.

**Design principle:** *The body is the punctuation. The face (phone screen) is the sentence.*

A forward walk is "I want to go there." A cower is "I'm scared." A bow is "I trust you." The phone's pixel face reinforces each behaviour with matching expressions, colours, and sounds.

---

## 2. Behaviour Taxonomy

### 2.1 Tier 1 — Core Locomotion (Train First)

These are the survival behaviours. Without them, the robot cannot function.

| # | Behaviour | Description | Training | Priority |
|---|-----------|-------------|----------|----------|
| L1 | **Walk Forward** | Steady alternating gait, positive x velocity | RL (PPO) | P0 |
| L2 | **Walk Backward** | Reverse gait, negative x velocity | RL (PPO) | P0 |
| L3 | **Turn Left** | Yaw rotation while walking or in place | RL (PPO) | P0 |
| L4 | **Turn Right** | Yaw rotation while walking or in place | RL (PPO) | P0 |
| L5 | **Stand Still** | Maintain upright posture, zero movement | RL (PPO) / Scripted | P0 |
| L6 | **Sit** | Lower body to ground, legs folded under | RL (PPO) | P1 |
| L7 | **Stand Up** | Transition from sitting/ground to standing | RL (PPO) | P1 |

### 2.2 Tier 2 — Balance Recovery (Critical for Autonomy)

These behaviours determine whether the robot is robust or fragile. They are the "reflex layer" that must work faster than the LLM can think.

| # | Behaviour | Description | Training | Priority |
|---|-----------|-------------|----------|----------|
| R1 | **Recover — Fallen Forward** | Face-down, legs behind. Push up and stand. | RL (PPO) | P0 |
| R2 | **Recover — Fallen Back** | Back-down, legs in air. Rock forward and stand. | RL (PPO) | P0 |
| R3 | **Recover — Fallen Left** | Side-tipped left. Right leg pushes, roll upright. | RL (PPO) | P1 |
| R4 | **Recover — Fallen Right** | Side-tipped right. Left leg pushes, roll upright. | RL (PPO) | P1 |
| R5 | **Balance Correction** | Real-time IMU feedback to prevent falling | CPG + RL | P0 |
| R6 | **Catch — Pushed** | Absorb external push, regain stance | RL (PPO) | P1 |
| R7 | **Stagger** | Brief loss of balance, exaggerated recovery step | RL (PPO) | P2 |

### 2.3 Tier 3 — Expressive / Emotional (The Soul)

These behaviours are what make SMOLCASE feel alive. They pair with the phone's pixel face, voice, and LED patterns.

| # | Behaviour | Description | Fiction Context | Training |
|---|-----------|-------------|-----------------|----------|
| E1 | **Bow** | Both legs extend, body dips forward, then recovers | "Hello, I trust you" | RL |
| E2 | **Cower** | Body low, legs tucked, minimal movement | "I'm scared / sorry" | RL |
| E3 | **Nod** | Small forward-back oscillation (agreement) | "Yes, I understand" | Scripted / CPG |
| E4 | **Shake Head** | Small left-right oscillation (disagreement) | "No, I don't like that" | Scripted / CPG |
| E5 | **Happy Dance** | Rapid small steps in place, slight bounce | "I'm excited!" | RL |
| E6 | **Sulk** | Slow, heavy steps, low body posture | "I'm upset / lonely" | RL |
| E7 | **Celebrate** | Small jump/spin combination | "I did it!" | RL |
| E8 | **Sleep** | Body fully lowered, legs relaxed, breathing motion | "I'm resting" | Scripted |
| E9 | **Wake Stretch** | Slow extension from sleep pose to standing | "Good morning" | Scripted |
| E10 | **Curious Lean** | Body tilts toward object/person of interest | "What is that?" | Scripted |
| E11 | **Startle** | Brief violent twitch, then freeze | "What was that?!" | CPG + scripted |
| E12 | **Shiver** | Rapid micro-oscillations | "I'm cold / scared" | CPG |
| E13 | **Attention Pose** | Body rises tall, still, alert | "I'm listening" | Scripted |

### 2.4 Tier 4 — Interactive / Social (LLM-Triggered)

These behaviours are invoked by the personality/LLM layer based on context, not trained as standalone policies. They compose Tier 1-3 primitives.

| # | Behaviour | Description | Trigger | Composition |
|---|-----------|-------------|---------|-------------|
| I1 | **Follow** | Walk toward detected face/voice | User calls its name | Walk Forward + Turn |
| I2 | **Flee** | Rapid backward walk away from threat | Loud noise / sudden motion | Walk Backward + Cower |
| I3 | **Circle** | Walk in a small circle around object/person | Curiosity about object | Walk Forward + Turn (alternating) |
| I4 | **Peek** | Lean body left/right to see around obstacle | Blocked view | Curious Lean + Stand Still |
| I5 | **Hide** | Back into dark corner, cower, stay still | Overwhelmed / frightened | Walk Backward + Cower + Sleep |
| I6 | **Greet** | Approach, bow, attention pose | User detected after absence | Walk Forward + Bow + Attention |
| I7 | **Guard** | Stand still, attention pose, track motion | Protective mode | Attention Pose + small Turns |
| I8 | **Play Chase** | Run (fast walk) in random pattern | Play mode activated | Walk Forward + rapid Turns |
| I9 | **Beg** | Small rapid nods while leaning forward | Wants something | Nod + Curious Lean |
| I10 | **Dismiss** | Turn away, slow walk, sulking posture | Rejected / bored | Turn + Sulk |

### 2.5 Tier 5 — Environmental / Utility

| # | Behaviour | Description | Training | Priority |
|---|-----------|-------------|----------|----------|
| U1 | **Climb Small Step** | Ascend ~10mm obstacle | RL + curriculum | P2 |
| U2 | **Descend Step** | Step down without falling | RL + curriculum | P2 |
| U3 | **Push Object** | Walk into light object, push forward | RL | P2 |
| U4 | **Avoid Edge** | Detect floor edge, back away | RL + vision | P2 |
| U5 | **Navigate Corridor** | Walk between walls without collision | RL + vision | P3 |
| U6 | **Dock / Home** | Return to charging station | RL + beacon | P3 |
| U7 | **Self-Right on Slope** | Stand and walk on inclined surface | RL + domain randomization | P2 |

---

## 3. Training Architecture

### 3.1 Policy Strategy: Hierarchical, Not Monolithic

A single neural network trying to learn all 30+ behaviours would be unstable. Instead, use a **hierarchical controller**:

```
┌─────────────────────────────────────────────────────────────────┐
│  HIGH-LEVEL: LLM / Behaviour Selector (Pixel 8)                  │
│  "I want to express joy" → select "Happy Dance" mode             │
├─────────────────────────────────────────────────────────────────┤
│  MID-LEVEL: Mode-Specific Policies (TFLite on Pixel 8)          │
│  Each behaviour has its own small neural network (~2K params)    │
│  Loaded/unloaded dynamically based on active mode                │
├─────────────────────────────────────────────────────────────────┤
│  LOW-LEVEL: Reflex Layer (always running)                        │
│  Balance correction, emergency stop, catch reflex                │
│  CPG-based, <1ms latency, hardcoded safety bounds               │
└─────────────────────────────────────────────────────────────────┘
```

**Why this works:**
- Each policy is small and focused — easier to train, more reliable
- The LLM chooses the "mode" (behaviour) — no need to embed personality in the physics
- Reflex layer is always active beneath everything — prevents damage
- Memory efficient: only 2-3 policies loaded at any time

### 3.2 Policy Bank

```
smolcase_policies/
├── core/
│   ├── walk_forward.tflite      # Trained ✓ (exists)
│   ├── walk_backward.tflite     # To train
│   ├── turn_left.tflite         # To train
│   ├── turn_right.tflite        # To train
│   ├── stand_still.tflite       # To train
│   ├── sit.tflite               # To train
│   └── stand_up.tflite          # To train
├── recovery/
│   ├── recover_front.tflite     # To train
│   ├── recover_back.tflite      # To train
│   ├── recover_left.tflite      # To train
│   └── recover_right.tflite     # To train
├── expressive/
│   ├── bow.tflite               # To train
│   ├── cower.tflite             # To train
│   ├── happy_dance.tflite       # To train
│   ├── sulk.tflite              # To train
│   └── celebrate.tflite         # To train
└── reflex/
    └── balance_corrector.py     # CPG, always active
```

### 3.3 Shared Observation Space (All Policies)

```python
OBS_DIM = 13  # same for every policy
obs = [
    pitch, roll,               # [0:2]  body orientation (IMU)
    gyro_x, gyro_y,            # [2:4]  angular velocity
    joint_left_pos,            # [4]    left leg angle
    joint_right_pos,           # [5]    right leg angle
    joint_left_vel,            # [6]    left leg velocity
    joint_right_vel,           # [7]    right leg velocity
    body_height,               # [8]    z-position
    last_action_left,          # [9]    previous servo target
    last_action_right,         # [10]   previous servo target
    foot_contact_left,         # [11]   ground contact flag
    foot_contact_right,        # [12]   ground contact flag
]
```

*Note: For recovery policies, add initial condition flags (fallen_front, fallen_back, etc.) as extra obs dimensions.*

### 3.4 Shared Action Space (All Policies)

```python
ACTION_DIM = 2
action = [
    left_servo_target,   # [-1, 1] → mapped to ±1.57 rad
    right_servo_target,  # [-1, 1] → mapped to ±1.57 rad
]
```

---

## 4. Per-Behaviour Training Specs

### 4.1 Core Locomotion

#### L1: Walk Forward (✅ TRAINED — baseline exists)
```python
# Reward function (current)
reward = (
    2.0 * forward_vel          # Main objective: go forward
    - 0.5 * upright_penalty    # Stay upright
    - 0.05 * energy            # Don't waste energy
    - 0.01 * action_jitter     # Smooth motion
    + 0.1 * contact_bonus      # Grounded steps
    + 1.0                      # Alive bonus per step
)
```
- **Training time:** 500K–1M steps
- **Success metric:** 10+ meters in 50s without falling

#### L2: Walk Backward
```python
reward = (
    2.0 * abs(backward_vel)    # Main: negative x velocity
    - 0.5 * upright_penalty
    - 0.05 * energy
    - 0.01 * action_jitter
    + 1.0                      # Alive bonus
)
```
- **Trick:** Mirror the forward policy's reward but reward negative velocity
- **Can also:** Fine-tune the forward policy with reversed velocity reward

#### L3 / L4: Turn Left / Turn Right
```python
reward = (
    1.0 * abs(yaw_rate)        # Main: rotate around z-axis
    + 0.5 * forward_vel        # Slight forward motion OK
    - 0.5 * upright_penalty
    - 0.05 * energy
    + 1.0                      # Alive bonus
)
```
- **Curriculum:** Start with in-place spin (zero forward vel), then add walking turn

#### L5: Stand Still
```python
reward = (
    -2.0 * abs(pitch)          # Stay perfectly upright
    - 2.0 * abs(roll)
    - 1.0 * abs(gyro_x)        # No angular velocity
    - 1.0 * abs(gyro_y)
    - 0.5 * abs(joint_vel_left)   # No joint motion
    - 0.5 * abs(joint_vel_right)
    - 0.01 * action_jitter
    + 2.0                      # Alive bonus (higher — this is harder than it looks)
)
```
- **Surprisingly hard:** 2-servo bipeds are inherently unstable. Standing still requires active micro-corrections.
- **Approach:** Train as "maintain pose with minimal energy" rather than "do nothing"

#### L6: Sit
```python
reward = (
    -1.0 * body_height         # Lower is better (target: ~0.02m)
    - 0.5 * upright_penalty    # But don't fall over
    - 0.5 * abs(joint_vel)     # Slow, controlled descent
    + 5.0 * reached_target     # Big bonus when height < 0.03m and stable
)
```
- **Terminal condition:** height < 0.03m and |pitch| < 0.3 for 50 steps
- **Safety:** Kill episode if robot falls on side (recovery mode should trigger instead)

#### L7: Stand Up
```python
reward = (
    2.0 * body_height          # Rise up
    - 1.0 * abs(pitch)         # Become upright
    - 0.5 * energy             # But don't flail
    + 10.0 * standing_bonus    # Huge bonus when upright and stable
)
```
- **Initial state:** Randomize from sitting pose, fallen front pose, fallen back pose
- **This is the most important recovery skill**

---

### 4.2 Recovery Behaviours

Recovery policies are trained with **fixed initial conditions** — the robot always starts in the fallen pose.

#### R1: Recover — Fallen Forward
```python
# Initial state: body face-down, pitch ≈ -1.5 rad, height ≈ 0.02m
reward = (
    3.0 * body_height          # Get up
    - 2.0 * abs(pitch)         # Become upright
    - 1.0 * abs(roll)
    - 0.5 * energy
    + 20.0 * standing_bonus    # Massive bonus for reaching standing
)
# Episode terminates when standing (height > 0.05, |pitch| < 0.2)
# OR when max_steps (500) reached
```

#### R2: Recover — Fallen Back
```python
# Initial state: body back-down, pitch ≈ +1.5 rad, legs in air
reward = (
    3.0 * body_height
    - 2.0 * abs(pitch)
    - 1.0 * abs(roll)
    + 20.0 * standing_bonus
)
# Key challenge: legs start in air, need to swing under body
```

#### R3 / R4: Recover — Fallen Left / Right
```python
# Initial state: side-tipped, roll ≈ ±1.5 rad
reward = (
    3.0 * body_height
    - 2.0 * abs(roll)          # Fix roll first
    - 1.0 * abs(pitch)
    + 20.0 * standing_bonus
)
# The lower leg pushes against ground, upper leg swings over
```

#### R5: Balance Correction (Reflex Layer)
```python
# NOT trained with RL — this is a real-time CPG feedback controller
# Runs at 100Hz, <1ms latency

def balance_reflex(obs, current_action):
    pitch, roll = obs[0], obs[1]
    gyro_x, gyro_y = obs[2], obs[3]

    correction = np.zeros(2)

    # Pitch correction (forward/backward tilt)
    if pitch > 0.2:       # Leaning forward → extend both legs
        correction += np.array([0.1, 0.1])
    elif pitch < -0.2:    # Leaning backward → retract both legs
        correction += np.array([-0.1, -0.1])

    # Roll correction (side tilt)
    if roll > 0.15:       # Tipping left → push with right leg
        correction += np.array([0.0, 0.15])
    elif roll < -0.15:    # Tipping right → push with left leg
        correction += np.array([0.15, 0.0])

    # Gyro damping (reduce oscillation)
    correction -= 0.05 * np.array([gyro_x, gyro_y])

    return np.clip(current_action + correction, -1.0, 1.0)
```

#### R6: Catch — Pushed
```python
# Initial state: upright, then apply random impulse to body
# Train with domain randomization: impulse magnitude 0.5–5.0 N·s, random direction

reward = (
    5.0 * standing_bonus       # Stay standing!
    - 2.0 * abs(pitch)         # Minimize tilt
    - 2.0 * abs(roll)
    - 0.5 * energy
)
# Success: absorb push and return to standing within 100 steps
# Failure: fall → episode terminates
```

---

### 4.3 Expressive Behaviours

These are shorter-duration, goal-conditioned policies. Episode length: 100–300 steps (1–3 seconds).

#### E1: Bow
```python
# Motion: extend both legs → body dips forward → recover to upright
# Like a deep nod

reward = (
    # Phase 1: Dip forward (first 50 steps)
    2.0 * forward_pitch if step < 50 else 0.0
    # Phase 2: Return to upright (steps 50–100)
    + 3.0 * upright_bonus if step >= 50 else 0.0
    - 0.5 * energy
    + 10.0 * completed_bonus   # Smooth bow + return
)
# Terminal: upright and stable after step 100
```

#### E2: Cower
```python
# Motion: body low, legs tucked, minimal movement
# Like a defensive curl

reward = (
    2.0 * low_bonus            # height < 0.03m
    - 1.0 * abs(pitch)         # But stable (not fallen)
    - 1.0 * abs(roll)
    - 2.0 * motion_penalty     # Very still
    + 5.0 * stable_low_bonus   # Held pose for 50+ steps
)
```

#### E3 / E4: Nod / Shake Head
```python
# These are CPG-based, not RL-trained
# Simple oscillations superimposed on standing pose

def nod(t, amplitude=0.15, freq=2.0):
    """Forward-back oscillation"""
    offset = amplitude * np.sin(2 * np.pi * freq * t)
    return np.array([offset, offset])  # Both legs move together

def shake(t, amplitude=0.1, freq=3.0):
    """Left-right oscillation"""
    offset = amplitude * np.sin(2 * np.pi * freq * t)
    return np.array([offset, -offset])  # Legs move opposite
```

#### E5: Happy Dance
```python
# Rapid small steps in place with bounce

reward = (
    1.0 * rapid_motion         # High joint velocity, but small amplitude
    - 2.0 * abs(forward_vel)   # Stay in place (no drift)
    - 0.5 * upright_penalty    # Stay upright
    + 0.5 * rhythmic_bonus     # Reward periodic motion (use FFT on joint pos)
)
```

#### E6: Sulk
```python
# Slow, heavy steps, low posture

reward = (
    0.5 * slow_forward_vel     # Slow movement OK
    - 1.0 * high_body          # Penalize tall posture
    + 1.0 * low_stable         # Reward low but stable
    - 2.0 * energy             # Heavy = high energy, but penalize it
)
# The contradiction in the reward creates the "sulking" quality
```

#### E7: Celebrate
```python
# Brief explosive motion: small jump + spin

reward = (
    3.0 * peak_height          # Jump high (momentary)
    + 2.0 * peak_yaw_rate      # Spin fast
    - 1.0 * fall_penalty       # But don't fall
    + 10.0 * landed_stable     # Must land and stabilize
)
# Episode: 150 steps. First 50 = jump/spin. Last 100 = recover to standing.
```

#### E8: Sleep
```python
# Fully lowered, legs relaxed, slow breathing motion

# Scripted, not RL:
def sleep_pose(t):
    breathing = 0.02 * np.sin(0.5 * t)  # Very slow, small oscillation
    return np.array([breathing, breathing])
# Body is in sit pose + micro-breathing
```

#### E9: Wake Stretch
```python
# Slow extension from sleep → stand

reward = (
    2.0 * body_height          # Rise up
    - 0.5 * energy             # But slowly (low energy = good)
    + 10.0 * standing_bonus    # Reach standing
)
# Initial state: sleep pose. Terminal: standing stable.
```

#### E10: Curious Lean
```python
# Body tilts toward object/person

# Scripted: given a direction vector from camera/vision:
def curious_lean(direction):
    """direction: -1 = left, 0 = center, 1 = right"""
    if direction < 0:
        return np.array([-0.2, 0.0])   # Left leg back, body leans left
    elif direction > 0:
        return np.array([0.0, -0.2])   # Right leg back, body leans right
    else:
        return np.array([0.0, 0.0])
```

#### E11: Startle
```python
# Brief violent twitch, then freeze

# CPG-based reflex:
def startle():
    # Single-frame random large action
    return np.random.uniform(-0.8, 0.8, size=2)
# Then immediately switch to Stand Still mode for 1 second
```

#### E12: Shiver
```python
# Rapid micro-oscillations

# CPG-based:
def shiver(t, amplitude=0.05, freq=8.0):
    return np.array([
        amplitude * np.sin(2 * np.pi * freq * t),
        amplitude * np.sin(2 * np.pi * freq * t + np.pi/4)
    ])
```

---

## 5. Training Curriculum

### Phase 0: Stand Still (Week 1)
- **Why first:** If the robot can't stand, nothing else works
- **Approach:** Start with a rigid body (no freejoint), learn joint holding. Then add freejoint.
- **Reward:** Heavy upright penalty, energy minimization
- **Success:** Stand for 10s without falling

### Phase 1: Walk Forward (Week 1–2)
- **Use existing policy as starting point**
- **Curriculum:**
  1. Flat floor, no randomization → learn basic gait
  2. Add small pitch/roll initial noise → robustness
  3. Add floor friction randomization (0.8–1.2×) → sim-to-real
  4. Add slight push disturbances → stability
- **Success:** Walk 10m on flat ground

### Phase 2: Walk Backward + Turns (Week 2–3)
- **Fine-tune** forward policy with modified reward
- **Or:** Train from scratch with velocity-direction reward
- **Curriculum:** Combine with forward (walk 3m forward, turn 90°, walk 3m backward)

### Phase 3: Recovery (Week 3–4)
- **Train each fallen pose separately**
- **Initial condition randomization:** vary exact fallen angle by ±0.3 rad
- **Curriculum:** Start with easy poses (slight lean), progress to full face-plant
- **Critical:** Recovery must succeed within 3 seconds (300 steps)

### Phase 4: Expressive Behaviours (Week 4–6)
- **Bow, Cower, Celebrate, Happy Dance**
- **Each is a short-horizon RL problem** (100–300 steps)
- **Can use Behaviour Cloning:** Record human-designed trajectories, fine-tune with RL
- **Success metric:** Behaviour looks intentional, not accidental

### Phase 5: Composition + Integration (Week 6–8)
- **Train a "mode switcher" policy** that transitions between behaviours smoothly
- **Smooth transitions:** No jerky mode switches. Blend policy outputs over 20–50 steps
- **LLM integration:** High-level behaviour selection from the personality layer

---

## 6. Reward Engineering Cheat Sheet

| Objective | Reward Component | Typical Weight |
|-----------|------------------|----------------|
| Go forward | `forward_vel` | +1.0 to +3.0 |
| Go backward | `abs(backward_vel)` | +1.0 to +3.0 |
| Turn | `abs(yaw_rate)` | +1.0 to +2.0 |
| Stay upright | `-abs(pitch) - abs(roll)` | -0.5 to -2.0 |
| Save energy | `-joint_vel²` | -0.01 to -0.1 |
| Smooth motion | `-action_jitter` | -0.001 to -0.01 |
| Ground contact | `+0.1 if foot down` | +0.1 |
| Alive bonus | `+1.0 per step` | +1.0 |
| Reach pose | `+10.0 to +20.0` | One-time |
| Stay low | `-height` | -1.0 to -2.0 |
| Stay high | `+height` | +1.0 to +3.0 |
| Be still | `-abs(joint_vel)` | -0.5 to -2.0 |
| Be rhythmic | FFT peak at target freq | +0.5 |

---

## 7. Sim-to-Real Transfer Strategy

### Domain Randomization (Apply During Training)

```python
# Randomize each episode:
randomizations = {
    "floor_friction": np.random.uniform(0.6, 1.4),
    "servo_delay": np.random.uniform(0, 2),        # frames of delay
    "servo_noise": np.random.uniform(0, 0.05),     # rad of position noise
    "body_mass": np.random.uniform(0.08, 0.12),    # kg
    "leg_mass": np.random.uniform(0.008, 0.015),   # kg
    "gravity": np.random.uniform(9.0, 10.0),       # m/s²
    "initial_pitch": np.random.uniform(-0.1, 0.1), # rad
    "initial_roll": np.random.uniform(-0.1, 0.1),  # rad
}
```

### Pixel 8 Deployment

```kotlin
// Android (Kotlin) — TFLite inference
data class Observation(
    val pitch: Float, val roll: Float,
    val gyroX: Float, val gyroY: Float,
    val jointL: Float, val jointR: Float,
    val velL: Float, val velR: Float,
    val height: Float,
    val lastL: Float, val lastR: Float,
    val contactL: Float, val contactR: Float
)

class GaitController(private val tflitePath: String) {
    private val interpreter: Interpreter
    private val inputBuffer = Array(1) { FloatArray(13) }
    private val outputBuffer = Array(1) { FloatArray(2) }

    fun inference(obs: Observation): Pair<Float, Float> {
        inputBuffer[0] = floatArrayOf(
            obs.pitch, obs.roll,
            obs.gyroX, obs.gyroY,
            obs.jointL, obs.jointR,
            obs.velL, obs.velR,
            obs.height,
            obs.lastL, obs.lastR,
            obs.contactL, obs.contactR
        )
        interpreter.run(inputBuffer, outputBuffer)
        return Pair(outputBuffer[0][0], outputBuffer[0][1])
    }
}
```

---

## 8. Testing Checklist

Before deploying each policy to the physical robot:

- [ ] Policy walks 10m in simulation without falling
- [ ] Policy recovers from 10 random push disturbances
- [ ] Policy transitions smoothly from previous behaviour
- [ ] TFLite inference runs < 5ms on Pixel 8
- [ ] Servo targets are within safe bounds (±1.57 rad)
- [ ] Policy does not command extreme velocities (max 3 rad/s)
- [ ] Policy is robust to 100ms communication delay
- [ ] Emergency stop (reflex layer) can override policy at any time

---

## 9. Summary: Behaviour Roadmap

```
WEEK 1:  Stand Still → Walk Forward (baseline)
WEEK 2:  Walk Backward → Turn Left/Right
WEEK 3:  Recover Front → Recover Back → Recover Side
WEEK 4:  Sit → Stand Up → Catch (pushed)
WEEK 5:  Bow → Cower → Attention Pose
WEEK 6:  Happy Dance → Sulk → Celebrate
WEEK 7:  Mode Switching → Smooth Transitions
WEEK 8:  Sim-to-Real → Physical Testing → Iterate
```

**Total trained policies:** ~20 TFLite models, each < 50KB  
**Total training time:** ~8 weeks (parallel training on workstation)  
**Deployed at any time:** 1 active policy + reflex layer + LLM personality  

---

*Document version 1.0. Iterate as behaviours are trained and tested.*
