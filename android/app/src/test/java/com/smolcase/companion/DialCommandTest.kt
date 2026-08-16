package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialCommandTest {

    @Test
    fun `set humor to 90 parses`() {
        val r = DialCommand.parse("set humor to 90")
        assertEquals(DialCommand.Result.Set(DialCommand.Dial.HUMOR, 90), r)
    }

    @Test
    fun `honesty 50 percent parses without the word set`() {
        val r = DialCommand.parse("honesty 50 percent")
        assertEquals(DialCommand.Result.Set(DialCommand.Dial.HONESTY, 50), r)
    }

    @Test
    fun `case and british spelling are accepted`() {
        assertEquals(
            DialCommand.Result.Set(DialCommand.Dial.HUMOR, 60),
            DialCommand.parse("Set HUMOUR to 60")
        )
    }

    @Test
    fun `percent sign is accepted`() {
        assertEquals(
            DialCommand.Result.Set(DialCommand.Dial.HONESTY, 100),
            DialCommand.parse("honesty 100%")
        )
    }

    @Test
    fun `out of range is rejected`() {
        val r = DialCommand.parse("set humor to 250")
        assertEquals(DialCommand.Result.Rejected(DialCommand.Dial.HUMOR, 250), r)
    }

    @Test
    fun `non-dial speech returns null`() {
        assertNull(DialCommand.parse("what's next"))
        assertNull(DialCommand.parse("remind me to call mum"))
        assertNull(DialCommand.parse("humor")) // no value
    }
}
