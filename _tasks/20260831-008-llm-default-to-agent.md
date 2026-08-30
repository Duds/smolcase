---
id: 20260831-008
title: "Default LLM backend to AGENT (cloud, OpenRouter)"
type: wayfinder:decision
status: ✅ Complete
date: 2026-08-31
supersedes: docs/07-plans/2026-08-31-mlc-llm-replacement
--- 

# Decision: Default LLM Backend → AGENT (Cloud)

## Context

Three on-device LLM backends were evaluated on Pixel 8 (Tensor G3) and all were found unreliable:

| Backend | Runtime | Status | Failure mode |
|---------|---------|--------|-------------|
| **Gemma 4 E2B** | LiteRT-LM 0.16.0 | 🔴 Unstable | Native engine crash after 2-3 replies (`INTERNAL: Failed to invoke the compiled model`). GPU path broken (issue #2202). CPU-only, 2.4GB model, 10-15s reinit. |
| **Gemini Nano** | ML Kit GenAI Prompt (AICore) | 🔴 Untestable | `DOWNLOADABLE` status — one-time Google model download never triggered or completed. |
| **MLC LLM + Llama-3.2-3B** | TVM-compiled | 🔴 High risk | Confirmed crash on Pixel 8 when loading Llama-3.2 models (mlc-ai/mlc-llm#3442). 4-8 week integration effort. |

## Decision

Ship the robot with **AGENT backend as default** — cloud LLM via OpenRouter (OpenAI-compatible API).

**Why not RULES (offline)?** RULES produces only canned greetings/dial adjustments — no personality, no conversation. The robot's core appeal is TARS-character responses.

**Why AGENT over continued on-device pursuit?** All three on-device paths are blocked by Tensor G3 stability issues at this time. Cloud inference gives us:
- Reliable, fast responses (~1-3s vs ~20s on-device)
- Full multi-turn context (12-15 turns vs 1 turn with Gemma)
- Model flexibility (swap models via API without rebuilding APK)
- Zero model size impact on device storage/RAM

## Changes Made

| File | Change |
|------|--------|
| `android/.../llm/LlmSettings.kt:29` | Default backend: `RULES` → `AGENT`. GEMMA auto-migrates to AGENT on read. |
| `android/.../SettingsActivity.kt` | Radios reordered: AGENT first, then NANO, GEMMA (greyed with "(unstable)"), RULES last. Help text for API key requirement. |
| `android/.../ConversationEngine.kt:89` | If AGENT backend selected but API key not configured: "I can't answer that yet — set up an API key in Settings." |
| `android/app/src/test/.../LlmSettingsTest.kt` | Test updated to reflect AGENT as default. |

## Defaults

| Setting | Value |
|---------|-------|
| Base URL | `https://openrouter.ai/api/v1` |
| Default model | `mistralai/mixtral-8x22b-instruct` |
| Max tokens | 200 |
| Temperature | 0.80 |

## Next Steps

- [ ] Enter OpenRouter API key on device (long-press face → Settings → Agent form)
- [ ] Consider smaller/cheaper model for robot replies (e.g. `mistralai/mistral-small-24b-instruct-2501`)
- [ ] Re-evaluate on-device LLM options if Tensor G4/Mali driver improvements become available