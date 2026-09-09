package com.smolcase.companion

import com.smolcase.companion.matrix.ApplianceMatrixCanvas
import com.smolcase.companion.matrix.CozmoEyeRasterizer
import com.smolcase.companion.matrix.ExpressionEyeParams
import com.smolcase.companion.matrix.ExpressionFaceModel
import com.smolcase.companion.matrix.ExpressionPresets
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
    fun `rasterizer maps face model onto canvas with coordinate flip`() {
        val canvas = ApplianceMatrixCanvas(cols = 38, rows = 68)
        val rasterizer = CozmoEyeRasterizer(canvas)

        val params = ExpressionEyeParams()
        rasterizer.rasterizeFace(params)

        // Left eye centre in model coords (y bottom-to-top) maps to a canvas dot.
        val eyeX = params.centerX * rasterizer.aspect - params.gap / 2f
        val eyeCol = (eyeX / rasterizer.aspect * canvas.cols).toInt()
        val eyeRow = ((1f - params.centerY) * canvas.rows).toInt()
        assertTrue(
            "Eye centre dot should be lit (got ${canvas.getDot(eyeCol, eyeRow)})",
            canvas.getDot(eyeCol, eyeRow) > 0.5f
        )

        // Model y=0 is canvas bottom row; eyes sit in the model upper half.
        val bottomRow = canvas.rows - 1
        assertEquals(
            "Bottom rows must stay unlit for the baseline face",
            0f, canvas.getDot(eyeCol, bottomRow), 0.001f
        )
    }

    @Test
    fun `heart channel stays separate from the canvas buffer`() {
        val canvas = ApplianceMatrixCanvas(cols = 38, rows = 68)
        val rasterizer = CozmoEyeRasterizer(canvas)

        val heart = ExpressionFaceModel.resolve(
            ExpressionEyeParams(), ExpressionPresets.HEART, 1f, 0f, 0f
        )
        rasterizer.rasterizeFace(heart)

        var heartLit = 0
        var bufferLit = 0
        for (i in heartBufferIndices(rasterizer)) {
            if (rasterizer.heartBuffer[i] > 0.5f) heartLit++
            if (canvas.buffer[i] > 0.5f) bufferLit++
        }
        assertTrue("Heart preset should light the heart channel", heartLit > 20)
        assertEquals("Heart preset must not light the normal buffer", 0, bufferLit)
    }

    private fun heartBufferIndices(r: CozmoEyeRasterizer): IntRange = r.heartBuffer.indices
}
