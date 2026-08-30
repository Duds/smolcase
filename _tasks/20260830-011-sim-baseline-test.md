---
id: 20260830-011
title: "Create sim baseline test script"
type: task
risk: low
status: open
---

# Task: Create sim baseline test script

**Assignee:** agent
**Priority:** low

## Objective
Create a `sim/src/test_sim_baseline.py` script that runs a simple CPG evaluation and checks expected metrics, establishing a repeatable observation baseline for the simulation subsystem. This addresses the "Observe" gap in the testing doctrine (STANDARDS.md §2.3).

## Steps to Execute
1. Read existing `sim/src/test_xml_load.py` and `sim/src/smolcase_train.py --mode cpg` for patterns
2. Create `sim/src/test_sim_baseline.py` that:
   - Loads the MuJoCo XML
   - Runs the CPG baseline for N episodes
   - Asserts minimum walk distance (e.g., > -2m, meaning it doesn't fall backwards catastrophically)
   - Exits with clear pass/fail output
3. Verify it runs without error
4. Document the command in AGENTS.md §5.2 or STANDARDS.md §2.3

## Acceptance Criteria
- [ ] `sim/src/test_sim_baseline.py` exists and runs successfully
- [ ] Output includes clear pass/fail indication
- [ ] Command is documented in the testing table
- [ ] All existing sim tests still pass