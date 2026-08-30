package com.smolcase.companion.llm

import android.content.Context
import com.smolcase.companion.PersonalityDials
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gemini Nano on-device (Pixel 8 AICore) via ML Kit GenAI Prompt API
 * (genai-prompt 1.0.0-beta4). Private and offline — the default thinking layer.
 */
class GeminiNanoBackend(private val context: Context, private val dials: PersonalityDials) : LlmBackend {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val model = Generation.getClient()

    @Volatile private var ready = false
    @Volatile private var unavailable: String? = null

    init {
        // Probe availability at startup so the telemetry status line and
        // logcat carry the real reason, not "not ready yet" forever.
        scope.launch {
            try {
                val status = model.checkStatus()
                when (status) {
                    FeatureStatus.AVAILABLE -> ready = true
                    FeatureStatus.DOWNLOADABLE ->
                        unavailable = "Gemini Nano needs a one-time model download"
                    FeatureStatus.DOWNLOADING -> unavailable = "Gemini Nano is downloading"
                    else -> unavailable =
                        "Gemini Nano unavailable on this device (raw status=$status; " +
                        "no Google account signed in blocks AICore provisioning)"
                }
            } catch (e: Exception) {
                unavailable = e.message
            }
        }
    }

    override fun unavailableReason(): String? =
        if (ready) null else (unavailable ?: "Gemini Nano not ready yet")

    override fun reply(userText: String, soulContext: String, callback: (String?) -> Unit) {
        scope.launch {
            try {
                if (!ready) {
                    when (model.checkStatus()) {
                        FeatureStatus.AVAILABLE -> ready = true
                        FeatureStatus.DOWNLOADABLE -> {
                            unavailable = "Gemini Nano downloading (first run)"
                            // Download flow completes when the model is on-device
                            model.download().collect { /* progress ignored */ }
                            ready = model.checkStatus() == FeatureStatus.AVAILABLE
                            if (!ready) {
                                callback(null)
                                return@launch
                            }
                        }
                        FeatureStatus.DOWNLOADING -> {
                            unavailable = "Gemini Nano is downloading"
                            callback(null)
                            return@launch
                        }
                        else -> {
                            unavailable = "Gemini Nano unavailable on this device"
                            callback(null)
                            return@launch
                        }
                    }
                }

                val prompt = CreaturePersona.prompt(dials.humor, dials.honesty) +
                    "\n\n" + soulContext +
                    "\n\nThe person says: " + userText

                val response = model.generateContent(
                    generateContentRequest(TextPart(prompt)) {}
                )
                callback(response.candidates.firstOrNull()?.text?.trim()?.ifBlank { null })
            } catch (e: Exception) {
                unavailable = e.message
                callback(null)
            }
        }
    }
}
