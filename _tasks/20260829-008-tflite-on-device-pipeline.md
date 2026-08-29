---
id: 20260829-008
title: "TFLite policy export and on-device inference pipeline"
type: wayfinder:task
status: open
blocked-by: [20260829-006]
---

## Question

How does a trained SMOLCASE policy go from MuJoCo to running on the Pixel 8, and how does the Android app run it in real-time?

**Blocked by:** 20260829-006 — requires at least one trained policy (Walk Backward) to export and verify.

**Requirements:**
1. Export trained PPO policy from Stable-Baselines3 to `.tflite` format (already partially implemented in `sim/src/smolcase_train.py --mode export`)
2. Verify TFLite quantization fidelity — compare float vs quantized policy output on a test observation batch
3. Load and run `.tflite` on Pixel 8 — verify inference latency is within gait loop budget (~10-20ms per inference)
4. Wire TFLite inference output → BLE command stream → ESP32 bridge → servos

**Resolution:** `.tflite` model verified on-device with latency <20ms. On-phone inference pipeline documented and tested.