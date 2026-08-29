package com.smolcase.companion.matrix

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Rasterizes procedural SDF eyes and shapes directly onto an [ApplianceMatrixCanvas].
 *
 * Supports sub-dot anti-aliasing (smooth intensity falloff at the boundary)
 * and soft bloom/glow calculation that can bleed softly beyond boundaries.
 */
class CozmoEyeRasterizer(
    private val canvas: ApplianceMatrixCanvas
) {
    /**
     * Rasterizes a pair of left and right Cozmo eyes onto the matrix canvas buffer.
     *
     * @param leftEye Parameters for the left eye aperture.
     * @param rightEye Parameters for the right eye aperture.
     * @param dotRadiusNorm Normalized radius of a dot cell in screen space.
     * @param glowRadiusNorm Normalized radius of optical glow bloom.
     */
    fun rasterizeEyes(
        leftEye: CozmoEyeParams,
        rightEye: CozmoEyeParams,
        glowIntensity: Float = 0.35f
    ) {
        val cols = canvas.cols
        val rows = canvas.rows
        val dotRadiusNorm = 0.5f / cols // approx half cell in normalized coordinates

        for (r in 0 until rows) {
            val ny = (r + 0.5f) / rows.toFloat()
            for (c in 0 until cols) {
                val nx = (c + 0.5f) / cols.toFloat()

                // Evaluate distance to both eyes
                val distL = leftEye.signedDistance(nx, ny)
                val distR = rightEye.signedDistance(nx, ny)
                val minDist = min(distL, distR)

                // Sub-dot intensity (0 inside/near boundary -> 1 inside core)
                // dist <= 0 is inside, dist > 0 is outside
                val coreIntensity = if (minDist <= -dotRadiusNorm) {
                    1.0f
                } else if (minDist < dotRadiusNorm) {
                    // Smooth transition across the dot boundary
                    0.5f - (minDist / (2f * dotRadiusNorm))
                } else {
                    0.0f
                }.coerceIn(0f, 1f)

                // Outer optical glow bloom (falls off exponentially outside the eye)
                val glow = if (minDist > 0f) {
                    glowIntensity * exp(-minDist / (dotRadiusNorm * 2.8f))
                } else {
                    0f
                }

                val finalIntensity = min(coreIntensity + glow, 1.0f)
                if (finalIntensity > 0.001f) {
                    canvas.setDot(c, r, finalIntensity)
                }
            }
        }
    }

    /**
     * Rasterizes a parametric Heart icon (`♥`) onto the matrix canvas (for care/fondness reactions).
     * Bounded to the upper half if desired or centered in the eye region.
     */
    fun rasterizeHeart(
        centerX: Float,
        centerY: Float,
        size: Float,
        intensity: Float = 1.0f
    ) {
        val cols = canvas.cols
        val rows = canvas.rows
        val dotRadiusNorm = 0.5f / cols

        for (r in 0 until rows) {
            val ny = (r + 0.5f) / rows.toFloat()
            if (ny > 0.55f) continue // stay in top half + slight bloom
            for (c in 0 until cols) {
                val nx = (c + 0.5f) / cols.toFloat()
                
                val dx = (nx - centerX) / size
                val dy = -(ny - centerY) / size // invert Y so heart points down

                // Heart equation: (x^2 + y^2 - 1)^3 - x^2 * y^3 <= 0
                val a = dx * dx + dy * dy - 0.7f
                val d = a * a * a - dx * dx * dy * dy * dy

                if (d <= 0.05f) {
                    val dotVal = if (d <= 0f) 1f else (1f - (d / 0.05f))
                    canvas.blendDot(c, r, dotVal * intensity)
                }
            }
        }
    }

    /**
     * Rasterizes a radar / thinking scan pulse within the eye region.
     */
    fun rasterizeThinkingScan(
        centerX: Float,
        centerY: Float,
        radius: Float,
        phase: Float,
        intensity: Float = 0.9f
    ) {
        val cols = canvas.cols
        val rows = canvas.rows

        for (r in 0 until rows) {
            val ny = (r + 0.5f) / rows.toFloat()
            if (ny > 0.52f) continue
            for (c in 0 until cols) {
                val nx = (c + 0.5f) / cols.toFloat()
                val dx = nx - centerX
                val dy = ny - centerY
                val dist = sqrt(dx * dx + dy * dy)

                // Ring ripple expanding outward
                val ringDist = abs(dist - (radius * (phase % 1.0f)))
                if (ringDist < 0.04f && dist <= radius) {
                    val pulseVal = (1f - (ringDist / 0.04f)) * (1f - (phase % 1.0f))
                    canvas.blendDot(c, r, pulseVal * intensity)
                }
            }
        }
    }
}
