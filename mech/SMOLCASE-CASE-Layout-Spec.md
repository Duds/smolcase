# SMOLCASE CASE — Fusion 360 Layout Spec (v1)

**Date:** 2026-08-15
**Feeds:** `mech/smolcase_v1.f3d`
**Governing decisions:** [[docs/03-decisions/005-case-architecture-2leg|ADR-005]] (2-leg CASE), ADR-005 §1a (monolithic tub + SHELL lid, internally-loaded servos)
**Related:** [[docs/04-hardware/SMOLCASE-Case-Design-Brainstorm]] (ideation source)
**Units:** mm throughout. All dimensions are *design targets* — verify against physical parts and the STS3215 STEP file before committing to print.

---

## 0. Datum scheme

```
                 Z (up)
                 │
        ┌────────┴────────┐
        │     PHONE       │        CASE outline: X = length (162)
        │   (screen out)  │        Y = width (82), Z = height (thickness)
        │                 │   → X
        └─────────────────┘
  Origin (0,0,0): geometric center of CASE outer back face, on the mid-plane.
  Pivot axis: parallel to Z (shaft axis through side walls... see §4 note).
```

Wait — geometry clarification for this design: the legs rotate **beside** the case, so the servo shaft axis is **horizontal, along Y** (through the left/right side walls). CASE axes:

- **X** = case length (162) — along the phone's long edge
- **Y** = case width/thickness direction (82) — servo shafts pass through these walls
- **Z** = case height (28–32) — phone thickness direction + cavity depth

> Sanity anchor: Pixel 8 = 150.5 (X) × 70.8 (Y-face) × 8.9 (Z). The phone lies *flat* in the case — screen faces ±Z outward, legs sweep in X–Z planes beside the ±Y faces.

---

## 1. Envelope summary

| Item | X | Y | Z | Mass |
|------|---|---|---|------|
| CASE outer | 162 | 82 | 30 (target) | ~90–110 g printed |
| LEG-L / LEG-R | 162 long | 12 wide | 6 thick | ~15–20 g each |
| SHELL lid | 156 | 76 | 2.4 | ~12 g |
| All-up robot | — | — | — | **~570–600 g** |

---

## 2. Phone channel (JAW)

| Parameter | Value | Basis |
|-----------|-------|-------|
| Channel X | 151.5 | Pixel 8 (150.5) + 0.5 clearance each side |
| Channel Y | 71.6 | Pixel 8 (70.8) + 0.4 clearance |
| Channel Z (depth) | 9.3 | Pixel 8 (8.9) + 0.4 |
| Corner radius | ≥ 3.0 | Phone corner + print cleanup |
| Screen lip | 1.0 overhang, 2.0 wide | Retains phone face edge, keeps screen visible |
| Camera window | TBD — measure device | Rear camera + mic must be unobstructed |
| JAW latch | 1× spring tab at −X end | Phone slides in from +X end, latch clicks behind |
| USB-C access | Cutout at phone port edge | Internal charging lead (ADR-005 §5) |

---

## 3. Servo pods (HIP POD-L / HIP POD-R)

Servo: **Feetech STS3215** — body 45.2 × 24.7 × 35 mm, 55 g; 25T spline, OD 5.9 mm; 150 mm lead wire; mounting via side ear flanges. *(Source: manufacturer/retailer datasheets, 2026-08-15.)*

| Parameter | Value | Notes |
|-----------|-------|-------|
| Pocket inner X | 46.0 | servo 45.2 + 0.4 clearance ×2 |
| Pocket inner Z | 25.5 | servo 24.7 + 0.4 ×2 |
| Pocket depth (Y) | 36.0 | servo 35 + 1.0 lead-relief |
| Shaft pass-through | Ø 8.0 | clears 5.9 spline boss + horn nut |
| Ear flange screw holes | **VERIFY from STS3215.stp** | 4× per servo, M2 self-tap or M2 heat-set inserts |
| Wire slot | 4 × 8 | from pocket into main cavity, grommet edge |
| Loading direction | **from inside cavity** (ADR-005 §1a) | pocket open toward cavity, closed toward outside except shaft bore |
| Pivot center height | **X = 108 from case bottom** | 2/3 of 162 case height |
| Pod wall thickness | 2.4 min, 3.0 around ears | impact + screw boss strength |

