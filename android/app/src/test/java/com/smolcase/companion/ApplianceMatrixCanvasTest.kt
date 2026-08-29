package com.smolcase.companion

import com.smolcase.companion.matrix.ApplianceMatrixCanvas
import com.smolcase.companion.matrix.CozmoEyeParams
import com.smolcase.companion.matrix.CozmoEyeRasterizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplianceMatrixCanvasTest {

    @Test
    fun `matrix buffer initializes empty and gets set properly`() {
        val canvas = ApplianceMatrixCanvas(cols = 20, rows = 30, ghostAlpha = 0.05f)
        assertEquals(20, canvas.cols)
        assertEquals(30, canvas.rows)
        assertEquals(0f, canvas.getDot(5, 5), 0.001f)

        canvas.setDot(5, 5, 0.8f)
        assertEquals(0.8f, canvas.getDot(5, 5), 0.001f)

        canvas.blendDot(5, 5, 0.5f)
        assertEquals(1.0f, canvas.getDot(5, 5), 0.001f) // clamped to 1.0

        canvas.clear()
        assertEquals(0f, canvas.getDot(5, 5), 0.001f)
    }

    @Test
    fun `ambient breathing produces values within ghost alpha range`() {
        val canvas = ApplianceMatrixCanvas(cols = 20, rows = 30, ghostAlpha = 0.05f)
        val breathAwake = canvas.ambientBreathing(5, 10, nowMs = 1000L, isSleeping = false)
        assertTrue("Breath awake should be positive", breathAwake > 0f)
        assertTrue("Breath awake should stay subtle", breathAwake <= 0.07f)

        val breathSleep = canvas.ambientBreathing(5, 10, nowMs = 1000L, isSleeping = true)
        assertTrue("Breath sleep should be positive", breathSleep > 0f)
    }

    @Test
    fun `cozmo eye SDF strictly enforces centerline boundary`() {
        val eye = CozmoEyeParams(
            centerX = 0.5f,
            centerY = 0.35f,
            width = 0.3f,
            height = 0.2f,
            maxBoundaryY = 0.50f
        )

        // Point well inside the upper eye region
        val insideDist = eye.signedDistance(0.5f, 0.35f)
        assertTrue("Center of eye must be negative signed distance", insideDist < 0f)

        // Point below the 0.50 centerline
        val outsideDist = eye.signedDistance(0.5f, 0.55f)
        assertTrue("Point below centerline must be outside (> 0)", outsideDist > 0f)
    }

    @Test
    fun `rasterizer fills upper matrix dots and respects bounds`() {
        val canvas = ApplianceMatrixCanvas(cols = 30, rows = 50)
        val rasterizer = CozmoEyeRasterizer(canvas)

        val leftEye = CozmoEyeParams(centerX = 0.3f, centerY = 0.25f, width = 0.2f, height = 0.15f)
        val rightEye = CozmoEyeParams(centerX = 0.7f, centerY = 0.25f, width = 0.2f, height = 0.15f)

        rasterizer.rasterizeEyes(leftEye, rightEye, glowIntensity = 0.1f)

        // Dot near left eye center should be lit
        val eyeCol = (0.3f * canvas.cols).toInt()
        val eyeRow = (0.25f * canvas.rows).toInt()
        assertTrue("Eye center dot should be brightly lit", canvas.getDot(eyeCol, eyeRow) > 0.8f)

        // Dot far into the bottom half (row = 40 of 50, ny = 0.8) should be 0
        val bottomRow = (0.8f * canvas.rows).toInt()
        assertEquals("Bottom half dot should be completely unlit", 0f, canvas.getDot(eyeCol, bottomRow), 0.001f)
    }
}
