# Voice System Phase 3 — Streaming Pipeline (LLM → Speaker)

**Source:** [[docs/01-research/SMOLCASE-Voice-System-Deep-Research]] §5.3
**Branch:** `voice-phase3-streaming`
**Started:** 2026-08-31
**Related:** [[docs/07-plans/2026-08-31-voice-phase2-piper-tts]] (pre-requisite), [[docs/06-specs/2026-08-17-gemma-backend-design]] (LiteRT-LM API), [[AGENTS]] §4.2 (GemmaBackend data flow)

## Context

The current pipeline is serial: wait for the LLM to finish generating the full reply, then synthesise the entire utterance into speech, then play it. For a 20-word reply this means 3-8s of LLM time + 0.5-2s of TTS time before the user hears anything.

Phase 3 eliminates this dead air by streaming: the LLM produces tokens incrementally, a sentence boundary detector fires complete sentences to Piper as soon as they're ready, and Piper begins speaking sentence 1 while the LLM is still generating sentence 2. The goal is zero dead air between the LLM's first meaningful token and the speaker's first audible word.

Key unknown: whether LiteRT-LM 0.16.0 supports token-level streaming callbacks. This must be investigated in Task 1 before designing the streaming architecture.

## Tasks

### Task 1 — Investigate LiteRT-LM streaming API

- [ ] Read `litertlm-android:0.16.0` API docs: does `LiteRuntimeLm.Conversation.predict()` support per-token callbacks?
- [ ] If yes: determine callback signature, thread safety, and cancellation semantics
- [ ] If no: design sentence-level heuristic (time-based flush of partial text on sentence boundaries)
- [ ] If partial support (non-standard): document the workaround
- [ ] Create ADR or research note documenting the streaming capability decision

### Task 2 — Token-level callbacks in GemmaBackend

- [ ] `GemmaBackend.kt`: Add optional token callback to `reply()`:
  ```kotlin
  fun reply(text: String, onToken: ((String) -> Unit)? = null): String?
  ```
- [ ] If LiteRT-LM supports streaming: wire `onToken` to the per-token callback from the inference engine
- [ ] If streaming not supported: implement sentence-boundary detection by running `.predict()` with incremental context — after each sentence, flush to callback and continue (less efficient but meets the streaming goal)
- [ ] Ensure cancellation works: if the user speaks again mid-utterance, stop the current stream
- [ ] Update `GemmaBackendTest.kt`:
  - Add streaming callback test (mocked/predictable output)
  - Verify cancellation works

### Task 3 — SentenceBuffer.kt

- [ ] New file `voice/SentenceBuffer.kt`:
  - Collects incoming tokens from `onToken` callback
  - Detects sentence boundaries: `.`, `!`, `?`, followed by whitespace or end-of-string
  - Emits complete sentences to a consumer callback: `onSentence: (String) -> Unit`
  - Handles edge cases:
    - Abbreviations ("Dr.", "Mr.", "etc.") — maintain a small abbreviation set to avoid false splits
    - Numbers with decimals ("3.14") — handled by abbreviation heuristic
    - Very long sentences without boundaries (>100 chars) — force-flush at comma or clause boundary
    - End of LLM stream — flush remaining buffer content as final sentence
- [ ] `SentenceBuffer` is stateless — call `append(token)`, listen for `onSentence` callbacks
- [ ] Add `SentenceBufferTest.kt`:
  - Normal sentence split ("Hello. How are you?" → ["Hello.", "How are you?"])
  - Abbreviation handling ("Dr. Smith said...")
  - Force flush on long gap
  - Single-sentence flush on stream end

### Task 4 — Wire sentence buffer to Piper streaming

- [ ] `PiperBackend.kt`: Add streaming `play()` variant that takes `ByteArray` chunks and concatenates them gaplessly via `AudioTrack.write()`
  - Pre-warm an `AudioTrack` in PLAY state
  - Each `write()` call writes PCM data directly — no gaps between chunks
  - Handle underrun: if next chunk isn't ready yet, write silence buffer (~50ms) to prevent `AudioTrack` from stopping
- [ ] `CreatureVoice.kt`: Wire the streaming pipeline:
  ```kotlin
  fun sayStreaming(
      text: String,
      onToken: ((String) -> Unit)?,
      onStreamEnd: (() -> Unit)?
  ) {
      val buffer = SentenceBuffer { sentence ->
          val pcm = piper.generate(sentence)
          piper.playStreaming(pcm)
      }
      // Feed tokens as they arrive from LLM
      llmBackend.reply(text) { token ->
          buffer.append(token)
      }
      buffer.flush() // flush remaining on stream end
  }
  ```
- [ ] Ensure edge cases:
  - Abandon previous stream if `sayStreaming()` is called again (user interrupts)
  - `AudioTrack` resource cleanup on shutdown
  - `onStreamEnd` fires after last sentence finishes playing (not after LLM finishes generating)

### Task 5 — Tests

- [ ] Add `SentenceBufferTest.kt`: comprehensive sentence boundary detection
- [ ] Add `PiperBackendTest.kt`: streaming write concatenation verification
- [ ] Add `CreatureVoiceTest.kt`: streaming pipeline integration test with mock LLM and mock Piper
- [ ] Run `:app:testDebugUnitTest` green

### Task 6 — On-device verification

- [ ] Deploy Phase 3 build
- [ ] Ask multi-sentence question → verify playback starts before LLM finishes generating
- [ ] Verify no audible gaps between sentences
- [ ] Verify interruption (speak mid-reply) → old stream cancels cleanly
- [ ] Measure: time from first token output to first audible word (target: <200ms)
- [ ] logcat: verify no `AudioTrack` underrun warnings
- [ ] Verify GeminiNano and Kimi backends also benefit from streaming (if their APIs support it)

### Task 7 — Merge

- [ ] Update PROJECT_INDEX.md with Phase 3 completion
- [ ] Merge `voice-phase3-streaming` → `main`, delete branch