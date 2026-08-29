# AGENTS.md — SMOLCASE Developer & Agent Guide

Comprehensive guide for AI agents and human contributors working in the SMOLCASE repository.

---

## 1. Project Overview & Architecture

SMOLCASE is an autonomous desk companion robot consisting of:
- **Brain & Face:** Google Pixel 8 running an on-device Android companion app (screen-as-face, camera-based face tracking, voice recognition & TTS, on-device LLM personality, ML policy inference).
- **Chassis & Mechanics:** 3D-printed chassis (`SC-CASE`) housing the phone, internal electronics, and battery, with two 1-piece 360°-rotating legs (`SC-LEG-L`, `SC-LEG-R`) hinged at ~2/3 case height.
- **Actuation & Firmware:** Exactly two 360° serial-bus servos driven by an ESP32 bridge communicating with the Pixel 8 over Bluetooth Low Energy (BLE).

```
+─────────────────────────────────────────────────────────────────────────+
│                        GOOGLE PIXEL 8 (Android)                         │
│                                                                         │
│  [CameraX / ML Kit] ──> FaceTracker ──> CreatureBrain ──> TarsFaceView  │
│  [SpeechRecognizer] ──> VoiceEars   ──> ConversationEngine              │
│                                                │                        │
│                           ┌────────────────────┴───────────────────┐    │
│                           ▼                                        ▼    │
│                     IntentRouter                             LlmBackend │
│                 (offline canned rules)                 ┌─ GemmaBackend  │
│                                                        ├─ GeminiNano    │
│                                                        └─ KimiBackend   │
│                                                                         │
│  [Future / In-Progress]                                                 │
│  TFLite Policy Runner (MuJoCo gait) ──> BLE Command Stream              │
+────────────────────────────────────────────────────┬────────────────────+
                                                     │ BLE
                                                     ▼
+─────────────────────────────────────────────────────────────────────────+
│                      ESP32 SERIAL-BUS SERVO BRIDGE                      │
│                                                                         │
│  BLE GATT Receiver ──> Serial-Bus Servo Controller (2x 360° Servos)     │
+─────────────────────────────────────────────────────────────────────────+
```

---

## 2. Directory Structure & Conventions

| Directory | Purpose | Notes / Conventions |
|---|---|---|
| `android/` | Android companion app (Kotlin, Gradle) | Native app containing face rendering, speech, and LLM backends |
| `sim/` | MuJoCo simulation & reinforcement learning | Python training pipeline (Gymnasium, Stable-Baselines3 PPO) |
| `models/` | Checkpoints, trained policies, model manifests | Policy weights (`smolcase_gait.zip`), `.tflite` exports |
| `firmware/` | ESP32 BLE-to-serial-bus servo bridge | C++/Arduino/ESP-IDF firmware |
| `mech/` | Mechanical CAD specs, Fusion 360 parameters | Datum schemes, layout specs, 3D printing tolerances |
| `docs/01-research/` | Deep-dives, teardowns, empirical studies | GrowBot reverse engineering, eye library reviews |
| `docs/02-behaviour-training/` | Robot behavior & locomotion curricula | 30-behaviour gait training curriculum |
| `docs/03-decisions/` | Architecture Decision Records (ADRs) | Numbered sequentially (`ADR-###.md` or `###-<slug>.md`) |
| `docs/04-hardware/` | BOM, servo selection, power architectures | Battery, buck converter, wiring specs |
| `docs/05-design-thinking/` | Design logs, ideation journals, roadmaps | Stage-by-stage evolution records |
| `docs/06-specs/` | Approved technical specifications | `YYYY-MM-DD-<topic>-design.md` |
| `docs/07-plans/` | Wayfinder map & step-by-step implementation plans | `YYYY-MM-DD-<topic>.md`, `wayfinder-map.md` |
| `_tasks/` | Wayfinder decision ticket detail files | `YYYYMMDD-NNN-slug.md` with YAML frontmatter |
| `tinytroupe-workshop/` | Multi-agent persona workshop tooling | Simulation and report generator |
| `testbed/` | Appliance Dot Matrix Lab (HTML5/Canvas) | Live interactive browser testbed for shader, eyes, & dials |
| `scripts/` | Empirical evaluation & optimization harnesses | Photometric optimizer (`eval_appliance_dots.py`), face SVG/aperture exporter (`export_face_app_svg.py`) |

