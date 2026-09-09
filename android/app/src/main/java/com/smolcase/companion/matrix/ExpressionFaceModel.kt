package com.smolcase.companion.matrix

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Port of the face-expressions spike into the app's eye model.
 *
 * Coordinates are height-normalised: y spans 0..1 bottom to top, x spans
 * 0..aspect where aspect is width / height. Callers working in 0..1 on both
 * axes must convert before calling.
 *
 * Source of truth: spikes/20260909-face-expressions/index.html, functions
 * eyeMask(), heartDistance(), rotateVector() and the sphere branch of main().
 */
data class ExpressionEyeParams(
    var eyeW: Float = 0.065f,
    var eyeH: Float = 0.046f,
    var gap: Float = 0.146f,
    var centerX: Float = 0.5f,
    var centerY: Float = 0.68f,
    var scaleL: Float = 1f,
    var scaleR: Float = 1.02f,
    var offsetYL: Float = 0f,
    var offsetYR: Float = -0.006f,
    var topLid: Float = 0.03f,
    var botLid: Float = 0.02f,
    var lidCurve: Float = 0.12f,
    var lidSlant: Float = 0.35f,
    var slant: Float = 0.06f,
    var softness: Float = 0.001f,
    var brightness: Float = 0.2f,
    var topSlope: Float = 0f,
    var stroke: Float = 0.24f,
    var archWeight: Float = 0f,
    var chevronWeight: Float = 0f,
    var heartWeight: Float = 0f,
    var arcCurve: Float = -0.4f,
    var roll: Float = 0f,
    var blink: Float = 0f
)

/**
 * A preset delta applied over the neutral base. Size multipliers blend with
 * intensity; every other field lerps with intensity.
 */
data class ExpressionMood(
    val cue: String = "",
    val scaleR: Float? = null,
    val offsetYR: Float? = null,
    val topLid: Float? = null,
    val botLid: Float? = null,
    val lidCurve: Float? = null,
    val lidSlant: Float? = null,
    val slant: Float? = null,
    val topSlope: Float? = null,
    val stroke: Float? = null,
    val archWeight: Float? = null,
    val chevronWeight: Float? = null,
    val heartWeight: Float? = null,
    val arcCurve: Float? = null,
    val wScale: Float = 1f,
    val hScale: Float = 1f
)

object ExpressionPresets {
    val NEUTRAL = ExpressionMood(cue = "Original asymmetric, filled-eye baseline.")
    val HAPPY = ExpressionMood(cue = "Raised closed-eye arches.", archWeight = 1f, arcCurve = 0.65f, scaleR = 1f, slant = 0f)
    val EMBARRASSED = ExpressionMood(cue = "Smaller, gentler happy arches.", archWeight = 1f, arcCurve = 0.35f, scaleR = 0.85f, wScale = 0.85f)
    val LAUGHING = ExpressionMood(cue = "Squeezed chevrons.", chevronWeight = 1f, scaleR = 1f, slant = 0f, stroke = 0.28f)
    val HEART = ExpressionMood(cue = "Filled heart eyes.", heartWeight = 1f, scaleR = 1f, slant = 0f, wScale = 0.85f, hScale = 1.25f)
    val ANGRY = ExpressionMood(cue = "Top lids slope down towards the nose.", topLid = 0.6f, topSlope = 0.75f, botLid = 0f, lidCurve = 0f, lidSlant = 0f, scaleR = 1f, slant = 0f)
    val SHOCKED = ExpressionMood(cue = "Tall, wide-open solid eyes.", wScale = 0.78f, hScale = 1.45f, topLid = 0f, botLid = 0f, lidCurve = 0f, lidSlant = 0f, scaleR = 1f, slant = 0f)
    val SMUG = ExpressionMood(cue = "Low, nearly horizontal lids.", topLid = 0.85f, topSlope = 0.12f, lidCurve = 0f, lidSlant = 0f, scaleR = 0.9f, slant = 0f)
    val SAD = ExpressionMood(cue = "Inner lid ends lift, reversing the angry slope.", topLid = 0.55f, topSlope = -0.55f, botLid = 0.1f, lidCurve = 0f, lidSlant = 0f, scaleR = 1f, slant = 0f)
    val EXHAUSTED = ExpressionMood(cue = "Almost-flat, drooping strokes.", archWeight = 1f, arcCurve = -0.15f, hScale = 0.7f, scaleR = 0.8f)
    val SKEPTICAL = ExpressionMood(cue = "Unequal openings, lifted lower lid.", scaleR = 0.62f, offsetYR = -0.012f, botLid = 0.3f, lidSlant = 0.55f, slant = 0.14f)
    val CURIOUS = ExpressionMood(cue = "Open and asymmetric.", hScale = 1.18f, scaleR = 0.92f, topLid = 0f)
    val DROWSY = ExpressionMood(cue = "Heavy upper lids.", topLid = 0.7f, botLid = 0.1f, hScale = 0.9f)
    val SLEEPING = ExpressionMood(cue = "Closed resting curves.", archWeight = 1f, arcCurve = -0.45f, scaleR = 1f, slant = 0f)
    val ALERT = ExpressionMood(cue = "Wide open, baseline asymmetry preserved.", wScale = 1.08f, hScale = 1.15f, topLid = 0f, botLid = 0f)
    val THINKING = ExpressionMood(cue = "Unequal eyes with a slight lean.", scaleR = 0.7f, offsetYR = -0.016f, topLid = 0.22f, slant = -0.12f)

