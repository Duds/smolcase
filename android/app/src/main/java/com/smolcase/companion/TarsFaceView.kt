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

        drawEye(canvas, w * 0.08f, cy, cellW, cellH, feedL, pupilAnchor, squint, glow, scanCol)
        drawEye(canvas, w * 0.62f, cy, cellW, cellH, feedR, pupilAnchor, squint, glow, scanCol)

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
                if (isPupil) alpha = min(255, (320 * glow).toInt()) // bright awake, breathes when asleep
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
