---
id: 20260830-012
title: "Create hardware assembly checklist"
type: task
risk: low
status: blocked
blocked-by: 20260829-004
---

# Task: Create hardware assembly checklist

**Assignee:** agent
**Priority:** low

## Objective
Create `mech/assembly-checklist.md` — a first-article inspection checklist for hardware assembly. This documents what to verify before first power-on, establishing the Observe protocol for the hardware subsystem per STANDARDS.md §2.3.

## Steps to Execute
1. Review ADR-005 (case architecture) for assembly steps and datum specs
2. Review `docs/04-hardware/SMOLCASE-Case-Design-Brainstorm.md` for design intent
3. Create `mech/assembly-checklist.md` covering:
   - Pre-assembly: printed part dimensional check against datum specs
   - Assembly sequence: per ADR-005 §1a (servos → RIBCAGE → BELLY → SHELL → phone)
   - Post-assembly: servo range-of-motion check, BLE pairing test, phone-seat security
   - First power-on: voltage check, smoke test, log capture
4. Link from STANDARDS.md §2.3 testing table

## Rollback Plan
Delete `mech/assembly-checklist.md`.

## Acceptance Criteria
- [ ] `mech/assembly-checklist.md` exists with pre-assembly, assembly, post-assembly, and power-on sections
- [ ] All checkpoints are measurable (pass/fail, not subjective)
- [ ] Linked from STANDARDS.md testing table
- [ ] Noted as blocked until 20260829-004 (SC-CASE CAD) is complete