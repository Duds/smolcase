package com.smolcase.companion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * The creature's ears (Stage 3).
 *
 * Continuous on-device speech recognition (Pixel 8 runs this on-device).
 * Listens in a restart loop; final results go to the intent router.
 * Tap the creature to mute/unmute — the work-desk privacy switch.
 */
class VoiceEars(
    private val context: Context,
    private val onHeard: (String) -> Unit,
    private val onMuteChanged: (Boolean) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var running = false

    // Post-TTS cooldown: ignore speech for 2s so we don't hear our own reply
    @Volatile private var cooldownUntilMs = 0L

    var muted = false
        private set

    fun start() {
        if (running) return
        running = true
        listen()
    }

    fun toggleMute() {
        muted = !muted
        if (muted) {
            recognizer?.stopListening()
        } else {
            listen()
        }
        onMuteChanged(muted)
    }

    /** Start a 2s cooldown after TTS finishes to avoid hearing our own reply. */
    fun cooldown() {
        cooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun listen() {
        if (!running || muted) return
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        handler.postDelayed({ listen() }, 800)
    }

    private val listener = object : RecognitionListener {
        private var hearingStartMs = 0L

        override fun onBeginningOfSpeech() {
            hearingStartMs = System.currentTimeMillis()
            Log.i(TAG, "🎤 hearing started")
        }

        override fun onResults(results: Bundle) {
            val text = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            val elapsed = System.currentTimeMillis() - hearingStartMs
            if (!text.isNullOrBlank()) {
                // Skip if still in post-TTS cooldown
                if (System.currentTimeMillis() < cooldownUntilMs) {
                    Log.i(TAG, "📝 ignored (cooldown): $text")
                } else {
                    Log.i(TAG, "📝 heard ($elapsed ms): $text")
                    onHeard(text)
                }
            } else {
                Log.w(TAG, "📝 heard blank after $elapsed ms")
            }
            scheduleRestart()
        }

        override fun onError(error: Int) {
            // NO_MATCH / SPEECH_TIMEOUT are the normal "nobody talking" path —
            // just keep listening. Anything else also retries, gently.
            scheduleRestart()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        private const val TAG = "SmolcaseEars"
        private const val COOLDOWN_MS = 2500L
    }
}
