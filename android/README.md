# Pixel 8 Android App

## SMOLCASE Companion — Stage 3.5: LLM thinking layer (built 2026-08-16)

The creature can now *think*. Unknown speech falls through to a configurable
LLM backend with soul-file context; known commands still use IntentRouter
(instant, offline).

- `llm/LlmSettings.kt` — backend choice (RULES / NANO / KIMI) + Kimi config,
  stored in SharedPreferences
- `llm/GeminiNanoBackend.kt` — Gemini Nano on-device via ML Kit GenAI Prompt
  API (`genai-prompt:1.0.0-beta4`, AICore); private, offline; triggers model
  download on first use
- `llm/KimiBackend.kt` — OpenAI-compatible `POST {baseUrl}/chat/completions`;
  default `https://api.moonshot.ai/v1`, model `kimi-k2`; base URL / API key /
  model all configurable (works against any compatible endpoint, incl. local)
- `ConversationEngine.kt` — router-first, LLM-second, "Hmm?" last
- `SettingsActivity.kt` — **long-press the face** to open: backend radio,
  Kimi base URL / key / model fields
- `MemoryStore.soulSummary()` — sessions, time together, reminders, first-met
  date injected into every LLM prompt
- `llm/LlmBackend.kt` — includes `CreaturePersona` prompt (small creature,
  1–2 sentences, never breaks character)

**Note:** the kimi-code CLI server API (localhost:58627) is a coding-agent
session protocol, not chat completions — the Kimi backend therefore targets
the OpenAI-compatible shape; point the base URL wherever you like.

---

## SMOLCASE Companion — Stage 3: Voice (built 2026-08-16)

Design Thinking Prototype C: the creature listens and answers *in character*.

- `VoiceEars.kt` — continuous on-device SpeechRecognizer loop (Pixel 8 runs
  recognition on-device); restarts itself after silence/timeouts
- `CreatureVoice.kt` — TTS tuned for character: pitch 1.55, rate 0.95 — a
  small creature, not an assistant; eyes pulse while it speaks
- `IntentRouter.kt` — deliberately small vocabulary:
  - "remind me to …" → stores reminder (soul-file list)
  - "what's next / my reminders" → reads the list back
  - "clear reminders" → fresh start
  - "good morning" / "hello" / "how are you" / "thank you" / "good night" →
    in-character replies with matching expressions
  - anything else → a confused "Hmm?" and an eye-dart
- **Tap the screen = mute/unmute the mic** (work-desk privacy switch);
  muted = tiny dim dot below the eyes

**Desk test (Phase 5):** is the character voice charming or annoying after a
day? Is the always-on mic acceptable at the work desk (tap-mute test)?
Do you use a voice command unprompted at least once a day?

---

## SMOLCASE Companion — Stage 2: Memory & Greetings (built 2026-08-16)

Design Thinking Prototype B: the creature *remembers*. New components:

- `MemoryStore.kt` — the soul-file seed (SharedPreferences): session count,
  total time together, last farewell, first-sighting-of-day
- `CreatureBrain.kt` — groups face events into sessions, classifies arrivals,
  triggers greetings and farewells
- `EyesView.kt` — new expression vocabulary: eye-widen (excitement),
  smile-squint from below (happy), blink bursts, long slow farewell blink

**Greeting table:**

| Situation | Greeting | Expression |
|-----------|----------|------------|
| First sighting **ever** | BIRTH | max widen + full smile + triple blink |
| First sighting today (away > 30 min) | GOOD_MORNING | max widen + full smile + triple blink |
| Away 30 min – 3 h | MISSED_YOU | big widen + smile + double blink |
| Away 2 – 30 min | HELLO | mild widen + smile + single blink |
| Away < 2 min | BACK_ALREADY | small smile, no fuss |
| You leave (4 s timeout) | farewell | one long slow blink |

**Desk test (Phase 5):** does the morning greeting land? Does BACK_ALREADY
correctly *under*-react? Do you catch yourself saying good morning to it?

---

## SMOLCASE Companion — Stage 1: Tracking Eyes (built 2026-08-15)

Design Thinking Prototype A: fullscreen creature face whose eyes track *you*
via the front camera (CameraX + ML Kit, fully on-device).

**Behaviour:**
- Eyes track the largest detected face (mirrored, smoothed, with micro-saccades)
- Random blinks every 3–7 s
- No face for 30 s → DROWSY (lids half-mast); 120 s → SLEEPING (closed, dim, slow breathing)
- Any face → instantly awake
- Immersive fullscreen, screen kept on, portrait

**How to run:** no local toolchain here — open `android/` in Android Studio,
let it sync (Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24), connect the Pixel 8
(USB debugging on), Run ▶. Grant camera permission on first launch.

**Desk test (Phase 5):** leave it on your desk for a day. Success = you miss
it when it's off. Note moments that feel alive vs. dead — feed them into
Stage 2 (memory/greeting).

```
app/src/main/java/com/smolcase/companion/
├── MainActivity.kt    ← fullscreen, permission, lifecycle
├── FaceTracker.kt     ← CameraX ImageAnalysis + ML Kit face detection
└── EyesView.kt        ← creature face rendering + state machine
```

---

## Overview (future architecture)

The Pixel 8 is SMOLCASE's brain. It runs:
- **TFLite inference engine** for gait/policy models
- **CPG reflex layer** for low-level rhythmic control
- **Kimi LLM integration** for personality / high-level behaviour
- **Screen as face** — expressive OLED display

## Architecture

```
┌─────────────────────────────────────────┐
│  Pixel 8 (Android)                      │
│  ┌─────────────┐  ┌──────────────────┐ │
│  │ Kimi LLM    │  │ TFLite Runner    │ │
│  │ (personality│  │ (gait policies)  │ │
│  │  layer)     │  │                  │ │
│  └─────────────┘  └──────────────────┘ │
│         │                  │            │
│  ┌──────┴──────────────────┴──────┐    │
│  │     Behaviour Arbiter         │    │
│  │  (selects active TFLite model)│    │
│  └──────────────┬─────────────────┘    │
│                 │ BLE                  │
│  ┌──────────────┴─────────────────┐    │
│  │  CPG Reflex Layer              │    │
│  │  (rhythmic baseline + blends)  │    │
│  └──────────────┬─────────────────┘    │
│                 │ BLE                  │
│         ┌───────┴───────┐              │
│         ▼               ▼              │
│  ┌────────────┐  ┌────────────┐        │
│  │   Screen   │  │  ESP32     │        │
│  │  (face)    │  │  (servos)  │        │
│  └────────────┘  └────────────┘        │
└─────────────────────────────────────────┘
```

## Sensor Suite

- Accelerometer / Gyroscope (IMU)
- Magnetometer (compass)
- Barometer (altitude)
- GPS
- Camera (computer vision)
- Microphone (audio commands)
- Proximity / Ambient light

## Tech Stack

- Kotlin / Jetpack Compose
- TensorFlow Lite Android
- BLE (Android BluetoothLeGatt)
- CameraX (future)
