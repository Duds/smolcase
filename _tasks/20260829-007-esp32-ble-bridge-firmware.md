---
id: 20260829-007
title: "Implement ESP32 BLE-to-serial-bus bridge firmware"
type: wayfinder:prototype
status: open
blocked-by: [20260829-003, 20260829-005]
---

## Question

What is the firmware implementation for the ESP32 BLE bridge that translates Pixel 8 commands into serial-bus servo signals?

**Blocked by:** 20260829-003 (servo protocol) + 20260829-005 (ESP32 module selection).

**Requirements:**
1. BLE GATT server on ESP32 — Pixel 8 connects and sends target angles/speeds
2. Serial-bus protocol translation — convert BLE command payload into servo-specific serial frames
3. Command frequency — target 50-100Hz update rate (should be achievable with BLE and serial bus)
4. Status telemetry — ESP32 reads back servo position/load and broadcasts via BLE notify
5. Safety — watchdog timeout: if BLE disconnects, servos go to neutral/braked state

**Resolution:** Firmware written, flashed to ESP32, and verified with serial monitor. BLE GATT service/characteristic schema documented for Android side (ticket 20260829-009).