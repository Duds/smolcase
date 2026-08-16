package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TelemetryFeedTest {

    private var now = 0L
    private fun feed(
        sources: List<() -> String?> = emptyList(),
        clock: String = "CLK 09:41"
    ) = TelemetryFeed(
        lineLength = 14,
        sources = sources,
        nowMs = { now },
        random = Random(42),
        clockLine = { clock }
    )

    @Test
    fun `filler lines fill the grid width and use low-density glyphs`() {
        val f = feed()
        val line = f.fillerLine()
        assertEquals(14, line.length)
        assertTrue(line.all { it in "▓▒░:· 0123456789ABCDEF" })
    }

    @Test
    fun `telemetry sources rotate round-robin`() {
        val f = feed(sources = listOf({ "AAAA" }, { "BBBB" }))
        assertEquals("AAAA", f.nextLine())
        assertEquals("BBBB", f.nextLine())
        assertEquals("AAAA", f.nextLine())
    }

    @Test
    fun `empty or failing sources fall back to filler in NORMAL mode`() {
        val f = feed(sources = listOf({ null }, { throw RuntimeException("boom") }))
        val line = f.nextLine()
        assertEquals(14, line.length) // silently became filler
    }

    @Test
    fun `ALL_FILLER ignores sources, ALL_TELEMETRY never emits filler`() {
        val f = feed(sources = listOf({ "REAL DATA" }))
        f.mode = TelemetryFeed.Mode.ALL_FILLER
        assertTrue(f.nextLine() != "REAL DATA")
        f.mode = TelemetryFeed.Mode.ALL_TELEMETRY
        assertEquals("REAL DATA", f.nextLine())
        val empty = feed(sources = listOf({ null }))
        empty.mode = TelemetryFeed.Mode.ALL_TELEMETRY
        assertEquals("", empty.nextLine()) // blank, never filler
    }

    @Test
    fun `sleeping feed emits only clock or filler`() {
        val f = feed(sources = listOf({ "SESS 0001" }), clock = "CLK 02:00")
        f.sleeping = true
        repeat(10) {
            val line = f.nextLine()
            assertTrue(line == "CLK 02:00" || line.all { it in "▓▒░:· 0123456789ABCDEF" })
        }
    }

    @Test
    fun `window returns the newest lines oldest-first and pads with blanks`() {
        val f = feed(sources = listOf({ "ONE" }, { "TWO" }, { "THREE" }))
        f.advance(); f.advance()
        val w = f.window(4)
        assertEquals(listOf("", "", "ONE", "TWO"), w)
    }

    @Test
    fun `tick advances about two rows per second awake, slower sleeping`() {
        val f = feed(sources = listOf({ "ROW" }))
        f.tick()                       // now=0: advances, next due at 500
        now += 499; f.tick()           // 499: not due
        now += 1; f.tick()             // 500: advances, next due at 1000
        assertEquals(2, f.window(7).count { it.isNotEmpty() })
        f.sleeping = true
        now += 499; f.tick()           // 999: not due
        now += 1; f.tick()             // 1000: advances, sleeping so next due at 3000
        assertEquals(3, f.window(7).count { it.isNotEmpty() })
        now += 1999; f.tick()          // 2999: not due (sleeping interval)
        assertEquals(3, f.window(7).count { it.isNotEmpty() })
        now += 1; f.tick()             // 3000: due
        assertEquals(4, f.window(7).count { it.isNotEmpty() })
    }

    @Test
    fun `FROZEN mode stops the scroll`() {
        val f = feed(sources = listOf({ "STOP" }))
        f.tick()
        f.mode = TelemetryFeed.Mode.FROZEN
        now += 10_000
        f.tick()
        assertEquals(listOf("STOP"), f.window(1))
    }
}
