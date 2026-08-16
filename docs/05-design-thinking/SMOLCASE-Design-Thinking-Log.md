# SMOLCASE Design Thinking Session Log

**Method:** IDEO / d.school 5-phase Design Thinking, run interactively with the owner
**Started:** 2026-08-15 (fresh pass; supersedes the hardware-only brainstorm of 2026-08-14 in scope, not in its technical findings)
**Current phase:** Phase 4 · Prototype (planning complete, build not started)

---

## Phase 1 · Empathize — findings

### Empathy Map: Dale (builder, owner, audience of one)

| | |
|---|---|
| **Says** | "It's for me — a fun little project." "I wanted one of my own, my way." "Failures are iterative challenges; I learn from them." |
| **Thinks** | This is a learning vehicle across four crafts (electronics, ML, 3D printing, code) as much as it is a robot. |
| **Does** | Tinkers iteratively; builds his own version of things he sees (GrowBot video); carries things between home and work desks. |
| **Feels** | Delight at a desk *companion*, not a tool. Zero fear of failure. |
| **Pains** | None about the build itself. Real risk is *soullessness* — a gadget instead of a creature. |
| **Gains** | A living desk companion that walks, expresses, and eventually plugs into his IR5OS world — reminders, events, emails, bills. |

### Insights

1. **Modularity is a feature, not overhead** — visible electronics, swappable parts, and debug access are the curriculum.
2. **Companion first, walker second** — personality/face/presence deserve design weight equal to locomotion.
3. ~~Desk-drop survival is a hard constraint~~ **corrected by owner (2026-08-15):** falls are *repair events*, not catastrophes — carpet at both desks, Pixel 8 is a proven faller, printed parts are consumables. Design goal: **fails cheap, reassembles fast.**
4. **Owner insight — breakaway retention:** on hard impact, phone *separating* from the case may be better (energy dump, F1-style). JAW latch = *calibrated* retention: firm through tumbling, releases on sharp impact, predictable slide-out path.
5. **Owner insight — the phone's stock sensors are the creature's senses:** no capacitive touch needed — high-rate IMU detects touch, location, and manner through the case; WiFi CSI jitter enables gesture/motion/presence (possibly respiration/heartbeat). Note: Android doesn't expose raw CSI on Pixel 8, but the **ESP32 does** — the robot's bridge board doubles as its radar sense. Design principle: *the case adds actuators, not sensors — check software first.*

---

## Phase 2 · Define

**POV (v2, owner-corrected):**

> Dale needs a desk-companion robot that is endlessly open to tinkering and fails cheap, because the robot's real job is a four-craft learning journey that grows a personality — and desk life (knocks, disassembly, reprints) is part of the fun, not a risk to eliminate.

**HMW questions and selection:**

| # | HMW | Status |
|---|-----|--------|
| 1 | Make internals visible/swappable so every repair is a lesson | Carried as design principle (not priority) |
| 2 | Make desk falls cheap/fast repair (breakaway JAW, sacrificial legs) | Downgraded → design-for-repair principle |
| 3 | **Delightful even when not moving (face, presence, sound)** | ✅ **SELECTED** |
| 4 | The case itself teaches (printed part names, exposed fasteners) | Garnish on #1 |
| 5 | **Useful before it walks (reminders/IR5OS while docked)** | ✅ **SELECTED** |
| 6 | Legs as sacrificial consumables | Absorbed into #2 |

---

## Phase 3 · Ideate — full idea inventory (22 ideas, 7 clusters)

### HMW #3 — Delightful when still

**Cluster A — The Face (screen)**
1. ⭐ Animated eyes that track you via front camera (face detection)
2. Eye states as mood: sleepy morning, alert before meetings, slow idle blink
3. Info inside the character: eyes + subtle ticker ("next: dentist Thu")
4. Night mode: two faint glowing eyes, breathing rhythm

**Cluster B — Body language (2 legs = whole-body gesture vocabulary)**
5. Idle micro-motion: weight rocks, leg readjustments — mechanical "breathing"
6. Posture vocabulary: sit, loaf, stretch, cower, perky-stand
7. Excited full-body wiggle (good email, Friday 5pm)
8. Slow spin-in-place = "thinking" indicator

**Cluster C — Sound**
9. Chirps/purrs — tiny non-verbal vocabulary (greeting, affirmation, grumble)
10. Quiet-hours awareness — emotes visually when it must be silent

