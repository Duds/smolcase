# Voice System Phase 1 — Fix Popping & Stream-Level Muting

**Source:** [[docs/01-research/SMOLCASE-Voice-System-Deep-Research]] §5.1
**Branch:** `voice-phase1-audio-fix`
**Started:** 2026-08-31
**Related:** [[AGENTS]] §3 (toolchains), [[docs/07-plans/2026-08-31-voice-phase2-piper-tts]] (next phase)

## Context

Four contributing mechanisms cause the audible speaker popping/crackling. This plan addresses the two highest-impact items (stream muting and voice preference ordering) and sets the stage for the Piper TTS replacement in Phase 2.

## Tasks

### Task 1 — Per-track volume fade (replace ADJUST_MUTE)

- [ ] `VoiceEars.kt`: Remove `applyStreamMute()` method entirely
- [ ] `VoiceEars.kt`: Remove `AudioManager` import and `audio` field — `VoiceEars` should only manage the `SpeechRecognizer` lifecycle
- [ ] `VoiceEars.kt`: Remove all `applyStreamMute()` calls from `start()`, `toggleMute()`, `stop()`
- [ ] `VoiceEars.kt`: Remove `allowSpeech()` method — move this responsibility to `CreatureVoice`
- [ ] `CreatureVoice.kt`: Add per-track `AudioTrack` volume control — use `VolumeShaper` (API 33+) with 50ms linear fade ramp for zero-transient gain transitions
- [ ] `CreatureVoice.kt`: Handle `allowSpeech()` via `AudioTrack.setVolume()` fade instead of stream-level muting

### Task 2 — Decouple VoiceEars from audio routing

- [ ] `VoiceEars.kt`: Strip all audio routing concerns — it only keeps `SpeechRecognizer`, cooldown gate, and `listen()` loop
- [ ] `VoiceEars.kt`: `start()` no longer mutes anything — just begins listening
- [ ] `VoiceEars.kt`: `stop()` no longer unmutes — just destroys recognizer
- [ ] `VoiceEars.kt`: `toggleMute()` only controls recognizer lifecycle, not stream muting
- [ ] `ConversationEngine.kt` (or wherever `VoiceEars.allowSpeech()` is called): update wiring — `CreatureVoice` handles its own audio routing internally

### Task 3 — Invert pickVoice() to prefer local voices

- [ ] `CreatureVoice.kt`: Change `pickVoice()` voice sorting — invert `sortedBy { it.isNetworkConnectionRequired }` so `*-local` voices sort before `*-network` voices
- [ ] `CreatureVoice.kt`: Update `MALE_VOICE_IDS` list order — put local variants first in each pair (already done? verify `en-us-x-iom-local` is before `en-us-x-iom-network`)

### Task 4 — Remove cloud TTS from the hot path

- [ ] `CreatureVoice.kt`: Change `say()` — make local TTS the primary, cloud TTS a configurable enhancement
- [ ] `CreatureVoice.kt`: `say()` fires `tts.speak()` immediately (no cloud attempt first), then optionally calls `cloudTts.speak()` as a low-priority enhancement that replaces local if it completes in time
- [ ] `CloudTtsBackend.kt`: No structural changes needed — the callback-based API already supports this pattern
- [ ] `ConversationEngine.kt` or relevant: Add settings toggle "Cloud TTS enhancement" (off by default)

### Task 5 — Tests

- [ ] Add test: verify `pickVoice()` returns a local voice when both local and network are available
- [ ] Add test: verify `say()` calls local TTS first (not cloud)
- [ ] Run `:app:testDebugUnitTest` green
- [ ] Controlled audio pop test: play TTS 10x with new per-track fade, record output, verify no audible transients

### Task 6 — Merge

- [ ] Update PROJECT_INDEX.md with Phase 1 completion
- [ ] Merge `voice-phase1-audio-fix` → `main`, delete branch