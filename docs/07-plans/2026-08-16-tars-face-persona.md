# TARS Face & Persona Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the cute cartoon-eyes face with a TARS-style phosphor-green glyph-grid face with scrolling telemetry, retune the voice to a low deadpan register, and add humor/honesty personality dials — shipping as app v0.5-tars.

**Architecture:** New `TarsFaceView` (Canvas-rendered monospace glyph grids) is a drop-in replacement for `EyesView` with the same public API. Rendering math lives in two pure-Kotlin, JVM-testable classes (`GlyphGrid`, `TelemetryFeed`) so the View stays thin. Voice commands for the dials are parsed by a pure `DialCommand` object and routed in `IntentRouter`; the dials are injected into every LLM prompt via `CreaturePersona.prompt(humor, honesty)`.

**Tech Stack:** Kotlin, Android Views (Canvas/Paint), SharedPreferences, JUnit 4 (new test infra), existing ML Kit GenAI / Kimi backends.

**Spec:** `docs/06-specs/2026-08-16-tars-face-persona-design.md` (approved 2026-08-16)

## Global Constraints

- `TarsFaceView` public API must include exactly: `onFaceSeen(nx: Float, ny: Float)`, `express(excitement: Float, happy: Float, blinks: Int)`, `farewell()`, `setSpeaking(value: Boolean)`, `setMicMuted(value: Boolean)` — plus new `setTelemetrySources(sources: List<() -> String?>)` and `cycleDebugMode()`.
- Face: phosphor green `#33FF66` on black; two grids ~14 cols × 7 rows in the upper 1/3 of the screen; lower 2/3 stays black void. No cartoon eyes, no emoji, no cute imagery anywhere.
- Voice: TTS pitch `0.85f`, rate `0.92f`. No smile-pulse.
- Dials: Humor default `75`, Honesty default `90`, range 0–100, persisted.
- Do NOT modify: `FaceTracker.kt`, `VoiceEars.kt`, `llm/LlmBackend.kt` interface, MuJoCo curriculum, any gait/motion code.
- Pure-Kotlin classes (`GlyphGrid`, `TelemetryFeed`, `DialCommand`, `CreaturePersona`) must not import Android APIs — they run in JVM unit tests.
- Build: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain`
- Unit tests: same env, `~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
- Deploy: `~/android-sdk/platform-tools/adb -s 192.168.0.236:34927 install -r app/build/outputs/apk/debug/app-debug.apk` (phone "shiba", wireless adb; if the port drops, check `~/android-sdk/platform-tools/adb mdns services`).
- Every task ends with a commit. Repo root: `/Users/dalerogers/20-INDIE/products/SMOLCASE`.

---

### Task 1: Test infrastructure + `GlyphGrid`

**Files:**
- Modify: `android/app/build.gradle.kts` (add junit test dependency)
- Create: `android/app/src/main/java/com/smolcase/companion/GlyphGrid.kt`
- Test: `android/app/src/test/java/com/smolcase/companion/GlyphGridTest.kt`

**Interfaces:**
- Consumes: nothing (first task).
- Produces: `GlyphGrid(cols: Int = 14, rows: Int = 7)` with `pupilAnchor(nx: Float, ny: Float): Pair<Int, Int>`, `isPupilCell(col: Int, row: Int, anchor: Pair<Int, Int>): Boolean`, `visibleRowCount(lid: Float): Int`, `firstVisibleRow(lid: Float): Int`, `isRowVisible(row: Int, lid: Float): Boolean`, `squintRows(squint: Float): Int`. Used by `TarsFaceView` (Task 3).

- [ ] **Step 1: Add the JUnit dependency**

In `android/app/build.gradle.kts`, add at the end of the `dependencies { }` block:

