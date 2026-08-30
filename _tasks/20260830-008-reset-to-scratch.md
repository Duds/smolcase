---
id: 20260830-008
title: "Spec: Factory reset / reset-to-scratch for SMOLCASE soul"
type: wayfinder:spec
status: open
blocked-by: []
---

## Question

What is the official spec for wiping SMOLCASE back to a fresh soul (all memory, personality dials, conversation logs) while keeping a starter personality, and what UI/voice-triggered path invokes it?

**Design notes in:** `.workspace-scratchpad.md` §Reset to Scratch

**Scope:**
- Wipe `smolcase_soul` SharedPreferences (session count, total time, reminders, first-seen, usual arrival time)
- Wipe `smolcase_dials` SharedPreferences (humor, honesty, voice name → revert to defaults)
- Reset `smolcase_log` session counter to 0
- Delete conversation JSONL file
- Reset `EyeExpressionState` in-memory mood to NEUTRAL
- Reset `TarsFaceView` in-memory face state (lastFaceAt, gaze, saccade, blink timers)
- Restart `CreatureBrain` with fresh MemoryStore
- Restart `ConversationLog` at session 1

**Resolution:** Approved spec in `docs/06-specs/` with implementation plan in `docs/07-plans/`.