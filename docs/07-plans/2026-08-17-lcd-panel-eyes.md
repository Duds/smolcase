# Plan: LCD Panel Eyes — TarsFaceView Legibility Revision

**Date:** 2026-08-17
**Spec:** [`docs/06-specs/2026-08-17-lcd-panel-eyes-design.md`](../06-specs/2026-08-17-lcd-panel-eyes-design.md)
**Files touched:** `GlyphGrid.kt`, `TarsFaceView.kt`, `GlyphGridTest.kt` (+ new test class if needed)
**Files NOT touched:** `TelemetryFeed.kt` (takes `lineLength` from grid.cols — adapts automatically), `MainActivity`, `FaceTracker`, `CreatureBrain`, `CreatureVoice`, all llm/*, MuJoCo/sim.

## Approach

TDD where logic is pure (GlyphGrid geometry + quiet-ring), then view rendering, then build/deploy/field-test. One commit per task group.

---

### Task 1 — GlyphGrid: new dimensions + quiet ring (TDD)

Grid goes 14×7 → **18 cols × 9 rows**; pupil 2×2 → **5 cols × 4 rows** (~28% of panel width).

1. Update `GlyphGridTest.kt` first (it hardcodes 14×7 and 2×2):
   - Clamp test: anchor at (1,1) → `(cols-5) to (rows-4)`; beyond-edge clamps identically.
   - Centred gaze: col in 6..8, row in 2..3 (recompute for 18×9).
   - Pupil block test: 5×4 cells at anchor.
   - Lid tests: rows 9 → `visibleRowCount(0f)=9`, `(1f)=1`, `(0.45f)≈5`; symmetric middle row index 4.
   - Squint: unchanged (max 2 rows).
2. New tests for **quiet ring**: `isQuietRingCell(col,row,anchor)` = within Chebyshev distance 1 of the pupil block, excluding pupil cells. Test corners, edges, and that pupil cells themselves are not ring cells.
3. Update `GlyphGrid.kt`: `DEFAULT_COLS=18`, `DEFAULT_ROWS=9`, `PUPIL_COLS=5`, `PUPIL_ROWS=4`; add `isQuietRingCell()`. `pupilAnchor` math is already generic — no change.
4. Run: `gradle testDebugUnitTest --tests "*GlyphGrid*"` → green.

### Task 2 — TarsFaceView: LCD palette + panel rendering

Paints (per spec §1):

| Role | Colour |
|------|--------|
| Panel field | `#B8C4A9` |
| Bezel | `#5A6850` (1 px stroke) |
| Text segments | `#2E3A28`, alpha cap ~140/255 |
| Pupil | `#1A2214`, 100% |
| Glint | `#E8F0DC` |

Changes in `onDraw`/`drawEye`:

1. **Panel geometry:** per eye, panel rect at existing positions (`w*0.08f` / `w*0.62f`, `cy=h/6f`), width `w*0.30f`, height `rows*cellH`, corner radius `0.06f * panelW`.
2. **Draw order:** bezel → panel field → text segments → pupil rect → glint.
3. **Pupil is now positive space:** replace the black punch-out with a solid rounded rect (radius ~20% of cell) in pupil colour spanning the 5×4 anchor block. No `bgPaint` hole.
4. **Quiet ring:** ring cells render text at half segment alpha.
5. **Speaking shimmer inverted:** dark column-scan — segments near `scanCol` get alpha boost toward 255 (darken) instead of brightening.
6. **Lid masking:** clip panel+text+pupil drawing to the visible row span (`firstVisibleRow`/`visibleRowCount`); collapsed area shows screen void, not panel.
7. **State dimming:** DROWSY panel field alpha → 60%; SLEEPING → 25% with existing breathing sine. (Lid droop behaviour unchanged — dimming is additive.)
8. Retire `PHOSPHOR` (#33FF66) from the face; mute dot unchanged.
9. Cell sizing: `cellW = panelW / cols`, `cellH = cellW * 1.15f`, textSize `cellH * 0.9f` — same formulas, denser grid.

### Task 3 — Verify build + unit tests

```bash
cd android
export JAVA_HOME=~/toolchains/jdk-17/Contents/Home
export ANDROID_HOME=~/android-sdk
~/toolchains/gradle-8.7/bin/gradle testDebugUnitTest assembleDebug --console=plain
```

All unit tests green (GlyphGrid + regression on TelemetryFeed/persona/dials), APK builds clean.

### Task 4 — Deploy

```bash
ADB=~/android-sdk/platform-tools/adb
$ADB -s 192.168.0.236:34927 shell am force-stop com.smolcase.companion
$ADB -s 192.168.0.236:34927 install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s 192.168.0.236:34927 shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1
```

(Check `adb mdns services` if the port has changed.)

### Task 5 — Field test (owner + son, per spec §4 "son protocol")

1. Eyes identified unprompted? 2. "Where is it looking?" — can he point? 3. Arm's-length instrument read. Record result in `docs/05-design-thinking/SMOLCASE-Design-Thinking-Log.md`. Fail → escalate to Option B (pixelated pupil fallback, new short spec).

## Risks / notes

- 18×9 at `w*0.30f` → cells ~18 px on a 1080 px-wide screen; textSize ~24 px. Filler glyphs (▓▒░) must stay distinguishable from pupil — salience invariant enforced by alpha cap + quiet ring, verified visually at desk.
- Feed lineLength widens 14→18; telemetry lines just get more room — no truncation risk.
- Idle-gaze-wander spec composes unchanged: quiet ring follows the pupil anchor wherever it glides.
