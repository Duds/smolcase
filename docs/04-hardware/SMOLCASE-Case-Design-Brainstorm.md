# SMOLCASE Case Design — Design Thinking Brainstorm (v2)

**Date:** 2026-08-14
**Method:** IDEO / d.school Design Thinking, run properly this time
**Supersedes:** v1 of this document (quadruped assumption was wrong)
**Result:** [[docs/03-decisions/005-case-architecture-2leg|ADR-005]] (2-leg CASE)
**See also:** [[mech/SMOLCASE-CASE-Layout-Spec]] (Fusion 360 datum layout)

## v2 Corrections (from owner, 2026-08-14)

The actual mechanical concept — this changes everything downstream:

- **Three primary components: CASE, LEFT LEG, RIGHT LEG.** That's it.
- Legs are **simple one-piece** parts, each directly driven by a **360° servo**.
- Legs rotate a **full 360° beside the phone/case** (GrowBot-style tumble/windmill gait).
- Servo hinge point is at **~2/3 up the case height**.
- **Leg length ≈ case height.** With the pivot at 2/3 height, the leg reaches ~1/3 of case height past the top and ~2/3 below the pivot → ground clearance ~1/3 case height below the case bottom when a leg points straight down.
- Secondary components inside CASE only: sub-frames, covers, PCB mount brackets.

> ✅ Corrected 2026-08-14: `README.md`, `docs/04-hardware/README.md`, `mech/README.md`, and ADR-004's packet draft previously claimed 8–12 servos / quadruped — that text was a stale assumption and never matched the project. The research doc, behaviour plan, `smolcase_train.py`, and the MuJoCo XML were always 2-servo biped. All records now reflect this 2-leg architecture.

---

## Phase 1 — EMPATHIZE

Who experiences this case, and what do they need?

| Who | Need | Pain to avoid |
|-----|------|---------------|
| **Pixel 8 (the brain)** | Secure seat; screen/cameras/mic unobstructed; thermal headroom; stays charged | Rattling loose mid-tumble (IMU noise = bad policy input); blocked rear camera kills "Follow" |
| **You (builder)** | 3 major parts, minimal fasteners, cheap reprints, easy debug access | 20-part assemblies; a cracked leg requiring a full rebuild |
| **The creature** | Clean silhouette; legs as the whole character | Looking like a phone with random paddles bolted on |

**Key insight:** with only 3 primary parts, *each part carries enormous design load*. The CASE is simultaneously chassis, electronics enclosure, battery box, servo frame, and phone accessory. The legs are simultaneously structure, actuator linkage, and feet. Simplicity of part count = density of design decisions per part.

## Phase 2 — DEFINE

**POV:** A solo builder needs a minimal-part-count phone-case robot where the case itself does all structural and electronic work, because every extra part is print time, assembly time, and a failure joint during 360° tumbles.

**How Might We:**
1. HMW make CASE a single rigid frame that mounts phone, PCB, battery, and both servos with zero secondary structure where possible?
2. HMW pick 360° servos that give *positional* control across full rotation (not just speed control)?
3. HMW shape a one-piece leg so it works as foot at *every* rotation angle?
4. HMW keep the phone charged and comms reliable with only two actuators to power?
5. HMW keep CoG such that the tumbling gait is controllable and repeatable in sim and on hardware?

## Phase 3 — IDEATE

### 3.1 Component naming

With 3 primary parts, naming stays flat and literal — the product is literally named after the case. Options:

**Option A — Literal / recursive (recommended):** the case is just **THE CASE**.

| Part | Name | Code |
|------|------|------|
| Phone case / main body | **CASE** | `SC-CASE` |
| Left leg | **LEG-L** | `SC-LEG-L` |
| Right leg | **LEG-R** | `SC-LEG-R` |
| Servo pockets (molded into CASE at 2/3 height, L/R) | **HIP POD-L / HIP POD-R** | `SC-POD-L/R` |
| Internal PCB bracket | **RIBCAGE** | `SC-RIB` |
| Battery tray / retainer | **BELLY** | `SC-BLY` |
| Back cover / service panel | **SHELL** | `SC-SHL` |
| Phone retention (clip/latch/frame lip) | **JAW** | `SC-JAW` |

