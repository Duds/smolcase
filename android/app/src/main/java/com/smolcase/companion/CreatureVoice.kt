package com.smolcase.companion

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private val eyes: TarsFaceView,
    private val allowSpeech: (Boolean) -> Unit
) {
    private var ready = false

    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
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
                allowSpeech(false) // re-mute the recognizer beep streams
                eyes.post { eyes.setSpeaking(false) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String) {
                allowSpeech(false)
                eyes.post { eyes.setSpeaking(false) }
            }
        })
    }

    fun say(text: String) {
        if (!ready) return
        allowSpeech(true) // briefly unmute so the reply is audible
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "smolcase-utterance")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
