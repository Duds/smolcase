---
id: 20260829-010
title: "Full 30-behaviour training suite"
type: wayfinder:task
status: open
blocked-by: [20260829-006]
---

## Question

Train and validate the complete 30-behaviour curriculum across all 5 tiers for SMOLCASE.

**Blocked by:** 20260829-006 — Walk Backward is the prerequisite; remaining behaviours build on validated body model.

**Curriculum (tiers from `docs/02-behaviour-training/`):**
1. **Locomotion** — walk, trot, bound, turn (Walk Forward done, Walk Backward is ticket 20260829-006)
2. **Stability** — stand, sit, recover from falls
3. **Agility** — side-step, pivot, reverse
4. **Expressive** — bow, greet, cower, shake, head tilt
5. **Advanced** — climb, navigate, push, follow

**Requirements:**
1. Each behaviour trained with its reward function, observation space, and hyperparameters from the behaviour training plan
2. Each exported to `.tflite` and verified for quantization fidelity
3. Model manifest updated (`models/README.md`)
4. Sim-to-real transfer notes captured per behaviour

**Resolution:** All 30+ behaviours trained, exported, and documented in model manifest. The full behaviour suite is available for on-device integration via the behaviour arbiter.