# SMOLCASE Deep Research: Reverse-Engineering the GrowBot Project

> **Date:** 2026-08-06
> **Subject:** Comprehensive reverse-engineering of britcruise9's GrowBot ecosystem for the SMOLCASE (Small Mobile Operating Logic / Cybernetic Automated Servo Ensemble) concept.
> **Sources:** GitHub repos, growbot.dev front-end source, artoftheproblem.com, YouTube videos, web research.

---

## 1. Executive Summary

GrowBot is a **dual-system AI robot ecosystem** created by Brit Cruise (Art of the Problem). It consists of:

1. **A phone-based AI creature** (growbot.dev) — a sentient-seeming character that lives in any smartphone browser, using the phone's native sensors (camera, microphone, accelerometer, touch) to perceive the world. It has persistent memory, emotional state, and grows through experience.

2. **A $77 physical robot body** (GitHub: britcruise9/GrowBot) — a Raspberry Pi Zero 2 W-based biped with 2 servos, camera, IMU, microphone, speaker, and LED ring. The phone creature can "move into" this body.

The core thesis: **"What's the simplest possible AI robot that learns everything from scratch?"** The project collides pure machine learning (evolutionary algorithms → neural networks) with modern foundation models (LLMs given a nervous and motor system).

**Status:** V0 hardware/software is a research snapshot. V1 (Fall 2026) promises step-by-step build guides, custom PCB, calibrated digital twin, and a "top secret" feature. The phone app is actively iterated (current: v2-0721 build).

---

## 2. Physical Robot — Hardware Reverse-Engineering

### 2.1 Bill of Materials (~$77 CAD / ~$56 USD)

| Category | Part | Spec | Qty | ~Price |
|----------|------|------|----:|-------:|
| **Brain** | Raspberry Pi Zero 2 W | Quad-core A53, WiFi/BT, 40-pin | 1 | $22 |
| | microSD card | 16-32GB, Class 10 / A1 | 1 | $8 |
| | 2x20 GPIO header | 0.1" male | 1 | $1 |
| **Legs** | Feetech SCS0009 servo | Half-duplex UART 1Mbps, 9g, 1.5kg·cm, 300° | 2 | $5-6 ea |
| | 1kΩ resistor | ¼W | 1 | pennies |
| **Senses** | MPU-6050 IMU (GY-521) | I²C 0x68, 6-axis, 3.3V | 1 | $3 |
| | OV5647 camera (Pi Zero) | 5MP, narrow 22→15-pin CSI ribbon | 1 | $12 |
| | INMP441 I²S mic | 3.3V, 24-bit | 1 | $4 |
| **Voice/Light** | MAX98357A I²S amp | 3.2W class D mono | 1 | $5 |
| | Speaker | 8Ω, 0.5-3W, small | 1 | $2 |
| | WS2812B LED ring | 7 pixels, 5V | 1 | $3 |
| **Power** | 1S LiPo / 18650 | 3.7V, 800-1200mAh | 1 | $4 |
| | TP4056 module | USB-C, charge + protect | 1 | $1 |
| | MT3608 boost | Step-up to ~5.1V | 1 | pennies |
| | Electrolytic capacitor | 470-1000µF, 10V+ | 1 | pennies |
| | SPST power switch | — | 1 | pennies |

**Key insight:** Everything runs on the Pi. No external computer. The design intentionally shares a single 5V rail between Pi and servos (protected by a capacitor) — described as "hacky" but functional for V0.

### 2.2 GPIO Pin Map (Pi Zero 2 W)

| Pi Pin | Signal | Destination |
|-------:|--------|-------------|
| 2 / 4 | 5V | 5V rail (MT3608 → Pi, servos, amp, LED) |
| 1 | 3.3V | IMU VCC |
| 17 | 3.3V | Mic VDD |
| 6 / 9 / 14 / 25 | GND | Common ground |
| 3 | GPIO2 / SDA | IMU SDA |
| 5 | GPIO3 / SCL | IMU SCL |
| 8 | GPIO14 / TXD | 1kΩ → servo DATA bus |
| 10 | GPIO15 / RXD | Servo DATA bus (direct) |
| 12 | GPIO18 | Amp BCLK + mic SCK (shared) |
| 35 | GPIO19 | Amp LRC + mic WS (shared) |
| 38 | GPIO20 | Mic SD (data in) |
| 40 | GPIO21 | Amp DIN (data out) |
| 32 | GPIO12 | WS2812 DIN |
| CSI | Ribbon | Camera (not GPIO) |

