# TASKS.md — SMOLCASE Expedition Task Board

> Living task board. Tasks use `YYYYMMDD-NNN` IDs.
> Decision tickets live on the [Wayfinder Map](docs/07-plans/wayfinder-map.md).
> Detail files in `_tasks/YYYYMMDD-NNN-slug.md`.

---

## 🔥 Now

### Wayfinder Map (Decision Tickets)

> Destination: Ship a desk-ready SMOLCASE robot.
> See `docs/07-plans/wayfinder-map.md` for full dependency chain.

| ID | Ticket | Type | Status |
|----|--------|------|--------|
| 20260829-002 | [Fix conversation quality regressions](_tasks/20260829-002-conversation-quality-fixes.md) | wayfinder:task | 🟡 In progress |
| 20260829-003 | [Decide serial-bus servo model for ~300g robot](_tasks/20260829-003-servo-selection.md) | wayfinder:research | 🔵 Unclaimed |
| 20260829-004 | [Design SC-CASE chassis and leg geometry in CAD](_tasks/20260829-004-sc-case-cad-design.md) | wayfinder:prototype | 🔵 Unclaimed |

### Phase 7: Conversation Quality (20260829-002)

- [x] Verify echo loop fix (cooldown, ignored logs in logcat) — mechanism confirmed, 2.5s cooldown wired through VoiceEars → CreatureVoice.onDone → MainActivity
- [x] Verify prompt no longer repeats "directives" in replies — anti-repetition directive present in CreaturePersona; regression test added
- [x] Add persistent conversation logging — JSONL file in internal storage with ISO-8601 datetime, latency, session ID, build code/name, survives rebuilds
- [x] Bump build to versionCode 2 / 0.6-tars with BuildConfig enabled
- [ ] **Investigate Gemma 7-8s latency** — Paper audit: CPU-only (GPU buggy), `maxNumTokens=1024` is generous for 1-2 sentence replies. Expect ~30-50ms/tok on Tensor G3. Needs logcat timing to confirm whether bottleneck is prompt processing or token generation. Temporarily lower `maxNumTokens` to 128 for test. Device required.
- [ ] **Test cloud TTS provider label field** — Field is wired (save/restore). Default "ElevenLabs". Now uses `styledEditText` for consistent bottom border. `CloudTtsBackend` doesn't read it (metadata only). Device required to verify visibility and save round-trip.
- [ ] **Confirm field + button contrast on device** — Paper audit all pass: white text 13.5:1, value text 9.3:1, hints 6.6:1, accent 4.8:1, buttons 48dp min touch. Device required for real-world viewing angle / brightness check.
- [x] `.gitignore` `models/*.xnnpack_cache` files — done plus regression test for anti-repetition directive

---

## 📋 Next

### Hardware & Mechanical (unblocked after servo decision)

- [ ] 20260829-003 **Decide serial-bus servo model** — benchmark torque, speed, dimensions, voltage for ~300g robot (see _tasks detail)
- [ ] 20260829-004 **Design SC-CASE in CAD** — chassis, leg attachment, mass distribution (see _tasks detail)

### Simulation (unblocked after CAD)

- [ ] 20260829-006 **Train Walk Backward PPO policy** — update MuJoCo XML with final body, target >5m backward (see _tasks detail)

---

## 🔴 Blocked

| ID | Ticket | Blocked By |
|----|--------|------------|
| 20260829-005 | [Decide ESP32 module and battery architecture](_tasks/20260829-005-esp32-battery.md) | 20260829-003 (servo dims → power budget) |
| 20260829-007 | [Implement ESP32 BLE-to-serial-bus bridge firmware](_tasks/20260829-007-esp32-ble-bridge-firmware.md) | 20260829-003, 20260829-005 |
| 20260829-008 | [TFLite policy export and on-device inference pipeline](_tasks/20260829-008-tflite-on-device-pipeline.md) | 20260829-006 (needs trained policy) |
| 20260829-009 | [Integrate behaviour arbiter (brain → BLE → servo)](_tasks/20260829-009-behaviour-arbiter-integration.md) | 20260829-007, 20260829-008 |
| 20260829-010 | [Full 30-behaviour training suite](_tasks/20260829-010-30-behaviour-suite.md) | 20260829-006 (Walk Backward is prerequisite) |

---

## 📦 Backlog

- Characterise thermal dissipation of Pixel 8 inside closed SC-CASE (vent channels / heatsink)
- Sim-to-real transfer validation after first physical build
- Gait transition smoothness tuning (CPG + TFLite blend)
- Mechanical part naming per ADR-005: SC-POD-L/R, SC-RIB, SC-BLY, SC-SHL, SC-JAW
- BOM tracking in `docs/04-hardware/`

---

## 🏁 Completed

| ID | Item | Evidence |
|----|------|----------|
| — | Gemma 4 E2B on-device backend | LiteRT-LM 0.16.0, CPU backend, live inference on Pixel 8 |
| — | Expressive Appliance Dot Matrix Eyes | SDF rasterizer, mood state machine, telemetry pips, 20/20 tests, debug APK |
| — | Settings Expansion & Cloud Intelligence | WCAG AA UI, cloud TTS/Vision/ReplyGen, Pixel sensors, all tests passing |
| — | ADR-001 through ADR-005 | Architecture locked: Pixel 8 brain, hierarchical policies, CPG+MuJoCo+TFLite, ESP32 BLE, 2-leg CASE |
| — | MuJoCo simulation environment & CPG baseline | Working, verified in `sim/src/smolcase_train.py` |
| — | PPO Walk Forward policy | +7.32m at 500K steps |
| — | Empirical eye analysis & photometric study | `docs/01-research/analysis/` |