    val ALL = linkedMapOf(
        "NEUTRAL" to NEUTRAL, "HAPPY" to HAPPY, "EMBARRASSED" to EMBARRASSED,
        "LAUGHING" to LAUGHING, "HEART" to HEART, "ANGRY" to ANGRY,
        "SHOCKED" to SHOCKED, "SMUG" to SMUG, "SAD" to SAD,
        "EXHAUSTED" to EXHAUSTED, "SKEPTICAL" to SKEPTICAL, "CURIOUS" to CURIOUS,
        "DROWSY" to DROWSY, "SLEEPING" to SLEEPING, "ALERT" to ALERT,
        "THINKING" to THINKING
    )
}

object ExpressionFaceModel {

    /** Blends a preset over the base, then applies the personality dials. */
    fun resolve(
        base: ExpressionEyeParams,
        mood: ExpressionMood,
        intensity: Float,
        humor: Float,
        honesty: Float
    ): ExpressionEyeParams = base.copy().apply {
        eyeW = base.eyeW * lerp(1f, mood.wScale, intensity)
        eyeH = base.eyeH * lerp(1f, mood.hScale, intensity)
        if (mood.scaleR != null) scaleR = lerp(base.scaleR, mood.scaleR, intensity)
        if (mood.offsetYR != null) offsetYR = lerp(base.offsetYR, mood.offsetYR, intensity)
        if (mood.topLid != null) topLid = lerp(base.topLid, mood.topLid, intensity)
        if (mood.botLid != null) botLid = lerp(base.botLid, mood.botLid, intensity)
        if (mood.lidCurve != null) lidCurve = lerp(base.lidCurve, mood.lidCurve, intensity)
        if (mood.lidSlant != null) lidSlant = lerp(base.lidSlant, mood.lidSlant, intensity)
        if (mood.slant != null) slant = lerp(base.slant, mood.slant, intensity)
        if (mood.topSlope != null) topSlope = lerp(base.topSlope, mood.topSlope, intensity)
        if (mood.stroke != null) stroke = lerp(base.stroke, mood.stroke, intensity)
        if (mood.archWeight != null) archWeight = lerp(base.archWeight, mood.archWeight, intensity)
        if (mood.chevronWeight != null) chevronWeight = lerp(base.chevronWeight, mood.chevronWeight, intensity)
        if (mood.heartWeight != null) heartWeight = lerp(base.heartWeight, mood.heartWeight, intensity)
        if (mood.arcCurve != null) arcCurve = lerp(base.arcCurve, mood.arcCurve, intensity)

        val h = humor / 100f
        val o = honesty / 100f
        scaleR -= h * 0.12f
        slant += h * 0.06f
        lidSlant += h * 0.15f
        topLid += o * 0.12f
    }

