---
id: 20260829-004
title: "Design SC-CASE chassis and leg geometry in CAD"
type: wayfinder:prototype
status: open
blocked-by: [20260829-003]
---

## Question

What is the exact 3D geometry for the SMOLCASE physical body, defined as parametric CAD ready for 3D printing?

**Scope:**
Main chassis (`SC-CASE`) housing the Pixel 8, internal electronics, and battery. Two 1-piece 360°-rotating legs (`SC-LEG-L`, `SC-LEG-R`) hinged at ~2/3 case height. Internal bracketry: servo pods (`SC-POD-L/R`), PCB bracket (`SC-RIB`), battery retainer (`SC-BLY`), back lid (`SC-SHL`), phone latch (`SC-JAW`).

**Known constraints from ADR-005:**
- Pixel 8 dimensions: ~150.5 × 70.8 × 8.9 mm — chassis must accommodate with screen exposed
- Servo mounting pods at ~2/3 case height
- Battery compartment: LiPo or 18650, accessed from rear
- ESP32 mounting: somewhere internal, with antenna clearance for BLE
- Ventilation: possible passive vent channels for Pixel 8 thermal dissipation
- Mass distribution: center of mass should be low and central for stable gait

**Prototype output:**
- Fusion 360 (or equivalent) parametric CAD model
- Export: dimensions exported to a datum reference for MuJoCo XML body update
- Print-ready STLs or STEP files
- Bill of Materials for printed parts (estimated filament volume, print time)

**Resolution:** Complete CAD model with all part files, assembly, and dimension reference for MuJoCo update. SC-CASE, SC-LEG-L, SC-LEG-R, and internal bracketry defined.