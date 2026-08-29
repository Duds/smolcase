package com.smolcase.companion

import android.content.Context
import android.util.Log
import com.smolcase.companion.BuildConfig
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Persistent conversation log — survives rebuilds.
 *
 * Writes one JSONL entry per line to [LOG_DIR]/conversations.jsonl.
 * Each entry carries ISO-8601 timestamp, utterance direction, text,
 * latency (when available), session ID, and build info.
 *
 * JSONL is append-only: open, write one line, close. No locks needed
 * because Android's single-activity model serialises all calls.
 */
class ConversationLog(private val context: Context) {

    private val logFile = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        .resolve(LOG_FILENAME)

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Incremented each app launch. Persisted so the creature tracks
     *  conversation sessions across lives. */
    var sessionId: Int
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SESSION, 0)
        private set(v) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SESSION, v).apply()

    /** Call once at startup — increments the session counter. */
    fun startSession() {
        sessionId = sessionId + 1
        // Log the session start as its own entry
        append(
            "___session_start",
            "session ${sessionId} (build ${BuildConfig.VERSION_CODE} - ${BuildConfig.VERSION_NAME})",
            0
        )
    }

    /** Log a "heard" utterance. */
    fun heard(text: String) {
        append("heard", text, 0)
    }

    /** Log a "replied" utterance with [latencyMs] from heard to reply. */
    fun replied(text: String, latencyMs: Long) {
        append("replied", text, latencyMs)
    }

    /**
     * Read the last [n] entries from the log file, newest first.
     * Returns empty list if file doesn't exist or is unreadable.
     */
    fun recent(n: Int = 10): List<String> {
        if (!logFile.isFile) return emptyList()
        return try {
            logFile.readLines().takeLast(n)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read conversation log: ${e.message}")
            emptyList()
        }
    }

    private fun append(direction: String, text: String, latencyMs: Long) {
        try {
            val entry = JSONObject().apply {
                put("ts", isoFmt.format(Date()))
                put("dir", direction)
                put("text", text.take(MAX_TEXT_LEN))
                put("session", sessionId)
                put("build_code", BuildConfig.VERSION_CODE)
                put("build_name", BuildConfig.VERSION_NAME)
                if (latencyMs > 0) put("latency_ms", latencyMs)
            }
            logFile.appendText(entry.toString() + "\n")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write conversation log: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SmolcaseLog"
        private const val LOG_DIR = "logs"
        private const val LOG_FILENAME = "conversations.jsonl"
        private const val PREFS_NAME = "smolcase_log"
        private const val KEY_SESSION = "log_session"
        private const val MAX_TEXT_LEN = 500
    }
}