### Critical Workflow Rules
1. **`PROJECT_INDEX.md` is the index of record:** Every new document, spec, plan, ADR, or status change MUST be linked from `PROJECT_INDEX.md`. If it is not in the index, it does not exist.
2. **Wayfinder Map is the decision hub:** The [Wayfinder Map](docs/07-plans/wayfinder-map.md) holds the current destination, decision tickets, dependency chain, and fog of war. Always read it at session start alongside GOAL.md.
3. **`_tasks/` holds ticket detail:** Each decision ticket has a detail file in `_tasks/YYYYMMDD-NNN-slug.md` with YAML frontmatter, question, and acceptance criteria.
4. **TASKS.md is the execution surface:** The task board in `TASKS.md` uses `YYYYMMDD-NNN` IDs and tracks now/next/blocked/backlog for both wayfinder tickets and implementation tasks.
5. **Specs before code:** New features require an approved design in `docs/06-specs/` and an execution plan in `docs/07-plans/` before starting implementation.
6. **Never create `superpowers/` branded paths:** Always use `docs/06-specs/` and `docs/07-plans/`.
7. **Mechanical Part Naming (ADR-005):**
   - Main Chassis: `SC-CASE`
   - Legs: `SC-LEG-L`, `SC-LEG-R`
   - Servo Pods: `SC-POD-L`, `SC-POD-R` (HIP POD)
   - Internal PCB Bracket: `SC-RIB` (RIBCAGE)
   - Battery Retainer: `SC-BLY` (BELLY)
   - Back Lid / Service Panel: `SC-SHL` (SHELL)
   - Phone Retention Latch: `SC-JAW` (JAW)
8. **Two legs, two servos only:** The robot has exactly 2x 360° serial-bus servos (one per leg, hinged ~2/3 up the case). Never write 8–12 servos or multi-jointed quadruped legs.

---

## 3. Toolchains & Essential Commands

### 3.1 Android Companion App (`android/`)

The local environment uses custom toolchain paths for JDK, Gradle, and Android SDK.

#### Environment Setup
```bash
export JAVA_HOME=~/toolchains/jdk-17/Contents/Home
export ANDROID_HOME=~/android-sdk
GRADLE=~/toolchains/gradle-8.7/bin/gradle
ADB=~/android-sdk/platform-tools/adb
```

#### Build & Test Commands
```bash
# Run unit test suite (pure JVM, fast)
cd android
export JAVA_HOME=~/toolchains/jdk-17/Contents/Home
export ANDROID_HOME=~/android-sdk
~/toolchains/gradle-8.7/bin/gradle test --console=plain

# Run only debug unit tests
~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain

# Assemble Debug APK
~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain
```

#### Wireless/Wired ADB Deployment
```bash
# Identify device port if using wireless ADB
$ADB mdns services

# Stop running app, install APK, and launch MainActivity
DEVICE="192.168.0.236:34927"  # Update with current device IP/port or serial
$ADB -s $DEVICE shell am force-stop com.smolcase.companion
$ADB -s $DEVICE install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s $DEVICE shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1
```

#### Sideloading Gemma 4 E2B Model
To run the on-device Gemma LLM backend:
```bash
# 1. Grant MANAGE_EXTERNAL_STORAGE once via appops
$ADB -s $DEVICE shell appops set com.smolcase.companion MANAGE_EXTERNAL_STORAGE allow

# 2. Push .litertlm model file to public storage
$ADB -s $DEVICE push models/gemma-4-E2B-it.litertlm /sdcard/models/gemma-4-E2B-it.litertlm
```

