# AGENTS.md — SMOLCASE Developer & Agent Guide

Comprehensive guide for AI agents and human contributors working in the SMOLCASE repository.

---

## 1. Session Workflow

This section defines the operating model for agent-human collaboration. Every agent session follows these rules.

### 1.1 Workflow Roles

| Role | File | Responsibility |
|---|---|---|
| **The Scratchpad** | `.workspace-scratchpad.md` (git-ignored) | Branch-independent buffer for raw ideas, design notes, cross-branch context, and work-in-progress thinking shared between human and agent. Read at session start; append architectural critique, open questions, or constraints directly to it. Never touch active branch files during ideation. |
| **The Planner** | `docs/07-plans/wayfinder-map.md` + `TASKS.md` | High-level project visibility — technical architecture nodes, unknowns, dependency graphs, and step-by-step implementation plans. Always read at session start. |
| **The Harness** | The terminal / agent executing the current branch task | Reads from the Scratchpad, plans via the Planner, and executes against `TASKS.md` on the active branch. |

### 1.2 Ideation Protocol

1. **Buffer Consultation:** At session start, read `.workspace-scratchpad.md` to capture raw, multi-branch background ideas.
2. **Asynchronous Collaboration:** If the human left raw thoughts in the scratchpad, append architectural critique, open questions, or constraints below their text. Do not touch active branch files during this phase.
3. **Backlog Graduation:** When an idea is ready for development, map it via the Planner. Translate raw bullets into the project's standard format (see `TASKS.md` conventions).
4. **Behavioral Enforcement:** Every graduated task must explicitly state its user story and BDD-style (`Given-When-Then`) acceptance criteria before any application code is generated.

---

## 2. Available Skills

The following skills are installed globally and should be triggered when relevant:

| Skill | When to trigger | What it does |
|-------|----------------|--------------|
| **orient** | Session start | Reads TASKS.md, triages tasks, recommends next action |
| **close-and-learn** | Session end | Captures decisions, learns lessons, updates TASKS.md, generates handoff |
| **handoff** | Mid-stream pause | Writes structured handoff doc to /tmp for fresh agent |
| **wayfinder** | Large effort, many unknowns | Charts decision tickets on issue tracker, resolves via frontier |
| **goal** | Continuous autonomous work | Sets verifiable completion condition, keeps working until met |
| **grill-me** | Before committing to a plan | Stress-tests assumptions, surfaces blind spots |
| **tdd** | Writing new code | Enforces red-green-refactor cycle |
| **code-standards** | Code review, refactoring | Pragmatic coding quality guidelines |
| **simplify** | After a complex code task | Audits for over-engineering, rewrites to simplest version |

### 2.1 Template Reference

| When you need to... | Use template | Save as |
|---------------------|-------------|---------|
| Report a bug | `docs/templates/bug-template.md` | `_tasks/YYYYMMDD-NNN-slug.md` |
| Investigate an unknown | `docs/templates/spike-template.md` | `_tasks/YYYYMMDD-NNN-slug.md` |
| Propose an architecture change | `docs/templates/adr-template.md` | `docs/03-decisions/NNN-slug.md` |
| Define a feature | `docs/templates/feature-template.md` | `docs/06-specs/YYYY-MM-DD-slug.md` |
| Write a user story | `docs/templates/us-template.md` | `docs/06-specs/stories/slug.md` |
| Scope an epic | `docs/templates/epic-template.md` | `docs/06-specs/epics/slug.md` |
| Plan an implementation task | `docs/templates/task-template.md` | `_tasks/YYYYMMDD-NNN-slug.md` |
| Document an outage | `docs/templates/incident-template.md` | `incidents/YYYY-MM-DD-slug.md` |

## 3. Project Overview & Architecture

