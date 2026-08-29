package com.smolcase.companion.matrix

import kotlin.math.abs

/**
 * Critically-damped spring-damper interpolator for smooth, organic eye morphing.
 */
class CozmoEyeInterpolator(
    private val speed: Float = 0.18f
) {
    var leftEye = CozmoEyeParams(centerX = 0.30f, centerY = 0.23f)
    var rightEye = CozmoEyeParams(centerX = 0.70f, centerY = 0.23f)

    /**
     * Smoothly steps current eye parameters toward target parameters.
     */
    fun update(targetLeft: CozmoEyeParams, targetRight: CozmoEyeParams) {
        leftEye.centerX += (targetLeft.centerX - leftEye.centerX) * speed
        leftEye.centerY += (targetLeft.centerY - leftEye.centerY) * speed
        leftEye.width += (targetLeft.width - leftEye.width) * speed
        leftEye.height += (targetLeft.height - leftEye.height) * speed
        leftEye.cornerRadius += (targetLeft.cornerRadius - leftEye.cornerRadius) * speed
        leftEye.slantRad += (targetLeft.slantRad - leftEye.slantRad) * speed
        leftEye.scaleX += (targetLeft.scaleX - leftEye.scaleX) * speed
        leftEye.scaleY += (targetLeft.scaleY - leftEye.scaleY) * speed
        leftEye.topLidPos += (targetLeft.topLidPos - leftEye.topLidPos) * speed
        leftEye.topLidAngleRad += (targetLeft.topLidAngleRad - leftEye.topLidAngleRad) * speed
        leftEye.topLidCurvature += (targetLeft.topLidCurvature - leftEye.topLidCurvature) * speed
        leftEye.bottomLidPos += (targetLeft.bottomLidPos - leftEye.bottomLidPos) * speed
        leftEye.bottomLidAngleRad += (targetLeft.bottomLidAngleRad - leftEye.bottomLidAngleRad) * speed
        leftEye.bottomLidCurvature += (targetLeft.bottomLidCurvature - leftEye.bottomLidCurvature) * speed

        rightEye.centerX += (targetRight.centerX - rightEye.centerX) * speed
        rightEye.centerY += (targetRight.centerY - rightEye.centerY) * speed
        rightEye.width += (targetRight.width - rightEye.width) * speed
        rightEye.height += (targetRight.height - rightEye.height) * speed
        rightEye.cornerRadius += (targetRight.cornerRadius - rightEye.cornerRadius) * speed
        rightEye.slantRad += (targetRight.slantRad - rightEye.slantRad) * speed
        rightEye.scaleX += (targetRight.scaleX - rightEye.scaleX) * speed
        rightEye.scaleY += (targetRight.scaleY - rightEye.scaleY) * speed
        rightEye.topLidPos += (targetRight.topLidPos - rightEye.topLidPos) * speed
        rightEye.topLidAngleRad += (targetRight.topLidAngleRad - rightEye.topLidAngleRad) * speed
        rightEye.topLidCurvature += (targetRight.topLidCurvature - rightEye.topLidCurvature) * speed
        rightEye.bottomLidPos += (targetRight.bottomLidPos - rightEye.bottomLidPos) * speed
        rightEye.bottomLidAngleRad += (targetRight.bottomLidAngleRad - rightEye.bottomLidAngleRad) * speed
        rightEye.bottomLidCurvature += (targetRight.bottomLidCurvature - rightEye.bottomLidCurvature) * speed
    }
}
