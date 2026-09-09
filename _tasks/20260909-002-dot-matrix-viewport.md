---
id: 20260909-002
title: "Spike: SMOLCASE dot matrix viewport"
type: spike
status: active
risk: medium
parent: "[[_tasks/20260909-001-smolcase-face-spike]]"
---

# Spike: SMOLCASE Dot Matrix Viewport

## User story

As the SMOLCASE face designer, I want the dot-matrix display implemented as a true viewport translation layer, so that each dot is an emitter whose brightness comes from the source image rather than a hole punched in a blur.

## Question

Can the source face be rendered offscreen, quantised into per-dot luminance, and re-emitted as a grid of glowing dots?

## Background

Split from [[_tasks/20260909-001-smolcase-face-spike|the face sphere spike]] after review found the CSS mask was a sampling mask, not a translation layer. The blur was applied before the mask, so it blurred the whole image uniformly and then sampled it sparsely. That cannot express per-dot optics.

## Acceptance criteria

- [ ] Given the prototype, when it renders, then the source scene reaches the screen only through the quantising shader.
- [ ] Given a configured grid of columns and rows, when the face renders, then the visible dot grid matches that configuration.
- [ ] Given dots over the eyes, when pixel values are measured, then they are brighter than dots over unlit sphere.
- [ ] Given the glow, halation, and exposure controls, when they are changed, then the rendered dots change accordingly.
- [ ] Given a ghost floor above zero, when the face renders, then unlit dots are faintly visible.
- [ ] Given the debug bypass, when it is enabled, then the raw source renders without the viewport.

## Deliverables

- `spikes/20260909-dot-matrix-viewport/index.html`
- `spikes/20260909-dot-matrix-viewport/README.md`
- `spikes/20260909-dot-matrix-viewport/findings.md`