SMOLCASE is an autonomous desk companion robot consisting of:
- **Brain & Face:** Google Pixel 8 running an on-device Android companion app (screen-as-face, camera-based face tracking, voice recognition & TTS, on-device LLM personality, ML policy inference). See [[docs/03-decisions/001-pixel8-brain\|ADR-001]] for the brain decision, [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design\|Expressive Appliance Eyes spec]] for the face, and [[docs/06-specs/2026-08-17-gemma-backend-design\|Gemma Backend spec]] for on-device LLM.
- **Chassis & Mechanics:** 3D-printed chassis (`SC-CASE`) housing the phone, internal electronics, and battery, with two 1-piece 360°-rotating legs (`SC-LEG-L`, `SC-LEG-R`) hinged at ~2/3 case height. See [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]] and [[mech/SMOLCASE-CASE-Layout-Spec]] for the mechanical spec.
- **Actuation & Firmware:** Exactly two 360° serial-bus servos driven by an ESP32 bridge communicating with the Pixel 8 over Bluetooth Low Energy (BLE). See [[docs/03-decisions/004-esp32-ble-bridge\|ADR-004]] and [[docs/03-decisions/003-cpg-mujoco-tflite\|ADR-003]] for the control stack.

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

## 4. Directory Structure & Conventions

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
| `.workspace-scratchpad.md` | (git-ignored) Scratchpad | Branch-independent buffer for raw ideas, design notes, and cross-branch context shared between human and agent |
| `tinytroupe-workshop/` | Multi-agent persona workshop tooling | Simulation and report generator |
| `testbed/` | Appliance Dot Matrix Lab (HTML5/Canvas) | Live interactive browser testbed for shader, eyes, & dials |
| `scripts/` | Empirical evaluation & optimization harnesses | Photometric optimizer (`eval_appliance_dots.py`), face SVG/aperture exporter (`export_face_app_svg.py`) |

### Critical Workflow Rules
1. **`PROJECT_INDEX.md` is the index of record:** Every new document, spec, plan, ADR, or status change MUST be linked from `PROJECT_INDEX.md`. If it is not in the index, it does not exist.
2. **Wayfinder Map is the decision hub:** The [Wayfinder Map](docs/07-plans/wayfinder-map.md) holds the current destination, decision tickets, dependency chain, and fog of war. Always read it at session start alongside GOAL.md.
3. **`_tasks/` holds ticket detail:** Each decision ticket has a detail file in `_tasks/YYYYMMDD-NNN-slug.md` with YAML frontmatter, question, and acceptance criteria.
4. **TASKS.md is the execution surface:** The task board in `TASKS.md` uses `YYYYMMDD-NNN` IDs and tracks now/next/blocked/backlog for both wayfinder tickets and implementation tasks. See [[docs/07-plans/wayfinder-map]] and [[GOAL]] for the destination contract.
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
8. **Two legs, two servos only:** The robot has exactly 2x 360° serial-bus servos (one per leg, hinged ~2/3 up the case). Never write 8–12 servos or multi-jointed quadruped legs. See [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]] for the full mechanical architecture.

---

## 5. Toolchains & Essential Commands

### 5.1 Android Companion App (`android/`)

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

### 5.2 Simulation & RL Training (`sim/`)

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

### 5.3 Appliance Lab & Photometric Evaluation (`testbed/`, `scripts/`)

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

## 6. Android App Architecture & Patterns

### 6.1 Subsystems & Data Flow

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

## 7. Critical Gotchas & Non-Obvious Patterns

### 7.0 Conversation Log

- **Location:** `context.filesDir/logs/conversations.jsonl` (app-internal storage — survives rebuilds)
- **Format:** JSONL (one JSON object per line). Fields: `ts` (ISO-8601), `dir` (heard/replied/___session_start), `text`, `latency_ms`, `session`, `build_code`, `build_name`
- **Session counter:** Incremented on each app launch, persisted in `smolcase_log` SharedPreferences
- **View on device:** `adb shell cat /data/data/com.smolcase.companion/files/logs/conversations.jsonl`

