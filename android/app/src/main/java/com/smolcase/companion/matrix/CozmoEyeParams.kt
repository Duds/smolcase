package com.smolcase.companion.matrix

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Parametric Cozmo-style kawaii eye model evaluated via Signed Distance Fields (SDF).
 *
 * Eyes are solid glowing dot clusters without inner pupils or specular glints.
 * Shape deformation is achieved through:
 * - Rounded rectangular pill with corner radius
 * - Top and bottom eyelid cutoffs (position, angle, curvature)
 * - Whole-eye slant angle (inward for determination/focus, outward for sad/pleading)
 * - Squash & stretch scaling
 * - Strictly bounded above the horizontal screen centerline (Y <= 0.5)
 */
data class CozmoEyeParams(
    var centerX: Float = 0.5f,
    var centerY: Float = 0.25f,
    var width: Float = 0.28f,
    var height: Float = 0.22f,
    var cornerRadius: Float = 0.08f,
    var slantRad: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    
    // Top lid: 0.0 = fully open, 1.0 = fully closed down
    var topLidPos: Float = 0f,
    var topLidAngleRad: Float = 0f,
    var topLidCurvature: Float = 0f,
    
    // Bottom lid: 0.0 = fully open, 1.0 = fully closed up
    var bottomLidPos: Float = 0f,
    var bottomLidAngleRad: Float = 0f,
    var bottomLidCurvature: Float = 0f,

    // Centerline boundary enforcement (normalized Y where eye body must clip)
    val maxBoundaryY: Float = 0.50f
) {
    /**
     * Evaluates signed distance from normalized point (nx, ny) in [0..1] to the eye shape.
     * Negative value = inside eye, positive value = outside eye.
     */
    fun signedDistance(nx: Float, ny: Float): Float {
        // Hard boundary cutoff at maxBoundaryY (centreline)
        if (ny > maxBoundaryY) {
            return (ny - maxBoundaryY) + 0.01f
        }

        // Translate to eye center
        val dx = nx - centerX
        val dy = ny - centerY

        // Rotate by slant angle
        val cosA = cos(-slantRad)
        val sinA = sin(-slantRad)
        val rx = dx * cosA - dy * sinA
        val ry = dx * sinA + dy * cosA

        // Apply inverse squash & stretch scale
        val sx = rx / max(scaleX, 0.01f)
        val sy = ry / max(scaleY, 0.01f)

        // Base rounded box SDF
        val halfW = (width * 0.5f) - cornerRadius
        val halfH = (height * 0.5f) - cornerRadius
        val qx = abs(sx) - halfW
        val qy = abs(sy) - halfH
        val outsideDist = sqrt(max(qx, 0f) * max(qx, 0f) + max(qy, 0f) * max(qy, 0f))
        val insideDist = min(max(qx, qy), 0f)
        val baseBoxDist = outsideDist + insideDist - cornerRadius

        // Top eyelid clipping plane
        // topLidPos: 0 = top of eye (+halfH), 1 = bottom of eye (-halfH)
        val topLidY = (height * 0.5f) - (topLidPos * height)
        val topLidPlane = (sy - topLidY) + (sx * sin(topLidAngleRad)) - (topLidCurvature * (1f - (sx / (width * 0.5f)).let { it * it }))

        // Bottom eyelid clipping plane
        // bottomLidPos: 0 = bottom of eye (-halfH), 1 = top of eye (+halfH)
        val bottomLidY = -(height * 0.5f) + (bottomLidPos * height)
        val bottomLidPlane = (bottomLidY - sy) + (sx * sin(bottomLidAngleRad)) - (bottomLidCurvature * (1f - (sx / (width * 0.5f)).let { it * it }))

        // Intersection (max of distances) gives clipped eye body
        return max(baseBoxDist, max(topLidPlane, bottomLidPlane))
    }
}
