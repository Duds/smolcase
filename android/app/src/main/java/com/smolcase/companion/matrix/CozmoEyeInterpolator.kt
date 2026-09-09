package com.smolcase.companion.matrix

import kotlin.reflect.KMutableProperty1

/**
 * Critically-damped spring-damper interpolator for smooth, organic face morphing.
 * Lerps every Float field of [ExpressionEyeParams] toward the target.
 */
class CozmoEyeInterpolator(
    private val speed: Float = 0.18f
) {
    var params = ExpressionEyeParams()

    private val fields: List<KMutableProperty1<ExpressionEyeParams, Float>> = listOf(
        ExpressionEyeParams::eyeW, ExpressionEyeParams::eyeH,
        ExpressionEyeParams::gap, ExpressionEyeParams::centerX,
        ExpressionEyeParams::centerY, ExpressionEyeParams::scaleL,
        ExpressionEyeParams::scaleR, ExpressionEyeParams::offsetYL,
        ExpressionEyeParams::offsetYR, ExpressionEyeParams::topLid,
        ExpressionEyeParams::botLid, ExpressionEyeParams::lidCurve,
        ExpressionEyeParams::lidSlant, ExpressionEyeParams::slant,
        ExpressionEyeParams::softness, ExpressionEyeParams::brightness,
        ExpressionEyeParams::topSlope, ExpressionEyeParams::stroke,
        ExpressionEyeParams::archWeight, ExpressionEyeParams::chevronWeight,
        ExpressionEyeParams::heartWeight, ExpressionEyeParams::arcCurve,
        ExpressionEyeParams::roll, ExpressionEyeParams::blink
    )

    /**
     * Smoothly steps current face parameters toward the target parameters.
     */
    fun update(target: ExpressionEyeParams) {
        for (field in fields) {
            val cur = field.get(params)
            field.set(params, cur + (field.get(target) - cur) * speed)
        }
    }
}
