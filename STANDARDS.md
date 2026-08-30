# SMOLCASE — Project Standards

> Quality baselines for collaborative human-agent development.
> See [[AGENTS]] for session workflow, toolchains, and conventions.
> See [[PROJECT_INDEX]] for the index of all active documents.

---

## 1. Document Standards

### 1.1 When to Use What

| You want to... | Write this | Save here |
|---|---|---|
| Propose an architecture change | ADR | `docs/03-decisions/NNN-slug.md` |
| Define a feature | Feature spec | `docs/06-specs/YYYY-MM-DD-slug.md` |
| Scope a large initiative | Epic | `docs/templates/epic-template.md` |
| Capture a user need | User Story | `docs/06-specs/stories/slug.md` |
| Plan implementation work | Task | `_tasks/YYYYMMDD-NNN-slug.md` |
| Investigate an unknown | Spike | `_tasks/YYYYMMDD-NNN-slug.md` |
| Report a defect | Bug report | `_tasks/YYYYMMDD-NNN-slug.md` |
| Document an outage | Incident report | `incidents/YYYY-MM-DD-slug.md` |

### 1.2 Every Document Must Have

- **YAML frontmatter** with `id`, `title`, and `type`
- **A link in PROJECT_INDEX.md** — if it's not indexed, it doesn't exist
- **Wiki-links** to related ADRs, specs, and tickets (see AGENTS.md §9)

### 1.3 ADR Standards

Every ADR must contain:
- **Context:** The forces at play and the problem to solve
- **Decision:** The chosen approach stated clearly
- **Consequences:** Positive and negative trade-offs
- **Alternatives Considered:** At least 2 alternatives with rejection rationale
- **Related links:** To sibling ADRs, specs, and research

Hardware ADRs (servo, battery, chassis, ESP32) must be revisited when new empirical data arrives — not on a calendar. When a new servo is selected, revisit ADR-005's torque basis. When the first print is assembled, revisit ADR-005's mass target.

### 1.4 Bug Report Standards

Every bug report must contain:
- **Current vs. Expected behaviour**
- **Replication steps** — clear, sequential, exact
- **BDD failure boundary** — Given/When/Then for regression and fix verification
- **Diagnostic data** — impacted subsystems, logs, environmental factors

---

## 2. Code Standards

### 2.1 Before Writing Code

- A spec (`docs/06-specs/`) or ticket (`_tasks/`) must exist
- BDD acceptance criteria must be stated
- The orient skill must have been run to confirm this is the right next task

### 2.2 Commit Messages

Every commit must describe *why* the change exists, not *what* changed:

```
Good: "Prevent session loading from crashing on missing metadata"
Bad:  "fix: nil pointer in session.go"

Good: "Reduce maxNumTokens to 128 to stabilise Gemma latency"
Bad:  "update GemmaBackend.kt"
```

First line under 72 characters. Body wraps at 72 characters if needed.

### 2.3 Testing Expectations

| Subsystem | What to test | Command | Must pass |
|-----------|-------------|---------|-----------|
| Android / Kotlin | Unit tests | `./gradlew :app:testDebugUnitTest` | 100% |
| MuJoCo simulation | XML loads, CPG baseline | `python3 sim/src/test_xml_load.py` | No errors |
| PPO policies | Reward vs. CPG baseline | `smolcase_train.py --mode eval` | Walk forward >5m |
| Conversation quality | Latency, repetition, recall | `scripts/test_conversation.sh` (when wired) | All prompts valid TARS |
| BLE firmware | Compilation, protocol | PlatformIO `test` | Compiles, tests pass |

Code written before tests should be the exception, not the rule. When it happens, go back and write the test before closing the task.

### 2.4 Self-Review Checklist

Before merging or closing any code change:

- [ ] Read each changed file. Does it match the acceptance criteria?
- [ ] What if input is null, device is offline, model fails to load?
- [ ] What if array is empty, value is max, operation is cancelled?
- [ ] Does this follow the same patterns as adjacent code?
- [ ] Do all relevant tests pass?
- [ ] Are affected docs updated (ADR, spec, TASKS.md, PROJECT_INDEX)?

---

## 3. Session Standards

### 3.1 Session Start

