package com.smolcase.companion.face

import android.content.res.AssetManager
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.pow

/** Native execution of the spike's source and dot shaders. All GL state stays on the GL thread. */
class FaceGpuRenderer(
    private val assets: AssetManager,
    initialFrame: FaceFrame,
    private val onFrameDrawn: ((Int, Int) -> Unit)? = null
) : GLSurfaceView.Renderer {
    @Volatile var frame: FaceFrame = initialFrame
    private lateinit var source: Program
    private lateinit var dots: Program
    private var framebuffer = 0
    private var texture = 0
    private var width = 0
    private var height = 0
    private val emitter = linearColour(0x8de8ff)
    private val sphere = linearColour(0x2b4354)
    private val heart = linearColour(0xff3355)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Surface recreation means the previous context and its objects are gone.
        framebuffer = 0
        texture = 0
        source = Program(asset("fullscreen.vert"), asset("source.frag"))
        dots = Program(asset("fullscreen.vert"), asset("dots.frag"))
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glDisable(GL_DITHER)
        glClearColor(0f, 0f, 0f, 1f)
        Log.i(TAG, "GLES ${glGetString(GL_VERSION)}; source + dot programs linked")
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        if (texture != 0) glDeleteTextures(1, intArrayOf(texture), 0)
        if (framebuffer != 0) glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        val ids = IntArray(1)
        glGenTextures(1, ids, 0)
        texture = ids[0]
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        // Matches Three.js WebGLRenderTarget({colorSpace: SRGBColorSpace}).
        // The FBO encodes linear RGB on write; texture sampling decodes it.
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_SRGB8_ALPHA8, width, height)
        glGenFramebuffers(1, ids, 0)
        framebuffer = ids[0]
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0)
        check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) { "Face FBO incomplete" }
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        checkGl("resize")
        Log.i(TAG, "Framebuffer ${width}x$height SRGB8_ALPHA8")
    }

    override fun onDrawFrame(unused: GL10?) {
        if (width == 0 || height == 0) return
        val snapshot = frame
        glViewport(0, 0, width, height)
        glBindFramebuffer(GL_FRAMEBUFFER, if (snapshot.parameters.showSource) 0 else framebuffer)
        glClear(GL_COLOR_BUFFER_BIT)
        source.use()
        setSourceUniforms(snapshot)
        glDrawArrays(GL_TRIANGLES, 0, 3)
        if (!snapshot.parameters.showSource) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glClear(GL_COLOR_BUFFER_BIT)
            dots.use()
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, texture)
            glUniform1i(dots.location("tSource"), 0)
            setDotUniforms(snapshot.parameters.optics)
            glDrawArrays(GL_TRIANGLES, 0, 3)
        }
        checkGl("draw")
        onFrameDrawn?.invoke(width, height)
    }

    private fun setSourceUniforms(frame: FaceFrame) {
        val p = frame.eyes
        val aspect = width.toFloat() / height
        val cx = p.centerX * aspect + frame.saccadeX
        val cy = p.centerY + frame.saccadeY
        source.vec2("uResolution", width.toFloat(), height.toFloat())
        source.vec4("uEyeA", cx - p.gap / 2f, cy + p.offsetYL, max(p.eyeW * p.scaleL, 0.001f), max(p.eyeH * p.scaleL, 0.001f))
        source.vec4("uEyeB", cx + p.gap / 2f, cy + p.offsetYR, max(p.eyeW * p.scaleR, 0.001f), max(p.eyeH * p.scaleR, 0.001f))
        source.vec4("uLidA", p.topLid, p.botLid, p.lidCurve, -1f)
        source.vec4("uLidB", p.topLid, p.botLid, p.lidCurve, 1f)
        source.scalar("uSlant", p.slant)
        source.scalar("uLidSlant", p.lidSlant)
        source.scalar("uSoft", p.softness)
        source.vec3("uColor", emitter, p.brightness)
        source.vec3("uShape", floatArrayOf(p.archWeight, p.chevronWeight, p.heartWeight))
        source.vec2("uArc", p.arcCurve, p.stroke)
        source.scalar("uTopSlope", p.topSlope)
        source.vec2("uPivot", cx, cy)
        source.scalar("uRoll", Math.toRadians(p.roll.toDouble()).toFloat())
        source.scalar("uBlink", frame.blink)
        val settings = frame.parameters.sphere
        source.scalar("uSphereMode", if (settings.enabled) 1f else 0f)
        source.vec2("uSphereCenter", aspect / 2f, settings.centerY)
        source.scalar("uSphereRadius", settings.radius)
        source.vec2("uGaze", frame.yawRadians, frame.pitchRadians)
        source.vec3("uSphereColor", sphere)
        source.vec3("uHeartColor", heart)
        source.vec3("uLightTint", emitter)
        source.scalar("uSphereGlow", settings.glow)
    }

    private fun setDotUniforms(p: EmitterParameters) {
        dots.vec2("uResolution", width.toFloat(), height.toFloat())
        dots.vec2("uGrid", p.cols.toFloat(), p.rows.toFloat())
        dots.scalar("uDotRadius", p.radius)
        dots.scalar("uDotSoftness", p.softness)
        dots.scalar("uCellFill", p.cellFill)
        dots.scalar("uGlow", p.glow)
        dots.scalar("uGlowSpread", p.glowSpread)
        dots.scalar("uHalation", p.halation)
        dots.scalar("uGhostFloor", p.ghostFloor)
        dots.scalar("uExposure", p.exposure)
        dots.scalar("uSourceTint", p.sourceTint)
        dots.vec3("uDotColor", emitter)
    }

    private fun asset(name: String) = assets.open("face/$name").bufferedReader().use { it.readText() }

    private class Program(vertex: String, fragment: String) {
        private val id: Int
        private val locations = mutableMapOf<String, Int>()
        init {
            val vs = compile(GL_VERTEX_SHADER, vertex)
            val fs = compile(GL_FRAGMENT_SHADER, fragment)
            id = glCreateProgram()
            glAttachShader(id, vs)
            glAttachShader(id, fs)
            glLinkProgram(id)
            glDeleteShader(vs)
            glDeleteShader(fs)
            val status = IntArray(1)
            glGetProgramiv(id, GL_LINK_STATUS, status, 0)
            check(status[0] == GL_TRUE) { "Face program link failed: ${glGetProgramInfoLog(id)}" }
        }
        fun use() = glUseProgram(id)
        fun location(name: String): Int = locations.getOrPut(name) {
            glGetUniformLocation(id, name).also { check(it >= 0) { "Missing face uniform $name" } }
        }
        fun scalar(name: String, x: Float) = glUniform1f(location(name), x)
        fun vec2(name: String, x: Float, y: Float) = glUniform2f(location(name), x, y)
        fun vec3(name: String, rgb: FloatArray, scale: Float = 1f) =
            glUniform3f(location(name), rgb[0] * scale, rgb[1] * scale, rgb[2] * scale)
        fun vec4(name: String, x: Float, y: Float, z: Float, w: Float) = glUniform4f(location(name), x, y, z, w)
    }

    companion object {
        private const val TAG = "FaceGpu"
        private fun compile(type: Int, source: String): Int {
            val id = glCreateShader(type)
            glShaderSource(id, source)
            glCompileShader(id)
            val status = IntArray(1)
            glGetShaderiv(id, GL_COMPILE_STATUS, status, 0)
            check(status[0] == GL_TRUE) { "Face shader compile failed: ${glGetShaderInfoLog(id)}" }
            return id
        }
        private fun checkGl(stage: String) {
            val error = glGetError()
            check(error == GL_NO_ERROR) { "Face GL error 0x${error.toString(16)} at $stage" }
        }
        // Three.Color(hex) converts sRGB palette constants into linear working RGB.
        private fun linearColour(hex: Int) = floatArrayOf(
            ((hex shr 16) and 255) / 255f, ((hex shr 8) and 255) / 255f, (hex and 255) / 255f
        ).map { if (it < 0.04045f) it / 12.92f else ((it + 0.055f) / 1.055f).pow(2.4f) }.toFloatArray()
    }
}
