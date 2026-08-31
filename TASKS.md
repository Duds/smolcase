# TASKS.md — SMOLCASE Expedition Task Board

> Living task board. Tasks use `YYYYMMDD-NNN` IDs.
> Decision tickets live on the [[docs/07-plans/wayfinder-map|Wayfinder Map]].
> Detail files in `_tasks/YYYYMMDD-NNN-slug.md`.
> See [[docs/06-specs/]] for approved specs and [[AGENTS]] §8 for link conventions.

---

## 🐛 Known Bugs

| ID | Bug | Severity | Status |
|----|-----|----------|--------|
| [[_tasks/20260830-001-directives-repetition|20260830-001]] | LLM parrots "directives" from system prompt | 🔴 High | ✅ Fixed — prompt reworded, word ban filter added |
| [[_tasks/20260830-002-latency-degradation|20260830-002]] | Latency degrades from 3.5s → 10s+ over session | 🔴 High | ✅ Fixed — `maxNumTokens` 1024→512 |
| [[_tasks/20260830-003-memory-recall|20260830-003]] | Memory facts not recalled across turns | 🟡 Medium | ✅ Fixed — removed meta-framing, memory as natural context |
| [[_tasks/20260830-004-dials-ineffective|20260830-004]] | Humor/honesty dials have no observable effect | 🟡 Medium | ✅ Fixed — stronger dial framing at end of prompt |
| [[_tasks/20260830-005-no-multi-turn|20260830-005]] | Multi-turn context not maintained | 🟡 Medium | ✅ Fixed — persistent Conversation → per-turn + manual exchange |
| [[_tasks/20260830-006-cooldown-inconclusive|20260830-006]] | Cooldown may let utterances through (~4s window) | 🟢 Low | Needs controlled re-test |
| — | Gemma 4 E2B native engine corruption on Pixel 8 (Tensor G3) | 🔴 High | 🔴 Wont fix — see [[_tasks/20260831-008-llm-default-to-agent]] |
| [[_tasks/20260831-009-settings-contrast-bug|20260831-009]] | Settings text input fields fail WCAG AA contrast on black | 🟡 Medium | 🔴 Open — border line 1.8:1, button bg 1.3:1 |
| [[_tasks/20260831-010-settings-ux-redesign-spike|20260831-010]] | Settings UX redesign spike — multi-page, tabs, styling | 🟡 Medium | 🔵 Spike — brainstorm only |

---

## 🔥 Now

> Hardware work is on hold (per request). See 🟡 On Hold section.
> Recommended: implement reset-to-scratch factory reset (spec+plan ready).

---

## 📋 Next

### Process improvements (from coherence review)

| ID | Task | Risk | Blocked By |
|----|------|------|------------|
| 20260830-010 | [[_tasks/20260830-010-risk-tagging|Add risk-level tagging to task template]] | 🟢 Low | — |
| 20260830-011 | [[_tasks/20260830-011-sim-baseline-test|Create sim baseline test script]] | 🟢 Low | — |
| 20260830-013 | [[_tasks/20260830-013-conversation-regression|Wire conversation regression test into process]] | 🟢 Low | — |
| 20260830-014 | [[_tasks/20260830-014-contradiction-log|Establish contradiction log practice]] | 🟢 Low | — |

### Voice System Refactoring (4-Phase Roadmap)

| ID | Task | Risk | Phase |
|----|------|------|-------|
| 20260831-001 | Phase 1: per-track volume fade, decouple VoiceEars, invert voice preference | 🟢 Complete | 1 |
| 20260831-002 | Phase 2: Piper TTS via sherpa-onnx, PiperBackend.kt | 🟡 Medium | 2 |
| 20260831-003 | Phase 3: token streaming, SentenceBuffer, gapless playback | 🟡 Medium | 3 |
| 20260831-004 | Phase 4: zero-shot TARS voice cloning with Pocket TTS | 🟢 Low | 4 (deferred) |

### Specs to write

