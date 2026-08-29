package com.smolcase.companion.matrix

import kotlin.math.PI
import kotlin.math.sin

/**
 * Appliance LED/VFD dot matrix canvas model.
 *
 * Represents a discrete LED grid spanning the entire screen.
 * Dots are addressed by (col, row). Dot values range from 0.0f (off) to 1.0f (full bright).
 *
 * Supports:
 * - Ghost dot baseline alpha
 * - Sub-dot intensity mapping with radial plateau
 * - Low-frequency ambient breathing wave across unlit dots
 */
class ApplianceMatrixCanvas(
    val cols: Int = 38,
    val rows: Int = 68,
    val ghostAlpha: Float = 0.05f
) {
    val totalDots = cols * rows
    val buffer = FloatArray(totalDots)

    /**
     * Clear all lit dots back to 0.
     */
    fun clear() {
        buffer.fill(0f)
    }

    /**
     * Set the intensity of a specific dot in [0..cols-1] and [0..rows-1].
     */
    fun setDot(col: Int, row: Int, intensity: Float) {
        if (col in 0 until cols && row in 0 until rows) {
            val idx = row * cols + col
            buffer[idx] = intensity.coerceIn(0f, 1f)
        }
    }

    /**
     * Blend/accumulate intensity for a dot (max or additive blend).
     */
    fun blendDot(col: Int, row: Int, intensity: Float) {
        if (col in 0 until cols && row in 0 until rows) {
            val idx = row * cols + col
            val current = buffer[idx]
            buffer[idx] = (current + intensity).coerceIn(0f, 1f)
        }
    }

    fun getDot(col: Int, row: Int): Float {
        if (col in 0 until cols && row in 0 until rows) {
            return buffer[row * cols + col]
        }
        return 0f
    }

    /**
     * Calculates the ambient breathing intensity for an unlit dot at (col, row)
     * at time [nowMs]. Unlit dots undulate subtly at ~0.1 Hz.
     */
    fun ambientBreathing(col: Int, row: Int, nowMs: Long, isSleeping: Boolean = false): Float {
        val freq = if (isSleeping) 0.0008f else 0.0006f // ~0.1 Hz
        val phase = (row.toFloat() / rows) * 0.5f // gentle vertical wave gradient
        val wave = 0.5f + 0.5f * sin((nowMs * freq * 2.0 * PI + phase).toFloat())
        
        return if (isSleeping) {
            // Sleeping: deeper breathing modulation
            ghostAlpha * (0.4f + 0.6f * wave)
        } else {
            // Awake: gentle idle pulse
            ghostAlpha * (0.8f + 0.4f * wave)
        }
    }
}
