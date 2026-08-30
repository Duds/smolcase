# Voice System Phase 2 — Replace Android TTS with Piper via sherpa-onnx

**Source:** [[docs/01-research/SMOLCASE-Voice-System-Deep-Research]] §5.2
**Branch:** `voice-phase2-piper-tts`
**Started:** 2026-08-31
**Related:** [[docs/07-plans/2026-08-31-voice-phase1-audio-fix]] (pre-requisite), [[AGENTS]] §5.1 (sherpa-onnx gotchas), [[docs/06-specs/2026-08-16-tars-face-persona-design]] (TARS voice register)

## Context

Replace the Android system `TextToSpeech` engine with Piper TTS via sherpa-onnx. This gives us sub-200ms first-token latency, streaming `AudioTrack` playback with per-track volume control (no more ADJUST_MUTE pops), and full pipeline ownership — no dependency on Google TTS voice availability or network queries.

Piper's 0.3x RTF means a 10-word reply renders in ~150ms. With a low-tier model (~20-30MB), total RAM impact alongside Gemma's 2.4GB is well within the Pixel 8's 8GB budget.

## Tasks

### Task 1 — sherpa-onnx AAR dependency + Gradle config

- [ ] `android/app/build.gradle.kts`: Add sherpa-onnx AAR dependency
  ```kotlin
  implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx-android:1.10.0")
  ```
  Note: verify the exact latest version at https://github.com/k2-fsa/sherpa-onnx/releases
- [ ] `android/app/build.gradle.kts`: Add `.onnx` to `noCompress` list:
  ```kotlin
  androidResources { noCompress += listOf(".onnx") }
  ```
- [ ] `android/app/build.gradle.kts`: If `mavenCentral()` or `jitpack.io` is not in `repositories`, add it (sherpa-onnx publishes to JitPack)
- [ ] Gradle sync → verify no dependency resolution errors

### Task 2 — Voice model selection and bundling

- [ ] Research and select a TARS-appropriate Piper voice from community models:
  - Priority: `en_GB-semaine-medium` (British, calm, measured — best TARS register fit)
  - Fallback: `en_US-lessac-medium` (American, authoritative)
  - Fallback: `en_US-ljspeech-high` (can pitch-adjust down)
- [ ] Download selected `.onnx` model file (~20-30MB for low tier) and companion `.json` config
- [ ] Place in `android/app/src/main/assets/piper/`:
  - `en_GB-semaine-medium.onnx`
  - `en_GB-semaine-medium.json`
- [ ] Add to `.gitignore` if models are too large for repo, or use `git lfs`
- [ ] Verify `aapt` does not compress `.onnx` during APK build (test with `aapt list` on APK)

### Task 3 — PiperBackend.kt

- [ ] New file `voice/PiperBackend.kt` in the package
- [ ] Implement class `PiperBackend` with:
  - `init(context: Context)`: Load ONNX model from assets via `OfflineTts`:
    ```kotlin
    val config = OfflineTtsConfig(
        model = OfflineTtsModelConfig(
            model = assetPath,
            tokens = jsonPath,
            numThreads = 4,
            provider = "cpu"
        ),
        modelType = "tts"
    )
    val tts = OfflineTts(config)
    ```
  - `generate(text: String): ByteArray?` — calls `tts.generate(text, sid, speed)`, returns PCM raw audio (16-bit, 16kHz by default)
  - `play(audio: ByteArray, sampleRate: Int = 16000)` — creates `AudioTrack` with explicit 48kHz output (Android hardware native), writes PCM data with per-track `VolumeShaper` fade (50ms ramp)
  - Model warm-up: after init, generate a 3-word test phrase (~400ms first-inference overhead for ONNX JIT)
  - `shutdown()` — release `OfflineTts`, release `AudioTrack`
- [ ] Handle errors gracefully: if model fails to load, return `false` from `isAvailable()` — caller falls back to Android TTS

### Task 4 — Refactor CreatureVoice.kt

- [ ] Add `PiperBackend` as a field alongside existing Android `TextToSpeech`
- [ ] Rewrite `say()` to use Piper as primary TTS engine:
  1. `PiperBackend.generate(text)` → PCM byte array
  2. `PiperBackend.play(pcm)` → streaming `AudioTrack` with per-track fade
  3. Eyes shimmer while speaking (same `setSpeaking(true/false)` pattern)
- [ ] Keep Android `TextToSpeech` as fallback for:
  - Edge case: Piper model not loaded
  - Very long utterances (if Piper batch synthesis becomes a concern)
  - Low-RAM scenarios where every MB counts
- [ ] Remove `pickVoice()` — voice selection is now the bundled Piper model
- [ ] Remove `setPitch()` / `setSpeechRate()` calls — select a naturally low-pitch Piper voice instead
- [ ] Update `shutdown()` to also shut down `PiperBackend`

### Task 5 — Settings UI update

- [ ] `SettingsActivity.kt` or relevant settings view: Replace `TextToSpeech` voice picker with Piper voice list
- [ ] List available `.onnx` models found in assets/piper/
- [ ] For now: single voice selector (the bundled model). Future: multi-model download.

### Task 6 — Tests

- [ ] Add `PiperBackendTest.kt`:
  - Verify `generate()` returns non-null PCM data with valid header
  - Verify model load from assets succeeds (or gives clear diagnostic if missing)
  - Verify fallback to Android TTS when model is absent
- [ ] Update `CreatureVoice` test coverage:
  - Verify Piper is used as primary when available
  - Verify Android TTS fallback fires when Piper is unavailable
- [ ] Run `:app:testDebugUnitTest` green

### Task 7 — On-device verification

- [ ] Deploy APK with bundled Piper model
- [ ] Measure first-token latency (expect ~400ms warm, ~150ms steady)
- [ ] Listen for audio artifacts — verify no popping/crackling with per-track `VolumeShaper`
- [ ] Test 5 short replies (5-15 words), 1 long utterance (30+ words)
- [ ] logcat clean of sherpa-onnx errors
- [ ] Verify total RAM usage alongside Gemma < 3GB

### Task 8 — Merge

- [ ] Update PROJECT_INDEX.md with Phase 2 completion
- [ ] Merge `voice-phase2-piper-tts` → `main`, delete branch