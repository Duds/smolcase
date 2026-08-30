package com.smolcase.companion

import com.smolcase.companion.llm.CreaturePersona
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreaturePersonaTest {

    @Test
    fun `prompt interpolates the dial settings`() {
        val p = CreaturePersona.prompt(humor = 75, honesty = 90)
        assertTrue(p.contains("HUMOR: 75 out of 100"))
        assertTrue(p.contains("HONESTY: 90 out of 100"))
    }

    @Test
    fun `prompt is the TARS register`() {
        val flat = CreaturePersona.prompt(10, 40).replace(Regex("\\s+"), " ")
        assertTrue(flat.contains("marine company commander"))
        assertTrue(flat.contains("gym teacher"))
        assertTrue(flat.contains("Never break character"))
        assertTrue(flat.contains("No emoji"))
    }

    @Test
    fun `prompt never offers the old cute persona`() {
        val p = CreaturePersona.prompt(75, 90)
        assertFalse(p.contains("warm, playful"))
    }

    @Test
    fun `prompt frames dials as behavioral controls`() {
        val p = CreaturePersona.prompt(50, 50)
        assertTrue(p.contains("HUMOR: 50 out of 100"))
        assertTrue(p.contains("HIGH humor = dry"))
        assertTrue(p.contains("LOW humor = literal"))
        assertTrue(p.contains("blunt"))
        assertTrue(p.contains("tactful"))
    }
}
