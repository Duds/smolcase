---
id: 20260830-009
title: "Spec: First-start wakeup / onboarding routine"
type: wayfinder:spec
status: open
blocked-by: [20260830-008]
---

## Question

What is the spec for the first-ever boot sequence when `Arrival.isFirstEver == true`, replacing the current single-line BIRTH greeting with a proper multi-turn onboarding flow?

**Design notes in:** `.workspace-scratchpad.md` §Wakeup Routine

**Scope:**
- Replace BIRTH branch in `CreatureBrain.greet()` with multi-step flow
- Boot animation sequence (matrix sweep-in, eyes blink open, ALERT state, self-test pips)
- Voice onboarding dialogue: name → owner name → honesty calibration
- Persist `creature_name`, `owner_name`, `wakeup_complete` in `smolcase_soul`
- New `WakeupHandler` transient mode in `ConversationEngine` — routes speech input during onboarding instead of LLM dispatch
- `MemoryStore` additions: `isWakeupComplete()`, `completeWakeup()`, `creatureName()`, `ownerName()`

**Resolution:** Approved spec in `docs/06-specs/` with implementation plan in `docs/07-plans/`.