**Pod orientation:** servo length (45.2) runs along case X; the shaft exits the ±Y side wall. Servo lead exits inward.

---

## 4. Structure

| Feature | Spec |
|---------|------|
| Outer wall | 2.0–2.4 PETG |
| Pod walls | 2.4–3.0 (see §3) |
| Back rim | tongue-and-groove, 1.2 deep × 1.0 wide — lid alignment under tumble loads |
| Ribs | 2.0 thick × 3.0 tall, along X between pod pockets and battery bay |
| Heat-set inserts | M2 × 3 (lid), M2 × 8 (servo ears, if not self-tap) |
| Edge chamfers | ≥ 1.0 everywhere (print + impact) |
| Fillets, internal corners | ≥ 1.5 (stress) |

---

## 5. Battery bay (BELLY) + electronics cavity

| Item | Envelope | Placement |
|------|----------|-----------|
| 2× 18650 | Ø 18.8 × 65.5 each (incl. clearance) | side-by-side along X, centered on mid-plane behind phone — CoG symmetry (ADR-005 §4) |
| BELLY retainer | printed cradle + 1 strap/latch | drop-in, tool-less swap |
| RIBCAGE (PCB zone) | 60 × 40 × 12 reserved | between battery bay and SHELL |
| Wire channel | 6 wide × 3 deep | pod pockets → RIBCAGE zone, both sides |
| USB-C charge port | panel cutout on −Z edge | reachable without opening SHELL |
| Power switch | Ø 6–8 rocker cutout, −Z edge | beside charge port |

---

## 6. Legs (LEG-L / LEG-R)

| Parameter | Value | Notes |
|-----------|-------|-------|
| Length | 162 | = case height (owner spec) |
| Cross-section | 12 × 6 rounded rectangle | symmetric — leg is the foot at every angle |
| Tip pads | TPU, both ends, ~10 long | friction-fit or 1× M2 screw (open Q4) |
| Hub end | horn pocket for 25T POM horn + center screw | verify horn dims from STEP file |
| Swept-plane offset | leg plane 9–12 outside case ±Y face | clears case silhouette through full 360° |
| Leg-to-leg timing | independent planes — no mutual collision | crossing gaits deferred (open Q6) |
| Ground clearance | ~54 | leg tip below case bottom at 6 o'clock |

---

## 7. SHELL (back lid)

| Parameter | Value |
|-----------|-------|
| Thickness | 2.0 (non-structural) |
| Fasteners | 4× M2 × 6 into heat-set inserts, one per corner |
| Vent slots | 4× slots over RIBCAGE zone |
| RGB light pipe | 1× Ø 3 hole over status LED |

---

## 8. Assembly order (from ADR-005 §1a)

1. Heat-set inserts into tub (lid + servo ears)
2. Servos into pods **from inside**, ear screws
3. RIBCAGE board → RIBCAGE zone, wire bus through channels
4. 18650 pair into BELLY
5. SHELL lid, 4 screws
6. Legs onto horns (bench-set servo mid-position first!)
7. Phone into JAW channel — last, or anytime

---

## 9. Pre-print checklist (do in Fusion 360 first)

- [ ] Import **STS3215.stp** (Waveshare/Feetech publish it) — verify pocket, ear holes, horn interface
- [ ] 360° sweep study: both legs vs. case silhouette and table edges
- [ ] CoG estimate with real masses (phone 187, servos 110, cells ~95, PCB ~50, prints TBD)
- [ ] Phone camera/mic window against a physical Pixel 8
- [ ] Tolerance test print: one pod pocket + one phone-channel corner before full tub

## Open questions carried from brainstorm (Phase 4)

1. Servo variant: **7.4 V / 19.5 kg·cm (C001)** fits the 2S battery directly; 12 V / 30 kg·cm (C018) needs 3S or boost. Rated (continuous) torque is only ~6.5 kg·cm @6V — gait loads are intermittent, but thermal watch via servo feedback is mandatory.
2. Phone charging in v1 vs v1.1
3. 18650 vs LiPo pouch
4. Fixed vs screw-on TPU leg tips
5. Battery behind phone (assumed here) vs below phone
6. Leg-crossing gaits allowed?