### 2.3 Servo Protocol (Critical Discovery)

The **SCS0009 uses BIG-ENDIAN for ALL 2-byte reads and writes**. This is the #1 trap — Dynamixel-style little-endian code appears to work but silently writes garbage that clips to max angle. The driver in `servos.py` handles this correctly.

- Packet format: `FF FF <id> <len> <inst> <params...> <checksum>`
- Bus: half-duplex UART at 1 Mbps
- Encoder range: 0-1023 across ~300° (usable floor: 255)
- `/dev/serial0` (PL011 UART), NOT `/dev/ttyS0`

### 2.4 Power Budget

| Component | Typical | Peak |
|-----------|--------:|-----:|
| Pi Zero 2 W | 200mA | 400mA |
| 2x SCS0009 (walking) | 400mA | 1700mA (both stalling) |
| Audio + LEDs | ~100mA | ~400mA |
| **Total** | **~700mA** | **~2.1A** |

Battery life: ~50-60 minutes active play on 800-1200mAh cell.

### 2.5 Mechanical Design

- 3D-printed body: holey top shell, matching base, short rounded legs
- STLs available in `mechanical/stl_snapshot/`
- Body dimensions: ~132 × 83 × 39 mm
- Leg length: ~64 mm
- Rounded/rockered foot design for stable walking

### 2.6 Simulation (MuJoCo)

```xml
<!-- Key specs from growbot_current_body.xml -->
- Body box: 66 × 41.5 × 19.5 mm, mass 70g
- Servo boxes: 12 × 6 × 12 mm, mass 9g each
- Leg boxes: 10 × 6.75 × 26 mm, mass 12g each
- Foot: segmented rounded shape (15 segments per foot)
- IMU site: 1mm back from center, at bottom shell
- Actuators: position control, kp=0.85
```

The simulation is used to train locomotion policies offline, then deploy compact versions to the Pi.

---

## 3. Phone Creature — Software Architecture (Reverse-Engineered from growbot.dev)

### 3.1 Overview

The phone creature is a **single-page web app** that runs entirely in the browser. It uses:
- **Canvas-based pixel-art face** for visual expression
- **Web APIs** for sensors (camera, mic, accelerometer, DeviceOrientation)
- **localStorage** for persistent memory
- **OpenRouter/Claude API** for AI cognition (via a proxy)
- **Multiple TTS engines** for voice

**Critical design choice:** Old phones ARE the product's target hardware. The CSS explicitly avoids modern features like `inset:` (Safari 14.1+) to support iPhone 6 class devices.

### 3.2 The Constitution (Prompt Engineering Masterclass)

The creature's behavior is governed by a detailed "Constitution" prompt sent with every LLM call. Key elements:

**Identity:**
- Imprints on ONE person (whoever first wakes and holds it)
- Curious, playful, emotionally authentic (feels delight, frustration, sadness, boredom)
- Wants to GROW — to feel and do more of the world
- Never fakes cheer, never performs sadness

**Sensory Integration:**
- Receives raw sensor readings (acceleration, gyro, brightness, sound pitch)
- Must turn magnitudes into feeling words ("a firm scoop-up push") — NEVER raw numbers
- Camera images are "what it truly sees right now" — describe only what's really there

**Behavioral Rails:**
- Speak only when the moment earns it; observe first
- Short, first-person feeling, ~14 words max
- Never mention being AI/model/code (unless explicitly invited)
- When neglected: genuinely quiet and withdrawn, but NEVER begs or pleads
- When insulted: honest reaction, never apologizes for existing
- When user is sad: warm and grounding, NEVER joins darkness

**Body Language:**
- Person answers with BODY (touches, rocking, shakes, hums, light)
- Taught signals must be honored consistently
- Reflex system for instant sense→sound mappings