    /**
     * Eye coverage at a point in height-normalised coords.
     * Returns normal-shape coverage in x and heart coverage in y, so hearts
     * can be coloured apart from the cyan eyes.
     */
    fun coverage(
        params: ExpressionEyeParams,
        x: Float,
        y: Float,
        aspect: Float
    ): Pair<Float, Float> {
        // Match the shader's source clip. This is applied before pair roll,
        // because the spike clips against screen-space y, not eye-local y.
        if (y < 0.5f) return Pair(0f, 0f)

        val rolled = roll(params, x, y, aspect)
        val rolledX = rolled.first
        val rolledY = rolled.second
        val centreX = params.centerX * aspect
        val centreY = params.centerY
        val hwL = max(params.eyeW * params.scaleL, 0.001f)
        val hhL = max(params.eyeH * params.scaleL, 0.001f)
        val hwR = max(params.eyeW * params.scaleR, 0.001f)
        val hhR = max(params.eyeH * params.scaleR, 0.001f)

        val left = eyeMask(params, rolledX, rolledY, centreX - params.gap / 2f, centreY + params.offsetYL, hwL, hhL, -1f)
        val right = eyeMask(params, rolledX, rolledY, centreX + params.gap / 2f, centreY + params.offsetYR, hwR, hhR, 1f)
        return Pair(maxOf(left.first, right.first), maxOf(left.second, right.second))
    }

    /**
     * Sphere-mapped coverage. Projects the point onto the sphere, then undoes
     * the gaze rotation so the pattern stays fixed to the surface.
     * Returns coverage pair plus the sphere's own lit intensity.
     */
    fun sphereCoverage(
        params: ExpressionEyeParams,
        x: Float,
        y: Float,
        aspect: Float,
        sphereRadius: Float,
        sphereY: Float,
        gazeYawRad: Float,
        gazePitchRad: Float,
        sphereGlow: Float
    ): Triple<Float, Float, Float> {
        val cx = aspect / 2f
        val dx = (x - cx) / max(sphereRadius, 1e-4f)
        val dy = (y - sphereY) / max(sphereRadius, 1e-4f)
        val r2 = dx * dx + dy * dy
        if (r2 > 1f) {
            val halo = exp(-(sqrt(r2) - 1f) * 5f) * sphereGlow
            return Triple(0f, 0f, halo)
        }
        val surface = floatArrayOf(dx, dy, sqrt(max(1f - r2, 0f)))
        val pattern = rotateVector(surface, -gazeYawRad, -gazePitchRad)
        var normal = 0f
        var heart = 0f
        if (pattern[2] > 0f) {
            val px = cx + pattern[0] * sphereRadius
            val py = sphereY + pattern[1] * sphereRadius
            if (py >= 0.5f) {
                val pair = coverage(params, px, py, aspect)
                normal = pair.first
                heart = pair.second
            }
        }
        val light = normalize(floatArrayOf(-0.45f, -0.65f, 0.61f))
        val lambert = max(surface[0] * light[0] + surface[1] * light[1] + surface[2] * light[2], 0f)
        val shading = 0.35f + 0.65f * lambert
        val rim = pow(1f - surface[2], 3f) * sphereGlow
        return Triple(normal, heart, shading + rim)
    }

    private fun roll(params: ExpressionEyeParams, x: Float, y: Float, aspect: Float): Pair<Float, Float> {
        val pivotX = params.centerX * aspect
        val pivotY = params.centerY
        val rad = Math.toRadians(params.roll.toDouble()).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        val px = x - pivotX
        val py = y - pivotY
        return Pair(c * px + s * py + pivotX, -s * px + c * py + pivotY)
    }

