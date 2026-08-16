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
        when (classify(arrival)) {
            Greeting.BIRTH ->
                // First sighting ever: full excitement, biggest smile, triple blink
                eyes.express(excitement = 1.0f, happy = 1.0f, blinks = 3)
            Greeting.GOOD_MORNING ->
                // First sighting today (and you were away a while)
                eyes.express(excitement = 1.0f, happy = 1.0f, blinks = 3)
            Greeting.MISSED_YOU ->
                // Away 30 min – 3 h
                eyes.express(excitement = 0.75f, happy = 0.9f, blinks = 2)
            Greeting.HELLO ->
                // Away 2 – 30 min
                eyes.express(excitement = 0.4f, happy = 0.6f, blinks = 1)
            Greeting.BACK_ALREADY ->
                // You barely left — a glance and a small smile, not a party
                eyes.express(excitement = 0.15f, happy = 0.3f, blinks = 0)
        }
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
