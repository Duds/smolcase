package com.smolcase.companion

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * The creature's social brain (Stage 2).
 *
 * Watches the raw face/no-face stream, groups it into *sessions* (you at the
 * desk), and decides how to greet you when you arrive and how to say goodbye
 * when you leave. Greeting intensity scales with how long you were away;
 * the first sighting of a day is always a big deal; the first sighting EVER
 * is the creature's birth.
 */
class CreatureBrain(
    context: Context,
    private val eyes: TarsFaceView
) {
    enum class Greeting { BACK_ALREADY, HELLO, MISSED_YOU, GOOD_MORNING, BIRTH }

    private val memory = MemoryStore(context)
    private val handler = Handler(Looper.getMainLooper())

    /** Spoken greeting lines (TARS deadpan). Wired to CreatureVoice in MainActivity. */
    var onGreetingLine: ((String) -> Unit)? = null

    private var sessionActive = false
    private var lastFaceEventAt = 0L

    private val farewellTimeoutMs = 4_000L
    private val watchdogTickMs = 1_000L

    private val watchdog = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (sessionActive && now - lastFaceEventAt > farewellTimeoutMs) {
                sessionActive = false
                memory.endSession(now)
                eyes.farewell()
            }
            handler.postDelayed(this, watchdogTickMs)
        }
    }

    fun start() {
        handler.post(watchdog)
    }

    fun stop() {
        handler.removeCallbacks(watchdog)
        if (sessionActive) {
            memory.endSession()
            sessionActive = false
        }
    }

    /** Raw face observation from FaceTracker. */
    fun onFace(nx: Float, ny: Float) {
        val now = System.currentTimeMillis()
        lastFaceEventAt = now
        eyes.onFaceSeen(nx, ny)

        if (!sessionActive) {
            sessionActive = true
            val arrival = memory.beginSession(now)
            greet(arrival)
        }
    }

    private fun greet(arrival: MemoryStore.Arrival) {
        val g = classify(arrival)
        when (g) {
            Greeting.BIRTH ->
                eyes.express(excitement = 0.6f, happy = 0.3f, blinks = 1)
            Greeting.GOOD_MORNING ->
                eyes.express(excitement = 0.5f, happy = 0.3f, blinks = 1)
            Greeting.MISSED_YOU ->
                eyes.express(excitement = 0.4f, happy = 0.3f, blinks = 1)
            Greeting.HELLO ->
                eyes.express(excitement = 0.25f, happy = 0.2f, blinks = 1)
            Greeting.BACK_ALREADY ->
                eyes.express(excitement = 0.1f, happy = 0.1f, blinks = 0)
        }
        onGreetingLine?.invoke(greetingLine(g))
    }

    private fun greetingLine(g: Greeting): String = when (g) {
        Greeting.BIRTH -> "Systems nominal. So this is the desk."
        Greeting.GOOD_MORNING -> {
            val late = memory.latenessMinutes()
            if (late > 5) "Morning. You're $late minutes later than usual."
            else "Morning. Right on schedule."
        }
        Greeting.MISSED_YOU -> "You were gone a while."
        Greeting.HELLO -> "Hello."
        Greeting.BACK_ALREADY -> "That was fast."
    }

    private fun classify(a: MemoryStore.Arrival): Greeting = when {
        a.isFirstEver -> Greeting.BIRTH
        a.isFirstToday && a.absenceMs > 30 * 60_000L -> Greeting.GOOD_MORNING
        a.absenceMs < 2 * 60_000L -> Greeting.BACK_ALREADY
        a.absenceMs < 30 * 60_000L -> Greeting.HELLO
        a.absenceMs < 3 * 3_600_000L -> Greeting.MISSED_YOU
        else -> Greeting.MISSED_YOU // very long absence: still "missed you"
    }
}
