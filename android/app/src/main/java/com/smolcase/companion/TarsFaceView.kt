package com.smolcase.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.smolcase.companion.face.FaceAnimator
import com.smolcase.companion.face.FaceFrame
import com.smolcase.companion.face.FaceGpuRenderer
import com.smolcase.companion.face.FaceInput
import com.smolcase.companion.face.FaceParameters
import com.smolcase.companion.matrix.EyeExpressionState
import com.smolcase.companion.matrix.Mood
import kotlin.math.min

/** App interaction facade. Face pixels come exclusively from the spike's two GPU passes. */
class TarsFaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    enum class State { AWAKE, DROWSY, SLEEPING }

    private val parameters = FaceParameters.fromJson(
        context.assets.open("face/parameters.json").bufferedReader().use { it.readText() }
    )
    val expressionState = EyeExpressionState().apply {
        currentMood = parameters.mood
        humor = parameters.humor.toInt()
        honesty = parameters.honesty.toInt()
    }
    private val animator = FaceAnimator(parameters)
    private val renderer = FaceGpuRenderer(context.assets, FaceFrame.fixed(parameters))
    private val surface = GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
    private val telemetry = TelemetryView(context)
    @Volatile private var faceTarget = PointF(0f, 0f)
    @Volatile private var lastFaceAt = SystemClock.uptimeMillis()
    private var previousFrameAt = 0L
    private var renderingActive = true

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) { performLongClick() }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean { performClick(); return true }
    })

    private val frameCallback = object : Runnable {
        override fun run() {
            if (!renderingActive) return
            tick()
            postOnAnimation(this)
        }
    }

    init {
        isLongClickable = true
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(telemetry, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        previousFrameAt = 0L
        if (renderingActive) {
            removeCallbacks(frameCallback)
            postOnAnimation(frameCallback)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frameCallback)
        super.onDetachedFromWindow()
    }

    fun resumeRendering() {
        renderingActive = true
        previousFrameAt = 0L
        surface.onResume()
        removeCallbacks(frameCallback)
        if (isAttachedToWindow) postOnAnimation(frameCallback)
    }

    fun pauseRendering() {
        renderingActive = false
        removeCallbacks(frameCallback)
        surface.onPause()
    }

    /** Camera/app callers supply positive-down Y. Convert once to the spike's positive-up gaze. */
    fun onFaceSeen(nx: Float, ny: Float) {
        faceTarget = PointF(nx.coerceIn(-1f, 1f), -ny.coerceIn(-1f, 1f))
        lastFaceAt = SystemClock.uptimeMillis()
    }

    fun express(excitement: Float, happy: Float, blinks: Int) {
        expressionState.currentMood = when {
            happy > 0.4f -> Mood.HAPPY
            excitement > 0.5f -> Mood.ALERT
            else -> Mood.NEUTRAL
        }
        animator.requestBlinks(blinks)
    }

    fun setDials(humor: Int, honesty: Int) {
        expressionState.humor = humor
        expressionState.honesty = honesty
    }

    fun triggerReaction(reaction: Mood, durationMs: Long = 1800L) {
        expressionState.triggerReaction(reaction, durationMs, SystemClock.uptimeMillis())
    }

    fun farewell() {
        animator.requestBlinks(1)
        expressionState.currentMood = Mood.NEUTRAL
    }

    fun setSpeaking(value: Boolean) {
        val mood = expressionState.currentMood
        if (value && mood == Mood.NEUTRAL) {
            expressionState.currentMood = if (expressionState.humor > 70) Mood.SKEPTICAL else Mood.CURIOUS
        } else if (!value && (mood == Mood.SKEPTICAL || mood == Mood.CURIOUS)) {
            expressionState.currentMood = Mood.NEUTRAL
        }
    }

    fun setMicMuted(value: Boolean) { telemetry.muted = value; telemetry.invalidate() }
    fun setTelemetry(battery: Int, ble: Boolean) {
        telemetry.battery = battery.coerceIn(0, 100)
        telemetry.ble = ble
        telemetry.invalidate()
    }

    @Suppress("UNUSED_PARAMETER")
    fun setTelemetrySources(sources: List<() -> String?>) { /* Legacy text telemetry API. */ }

    fun cycleDebugMode() {
        val moods = Mood.values()
        triggerReaction(moods[(expressionState.currentMood.ordinal + 1) % moods.size], 3000L)
    }

    override fun onInterceptTouchEvent(event: MotionEvent) = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if ((event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE)
            && width > 0 && height > 0) {
            onFaceSeen(event.x / width * 2f - 1f, event.y / height * 2f - 1f)
        }
        return true
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = if (previousFrameAt == 0L) 0f else (now - previousFrameAt) / 1000f
        previousFrameAt = now
        expressionState.updateClock(now)
        val mood = expressionState.currentMood
        if (mood != Mood.HEART && mood != Mood.THINKING) {
            val absentMs = now - lastFaceAt
            expressionState.currentMood = when {
                absentMs >= 120_000L -> Mood.SLEEPING
                absentMs >= 30_000L -> Mood.DROWSY
                mood == Mood.SLEEPING || mood == Mood.DROWSY -> Mood.NEUTRAL
                else -> mood
            }
        }
        val gaze = faceTarget
        renderer.frame = animator.advance(dt, FaceInput(expressionState.currentMood,
            expressionState.humor.toFloat(), expressionState.honesty.toFloat(), gaze.x, gaze.y))
        surface.requestRender()
    }

    /** Telemetry is not part of the reference face shader or parity captures. */
    private class TelemetryView(context: Context) : View(context) {
        var battery = 100
        var ble = false
        var muted = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            val cellW = width / 38f
            val cellH = height / 68f
            val radius = min(cellW, cellH) * 0.15f
            paint.color = Color.rgb(141, 232, 255)
            repeat(4) {
                paint.alpha = if (it < battery / 25) 166 else 20
                canvas.drawCircle((it + 2.5f) * cellW, 66.5f * cellH, radius, paint)
            }
            if (ble) {
                paint.color = Color.rgb(74, 144, 226)
                canvas.drawCircle(35.5f * cellW, 66.5f * cellH, radius, paint)
            }
            if (muted) {
                paint.color = Color.rgb(224, 85, 85)
                canvas.drawCircle(width * 0.5f, height * 0.96f, min(width, height) * 0.014f, paint)
            }
        }
    }
}
