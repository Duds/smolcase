package com.smolcase.companion.face

import com.smolcase.companion.matrix.ExpressionEyeParams
import com.smolcase.companion.matrix.Mood
import org.json.JSONObject

/** Captured spike controls. No renderer-local tuning constants. */
data class SphereParameters(
    val enabled: Boolean, val radius: Float, val centerY: Float,
    val glow: Float, val yawDegrees: Float, val pitchDegrees: Float
)

data class EmitterParameters(
    val cols: Int, val rows: Int, val radius: Float, val softness: Float,
    val cellFill: Float, val glow: Float, val glowSpread: Float,
    val halation: Float, val ghostFloor: Float, val exposure: Float, val sourceTint: Float
)

data class FaceParameters(
    val eyes: ExpressionEyeParams,
    val sphere: SphereParameters,
    val optics: EmitterParameters,
    val mood: Mood,
    val intensity: Float,
    val blendSpeed: Float,
    val humor: Float,
    val honesty: Float,
    val showSource: Boolean,
    val blinking: Boolean,
    val saccade: Boolean
) {
    fun toJson(): String = JSONObject().apply {
        for (field in EYE_FIELDS) put(field.name, field.get(eyes).toDouble())
        put("sphereMode", sphere.enabled)
        put("sphereRadius", sphere.radius.toDouble())
        put("sphereY", sphere.centerY.toDouble())
        put("sphereGlow", sphere.glow.toDouble())
        put("gazeYaw", sphere.yawDegrees.toDouble())
        put("gazePitch", sphere.pitchDegrees.toDouble())
        put("cols", optics.cols)
        put("rows", optics.rows)
        put("dotRadius", optics.radius.toDouble())
        put("dotSoftness", optics.softness.toDouble())
        put("cellFill", optics.cellFill.toDouble())
        put("glow", optics.glow.toDouble())
        put("glowSpread", optics.glowSpread.toDouble())
        put("halation", optics.halation.toDouble())
        put("ghostFloor", optics.ghostFloor.toDouble())
        put("exposure", optics.exposure.toDouble())
        put("sourceTint", optics.sourceTint.toDouble())
        put("mood", mood.name)
        put("intensity", intensity.toDouble())
        put("blendSpeed", blendSpeed.toDouble())
        put("humor", humor.toDouble())
        put("honesty", honesty.toDouble())
        put("showSource", showSource)
        put("blinking", blinking)
        put("saccade", saccade)
    }.toString(2)

    companion object {
        // Matches SHAPE_KEYS in the spike. Blink is transient, not a captured shape field.
        val EYE_FIELDS = listOf(
            ExpressionEyeParams::eyeW, ExpressionEyeParams::eyeH, ExpressionEyeParams::gap,
            ExpressionEyeParams::centerX, ExpressionEyeParams::centerY,
            ExpressionEyeParams::scaleL, ExpressionEyeParams::scaleR,
            ExpressionEyeParams::offsetYL, ExpressionEyeParams::offsetYR,
            ExpressionEyeParams::topLid, ExpressionEyeParams::botLid,
            ExpressionEyeParams::lidCurve, ExpressionEyeParams::lidSlant,
            ExpressionEyeParams::slant, ExpressionEyeParams::softness,
            ExpressionEyeParams::brightness, ExpressionEyeParams::topSlope,
            ExpressionEyeParams::stroke, ExpressionEyeParams::archWeight,
            ExpressionEyeParams::chevronWeight, ExpressionEyeParams::heartWeight,
            ExpressionEyeParams::arcCurve, ExpressionEyeParams::roll
        )

        fun fromJson(text: String): FaceParameters {
            try {
                val json = JSONObject(text)
                val consumed = mutableSetOf<String>()
                fun number(key: String, min: Float, max: Float): Float {
                    consumed += key
                    val value = json.get(key)
                    require(value is Number) { "$key must be numeric" }
                    return value.toFloat().also {
                        require(it.isFinite() && it in min..max) { "$key outside $min..$max" }
                    }
                }
                fun integer(key: String, min: Int, max: Int): Int {
                    val value = number(key, min.toFloat(), max.toFloat())
                    require(value == value.toInt().toFloat()) { "$key must be an integer" }
                    return value.toInt()
                }
                fun bool(key: String): Boolean {
                    consumed += key
                    return json.get(key).also { require(it is Boolean) { "$key must be boolean" } } as Boolean
                }
                val eyes = ExpressionEyeParams()
                for (field in EYE_FIELDS) {
                    val range = when (field.name) {
                        "eyeW", "eyeH", "softness", "stroke" -> 0.0001f..1f
                        "scaleL", "scaleR" -> 0.01f..3f
                        "centerX", "centerY", "gap", "topLid", "botLid",
                        "archWeight", "chevronWeight", "heartWeight" -> 0f..1f
                        "brightness" -> 0f..6f
                        "roll" -> -180f..180f
                        else -> -2f..2f
                    }
                    field.set(eyes, number(field.name, range.start, range.endInclusive))
                }
                val sphere = SphereParameters(
                    bool("sphereMode"), number("sphereRadius", 0.001f, 1f),
                    number("sphereY", 0f, 1f), number("sphereGlow", 0f, 1f),
                    number("gazeYaw", 0f, 180f), number("gazePitch", 0f, 180f)
                )
                val optics = EmitterParameters(
                    integer("cols", 1, 256), integer("rows", 1, 256),
                    number("dotRadius", 0.001f, 1f), number("dotSoftness", 0.001f, 1f),
                    number("cellFill", 0.001f, 1f), number("glow", 0f, 6f),
                    number("glowSpread", 0.001f, 2f), number("halation", 0f, 1f),
                    number("ghostFloor", 0f, 1f), number("exposure", 0f, 10f),
                    number("sourceTint", 0f, 1f)
                )
                consumed += "mood"
                val result = FaceParameters(
                    eyes, sphere, optics, Mood.valueOf(json.getString("mood")),
                    number("intensity", 0f, 1f), number("blendSpeed", 0.001f, 20f),
                    number("humor", 0f, 100f), number("honesty", 0f, 100f),
                    bool("showSource"), bool("blinking"), bool("saccade")
                )
                require(json.keys().asSequence().toSet() == consumed) { "Unknown face parameters" }
                return result
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid face parameters: ${e.message}", e)
            }
        }
    }
}
