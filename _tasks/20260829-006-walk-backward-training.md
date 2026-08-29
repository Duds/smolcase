---
id: 20260829-006
title: "Train Walk Backward PPO policy with final body geometry"
type: wayfinder:task
status: open
blocked-by: [20260829-004]
---

## Question

Train a PPO Walk Backward policy using the final SC-CASE body geometry in MuJoCo.

**Blocked by:** 20260829-004 — requires accurate mass distribution from CAD.

**Requirements:**
1. Update MuJoCo body XML (`sim/assets/`) with final SC-CASE dimensions, mass, and inertia from CAD datum sheet
2. Update CPG baseline parameters for new body if needed
3. Train PPO Walk Backward policy (target: >5m backward displacement)
4. Log training metrics to TensorBoard (`logs/PPO_2/`)
5. Save checkpoints every 50K steps
6. Evaluate trained policy (5+ episodes)

**Resolution:** Walk Backward policy saved in `models/`, evaluation metrics documented. Body XML updated to match final CAD geometry.