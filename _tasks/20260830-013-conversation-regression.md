---
id: 20260830-013
title: "Wire conversation regression test into process"
type: task
risk: low
status: open
---

# Task: Wire conversation regression test into process

**Assignee:** agent
**Priority:** low

## Objective
Wire `scripts/test_conversation.sh` into the bug closure checklist so conversation quality regressions are caught before closing a fix. This closes the quality feedback loop identified in the process coherence review.

## Steps to Execute
1. Read `scripts/test_conversation.sh` to understand its current inputs and output
2. Read `scripts/conversation-script.md` for the prompt set and expected behaviour
3. Determine whether the script can run offline or requires a device
4. If device-required: document the test protocol in STANDARDS.md §2.3 with device connection steps
5. If runnable offline: configure it as a pre-closure gate
6. Add a note to AGENTS.md §1.4 (Task Closure Gate) referencing the conversation test

## Acceptance Criteria
- [ ] Conversation test is referenced from STANDARDS.md §2.3 testing table
- [ ] Test is referenced from AGENTS.md §1.4 closure checklist
- [ ] Instructions for running are documented (device required or offline)
- [ ] The 6 bugs (20260830-001 through 006) have before/after baseline documentation