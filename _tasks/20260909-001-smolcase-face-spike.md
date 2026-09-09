---
id: 20260909-001
title: "Spike: SMOLCASE face sphere architecture"
type: spike
status: active
risk: medium
---

# Spike: SMOLCASE Face Sphere Architecture

## User story

As the SMOLCASE face designer, I want to test a sphere-based face architecture before applying the dot-matrix display layer, so that gaze movement and eye expression have a sound underlying model.

## Question

Can a Three.js sphere carrying animated eyes provide the source representation for SMOLCASE's face, with gaze expressed by sphere translation and expression expressed by eye animation in place?

## Acceptance criteria

- [ ] Given a local HTML page, when the prototype loads, then an awake sphere face is visible.
- [ ] Given horizontal or vertical gaze input, when the prototype receives it, then the sphere rotates smoothly around its centre pin in the corresponding direction.
- [ ] Given an awake face, when the blink animation runs, then the eyes animate in place while remaining attached to the rotating sphere.
- [ ] Given the eventual dot-matrix requirement, when the architecture is reviewed, then the dot-matrix display is documented as a future viewport translation layer and is not implemented in this spike.
- [ ] Given the prototype observations, when the spike closes, then `spikes/20260909-smolcase-face/findings.md` records a verdict and follow-up recommendation.

## Deliverables

- `spikes/20260909-smolcase-face/index.html`
- `spikes/20260909-smolcase-face/README.md`
- `spikes/20260909-smolcase-face/findings.md`
