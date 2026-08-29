package com.smolcase.companion

import android.content.Context
import android.content.SharedPreferences
import com.smolcase.companion.llm.LlmSettings
import com.smolcase.companion.sensors.SensorConfig

/**
 * Creature settings aggregate — coordinates atomic save across all
 * settings subsystems and adds the cloud TTS provider label (FR-3).
 *
 * The Save button in SettingsActivity calls save() which commits all
 * pending writes synchronously via Editor.commit() (atomic within each
 * prefs file; cross-file atomicity is best-effort on Android).
 */
class CreatureSettings(context: Context) {

    val llm = LlmSettings(context)
    val dials = PersonalityDials(context)
    val sensors = SensorConfig(context)

    private val ttsPrefs = context.getSharedPreferences("smolcase_llm", Context.MODE_PRIVATE)

    /** Cloud TTS provider label (FR-3: "default 'ElevenLabs'"). */
    var cloudTtsProviderLabel: String
        get() = ttsPrefs.getString(KEY_TTS_PROVIDER, DEFAULT_TTS_PROVIDER)!!
        set(v) = ttsPrefs.edit().putString(KEY_TTS_PROVIDER, v.trim()).apply()

    /**
     * Atomically commit all pending changes across settings subsystems.
     * Uses Editor.commit() (synchronous, atomic per file) instead of
     * apply() (async, batched).
     */
    fun saveAll(
        backend: LlmSettings.Backend,
        agentBaseUrl: String, agentApiKey: String, agentModel: String,
        agentMaxTokens: Int, agentTemperature: Int,
        humor: Int, honesty: Int,
        ttsEnabled: Boolean, ttsProvider: String, ttsBaseUrl: String, ttsApiKey: String,
        ttsVoiceId: String, ttsModelId: String, ttsStability: Int, ttsSimilarity: Int,
        visionEnabled: Boolean, visionBaseUrl: String, visionApiKey: String, visionModel: String,
        replyEnabled: Boolean, replyBaseUrl: String, replyApiKey: String, replyModel: String
    ) {
        // LlmSettings — atomic commit
        llm.commit(
            backend = backend,
            providerLabel = cloudTtsProviderLabel,
            agentBaseUrl = agentBaseUrl, agentApiKey = agentApiKey, agentModel = agentModel,
            agentMaxTokens = agentMaxTokens, agentTemperature = agentTemperature,
            ttsEnabled = ttsEnabled, ttsProvider = ttsProvider, ttsBaseUrl = ttsBaseUrl, ttsApiKey = ttsApiKey,
            ttsVoiceId = ttsVoiceId, ttsModelId = ttsModelId,
            ttsStability = ttsStability, ttsSimilarity = ttsSimilarity,
            visionEnabled = visionEnabled, visionBaseUrl = visionBaseUrl,
            visionApiKey = visionApiKey, visionModel = visionModel,
            replyEnabled = replyEnabled, replyBaseUrl = replyBaseUrl,
            replyApiKey = replyApiKey, replyModel = replyModel
        )

        // PersonalityDials
        dials.commit(humor, honesty)

        // Sensors — already persisted immediately via property setters
    }

    companion object {
        const val DEFAULT_TTS_PROVIDER = "ElevenLabs"
        private const val KEY_TTS_PROVIDER = "cloud_tts_provider_label"
    }
}