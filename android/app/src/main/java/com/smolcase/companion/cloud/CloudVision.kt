package com.smolcase.companion.cloud

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.smolcase.companion.llm.LlmSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Cloud Vision — captures a single CameraX frame, base64-encodes as JPEG,
 * sends to a configurable OpenAI-compatible vision endpoint, returns text.
 *
 * Rate-limited to one analysis per 10 seconds.
 */
class CloudVision(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val settings: LlmSettings
) {
    private val appContext = context.applicationContext
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @Volatile private var lastAnalysisMs = 0L

    /**
     * Capture one frame and send for vision analysis.
     * Rate-limited: max 1 per 10 seconds.
     */
    fun analyze(cameraProvider: ProcessCameraProvider, callback: (String?) -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisMs < 10_000) {
            mainHandler.post { callback(null) }
            return
        }
        if (!settings.cloudVisionEnabled || !settings.cloudVisionConfigured) {
            mainHandler.post { callback(null) }
            return
        }
        lastAnalysisMs = now

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture
        )

        val photoFile = File(appContext.cacheDir, "vision_${System.nanoTime()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cameraProvider.unbind(imageCapture)
                    processFrame(photoFile, callback)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "ImageCapture failed", exception)
                    photoFile.delete()
                    cameraProvider.unbind(imageCapture)
                    mainHandler.post { callback(null) }
                }
            }
        )
    }

    private fun processFrame(photoFile: File, callback: (String?) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val jpegBytes = photoFile.readBytes()
                photoFile.delete()
                if (jpegBytes.isEmpty()) {
                    mainHandler.post { callback(null) }
                    return@launch
                }
                val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                val description = sendVisionRequest(base64)
                mainHandler.post { callback(description) }
            } catch (e: Exception) {
                Log.w(TAG, "Vision processing failed", e)
                photoFile.delete()
                mainHandler.post { callback(null) }
            }
        }
    }

    private fun sendVisionRequest(base64Image: String): String? {
        try {
            val url = URL(settings.cloudVisionBaseUrl + "/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${settings.cloudVisionApiKey}")
                doOutput = true
            }

            val body = JSONObject().apply {
                put("model", settings.cloudVisionModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", "Describe what you see in detail")
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        })
                    })
                })
                put("max_tokens", 300)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val code = conn.responseCode
            if (code !in 200..299) return null

            val responseText = conn.inputStream.bufferedReader().readText()
            return JSONObject(responseText)
                .getJSONArray("choices")
                .optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Vision request failed", e)
            return null
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CloudVision"
    }
}