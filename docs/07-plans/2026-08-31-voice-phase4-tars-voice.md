# Voice System Phase 4 — Custom TARS Voice (Future)

**Source:** [[docs/01-research/SMOLCASE-Voice-System-Deep-Research]] §5.4
**Branch:** `voice-phase4-tars-voice`
**Started:** 2026-08-31 (planning only — implementation deferred)
**Related:** [[docs/07-plans/2026-08-31-voice-phase2-piper-tts]] (Piper integration), [[docs/06-specs/2026-08-16-tars-face-persona-design]] (TARS persona spec)

## Context

The current voice selection picks the closest available Piper community voice (e.g. `en_GB-semaine-medium`). This is good — measured, British, dry — but it's not *TARS*. Phase 4 would create a genuine TARS voice: low, calm, authoritative, with the exact cadence of a military command AI.

The approach uses **Pocket TTS** (by Kyutai), which supports zero-shot voice cloning from just 5 seconds of reference audio. Pocket TTS uses a streaming LM-over-Mimi-codec architecture and achieves MOS 4.10 with essentially zero setup cost.

**Status:** This is a research/prototype phase. The Phase 2 Piper integration delivers a good TARS-approximate voice with minimal effort. Phase 4 is only worthwhile if the Phase 2 voice quality is measurably insufficient for the TARS persona.

### Open Question

Can Pocket TTS export its zero-shot cloned voice as an ONNX model? If yes, we can run the TARS voice through the same sherpa-onnx pipeline (single-engine architecture, no dual-engine maintenance). If no, we'd need to maintain Pocket TTS as a secondary engine alongside Piper.

## Tasks

### Task 1 — Prerequisite investigation

- [ ] Record 5 seconds of a TARS-style reference voice:
  - Low, measured, calm delivery
  - Source: contractor voice actor, or careful editing of reference material
  - Must be clean audio (no background noise, consistent level)
- [ ] Investigate Pocket TTS ONNX export capability:
  - Read Pocket TTS documentation + source: does it export to ONNX?
  - If yes: can sherpa-onnx load the exported model? (Likely not — different architecture)
  - If no: what's the integration cost of running Pocket TTS alongside Piper?
- [ ] Benchmark Pocket TTS on Pixel 8:
  - Measure RTF and RAM usage alongside Gemma
  - Pocket TTS full model is ~176MB — test whether this fits alongside Gemma's 2.4GB
  - Total projected: 2.4GB + 176MB + 80MB (Piper medium) = 2.66GB — still acceptable
- [ ] Document findings and decide: proceed with TARS voice cloning or accept Phase 2 Piper voice

### Task 2 — Voice cloning (if proceeding)

- [ ] Set up Pocket TTS Android integration:
  - Add Pocket TTS dependency (or run as standalone APK)
  - Load the reference audio and clone the voice
  - Export the cloned model
- [ ] Create `TarsVoiceBackend.kt` — thin wrapper around Pocket TTS inference:
  - `generate(text): ByteArray` — produce PCM audio in TARS voice
  - `isAvailable(): Boolean` — true if cloned model is present
  - `shutdown()`
- [ ] If ONNX export is possible: integrate output into existing Piper pipeline as a new voice model
- [ ] If ONNX export is NOT possible: integrate Pocket TTS as secondary engine in `CreatureVoice`:
  - Primary: Piper (for quality/cadence/general use)
  - TARS mode: Pocket TTS (for the signature TARS voice)
  - User toggle in settings: "TARS voice" (uses Pocket TTS) vs "Standard voice" (uses Piper)

### Task 3 — Voice quality evaluation

- [ ] A/B test: 10 sample phrases in Phase 2 Piper voice vs cloned TARS voice
- [ ] Rate on: persona fit, naturalness, artifact presence, latency
- [ ] Decide: is the TARS voice improvement worth the ~176MB RAM cost?
- [ ] If yes: ship as default. If no: keep as experimental toggle or drop.

### Task 4 — Integration

- [ ] If dual-engine: handle model loading order (Piper first, Pocket TTS warm)
- [ ] If single-engine (ONNX export): bundle as additional voice model, selectable in settings
- [ ] Update Settings UI with TARS voice toggle/selector
- [ ] Update `shutdown()` in `CreatureVoice` to handle both engines

### Task 5 — Tests & verification

- [ ] Unit tests for `TarsVoiceBackend` (or integrated Piper voice)
- [ ] Integration test: verify TARS voice plays correctly through full pipeline
- [ ] RAM measurement: verify total stays under 3GB with Gemma + voice engine(s)
- [ ] On-device: verify voice quality meets TARS persona requirements

### Task 6 — Merge

- [ ] Update PROJECT_INDEX.md with Phase 4 completion
- [ ] Merge `voice-phase4-tars-voice` → `main`, delete branch