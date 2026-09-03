package com.smolcase.companion.cloud

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smolcase.companion.llm.LlmSettings
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Cloud TTS backend — fetches audio from OpenRouter TTS API and plays it.
 *
 * Uses the OpenRouter audio/speech endpoint (OpenAI-compatible) with the
 * Microsoft MAI-Voice-2-Flash model (en-US-Harper voice). Falls back
 * gracefully: returns false from speak() if network or decode fails, letting
 * the caller use local TTS instead.
 */
class CloudTtsBackend(private val context: Context, private val settings: LlmSettings) {

    companion object {
        private const val TAG = "CloudTTS"
        private const val OR_API_BASE = "https://openrouter.ai/api/v1"
        private const val MODEL = "microsoft/mai-voice-2-flash"
        private const val VOICE = "en-US-Harper:MAI-Voice-2"
        private const val RESPONSE_FORMAT = "mp3"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    /**
     * Attempt cloud TTS. Returns false if not configured, network fails, or
     * audio can't be played — caller falls back to local TTS.
     */
    fun speak(text: String, onResult: ((Boolean) -> Unit)? = null): Boolean {
        if (!settings.cloudTtsEnabled) return false

        val apiKey = settings.agentApiKey
        if (apiKey.isBlank()) {
            Log.w(TAG, "Cloud TTS disabled: no OpenRouter API key set")
            return false
        }

        thread {
            try {
                val url = URL("$OR_API_BASE/audio/speech")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                }

                val body = JSONObject()
                    .put("model", MODEL)
                    .put("input", text)
                    .put("voice", VOICE)
                    .put("response_format", RESPONSE_FORMAT)

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                    Log.w(TAG, "Cloud TTS HTTP $code: $err")
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
}