---

### 3.2 Simulation & RL Training (`sim/`)

#### Environment Setup
```bash
# Install Python dependencies
pip install -r sim/requirements.txt
```

#### Training & Evaluation Commands
```bash
# Verify MuJoCo body XML loads correctly
python3 sim/src/test_xml_load.py

# Run CPG baseline controller (10 episodes)
python3 sim/src/smolcase_train.py --mode cpg --episodes 10

# Train PPO walk policy in MuJoCo (500K steps)
python3 sim/src/smolcase_train.py --mode train --timesteps 500000

# Evaluate trained model
python3 sim/src/smolcase_train.py --mode eval --model models/smolcase_gait.zip --episodes 5

# Export trained policy to TFLite
python3 sim/src/smolcase_train.py --mode export --model models/smolcase_gait.zip
```

---

### 3.3 Appliance Lab & Photometric Evaluation (`testbed/`, `scripts/`)

#### Interactive Dot Matrix Lab (`testbed/index.html`)
A standalone HTML5 Canvas testbed for visual prototyping and testing of the dot matrix shader before deploying to Android:
- **Features:** 38×68 LED grid simulation, live Cozmo SDF eye rendering, mood presets, TARS humor/honesty dial tuning, touch gaze dragging, ambient breathing waves, and bottom-edge telemetry pips.
- **Usage:** Open `testbed/index.html` directly in any web browser or serve via `python3 -m http.server 8000 --directory testbed`.

#### Photometric Evaluation Harness (`scripts/eval_appliance_dots.py`)
Compares synthetic diode renders against empirical macro camera captures (`Screenshot_20260821-135151.png`):
- **Evaluates:** Peak luminance, ghost dot substrate alpha, radial point spread function (PSF decay curve), and SSIM/PSNR fidelity.
- **Output:** Optimization reports and metrics written to `docs/01-research/analysis/eval_loop/`.
```bash
# Run photometric evaluation and optimizer
python3 scripts/eval_appliance_dots.py
```

---

## 4. Android App Architecture & Patterns

### 4.1 Subsystems & Data Flow

1. **Visual Expression System (`matrix/` & `TarsFaceView.kt`):**
   - **`ApplianceMatrixCanvas`**: Discrete LED/VFD dot matrix canvas (default 38 cols × 68 rows). Manages dot buffers, anti-aliased dot intensities, ghost dot baselines (~5% alpha), and ambient breathing wave modulation.
   - **`CozmoEyeParams`**: Parametric signed distance field (SDF) model for solid glowing Cozmo-style kawaii eyes. Features corner rounding, top/bottom lid cutoffs, angular slant, squash/stretch scaling, and **strict upper-half boundary enforcement ($Y \le 0.50$)**.
   - **`CozmoEyeRasterizer`**: Evaluates SDFs over the discrete LED grid with radial glow falloff.
   - **`EyeExpressionState`**: Discrete mood state machine (`NEUTRAL`, `SKEPTICAL`, `HAPPY`, `CURIOUS`, `DROWSY`, `SLEEPING`, `ALERT`, `THINKING`, `HEART`) modulated by TARS `humor` and `honesty` dials.
   - **`ApertureGrid`**: Generated aperture layout constants (38×68 grid, physical mm cell geometry). **Do not hand-edit** — regenerate with `python3 scripts/export_face_app_svg.py --grid 38x68`.
   - **`CozmoEyeInterpolator`**: Smooths parameter transitions between eye frames.

2. **Sensory & Tracking (`FaceTracker.kt`, `VoiceEars.kt`):**
   - **`FaceTracker`**: CameraX image analysis stream running Google ML Kit Face Detection. Feeds normalized coordinates `(nx, ny)` to `CreatureBrain`.
   - **`VoiceEars`**: Android `SpeechRecognizer` wrapper with mute/unmute control and echo prevention during TTS playback.

