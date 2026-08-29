package com.smolcase.companion.matrix

import kotlin.math.sin

/**
 * Discrete emotional and functional states for the Cozmo-style appliance eyes.
 */
enum class Mood {
    NEUTRAL,      // Calm, balanced, steady gaze
    SKEPTICAL,    // Asymmetrical deadpan squint (humor/sarcasm driven)
    HAPPY,        // Upward curved crescents with bounce
    CURIOUS,      // Slight inward slant, widened apertures
    DROWSY,       // Half-mast top lids, slower drift
    SLEEPING,     // Nearly closed horizontal slit, rhythmic breathing
    ALERT,        // Wide, tall apertures reacting to sound / IMU
    THINKING,     // Processing pulse / radar scan
    HEART         // Love / care reaction overlay
}

/**
 * Procedural target state generator and morphing interpolator.
 * Modulates eye parameters based on internal Mood and TARS personality dials.
 */
class EyeExpressionState {

    var currentMood: Mood = Mood.NEUTRAL
    
    // Personality modifiers (0..100)
    var humor: Int = 75
    var honesty: Int = 90

    // Momentary reaction timer
    private var reactionEndMs: Long = 0L
    private var prevMood: Mood = Mood.NEUTRAL

    /**
     * Trigger a momentary reaction (e.g. HEART or THINKING) that reverts after [durationMs].
     */
    fun triggerReaction(reaction: Mood, durationMs: Long, nowMs: Long) {
        if (currentMood != reaction) {
            prevMood = currentMood
            currentMood = reaction
        }
        reactionEndMs = nowMs + durationMs
    }

    /**
     * Update internal state machine clock.
     */
    fun updateClock(nowMs: Long) {
        if (reactionEndMs > 0L && nowMs >= reactionEndMs) {
            currentMood = prevMood
            reactionEndMs = 0L
        }
    }

    /**
     * Compute target Cozmo eye parameters for left and right eyes based on mood and gaze offset.
     *
     * @param gazeX Normalized gaze offset in [-1..1]
     * @param gazeY Normalized gaze offset in [-1..1]
     * @param blinkProgress Blink closure in [0..1] (0 = open, 1 = fully shut)
     * @param nowMs Monotonic timestamp for continuous subtle micro-motions
     */
    fun computeTargets(
        gazeX: Float,
        gazeY: Float,
        blinkProgress: Float,
        nowMs: Long
    ): Pair<CozmoEyeParams, CozmoEyeParams> {
        // Base eye centers in upper half (gaze shifts the whole eye within boundaries)
        // Upper half roaming range: X in [0.22..0.38] / [0.62..0.78], Y in [0.15..0.35]
        val gazeOffsetX = gazeX.coerceIn(-1f, 1f) * 0.08f
        val gazeOffsetY = gazeY.coerceIn(-1f, 1f) * 0.06f

        val baseLeftX = 0.30f + gazeOffsetX
        val baseRightX = 0.70f + gazeOffsetX
        val baseY = 0.23f + gazeOffsetY

        val left = CozmoEyeParams(centerX = baseLeftX, centerY = baseY)
        val right = CozmoEyeParams(centerX = baseRightX, centerY = baseY)

        // Apply Mood profiles
        when (currentMood) {
            Mood.NEUTRAL -> {
                left.width = 0.26f
                left.height = 0.20f
                left.cornerRadius = 0.07f
                right.width = 0.26f
                right.height = 0.20f
                right.cornerRadius = 0.07f
            }
            Mood.SKEPTICAL -> {
                // Asymmetrical expression: humor dial amplifies right-eye brow raise / left squint
                val humorFactor = (humor / 100f).coerceIn(0.2f, 1.0f)
                left.width = 0.26f
                left.height = 0.16f
                left.topLidPos = 0.40f * humorFactor
                left.slantRad = -0.05f

                right.width = 0.26f
                right.height = 0.22f
                right.topLidPos = 0.0f
                right.bottomLidPos = 0.25f * humorFactor
                right.slantRad = 0.08f * humorFactor
            }
            Mood.HAPPY -> {
                // Kawaii curved crescents (^ ^)
                left.width = 0.27f
                left.height = 0.18f
                left.bottomLidPos = 0.55f
                left.bottomLidCurvature = 0.06f
                left.scaleY = 0.9f

                right.width = 0.27f
                right.height = 0.18f
                right.bottomLidPos = 0.55f
                right.bottomLidCurvature = 0.06f
                right.scaleY = 0.9f
            }
            Mood.CURIOUS -> {
                // Widened eyes with inward slant
                left.width = 0.28f
                left.height = 0.24f
                left.slantRad = 0.08f
                right.width = 0.28f
                right.height = 0.24f
                right.slantRad = -0.08f
            }
            Mood.DROWSY -> {
                // Droopy top lids
                left.width = 0.26f
                left.height = 0.18f
                left.topLidPos = 0.55f
                right.width = 0.26f
                right.height = 0.18f
                right.topLidPos = 0.55f
            }
            Mood.SLEEPING -> {
                // Slit line
                left.width = 0.24f
                left.height = 0.08f
                left.topLidPos = 0.85f
                right.width = 0.24f
                right.height = 0.08f
                right.topLidPos = 0.85f
            }
            Mood.ALERT -> {
                // Wide, shocked apertures
                left.width = 0.29f
                left.height = 0.26f
                right.width = 0.29f
                right.height = 0.26f
            }
            Mood.THINKING, Mood.HEART -> {
                // Main eye bodies shrink or dim during full reaction overlays
                left.width = 0.20f
                left.height = 0.14f
                right.width = 0.20f
                right.height = 0.14f
            }
        }

        // Apply Blink closure
        if (blinkProgress > 0f) {
            val closeAmount = blinkProgress.coerceIn(0f, 1f)
            left.topLidPos = max(left.topLidPos, closeAmount)
            left.bottomLidPos = max(left.bottomLidPos, closeAmount * 0.6f)
            right.topLidPos = max(right.topLidPos, closeAmount)
            right.bottomLidPos = max(right.bottomLidPos, closeAmount * 0.6f)
        }

        return Pair(left, right)
    }

    private fun max(a: Float, b: Float): Float = if (a > b) a else b
}
