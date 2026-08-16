package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MemoryMathTest {

    @Test
    fun `minutes since midnight is computed in the given timezone`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.AUGUST, 16, 9, 30, 0)
        assertEquals(
            570,
            MemoryStore.minutesSinceMidnight(cal.timeInMillis, TimeZone.getTimeZone("UTC"))
        )
    }

    @Test
    fun `usual arrival time is an exponential moving average`() {
        assertEquals(600f, MemoryStore.smoothUsual(600f, 600f), 0.01f)
        assertEquals(570f, MemoryStore.smoothUsual(600f, 500f), 0.01f) // 0.7*600 + 0.3*500
    }
}
