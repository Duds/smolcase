# Gemma 4 E2B On-Device Backend — Design

**Date:** 2026-08-17
**Status:** Approved (owner pre-approved "spec and build")
**Supersedes:** nothing; extends the LLM backend layer from the v0.5-tars design

## Problem

Gemini Nano (AICore) reports `FeatureStatus.UNAVAILABLE` (raw status=0) on the
desk Pixel 8. Root cause confirmed on-device: the phone has **no Google account
signed in**, and AICore will not provision Gemini Nano without one. The owner
does not want a Google account on this device, and does not want a cloud-only
brain as the default. The creature currently falls back to "You'll have to
rephrase that." for every non-canned utterance.

## Decision

Add a fourth thinking layer: **Gemma 4 E2B running locally via LiteRT-LM**,
with the model file sideloaded over adb. No Google account, no AICore, no
network after installation.

### Why Gemma 4 E2B / LiteRT-LM

- LiteRT-LM is Google's production on-device LLM runtime (powers AI Edge
  Gallery, Chrome on-device, Pixel Watch) with a **stable Kotlin API** published
  to Google Maven: `com.google.ai.edge.litertlm:litertlm-android:0.16.0`.
- The model `litert-community/gemma-4-E2B-it-litert-lm` on Hugging Face is
  **public, ungated, Apache-2.0** — no account or token needed to download.
- `gemma-4-E2B-it.litertlm` is 2.41 GiB, mixed 2/4/8-bit quantization, ~0.8 GB
  resident for text. Fits the Pixel 8 (8 GB RAM) comfortably.
- Pixel 8 is on Google's supported-hardware list for LiteRT-LM.

### Why CPU backend (not GPU)

Field reports (LiteRT-LM issue #2202, HF discussion #10) document a **GPU
backend bug on Mali-G715** — the Tensor G3's GPU — that mangles numbers and
contractions in output. The CPU backend does not exhibit it. Expected
~10-25 tok/s on CPU is acceptable for one-or-two-sentence TARS replies. GPU is
deferred behind a future setting; not in scope.

## Architecture

```
VoiceEars → ConversationEngine ─┬─ IntentRouter (rules, always first)
                                ├─ GeminiNanoBackend (AICore)   [existing]
                                ├─ KimiBackend (cloud)          [existing]
                                └─ GemmaBackend (LiteRT-LM)     [NEW]
                                       │
                                       ▼
                        Engine(EngineConfig(modelPath, backend=CPU))
                                       │
                              createConversation(systemInstruction=persona)
                                       │
                              sendMessage(userText + soul context)
```

### GemmaBackend (llm/GemmaBackend.kt) — implements LlmBackend

- **Model location:** `/sdcard/models/gemma-4-E2B-it.litertlm` — a public
  directory the owner can see in the Files app. App-private external storage
  (`Android/data/<pkg>/files`) was tried first and rejected: files `adb push`ed
  by shell stay shell-owned and Android's FUSE layer hides them from the app
  entirely. Requires `MANAGE_EXTERNAL_STORAGE` (manifest-declared), granted
  once over adb:
  `adb shell appops set com.smolcase.companion MANAGE_EXTERNAL_STORAGE allow`.
- **Startup probe (init):** if the file exists, begin `engine.initialize()` on a
  background coroutine immediately (takes up to ~10 s cold) so the brain is
  warm before the first spoken question. If the file is missing, skip straight
  to an actionable unavailable reason.
- **unavailableReason():**
  - file missing → "Gemma model not sideloaded: adb push gemma-4-E2B-it.litertlm
    to /sdcard/Android/data/com.smolcase.companion/files/models/"
  - loading → "Gemma is warming up (model load)"
  - load failure → the exception message
  - ready → null
- **reply():** single-turn, stateless — matches the Nano/Kimi backends.
  Creates a fresh `Conversation` per reply with
  `ConversationConfig(systemInstruction = Contents.of(CreaturePersona.prompt(humor, honesty)))`,
  sends `soulContext + userText`, returns the text, closes the conversation.
  Engine stays open for the process lifetime.
- **Error handling:** `LiteRtLmJniException` and friends → callback(null),
  reason stored for the status line, logged to logcat (tag SmolcaseLLM via
  ConversationEngine).

### Settings

- `LlmSettings.Backend.GEMMA` added to the enum (stored by name; existing
  values unchanged, so existing installs keep their setting).
- SettingsActivity gets a fourth radio: **"Gemma 4 E2B (on-device, sideloaded)"**.
- `ConversationEngine.statusLine()` labels it `LLM GEMMA OK` / `LLM GEMMA --`.

### Build changes

- `app/build.gradle.kts`: add
  `implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.0")`
  (pinned, not `latest.release`, for reproducible builds).
- No `AndroidManifest.xml` change: the `libOpenCL.so`/`libvndksupport.so`
  `<uses-native-library>` entries are only required for the GPU backend.

## Model acquisition (one-time, developer-run)

```bash
# On the Mac:
curl -L -C - -o models/gemma-4-E2B-it.litertlm \
  "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
# To the phone (2.41 GiB over adb):
adb push models/gemma-4-E2B-it.litertlm /sdcard/models/
adb shell appops set com.smolcase.companion MANAGE_EXTERNAL_STORAGE allow
```

`models/*.litertlm` is gitignored — the weights never enter the repo.

## Testing

- **Unit (JVM):** LiteRT-LM's native engine can't run in host unit tests, so
  tests cover what's testable: `LlmSettings.Backend.GEMMA` round-trips through
  SharedPreferences, `ConversationEngine.statusLine()` renders the GEMMA label,
  and `GemmaBackend.unavailableReason()` reports the missing-file reason with
  the correct adb instructions (file-absent path is pure JVM).
- **On-device:** ask a non-canned question ("what do you think about
  Mondays?"), expect a real TARS-register reply, `LLM GEMMA OK` in the
  telemetry feed, and no `SmolcaseLLM` warnings.

## Risks

| Risk | Mitigation |
|------|------------|
| 2.41 GiB adb push is slow/fragile | Resume-capable `curl -C -` on the Mac; `adb push` verifies size after |
| Engine load OOMs with face tracker + camera alive | Load once, keep warm; monitor logcat for OOM on first runs |
| APK bloat from bundled native libs | Accepted (tens of MB); CPU-only keeps it to one native backend |
| E2B quality below Nano | Persona prompt carries most of the voice; dials still injected; reversible setting |
| GPU number-mangling bug if GPU tried later | Ship CPU-only; GPU requires an explicit future setting |

## Out of scope

- GPU / NPU backends, MTP speculative decoding
- Multi-turn chat history (all backends are single-turn today)
- Tool calling via LiteRT-LM `@Tool` (future: reminders/tools through the LLM)
- In-app model download UI (adb sideload is fine for a single-device project)