1. Run the **orient** skill — triage TASKS.md, choose next action
2. Read `.workspace-scratchpad.md` for raw ideas and cross-branch context
3. Read `docs/07-plans/wayfinder-map.md` for current destination and fog of war
4. Read `GOAL.md` for active destination contract
5. Read `STANDARDS.md` (this file) for quality baselines

### 3.2 Session End

1. Run the **close-and-learn** skill — captures decisions, lessons, updates TASKS.md, generates handoff prompt
2. If work is continuing across sessions, also run the **handoff** skill for a structured doc in `/tmp/`
3. Verify TASKS.md is accurate and PROJECT_INDEX is current

### 3.3 Task Closure Gate

Before declaring any task complete:

- [ ] Acceptance criteria are met (re-read them, test each)
- [ ] All relevant tests pass
- [ ] TASKS.md updated (status, completed date)
- [ ] PROJECT_INDEX current (new docs linked)
- [ ] Wayfinder map updated if a decision changed
- [ ] Handoff written if work is mid-stream

---

## 4. Decision-Making Standards

### 4.1 Risk Gating

| Risk Level | Criteria | Gate | Examples |
|-----------|----------|------|---------|
| **Critical** | Irreversible, costly, or breaking | Human approval required | Ordering parts, merging to main, flashing firmware to device |
| **Moderate** | Reversible but time-consuming | Self-review checklist | Code changes, training runs, spec updates |
| **Low** | Easily reversible | None — proceed | Documentation edits, refactoring, test additions |

### 4.2 When to Revisit a Decision

- **Hardware ADRs:** Revisit when new empirical data arrives (new servo selected → revisit ADR-005 torque basis; first print assembled → revisit ADR-005 mass target)
- **Non-hardware ADRs:** Revisit only when a decision proves incorrect
- **Wayfinder tickets:** Revisit when a blocking ticket is resolved and the frontier advances

### 4.3 When Not to Decide

If orientation is insufficient (contradictory data, unclear root cause, incomplete requirements), escalate rather than proceeding. Work the ambiguity via a spike ticket before committing to a decision.

---

## 5. Failure Mode Reference

| What goes wrong | How to diagnose | How to recover |
|----------------|----------------|----------------|
| Android app crashes on launch | `adb logcat -d \| grep AndroidRuntime` | Revert last APK, check `git diff` on manifest |
| MuJoCo sim diverges from real body | Compare mass/inertia in MJCF vs CAD | Revert XML, re-measure geometry |
| PPO training destabilises (reward collapse) | Check TensorBoard reward curve | Revert hyperparameters, restore checkpoint |
| BLE connection drops during gait | Check ESP32 serial monitor | Power-cycle ESP32, re-pair BLE |
| 3D print tolerances fail | Measure with calipers vs. datum spec | Adjust printer settings, re-slice |
| LiteRT-LM fails on Android update | Check logcat for native crash | Downgrade LiteRT-LM or pin Gradle dep |
| Conversation quality regresses | Run `scripts/test_conversation.sh` | Re-open bug ticket, re-apply fix with regression test |

---

## 6. Style & Language Standards

### 6.1 TARS Register

The creature persona is TARS from *Interstellar*. All assistant-facing agent communication follows:
- Deadpan, dry wit, measured tone
- Military commander / coach register
- No emojis ever
- No lists or bullet points in simulated speech
- No cheerful assistant mannerisms

### 6.2 Documentation Voice

Documentation is technical and direct. Use:
- Active voice ("the agent runs tests" not "tests are run by the agent")
- Second person for agent instructions ("run the orient skill at session start")
- Third person for system descriptions ("the robot walks via two 360° servos")

### 6.3 Wiki-Links

All cross-references use `[[wiki-links]]` per AGENTS.md §9. Every document links to:
- Its governing ADR
- Related specs and plans
- Any predecessors it supersedes
- PROJECT_INDEX.md (via the index being authoritative)

---

## 7. Process Improvement

These standards are living. If a process is causing more friction than it prevents, change it. The mechanism is:
1. Document the friction in `.workspace-scratchpad.md`
2. Propose a change via ADR or spike ticket
3. If agreed, update this file and AGENTS.md
4. If not agreed, close the ticket and note why

The goal is effective collaboration, not rule compliance.