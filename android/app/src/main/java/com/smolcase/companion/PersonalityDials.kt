package com.smolcase.companion

import android.content.Context

/**
 * The creature's personality settings: HUMOR and HONESTY percentages,
 * TARS-style. Persisted; edited by voice (IntentRouter), sliders
 * (SettingsActivity), and injected into every LLM prompt.
 */
class PersonalityDials(context: Context) {

    private val prefs = context.getSharedPreferences("smolcase_dials", Context.MODE_PRIVATE)

    var humor: Int
        get() = prefs.getInt(KEY_HUMOR, DEFAULT_HUMOR)
        set(v) = prefs.edit().putInt(KEY_HUMOR, v.coerceIn(0, 100)).apply()

    var honesty: Int
        get() = prefs.getInt(KEY_HONESTY, DEFAULT_HONESTY)
        set(v) = prefs.edit().putInt(KEY_HONESTY, v.coerceIn(0, 100)).apply()

    /** Preferred TTS voice name (null = auto-pick a male voice). */
    var voiceName: String?
        get() = prefs.getString(KEY_VOICE_NAME, null)
        set(v) = prefs.edit().putString(KEY_VOICE_NAME, v).apply()

    /** Atomically commit humor + honesty via Editor.commit(). */
    fun commit(humor: Int, honesty: Int) {
        prefs.edit()
            .putInt(KEY_HUMOR, humor.coerceIn(0, 100))
            .putInt(KEY_HONESTY, honesty.coerceIn(0, 100))
            .commit()
    }

    companion object {
        const val DEFAULT_HUMOR = 75
        const val DEFAULT_HONESTY = 90
        private const val KEY_HUMOR = "humor"
        private const val KEY_HONESTY = "honesty"
        private const val KEY_VOICE_NAME = "voice_name"
    }
}
