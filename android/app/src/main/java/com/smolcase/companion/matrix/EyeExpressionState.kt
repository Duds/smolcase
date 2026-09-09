package com.smolcase.companion.matrix

/**
 * Discrete emotional and functional states for the appliance eyes.
 *
 * The 16 names map 1:1 onto [ExpressionPresets], ported from the
 * face-expressions spike reference table.
 */
enum class Mood {
    NEUTRAL,      // Original asymmetric, filled-eye baseline
    HAPPY,        // Raised closed-eye arches
    EMBARRASSED,  // Smaller, gentler happy arches
    LAUGHING,     // Squeezed chevrons
    HEART,        // Filled heart eyes
    ANGRY,        // Top lids slope down towards the nose
    SHOCKED,      // Tall, wide-open solid eyes
    SMUG,         // Low, nearly horizontal lids
    SAD,          // Inner lid ends lift, reversing the angry slope
    EXHAUSTED,    // Almost-flat, drooping strokes
    SKEPTICAL,    // Unequal openings, lifted lower lid
    CURIOUS,      // Open and asymmetric
    DROWSY,       // Heavy upper lids
    SLEEPING,     // Closed resting curves
    ALERT,        // Wide open, baseline asymmetry preserved
    THINKING      // Unequal eyes with a slight lean
}

/**
 * Procedural target state generator.
 * Resolves a [Mood] preset through [ExpressionFaceModel], modulated by the
 * TARS personality dials, and applies gaze and blink.
 */
class EyeExpressionState {

    /** Matches the spike's locked default parameter set. */
    var expressionIntensity: Float = 0.6f
    var faceRollDegrees: Float = 0f

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
     * Compute the face target for the current mood, gaze offset and blink.
     * Returns a single [ExpressionEyeParams] holding both eyes.
     */
    fun computeTargets(
        gazeX: Float,
        gazeY: Float,
        blinkProgress: Float,
        nowMs: Long
    ): ExpressionEyeParams {
        val preset = ExpressionPresets.ALL.getValue(currentMood.name)
        val params = ExpressionFaceModel.resolve(
            base = ExpressionEyeParams(),
            mood = preset,
            intensity = expressionIntensity,
            humor = humor.toFloat(),
            honesty = honesty.toFloat()
        )

        // Gaze shifts the whole face within the panel (model coords are
        // height-normalised, y bottom-to-top).
        params.centerX += gazeX.coerceIn(-1f, 1f) * 0.04f
        params.centerY += gazeY.coerceIn(-1f, 1f) * 0.03f

        // Blink squeezes the apertures vertically via the model's blink term.
        params.blink = blinkProgress.coerceIn(0f, 1f)
        params.roll = faceRollDegrees

        return params
    }
}
