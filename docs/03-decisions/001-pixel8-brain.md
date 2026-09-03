# ADR-001: Pixel 8 as Brain (Not Raspberry Pi Zero)

## Status
Accepted — 2026-08-06

## Context
GrowBot uses a Raspberry Pi Zero 2 W as its onboard computer. We needed to decide whether to follow this pattern or use an alternative.

## Decision
Use a **Google Pixel 8** as SMOLCASE's primary onboard computer instead of a Raspberry Pi Zero.

## Consequences

### Positive
- **Full Android sensor suite**: IMU, GPS, barometer, cameras, microphone, proximity — all accessible through standard Android APIs
- **Screen as face**: The OLED display becomes an expressive "face" for the robot — a core part of SMOLCASE's personality
- **No separate compute module**: One device, one battery, one OS to manage
- **Kimi integration**: Direct access to Kimi LLM through the subscription, no networking complexity
- **Faster processor**: Pixel 8 Tensor G3 significantly outperforms Pi Zero 2 W for ML inference
- **Built-in connectivity**: WiFi, 5G, BLE 5.0 — no dongles

### Negative
- **No GPIO pins**: Must use BLE or USB to communicate with servos (ESP32 bridge required)
- **Android constraints**: Less flexible than Linux for low-level hardware hacking
- **Power management**: Phone battery + robot battery = two power systems to manage, or phone runs from robot battery
- **Form factor**: Phone is larger and heavier than Pi Zero — affects mechanical design

## Related
- [[docs/03-decisions/004-esp32-ble-bridge\|ADR-004: ESP32 BLE Bridge]] — no GPIO → need wireless servo control
- [[docs/01-research/SMOLCASE-GrowBot-Reverse-Engineering]] — GrowBot's Pi Zero design
- [[docs/06-specs/2026-08-17-gemma-backend-design]] — on-device LLM on Pixel 8
- [[docs/06-specs/2026-08-22-expressive-appliance-eyes-design]] — screen as expressive face
- [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]] — phone form factor drives case design