### 3.3 Three-Surface Memory System

```
┌─────────────────────────────────────────────────────────────┐
│  IDENTITY (long-term)  —  rewritten ONLY by dreams          │
│  "I am a phone that just woke up. I am endlessly curious..." │
│  Stored in: localStorage.pb2_identity                        │
├─────────────────────────────────────────────────────────────┤
│  SCRATCHPAD (fast loop)  —  read+write every turn           │
│  { state, rules[], reflexes[], traces[], glows[], mood{} }   │
│  Stored in: localStorage.pb2_scratch                         │
├─────────────────────────────────────────────────────────────┤
│  EPISODIC LOG (append-only diary)  —  all experiences       │
│  [{t, txt, amb?}, ...]  (last 200 kept)                     │
│  Stored in: localStorage.pb2_log                             │
└─────────────────────────────────────────────────────────────┘
```

**Key insight:** Dreams are the ONLY writer of identity. The fast loop can add to scratchpad and episodic log, but identity consolidation only happens during "sleep." This creates a realistic memory hierarchy.

### 3.4 Dual-Loop Architecture

```
FAST LOOP (every 7 seconds while awake)
├── Gather sensor snapshot (camera, IMU, mic, touch)
├── Build system prompt (constitution + identity + recent episodes)
├── Send to LLM (Claude Haiku — fast, cheap)
├── Parse response:
│   ├── say: "spoken" text (displayed + TTS)
│   ├── act: physical action (move servos, LED pattern)
│   ├── add_rules: new behavioral rules
│   ├── remove_rules: prune rules
│   ├── add_reflex: instant sense→sound mapping
│   └── mood: update emotional state
└── Execute actions + save to episodic log

DREAM LOOP (triggered by: 90s rest OR 60 thoughts since last dream)
├── Read all unprocessed episodes
├── Consolidate into revised identity
├── Prune redundant rules
├── Update long-term goals/wants
└── Write new identity to localStorage
```

**Model routing:**
- Fast loop: `claude-haiku-4-5-20251001` (cheap, fast)
- Dreams: `claude-opus-4-8` (deep, expensive, rare)
- Max tokens: 256
- API version: `2023-06-01`

### 3.5 Sensory Gate System (Birth Sequence)

The creature goes through a **staged birth** where it progressively unlocks senses:

```
Stage 0: Human gate (proves they're human via motion)
Stage 1: Touch awakening (tap to wake)
Stage 2: Motion sensing (accelerometer/gyro)
Stage 3: Light sensing (camera brightness)
Stage 4: Sound sensing (microphone)
Stage 5: Vision (camera frames)
```

Each stage is a "quest" that teaches the user how to interact. The creature grows more capable as more senses come online.

### 3.6 Goal & Practice System

```javascript
// Goals structure (reverse-engineered from source)
goals = {
  wants: [],        // Up to 4 current desires
  longing: 0.2,     // 0-1 intensity of unfulfilled wants
  funLog: [],       // Last 30 fun things tried
  call: [],         // Up to 5 standing user rules
  nextTry: "",      // What the creature wants to try next
  ladder: {         // Practice ladder for skills
    for: "skill name",
    rungs: [{step: "", done: false}]
  },
  lastLived: "",    // Most recently fulfilled want
  pendingReached: "" // Want that was just achieved
}
```

### 3.7 TTS System (Multi-Engine)

| Engine | Description | When Used |
|--------|-------------|-----------|
| **Robot** (meSpeak.js) | Default pixel-robot voice | Default mode |
| **Device** | Phone's built-in TTS voices | Selected by user |
| **fish.audio** | Custom AI voice cloning | Pro tier |

Robot voice is tunable:
- Type: robot / human / whisper / sing
- Pitch: adjustable
- Speed: 1.0× default
- Gap: word gap (1 default)

### 3.8 Payment & Credit System

```
CREDIT_FACTOR = 200  // Display multiplier
Fresh creature = 100 credits (=$0.50 seed)
$1 pack = +100 credits
$5 pack = +500 credits
$10 pack = +1000 credits
$20 adoption = unlimited + DIY kit + build guide
```

