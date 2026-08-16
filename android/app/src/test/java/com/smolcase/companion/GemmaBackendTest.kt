package com.smolcase.companion

import com.smolcase.companion.llm.GemmaBackend
import com.smolcase.companion.llm.LlmSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaBackendTest {

    @Test
    fun `GEMMA backend enum exists and round-trips by name`() {
        // Persisted by name in SharedPreferences — must never break.
        assertEquals(LlmSettings.Backend.GEMMA, LlmSettings.Backend.valueOf("GEMMA"))
    }

    @Test
    fun `missing model reason carries adb push instructions`() {
        val reason = GemmaBackend.missingFileReason(
            "/sdcard/Android/data/com.smolcase.companion/files/models/gemma-4-E2B-it.litertlm"
        )
        assertTrue(reason.contains("adb push"))
        assertTrue(reason.contains(GemmaBackend.MODEL_FILENAME))
        assertTrue(reason.contains("/sdcard/Android/data/com.smolcase.companion/files/models/"))
    }
}