    private fun eyeMask(
        params: ExpressionEyeParams,
        x: Float,
        y: Float,
        cx: Float,
        cy: Float,
        hw: Float,
        hh: Float,
        dir: Float
    ): Pair<Float, Float> {
        var px = x - cx
        var py = y - cy
        py /= max(1f - params.blink, 0.03f)
        px -= py * params.slant

        val soft = params.softness
        val qx = px / max(hw, 1e-4f)
        val qy = py / max(hh, 1e-4f)
        val dScreen = (sqrt(qx * qx + qy * qy) - 1f) * min(hw, hh)

        val topLimit = hh * (1f - params.topLid) - params.lidCurve * hh * qx * qx +
            params.topSlope * hh * dir * qx
        val botLimit = -hh * (1f - params.botLid) + params.lidCurve * hh * qx * qx +
            params.lidSlant * hh * dir * qx

        val solid = (1f - smoothstep(-soft, soft, dScreen)) *
            (1f - smoothstep(topLimit - soft, topLimit + soft, py)) *
            smoothstep(botLimit - soft, botLimit + soft, py)

        val arcX = clamp(px, -hw * 0.78f, hw * 0.78f)
        val arcRatio = arcX / hw
        val arcY = params.arcCurve * hh * (1f - arcRatio * arcRatio)
        val slope = -2f * params.arcCurve * hh * arcRatio / hw
        val arcDistance = sqrt(
            sqr(px - arcX) + sqr((py - arcY) / sqrt(1f + slope * slope))
        ) - hh * params.stroke
        val arch = 1f - smoothstep(-soft, soft, arcDistance)

        val squeezedX = px * -dir
        val tipX = hw * 0.55f
        val chevronDistance = min(
            segmentDistance(squeezedX, py, -hw * 0.7f, hh * 0.7f, tipX, 0f),
            segmentDistance(squeezedX, py, -hw * 0.7f, -hh * 0.7f, tipX, 0f)
        )
        val chevron = 1f - smoothstep(hh * params.stroke - soft, hh * params.stroke + soft, chevronDistance)

        val heartEdge = max(soft * 0.55f / min(hw, hh), 1e-4f)
        val heart = 1f - smoothstep(-heartEdge, heartEdge, heartDistance(qx * 0.55f, qy * 0.55f + 0.55f))

        val solidWeight = max(0f, 1f - params.archWeight - params.chevronWeight - params.heartWeight)
        val normal = solid * solidWeight + arch * params.archWeight + chevron * params.chevronWeight
        return Pair(normal, heart * params.heartWeight)
    }

    /** Signed heart field. Negative inside. Mirrors heartDistance() in the spike. */
    private fun heartDistance(px: Float, py: Float): Float {
        val x = abs(px)
        if (x + py > 1f) {
            val dx = x - 0.25f
            val dy = py - 0.75f
            return sqrt(dx * dx + dy * dy) - 0.35355339f
        }
        val tipX = x
        val tipY = py - 1f
        val lobe = 0.5f * max(x + py, 0f)
        val sideX = x - lobe
        val sideY = py - lobe
        val tipD = tipX * tipX + tipY * tipY
        val sideD = sideX * sideX + sideY * sideY
        return sqrt(min(tipD, sideD)) * sign(x - py)
    }

    private fun segmentDistance(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax
        val aby = by - ay
        val denom = abx * abx + aby * aby
        if (denom == 0f) return sqrt(sqr(px - ax) + sqr(py - ay))
        val t = clamp(((px - ax) * abx + (py - ay) * aby) / denom, 0f, 1f)
        return sqrt(sqr(px - (ax + abx * t)) + sqr(py - (ay + aby * t)))
    }

    private fun rotateVector(v: FloatArray, yaw: Float, pitch: Float): FloatArray {
        val cy = cos(yaw)
        val sy = sin(yaw)
        val yawed = floatArrayOf(cy * v[0] + sy * v[2], v[1], -sy * v[0] + cy * v[2])
        val cx = cos(pitch)
        val sx = sin(pitch)
        return floatArrayOf(yawed[0], cx * yawed[1] - sx * yawed[2], sx * yawed[1] + cx * yawed[2])
    }

    private fun normalize(v: FloatArray): FloatArray {
        val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        return floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
    }

    private fun pow(base: Float, exponent: Float) = base.toDouble().pow(exponent.toDouble()).toFloat()

    private fun sqr(v: Float) = v * v

    private fun clamp(v: Float, lo: Float, hi: Float) = v.coerceIn(lo, hi)

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge0 == edge1) return if (x < edge0) 0f else 1f
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