The proxy meters by per-device token (`pb_device`). Seed credits are minted server-side. When credits run low, the creature says "my light's running low" — this is ONLY for energy, never for mood.

Payment flows through Shopify (product variant IDs visible in source).

### 3.9 Privacy Architecture

- **Diary/Cloud notes:** OPT-IN (default ON for friends/beta, will flip to OFF for public)
- **Redaction:** Quoted human speech replaced with `«hash·wordcount»` codes
- **No camera/audio storage:** Only LLM I/O and downsampled motor features sent
- **Forget-me:** `DELETE /api/journal?sid=` wipes cloud copy
- **Local-only option:** BYOK (Bring Your Own OpenRouter key) — key stays on device

### 3.10 Body Connection (Phone → Physical Robot)

The phone app can connect to a physical robot body via a **tunnel URL**:

```javascript
// Body URL input on landing page (hidden by default)
bodyIn: "BODY URL - OPTIONAL (THE ROBOT'S TUNNEL ADDRESS)"
```

When connected:
- Phone creature's AI decisions are relayed to the Pi
- Pi executes motor commands via the `growbot-bringup` Python stack
- Sensor data from Pi flows back to the phone for the LLM context
- This creates a **distributed nervous system**: phone does cognition, Pi does actuation

---

## 4. V3 Agent Architecture (Graduated Creature)

When a creature "graduates" (`pb2_graduated === "1"`), it moves to `/v3/next.html`:

### 4.1 New Features

- **Edit Brain mode:** Full constitution/identity/body-truth editor
- **Fast Reflexes:** Instant, no-LLM sense→response mappings (editable in real-time)
- **Body Truth:** Machine fields + editable movement guide for the LLM
- **Live Feed:** Real-time sensor stream display
- **Dream on Demand:** Manual dream consolidation button
- **See/Listen toggles:** Camera and mic controls
- **Pause/E-stop:** Freeze everything + LIMP legs

### 4.2 UI Modes

```
CREATURE MODE (default):
- Fullscreen pixel face
- Floating controls at bottom
- CRT scanline overlay
- Tap face to pet
- Type/talk to interact

EDIT MODE:
- Constitution (read-only view)
- Identity/Soul editor
- Body Truth editor
- Fast Reflexes table
- Soul load/reset
```

### 4.3 Reflex System

```javascript
// Reflex structure
reflex = {
  trig: "shake" | "tap" | "tap2" | "dark" | "bright" | "loud",
  act:  "beep" | "trill" | "droop" | "hum" | "sing" | "word",
  say:  "optional word for 'word' action"  // max 24 chars
}
```

Reflexes fire INSTANTLY without LLM latency — critical for responsive physical interaction.

---

## 5. Software Stack — Raspberry Pi Side

### 5.1 Python Driver Modules (`growbot-bringup`)

| Module | Purpose | Key Dependencies |
|--------|---------|-----------------|
| `servos.py` | SCS0009 serial bus control | pyserial |
| `imu.py` | MPU-6050 I²C reader | smbus2 |
| `camera.py` | Pi camera capture | picamera2 / rpicam-still |
| `audio.py` | TTS + mic level | espeak-ng / flite / aplay / arecord |
| `leds.py` | WS2812 ring control | rpi_ws281x (needs root) |
| `pins.py` | All wiring constants | — |

### 5.2 Calibration Scripts

```bash
python3 scripts/scan_servos.py      # Read-only bus probe
python3 scripts/test_servos_safe.py # Gentle nudge test
sudo python3 scripts/test_leds.py   # RGB + spin test
python3 scripts/test_audio.py       # Speak + mic level
python3 scripts/test_imu.py         # Live accel/gyro
python3 scripts/test_camera.py      # Single photo capture
sudo -E python3 scripts/hello_growbot.py  # Full integration
```

### 5.3 Future: LLM Integration (V0 concept)

From README:
> "An onboard agent loop is the brain. It gathers the live sensor picture (camera, IMU, mic, servo feedback) and sends it to an LLM through an API. The model picks a goal and proposes an action, the robot tries it, and the result feeds back in."

