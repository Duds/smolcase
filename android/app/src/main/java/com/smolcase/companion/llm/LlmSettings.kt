package com.smolcase.companion.llm

import android.content.Context

/**
 * LLM configuration. Stored in SharedPreferences, edited in SettingsActivity.
 *
 * Backend order of preference is the user's choice:
 *  RULES — IntentRouter only, no LLM (default until configured)
 *  NANO  — Gemini Nano on-device (private, offline; Pixel 8 AICore)
 *  KIMI  — OpenAI-compatible chat endpoint (default: Moonshot public API)
 */
class LlmSettings(context: Context) {

    enum class Backend { RULES, NANO, KIMI, GEMMA }

    private val prefs = context.getSharedPreferences("smolcase_llm", Context.MODE_PRIVATE)

    var backend: Backend
        get() = Backend.valueOf(prefs.getString(KEY_BACKEND, Backend.RULES.name)!!)
        set(v) = prefs.edit().putString(KEY_BACKEND, v.name).apply()

    var kimiBaseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)!!
        set(v) = prefs.edit().putString(KEY_BASE_URL, v.trim().trimEnd('/')).apply()

    var kimiApiKey: String
        get() = prefs.getString(KEY_API_KEY, "")!!
        set(v) = prefs.edit().putString(KEY_API_KEY, v.trim()).apply()

    var kimiModel: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL)!!
        set(v) = prefs.edit().putString(KEY_MODEL, v.trim()).apply()

    val kimiConfigured: Boolean
        get() = kimiApiKey.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.moonshot.ai/v1"
        const val DEFAULT_MODEL = "kimi-k2"

        private const val KEY_BACKEND = "backend"
        private const val KEY_BASE_URL = "kimi_base_url"
        private const val KEY_API_KEY = "kimi_api_key"
        private const val KEY_MODEL = "kimi_model"
    }
}
