package com.smolcase.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.smolcase.companion.matrix.ApplianceMatrixCanvas
import com.smolcase.companion.matrix.CozmoEyeInterpolator
import com.smolcase.companion.matrix.CozmoEyeRasterizer
import com.smolcase.companion.matrix.EyeExpressionState
import com.smolcase.companion.matrix.Mood
import kotlin.math.min
import kotlin.random.Random

/**
 * SMOLCASE Expressive Appliance Face:
 * Full-screen LED/VFD dot matrix display with Cozmo-style kawaii expressive eyes.
 *
 * Key features:
 * - Full-screen dot matrix grid with authentic ghost dot background and ambient breathing.
 * - Upper half strictly bounds the eye apertures (Y <= 0.50), leaving space for 2D gaze roaming.
 * - Solid luminous eyes without inner pupils or specular glints.
 * - Honesty and humour dials drive emotive variations (lid asymmetry, deadpan vs bounce).
 * - Momentary reaction overlays (hearts for care/fondness, radar scan for thinking).
 * - Minimalist hardware appliance telemetry pips at bottom edge.
 */
class TarsFaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class State { AWAKE, DROWSY, SLEEPING }

    // Timing constants (ms)
    private val drowsyAfterMs = 30_000L
    private val sleepAfterMs = 120_000L
    private val blinkDurationMs = 140L
    private val farewellBlinkDurationMs = 600L

    // Matrix resolution
    private val matrix = ApplianceMatrixCanvas(cols = 38, rows = 68, ghostAlpha = 0.05f)
    private val rasterizer = CozmoEyeRasterizer(matrix)
    private val expressionState = EyeExpressionState()
    private val interpolator = CozmoEyeInterpolator(speed = 0.22f)

    // Creature tracking state
    @Volatile private var faceTarget: PointF? = null
    @Volatile private var lastFaceAt: Long = SystemClock.uptimeMillis()

    private val gaze = PointF(0f, 0f)
    private val saccade = PointF(0f, 0f)
    private var nextSaccadeAt = 0L
    private var blinkStartedAt = 0L
    private var blinkDuration = blinkDurationMs
    private var nextBlinkAt = 0L
    private var pendingBlinks = 0

    // Voice & sensory state
    @Volatile private var speaking = false
    @Volatile private var micMuted = false
    @Volatile private var batteryPercent: Int = 100
    @Volatile private var bleConnected: Boolean = false

    // Color palette (Monochrome Warm Appliance Phosphor)
    private val bgPaint = Paint().apply { color = Color.parseColor("#0A0D10") }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAEEEF")
    }
    private val mutePipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E05555")
    }
    private val blePipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
    }

    // Frame loop
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

    // Public API

    /** Face observed at normalized position (-1..1). */
    fun onFaceSeen(nx: Float, ny: Float) {
        faceTarget = PointF(nx.coerceIn(-1f, 1f), ny.coerceIn(-1f, 1f))
        lastFaceAt = SystemClock.uptimeMillis()
    }

    /** Expression trigger from brain / conversation. */
    fun express(excitement: Float, happy: Float, blinks: Int) {
        if (happy > 0.4f) {
            expressionState.currentMood = Mood.HAPPY
        } else if (excitement > 0.5f) {
            expressionState.currentMood = Mood.ALERT
        } else {
            expressionState.currentMood = Mood.NEUTRAL
        }
        pendingBlinks += blinks
    }

    /** Set personality dials. */
    fun setDials(humor: Int, honesty: Int) {
        expressionState.humor = humor
        expressionState.honesty = honesty
    }

    /** Trigger momentary reaction icon (e.g. HEART or THINKING). */
    fun triggerReaction(reaction: Mood, durationMs: Long = 1800L) {
        expressionState.triggerReaction(reaction, durationMs, SystemClock.uptimeMillis())
    }

    /** You left: slow farewell blink. */
    fun farewell() {
        if (blinkStartedAt == 0L) {
            blinkDuration = farewellBlinkDurationMs
            blinkStartedAt = SystemClock.uptimeMillis()
        }
        expressionState.currentMood = Mood.NEUTRAL
    }

    /** Creature speaking indicator / animation. */
    fun setSpeaking(value: Boolean) {
        speaking = value
        if (speaking && expressionState.currentMood == Mood.NEUTRAL) {
            // Subtle expressive attentive tilt when speaking
            expressionState.currentMood = if (expressionState.humor > 70) Mood.SKEPTICAL else Mood.CURIOUS
        } else if (!speaking && (expressionState.currentMood == Mood.SKEPTICAL || expressionState.currentMood == Mood.CURIOUS)) {
            expressionState.currentMood = Mood.NEUTRAL
        }
    }

    /** Mic muted indicator. */
    fun setMicMuted(value: Boolean) {
        micMuted = value
    }

    /** Update hardware telemetry for bottom appliance pips. */
    fun setTelemetry(battery: Int, ble: Boolean) {
        batteryPercent = battery
        bleConnected = ble
    }

    // Deprecated compatibility methods for existing callers
    fun setTelemetrySources(sources: List<() -> String?>) {
        // Text telemetry stream deprecated in favor of appliance matrix & bottom pips
    }

    fun cycleDebugMode() {
        // Toggle sample reaction for testing
        val moods = arrayOf(Mood.NEUTRAL, Mood.HAPPY, Mood.SKEPTICAL, Mood.CURIOUS, Mood.HEART, Mood.THINKING)
        val next = moods[(expressionState.currentMood.ordinal + 1) % moods.size]
        triggerReaction(next, 3000L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // Screen touch steers gaze smoothly toward touch position
            val nx = ((event.x / width.toFloat()) - 0.5f) * 2f
            val ny = ((event.y / (height.toFloat() * 0.5f)) - 0.5f) * 2f
            onFaceSeen(nx, ny)
        }
        return super.onTouchEvent(event)
    }

    private fun state(now: Long): State {
        val since = now - lastFaceAt
        return when {
            since < drowsyAfterMs -> State.AWAKE
            since < sleepAfterMs -> State.DROWSY
            else -> State.SLEEPING
        }
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val st = state(now)

        expressionState.updateClock(now)

        // Set base mood based on sleep state if not currently overridden by reaction
        if (expressionState.currentMood != Mood.HEART && expressionState.currentMood != Mood.THINKING) {
            when (st) {
                State.SLEEPING -> expressionState.currentMood = Mood.SLEEPING
                State.DROWSY -> expressionState.currentMood = Mood.DROWSY
                State.AWAKE -> {
                    if (expressionState.currentMood == Mood.SLEEPING || expressionState.currentMood == Mood.DROWSY) {
                        expressionState.currentMood = Mood.NEUTRAL
                    }
                }
            }
        }

        // Blinking
        if (pendingBlinks > 0 && blinkStartedAt == 0L) {
            pendingBlinks--
            blinkDuration = blinkDurationMs
            blinkStartedAt = now
        }

        if (st != State.SLEEPING && blinkStartedAt == 0L && pendingBlinks == 0 && now >= nextBlinkAt) {
            blinkDuration = blinkDurationMs
            blinkStartedAt = now
        }

        // Micro-saccades
        if (st == State.AWAKE && now >= nextSaccadeAt) {
            saccade.set(Random.nextFloat() * 0.12f - 0.06f, Random.nextFloat() * 0.08f - 0.04f)
            nextSaccadeAt = now + Random.nextLong(1_500, 4_000)
        }

        // Gaze tracking interpolation
        val t = faceTarget
        val targetGazeX = (t?.x ?: 0f) + saccade.x
        val targetGazeY = (t?.y ?: 0f) + saccade.y
        gaze.x += (targetGazeX - gaze.x) * 0.18f
        gaze.y += (targetGazeY - gaze.y) * 0.18f

        // Blink progress
        var blinkProgress = 0f
        if (blinkStartedAt != 0L) {
            val p = (now - blinkStartedAt).toFloat() / blinkDuration
            if (p >= 1f) {
                blinkStartedAt = 0L
                nextBlinkAt = now + Random.nextLong(3_000, 7_000)
            } else {
                blinkProgress = if (p < 0.5f) p * 2f else (1f - p) * 2f
            }
        }

        // Compute targets & update interpolator
        val (targetL, targetR) = expressionState.computeTargets(gaze.x, gaze.y, blinkProgress, now)
        interpolator.update(targetL, targetR)

        // Rasterize frame to matrix buffer
        matrix.clear()
        if (expressionState.currentMood == Mood.HEART) {
            rasterizer.rasterizeHeart(0.30f + gaze.x * 0.04f, 0.23f + gaze.y * 0.03f, size = 0.12f)
            rasterizer.rasterizeHeart(0.70f + gaze.x * 0.04f, 0.23f + gaze.y * 0.03f, size = 0.12f)
        } else if (expressionState.currentMood == Mood.THINKING) {
            val phase = (now % 1200L) / 1200f
            rasterizer.rasterizeThinkingScan(0.30f, 0.23f, radius = 0.12f, phase = phase)
            rasterizer.rasterizeThinkingScan(0.70f, 0.23f, radius = 0.12f, phase = phase)
        } else {
            rasterizer.rasterizeEyes(interpolator.leftEye, interpolator.rightEye)
        }

        // Rasterize subtle bottom appliance telemetry pips
        rasterizeTelemetryPips()
    }

    private fun rasterizeTelemetryPips() {
        val lastRow = matrix.rows - 2
        
        // Battery pips (4 dots on bottom-left, cols 2..5)
        val pipsLit = (batteryPercent / 25).coerceIn(1, 4)
        for (i in 0 until 4) {
            val col = 2 + i
            val intensity = if (i < pipsLit) 0.65f else 0.08f
            matrix.setDot(col, lastRow, intensity)
        }

        // BLE pip (bottom-right, col = cols - 3)
        if (bleConnected) {
            matrix.setDot(matrix.cols - 3, lastRow, 0.85f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val now = SystemClock.uptimeMillis()
        val isSleeping = state(now) == State.SLEEPING

        // Black background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val cols = matrix.cols
        val rows = matrix.rows
        val cellW = w / cols
        val cellH = h / rows
        val dotRadius = min(cellW, cellH) * 0.38f

        // Draw physical appliance dot matrix
        for (r in 0 until rows) {
            val cy = (r + 0.5f) * cellH
            for (c in 0 until cols) {
                val cx = (c + 0.5f) * cellW
                val litIntensity = matrix.getDot(c, r)

                if (litIntensity > 0.02f) {
                    dotPaint.alpha = (litIntensity * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(cx, cy, dotRadius, dotPaint)
                } else {
                    // Ambient breathing unlit ghost dot
                    val ambient = matrix.ambientBreathing(c, r, now, isSleeping)
                    dotPaint.alpha = (ambient * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(cx, cy, dotRadius * 0.85f, dotPaint)
                }
            }
        }

        // Mic mute pip (middle bottom)
        if (micMuted) {
            val muteRadius = min(w, h) * 0.014f
            canvas.drawCircle(w * 0.5f, h * 0.96f, muteRadius, mutePipPaint)
        }
    }
}
