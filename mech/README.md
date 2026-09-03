# Mechanical Design

> Placeholder — Fusion 360 CAD models will live here.
>
> **Start here:** [[SMOLCASE-CASE-Layout-Spec]] — dimensioned layout spec v1 (datums, pod pockets, phone channel, legs, lid, assembly order).

## Design Philosophy

SMOLCASE is a **phone-robot hybrid** — the Pixel 8 is not just the brain but a visible, expressive part of the creature. The mechanical design must:

1. **Secure the phone** — rigid mount, easy insert/remove
2. **Expose the screen** — the face must be visible
3. **Allow sensor access** — cameras, microphones unobstructed
4. **House servos** — 2x 360° servos, symmetrical biped layout (legs hinge at ~2/3 case height)
5. **Manage cables** — BLE module, power, servos routed cleanly
6. **Look like a creature** — not just a phone on sticks

## GrowBot Reference Dimensions

GrowBot is approximately:
- Body: ~100mm × 60mm × 40mm
- Legs: 1-piece rotating legs, 1 servo each, ~64mm
- Weight: ~250g total (without phone)

SMOLCASE will be larger due to the Pixel 8 (~150mm × 70mm × 9mm).

## Fusion 360 Files

| File | Description | Status |
|------|-------------|--------|
| `smolcase_v1.f3d` | Initial concept | ⏳ Pending |

## Print Settings

- Printer: TBD
- Material: PETG or ABS (durability + heat resistance)
- Infill: 30-40% for structural parts
