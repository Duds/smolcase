package com.smolcase.companion

import com.smolcase.companion.matrix.CozmoEyeInterpolator
import com.smolcase.companion.matrix.EyeExpressionState
import com.smolcase.companion.matrix.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EyeExpressionStateTest {

    @Test
    fun `skeptical mood applies asymmetrical lid angles modulated by humor dial`() {
        val state = EyeExpressionState().apply {
            currentMood = Mood.SKEPTICAL
            humor = 90
        }

        val (left, right) = state.computeTargets(0f, 0f, 0f, 1000L)

        // Left eye should have a lowered top lid (squinting)
        assertTrue("Left eye top lid should be lowered", left.topLidPos > 0.3f)
        // Right eye should have a slant and raised brow
        assertTrue("Right eye slant should be positive", right.slantRad > 0.05f)
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
    fun `interpolator smoothly approaches target parameters`() {
        val state = EyeExpressionState().apply { currentMood = Mood.HAPPY }
        val interpolator = CozmoEyeInterpolator(speed = 0.5f)

        val (targetL, targetR) = state.computeTargets(0f, 0f, 0f, 1000L)
        val initialLid = interpolator.leftEye.bottomLidPos

        interpolator.update(targetL, targetR)
        assertTrue("Bottom lid should move toward happy target", interpolator.leftEye.bottomLidPos > initialLid)
    }
}
