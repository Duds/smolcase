package com.smolcase.companion.cloud

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smolcase.companion.llm.LlmSettings
import kotlin.concurrent.thread

/**
 * Cloud TTS backend — fetches audio from a configurable endpoint and plays it.
 *
 * Default: ElevenLabs-compatible API. Configurable base URL, key, and voice ID.
 * Falls back gracefully: returns false from speak() if network or decode fails,
 * letting the caller use local TTS instead.
 */
class CloudTtsBackend(private val context: Context, private val settings: LlmSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    /**
     * Attempt cloud TTS. Returns false if not configured, network fails, or
     * audio can't be played — caller falls back to local TTS.
     */
    fun speak(text: String, onResult: ((Boolean) -> Unit)? = null): Boolean {
        if (!settings.cloudTtsEnabled) return false
        if (settings.cloudTtsApiKey.isBlank()) return false

        thread {
            try {
                val url = java.net.URL(settings.cloudTtsBaseUrl + "/text-to-speech/" + settings.cloudTtsVoiceId)
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("xi-api-key", settings.cloudTtsApiKey)
                    doOutput = true
                }

                val body = org.json.JSONObject().apply {
                    put("text", text)
                    put("model_id", settings.cloudTtsModelId)
                    put("voice_settings", org.json.JSONObject().apply {
                        put("stability", settings.cloudTtsStability / 100.0)
                        put("similarity_boost", settings.cloudTtsSimilarity / 100.0)
                    })
                }
                java.io.OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "Cloud TTS HTTP $code")
                    mainHandler.post { onResult?.invoke(false) }
                    return@thread
                }

                val audioBytes = conn.inputStream.readBytes()
                playAudio(audioBytes, onResult)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud TTS failed", e)
                mainHandler.post { onResult?.invoke(false) }
            }
        }
        return true
    }

    private fun playAudio(audioBytes: ByteArray, onResult: ((Boolean) -> Unit)?) {
        mainHandler.post {
            try {
                player?.release()
                val tempFile = java.io.File(context.cacheDir, "cloud_tts_${System.nanoTime()}.mp3")
                tempFile.outputStream().use { it.write(audioBytes) }
                player = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setOnCompletionListener {
                        tempFile.delete()
                        onResult?.invoke(true)
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w(TAG, "MediaPlayer error: $what $extra")
                        tempFile.delete()
                        onResult?.invoke(false)
                        true
                    }
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud TTS playback failed", e)
                onResult?.invoke(false)
            }
        }
    }

    fun shutdown() {
        player?.release()
        player = null
    }

    companion object {
        private const val TAG = "CloudTTS"
    }
}