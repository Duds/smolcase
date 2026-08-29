# GOAL.md — SMOLCASE Long-Running Autonomous Agent Protocol

> **Purpose**: Enable multi-session autonomous work on SMOLCASE goals.
> This file is the persistent state contract between agent sessions.
> Agents read it at session start, update it during work, and write a handoff before ending.

---

## 1. Architecture: How Long-Running Goals Work

### The Problem

Single agent sessions have hard limits:
- **Context overflow**: ~60-80% of context window is usable; beyond that, compounding errors accelerate
- **Realistic ceiling**: 2-4 hours of active coding (200K window), 6-12 hours (1M window)
- **A 95% per-step success rate drops to ~60% by step 10** — verification at every step is non-negotiable

### The Solution: File-Based State Persistence

```
GOAL.md           ← This file. Persistent state contract.
  │
  ├── [SESSION-STATE.md]  ← Created per session. Handoff doc for next agent.
  │     ├── Current focus
  │     ├── Completed work
  │     ├── Remaining work
  │     ├── Blockers & decisions
  │     └── Files modified
  │
  └── [TASKS.md]  ← Living task board. Updated by agent during work.
```

**Session lifecycle**:
1. Agent reads GOAL.md (this file) + TASKS.md at session start
2. Agent reads or creates SESSION-STATE.md to resume context
3. Agent executes tasks, updates TASKS.md after each completed task
4. At 60% context or session end: agent writes SESSION-STATE.md handoff
5. Next session: repeat from step 1

---

## 2. Active Goal

### Current Objective

> **Complete the Expressive Appliance Dot Matrix Eyes system and ship a working Android demo.**

### Success Criteria (Verification Check)

```
1. ./gradlew :app:testDebugUnitTest exits 0 (all unit tests pass)
2. ./gradlew assembleDebug exits 0 (APK builds)
3. testbed/index.html renders eyes correctly in browser
4. Eye expressions respond to dial changes (humor/honesty)
5. No deprecated GlyphGrid or TelemetryFeed references remain
```

### Constraints

- Do NOT modify LiteRT-LM version pin (stays at 0.10.2)
- Do NOT modify GemmaBackend.kt (stable, tested, on-device critical)
- Eye apertures MUST stay in upper half ($Y \le 0.50$)
- No specular glint or pupils — solid LED clusters only

### Kill Conditions (Stop When)

- 3 consecutive test failures → stop, report
- Goal evaluator says "goal met" → stop
- Token budget exhausted → compact or new session
- No progress after 5 evaluations → stop, reassess
- Wall-clock time > 4 hours → stop, write handoff

---

## 3. Phase Decomposition

### Phase 1: Core Dot Matrix & Procedural Eye Engine

**Milestone**: `ApplianceMatrixCanvas` renders full-screen LED grid with ghost dots

| Task | Verification | Status |
|------|-------------|--------|
| Implement `ApplianceMatrixCanvas` (LED/VFD dot grid, ghost dots, anti-aliasing) | Unit tests pass | ☐ |
| Implement `CozmoEyeRenderer` (SDF boundary, lid deformation, slant, squash/stretch, $Y \le 0.5$ clamping) | Unit tests pass | ☐ |
| Implement ambient breathing wave generator | Visual test in testbed | ☐ |
| Unit tests for coordinate projection, lid clipping, rasterization | `./gradlew :app:testDebugUnitTest` | ☐ |

**Depends on**: Nothing (greenfield)

---

### Phase 2: Expression & Reaction State Machine

**Milestone**: Eye expressions morph smoothly between moods via dials

| Task | Verification | Status |
|------|-------------|--------|
| Implement `EyeExpressionState` presets (Neutral, Skeptical, Happy, Curious, Drowsy, Surprised, Thinking) | Unit tests | ☐ |
| Implement spring-damper interpolator for continuous morphing | Unit tests | ☐ |
| Wire TARS Honesty and Humour dials into emotive deformation parameters | Manual test | ☐ |
| Implement momentary reaction overlays (Heart morphing, Thinking radar) | Visual test | ☐ |

**Depends on**: Phase 1 complete

---

### Phase 3: Telemetry & Face View Integration

**Milestone**: Full-screen matrix renderer replaces old face system

| Task | Verification | Status |
|------|-------------|--------|
| Implement bottom-edge telemetry pips (battery, BLE/Wi-Fi) | Visual test | ☐ |
| Refactor `TarsFaceView.kt` to use unified matrix renderer | Build succeeds | ☐ |
| Remove deprecated `GlyphGrid.kt`, `TelemetryFeed.kt`, on-screen text, sliders | No references remain | ☐ |
| Wire touch/tap gestures and IMU inertial lag into eye gaze | Manual test on device | ☐ |

**Depends on**: Phase 2 complete

---

### Phase 4: Verification & Polish

**Milestone**: All tests pass, APK builds, on-device demo works