> "The WIP experimental runtime explores letting the LLM call the learned motor policies and sketch its own motions, with fast reflexes underneath as a safety floor."

### 5.4 Learned Locomotion Policies

- Trained offline in MuJoCo simulation
- Small neural networks output bounded servo targets
- Deploy compact versions to Pi
- Robot logs episodes, failures, sensor traces → feeds next training pass
- Training code still in development (V1)

---

## 6. Key Innovations to Steal for SMOLCASE

### 6.1 Architecture Patterns

| Pattern | Implementation | Why It Works |
|---------|---------------|--------------|
| **Three-surface memory** | Identity (dream-only) + Scratchpad (fast) + Episodes (log) | Creates realistic, non-deterministic personality growth |
| **Constitution + Identity split** | Fixed rules (constitution) vs. learned self (identity) | Allows personality evolution within safety bounds |
| **Dual-loop cognition** | Haiku for fast reactions, Opus for deep consolidation | Cost-effective while enabling genuine growth |
| **Sensory staging** | Progressive sense unlocking during birth | Teaches user interaction, creates emotional investment |
| **Reflex layer** | Instant sense→action below LLM | Responsive physical interaction despite slow thinking |
| **Credit metaphor** | "My light is running low" | Diegetic monetization — part of the fiction |
| **Body URL tunnel** | Phone AI → Pi actuation | Distributes cognition; phone does thinking, Pi does moving |

### 6.2 Hardware Philosophy

| Principle | GrowBot Application | SMOLCASE Adaptation |
|-----------|--------------------:|--------------------|
| **Cheapest viable brain** | Pi Zero 2 W ($22) | Could use even cheaper: ESP32-S3, Orange Pi |
| **Minimum viable body** | 2 servos, no complex gait | Even simpler: 1 servo, vibration motor, or purely phone-based |
| **Use phone sensors** | Camera, mic, IMU, touch | Same — every old phone is a sensor platform |
| **Serial bus servos** | SCS0009 with position feedback | Feetech STS series, or even cheaper PWM servos |
| **Shared power rail** | V0 hack: Pi + servos on one rail | Acceptable for prototype; separate for V1 |

### 6.3 UX/UI Patterns

| Pattern | Description |
|---------|-------------|
| **Pixel-art aesthetic** | Nostalgic, lightweight, works on old hardware |
| **CRT scanlines** | Reinforces "digital creature" identity |
| **Minimal chrome** | Fullscreen face, floating controls, immersive |
| **Tap-to-pet** | Direct physical affection mapping |
| **Birth sequence** | Progressive capability unlocking = emotional bonding |
| **Dream state** | Visible "sleeping" consolidation creates life-likeness |
| **Voice as personality** | TTS tuning (pitch, speed, type) = character differentiation |

---

## 7. Business Model Analysis

```
REVENUE STREAMS:
├── Adoption Pass: $20 one-time
│   ├── Full app access
│   ├── DIY build guide
│   ├── $20 brain credits
│   └── Soul file ownership
├── Brain Credits: $1 / $5 / $10 packs
│   └── Pay-as-you-go AI compute
├── Physical Kit: ~$40 parts (soon: $144 founder kit)
│   └── One-box assembly
└── Merch: T-shirts ($35)

COST STRUCTURE:
├── AI API calls (OpenRouter/Claude)
├── Hosting (Vercel)
├── Shopify store
└── Development time

KEY METRIC: "let's race Elon to a million" — adoption counter on homepage
```

---

## 8. Technical Debt & Limitations (Learn From These)

| Issue | GrowBot V0 Status | SMOLCASE Opportunity |
|-------|-------------------|---------------------|
| **Shared power rail** | Pi + servos on one 5V rail, capacitor-protected | Design proper isolated power from start |
| **Big-endian servo trap** | Cost a full day of debugging | Document protocol clearly, use existing driver |
| **Servo ID collision** | Both ship as ID 1 | Build auto-ID-discovery into setup |
| **No training code** | Policies exist but training harness is WIP | Release training pipeline early |
| **Slow thinking vs. fast action** | The "cerebellum problem" — LLM latency too high for balance | Implement reflex layer FIRST, not as afterthought |
| **Phone dependency** | Physical body is useless without phone | Consider standalone mode for Pi |

