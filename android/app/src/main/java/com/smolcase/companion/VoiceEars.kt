package com.smolcase.companion

import android.content.Context
import android.content.Intent
import android.media.AudioManager
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
 *
 * Suppresses Android SpeechRecognizer system sounds by temporarily setting
 * notification/system stream volume to 0 while listening. Uses `setStreamVolume`
 * (level 0), not ADJUST_MUTE — the old method caused audio popping.
 */
class VoiceEars(
    private val context: Context,
    private val onHeard: (String) -> Unit,
    private val onMuteChanged: (Boolean) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var savedNotificationVolume = 0
    private var savedSystemVolume = 0

    // Post-TTS cooldown: ignore speech for 2.5s so we don't hear our own reply
    @Volatile private var cooldownUntilMs = 0L

    var muted = false
        private set

    private fun suppressRecognizerSounds() {
        savedNotificationVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        savedSystemVolume = audio.getStreamVolume(AudioManager.STREAM_SYSTEM)
        if (savedNotificationVolume > 0) {
            audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            Log.v(TAG, "suppressed notification volume ($savedNotificationVolume → 0)")
        }
        if (savedSystemVolume > 0) {
            audio.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            Log.v(TAG, "suppressed system volume ($savedSystemVolume → 0)")
        }
    }

    private fun restoreRecognizerSounds() {
        if (savedNotificationVolume > 0) {
            audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotificationVolume, 0)
            Log.v(TAG, "restored notification volume → $savedNotificationVolume")
        }
        if (savedSystemVolume > 0) {
            audio.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystemVolume, 0)
            Log.v(TAG, "restored system volume → $savedSystemVolume")
        }
        savedNotificationVolume = 0
        savedSystemVolume = 0
    }

    fun start() {
        if (running) return
        running = true
        Log.i(TAG, "🎧 listening started")
        suppressRecognizerSounds()
        listen()
    }

    fun toggleMute() {
        muted = !muted
        Log.i(TAG, if (muted) "🔇 do not listen — recognizer stopped" else "🎧 listening resumed")
        if (muted) {
            recognizer?.stopListening()
            restoreRecognizerSounds()
        } else {
            suppressRecognizerSounds()
            listen()
        }
        onMuteChanged(muted)
    }

    /** Start a 2.5s cooldown after TTS finishes to avoid hearing our own reply. */
    fun cooldown() {
        cooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
    }

    fun stop() {
        running = false
        restoreRecognizerSounds()
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        Log.i(TAG, "🎧 listening stopped")
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
            // Require 2s of silence before considering speech input complete.
            // This reduces premature finalisation from road noise / ambient sounds.
            putExtra(
                "android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS",
                2000L
            )
            // Prefer on-device recognition to avoid network latency on repeat listens
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        handler.postDelayed({ listen() }, RESTART_DELAY_MS)
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
            val errName = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "BUSY"
                SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
                SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
                SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
                SpeechRecognizer.ERROR_SERVER -> "SERVER"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "NO_PERMISSION"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "TOO_MANY"
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "LANG_UNSUPPORTED"
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "LANG_UNAVAILABLE"
                else -> "UNKNOWN($error)"
            }
            Log.i(TAG, "🎤 recognizer error: $errName — restarting")
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
        private const val RESTART_DELAY_MS = 800L
    }
}