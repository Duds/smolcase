# AGENTS.md — SMOLCASE Project Conventions

Guidance for any AI agent (or human collaborator) working in this repository.

## What this project is

SMOLCASE is a desk companion robot: a 3D-printed phone case with two 360°-rotating legs. A Pixel 8 phone is the brain (screen-as-face, sensors, LLM personality, ML policies); an ESP32 bridges BLE to serial-bus servos. Architecture is defined by ADR-001 through ADR-005.

## Directory conventions

| Content | Location |
|---------|----------|
| Research | `docs/01-research/` |
| Behaviour/training curriculum | `docs/02-behaviour-training/` |
| Architecture Decision Records | `docs/03-decisions/ADR-###.md` |
| Hardware (BOM, servo, power) | `docs/04-hardware/` |
| Design thinking log & roadmap | `docs/05-design-thinking/` |
| **Specs (approved designs)** | `docs/06-specs/YYYY-MM-DD-<topic>-design.md` |
| **Implementation plans** | `docs/07-plans/` |
| Simulation / training code | `sim/` |
| Trained models | `models/` |
| ESP32 firmware | `firmware/` |
| Android companion app | `android/` |
| CAD / mechanical | `mech/` |

**Never create `superpowers/`-branded folders** (e.g. `docs/superpowers/specs/`). If a skill or tool suggests a default spec/plan path, override it with `docs/06-specs/` or `docs/07-plans/`.

## House rules

1. **PROJECT_INDEX.md is the index of record.** If a document, decision, or status isn't linked from it, it doesn't exist. Update it whenever you add a document, ADR, or change status.
2. **Decisions go in ADRs.** Number sequentially (`docs/03-decisions/###-<slug>.md`), link from the index.
3. **Part codes:** `SC-<PART>-<variant>` per ADR-005. Primary parts: `SC-CASE`, `SC-LEG-L`, `SC-LEG-R`; secondary: HIP POD, RIBCAGE, BELLY, SHELL, JAW as defined in ADR-005.
4. **Two legs, two servos.** The robot has exactly 2× 360° serial-bus servos, one per leg, hinged ~2/3 up the case. Never write 8–12 servos or any multi-jointed leg chain; that was an early error and must not reappear in any document.
5. **Specs before code.** New features get an approved spec in `docs/06-specs/` and a plan in `docs/07-plans/` before implementation.

## Build & deploy (Android app)

```bash
# Build
cd android
export JAVA_HOME=~/toolchains/jdk-17/Contents/Home
export ANDROID_HOME=~/android-sdk
~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain

# Deploy over wireless adb (check `adb mdns services` if the port changed)
ADB=~/android-sdk/platform-tools/adb
$ADB -s 192.168.0.236:34927 shell am force-stop com.smolcase.companion
$ADB -s 192.168.0.236:34927 install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s 192.168.0.236:34927 shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1
```

Toolchain notes: JDK 17 at `~/toolchains/jdk-17`, Gradle 8.7 at `~/toolchains/gradle-8.7`, Android SDK at `~/android-sdk`. Kotlin plugin 2.2.21 (genai libraries carry Kotlin 2.3 metadata). Gemini Nano client: `com.google.mlkit:genai-prompt:1.0.0-beta4`.

## Personality / design language

SMOLCASE's persona is TARS (Interstellar): deadpan, measured, low-pitched voice; humor/honesty dials; monochrome phosphor-green text face. No cute cartoon eyes, no emoji, no high-pitched voice. SMOLCASE never performs "the wheel" — leg motion comes from the MuJoCo/ML gait curriculum.