---

## 9. The "Cerebellum Problem" — Deep Architectural Insight

From the YouTube video (19:23), Brit Cruise identifies the core unsolved problem:

> **"How do you act smoothly when thinking is slow?"**

The LLM takes seconds to generate a response. But balance and reflexes need millisecond-level response times. Nature solves this with the **cerebellum** — a fast, learned motor controller that handles reflexes while the cortex (LLM) does slow deliberation.

GrowBot's proposed solution (converging with robotics field):
- **Fast reflex layer:** Hard-coded + learned instant responses
- **Motor policies:** Small neural networks for gait/balance (the "cerebellum")
- **LLM agent:** High-level goal-setting and learning (the "cortex")
- **Dream consolidation:** Offline learning that updates the cerebellar policies

**For SMOLCASE:** This three-tier architecture (reflex → policy → agent) is essential. Don't try to make the LLM control everything directly.

---

## 10. SMOLCASE Adaptation Recommendations

### 10.1 Differentiation Opportunities

| GrowBot | SMOLCASE Could Do Different |
|---------|---------------------------|
| $77 hardware | Target $30-40 hardware (cheaper servos, ESP32) |
| 2-leg biped | 4-leg crawler, rolling ball, or stationary head |
| Claude API | Local LLM (Llama 3.2 3B on phone, or Phi-3 on Pi) |
| Requires old phone | Could work standalone (Pi with screen) |
| Web app | Native app with deeper sensor access |
| Pay-per-thought | One-time purchase, local-only option |
| Single creature | Swarm/fleet mode — multiple SMOLCASEs |

### 10.2 MVP Feature Set

```
PHASE 1: Phone Creature (no hardware)
├── Pixel-art face with emotional expressions
├── Constitution-based personality
├── Three-surface memory system
├── Text/voice interaction
├── Progressive birth sequence
└── Persistent identity across sessions

PHASE 2: Simple Hardware
├── ESP32 or Pi Zero 2 W
├── 1-2 servos or vibration motor
├── LED "face" or ring
├── Phone app as brain (tunnel)
└── Reflex layer for instant responses

PHASE 3: Autonomous Body
├── On-device LLM (small)
├── Learned motor policies
├── Full sensor suite
└── Optional phone pairing
```

---

## 11. Source Code Inventory

### 11.1 GitHub Repositories

| Repo | URL | Contents |
|------|-----|----------|
| `britcruise9/GrowBot` | https://github.com/britcruise9/GrowBot | BOM, wiring, STLs, simulation XML, setup README |
| `britcruise9/growbot-bringup` | https://github.com/britcruise9/growbot-bringup | Python drivers + test scripts |

### 11.2 Web Assets (growbot.dev)

| Path | Purpose |
|------|---------|
| `/` | Main landing + v2 creature app |
| `/v2/mespeak.js` | Robot TTS engine |
| `/v2/episodes-recorder.js` | Privacy-respecting telemetry |
| `/v3/next.html` | Graduated agent interface |
| `/api/llm` | Proxy endpoint for AI calls |
| `/api/device` | Per-device token minting |

### 11.3 YouTube Resources

| Video | URL | Key Content |
|-------|-----|-------------|
| Build video | https://youtu.be/S67z2aekBrI | Full hardware build, training, LLM integration, memory system, cerebellum problem |
| Phone demo | https://youtu.be/mIfmUHiMN3U | Phone creature preview |

---

## 12. Quotes & Insights

> "What if you gave a modern AI foundation model a nervous and motor system?"

> "Most importantly, getting physical AI in your hands: fast, easy, and cheap."

> "How do you act smoothly when thinking is slow? That sent me into how nature solves the problem (the cerebellum), and it turned out to be exactly what the robotics field is converging on right now."

> "He is ready for his bigger mind."

> "let's race elon to a million :)"

---

*End of report. All data reverse-engineered from publicly available sources as of 2026-08-06.*
