# Gemma Backend — Implementation Plan

**Spec:** [[docs/06-specs/2026-08-17-gemma-backend-design]]
**Branch:** `gemma-backend` → merge to `main` when green on-device
**Started:** 2026-08-17
**Related:** [[docs/06-specs/2026-08-29-settings-expansion-design]] (AgentBackend generalization), [[AGENTS]] §5.1 (LiteRT-LM gotchas)

## Tasks

### Task 1 — Model file (runs in parallel with code work)
- [ ] `curl -L -C -` resume download of `gemma-4-E2B-it.litertlm` (2.41 GiB) into `models/`
- [ ] Verify size == 2,588,147,712 bytes
- [ ] `models/*.litertlm` in .gitignore (done on branch creation)

### Task 2 — Gradle dependency
- [ ] `android/app/build.gradle.kts`: add `implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.0")`
- [ ] Sync/build resolves from Google Maven

### Task 3 — GemmaBackend
- [ ] New file `llm/GemmaBackend.kt` implementing `LlmBackend`
- [ ] Model path: `getExternalFilesDir(null)/models/gemma-4-E2B-it.litertlm`
- [ ] init: async probe — file missing → actionable reason; present → `engine.initialize()` on Dispatchers.Default
- [ ] reply(): fresh `Conversation` per call, `CreaturePersona.prompt(dials)` as systemInstruction, soul context + user text as message; null + stored reason on failure
- [ ] `close()` on engine in a `shutdown()` (called from MainActivity.onDestroy)

### Task 4 — Settings + wiring
- [ ] `LlmSettings.Backend.GEMMA`
- [ ] `ConversationEngine`: construct GemmaBackend, add GEMMA branch, statusLine label `GEMMA`
- [ ] `SettingsActivity`: fourth radio "Gemma 4 E2B (on-device, sideloaded)"

### Task 5 — Tests
- [ ] Backend enum round-trip test (GEMMA persists)
- [ ] statusLine() GEMMA label test
- [ ] GemmaBackend missing-file reason test (JVM-safe, no engine load)
- [ ] Full `:app:testDebugUnitTest` green

### Task 6 — On-device verification
- [ ] `adb push` model to `/sdcard/Android/data/com.smolcase.companion/files/models/`
- [ ] Deploy, select GEMMA in Settings
- [ ] Ask a non-canned question → real reply in TARS register
- [ ] Screenshot: telemetry shows `LLM GEMMA OK`
- [ ] logcat clean of SmolcaseLLM warnings

### Task 7 — Merge
- [ ] Update PROJECT_INDEX.md status
- [ ] Merge `gemma-backend` → `main`, delete branch