Rationale: "SMOLCASE is a case" is the joke and the truth — lean in. Primary parts get dead-simple names (firmware already wants `LEG_L`, `LEG_R`); internal secondaries keep light anatomical names so docs stay readable.

**Option B — Full anatomical:** TORSO, FEMUR-L/R... mismatched — a one-piece rotating leg isn't a femur.

**Option C — Nautical:** HULL, PORT/STARBOARD paddles. Charming; "port/left" confusion tax forever.

**Recommendation:** Option A. One naming rule: whatever Fusion 360 calls it, the BOM, firmware constants, and ADRs call it.

### 3.2 Servos — "360°" resolved for this design

Now that legs rotate full circles, the servo question sharpens:

| Type | What you get | Fit |
|------|-------------|-----|
| Continuous-rotation (CR) hobby servo | Speed control only, no position | ❌ Can't command "leg to 137°" — gait impossible |
| **360° multi-turn serial bus servo** (Feetech STS3215 / STS3020, Waveshare ST3215) | Absolute position 0–360° (magnetic encoder), multi-turn counting, torque/temp/voltage feedback, daisy-chain UART | ✅ Correct part. Position mode for poses, wheel mode for free spin |
| 270°/300° PWM servo | Position but not full circle | ❌ Dead zone blocks the tumble |
| CR servo + external encoder | Works but adds parts | ❌ Violates the minimal-part ethos |

**Recommendation:** 2× **Feetech STS3215-class** (~19–30 kg·cm, ~55 g each, ~$20).

**Torque check (this design):** worst case the servo lifts the whole robot on a ~107 mm leg (2/3 of ~160 mm case height). All-up mass ~550–650 g → static ~6.5 kg·cm; with a 2–3× dynamic/tumbling factor → need ~15–20 kg·cm. STS3215 covers it with real margin.

### 3.3 BOM (draft v2 — much lighter than v1)

| Qty | Component | Candidate | Est. cost |
|-----|-----------|-----------|-----------|
| 2 | 360° serial bus servo, mag encoder | Feetech STS3215 | $20 ea |
| 1 | Microcontroller | ESP32-S3-WROOM-1 | $8 |
| 1 | PCB (RIBCAGE carrier or custom) | See 3.4 | $10–20 |
| 2 | 18650 cells (2S) | Samsung 35E | $6 ea |
| 1 | 2S BMS/charge module | CN3791-class | $5 |
| 1 | Buck 7.4 V→5 V, 3 A (logic rail) | MP1584-class | $3 |
| 1 | USB-C charge/PD front-end | IP2326 module | $4 |
| 1 | Switch, fuse, wiring, headers | — | $5 |
| — | Printed parts (CASE, 2 legs, brackets) | PETG + TPU leg tips? | ~$6 |
| — | Heat-set inserts + M2/M3 screws | — | $8 |

**Est. total: ~$95–120** (phone excluded). Two-servo architecture halves the v1 estimate.

### 3.4 PCB features (RIBCAGE)

Only two actuators means the board shrinks a lot. Feature list by priority:

**Must have**
- ESP32-S3 module (BLE 5 + native USB)
- Servo bus UART header (2 devices on one daisy chain) at 7.4 V
- 2S input: fuse, reverse-polarity protection, main switch
- Logic rail buck (5 V/3.3 V)
- Battery charging from USB-C (CN3791 2S charger)
- Status RGB LED, UART debug header accessible without disassembly

**Comms with phone — USB-C vs BLE, revisited for this design:**
- BLE remains primary command channel (ADR-004): wireless matters *more* here — the robot tumbles; any external cable is a snag hazard.
- **But:** the docked phone is the biggest battery on the robot. A short *internal* USB-C lead from RIBCAGE to the phone (5 V/2 A from the 2S pack) keeps the brain charged. Internal routing = no snag issue.
- ESP32-S3 native USB also allows a wired data fallback if BLE jitter hurts the control loop. Measure first; the gait here is slow dynamics compared to a running quadruped, so BLE will very likely suffice.