**Cluster D — Relationship loop (v2, owner-corrected: sensor-free)**
11. ⭐ Tamagotchi-ish persistence: remembers interactions, learns your desk rhythm
12. IMU touch-sensing: pats/scritches/pokes detected (and located) via accelerometer — the whole CASE is the touch sensor
- (CSI presence detection feeds #1/#11 — ESP32 as radar, see insight 5)

### HMW #5 — Useful before it walks

**Cluster E — The Nest (dock)**
13. ⭐ Desk dock/"nest" with charging — "sleeping in the nest" is the charging UX
14. Docked mode = smart display, rendered *as the creature*, not a widget grid
15. Two nests (home + work) — carrying ritual; it knows which desk it's at

**Cluster F — IR5OS bridge**
16. ⭐ Companion app pulls events/reminders/emails/bills → creature expresses them (posture + face + chirp as notification channel)
17. Escalating bill-due "anxiety" animation
18. Meeting countdown fidgeting (5-min warning)
19. Priority-sender email alerts only — charming, not noisy
20. Pomodoro companion — works and stretches alongside you

**Cluster G — Voice & senses**
21. ⭐ Always-listening for your voice: "remind me…", "what's next", in-character replies
22. Speaker answers in character voice, not assistant voice

**⭐ = selected for prototype: #1 (tracking eyes), #11 (persistence), #21 (voice)**

**Key realization:** #3 and #5 are mostly *software* — the bare Pixel 8 can prototype the face, moods, notification reactions, and docked mode before any hardware exists. The case design will inherit a *proven* personality instead of a guessed one.

---

## GrowBot preview page — cross-check (added 2026-08-15)

Cross-checked our decisions against [artoftheproblem.com/pages/growbot-preview](https://artoftheproblem.com/pages/growbot-preview) (already archived as a source in `docs/01-research/`):

| GrowBot page says | Our design log | Verdict |
|---|---|---|
| "Wakes up in any phone… eventually, he'll want legs" — app first, body later | Stream 1 (companion software on bare Pixel 8) precedes hardware | ✅ Independently converged on the same proven sequencing |
| "First thing he does is look for you" | #1 tracking eyes = Stage 1 prototype | ✅ Our Stage 1 was literally GrowBot's first behaviour |
| "Soul file — a unique memory of everything he experiences… no two grow the same" | #11 persistence = Stage 2 | ✅ Their core product asset is our Stage 2 |
| Imprints on ONE person (whoever first wakes it) | Dale is audience of one; greeting/time-away behaviour | ✅ Aligned |
| Touch awakening (tap to wake) | IMU touch-sensing — no extra sensors (owner insight 5) | ✅ GrowBot also used the phone's stock sensors |
| "Snap in any phone" / "He moves into his body" | Our CASE is Pixel 8-specific | ⚠️ Conscious divergence: tailored fit beats universality for a single-owner project. But we keep the *principle*: personality lives in software (soul file), bodies are disposable vessels — matches "fails cheap, reassembles fast" |
| ~$40 parts, 15-minute assembly, "easiest build ever" | Our BOM ~$95–120 (STS3215-class servos) | ⚠️ Conscious divergence: Pixel 8 payload (187 g vs GrowBot's lighter brain) demands bigger servos; simplicity bar still applies to assembly order (ADR-005 §1a) |
| Brain credits / LLM thought loop every 7 s | Kimi LLM personality layer (future) | ✅ Same architecture, deferred |

**Takeaway:** the page validates the roadmap's software-first ordering and all three prototype picks. Two conscious divergences (phone-specific case, servo class) are recorded with rationale.

---

## Phase 4 · Prototype — plan

**"SMOLCASE Companion v0"** — one Android app in `android/`, three stages:

| Stage | Content | Prototype | Success criterion |
|-------|---------|-----------|-------------------|
| 1 | Eyes + face tracking + blink + sleepy idle | A (#1) | You miss it when it's off |
| 2 | + memory, greeting behavior, time-away awareness | B (#11) | You say good morning to it |
| 3 | + always-listening voice, in-character replies | C (#21) | You use a voice command unprompted daily |

Each stage is tested by *living with it* on the desk (Phase 5), then iterating.

**Status:** ✅ Stages 1–3 + LLM layer **built and deployed** (2026-08-16) — tracking eyes + memory/greetings + voice + configurable LLM thinking (Gemini Nano default, Kimi optional) in `android/`, live on the Pixel 8 via wireless adb. Desk test (Phase 5) in progress.

## Phase 5 · Test — 🟢 LIVE (started 2026-08-16)

Companion v0 (Stages 1–3, v0.3) installed on the Pixel 8 via wireless adb.
Desk test in progress: live with the creature, note alive-vs-dead moments.

**Test protocol:** leave it on the desk for 1–2 days. Watch: tracking feel,
blink/saccade timing, GOOD_MORNING landing, BACK_ALREADY restraint, character
voice charm, tap-mute at the work desk. Record observations below.

### Observations
- *(pending — owner's desk-test notes)*

---

## Roadmap

Streams run in parallel; arrows show hard dependencies. No dates — this is a fun project, sequence matters, schedule doesn't.

### Stream 1 — Companion software (bare Pixel 8, no hardware needed)
```
Stage 1 Eyes ──→ Stage 2 Memory ──→ Stage 3 Voice ──→ IR5OS bridge
   (now)            (weeks)           (weeks)      (#16-20: events,
                                                   reminders, bills,
                                                   email alerts)
```

### Stream 2 — Locomotion (sim, already running)
```
Walk Forward ✅ ──→ Walk Backward ──→ Tier 1 complete ──→ Tiers 2-5 curriculum
                                (see docs/02-behaviour-training/)
```

### Stream 3 — Hardware (case + servos)
```
Layout spec v1 ✅ ──→ tolerance test print ──→ Fusion 360 CASE v1 ──→ servo purchase
(mech/)              (pod pocket + phone        (needs STS3215 STEP    (needs variant
                       channel corner)           import + sweep study)  decision: 7.4V)
                          ↓
                     ESP32 BLE bridge bench test (independent of case)
```

### Convergence points

| When | What merges |
|------|-------------|
| After Stage 3 + Tier 1 gaits | Companion app meets walking body — personality gains legs |
| After CASE v1 prints | First full robot: phone + case + 2 legs + ESP32 |
| IR5OS bridge live | Creature becomes useful (#16–20); nest/dock design (#13–15) gets a reason to exist |
| ESP32 CSI experiment | Presence radar sense — upgrades greeting (#1) and persistence (#11) |

### Next actions (priority order)

1. **Build Stage 1 (tracking-eyes app)** — cheapest delight test, starts Phase 5
2. **Train Walk Backward policy** — continues sim stream
3. **Tolerance test print** — validates pod pocket + phone channel before full tub
4. **Decide servo variant** (STS3215 C001 7.4 V recommended) — unblocks purchase + RIBCAGE PCB
5. **ESP32 BLE bridge bench test** — firmware risk retirement, independent of case