3. **Thinking & Conversation (`ConversationEngine.kt`, `llm/`):**
   - **`IntentRouter`**: Offline rule engine checking canned phrases (reminders, greetings, dial commands). Always evaluates before LLM backends.
   - **`DialCommand`**: Natural language parser for adjusting personality dials ("set humor to 90", "honesty 50%").
   - **`PersonalityDials`**: Manages persistent `humor` (0..100) and `honesty` (0..100) dials in `SharedPreferences`.
   - **`MemoryStore`**: Local JSON persistence for interactions, owner face recognition counts, and context summaries.
   - **`LlmBackend` Hierarchy**:
     - `GemmaBackend`: On-device Gemma 4 E2B via LiteRT-LM (sideloaded `.litertlm`).
     - `GeminiNanoBackend`: On-device Gemini Nano via AICore / ML Kit GenAI Prompt API.
     - `KimiBackend`: Cloud OpenAI-compatible endpoint (Moonshot API).

---

## 5. Critical Gotchas & Non-Obvious Patterns

### 5.1 LiteRT-LM Dependency & Execution
- **Version (`litertlm-android:0.16.0`):** LiteRT-LM is at `0.16.0` in `android/app/build.gradle.kts`. Earlier `0.10.2` lacked the `DYNAMIC_UPDATE_SLICE` op needed by Gemma 4 E2B. The `0.16.0` version works on Tensor G3 (Pixel 8) without the livelock previously observed.
- **CPU Backend Only:** `GemmaBackend` MUST use `Backend.CPU()`. The Mali-G715 GPU path on Tensor G3 suffers from a known upstream bug (LiteRT-LM issue #2202) that corrupts numbers and contractions.
- **Fast Storage Copying:** LiteRT-LM memory-maps (`mmap`) the model. Directly mapping from `/sdcard/` triggers severe FUSE daemon page fault latency (tens of minutes to initialize). `GemmaBackend` automatically copies the `.litertlm` file from `/sdcard/models/` to internal `context.filesDir/models/` on first boot.

### 5.2 Face Geometry & Eye Topography
- **Centerline Constraint ($Y \le 0.50$):** Eye apertures are hard-clipped at the horizontal centerline ($Y = 0.50$). The eyes MUST remain in the upper half of the screen so the robot maintains eye contact above desk clutter and leaves 2D gaze roaming room. Only soft optical bloom is allowed into the lower half.
- **Solid Eyes (No Pupils, No Specular Glint):** SMOLCASE eyes are solid illuminated LED clusters. Never add inner black pupils, animated iris rings, or cartoon white specular glints.

### 5.3 Persona & Design Language
- **TARS Register:** Persona is TARS from *Interstellar* (deadpan, dry wit, measured, low-pitched voice, military commander / coach register). No emojis, no lists, no bullet points, no cheerful assistant mannerisms.
- **Personality Dials:** `humor` (0..100) controls deadpan sarcasm and eyelid asymmetry (skeptical squint); `honesty` (0..100) controls bluntness.

---

## 6. Testing Conventions

- **Pure JVM Unit Tests:** Math, coordinate geometry, parsers, and state machines are written in pure Kotlin and run on the host JVM without an emulator via `./gradlew test`.
- **Key Test Files:**
  - `ApplianceMatrixCanvasTest.kt`: Validates grid buffering, ambient breathing math, and SDF boundary clipping.
  - `EyeExpressionStateTest.kt`: Validates mood parameter generation, dial modulation, and momentary reaction timers.
  - `DialCommandTest.kt`: Tests speech input parsing for humor/honesty dial adjustments.
  - `GemmaBackendTest.kt`: Tests backend configuration and missing-model diagnostic messages.
  - `MemoryMathTest.kt` & `CreaturePersonaTest.kt`: Tests prompt construction and memory context formatting.
