package com.smolcase.companion

import com.smolcase.companion.face.*
import com.smolcase.companion.matrix.Mood
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FaceFrameTest {
    private val parameters = FaceParameters.fromJson(File("src/main/assets/face/parameters.json").readText())

    @Test fun `fixed neutral frame applies the spike dials and gaze without translating the pattern`() {
        val frame = FaceFrame.fixed(parameters, gazeX = 0.5f, gazeY = 0.5f)
        assertEquals(0.93f, frame.eyes.scaleR, 0.000001f)
        assertEquals(0.105f, frame.eyes.slant, 0.000001f)
        assertEquals(0.4625f, frame.eyes.lidSlant, 0.000001f)
        assertEquals(0.138f, frame.eyes.topLid, 0.000001f)
        assertEquals(0.5f, frame.eyes.centerX, 0f)
        assertEquals(0.68f, frame.eyes.centerY, 0f)
        assertEquals(0.4363323f, frame.yawRadians, 0.000001f)
        assertEquals(-0.4363323f, frame.pitchRadians, 0.000001f)
        assertEquals(frame, FaceFrame.fixed(parameters, gazeX = 0.5f, gazeY = 0.5f))
    }

    @Test fun `expression damping uses seconds rather than frame count`() {
        val settings = parameters.copy(blinking = false, saccade = false)
        fun afterOneSecond(fps: Int): FaceFrame {
            val animator = FaceAnimator(settings, Random(0))
            var frame = FaceFrame.fixed(settings)
            repeat(fps) { frame = animator.advance(1f / fps, FaceInput(Mood.HAPPY, 75f, 90f)) }
            return frame
        }
        assertEquals(0.550749f, afterOneSecond(30).eyes.archWeight, 0.00001f)
        assertEquals(afterOneSecond(30).eyes.archWeight, afterOneSecond(120).eyes.archWeight, 0.00001f)
    }

    @Test fun `disabled blinking and saccades do not change a settled neutral frame`() {
        val settings = parameters.copy(blinking = false, saccade = false)
        val animator = FaceAnimator(settings, Random(0))
        repeat(400) {
            val frame = animator.advance(0.05f, FaceInput(Mood.NEUTRAL, 75f, 90f))
            assertEquals(0f, frame.blink, 0f)
            assertEquals(0f, frame.saccadeX, 0f)
            assertEquals(0f, frame.saccadeY, 0f)
        }
    }
}
