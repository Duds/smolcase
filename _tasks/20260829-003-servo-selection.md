---
id: 20260829-003
title: "Decide serial-bus servo model for ~300g robot"
type: wayfinder:research
status: open
blocked-by: []
---

## Question

Which specific 360° serial-bus servo model should SMOLCASE use for powering SC-LEG-L and SC-LEG-R?

**Constraints from ADR-005:**
- Exactly 2 servos (one per leg, hinged at ~2/3 case height)
- 360° continuous rotation (not positional)
- Serial-bus protocol (for ESP32 BLE bridge compatibility — ADR-004)
- Must lift/hold/articulate a ~300g robot on a desk surface

**Research dimensions:**
1. **Torque requirement**: What holding torque (kg·cm) is needed for ~300g load at ~100mm moment arm? Factor in margin for dynamic gait forces.
2. **Speed**: What angular velocity (RPM or °/s) is needed for natural-looking gait? Consider typical desk-robot walking cadence.
3. **Physical dimensions**: Height, width, depth, mounting hole pattern — drives SC-POD-L/R design and overall chassis layout.
4. **Voltage & current**: Operating range and peak stall current — drives battery and ESP32 power architecture.
5. **Protocol compatibility**: Confirm the serial-bus protocol (e.g. PWM-over-serial, half-duplex UART) is implementable on ESP32.
6. **Cost & availability**: Off-the-shelf, not custom — must be orderable in single-unit quantities.

**Candidates to evaluate:**
- Feetech/LX-16A serial bus servo (popular in hobby robotics)
- Waveshare / HiWonder serial bus servos
- Dynamixel XL / X-series (robust but expensive)
- Any other 360° serial-bus servo with adequate torque at the ~300g scale

**Resolution:** One specific servo model selected with documented torque margins, dimensions, voltage, and protocol details. Mounting hole pattern captured for CAD (ticket 20260829-004).