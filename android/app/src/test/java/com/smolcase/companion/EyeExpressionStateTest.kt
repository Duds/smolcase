package com.smolcase.companion

import com.smolcase.companion.matrix.CozmoEyeInterpolator
import com.smolcase.companion.matrix.EyeExpressionState
import com.smolcase.companion.matrix.ExpressionPresets
import com.smolcase.companion.matrix.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EyeExpressionStateTest {

    @Test
    fun `skeptical mood applies unequal openings modulated by humor dial`() {
        val state = EyeExpressionState().apply {
            currentMood = Mood.SKEPTICAL
            humor = 90
        }

        val params = state.computeTargets(0f, 0f, 0f, 1000L)

        // SKEPTICAL base scaleR is 0.62; the humor dial subtracts up to 0.12 more.
        assertTrue(
            "Right eye should be scaled smaller than left (deadpan squint)",
            params.scaleR < params.scaleL - 0.2f
        )
        // lidSlant base 0.35 plus humor term
        assertTrue("Lower lid should be lifted asymmetrically", params.lidSlant > 0.4f)
    }

    @Test
    fun `every mood name resolves to a preset`() {
        for (mood in Mood.values()) {
            assertEquals(mood.name, ExpressionPresets.ALL[mood.name]?.let { mood.name })
        }
    }

    @Test
    fun `momentary reaction switches mood and reverts after clock expiry`() {
        val state = EyeExpressionState().apply {
            currentMood = Mood.NEUTRAL
        }

        state.triggerReaction(Mood.HEART, durationMs = 1500L, nowMs = 1000L)
        assertEquals(Mood.HEART, state.currentMood)

        state.updateClock(nowMs = 2000L)
        assertEquals("Should still be in heart reaction", Mood.HEART, state.currentMood)

        state.updateClock(nowMs = 2600L)
        assertEquals("Should revert to neutral after duration", Mood.NEUTRAL, state.currentMood)
    }

    @Test
    fun `blink drives the model blink field`() {
        val state = EyeExpressionState()
        val open = state.computeTargets(0f, 0f, 0f, 1000L)
        val shut = state.computeTargets(0f, 0f, 1f, 1000L)

        assertEquals(0f, open.blink, 1e-6f)
        assertEquals(1f, shut.blink, 1e-6f)
    }

    @Test
    fun `interpolator smoothly approaches target parameters`() {
        val state = EyeExpressionState().apply { currentMood = Mood.HAPPY }
        val interpolator = CozmoEyeInterpolator(speed = 0.5f)

        val target = state.computeTargets(0f, 0f, 0f, 1000L)
        val initial = interpolator.params.archWeight

        interpolator.update(target)
        assertTrue(
            "Arch weight should move toward happy target",
            interpolator.params.archWeight > initial
        )
    }
}