| ID | Task | Blocked By | Notes |
|----|------|------------|-------|
| 20260830-008 | [[_tasks/20260830-008-reset-to-scratch|Implement reset-to-scratch factory reset]] | — | Spec [[docs/06-specs/2026-08-30-reset-to-scratch-design|written]] + [[docs/07-plans/2026-08-30-reset-to-scratch|plan]] ready |
| 20260830-009 | [[_tasks/20260830-009-wakeup-routine|Implement first-start wakeup / onboarding]] | 20260830-008 | Spec [[docs/06-specs/2026-08-30-wakeup-onboarding-design|written]] + [[docs/07-plans/2026-08-30-wakeup-onboarding|plan]] ready |

---

## 🔴 Blocked

> All hardware items moved to 🟡 On Hold. No active blockers.

---

## 🟡 On Hold (Hardware)

| ID | Ticket | Blocked By |
|----|--------|------------|
| 20260829-003 | [[_tasks/20260829-003-servo-selection|Decide serial-bus servo model]] | — |
| 20260829-004 | [[_tasks/20260829-004-sc-case-cad-design|Design SC-CASE chassis and leg geometry in CAD]] | 20260829-003 |
| 20260829-005 | [[_tasks/20260829-005-esp32-battery|Decide ESP32 module and battery architecture]] | 20260829-003 |
| 20260829-006 | Train Walk Backward PPO policy | 20260829-004 |
| 20260829-007 | [[_tasks/20260829-007-esp32-ble-bridge-firmware|Implement ESP32 BLE-to-serial-bus bridge firmware]] | 20260829-003, 20260829-005 |
| 20260829-008 | [[_tasks/20260829-008-tflite-on-device-pipeline|TFLite policy export and on-device inference pipeline]] | 20260829-006 |
| 20260829-009 | [[_tasks/20260829-009-behaviour-arbiter-integration|Integrate behaviour arbiter]] | 20260829-007, 20260829-008 |
| 20260829-010 | [[_tasks/20260829-010-30-behaviour-suite|Full 30-behaviour training suite]] | 20260829-006 |
| 20260830-012 | [[_tasks/20260830-012-assembly-checklist|Create hardware assembly checklist]] | 20260829-004 |

---

## 📦 Backlog

### Voice System Refactoring (4-Phase Roadmap)

| ID | Task | Risk | Phase |
|----|------|------|-------|
| 20260831-001 | Phase 1: per-track volume fade, decouple VoiceEars, invert voice preference | 🟢 Complete | 1 |
| 20260831-002 | Phase 2: Piper TTS via sherpa-onnx, PiperBackend.kt | 🟡 Medium | 2 |
| 20260831-003 | Phase 3: token streaming, SentenceBuffer, gapless playback | 🟡 Medium | 3 |
| 20260831-004 | Phase 4: zero-shot TARS voice cloning with Pocket TTS | 🟢 Low | 4 (deferred) |

### Specs to write

- ~~[[_tasks/20260830-008-reset-to-scratch|Reset-to-scratch factory reset]]~~ → spec + plan written, ready for implementation
- ~~[[_tasks/20260830-009-wakeup-routine|First-start wakeup / onboarding]]~~ → spec + plan written, ready for implementation

### Hardware & engineering (on hold)

> These items are on hold pending hardware resumption.

- Characterise thermal dissipation of Pixel 8 inside closed SC-CASE (vent channels / heatsink) — see [[docs/04-hardware/SMOLCASE-Case-Design-Brainstorm]] §3.6
- Sim-to-real transfer validation after first physical build — see [[docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan]]
- Gait transition smoothness tuning (CPG + TFLite blend) — see [[docs/03-decisions/003-cpg-mujoco-tflite]]
- Mechanical part naming per [[docs/03-decisions/005-case-architecture-2leg|ADR-005]]: SC-POD-L/R, SC-RIB, SC-BLY, SC-SHL, SC-JAW
- BOM tracking in [[docs/04-hardware/README]]

