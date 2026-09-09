package com.smolcase.companion.matrix

/**
 * Rasterizes [ExpressionFaceModel] faces onto an [ApplianceMatrixCanvas].
 *
 * Coordinate conversion: the canvas is (nx, ny) in 0..1 with ny top-down;
 * the face model is height-normalised with y bottom-to-top and x in
 * 0..aspect (aspect = width / height). Conversion happens at the call site:
 * x = nx * aspect, y = 1 - ny.
 *
 * Two channels are produced: the normal eye coverage lands in the canvas
 * buffer, and the heart coverage (heart-eye preset) lands in [heartBuffer]
 * so the view can colour it apart from the eyes.
 */
data class DotMatrixParams(
    val dotRadius: Float = 0.36f,
    val dotSoftness: Float = 0.30f,
    val cellFill: Float = 0.82f,
    val glow: Float = 1.51f,
    val glowSpread: Float = 0.50f,
    val halation: Float = 0.60f,
    val ghostFloor: Float = 0.05f,
    val exposure: Float = 4f,
    val sourceBrightness: Float = 0.20f,
    val sourceTint: Float = 0.80f
)

class CozmoEyeRasterizer(
    private val canvas: ApplianceMatrixCanvas,
    private val dotParams: DotMatrixParams = DotMatrixParams()
) {
    /** Sphere substrate lighting, separate from eye emitter energy. */
    val sphereBuffer = FloatArray(canvas.totalDots)

    /** Parallel channel for heart-mask coverage (heart eyes preset). */
    val heartBuffer = FloatArray(canvas.totalDots)

    val aspect: Float = canvas.cols.toFloat() / canvas.rows.toFloat()

    /**
     * Clear both the canvas and the heart channel.
     */
    fun clear() {
        canvas.clear()
        sphereBuffer.fill(0f)
        heartBuffer.fill(0f)
    }

    private fun setChannels(col: Int, row: Int, normal: Float, heart: Float) {
        val idx = row * canvas.cols + col
        if (normal > 0.001f) {
            val blended = (canvas.buffer[idx] + normal).coerceIn(0f, 1f)
            canvas.buffer[idx] = blended
        }
        if (heart > 0.001f) {
            heartBuffer[idx] = (heartBuffer[idx] + heart).coerceIn(0f, 1f)
        }
    }

    /**
     * Applies the spike's five-tap cell average and exposure. The source
     * shader uses cyan at brightness 0.2 and red hearts at full strength.
     * The returned values are emitter energy, not raw SDF coverage.
     */
    private fun quantize(normal: Float, heart: Float, sphereLight: Float): Pair<Float, Float> {
        val normalSource = normal * dotParams.sourceBrightness
        val heartSource = heart
        val normalEnergy = (normalSource * dotParams.exposure).coerceIn(0f, 1f)
        val heartEnergy = (heartSource * dotParams.exposure).coerceIn(0f, 1f)
        val lightEnergy = (sphereLight * dotParams.exposure).coerceIn(0f, 1f)
        return Pair(
            (normalEnergy + lightEnergy * 0.18f).coerceIn(0f, 1f),
            heartEnergy
        )
    }

    private fun fiveTapSphere(
        params: ExpressionEyeParams,
        x: Float,
        y: Float,
        cellW: Float,
        cellH: Float,
        sphereRadius: Float,
        sphereY: Float,
        gazeYawRad: Float,
        gazePitchRad: Float,
        sphereGlow: Float,
        screenAspect: Float
    ): Triple<Float, Float, Float> {
        val taps = arrayOf(
            Pair(0f, 0f), Pair(cellW * 0.25f, cellH * 0.25f),
            Pair(-cellW * 0.25f, cellH * 0.25f), Pair(cellW * 0.25f, -cellH * 0.25f),
            Pair(-cellW * 0.25f, -cellH * 0.25f)
        )
        var normal = 0f
        var heart = 0f
        var light = 0f
        for ((dx, dy) in taps) {
            val sample = ExpressionFaceModel.sphereCoverage(
                params, x + dx, y + dy, screenAspect,
                sphereRadius = sphereRadius, sphereY = sphereY,
                gazeYawRad = gazeYawRad, gazePitchRad = gazePitchRad,
                sphereGlow = sphereGlow
            )
            normal += sample.first
            heart += sample.second
            light += sample.third
        }
        val lightSource = (light / 5f).coerceIn(0f, 1f)
        val energy = quantize(normal / 5f, heart / 5f, lightSource)
        return Triple(energy.first, energy.second, lightSource)
    }

    /**
     * Rasterizes the flat face pattern (both eyes) onto the matrix.
     */
    fun rasterizeFace(
        params: ExpressionEyeParams,
        screenAspect: Float = aspect
    ) {
        val cols = canvas.cols
        val rows = canvas.rows

        for (r in 0 until rows) {
            val y = 1f - (r + 0.5f) / rows.toFloat()
            for (c in 0 until cols) {
                val x = (c + 0.5f) / cols.toFloat() * screenAspect
                val (normal, heart) = ExpressionFaceModel.coverage(params, x, y, screenAspect)
                val energy = quantize(normal, heart, 0f)
                setChannels(c, r, energy.first, energy.second)
            }
        }
    }

    /**
     * Rasterizes the face pattern mapped onto a sphere. Projects each dot
     * onto the sphere surface, undoes the gaze rotation so the pattern stays
     * fixed to the surface, and adds the sphere's own shading/rim/halo light.
     */
    fun rasterizeSphereFace(
        params: ExpressionEyeParams,
        sphereRadius: Float,
        sphereY: Float,
        gazeYawRad: Float = 0f,
        gazePitchRad: Float = 0f,
        sphereGlow: Float = 0.25f,
        screenAspect: Float = aspect
    ) {
        val cols = canvas.cols
        val rows = canvas.rows

        for (r in 0 until rows) {
            val y = 1f - (r + 0.5f) / rows.toFloat()
            for (c in 0 until cols) {
                val x = (c + 0.5f) / cols.toFloat() * screenAspect
                // Five taps match the shared WebGL dot-matrix pass.
                val energy = fiveTapSphere(
                    params, x, y,
                    cellW = screenAspect / cols,
                    cellH = 1f / rows,
                    sphereRadius, sphereY, gazeYawRad, gazePitchRad,
                    sphereGlow, screenAspect
                )
                val idx = r * cols + c
                sphereBuffer[idx] = energy.third
                setChannels(c, r, energy.first, energy.second)
            }
        }
    }
}
