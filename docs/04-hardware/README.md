# Hardware Specification

> Placeholder — populated when mechanical design begins in Fusion 360.\n> See the [[docs/04-hardware/SMOLCASE-Case-Design-Brainstorm|Case Design Brainstorm]] for ideation.\n> See [[mech/SMOLCASE-CASE-Layout-Spec]] for the Fusion 360 datum layout.\n> Component naming per [[docs/03-decisions/005-case-architecture-2leg\\|ADR-005]].

## Target Spec

- **Body**: Phone-robot hybrid, GrowBot-inspired biped (CASE + LEFT LEG + RIGHT LEG)
- **Brain**: Google Pixel 8 (Android, full sensor suite, screen as face)
- **Servos**: 2x 360° serial bus servos, one per leg (Feetech STS3215-class, TBD — see brainstorm doc)
- **Servo Controller**: ESP32-based BLE bridge (dumb command executor)
- **Battery**: TBD — LiPo or 18650 pack
- **Communications**: BLE 5.0 (Pixel 8 ↔ ESP32)

## Bill of Materials (BOM)

| Component | Part Number | Qty | Supplier | Est. Cost |
|-----------|-------------|-----|----------|-----------|
| Servo | Feetech STS3215-class (360° serial bus) | 2 | TBD | ~$20 ea |
| ESP32 Dev Board | TBD | 1 | TBD | TBD |
| Battery | TBD | 1 | TBD | TBD |
| Frame / Chassis | Custom (Fusion 360) | 1 | 3D print | TBD |
| Phone Mount | Custom (Fusion 360) | 1 | 3D print | TBD |

## Power Budget

> To be calculated once servos and battery are selected.
