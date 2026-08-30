---
title: "LLM Backend Replacement: Gemma 4 E2B → MLC LLM + Llama-3.2-3B"
date: 2026-08-31
status: superseded
supersedes: docs/06-specs/2026-08-17-gemma-backend-design
superseded-by: _tasks/20260831-008-llm-default-to-agent
---

# Plan: Replace Gemma 4 E2B with MLC LLM + Llama-3.2-3B

## Motivation

Gemma 4 E2B via LiteRT-LM 0.16.0 is unreliable on Pixel 8 (Tensor G3):

1. **Native engine corruption** — `INTERNAL: Failed to invoke the compiled model` after 2-3 successful replies; requires full 2.4GB model reload (10-15s)
2. **CPU-only** — Mali-G715 GPU path has a number/contraction mangling bug (LiteRT-LM issue #2202)
3. **Google account / sideload dependency** — model pushed via ADB, not bundled; no automatic download path
4. **Large memory footprint** — 2.4GB resident, leaving ~5.6GB for Android 17 + app

## Proposed Solution

Replace Gemma with **MLC LLM runtime + Llama-3.2-3B (INT4)**:

| Metric | Gemma 4 E2B (current) | Llama-3.2-3B via MLC (proposed) |
|--------|----------------------|--------------------------------|
| Model size | 2.4 GB | ~1.6-1.8 GB (INT4) |
| Runtime | LiteRT-LM 0.16.0 | MLC LLM (TVM-based) |
| GPU support | Broken (issue #2202) | Vulkan compute shaders |
| Google account | No (sideloaded) | No (bundled or downloaded) |
| Tool calling | Prompt-based | Structured `<tool>` tag parsing |
| Streaming | No | Token-level streaming |
| Community | Closed, Google-only | Open, GGUF ecosystem |

## Phased Implementation

### Phase 1: Evaluate MLC LLM on Pixel 8 (3-5 days)

- [ ] Build MLC LLM Android AAR from source (or use prebuilt)
- [ ] Create `MlcBackend.kt` implementing `LlmBackend` interface
- [ ] Load Llama-3.2-3B INT4 model on device
- [ ] Benchmark: inference latency, memory usage, reliability (100-turn stress test)
- [ ] Compare against current Gemma baseline

**Decision gate:** If MLC LLM is stable (>95% success rate over 100 turns), proceed to Phase 2. If also unstable, investigate alternative runtimes (llama.cpp Android, ExecuTorch).

### Phase 2: Streaming & Tool-Calling Pipeline (3-5 days)

- [ ] Implement token-level streaming from MLC LLM
- [ ] Build `<tool>` tag interceptor in Reply routing
- [ ] Wire tool execution to BLE bridge + face expression system
- [ ] Implement strict JSON tool-calling system prompt

### Phase 3: Bundling & Production (2-3 days)

- [ ] Bundle model as APK asset or first-run download
- [ ] Remove LiteRT-LM dependency + GemmaBackend
- [ ] Update settings UI (remove GEMMA backend option, add MLC)
- [ ] Full regression test suite

## Dependencies & Risks

| Risk | Mitigation |
|------|------------|
| MLC LLM Android build broken | Fall back to llama.cpp Android (termux or NDK) |
| Vulkan driver bugs on Tensor G3 | CPU fallback in MLC config |
| 1.8GB download too large for first run | Progressive download with resume; model bundled in APK variant |
| Tool-calling prompt breaks 3B model | Iterative prompt engineering + streaming buffer validation |

## Backward Compatibility

Existing `GemmaBackend.kt` and LiteRT-LM dependency stay in the codebase during Phase 1 evaluation. `LlmBackend` interface unchanged — `MlcBackend` implements the same contract. Settings UI gains a new `MLC` backend option alongside `GEMMA` (deprecated).

## References

- [MLC LLM Android docs](https://llm.mlc.ai/docs/deploy/android.html)
- Llama-3.2-3B Instruct (INT4 GGUF)
- [[docs/06-specs/2026-08-17-gemma-backend-design]] (current Gemma spec — to be superseded)
- [[docs/03-decisions/001-pixel8-brain]] (Pixel 8 as brain ADR)