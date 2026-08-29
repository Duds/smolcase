---
id: 20260829-009
title: "Integrate behaviour arbiter on Android (brain → BLE → servo)"
type: wayfinder:prototype
status: open
blocked-by: [20260829-007, 20260829-008]
---

## Question

How does SMOLCASE's Android brain decide what movement to make and send it through the full pipeline to the servos?

**Blocked by:** 20260829-007 (BLE bridge firmware) + 20260829-008 (TFLite on-device pipeline).

**Requirements:**
1. **Behaviour arbiter** — a new Android class/module that decides which movement to execute based on creature state (idle, tracking face, speaking, listening, etc.)
2. **TFLite policy invocation** — load the appropriate `.tflite` model for the chosen behaviour, run inference with current observation (IMU + servo angles), get action output
3. **BLE command stream** — format action output into BLE GATT write commands matching the ESP32 bridge protocol (from 20260829-007)
4. **Safety** — if BLE disconnects, behaviour arbiter should stop sending commands and notify the creature brain
5. **State machine** — simple state machine: IDLE → STAND → WALK_FORWARD → WALK_BACKWARD → TURN → STOP → IDLE

**Resolution:** Behaviour arbiter implemented and tested on-device. Robot can transition between at least three states (stand, walk forward, walk backward) via BLE.