### 7.1 LiteRT-LM Dependency & Execution
- **Version (`litertlm-android:0.16.0`):** LiteRT-LM is at `0.16.0` in `android/app/build.gradle.kts`. Earlier `0.10.2` lacked the `DYNAMIC_UPDATE_SLICE` op needed by Gemma 4 E2B. The `0.16.0` version works on Tensor G3 (Pixel 8) without the livelock previously observed.
- **CPU Backend Only:** `GemmaBackend` MUST use `Backend.CPU()`. The Mali-G715 GPU path on Tensor G3 suffers from a known upstream bug (LiteRT-LM issue #2202) that corrupts numbers and contractions.
- **Fast Storage Copying:** LiteRT-LM memory-maps (`mmap`) the model. Directly mapping from `/sdcard/` triggers severe FUSE daemon page fault latency (tens of minutes to initialize). `GemmaBackend` automatically copies the `.litertlm` file from `/sdcard/models/` to internal `context.filesDir/models/` on first boot.

### 7.2 Face Geometry & Eye Topography
- **Centerline Constraint ($Y \le 0.50$):** Eye apertures are hard-clipped at the horizontal centerline ($Y = 0.50$). The eyes MUST remain in the upper half of the screen so the robot maintains eye contact above desk clutter and leaves 2D gaze roaming room. Only soft optical bloom is allowed into the lower half.
- **Solid Eyes (No Pupils, No Specular Glint):** SMOLCASE eyes are solid illuminated LED clusters. Never add inner black pupils, animated iris rings, or cartoon white specular glints.

### 7.3 Persona & Design Language
- **TARS Register:** Persona is TARS from *Interstellar* (deadpan, dry wit, measured, low-pitched voice, military commander / coach register). No emojis, no lists, no bullet points, no cheerful assistant mannerisms.
- **Personality Dials:** `humor` (0..100) controls deadpan sarcasm and eyelid asymmetry (skeptical squint); `honesty` (0..100) controls bluntness.

---

## 8. Testing Conventions

- **Pure JVM Unit Tests:** Math, coordinate geometry, parsers, and state machines are written in pure Kotlin and run on the host JVM without an emulator via `./gradlew test`.
- **Key Test Files:**
  - `ApplianceMatrixCanvasTest.kt`: Validates grid buffering, ambient breathing math, and SDF boundary clipping.
  - `EyeExpressionStateTest.kt`: Validates mood parameter generation, dial modulation, and momentary reaction timers.
  - `DialCommandTest.kt`: Tests speech input parsing for humor/honesty dial adjustments.
  - `GemmaBackendTest.kt`: Tests backend configuration and missing-model diagnostic messages.
  - `MemoryMathTest.kt` & `CreaturePersonaTest.kt`: Tests prompt construction and memory context formatting.

---

## 9. Numbering Schemes — Disambiguation

The project uses several independent numbering schemes. Keep them distinct and always qualify the scheme name:

| Scheme | Scope | Examples | Purpose |
|--------|-------|----------|---------|
| **7s** | Workspace audit | Seiri, Seiton, Seiso, Seiketsu, Shitsuke, Seibi, Shuhi | Sort, set-in-order, shine, standardise, sustain, safe, secure — invoked via `/7s` skill |
| **ADR-NNN** | Architecture decisions | `ADR-001`, `ADR-005` | Permanent architecture records in `docs/03-decisions/` |
| **YYYYMMDD-NNN** | Task/bug IDs | `20260830-001`, `20260829-003` | Wayfinder tickets, bug reports, and implementation tasks in `_tasks/` |
| **Stage N** | App development | Stage 1 (Eyes), Stage 2 (Memory), Stage 3 (Voice) | Android companion app evolution — see [[android/README]] |
| **Voice Step N** | Voice improvements | Voice Step 1 (audio fix), Voice Step 2 (Piper TTS) | Sequential voice subsystem upgrades — see [[PROJECT_INDEX]] Status Board |
| **Tier N** | Behaviour training | Tier 1 (Locomotion), Tier 2 (Stability) | Behaviour curriculum tiers — see [[docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan]] |

**Rule:** Never refer to any of these as unqualified "Phase N" or "Step N". Always prefix the scheme name (e.g. "Voice Step 2", not "Phase 2").

---

## 10. Documentation Standards

### 10.1 Diagrams

Use **Mermaid** syntax for all diagrams embedded in Markdown docs. Mermaid is renderable by GitHub, Obsidian, and most AI agent context viewers. Supported diagram types:

| Diagram type | When to use | Example use case |
|-------------|-------------|-----------------|
| `flowchart` | Processes, workflows, decision trees | Session start flow, OODA loop, deployment pipeline |
| `sequenceDiagram` | Time-ordered interactions | API call flow, BLE handshake, user ↔ agent turn sequence |
| `gantt` | Timelines, schedules, roadmaps | Implementation plan, sprint timeline |
| `quadrantChart` | Prioritisation, risk/value matrices | Bug severity vs. impact, OODA phase maturity |
| `mindmap` | Brainstorming, hierarchical decomposition | Project structure, requirements breakdown |
| `stateDiagram` | State machines, lifecycle | Mood state machine, conversation lifecycle |
| `classDiagram` | Data models, class hierarchies | Android app architecture, LLM backend hierarchy |

Keep diagrams focused — one concept per diagram. Avoid deeply nested subgraphs that become unreadable.

### 10.2 Wiki-Link Conventions

All documentation uses **Foam-style `[[wiki-links]]`** for cross-references between files. This section defines how links are formatted, where they go, and how to maintain them.

### 10.3 Link Format

| Pattern | Usage | Example |
|---------|-------|---------|
| `[[path/to/file]]` | Standard cross-link. Omit `.md` extension. Resolves relative to repo root. | `[[docs/03-decisions/001-pixel8-brain]]` |
| `[[path/to/file\|Display Text]]` | Link with display text different from the file name. | `[[docs/03-decisions/001-pixel8-brain\|ADR-001]]` |
| `[[file]]` | Shorthand for root-level files (README, AGENTS, GOAL, TASKS). | `[[GOAL]]`, `[[TASKS]]` |
| `[[dir/]]` | Link to a directory README or index. | `[[docs/03-decisions/]]`, `[[docs/06-specs/]]` |
| `[[link]] §N` | Section reference within a target document. | `[[AGENTS]] §5`, `[[docs/06-specs/2026-08-17-gemma-backend-design]] §Why CPU` |

### 10.2 Where to Place Links

| Section location | Links to add |
|---|---|
| **YAML frontmatter / header block** (specs, plans, ADRs) | `Related:`, `See also:`, `Informs:`, `Supersedes:`, `Superseded by:`, `Implementation plan:` |
| **"Related" section** (end of document) | All sibling decisions, specs, plans, and research files that the reader should visit next |
| **"Background" section** | Links to predecessor documents that provide context (e.g. an eye spec linking to the research review that motivated it) |
| **Inline references** | Any mention of another document, concept decided in an ADR, or defined in a spec |

### 10.3 Maintenance Rules

1. **Add links on creation.** Every new document must link to its governing ADR(s), related specs/plans, and any predecessors it supersedes.
2. **Update superseded links.** When a document is superseded, add `Superseded by: [[newer-file]]` to its header block so readers find the current version.
3. **Verify on rename/move.** If a file is moved or renamed, update all `[[wiki-links]]` pointing to it. Use `git grep '\[\[old-path'` to find stale links.
4. **Verify on session start.** Run a quick check: does every `[[wiki-link]]` in a file resolve to an existing `.md` file? Stale links get a `FIXME` comment and a task in `TASKS.md`.
5. **Spec evolution chain.** The eye rendering lineage (TARS glyph → LCD panel → Appliance Dot Matrix) is the model for how supersession chains work — each spec links to the one it replaces and the one that replaced it.
6. **Index is authoritative.** `PROJECT_INDEX.md` must contain links to every active document. If a document is not indexed, it is not discoverable.
