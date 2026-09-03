# ADR-002: Hierarchical Policy Architecture

## Status
Accepted — 2026-08-06

## Context
GrowBot uses a monolithic neural policy trained end-to-end. We needed to decide whether to follow this or use a different architecture given our expanded behaviour suite (30+ behaviours vs GrowBot's core gaits).

## Decision
Use a **hierarchical policy architecture**:
- **~20 small TFLite models**, one active at a time
- Each model is a specialist (walk forward, turn left, recover from fall, etc.)
- A **behaviour arbiter** selects which model is active based on high-level intent
- A **CPG reflex layer** runs underneath for rhythmic baseline and emergency reflexes

## Consequences

### Positive
- **Modular training**: Train each behaviour independently, no catastrophic forgetting
- **Swappable behaviours**: Add new gaits without retraining everything
- **Interpretable**: You know exactly which model is running and why
- **Resource efficient**: Only one small model in memory at a time (~50-150KB each)
- **Failure isolation**: One bad model doesn't break everything

### Negative
- **Arbiter complexity**: Need logic to blend between models smoothly
- **No emergent creativity**: Behaviours are explicit, not discovered
- **More total training time**: 20 models × 500K steps vs one model × 2M steps
- **State machine risk**: Arbiter can become a brittle state machine

## Architecture Diagram

```
High-Level Intent
       │
       ▼
┌──────────────────┐
│ Behaviour Arbiter│ ← Selects active policy
│ (LLM / rules)    │
└────────┬─────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐  ┌───────┐  ┌───────┐
│ Walk  │  │ Turn  │  │ Recover│ ... ~20 models
│Fwd    │  │Left   │  │Fall   │
│TFLite │  │TFLite │  │TFLite │
└───┬───┘  └───┬───┘  └───┬───┘
    │          │          │
    └────┬─────┴─────┬────┘
         ▼           ▼
┌──────────────────────────┐
│ CPG Reflex Layer         │
│ (rhythmic baseline)      │
└────────────┬─────────────┘
             │ BLE
             ▼
┌──────────────────────────┐
│ ESP32 → Servos           │
└──────────────────────────┘
```

## Related
- [[docs/03-decisions/003-cpg-mujoco-tflite\|ADR-003: CPG vs MuJoCo vs TFLite]] — three-layer control stack
- [[docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan]] — 30-behaviour curriculum
- [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]] — 2-leg body matches hierarchical simplicity
