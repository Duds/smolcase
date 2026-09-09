---
id: 20260909-003
title: "Spike: SMOLCASE face expressions"
type: spike
status: active
risk: medium
parent: "[[_tasks/20260909-002-dot-matrix-viewport]]"
---

# Spike: SMOLCASE Face Expressions

## User story

As the SMOLCASE face designer, I want to test eye shapes and moods in a browser before touching Android, so that I know which expressions survive the dot-matrix display.

## Question

Which expressions survive quantisation to 38 x 68 dots, and what eye-shape parameters produce them?

## Background

Split from [[_tasks/20260909-002-dot-matrix-viewport|the viewport spike]]. The dot-matrix pass is now shared, so this spike only builds the source: a signed distance field eye model plus a mood system.

## Acceptance criteria

- [ ] Given the prototype, when it renders, then two asymmetric SDF eyes are drawn and quantised through the shared dot-matrix pass.
- [ ] Given the supplied expression table, when a preset is selected, then eyes alone convey its shape cue (arches, squeezed chevrons, lowered lids, opposing slants, wide surprise or hearts), without pupils, mouths or decorative marks.
- [ ] Given any preset, when intensity is zero, then the source matches neutral with the same base parameters and dials.
- [ ] Given the angled examples, when face tilt changes, then both eyes rotate around their shared centre and capture/restore preserves the tilt.
- [ ] Given the comparison controls, when animation is disabled, then raw and dot-matrix captures are repeatable.
- [ ] Given any pose, when rendered, then the source eyes remain above the screen centreline.
- [ ] Given the humor and honesty dials, when they change, then asymmetry and lid position change accordingly.
- [ ] Given blinking and micro-saccade, when enabled, then both animate without altering the mood.
- [ ] Given a mood and parameter set, when captured, then the JSON can be pasted back to reproduce it.

## Deliverables

- `spikes/20260909-face-expressions/index.html`
- `spikes/20260909-face-expressions/README.md`
- `spikes/20260909-face-expressions/findings.md`
- `spikes/shared/dot-matrix-shader.js`, shared with the viewport spike
