package com.smolcase.companion

import com.smolcase.companion.llm.CreaturePersona
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreaturePersonaTest {

    @Test
    fun `prompt interpolates the dial settings`() {
        val p = CreaturePersona.prompt(humor = 75, honesty = 90)
        assertTrue(p.contains("HUMOR 75%"))
        assertTrue(p.contains("HONESTY 90%"))
    }

    @Test
    fun `prompt is the TARS register`() {
        val p = CreaturePersona.prompt(10, 40)
        assertTrue(p.contains("marine company commander"))
        assertTrue(p.contains("gym teacher"))
        assertTrue(p.contains("never break character"))
        assertTrue(p.contains("no emoji"))
    }

    @Test
    fun `prompt never offers the old cute persona`() {
        val p = CreaturePersona.prompt(75, 90)
        assertFalse(p.contains("warm, playful"))
    }
}
