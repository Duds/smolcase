# GOAL.md — SMOLCASE Destination Contract

> **Purpose**: Define the current destination, success criteria, and constraints.
> The Wayfinder Map at [[docs/07-plans/wayfinder-map]] holds the decision tickets.
> The task board at [[TASKS]] tracks execution.
> See [[AGENTS]] for conventions and gotchas.

---

## Current Objective

> **Ship a desk-ready SMOLCASE robot that walks, talks, and responds autonomously — integrating the Android brain/face with a physical 3D-printed body, 2x 360° serial-bus servos, and an ESP32 BLE bridge.**

---

## Success Criteria

```
1. SC-CASE, SC-LEG-L, SC-LEG-R 3D-printed and assembled
2. 2x 360° serial-bus servos installed, BLE-commandable from Pixel 8
3. Robot walks forward/backward reliably on desk surface
4. Android face (dot matrix eyes + TARS persona) runs during movement
5. Tests pass: ./gradlew :app:testDebugUnitTest && assembleDebug
6. Wayfinder map fully resolved (all tickets closed)
```

---

## Constraints

- Exactly **2 legs, 2 servos** ([[docs/03-decisions/005-case-architecture-2leg\|ADR-005]]) — never more
- Eyes stay in upper screen half ($Y \le 0.50$), solid LED clusters (no pupils/glint) — see [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]]
- TARS register: deadpan, dry wit, no emojis, military commander / coach
- LiteRT-LM stays at `0.16.0`; GemmaBackend.kt is stable — see [[docs/06-specs/2026-08-17-gemma-backend-design]]
- All hardware decisions documented as ADRs in [[docs/03-decisions/]]

---

## References

| Document | Purpose |
|----------|---------|
|| [[docs/07-plans/wayfinder-map\|Wayfinder Map]] | Decision tickets for remaining unknowns |
|| [[TASKS]] | Execution task board |
|| [[AGENTS]] | Codebase conventions, gotchas, commands |
|| [[PROJECT_INDEX]] | Central index, status board, discovery archive |
|| [[docs/03-decisions/]] | Architecture Decision Records (ADR-001 through ADR-005) |
|| [[docs/06-specs/]] | Approved design specs (Eyes, Gemma, Settings) |
|| [[docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan]] | 30-behaviour training curriculum |