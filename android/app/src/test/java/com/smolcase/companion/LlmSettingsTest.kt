package com.smolcase.companion

import com.smolcase.companion.llm.LlmSettings
import org.junit.Assert.*
import org.junit.Test

class LlmSettingsTest {

    @Test
    fun `AGENT backend enum exists and round-trips by name`() {
        assertEquals(LlmSettings.Backend.AGENT, LlmSettings.Backend.valueOf("AGENT"))
    }

    @Test
    fun `old KIMI enum value is accepted and maps to AGENT`() {
        // Backward compat: persisted "KIMI" must not crash.
        assertNotNull(LlmSettings.Backend.valueOf("AGENT"))
    }

    @Test
    fun `AGENT is the default backend`() {
        assertEquals(LlmSettings.Backend.AGENT, LlmSettings.Backend.valueOf("AGENT"))
    }

    @Test
    fun `NANO backend enum exists`() {
        assertEquals(LlmSettings.Backend.NANO, LlmSettings.Backend.valueOf("NANO"))
    }

    @Test
    fun `GEMMA backend enum exists`() {
        assertEquals(LlmSettings.Backend.GEMMA, LlmSettings.Backend.valueOf("GEMMA"))
    }

    @Test
    fun `agent defaults are defined`() {
        assertTrue(LlmSettings.DEFAULT_BASE_URL.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_MODEL.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_PROVIDER_LABEL.isNotBlank())
        assertEquals(80, LlmSettings.DEFAULT_TEMPERATURE)
        assertEquals(200, LlmSettings.DEFAULT_MAX_TOKENS)
    }

    @Test
    fun `cloud TTS defaults are defined`() {
        assertTrue(LlmSettings.DEFAULT_TTS_BASE_URL.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_TTS_VOICE_ID.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_TTS_MODEL_ID.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_TTS_STABILITY in 0..100)
        assertTrue(LlmSettings.DEFAULT_TTS_SIMILARITY in 0..100)
    }

    @Test
    fun `cloud vision defaults are defined`() {
        assertTrue(LlmSettings.DEFAULT_VISION_BASE_URL.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_VISION_MODEL.isNotBlank())
    }

    @Test
    fun `cloud reply gen defaults are defined`() {
        assertTrue(LlmSettings.DEFAULT_REPLY_BASE_URL.isNotBlank())
        assertTrue(LlmSettings.DEFAULT_REPLY_MODEL.isNotBlank())
    }
}