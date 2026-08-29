package com.smolcase.companion

import android.content.Context
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
 */
class CreatureVoice(
    context: Context,
    private val dials: PersonalityDials,
    private val eyes: TarsFaceView,
    private val allowSpeech: (Boolean) -> Unit,
    private val onTtsDone: (() -> Unit)? = null
) {
    private var ready = false
    private val cloudTts = CloudTtsBackend(context, LlmSettings(context))

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
                allowSpeech(false)
                eyes.post { eyes.setSpeaking(false) }
                onTtsDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String) {
                allowSpeech(false)
                eyes.post { eyes.setSpeaking(false) }
                onTtsDone?.invoke()
            }
        })
    }

    fun say(text: String, uid: Int = 0) {
        if (!ready) return
        allowSpeech(true)
        val tag = if (uid > 0) "[#$uid]" else ""
        Log.i(TAG, "${tag}TTS start: $text")
        val tts = this.tts
        val spoken = cloudTts.speak(text) { success ->
            if (!success) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "smolcase-utterance")
            } else {
                Log.i(TAG, "${tag}TTS cloud done")
                allowSpeech(false)
                eyes.post { eyes.setSpeaking(false) }
                onTtsDone?.invoke()
            }
        }
        if (!spoken) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "smolcase-utterance")
        }
    }

    /**
     * Voice choice: the saved preference wins; otherwise prefer a known
     * male Google TTS voice (TARS is not a female-sounding robot), falling
     * back to the first local English voice.
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
        // Google TTS male English voice IDs, most-preferred first
        private val MALE_VOICE_IDS = listOf(
            "en-us-x-iom-local", "en-us-x-iom-network",
            "en-us-x-tpd-local", "en-us-x-tpd-network",
            "en-gb-x-gbb-local", "en-gb-x-gbb-network"
        )
    }
}
