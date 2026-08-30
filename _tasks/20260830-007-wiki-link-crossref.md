---
id: 20260830-007
title: "Cross-document wiki-link maintenance pass"
type: task
severity: medium
reported: 2026-08-30
status: completed
completed: 2026-08-30
---

### Cross-document wiki-link maintenance pass

**Work done:**
- Added 100+ `[[wiki-links]]` across 33 markdown files
- Every ADR now links to related ADRs, specs, plans, and research
- Every spec now carries `Supersedes:`, `Superseded by:`, `Implementation plan:`, and `Related:` links
- Research docs now carry `Informs:` links to the decisions/specs they shaped
- Root files (README, AGENTS, GOAL, TASKS, PROJECT_INDEX) link to each other and to key ADRs/specs
- Added Section 8 (Wiki-Link Conventions) to AGENTS.md documenting format, placement, and maintenance rules

**Files with new links:**
- README.md, AGENTS.md, GOAL.md, TASKS.md, PROJECT_INDEX.md
- All 5 ADRs in docs/03-decisions/
- All 6 specs in docs/06-specs/
- All 4 plans in docs/07-plans/
- Both research docs in docs/01-research/
- docs/04-hardware/SMOLCASE-Case-Design-Brainstorm.md
- docs/02-behaviour-training/SMOLCASE-Behaviour-Training-Plan.md
- docs/05-design-thinking/SMOLCASE-Design-Thinking-Log.md
- mech/README.md, mech/SMOLCASE-CASE-Layout-Spec.md
- firmware/README.md, models/README.md, android/README.md
- _tasks/20260829-003-servo-selection.md, _tasks/20260829-004-sc-case-cad-design.md