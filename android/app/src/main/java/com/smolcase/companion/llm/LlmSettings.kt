package com.smolcase.companion.llm

import android.content.Context

/**
 * LLM configuration. Stored in SharedPreferences, edited in SettingsActivity.
 *
 * Backend order of preference is the user's choice:
 *  RULES — IntentRouter only, no LLM (default until configured)
 *  NANO  — Gemini Nano on-device (private, offline; Pixel 8 AICore)
 *  AGENT — OpenAI-compatible chat endpoint (OpenRouter, Together, etc.)
 *  GEMMA — Gemma 4 E2B on-device (sideloaded .litertlm)
 *
 * Backward-compatibility: old persisted KIMI backend is automatically
 * migrated to AGENT, and old kimi_* preference keys are migrated to
 * agent_* keys on first read.
 */
class LlmSettings(context: Context) {

    enum class Backend { RULES, NANO, AGENT, GEMMA }

    private val prefs = context.getSharedPreferences("smolcase_llm", Context.MODE_PRIVATE)

    /**
     * Migrate old persisted "KIMI" to "AGENT" transparently.
     */
    var backend: Backend
        get() {
            val raw = prefs.getString(KEY_BACKEND, Backend.AGENT.name)!!
            if (raw == "KIMI") {
                prefs.edit().putString(KEY_BACKEND, Backend.AGENT.name).apply()
                return Backend.AGENT
            }
            // If the user was on GEMMA, migrate them to AGENT — the on-device
            // Gemma 4 E2B engine is unstable on Tensor G3 (native crashes).
            if (raw == "GEMMA") {
                prefs.edit().putString(KEY_BACKEND, Backend.AGENT.name).apply()
                return Backend.AGENT
            }
            return try {
                Backend.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                Backend.RULES
            }
        }
        set(v) = prefs.edit().putString(KEY_BACKEND, v.name).apply()

    // ── Agent endpoint (OpenAI-compatible, replaces old Kimi backend) ──

    /**
     * Human-readable provider label (e.g. "OpenRouter", "Together").
     */
    var agentProviderLabel: String
        get() = prefs.getString(KEY_AGENT_PROVIDER_LABEL, DEFAULT_PROVIDER_LABEL)!!
        set(v) = prefs.edit().putString(KEY_AGENT_PROVIDER_LABEL, v.trim()).apply()

    /**
     * Base URL for the OpenAI-compatible chat completions endpoint.
     * Migrates from old kimi_base_url on first read.
     */
    var agentBaseUrl: String
        get() = prefs.getString(KEY_AGENT_BASE_URL, null)
            ?: prefs.getString(KEY_KIMI_BASE_URL, DEFAULT_BASE_URL)!!
            .also { prefs.edit().putString(KEY_AGENT_BASE_URL, it).apply() }
        set(v) = prefs.edit().putString(KEY_AGENT_BASE_URL, v.trim().trimEnd('/')).apply()

    /**
     * API key for the agent endpoint.
     * Migrates from old kimi_api_key on first read.
     */
    var agentApiKey: String
        get() = prefs.getString(KEY_AGENT_API_KEY, null)
            ?: prefs.getString(KEY_KIMI_API_KEY, "")!!
            .also { prefs.edit().putString(KEY_AGENT_API_KEY, it).apply() }
        set(v) = prefs.edit().putString(KEY_AGENT_API_KEY, v.trim()).apply()

    /**
     * Model name override for the agent endpoint.
     * Migrates from old kimi_model on first read.
     */
    var agentModel: String
        get() = prefs.getString(KEY_AGENT_MODEL, null)
            ?: prefs.getString(KEY_KIMI_MODEL, DEFAULT_MODEL)!!
            .also { prefs.edit().putString(KEY_AGENT_MODEL, it).apply() }
        set(v) = prefs.edit().putString(KEY_AGENT_MODEL, v.trim()).apply()

    val agentConfigured: Boolean
        get() = agentApiKey.isNotBlank()

    /** Temperature 0–200 (displayed as 0.00–2.00, default 0.80 → stored 80). */
    var agentTemperature: Int
        get() = prefs.getInt(KEY_AGENT_TEMPERATURE, DEFAULT_TEMPERATURE)
        set(v) = prefs.edit().putInt(KEY_AGENT_TEMPERATURE, v.coerceIn(0, 200)).apply()

    /** Max tokens 64–1024 (default 200). */
    var agentMaxTokens: Int
        get() = prefs.getInt(KEY_AGENT_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        set(v) = prefs.edit().putInt(KEY_AGENT_MAX_TOKENS, v.coerceIn(64, 1024)).apply()

    // ── Cloud TTS (ElevenLabs-style endpoint) ──

    var cloudTtsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_TTS_ENABLED, v).apply()

    /** Human-readable provider label (e.g. "ElevenLabs", "PlayHT"). */
    var cloudTtsProviderLabel: String
        get() = prefs.getString(KEY_TTS_PROVIDER, DEFAULT_TTS_PROVIDER)!!
        set(v) = prefs.edit().putString(KEY_TTS_PROVIDER, v.trim()).apply()

