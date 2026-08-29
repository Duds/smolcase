package com.smolcase.companion

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * The creature's memory (Stage 2, "Soul File" seed).
 *
 * Deliberately tiny: a few SharedPreferences fields. Enough for the creature
 * to know: have we met before, when did you last leave, how long were you
 * gone, have I seen you today yet, and how much of our lives we've spent
 * together. Grows into the full soul file in later stages.
 */
class MemoryStore(context: Context) {

    data class Arrival(
        val absenceMs: Long,     // how long you were away (0 if first ever)
        val isFirstEver: Boolean, // the creature's birth moment
        val isFirstToday: Boolean // first sighting since midnight
    )

    private val prefs = context.getSharedPreferences("smolcase_soul", Context.MODE_PRIVATE)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private var sessionStartedAt = 0L

    var totalSessions: Int
        get() = prefs.getInt(KEY_TOTAL_SESSIONS, 0)
        private set(v) = prefs.edit().putInt(KEY_TOTAL_SESSIONS, v).apply()

    var totalTimeTogetherMs: Long
        get() = prefs.getLong(KEY_TOTAL_TIME, 0L)
        private set(v) = prefs.edit().putLong(KEY_TOTAL_TIME, v).apply()

    val lastSessionEndAt: Long
        get() = prefs.getLong(KEY_LAST_SESSION_END, 0L)

    /** Call when a session begins (face appears after an absence). */
    fun beginSession(now: Long = System.currentTimeMillis()): Arrival {
        val lastEnd = lastSessionEndAt
        val firstEver = totalSessions == 0 && lastEnd == 0L
        val today = dayFormat.format(Date(now))
        val firstToday = prefs.getString(KEY_FIRST_DATE, null) != today

        if (firstToday) {
            prefs.edit().putString(KEY_FIRST_DATE, today).apply()
            val minutes = minutesSinceMidnight(now, TimeZone.getDefault()).toFloat()
            val prev = prefs.getFloat(KEY_USUAL_MINUTES, -1f)
            val updated = if (prev < 0f) minutes else smoothUsual(prev, minutes)
            prefs.edit().putFloat(KEY_USUAL_MINUTES, updated).apply()
        }
        sessionStartedAt = now
        totalSessions += 1

        return Arrival(
            absenceMs = if (lastEnd == 0L) 0L else now - lastEnd,
            isFirstEver = firstEver,
            isFirstToday = firstToday
        )
    }

    /** Call when a session ends (face gone past the farewell timeout). */
    fun endSession(now: Long = System.currentTimeMillis()) {
        if (sessionStartedAt > 0L) {
            totalTimeTogetherMs += now - sessionStartedAt
            sessionStartedAt = 0L
        }
        prefs.edit().putLong(KEY_LAST_SESSION_END, now).apply()
    }

    // ---- Reminders (Stage 3 voice commands; IR5OS bridge later) ----

    fun addReminder(text: String) {
        val list = getReminders().toMutableList()
        list.add(text)
        prefs.edit().putString(KEY_REMINDERS, list.joinToString("\n")).apply()
    }

    fun getReminders(): List<String> =
        prefs.getString(KEY_REMINDERS, "").orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }

    fun clearReminders() {
        prefs.edit().remove(KEY_REMINDERS).apply()
    }

    /**
     * How many minutes later than usual you first appeared today
     * (0 if you're early/on time, or if no baseline exists yet).
     */
    fun latenessMinutes(now: Long = System.currentTimeMillis()): Int {
        val usual = prefs.getFloat(KEY_USUAL_MINUTES, -1f)
        if (usual < 0f) return 0
        val diff = minutesSinceMidnight(now, TimeZone.getDefault()) - usual
        return diff.roundToInt().coerceAtLeast(0)
    }

    /** Soul-file summary injected into LLM prompts as context. */
    fun soulSummary(): String {
        val hours = totalTimeTogetherMs / 3_600_000f
        return buildString {
            append("We have shared ${totalSessions} moments together")
            append(" (about %.1f hours total).\n".format(hours))
            val reminders = getReminders()
            if (reminders.isNotEmpty()) {
                append("Notes from before: ")
                append(reminders.joinToString("; "))
                append(".\n")
            }
            append("You first met your human on ${prefs.getString(KEY_FIRST_DATE, "recently")}.")
        }
    }

    companion object {
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_TOTAL_TIME = "total_time_ms"
        private const val KEY_LAST_SESSION_END = "last_session_end"
        private const val KEY_FIRST_DATE = "first_seen_date"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_USUAL_MINUTES = "usual_first_seen_minutes"

        /** Minutes since local midnight — pure, unit-tested. */
        fun minutesSinceMidnight(now: Long, tz: TimeZone): Int {
            val cal = Calendar.getInstance(tz).apply { timeInMillis = now }
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }

        /** Exponential moving average for the usual first-seen time. */
        fun smoothUsual(prev: Float, new: Float): Float = 0.7f * prev + 0.3f * new
    }
}
