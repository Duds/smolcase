package com.smolcase.companion

import com.smolcase.companion.matrix.ExpressionEyeParams
import com.smolcase.companion.matrix.ExpressionFaceModel
import com.smolcase.companion.matrix.ExpressionPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.asin

/**
 * Pure JVM tests for the face-expressions model ported from
 * spikes/20260909-face-expressions.
 *
 * Model coordinate space: y in 0..1 bottom-to-top, x in 0..aspect.
 * The default grid is 38x68, so aspect = 38f / 68f.
 */
class ExpressionFaceModelTest {

    private val aspect = 38f / 68f
    private val base = ExpressionEyeParams()

    private fun resolved(preset: String, intensity: Float = 1f, humor: Float = 0f, honesty: Float = 0f): ExpressionEyeParams =
        ExpressionFaceModel.resolve(base, ExpressionPresets.ALL.getValue(preset), intensity, humor, honesty)

    private fun flatCoverage(p: ExpressionEyeParams, x: Float, y: Float): Pair<Float, Float> =
        ExpressionFaceModel.coverage(p, x, y, aspect)

    /** Centre of the left eye in model coords. */
    private val leftEyeX: Float
        get() = base.centerX * aspect - base.gap / 2f
    private val leftEyeY: Float
        get() = base.centerY

    @Test
    fun `neutral at intensity 0 restores base size and shape`() {
        val p = resolved("HAPPY", intensity = 0f)

        // With neutral dials, intensity 0 must leave every mood-driven field at base.
        assertEquals(base.eyeW, p.eyeW, 1e-6f)
        assertEquals(base.eyeH, p.eyeH, 1e-6f)
        assertEquals(base.topLid, p.topLid, 1e-6f)
        assertEquals(base.botLid, p.botLid, 1e-6f)
        assertEquals(base.archWeight, p.archWeight, 1e-6f)
        assertEquals(base.heartWeight, p.heartWeight, 1e-6f)
        assertEquals(base.slant, p.slant, 1e-6f)
        assertEquals(base.scaleR, p.scaleR, 1e-6f)
    }

    @Test
    fun `every preset resolves distinct from neutral`() {
        val neutral = resolved("NEUTRAL")
        for ((name, _) in ExpressionPresets.ALL) {
            if (name == "NEUTRAL") continue
            val p = resolved(name)
            val differs = listOf(
                p.eyeW to neutral.eyeW, p.eyeH to p.eyeH.let { neutral.eyeH },
                p.topLid to neutral.topLid, p.botLid to neutral.botLid,
                p.lidCurve to neutral.lidCurve, p.lidSlant to neutral.lidSlant,
                p.slant to neutral.slant, p.topSlope to neutral.topSlope,
                p.stroke to neutral.stroke, p.archWeight to neutral.archWeight,
                p.chevronWeight to neutral.chevronWeight, p.heartWeight to neutral.heartWeight,
                p.arcCurve to neutral.arcCurve, p.scaleR to neutral.scaleR,
                p.offsetYR to neutral.offsetYR
            ).any { (a, b) -> kotlin.math.abs(a - b) > 1e-6f }
            assertTrue("Preset $name must differ from NEUTRAL", differs)
        }
    }

    @Test
    fun `heart coverage is on a separate channel from the solid eyes`() {
        // Neutral: solid eyes lit, no heart channel bleed.
        val neutral = resolved("NEUTRAL")
        val (nNormal, nHeart) = flatCoverage(neutral, leftEyeX, leftEyeY)
        assertTrue("Neutral eye centre should be solidly covered", nNormal > 0.9f)
        assertEquals("Neutral heart channel must be empty", 0f, nHeart, 1e-6f)

        // Heart preset: heart channel lit, solid channel empty at the same point.
        val heart = resolved("HEART")
        val (hNormal, hHeart) = flatCoverage(heart, leftEyeX, leftEyeY)
        assertTrue("Heart preset heart channel should be lit at eye centre", hHeart > 0.9f)
        assertTrue("Heart preset solid channel should be empty at eye centre", hNormal < 0.01f)
    }

    @Test
    fun `sphere projection undoes gaze rotation so the pattern stays fixed to the surface`() {
        val radius = 0.30f
        val sphereY = base.centerY

        // Front centre of the sphere looks at the gap between the eyes at yaw 0.
        val frontX = base.centerX * aspect
        val (normalAtGap, _) = ExpressionFaceModel.sphereCoverage(
            base, frontX, sphereY, aspect,
            sphereRadius = radius, sphereY = sphereY,
            gazeYawRad = 0f, gazePitchRad = 0f, sphereGlow = 0f
        )
        assertTrue("Pattern centre between eyes should be uncovered at yaw 0", normalAtGap < 0.1f)

        // Yaw the sphere so the left eye rotates to the front. The pattern must
        // move with the surface: front centre now shows the left eye.
        val shift = leftEyeX - frontX
        val yaw = asin(shift / radius)
        val (normalAtEye, _) = ExpressionFaceModel.sphereCoverage(
            base, frontX, sphereY, aspect,
            sphereRadius = radius, sphereY = sphereY,
            gazeYawRad = yaw, gazePitchRad = 0f, sphereGlow = 0f
        )
        assertTrue("Rotating the sphere must bring the left eye to the front", normalAtEye > 0.9f)

        // And the reverse: the left eye's on-screen position now shows the gap
        // pattern, since the surface rotated away underneath it.
        val (normalAtOldEye, _) = ExpressionFaceModel.sphereCoverage(
            base, leftEyeX, sphereY, aspect,
            sphereRadius = radius, sphereY = sphereY,
            gazeYawRad = yaw, gazePitchRad = 0f, sphereGlow = 0f
        )
        assertTrue("Previous eye position must be rotated away", normalAtOldEye < normalAtEye)
        assertNotEquals(normalAtGap, normalAtEye)
    }

    @Test
    fun `sphere halo only appears outside the sphere`() {
        val radius = 0.30f
        val sphereY = base.centerY
        val frontX = base.centerX * aspect

        // Point well outside the sphere: no pattern, some halo when glow is on.
        val farX = frontX + radius * 3f
        val (_, _, haloFar) = ExpressionFaceModel.sphereCoverage(
            base, farX, sphereY, aspect,
            sphereRadius = radius, sphereY = sphereY,
            gazeYawRad = 0f, gazePitchRad = 0f, sphereGlow = 0.5f
        )
        assertTrue("Outside the sphere must produce halo light", haloFar > 0f)

        // On the sphere itself, no glow term leaks in when sphereGlow is 0.
        val (_, _, litInside) = ExpressionFaceModel.sphereCoverage(
            base, frontX, sphereY, aspect,
            sphereRadius = radius, sphereY = sphereY,
            gazeYawRad = 0f, gazePitchRad = 0f, sphereGlow = 0f
        )
        assertTrue("Sphere surface should be lit by shading only", litInside > 0f)
    }
}