---

## 🏁 Completed

| ID | Item | Evidence |
|----|------|----------|
| 20260830-001 through 005 | **Conversation bug fixes** — directives repetition, latency, memory recall, dials ineffective, multi-turn | All 5 fixed in one pass: prompt reworded, maxNumTokens 128, removed meta-framing, dials at end, persistent Conversation |
| 20260829-002 | [[_tasks/20260829-002-conversation-quality-fixes|Phase 7 conversation quality]] — all sub-items resolved; remaining bugs tracked separately | Superseded by 20260830-001 through 006 |
| 20260830-007 | [[_tasks/20260830-007-wiki-link-crossref|Cross-document wiki-link maintenance pass]] | 100+ `[[wiki-links]]` across 33 files; AGENTS.md §8 added |
| — | Gemma 4 E2B on-device backend — see [[docs/06-specs/2026-08-17-gemma-backend-design]] | LiteRT-LM 0.16.0, CPU backend, live inference on Pixel 8 |
| — | Expressive Appliance Dot Matrix Eyes — see [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]] | SDF rasterizer, mood state machine, telemetry pips, 20/20 tests, debug APK |
| — | Settings Expansion & Cloud Intelligence — see [[docs/06-specs/2026-08-29-settings-expansion-design]] | WCAG AA UI, cloud TTS/Vision/ReplyGen, Pixel sensors, all tests passing |
| — | ADR-001 through ADR-005 — see [[docs/03-decisions/]] | Architecture locked: Pixel 8 brain, hierarchical policies, CPG+MuJoCo+TFLite, ESP32 BLE, 2-leg CASE |
| — | MuJoCo simulation environment & CPG baseline | Working, verified in `sim/src/smolcase_train.py` |
| — | PPO Walk Forward policy — see [[models/README]] | +7.32m at 500K steps |
| — | Empirical eye analysis & photometric study — see [[docs/01-research/SMOLCASE-Robot-Eye-Libraries-Review]] | `docs/01-research/analysis/` |
| — | **Phase 1: Process framework** — AGENTS.md updated (skills table, template refs, session start/end, closure gate) | `AGENTS.md` |
| — | **Phase 1: Process framework** — STANDARDS.md created (document/code/session/decision standards, failure modes, testing doctrine) | `STANDARDS.md` |
| — | **Phase 1: Process framework** — Templates consolidated into `docs/templates/` | 8 templates moved |
| — | **Phase 1: Process framework** — PROJECT_INDEX.md updated with new refs | `PROJECT_INDEX.md` |
| — | **Phase 1: Process framework** — Process coherence review written | `docs/analysis/process-coherence-review.md` |
| — | **Voice System Deep Research** — Latency/popping/TTS-engine analysis | `docs/01-research/SMOLCASE-Voice-System-Deep-Research.md` |
| — | **Voice Phase 1 plan** — Audio fix (per-track fade, stream decouple, voice pref) | `docs/07-plans/2026-08-31-voice-phase1-audio-fix.md` |
| 20260831-001 | **Voice Phase 1 — implementation** | VoiceEars stripped of audio routing, CreatureVoice local-first say(), pickVoice() prefers local voices, cloud TTS off by default |
| — | **Voice Phase 2 plan** — Piper TTS via sherpa-onnx | `docs/07-plans/2026-08-31-voice-phase2-piper-tts.md` |
| — | **Voice Phase 3 plan** — Streaming LLM→speaker pipeline | `docs/07-plans/2026-08-31-voice-phase3-streaming.md` |
| — | **Voice Phase 4 plan** — TARS voice cloning with Pocket TTS | `docs/07-plans/2026-08-31-voice-phase4-tars-voice.md` |
| | 20260831-008 | **LLM backend: default to AGENT (cloud)** — all 3 on-device runtimes unstable on Tensor G3. Default changed to OpenRouter agent endpoint. | [[_tasks/20260831-008-llm-default-to-agent]] |