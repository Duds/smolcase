package com.smolcase.companion.face

import com.smolcase.companion.matrix.ExpressionEyeParams
import com.smolcase.companion.matrix.ExpressionFaceModel
import com.smolcase.companion.matrix.ExpressionPresets
import com.smolcase.companion.matrix.Mood
import kotlin.math.exp
import kotlin.random.Random

/** Snapshot owned by the render thread after publication; never mutate its eyes. */
data class FaceFrame(
    val parameters: FaceParameters,
    val eyes: ExpressionEyeParams,
    val yawRadians: Float,
    val pitchRadians: Float,
    val blink: Float,
    val saccadeX: Float = 0f,
    val saccadeY: Float = 0f
) {
    companion object {
        fun fixed(parameters: FaceParameters, gazeX: Float = 0f, gazeY: Float = 0f, blink: Float = 0f) =
            FaceFrame(parameters, resolveEyes(parameters, parameters.mood, parameters.humor, parameters.honesty),
                Math.toRadians((gazeX.coerceIn(-1f, 1f) * parameters.sphere.yawDegrees).toDouble()).toFloat(),
                -Math.toRadians((gazeY.coerceIn(-1f, 1f) * parameters.sphere.pitchDegrees).toDouble()).toFloat(),
                blink.coerceIn(0f, 1f))
    }
}

/** Gaze at this boundary is positive right/up, as in the browser spike. */
data class FaceInput(val mood: Mood, val humor: Float, val honesty: Float,
    val gazeX: Float = 0f, val gazeY: Float = 0f)

private fun resolveEyes(parameters: FaceParameters, mood: Mood, humor: Float, honesty: Float) =
    ExpressionFaceModel.resolve(parameters.eyes, ExpressionPresets.ALL.getValue(mood.name),
        parameters.intensity, humor.coerceIn(0f, 100f), honesty.coerceIn(0f, 100f))

/** Animation equations and timing from the spike's animate(), independent of refresh rate. */
class FaceAnimator(private val parameters: FaceParameters, private val random: Random = Random.Default) {
    private val current = parameters.eyes.copy()
    private var gazeX = 0f
    private var gazeY = 0f
    private var blink = 0f
    private var nextBlink = 2.5f
    private var nextSaccade = 1.5f
    private var saccadeX = 0f
    private var saccadeY = 0f
    private var pendingBlinks = 0

    fun requestBlinks(count: Int) { pendingBlinks += count.coerceIn(0, 5) }

    fun advance(deltaSeconds: Float, input: FaceInput): FaceFrame {
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        val target = resolveEyes(parameters, input.mood, input.humor, input.honesty)
        val blend = 1f - exp(-parameters.blendSpeed * dt)
        for (field in FaceParameters.EYE_FIELDS) {
            val value = field.get(current)
            field.set(current, value + (field.get(target) - value) * blend)
        }
        val gazeBlend = 1f - exp(-4f * dt)
        gazeX += (input.gazeX.coerceIn(-1f, 1f) - gazeX) * gazeBlend
        gazeY += (input.gazeY.coerceIn(-1f, 1f) - gazeY) * gazeBlend
        updateBlink(dt)
        updateSaccade(dt)
        return FaceFrame(parameters, current.copy(),
            Math.toRadians((gazeX * parameters.sphere.yawDegrees).toDouble()).toFloat(),
            -Math.toRadians((gazeY * parameters.sphere.pitchDegrees).toDouble()).toFloat(),
            blink, saccadeX, saccadeY)
    }

    private fun updateBlink(dt: Float) {
        if (!parameters.blinking) { blink = 0f; return }
        nextBlink -= dt
        if (nextBlink <= 0f || (blink == 0f && pendingBlinks > 0)) {
            if (pendingBlinks > 0) pendingBlinks--
            blink = 1f
            nextBlink = 2.5f + random.nextFloat() * 3f
        }
        blink = (blink - dt * 5f).coerceAtLeast(0f)
    }

    private fun updateSaccade(dt: Float) {
        if (!parameters.saccade) { saccadeX = 0f; saccadeY = 0f; return }
        nextSaccade -= dt
        if (nextSaccade <= 0f) {
            saccadeX = (random.nextFloat() - 0.5f) * 0.012f
            saccadeY = (random.nextFloat() - 0.5f) * 0.008f
            nextSaccade = 1f + random.nextFloat() * 2f
        }
    }
}
