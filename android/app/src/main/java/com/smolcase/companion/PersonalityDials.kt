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

    companion object {
        const val DEFAULT_HUMOR = 75
        const val DEFAULT_HONESTY = 90
        private const val KEY_HUMOR = "humor"
        private const val KEY_HONESTY = "honesty"
    }
}