**Nice to have**
- INA226 current sensing on the servo rail (torque proxy → safety reflex trigger)
- On-board IMU (ICM-42688) as ground truth vs. phone IMU for sim-to-real calibration
- Servo rail kill (electronic e-stop from phone)

**Defer:** speaker, extra sensors, PD fast-charge input.

### 3.5 Dimensions & layout

**Anchor:** Pixel 8 = 150.5 × 70.8 × 8.9 mm, 187 g.

**Proposed envelope:**
- CASE: ~162 × 82 × 26–32 mm (phone + ~1.5 mm wall + internal depth for 18650 pair side-by-side *behind* the phone, or end-to-end)
- Legs: ~162 mm long (≈ case height), pivot at ~108 mm from case bottom (2/3 up)
- Leg tip reach below case bottom: ~54 mm → standing clearance when legs point down
- Leg planes: offset outward from case faces by ~8–12 mm so legs sweep 360° clear of the CASE and each other (legs in parallel planes on opposite sides — they can pass each other if timed; safer: keep each leg's swept cylinder fully outside the case silhouette)
- Standing height (on leg tips): ~190 mm; pocketable it is not, but bag-able

**Weight budget:**

| Item | Mass |
|------|------|
| Pixel 8 | 187 g |
| 2× STS3215 | ~110 g |
| 2× 18650 + holder | ~105 g |
| PCB + ESP32 + wiring | ~50 g |
| CASE + legs + brackets (printed) | ~120–150 g |
| **All-up target** | **~570–600 g** |

**Layout principles:**
- **Battery behind the phone, centered** — densest mass nearest the geometric center keeps the tumble axis predictable. CASE rotation symmetry matters: the robot rotates *around its own body* constantly.
- **Servo pods at 2/3 height, integrated into CASE walls** (no separate hip brackets unless printability forces it — that would become a secondary part).
- **RIBCAGE bracket** clips into CASE interior below/behind battery; 4 heat-set inserts max.
- **SHELL** = one back cover over battery + PCB, 2–4 screws.
- **JAW** phone retention: slide-in channel + single spring latch; screen faces out, rear camera clear of the case rim.

### 3.6 Other details worth deciding early

- **Leg cross-section:** the leg is the foot at every angle — a symmetric airfoil/rounded-rectangle section with TPU overmolded (or friction-fit) tip pads at *both* ends. Which end touches ground changes every 180°.
- **Leg end shape:** slightly widened paddle ends = better grip and softer landings; also where the creature's "feet" read visually.
- **Leg sync:** two independent servos = the gait *is* the phase relationship between legs. Firmware needs a phase-locked control primitive (fits the CPG layer nicely — 2-oscillator CPG).
- **Materials:** PETG for CASE and legs (impact), TPU pads; heat-set inserts for servo horn screws.
- **Clearance check in CAD first:** full 360° sweep of both legs vs. case, table edge, and each other — one Fusion 360 motion study before any print.
- **Thermal:** phone is mostly exposed (it's in a case shell, not buried) — good; servos self-report temperature.
- **Sim alignment:** update MuJoCo XML to 2-leg body so trained policies match hardware geometry — critical, currently mismatched.

## Phase 4 — OPEN QUESTIONS (to converge on)

1. Servos: STS3215 confirmed, or cheaper SCS15-class (~15 kg·cm) acceptable?
2. Phone charging from robot battery in v1, or ship v1 without it?
3. Battery: 2S 18650 (~105 g) vs. 2S LiPo pouch (lighter, custom fit)?
4. Leg ends: fixed TPU pads vs. replaceable screw-on tips?
5. CASE thickness: battery behind phone (thicker, symmetric) vs. battery below phone (thinner, off-center CoG)?
6. Do legs ever need to pass *each other* (crossed gait), or are independent swept cylinders the hard constraint?

## Phase 5 — SUGGESTED NEXT STEP

Answer Q1–Q6 → write **ADR-005 (Case Architecture: 2-Leg CASE)** → correct README + hardware README → rebuild the MuJoCo body XML as a 2-leg model so sim matches the real geometry before any Fusion 360 work.