    var cloudTtsBaseUrl: String
        get() = prefs.getString(KEY_TTS_BASE_URL, DEFAULT_TTS_BASE_URL)!!
        set(v) = prefs.edit().putString(KEY_TTS_BASE_URL, v.trim().trimEnd('/')).apply()

    var cloudTtsApiKey: String
        get() = prefs.getString(KEY_TTS_API_KEY, "")!!
        set(v) = prefs.edit().putString(KEY_TTS_API_KEY, v.trim()).apply()

    var cloudTtsVoiceId: String
        get() = prefs.getString(KEY_TTS_VOICE_ID, DEFAULT_TTS_VOICE_ID)!!
        set(v) = prefs.edit().putString(KEY_TTS_VOICE_ID, v.trim()).apply()

    var cloudTtsModelId: String
        get() = prefs.getString(KEY_TTS_MODEL_ID, DEFAULT_TTS_MODEL_ID)!!
        set(v) = prefs.edit().putString(KEY_TTS_MODEL_ID, v.trim()).apply()

    /** Stability 0–100 (default 50). */
    var cloudTtsStability: Int
        get() = prefs.getInt(KEY_TTS_STABILITY, DEFAULT_TTS_STABILITY)
        set(v) = prefs.edit().putInt(KEY_TTS_STABILITY, v.coerceIn(0, 100)).apply()

    /** Similarity boost 0–100 (default 75). */
    var cloudTtsSimilarity: Int
        get() = prefs.getInt(KEY_TTS_SIMILARITY, DEFAULT_TTS_SIMILARITY)
        set(v) = prefs.edit().putInt(KEY_TTS_SIMILARITY, v.coerceIn(0, 100)).apply()

    // ── Cloud Vision (OpenAI-compatible vision endpoint) ──

    var cloudVisionEnabled: Boolean
        get() = prefs.getBoolean(KEY_VISION_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_VISION_ENABLED, v).apply()

    var cloudVisionBaseUrl: String
        get() = prefs.getString(KEY_VISION_BASE_URL, DEFAULT_VISION_BASE_URL)!!
        set(v) = prefs.edit().putString(KEY_VISION_BASE_URL, v.trim().trimEnd('/')).apply()

    var cloudVisionApiKey: String
        get() = prefs.getString(KEY_VISION_API_KEY, "")!!
        set(v) = prefs.edit().putString(KEY_VISION_API_KEY, v.trim()).apply()

    var cloudVisionModel: String
        get() = prefs.getString(KEY_VISION_MODEL, DEFAULT_VISION_MODEL)!!
        set(v) = prefs.edit().putString(KEY_VISION_MODEL, v.trim()).apply()

    val cloudVisionConfigured: Boolean
        get() = cloudVisionApiKey.isNotBlank()

    // ── Cloud AI Reply Generation (independent endpoint for reply() calls) ──

    var cloudReplyGenEnabled: Boolean
        get() = prefs.getBoolean(KEY_REPLY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_REPLY_ENABLED, v).apply()

    var cloudReplyGenBaseUrl: String
        get() = prefs.getString(KEY_REPLY_BASE_URL, DEFAULT_REPLY_BASE_URL)!!
        set(v) = prefs.edit().putString(KEY_REPLY_BASE_URL, v.trim().trimEnd('/')).apply()

    var cloudReplyGenApiKey: String
        get() = prefs.getString(KEY_REPLY_API_KEY, "")!!
        set(v) = prefs.edit().putString(KEY_REPLY_API_KEY, v.trim()).apply()

    var cloudReplyGenModel: String
        get() = prefs.getString(KEY_REPLY_MODEL, DEFAULT_REPLY_MODEL)!!
        set(v) = prefs.edit().putString(KEY_REPLY_MODEL, v.trim()).apply()

    val cloudReplyGenConfigured: Boolean
        get() = cloudReplyGenApiKey.isNotBlank()

    // ── Atomic commit (called by CreatureSettings) ──

