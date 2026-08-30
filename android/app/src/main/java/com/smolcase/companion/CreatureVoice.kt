package com.smolcase.companion

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.smolcase.companion.cloud.CloudTtsBackend
import com.smolcase.companion.llm.LlmSettings
import java.util.Locale

/**
 * The creature's voice (Stage 5, TARS persona).
 *
 * Text-to-speech tuned for TARS: low, measured, deadpan — a marine company
 * commander by way of a gym teacher, not a cute mascot. The glyph face
 * shimmers (column-scan) while it speaks.
 *
 * Phase 1: local TTS is always the primary path. Cloud TTS is a disabled-by-
 * default enhancement, not the hot-path fallback.
 */
class CreatureVoice(
    context: Context,
    private val dials: PersonalityDials,
    private val eyes: TarsFaceView,
    private val onTtsDone: (() -> Unit)? = null
) {
    private var ready = false
    private val cloudTts = CloudTtsBackend(context, LlmSettings(context))

    /** Cloud TTS enhancement — off by default (Phase 1). */
    var cloudTtsEnabled = false

    // One-time unmute of audio streams on construction — the old VoiceEars
    // could have left streams muted after a force-stop, and we no longer
    // toggle stream mute for audio routing.
    init {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
        audio.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
        audio.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
    }

    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            pickVoice()
            ready = true
        }
    }.apply {
        language = Locale.getDefault()
        setPitch(0.85f)        // TARS: low, measured
        setSpeechRate(0.92f)   // deadpan, unhurried
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String) {
                eyes.post { eyes.setSpeaking(true) }
            }

            override fun onDone(id: String) {
                eyes.post { eyes.setSpeaking(false) }
                onTtsDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String) {
                eyes.post { eyes.setSpeaking(false) }
                onTtsDone?.invoke()
            }
        })
    }

    /**
     * Speak [text] through the local TTS engine immediately.
     * Cloud TTS is no longer in the hot path — it runs as a low-priority
     * enhancement only when [cloudTtsEnabled] is true.
     */
    fun say(text: String, uid: Int = 0) {
        if (!ready) return
        val tag = if (uid > 0) "[#$uid]" else ""
        Log.i(TAG, "${tag}TTS start: $text")
        // Local TTS is always the primary path
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "smolcase-utterance")
        // Cloud TTS is a disabled-by-default enhancement
        if (cloudTtsEnabled) {
            cloudTts.speak(text) { _ -> }
        }
    }

    /**
     * Voice choice: the saved preference wins; otherwise prefer a known
     * male Google TTS voice (TARS is not a female-sounding robot), falling
     * back to the first local English voice.
     *
     * Local voices sort before network voices in every path.
     */
    private fun pickVoice() {
        val voices = tts.voices ?: return
        val saved = dials.voiceName
        val chosen = when {
            saved != null -> voices.firstOrNull { it.name == saved }
            else -> MALE_VOICE_IDS.firstNotNullOfOrNull { id ->
                voices.firstOrNull { it.name == id }
            }
        } ?: voices.filter { it.locale.language == "en" }
            .sortedBy { it.isNetworkConnectionRequired }
            .firstOrNull()
        chosen?.let { tts.voice = it }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        cloudTts.shutdown()
    }

    companion object {
        private const val TAG = "SmolcaseVoice"
        // Local variants first in each pair to prefer on-device voices
        private val MALE_VOICE_IDS = listOf(
            "en-us-x-iom-local", "en-us-x-iom-network",
            "en-us-x-tpd-local", "en-us-x-tpd-network",
            "en-gb-x-gbb-local", "en-gb-x-gbb-network"
        )
    }
}
