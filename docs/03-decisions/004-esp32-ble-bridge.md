# ADR-004: ESP32 BLE Bridge

## Status
Accepted — 2026-08-06

## Context
The Pixel 8 has no GPIO pins. We need a way to control servos from the phone. Options:
1. USB-OTG to a microcontroller
2. BLE to a wireless microcontroller
3. WiFi to an ESP8266/ESP32

## Decision
Use an **ESP32-based BLE bridge** as the servo controller.

## Rationale

- **BLE 5.0** is native on Pixel 8, low latency (~10-20ms), low power
- **ESP32** is cheap (~$5), widely available, has built-in BLE + WiFi
- **Decouples concerns**: Phone does brain/ML, ESP32 does real-time PWM
- **Wireless**: No USB cable tethering the robot
- **Proven pattern**: Many phone-robot projects use this exact architecture

## Protocol (Draft)

### BLE GATT Service

| UUID | Characteristic | Permissions | Description |
|------|---------------|-------------|-------------|
| `0xSMOL` (TBD) | `SERVO_ANGLES` | Write | 2 bytes, one per servo |
| `0xSMOL` (TBD) | `STATUS` | Notify | Battery, fault codes |

### Servo Angle Packet

```
Byte 0:   Command type (0x01 = immediate angles)
Byte 1-2: Servo 0-1 targets (0-255 maps to 0-360°)
```

### Future: Command Queue Mode

```
Byte 0:   Command type (0x02 = queued keyframes)
Byte 1:   Number of keyframes
Byte 2+:  [time_ms, servo0, servo1, ...] × N
```

## Firmware Stack

- **Arduino-ESP32** core (fastest to prototype)
- **ESP-IDF** (future, for production efficiency)
- **BLE** via `BLEServer` / `BLECharacteristic`
- **Servo** via `ledc` PWM (ESP32 native, 50Hz, 0.5-2.5ms pulse)

## Power

- ESP32 powered from robot battery (3.3V LDO or buck converter from LiPo)
- Servos powered directly from LiPo (4.8-6V)
- Shared ground essential

## Related
- [[docs/03-decisions/001-pixel8-brain\|ADR-001: Pixel 8 as Brain]] — no GPIO → BLE bridge needed
- [[docs/03-decisions/005-case-architecture-2leg\|ADR-005]] — UART servo interface supersedes PWM (see §Comms)
- [[firmware/README]] — firmware placeholder
- [[docs/06-specs/2026-08-29-settings-expansion-design]] — BLE integration for sensor readout