    /**
     * Commit all settings atomically via Editor.commit().
     * This is the write path used by the Save button — the getter/setter
     * properties above continue to use apply() for individual edits.
     */
    fun commit(
        backend: Backend,
        providerLabel: String,
        agentBaseUrl: String, agentApiKey: String, agentModel: String,
        agentMaxTokens: Int, agentTemperature: Int,
        ttsEnabled: Boolean, ttsProvider: String, ttsBaseUrl: String, ttsApiKey: String,
        ttsVoiceId: String, ttsModelId: String, ttsStability: Int, ttsSimilarity: Int,
        visionEnabled: Boolean, visionBaseUrl: String, visionApiKey: String, visionModel: String,
        replyEnabled: Boolean, replyBaseUrl: String, replyApiKey: String, replyModel: String
    ) {
        prefs.edit()
            .putString(KEY_BACKEND, backend.name)
            .putString(KEY_AGENT_PROVIDER_LABEL, providerLabel.trim())
            .putString(KEY_AGENT_BASE_URL, agentBaseUrl.trim().trimEnd('/'))
            .putString(KEY_AGENT_API_KEY, agentApiKey.trim())
            .putString(KEY_AGENT_MODEL, agentModel.trim())
            .putInt(KEY_AGENT_MAX_TOKENS, agentMaxTokens.coerceIn(64, 1024))
            .putInt(KEY_AGENT_TEMPERATURE, agentTemperature.coerceIn(0, 200))
            .putBoolean(KEY_TTS_ENABLED, ttsEnabled)
            .putString(KEY_TTS_PROVIDER, ttsProvider.trim())
            .putString(KEY_TTS_BASE_URL, ttsBaseUrl.trim().trimEnd('/'))
            .putString(KEY_TTS_API_KEY, ttsApiKey.trim())
            .putString(KEY_TTS_VOICE_ID, ttsVoiceId.trim())
            .putString(KEY_TTS_MODEL_ID, ttsModelId.trim())
            .putInt(KEY_TTS_STABILITY, ttsStability.coerceIn(0, 100))
            .putInt(KEY_TTS_SIMILARITY, ttsSimilarity.coerceIn(0, 100))
            .putBoolean(KEY_VISION_ENABLED, visionEnabled)
            .putString(KEY_VISION_BASE_URL, visionBaseUrl.trim().trimEnd('/'))
            .putString(KEY_VISION_API_KEY, visionApiKey.trim())
            .putString(KEY_VISION_MODEL, visionModel.trim())
            .putBoolean(KEY_REPLY_ENABLED, replyEnabled)
            .putString(KEY_REPLY_BASE_URL, replyBaseUrl.trim().trimEnd('/'))
            .putString(KEY_REPLY_API_KEY, replyApiKey.trim())
            .putString(KEY_REPLY_MODEL, replyModel.trim())
            .commit()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_MODEL = "mistralai/mixtral-8x22b-instruct"
        const val DEFAULT_TEMPERATURE = 80   // displayed as 0.80
        const val DEFAULT_MAX_TOKENS = 200
        const val DEFAULT_PROVIDER_LABEL = "OpenRouter"

        const val DEFAULT_TTS_BASE_URL = "https://api.elevenlabs.io/v1"
        const val DEFAULT_TTS_VOICE_ID = "21m00Tcm4TlvDq8ikWAM"
        const val DEFAULT_TTS_MODEL_ID = "eleven_monolingual_v1"
        const val DEFAULT_TTS_STABILITY = 50
        const val DEFAULT_TTS_SIMILARITY = 75
        const val DEFAULT_TTS_PROVIDER = "ElevenLabs"

        const val DEFAULT_VISION_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_VISION_MODEL = "gpt-4o"

        const val DEFAULT_REPLY_BASE_URL = "https://openrouter.ai/api/v1"
        const val DEFAULT_REPLY_MODEL = "mistralai/mixtral-8x22b-instruct"

        private const val KEY_BACKEND = "backend"

        // Agent keys (new)
        private const val KEY_AGENT_PROVIDER_LABEL = "agent_provider_label"
        private const val KEY_AGENT_BASE_URL = "agent_base_url"
        private const val KEY_AGENT_API_KEY = "agent_api_key"
        private const val KEY_AGENT_MODEL = "agent_model"
        private const val KEY_AGENT_TEMPERATURE = "agent_temperature"
        private const val KEY_AGENT_MAX_TOKENS = "agent_max_tokens"

        // Old Kimi keys (for migration)
        private const val KEY_KIMI_BASE_URL = "kimi_base_url"
        private const val KEY_KIMI_API_KEY = "kimi_api_key"
        private const val KEY_KIMI_MODEL = "kimi_model"

        // Cloud TTS
        private const val KEY_TTS_ENABLED = "cloud_tts_enabled"
        private const val KEY_TTS_PROVIDER = "cloud_tts_provider_label"
        private const val KEY_TTS_BASE_URL = "cloud_tts_base_url"
        private const val KEY_TTS_API_KEY = "cloud_tts_api_key"
        private const val KEY_TTS_VOICE_ID = "cloud_tts_voice_id"
        private const val KEY_TTS_MODEL_ID = "cloud_tts_model_id"
        private const val KEY_TTS_STABILITY = "cloud_tts_stability"
        private const val KEY_TTS_SIMILARITY = "cloud_tts_similarity"

        // Cloud Vision
        private const val KEY_VISION_ENABLED = "cloud_vision_enabled"
        private const val KEY_VISION_BASE_URL = "cloud_vision_base_url"
        private const val KEY_VISION_API_KEY = "cloud_vision_api_key"
        private const val KEY_VISION_MODEL = "cloud_vision_model"

        // Cloud Reply Gen
        private const val KEY_REPLY_ENABLED = "cloud_reply_gen_enabled"
        private const val KEY_REPLY_BASE_URL = "cloud_reply_gen_base_url"
        private const val KEY_REPLY_API_KEY = "cloud_reply_gen_api_key"
        private const val KEY_REPLY_MODEL = "cloud_reply_gen_model"
    }
}