| Task | Verification | Status |
|------|-------------|--------|
| Run all Android unit and instrumentation tests | `./gradlew :app:testDebugUnitTest` | ☐ |
| Build debug APK | `./gradlew assembleDebug` | ☐ |
| Test on-device rendering fidelity and frame rates | Visual inspection | ☐ |
| Update TASKS.md, PROJECT_INDEX.md, AGENTS.md | Documentation complete | ☐ |

**Depends on**: Phase 3 complete

---

## 4. Session Handoff Protocol

### When to Write SESSION-STATE.md

Write the handoff file when:
- Context window reaches ~60% capacity
- A natural phase boundary is reached
- The agent is about to stop (any reason)
- The user says "stop" or "pause"

### SESSION-STATE.md Template

```markdown
# SESSION-STATE.md — Handoff

**Session**: [date/time]
**Goal**: [active goal from GOAL.md]
**Phase**: [current phase number and name]

## Completed This Session
- [ ] Task description — commit SHA, files touched

## In Progress
- [ ] Task description
  - Status: [what was done, what's left]
  - Blockers: [any blockers]
  - Context: [why this approach, what was tried]

## Remaining Work
- [ ] Task description (priority, dependencies)

## Decisions Made
- [Decision] rationale — affects [scope]

## Files Modified This Session
- path/to/file.kt — [what changed and why]

## Warnings / Known Issues
- [issue description] — severity, workaround

## Next Action (for next agent)
[One sentence: what should the next agent do first?]
```

---

## 5. Verification Checkpoints

Run these after every significant change:

| Check | Command | When |
|-------|---------|------|
| Unit tests | `cd android && ~/toolchains/gradle-8.7/bin/gradle test --console=plain` | After any Kotlin change |
| APK build | `cd android && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain` | Before handoff |
| Visual test | Open `testbed/index.html` in browser | After eye/matrix changes |
| No deprecated refs | `grep -r "GlyphGrid\|TelemetryFeed" android/` | Phase 3 completion |
| Project index | Check `PROJECT_INDEX.md` links new files | Before handoff |

---

## 6. Goal Drift Prevention

### Guardrails

1. **No scope creep**: If a task isn't in the phase plan, don't add it. Write it in TASKS.md "Ready to Pick Up" instead.
2. **Don't fix unrelated bugs**: Note them in SESSION-STATE.md "Warnings", but don't fix them now.
3. **One task at a time**: Complete it, verify it, commit it, then move on.
4. **Baseline at session start**: `git status` and `git log --oneline -5` — know what changed since last session.

### Drift Detection Signals (Self-Check)

If you notice any of these, STOP and reassess:
- You're working on a task not in the current phase
- You've read 5+ files without making any edits
- You've been exploring for > 20% of your session without implementing
- You're "improving" code that isn't broken
- You're adding features not in the success criteria

---

## 7. Compounding Error Mitigation

### Per-Step Verification

After EVERY significant edit:
1. Run the relevant test suite
2. Check for compilation errors
3. Verify the change actually does what you intended
4. If 2 consecutive attempts fail, STOP and reassess approach

### Context Management

- **At 40% context**: Start thinking about handoff
- **At 60% context**: Write SESSION-STATE.md and stop
- **Never push to 80%+**: Quality degrades, errors compound

### Recovery Protocol

If you encounter repeated failures:
1. Re-read the spec (`docs/06-specs/`)
2. Re-read the plan (`docs/07-plans/`)
3. Check AGENTS.md for gotchas
4. If still stuck, write a clear blocker in SESSION-STATE.md and stop

---

## 8. References

| Document | Purpose |
|----------|---------|
| `AGENTS.md` | Codebase conventions, gotchas, commands |
| `TASKS.md` | Living task board (update during work) |
| `SESSION-STATE.md` | Per-session handoff (create at session end) |
| `docs/06-specs/2026-08-22-expressive-appliance-eyes-design.md` | Eye system spec |
| `docs/07-plans/2026-08-22-expressive-appliance-eyes.md` | Phase-by-phase implementation plan |
| `docs/03-decisions/` | Architecture Decision Records |
| `PROJECT_INDEX.md` | Central index (update when adding files) |

---

## 9. Strategies Applied (Research Summary)

This protocol is informed by patterns from:

- **DeerFlow `/goal` system**: Evaluator-based goal checking, bounded continuations, automatic stop on no-progress
- **Cline Focus Chain**: Real-time task tracking, auto-compact at context limits
- **Session Handoff Protocol**: Structured handoff docs for multi-session persistence
- **LangGraph Checkpoints**: Durable state persistence across sessions
- **Compounding Error Research**: "95% per-step success = 60% at step 10" — verification at every step

**Key insight**: File-based state persistence (GOAL.md, TASKS.md, SESSION-STATE.md) is the most reliable multi-session strategy. It's simple, debuggable, human-readable, and survives agent restarts.
