# ESP32 Servo Bridge Firmware

> Placeholder — firmware development begins after hardware selection.

## Overview

The ESP32 acts as a **dumb command executor**. It receives servo angle commands over BLE from the Pixel 8 and outputs PWM signals.

**See:** [[docs/03-decisions/004-esp32-ble-bridge|ADR-004: ESP32 BLE Bridge]] (BLE GATT protocol, power)

## Protocol (TBD)

BLE GATT characteristic for servo commands:
- Byte 0: Command type (0x01 = servo angles)
- Byte 1-8: Servo angles (8-bit, mapped 0-180°)

## Dependencies

- Arduino-ESP32 core or ESP-IDF
- BLE library
