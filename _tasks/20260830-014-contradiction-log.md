---
id: 20260830-014
title: "Establish contradiction log practice"
type: task
risk: low
status: open
---

# Task: Establish contradiction log practice

**Assignee:** agent
**Priority:** low

## Objective
Add a lightweight contradiction tracking practice to the session workflow. When new empirical data contradicts a belief documented in an ADR, spec, or wayfinder map entry, the contradiction should be noted in `.workspace-scratchpad.md` and surfaced before acting.

This is the lightest-weight version of the OODA "world model maintenance" concept — not a database or formal process, just a documented habit.

## Steps to Execute
1. Add a "Contradictions" subsection to AGENTS.md §1 (Session Workflow) — short, ~3 lines
2. Add a sentence to STANDARDS.md §4.2 (When to Revisit a Decision) referencing the scratchpad as the capture mechanism
3. Add a note to `.workspace-scratchpad.md` (if it exists) with a `## Contradictions` section template

## Rollback Plan
Revert edits to AGENTS.md, STANDARDS.md.

## Acceptance Criteria
- [ ] AGENTS.md §1 has a contradiction capture note in session workflow
- [ ] STANDARDS.md §4.2 references scratchpad for tracking contradictions
- [ ] `.workspace-scratchpad.md` has a `## Contradictions` section template (or note explaining where to log them)