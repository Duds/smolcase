# GOAL.md — SMOLCASE Destination Contract

> **Purpose**: Define the current destination, success criteria, and constraints.
> The Wayfinder Map at [`docs/07-plans/wayfinder-map.md`](docs/07-plans/wayfinder-map.md) holds the decision tickets.
> The task board at [`TASKS.md`](TASKS.md) tracks execution.

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

- Exactly **2 legs, 2 servos** (ADR-005) — never more
- Eyes stay in upper screen half ($Y \le 0.50$), solid LED clusters (no pupils/glint)
- TARS register: deadpan, dry wit, no emojis, military commander / coach
- LiteRT-LM stays at `0.16.0`; GemmaBackend.kt is stable — do not modify
- All hardware decisions documented as ADRs in `docs/03-decisions/`

---

## References

| Document | Purpose |
|----------|---------|
| [Wayfinder Map](docs/07-plans/wayfinder-map.md) | Decision tickets for remaining unknowns |
| [TASKS.md](TASKS.md) | Execution task board |
| [AGENTS.md](AGENTS.md) | Codebase conventions, gotchas, commands |
| [PROJECT_INDEX.md](PROJECT_INDEX.md) | Central index, status board, discovery archive |
| `docs/03-decisions/` | Architecture Decision Records (ADR-001 through ADR-005) |