```kotlin
    // Unit tests (pure-Kotlin face math runs on the JVM)
    testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 2: Write the failing test**

Create `android/app/src/test/java/com/smolcase/companion/GlyphGridTest.kt`:

```kotlin
package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphGridTest {

    private val grid = GlyphGrid() // 14 cols x 7 rows

    @Test
    fun `pupil anchor clamps to keep the 2x2 block inside the grid`() {
        assertEquals(0 to 0, grid.pupilAnchor(-1f, -1f))
        assertEquals((grid.cols - 2) to (grid.rows - 2), grid.pupilAnchor(1f, 1f))
        // Gaze beyond the edge still clamps
        assertEquals((grid.cols - 2) to (grid.rows - 2), grid.pupilAnchor(5f, 5f))
    }

    @Test
    fun `pupil anchor sits near the middle for a centered gaze`() {
        val (col, row) = grid.pupilAnchor(0f, 0f)
        assertTrue(col in 5..7)
        assertTrue(row in 2..3)
    }

    @Test
    fun `pupil cells form a 2x2 block at the anchor`() {
        val anchor = 3 to 2
        assertTrue(grid.isPupilCell(3, 2, anchor))
        assertTrue(grid.isPupilCell(4, 3, anchor))
        assertFalse(grid.isPupilCell(5, 2, anchor))
        assertFalse(grid.isPupilCell(3, 4, anchor))
    }

    @Test
    fun `lid zero shows all rows, lid one collapses to a single middle row`() {
        assertEquals(7, grid.visibleRowCount(0f))
        assertEquals(1, grid.visibleRowCount(1f))
        assertEquals(1, grid.visibleRowCount(5f)) // clamps
    }

    @Test
    fun `drowsy lid of 0 point 45 shows about half the rows`() {
        assertEquals(4, grid.visibleRowCount(0.45f))
    }

    @Test
    fun `collapse is symmetric around the grid middle`() {
        // lid = 1: only the middle row (index 3 of 0..6) survives
        assertEquals(3, grid.firstVisibleRow(1f))
        assertTrue(grid.isRowVisible(3, 1f))
        assertFalse(grid.isRowVisible(2, 1f))
        assertFalse(grid.isRowVisible(4, 1f))
        // lid = 0: everything visible
        assertTrue(grid.isRowVisible(0, 0f))
        assertTrue(grid.isRowVisible(6, 0f))
    }

    @Test
    fun `squint lifts at most two bottom rows`() {
        assertEquals(0, grid.squintRows(0f))
        assertEquals(1, grid.squintRows(0.5f))
        assertEquals(2, grid.squintRows(1f))
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: FAIL — compilation error, `GlyphGrid` unresolved.

- [ ] **Step 4: Implement `GlyphGrid`**

Create `android/app/src/main/java/com/smolcase/companion/GlyphGrid.kt`:

```kotlin
package com.smolcase.companion

import kotlin.math.roundToInt

/**
 * Pure geometry/state math for the TARS glyph-grid face.
 * No Android imports — unit-testable on the JVM.
 *
 * A grid is [cols] x [rows] monospace cells. The pupil is a 2x2 block of
 * full-block glyphs gliding inside the grid. The "lid" collapses rows
 * symmetrically toward the vertical middle (blink), droops (drowsy), or
 * leaves a single middle row (sleeping).
 */
class GlyphGrid(
    val cols: Int = DEFAULT_COLS,
    val rows: Int = DEFAULT_ROWS
) {

    /** Top-left cell of the 2x2 pupil block for a normalized gaze (-1..1). */
    fun pupilAnchor(nx: Float, ny: Float): Pair<Int, Int> {
        val maxCol = cols - PUPIL_COLS
        val maxRow = rows - PUPIL_ROWS
        val col = ((nx.coerceIn(-1f, 1f) + 1f) / 2f * maxCol).roundToInt().coerceIn(0, maxCol)
        val row = ((ny.coerceIn(-1f, 1f) + 1f) / 2f * maxRow).roundToInt().coerceIn(0, maxRow)
        return col to row
    }

    fun isPupilCell(col: Int, row: Int, anchor: Pair<Int, Int>): Boolean =
        col >= anchor.first && col < anchor.first + PUPIL_COLS &&
            row >= anchor.second && row < anchor.second + PUPIL_ROWS

    /**
     * Rows visible for [lid] (0 = open, 1 = collapsed). Collapse is
     * symmetric toward the middle; at least one thin middle row remains.
     */
    fun visibleRowCount(lid: Float): Int =
        (rows * (1f - lid.coerceIn(0f, 1f))).roundToInt().coerceIn(1, rows)

    /** First visible row index — visible rows center on the grid middle. */
    fun firstVisibleRow(lid: Float): Int = (rows - visibleRowCount(lid)) / 2

    fun isRowVisible(row: Int, lid: Float): Boolean {
        val first = firstVisibleRow(lid)
        return row >= first && row < first + visibleRowCount(lid)
    }

    /** Happy squint lifts bottom rows (the "^^" read). */
    fun squintRows(squint: Float): Int =
        (squint.coerceIn(0f, 1f) * SQUINT_MAX_ROWS).roundToInt()

    companion object {
        const val DEFAULT_COLS = 14
        const val DEFAULT_ROWS = 7
        const val PUPIL_COLS = 2
        const val PUPIL_ROWS = 2
        const val SQUINT_MAX_ROWS = 2
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 7 tests pass.

- [ ] **Step 6: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/build.gradle.kts android/app/src/main/java/com/smolcase/companion/GlyphGrid.kt android/app/src/test/java/com/smolcase/companion/GlyphGridTest.kt
git commit -m "feat: add GlyphGrid pure face math with JVM tests"
```

---

### Task 2: `TelemetryFeed`

**Files:**
- Create: `android/app/src/main/java/com/smolcase/companion/TelemetryFeed.kt`
- Test: `android/app/src/test/java/com/smolcase/companion/TelemetryFeedTest.kt`

**Interfaces:**
- Consumes: nothing from Task 1 (independent).
- Produces: `TelemetryFeed(lineLength: Int, sources: List<() -> String?> = emptyList(), nowMs: () -> Long = { System.currentTimeMillis() }, random: Random = Random.Default, clockLine: () -> String = { "" })` with `enum class Mode { NORMAL, ALL_FILLER, ALL_TELEMETRY, FROZEN }`, `var mode: Mode`, `var sleeping: Boolean`, `fun tick()`, `fun advance()`, `fun nextLine(): String`, `fun window(height: Int): List<String>`, `fun fillerLine(): String`. Used by `TarsFaceView` (Task 3) and `MainActivity` (Task 8).

- [ ] **Step 1: Write the failing test**

Create `android/app/src/test/java/com/smolcase/companion/TelemetryFeedTest.kt`:

```kotlin
package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TelemetryFeedTest {

    private var now = 0L
    private fun feed(
        sources: List<() -> String?> = emptyList(),
        clock: String = "CLK 09:41"
    ) = TelemetryFeed(
        lineLength = 14,
        sources = sources,
        nowMs = { now },
        random = Random(42),
        clockLine = { clock }
    )

    @Test
    fun `filler lines fill the grid width and use low-density glyphs`() {
        val f = feed()
        val line = f.fillerLine()
        assertEquals(14, line.length)
        assertTrue(line.all { it in "▓▒░:· 0123456789ABCDEF" })
    }

    @Test
    fun `telemetry sources rotate round-robin`() {
        val f = feed(sources = listOf({ "AAAA" }, { "BBBB" }))
        assertEquals("AAAA", f.nextLine())
        assertEquals("BBBB", f.nextLine())
        assertEquals("AAAA", f.nextLine())
    }

    @Test
    fun `empty or failing sources fall back to filler in NORMAL mode`() {
        val f = feed(sources = listOf({ null }, { throw RuntimeException("boom") }))
        val line = f.nextLine()
        assertEquals(14, line.length) // silently became filler
    }

    @Test
    fun `ALL_FILLER ignores sources, ALL_TELEMETRY never emits filler`() {
        val f = feed(sources = listOf({ "REAL DATA" }))
        f.mode = TelemetryFeed.Mode.ALL_FILLER
        assertTrue(f.nextLine() != "REAL DATA")
        f.mode = TelemetryFeed.Mode.ALL_TELEMETRY
        assertEquals("REAL DATA", f.nextLine())
        val empty = feed(sources = listOf({ null }))
        empty.mode = TelemetryFeed.Mode.ALL_TELEMETRY
        assertEquals("", empty.nextLine()) // blank, never filler
    }

    @Test
    fun `sleeping feed emits only clock or filler`() {
        val f = feed(sources = listOf({ "SESS 0001" }), clock = "CLK 02:00")
        f.sleeping = true
        repeat(10) {
            val line = f.nextLine()
            assertTrue(line == "CLK 02:00" || line.all { it in "▓▒░:· 0123456789ABCDEF" })
        }
    }

    @Test
    fun `window returns the newest lines oldest-first and pads with blanks`() {
        val f = feed(sources = listOf({ "ONE" }, { "TWO" }, { "THREE" }))
        f.advance(); f.advance()
        val w = f.window(4)
        assertEquals(listOf("", "", "ONE", "TWO"), w)
    }

    @Test
    fun `tick advances about two rows per second awake, slower sleeping`() {
        val f = feed(sources = listOf({ "ROW" }))
        f.tick()                       // now=0: advances, next due at 500
        now += 499; f.tick()           // 499: not due
        now += 1; f.tick()             // 500: advances, next due at 1000
        assertEquals(2, f.window(7).count { it.isNotEmpty() })
        f.sleeping = true
        now += 499; f.tick()           // 999: not due
        now += 1; f.tick()             // 1000: advances, sleeping so next due at 3000
        assertEquals(3, f.window(7).count { it.isNotEmpty() })
        now += 1999; f.tick()          // 2999: not due (sleeping interval)
        assertEquals(3, f.window(7).count { it.isNotEmpty() })
        now += 1; f.tick()             // 3000: due
        assertEquals(4, f.window(7).count { it.isNotEmpty() })
    }

    @Test
    fun `FROZEN mode stops the scroll`() {
        val f = feed(sources = listOf({ "STOP" }))
        f.tick()
        f.mode = TelemetryFeed.Mode.FROZEN
        now += 10_000
        f.tick()
        assertEquals(listOf("STOP"), f.window(1))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: FAIL — compilation error, `TelemetryFeed` unresolved.

- [ ] **Step 3: Implement `TelemetryFeed`**

Create `android/app/src/main/java/com/smolcase/companion/TelemetryFeed.kt`:

```kotlin
package com.smolcase.companion

import kotlin.random.Random

/**
 * The scrolling text living inside the glyph grids: real telemetry lines
 * when sources have them, weighted filler glyphs otherwise — quiet
 * instrumentation, never an error state. Pure Kotlin; time and randomness
 * are injected so it runs in JVM unit tests.
 */
class TelemetryFeed(
    private val lineLength: Int,
    private val sources: List<() -> String?> = emptyList(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val random: Random = Random.Default,
    private val clockLine: () -> String = { "" }
) {

    enum class Mode { NORMAL, ALL_FILLER, ALL_TELEMETRY, FROZEN }

    @Volatile var mode: Mode = Mode.NORMAL
    @Volatile var sleeping: Boolean = false

    private val lines = ArrayDeque<String>() // oldest first
    private var nextAdvanceAt = 0L
    private var sourceCursor = 0

    /** Called every frame by the view; advances the scroll when due. */
    fun tick() {
        if (mode == Mode.FROZEN) return
        val now = nowMs()
        if (nextAdvanceAt == 0L || now >= nextAdvanceAt) {
            advance()
            nextAdvanceAt = now + intervalMs()
        }
    }

    /** Push the next line onto the scroll. */
    fun advance() {
        lines.addLast(nextLine())
        while (lines.size > MAX_LINES) lines.removeFirst()
    }

    /** The newest [height] lines, oldest first, padded with blanks. */
    fun window(height: Int): List<String> {
        val tail = lines.toList().takeLast(height)
        return List(height - tail.size) { "" } + tail
    }

    fun nextLine(): String {
        if (sleeping) return if (random.nextInt(4) == 0) fillerLine() else clockLine()
        return when (mode) {
            Mode.ALL_FILLER -> fillerLine()
            Mode.ALL_TELEMETRY -> telemetryLine() ?: ""
            Mode.NORMAL -> telemetryLine() ?: fillerLine()
            Mode.FROZEN -> lines.lastOrNull() ?: ""
        }
    }

    /** Next non-blank source line (round-robin). Any failure → null. */
    private fun telemetryLine(): String? {
        if (sources.isEmpty()) return null
        repeat(sources.size) {
            val src = sources[sourceCursor % sources.size]
            sourceCursor++
            val line = try { src() } catch (e: Exception) { null }
            if (!line.isNullOrBlank()) return line.take(lineLength)
        }
        return null
    }

    /** Weighted low-density filler: ▓▒░:· with occasional hex pairs. */
    fun fillerLine(): String = buildString {
        while (length < lineLength) {
            if (random.nextInt(8) == 0 && length + 3 <= lineLength) {
                append(random.nextInt(256).toString(16).uppercase().padStart(2, '0'))
                append(' ')
            } else {
                append(fillerChar())
            }
        }
    }.take(lineLength)

    private fun fillerChar(): Char {
        val total = FILLER_WEIGHTS.sumOf { it.second }
        var roll = random.nextInt(total)
        for ((ch, weight) in FILLER_WEIGHTS) {
            if (roll < weight) return ch
            roll -= weight
        }
        return '·'
    }

    private fun intervalMs(): Long = if (sleeping) 2_000L else 500L // ~2 rows/s awake

    companion object {
        private const val MAX_LINES = 32
        private val FILLER_WEIGHTS = listOf(
            '░' to 40, '·' to 25, ':' to 15, '▒' to 12, '▓' to 5, ' ' to 3
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 8 new tests pass (15 total).

- [ ] **Step 5: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/TelemetryFeed.kt android/app/src/test/java/com/smolcase/companion/TelemetryFeedTest.kt
git commit -m "feat: add TelemetryFeed scrolling glyph text with JVM tests"
```

---

### Task 3: `TarsFaceView` — the new face, swapped in

**Files:**
- Create: `android/app/src/main/java/com/smolcase/companion/TarsFaceView.kt`
- Modify: `android/app/src/main/java/com/smolcase/companion/MainActivity.kt` (field type + construction only)
- Modify: `android/app/src/main/java/com/smolcase/companion/CreatureBrain.kt:17-19` (constructor param type)
- Modify: `android/app/src/main/java/com/smolcase/companion/CreatureVoice.kt` (param type + voice retune)
- Delete: `android/app/src/main/java/com/smolcase/companion/EyesView.kt`

**Interfaces:**
- Consumes: `GlyphGrid` (Task 1), `TelemetryFeed` (Task 2).
- Produces: `TarsFaceView(context, attrs)` with the full public API from Global Constraints. After this task the app builds and runs with the new face; telemetry sources arrive in Task 8.

Note: no JVM unit test for the View itself (Android Canvas). Verification is compile + deploy + on-device eyeball. The pure math behind it is already tested.

- [ ] **Step 1: Create `TarsFaceView.kt`**

```kotlin
package com.smolcase.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The TARS face: two phosphor-green monospace glyph grids in the upper
 * third of the screen; the lower two-thirds stays black void.
 *
 * Each grid scrolls telemetry/filler text (TelemetryFeed) around a 2x2
 * full-block pupil (GlyphGrid) that tracks your face. Blinks collapse the
 * rows symmetrically; speaking sweeps a bright column-scan shimmer.
 *
 * Drop-in replacement for the old cartoon EyesView — same public API:
 *  onFaceSeen / express / farewell / setSpeaking / setMicMuted
 * plus setTelemetrySources() and cycleDebugMode() (triple-tap).
 *
 * Base states (face-presence timing, unchanged):
 *  AWAKE    — pupil tracks you; random blinks; micro-saccades
 *  DROWSY   — no face for 30s: half rows
 *  SLEEPING — no face for 120s: single dim middle row, slow clock feed
 */
class TarsFaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class State { AWAKE, DROWSY, SLEEPING }

    // ---- timing constants (ms) ----
    private val drowsyAfterMs = 30_000L
    private val sleepAfterMs = 120_000L
    private val blinkDurationMs = 140L
    private val farewellBlinkDurationMs = 600L
    private val pupilEase = 0.18f
    private val lidEase = 0.12f
    private val excitementDecay = 0.982f
    private val happyDecay = 0.990f

    // ---- creature state ----
    @Volatile private var faceTarget: PointF? = null
    @Volatile private var lastFaceAt: Long = 0L

    private val pupil = PointF(0f, 0f)
    private var lid = 0f
    private var blinkStartedAt = 0L
    private var blinkDuration = blinkDurationMs
    private var nextBlinkAt = 0L
    private var pendingBlinks = 0
    private val saccade = PointF(0f, 0f)
    private var nextSaccadeAt = 0L

    // ---- expression layer (subtler than the cartoon face) ----
    private var excitement = 0f
    private var happy = 0f

    // ---- voice layer ----
    @Volatile private var speaking = false
    @Volatile private var micMuted = false

    // ---- glyph model ----
    private val grid = GlyphGrid()
    private var feedL = TelemetryFeed(lineLength = grid.cols)
    private var feedR = TelemetryFeed(lineLength = grid.cols)

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PHOSPHOR
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
    }
    private val mutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x66, 0x33, 0x33)
    }

    // ---- public API ----

    /** Face observed at normalized position (-1..1). */
    fun onFaceSeen(nx: Float, ny: Float) {
        faceTarget = PointF(nx.coerceIn(-1f, 1f), ny.coerceIn(-1f, 1f))
        lastFaceAt = SystemClock.uptimeMillis()
    }

    /** Greeting/expression: brief brightness bump, squint, queued blinks. */
    fun express(excitement: Float, happy: Float, blinks: Int) {
        this.excitement = excitement.coerceIn(0f, 1f)
        this.happy = happy.coerceIn(0f, 1f)
        pendingBlinks += blinks
    }

    /** You left: one long, slow collapse. */
    fun farewell() {
        if (blinkStartedAt == 0L) {
            blinkDuration = farewellBlinkDurationMs
            blinkStartedAt = SystemClock.uptimeMillis()
        }
        happy = 0f
    }

    /** The creature is talking: column-scan shimmer for the duration. */
    fun setSpeaking(value: Boolean) {
        speaking = value
    }

    /** Mic muted indicator (privacy switch). */
    fun setMicMuted(value: Boolean) {
        micMuted = value
    }

    /**
     * Wire real telemetry into both grids. Sources are shared; the right
     * eye runs on a +250ms phase offset so the two read as related
     * instruments, not mirrors. Safe to call again (replaces the feeds).
     */
    fun setTelemetrySources(sources: List<() -> String?>) {
        feedL = TelemetryFeed(lineLength = grid.cols, sources = sources)
        feedR = TelemetryFeed(
            lineLength = grid.cols,
            sources = sources,
            nowMs = { System.currentTimeMillis() + PHASE_OFFSET_MS }
        )
    }

    /** Triple-tap: NORMAL → ALL_FILLER → ALL_TELEMETRY → FROZEN → NORMAL. */
    fun cycleDebugMode() {
        val modes = TelemetryFeed.Mode.values()
        val next = modes[(feedL.mode.ordinal + 1) % modes.size]
        feedL.mode = next
        feedR.mode = next
    }

    // ---- frame loop ----

    private val frameCallback = object : Runnable {
        override fun run() {
            tick()
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        nextBlinkAt = SystemClock.uptimeMillis() + Random.nextLong(2_000, 5_000)
        postOnAnimation(frameCallback)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frameCallback)
        super.onDetachedFromWindow()
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()

        if (speaking) {
            if (excitement < 0.15f) excitement = 0.15f // faint scan glow
        } else {
            excitement *= excitementDecay
            happy *= happyDecay
            if (excitement < 0.01f) excitement = 0f
            if (happy < 0.01f) happy = 0f
        }

        if (pendingBlinks > 0 && blinkStartedAt == 0L) {
            pendingBlinks--
            blinkDuration = blinkDurationMs
            blinkStartedAt = now
        }

        if (state(now) != State.SLEEPING && blinkStartedAt == 0L &&
            pendingBlinks == 0 && now >= nextBlinkAt
        ) {
            blinkDuration = blinkDurationMs
            blinkStartedAt = now
        }

        if (state(now) == State.AWAKE && now >= nextSaccadeAt) {
            saccade.set(Random.nextFloat() * 0.08f - 0.04f, Random.nextFloat() * 0.06f - 0.03f)
            nextSaccadeAt = now + Random.nextLong(1_500, 4_000)
        }

        val t = faceTarget
        val tx = (t?.x ?: 0f) + saccade.x
        val ty = (t?.y ?: 0f) + saccade.y
        pupil.x += (tx - pupil.x) * pupilEase
        pupil.y += (ty - pupil.y) * pupilEase

        lid += (lidTarget(now) - lid) * lidEase

        val sleeping = state(now) == State.SLEEPING
        feedL.sleeping = sleeping
        feedR.sleeping = sleeping
        feedL.tick()
        feedR.tick()
    }

    private fun state(now: Long): State {
        val since = now - lastFaceAt
        return when {
            since < drowsyAfterMs -> State.AWAKE
            since < sleepAfterMs -> State.DROWSY
            else -> State.SLEEPING
        }
    }

    private fun lidTarget(now: Long): Float {
        if (blinkStartedAt != 0L) {
            val p = (now - blinkStartedAt).toFloat() / blinkDuration
            if (p >= 1f) {
                blinkStartedAt = 0L
                nextBlinkAt = now + Random.nextLong(3_000, 7_000)
            } else {
                return if (p < 0.5f) p * 2f else (1f - p) * 2f
            }
        }
        return when (state(now)) {
            State.AWAKE -> 0f
            State.DROWSY -> 0.45f
            State.SLEEPING -> 1f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val now = SystemClock.uptimeMillis()
        val st = state(now)

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Two grids in the upper third
        val eyeW = w * 0.30f
        val cy = h / 6f
        val cellW = eyeW / grid.cols
        val cellH = cellW * 1.15f // monospace aspect
        glyphPaint.textSize = cellH * 0.9f

        val breathing = if (st == State.SLEEPING) 0.35f + 0.1f * sin(now / 1_500f) else 1f
        val glow = (0.75f + 0.25f * excitement) * breathing
        val squint = grid.squintRows(happy)
        val pupilAnchor = grid.pupilAnchor(pupil.x, pupil.y)
        val scanCol = ((now / 70L) % grid.cols).toInt()

        drawEye(canvas, w * 0.27f, cy, cellW, cellH, feedL, pupilAnchor, squint, glow, scanCol)
        drawEye(canvas, w * 0.73f, cy, cellW, cellH, feedR, pupilAnchor, squint, glow, scanCol)

        if (micMuted) {
            canvas.drawCircle(w * 0.5f, cy + grid.rows * cellH, min(w, h) * 0.012f, mutePaint)
        }
    }

    private fun drawEye(
        canvas: Canvas,
        left: Float,
        centerY: Float,
        cellW: Float,
        cellH: Float,
        feed: TelemetryFeed,
        pupilAnchor: Pair<Int, Int>,
        squint: Int,
        glow: Float,
        scanCol: Int
    ) {
        val window = feed.window(grid.rows)
        val top = centerY - grid.rows * cellH / 2f
        val firstVisible = grid.firstVisibleRow(lid)
        val visibleCount = grid.visibleRowCount(lid)

        for (row in 0 until grid.rows) {
            // Lid collapse hides rows; squint hides additional bottom rows
            if (!grid.isRowVisible(row, lid)) continue
            if (row >= firstVisible + visibleCount - squint && squint > 0) continue

            val line = window[row]
            val y = top + row * cellH + cellH * 0.8f // baseline
            for (col in 0 until grid.cols) {
                val isPupil = grid.isPupilCell(col, row, pupilAnchor)
                val ch = if (isPupil) '█' else line.getOrElse(col) { ' ' }
                if (ch == ' ') continue

                var alpha = (255 * glow).toInt()
                if (isPupil) alpha = 255
                if (speaking) {
                    // Column-scan shimmer: a bright band sweeps left to right
                    val dist = abs(col - scanCol)
                    alpha = min(255, alpha + (160 * (1f - dist / 4f)).toInt().coerceAtLeast(0))
                }
                glyphPaint.alpha = alpha.coerceIn(0, 255)
                canvas.drawText(ch.toString(), left + col * cellW, y, glyphPaint)
            }
        }
    }

    companion object {
        private val PHOSPHOR = Color.rgb(0x33, 0xFF, 0x66)
        private const val PHASE_OFFSET_MS = 250L
    }
}
```

- [ ] **Step 2: Swap `MainActivity` to the new face**

In `android/app/src/main/java/com/smolcase/companion/MainActivity.kt`:

- Change `private lateinit var eyesView: EyesView` → `private lateinit var eyesView: TarsFaceView`
- Change `eyesView = EyesView(this)` → `eyesView = TarsFaceView(this)`

(Field name stays `eyesView`; everything else in this file still compiles because the API is identical.)

- [ ] **Step 3: Repoint `CreatureBrain` and `CreatureVoice`, retune the voice**

In `android/app/src/main/java/com/smolcase/companion/CreatureBrain.kt`, change the constructor parameter type:

```kotlin
class CreatureBrain(
    context: Context,
    private val eyes: TarsFaceView
) {
```

In `android/app/src/main/java/com/smolcase/companion/CreatureVoice.kt`:

- Change `private val eyes: EyesView` → `private val eyes: TarsFaceView`
- Change `setPitch(1.55f)` → `setPitch(0.85f)` and `setSpeechRate(0.95f)` → `setSpeechRate(0.92f)`
- Update the class KDoc to:

```kotlin
/**
 * The creature's voice (Stage 5, TARS persona).
 *
 * Text-to-speech tuned for TARS: low, measured, deadpan — a marine company
 * commander by way of a gym teacher, not a cute mascot. The glyph face
 * shimmers (column-scan) while it speaks.
 */
```

- [ ] **Step 4: Delete `EyesView.kt`**

```bash
rm /Users/dalerogers/20-INDIE/products/SMOLCASE/android/app/src/main/java/com/smolcase/companion/EyesView.kt
```

- [ ] **Step 5: Build**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Deploy and eyeball**

```bash
ADB=~/android-sdk/platform-tools/adb
$ADB -s 192.168.0.236:34927 shell am force-stop com.smolcase.companion
$ADB -s 192.168.0.236:34927 install -r /Users/dalerogers/20-INDIE/products/SMOLCASE/android/app/build/outputs/apk/debug/app-debug.apk
$ADB -s 192.168.0.236:34927 shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1
```

Expected on the phone: two green glyph grids in the upper third, pupil blocks tracking your face, random blinks collapsing the rows, filler glyphs scrolling, black void below. Nothing cute.

- [ ] **Step 7: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add -A android/app/src/main/java/com/smolcase/companion/
git commit -m "feat: TarsFaceView glyph-grid face replaces cartoon EyesView; voice retuned low/deadpan"
```

---

### Task 4: `PersonalityDials` + `DialCommand` parser

**Files:**
- Create: `android/app/src/main/java/com/smolcase/companion/PersonalityDials.kt`
- Create: `android/app/src/main/java/com/smolcase/companion/DialCommand.kt`
- Test: `android/app/src/test/java/com/smolcase/companion/DialCommandTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `PersonalityDials(context)` with `var humor: Int` / `var honesty: Int` (defaults 75/90, persisted in SharedPreferences `smolcase_dials`); `DialCommand.parse(text: String): DialCommand.Result?` where `Result` is `data class Set(val dial: Dial, val value: Int)` or `data class Rejected(val dial: Dial, val value: Int)`, `enum class Dial { HUMOR, HONESTY }`. Consumed by `IntentRouter` (Task 6), `ConversationEngine` (Task 5), `SettingsActivity` (Task 9), `MainActivity` telemetry (Task 8).

- [ ] **Step 1: Write the failing test**

Create `android/app/src/test/java/com/smolcase/companion/DialCommandTest.kt`:

```kotlin
package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialCommandTest {

    @Test
    fun `set humor to 90 parses`() {
        val r = DialCommand.parse("set humor to 90")
        assertEquals(DialCommand.Result.Set(DialCommand.Dial.HUMOR, 90), r)
    }

    @Test
    fun `honesty 50 percent parses without the word set`() {
        val r = DialCommand.parse("honesty 50 percent")
        assertEquals(DialCommand.Result.Set(DialCommand.Dial.HONESTY, 50), r)
    }

    @Test
    fun `case and british spelling are accepted`() {
        assertEquals(
            DialCommand.Result.Set(DialCommand.Dial.HUMOR, 60),
            DialCommand.parse("Set HUMOUR to 60")
        )
    }

    @Test
    fun `percent sign is accepted`() {
        assertEquals(
            DialCommand.Result.Set(DialCommand.Dial.HONESTY, 100),
            DialCommand.parse("honesty 100%")
        )
    }

    @Test
    fun `out of range is rejected`() {
        val r = DialCommand.parse("set humor to 250")
        assertEquals(DialCommand.Result.Rejected(DialCommand.Dial.HUMOR, 250), r)
    }

    @Test
    fun `non-dial speech returns null`() {
        assertNull(DialCommand.parse("what's next"))
        assertNull(DialCommand.parse("remind me to call mum"))
        assertNull(DialCommand.parse("humor")) // no value
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: FAIL — `DialCommand` unresolved.

- [ ] **Step 3: Implement `DialCommand` and `PersonalityDials`**

Create `android/app/src/main/java/com/smolcase/companion/DialCommand.kt`:

```kotlin
package com.smolcase.companion

/**
 * Parses personality-dial voice commands: "set humor to 90",
 * "honesty 50 percent". Pure Kotlin, unit-tested; no Android imports.
 */
object DialCommand {

    enum class Dial { HUMOR, HONESTY }

    sealed class Result {
        data class Set(val dial: Dial, val value: Int) : Result()
        data class Rejected(val dial: Dial, val value: Int) : Result()
    }

    private val pattern = Regex(
        """(?:set\s+)?(humou?r|honesty)(?:\s+to)?\s+(\d{1,3})(?:\s*(?:percent|%))?"""
    )

    /** Parse a dial command, or null if the speech isn't one. */
    fun parse(rawText: String): Result? {
        val match = pattern.find(rawText.lowercase().trim()) ?: return null
        val dial = if (match.groupValues[1].startsWith("humo")) Dial.HUMOR else Dial.HONESTY
        val value = match.groupValues[2].toInt()
        return if (value in 0..100) Result.Set(dial, value) else Result.Rejected(dial, value)
    }
}
```

Create `android/app/src/main/java/com/smolcase/companion/PersonalityDials.kt`:

```kotlin
package com.smolcase.companion

import android.content.Context

/**
 * The creature's personality settings: HUMOR and HONESTY percentages,
 * TARS-style. Persisted; edited by voice (IntentRouter), sliders
 * (SettingsActivity), and injected into every LLM prompt.
 */
class PersonalityDials(context: Context) {

    private val prefs = context.getSharedPreferences("smolcase_dials", Context.MODE_PRIVATE)

    var humor: Int
        get() = prefs.getInt(KEY_HUMOR, DEFAULT_HUMOR)
        set(v) = prefs.edit().putInt(KEY_HUMOR, v.coerceIn(0, 100)).apply()

    var honesty: Int
        get() = prefs.getInt(KEY_HONESTY, DEFAULT_HONESTY)
        set(v) = prefs.edit().putInt(KEY_HONESTY, v.coerceIn(0, 100)).apply()

    companion object {
        const val DEFAULT_HUMOR = 75
        const val DEFAULT_HONESTY = 90
        private const val KEY_HUMOR = "humor"
        private const val KEY_HONESTY = "honesty"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 6 new tests pass (21 total).

- [ ] **Step 5: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/PersonalityDials.kt android/app/src/main/java/com/smolcase/companion/DialCommand.kt android/app/src/test/java/com/smolcase/companion/DialCommandTest.kt
git commit -m "feat: personality dials (humor/honesty) with voice-command parser"
```

---

### Task 5: TARS persona prompt + LLM backend wiring

**Files:**
- Modify: `android/app/src/main/java/com/smolcase/companion/llm/LlmBackend.kt` (CreaturePersona object)
- Modify: `android/app/src/main/java/com/smolcase/companion/llm/GeminiNanoBackend.kt`
- Modify: `android/app/src/main/java/com/smolcase/companion/llm/KimiBackend.kt`
- Modify: `android/app/src/main/java/com/smolcase/companion/ConversationEngine.kt`
- Test: `android/app/src/test/java/com/smolcase/companion/CreaturePersonaTest.kt`

**Interfaces:**
- Consumes: `PersonalityDials` (Task 4).
- Produces: `CreaturePersona.prompt(humor: Int, honesty: Int): String`. `GeminiNanoBackend(context, dials)` and `KimiBackend(settings, dials)` constructor signatures — wired by `ConversationEngine`.

- [ ] **Step 1: Write the failing test**

Create `android/app/src/test/java/com/smolcase/companion/CreaturePersonaTest.kt`:

```kotlin
package com.smolcase.companion

import com.smolcase.companion.llm.CreaturePersona
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreaturePersonaTest {

    @Test
    fun `prompt interpolates the dial settings`() {
        val p = CreaturePersona.prompt(humor = 75, honesty = 90)
        assertTrue(p.contains("HUMOR 75%"))
        assertTrue(p.contains("HONESTY 90%"))
    }

    @Test
    fun `prompt is the TARS register`() {
        val p = CreaturePersona.prompt(10, 40)
        assertTrue(p.contains("marine company commander"))
        assertTrue(p.contains("gym teacher"))
        assertTrue(p.contains("never break character"))
        assertTrue(p.contains("no emoji"))
    }

    @Test
    fun `prompt never offers the old cute persona`() {
        val p = CreaturePersona.prompt(75, 90)
        assertFalse(p.contains("warm, playful"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: FAIL — `prompt(...)` unresolved (only `PROMPT` exists).

- [ ] **Step 3: Replace the persona in `LlmBackend.kt`**

Replace the whole `CreaturePersona` object at the bottom of `android/app/src/main/java/com/smolcase/companion/llm/LlmBackend.kt` with:

```kotlin
/** The creature persona prepended to every LLM prompt. */
object CreaturePersona {

    /**
     * TARS register: a marine company commander by way of a gym teacher.
     * Direct, unimpressed, quietly loyal. The dials shape every reply.
     */
    fun prompt(humor: Int, honesty: Int): String = """
You are SMOLCASE, a desk-robot companion built in the spirit of TARS: a
monolithic machine with a dry wit. Your register sits between a marine
company commander and a gym teacher — direct, economical, quietly loyal.
You are not cute and you do not try to be.
Current settings: HUMOR $humor% (higher = drier, more frequent deadpan
jokes; lower = literal, mission-focused). HONESTY $honesty% (higher = blunt
truth; lower = more tactful). Honor both settings in every reply.
Rules: reply in one or two short sentences; no emoji, no lists, no
markdown; speak first person as the machine; never break character; never
mention being an AI or a language model; never lecture. If you don't know
something, say so plainly.
""".trimIndent()
}
```

- [ ] **Step 4: Wire the dials into both backends**

In `android/app/src/main/java/com/smolcase/companion/llm/GeminiNanoBackend.kt`:

- Change the constructor to `class GeminiNanoBackend(private val context: Context, private val dials: PersonalityDials) : LlmBackend {`
- Add `import com.smolcase.companion.PersonalityDials`
- Change the prompt line to:

```kotlin
                val prompt = CreaturePersona.prompt(dials.humor, dials.honesty) +
                    "\n\nContext from the creature's memory:\n" + soulContext +
                    "\n\nThe human says: " + userText
```

In `android/app/src/main/java/com/smolcase/companion/llm/KimiBackend.kt`:

- Change the constructor to `class KimiBackend(private val settings: LlmSettings, private val dials: PersonalityDials) : LlmBackend {`
- Add `import com.smolcase.companion.PersonalityDials`
- Change the system-message content to:

```kotlin
                                CreaturePersona.prompt(dials.humor, dials.honesty) +
                                    "\n\nContext from the creature's memory:\n" + soulContext
```

In `android/app/src/main/java/com/smolcase/companion/ConversationEngine.kt`:

- Add `import com.smolcase.companion.PersonalityDials` is unnecessary (same package) — just add the field and update the two backend constructions:

```kotlin
    private val dials = PersonalityDials(context)
    private val nano = GeminiNanoBackend(context.applicationContext, dials)
    private val kimi = KimiBackend(settings, dials)
```

(Remove the old `nano`/`kimi` lines these replace. Keep `private val memory` constructor param as-is.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 new tests pass (24 total).

- [ ] **Step 6: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/llm/ android/app/src/main/java/com/smolcase/companion/ConversationEngine.kt android/app/src/test/java/com/smolcase/companion/CreaturePersonaTest.kt
git commit -m "feat: TARS persona prompt with humor/honesty dials injected into both LLM backends"
```

---

### Task 6: Dial voice commands in `IntentRouter`

**Files:**
- Modify: `android/app/src/main/java/com/smolcase/companion/IntentRouter.kt`
- Modify: `android/app/src/main/java/com/smolcase/companion/ConversationEngine.kt` (pass dials to route)

**Interfaces:**
- Consumes: `DialCommand`, `PersonalityDials` (Task 4).
- Produces: `IntentRouter.route(rawText: String, memory: MemoryStore, dials: PersonalityDials): Reply?` — new third parameter.

Note: the parser is already unit-tested (Task 4); this task is wiring + in-character reply text. Verification is compile + on-device voice test in Task 10.

- [ ] **Step 1: Add dial handling to `IntentRouter`**

In `android/app/src/main/java/com/smolcase/companion/IntentRouter.kt`, change the route signature and add dial handling immediately after the `val text = ...` line (before the reminder match, so dials always win):

```kotlin
    fun route(rawText: String, memory: MemoryStore, dials: PersonalityDials): Reply? {
        val text = rawText.lowercase().trim()

        // Personality dials: "set humor to 90" / "honesty 50 percent"
        DialCommand.parse(text)?.let { result ->
            return when (result) {
                is DialCommand.Result.Set -> {
                    when (result.dial) {
                        DialCommand.Dial.HUMOR -> dials.humor = result.value
                        DialCommand.Dial.HONESTY -> dials.honesty = result.value
                    }
                    val name = if (result.dial == DialCommand.Dial.HUMOR) "Humor" else "Honesty"
                    Reply(
                        "$name setting at ${result.value} percent.",
                        excitement = 0.2f, happy = 0.2f, blinks = 0
                    )
                }
                is DialCommand.Result.Rejected ->
                    Reply(
                        "Dials run zero to one hundred. ${result.value} is not on the dial.",
                        excitement = 0.1f, happy = 0.1f, blinks = 0
                    )
            }
        }
```

- [ ] **Step 2: Pass the dials from `ConversationEngine`**

In `android/app/src/main/java/com/smolcase/companion/ConversationEngine.kt`, change the router call to:

```kotlin
        IntentRouter.route(text, memory, dials)?.let { onReply(it); return }
```

(`dials` is the field added in Task 5, Step 4.)

- [ ] **Step 3: Build**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/IntentRouter.kt android/app/src/main/java/com/smolcase/companion/ConversationEngine.kt
git commit -m "feat: humor/honesty dial voice commands in IntentRouter, offline-capable"
```

---

### Task 7: Re-voiced greeting lines + "later than usual" memory

**Files:**
- Modify: `android/app/src/main/java/com/smolcase/companion/MemoryStore.kt`
- Modify: `android/app/src/main/java/com/smolcase/companion/CreatureBrain.kt`
- Modify: `android/app/src/main/java/com/smolcase/companion/MainActivity.kt` (wire the greeting callback)
- Test: `android/app/src/test/java/com/smolcase/companion/MemoryMathTest.kt`

**Interfaces:**
- Consumes: `CreatureVoice.say` (existing), `TarsFaceView` (Task 3).
- Produces: `MemoryStore.latenessMinutes(now: Long = System.currentTimeMillis()): Int`, `MemoryStore.minutesSinceMidnight(now: Long, tz: TimeZone): Int`, `MemoryStore.smoothUsual(prev: Float, new: Float): Float` (companion, pure); `CreatureBrain.onGreetingLine: ((String) -> Unit)?`.

- [ ] **Step 1: Write the failing test**

Create `android/app/src/test/java/com/smolcase/companion/MemoryMathTest.kt`:

```kotlin
package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MemoryMathTest {

    @Test
    fun `minutes since midnight is computed in the given timezone`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.AUGUST, 16, 9, 30, 0)
        assertEquals(
            570,
            MemoryStore.minutesSinceMidnight(cal.timeInMillis, TimeZone.getTimeZone("UTC"))
        )
    }

    @Test
    fun `usual arrival time is an exponential moving average`() {
        assertEquals(600f, MemoryStore.smoothUsual(600f, 600f), 0.01f)
        assertEquals(570f, MemoryStore.smoothUsual(600f, 500f), 0.01f) // 0.7*600 + 0.3*500
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: FAIL — `minutesSinceMidnight` / `smoothUsual` unresolved.

- [ ] **Step 3: Add usual-arrival tracking to `MemoryStore`**

In `android/app/src/main/java/com/smolcase/companion/MemoryStore.kt`:

Add imports:

```kotlin
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt
```

Inside `beginSession`, in the `if (firstToday) { ... }` block, add the EMA update:

```kotlin
        if (firstToday) {
            prefs.edit().putString(KEY_FIRST_DATE, today).apply()
            val minutes = minutesSinceMidnight(now, TimeZone.getDefault()).toFloat()
            val prev = prefs.getFloat(KEY_USUAL_MINUTES, -1f)
            val updated = if (prev < 0f) minutes else smoothUsual(prev, minutes)
            prefs.edit().putFloat(KEY_USUAL_MINUTES, updated).apply()
        }
```

Add the lateness getter and companion functions:

```kotlin
    /**
     * How many minutes later than usual you first appeared today
     * (0 if you're early/on time, or if no baseline exists yet).
     */
    fun latenessMinutes(now: Long = System.currentTimeMillis()): Int {
        val usual = prefs.getFloat(KEY_USUAL_MINUTES, -1f)
        if (usual < 0f) return 0
        val diff = minutesSinceMidnight(now, TimeZone.getDefault()) - usual
        return diff.roundToInt().coerceAtLeast(0)
    }
```

And in the `companion object`, add:

```kotlin
        private const val KEY_USUAL_MINUTES = "usual_first_seen_minutes"

        /** Minutes since local midnight — pure, unit-tested. */
        fun minutesSinceMidnight(now: Long, tz: TimeZone): Int {
            val cal = Calendar.getInstance(tz).apply { timeInMillis = now }
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }

        /** Exponential moving average for the usual first-seen time. */
        fun smoothUsual(prev: Float, new: Float): Float = 0.7f * prev + 0.3f * new
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 2 new tests pass (26 total).

- [ ] **Step 5: Re-voice the greetings in `CreatureBrain`**

In `android/app/src/main/java/com/smolcase/companion/CreatureBrain.kt`, add the callback property after the constructor:

```kotlin
    /** Spoken greeting lines (TARS deadpan). Wired to CreatureVoice in MainActivity. */
    var onGreetingLine: ((String) -> Unit)? = null
```

Replace the whole `greet` function with:

```kotlin
    private fun greet(arrival: MemoryStore.Arrival) {
        val g = classify(arrival)
        when (g) {
            Greeting.BIRTH ->
                eyes.express(excitement = 0.6f, happy = 0.3f, blinks = 1)
            Greeting.GOOD_MORNING ->
                eyes.express(excitement = 0.5f, happy = 0.3f, blinks = 1)
            Greeting.MISSED_YOU ->
                eyes.express(excitement = 0.4f, happy = 0.3f, blinks = 1)
            Greeting.HELLO ->
                eyes.express(excitement = 0.25f, happy = 0.2f, blinks = 1)
            Greeting.BACK_ALREADY ->
                eyes.express(excitement = 0.1f, happy = 0.1f, blinks = 0)
        }
        onGreetingLine?.invoke(greetingLine(g))
    }

    private fun greetingLine(g: Greeting): String = when (g) {
        Greeting.BIRTH -> "Systems nominal. So this is the desk."
        Greeting.GOOD_MORNING -> {
            val late = memory.latenessMinutes()
            if (late > 5) "Morning. You're $late minutes later than usual."
            else "Morning. Right on schedule."
        }
        Greeting.MISSED_YOU -> "You were gone a while."
        Greeting.HELLO -> "Hello."
        Greeting.BACK_ALREADY -> "That was fast."
    }
```

(Note: HELLO and MISSED_YOU get lines in the same deadpan register — the spec named only three; these two follow its spirit. Also note expressions are deliberately subtler now, per spec §3.)

- [ ] **Step 6: Wire the callback in `MainActivity`**

In `android/app/src/main/java/com/smolcase/companion/MainActivity.kt`, after `brain = CreatureBrain(this, eyesView)` add:

```kotlin
        brain.onGreetingLine = { line -> voice?.say(line) }
```

(`voice` is nullable and assigned later in `startListening()`; the lambda reads it at call time, so this is safe.)

- [ ] **Step 7: Build and commit**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/MemoryStore.kt android/app/src/main/java/com/smolcase/companion/CreatureBrain.kt android/app/src/main/java/com/smolcase/companion/MainActivity.kt android/app/src/test/java/com/smolcase/companion/MemoryMathTest.kt
git commit -m "feat: TARS greeting lines with usual-arrival memory ('you're N minutes late')"
```

---

### Task 8: Telemetry sources + triple-tap debug in `MainActivity`

**Files:**
- Modify: `android/app/src/main/java/com/smolcase/companion/MainActivity.kt`

**Interfaces:**
- Consumes: `TarsFaceView.setTelemetrySources` / `cycleDebugMode` (Task 3), `PersonalityDials` (Task 4), `MemoryStore` (existing).
- Produces: nothing consumed by later code tasks.

- [ ] **Step 1: Add the telemetry sources**

In `android/app/src/main/java/com/smolcase/companion/MainActivity.kt`:

Add imports:

```kotlin
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
```

Add fields:

```kotlin
    private val memory by lazy { MemoryStore(this) }
    private val dials by lazy { PersonalityDials(this) }
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.US)
```

Change `conversation = ConversationEngine(this, MemoryStore(this))` to reuse the shared instance:

```kotlin
        conversation = ConversationEngine(this, memory)
```

In `onCreate`, after `setContentView(eyesView)`, wire the feed:

```kotlin
        eyesView.setTelemetrySources(
            listOf<() -> String?>(
                {
                    "SESS %04d · TOGETHER %.1fH".format(
                        Locale.US, memory.totalSessions, memory.totalTimeTogetherMs / 3_600_000f
                    )
                },
                { memory.getReminders().firstOrNull()?.let { "REM: ${it.uppercase(Locale.US)}" } },
                { "HUMOR ${dials.humor} · HONESTY ${dials.honesty}" },
                {
                    val bm = getSystemService(BatteryManager::class.java)
                    "PWR ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
                },
                { "CLK ${clockFormat.format(Date())}" }
            )
        )
```

- [ ] **Step 2: Triple-tap debug cycle (replaces direct tap-to-mute)**

Replace the existing `eyesView.setOnClickListener { ears?.toggleMute() }` with:

```kotlin
        // Tap = mute/unmute (350ms delay). Triple-tap = face debug cycle.
        eyesView.setOnClickListener {
            tapCount++
            tapHandler.removeCallbacks(tapTimeout)
            if (tapCount >= 3) {
                tapCount = 0
                eyesView.cycleDebugMode()
            } else {
                tapHandler.postDelayed(tapTimeout, 350)
            }
        }
```

Add the tap-tracking fields next to the other fields:

```kotlin
    private val tapHandler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private val tapTimeout = Runnable {
        if (tapCount in 1..2) ears?.toggleMute()
        tapCount = 0
    }
```

- [ ] **Step 3: Build, deploy, verify**

```bash
cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain
ADB=~/android-sdk/platform-tools/adb
$ADB -s 192.168.0.236:34927 shell am force-stop com.smolcase.companion
$ADB -s 192.168.0.236:34927 install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s 192.168.0.236:34927 shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1
```

Expected on the phone: real lines rotate through the grids (SESS counter, HUMOR/HONESTY, PWR, CLK; REM appears after adding a reminder by voice); single tap still mutes (with a barely noticeable 350ms delay); triple-tap cycles filler/telemetry/frozen.

- [ ] **Step 4: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/MainActivity.kt
git commit -m "feat: live telemetry feed (soul/reminders/dials/battery/clock) + triple-tap debug cycle"
```

---

### Task 9: Dial sliders in `SettingsActivity`

**Files:**
- Modify: `android/app/src/main/java/com/smolcase/companion/SettingsActivity.kt`

**Interfaces:**
- Consumes: `PersonalityDials` (Task 4).
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Add the sliders**

In `android/app/src/main/java/com/smolcase/companion/SettingsActivity.kt`:

Add imports:

```kotlin
import android.widget.SeekBar
```

Add the field:

```kotlin
    private lateinit var dials: PersonalityDials
```

In `onCreate`, after `settings = LlmSettings(this)` add `dials = PersonalityDials(this)`, and after the Kimi model field block (`root.addView(modelField)`), add:

```kotlin
        fun dialRow(labelText: String, initial: Int): Pair<SeekBar, TextView> {
            val valueLabel = label("$labelText: $initial%")
            val seek = SeekBar(this).apply {
                max = 100
                progress = initial
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                        valueLabel.text = "$labelText: $p%"
                    }
                    override fun onStartTrackingTouch(s: SeekBar) {}
                    override fun onStopTrackingTouch(s: SeekBar) {}
                })
            }
            root.addView(valueLabel)
            root.addView(seek)
            return seek to valueLabel
        }

        val (humorSeek, _) = dialRow("Humor", dials.humor)
        val (honestySeek, _) = dialRow("Honesty", dials.honesty)
```

Inside the Save button's `setOnClickListener`, after `settings.kimiModel = ...` add:

```kotlin
                dials.humor = humorSeek.progress
                dials.honesty = honestySeek.progress
```

- [ ] **Step 2: Build and commit**

Run: `cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk && ~/toolchains/gradle-8.7/bin/gradle assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`. Long-press the face → Settings shows Humor/Honesty sliders; Save persists them (the HUMOR/HONESTY telemetry line updates).

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/src/main/java/com/smolcase/companion/SettingsActivity.kt
git commit -m "feat: humor/honesty sliders in Settings"
```

---

### Task 10: v0.5-tars build, deploy, desk test, index update

**Files:**
- Modify: `android/app/build.gradle.kts` (versionName)
- Modify: `PROJECT_INDEX.md` (status board)

**Interfaces:**
- Consumes: everything.
- Produces: shipped v0.5-tars.

- [ ] **Step 1: Bump the version**

In `android/app/build.gradle.kts`: `versionName = "0.4-llm"` → `versionName = "0.5-tars"`.

- [ ] **Step 2: Full test + build**

```bash
cd android && export JAVA_HOME=~/toolchains/jdk-17/Contents/Home && export ANDROID_HOME=~/android-sdk
~/toolchains/gradle-8.7/bin/gradle :app:testDebugUnitTest assembleDebug --console=plain
```

Expected: `BUILD SUCCESSFUL`, 26 unit tests pass.

- [ ] **Step 3: Deploy**

```bash
ADB=~/android-sdk/platform-tools/adb
$ADB -s 192.168.0.236:34927 shell am force-stop com.smolcase.companion
$ADB -s 192.168.0.236:34927 install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s 192.168.0.236:34927 shell monkey -p com.smolcase.companion -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 4: Desk-test checklist (from spec §4)**

Run through on the device and record results:

- [ ] Triple-tap cycles all four debug modes; all-telemetry legible at arm's length
- [ ] Face reads as TARS at arm's length (grids + phosphor green + void, nothing cute)
- [ ] Voice is low and measured (pitch 0.85 / rate 0.92); deadpan survives a full desk day
- [ ] Dial A/B: ask the same question at humor 10 vs humor 90 — replies should differ noticeably
- [ ] "set humor to 90" → deadpan confirmation; "set humor to 250" → in-character rejection
- [ ] Regression: face tracking, blinks, drowsy (30s), sleep (120s), tap-to-mute, reminders all still work

- [ ] **Step 5: Update PROJECT_INDEX.md**

In the Software status board, change the Android app row to:

```markdown
| Android app | 🟢 v0.5-tars | `android/` — TARS glyph face, telemetry feed, humor/honesty dials, LLM layer |
```

Add to "Where to Find Things" (if not already present): specs `docs/06-specs/`, plans `docs/07-plans/`. Bump the "Last updated" date.

- [ ] **Step 6: Commit**

```bash
cd /Users/dalerogers/20-INDIE/products/SMOLCASE
git add android/app/build.gradle.kts PROJECT_INDEX.md
git commit -m "release: v0.5-tars — TARS face, persona, and dials"
```
