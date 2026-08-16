package com.smolcase.companion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smolcase.companion.llm.GeminiNanoBackend
import com.smolcase.companion.llm.KimiBackend
import com.smolcase.companion.llm.LlmBackend
import com.smolcase.companion.llm.LlmSettings

/**
 * Conversation flow (Stage 3+):
 *   1. IntentRouter handles the known command set (reminders, greetings)
 *   2. Unknown speech goes to the configured LLM (Gemini Nano or Kimi)
 *      with the soul-file context
 *   3. If the LLM is unavailable/fails, fall back to the deadpan
 *      "You'll have to rephrase that."
 */
class ConversationEngine(context: Context, private val memory: MemoryStore) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val settings = LlmSettings(context)
    private val dials = PersonalityDials(context)
    private val nano = GeminiNanoBackend(context.applicationContext, dials)
    private val kimi = KimiBackend(settings, dials)

    fun handle(text: String, onReply: (IntentRouter.Reply) -> Unit) {
        // Known commands always win — instant and offline
        IntentRouter.route(text, memory, dials)?.let { onReply(it); return }

        val backend: LlmBackend? = when (settings.backend) {
            LlmSettings.Backend.NANO -> nano
            LlmSettings.Backend.KIMI -> kimi
            LlmSettings.Backend.RULES -> null
        }

        if (backend == null ||
            (settings.backend == LlmSettings.Backend.KIMI && !settings.kimiConfigured)
        ) {
            onReply(IntentRouter.unknownReply())
            return
        }

        backend.reply(text, memory.soulSummary()) { llmText ->
            mainHandler.post {
                if (llmText != null) {
                    onReply(
                        IntentRouter.Reply(
                            say = llmText,
                            excitement = 0.5f,
                            happy = 0.7f,
                            blinks = 1
                        )
                    )
    @Volatile private var lastLoggedReason: String? = null

    /**
     * One telemetry line describing the thinking layer's health, for the
     * face feed ("LLM NANO OK" / "LLM NANO --"). The full reason goes to
     * logcat (tag SmolcaseLLM) when it changes, for remote diagnosis.
     */
    fun statusLine(): String {
        val (label, reason) = when (settings.backend) {
            LlmSettings.Backend.RULES -> return "LLM RULES"
            LlmSettings.Backend.NANO -> "NANO" to nano.unavailableReason()
            LlmSettings.Backend.KIMI -> "KIMI" to
                if (settings.kimiConfigured) kimi.unavailableReason() else "API key not set"
        }
        if (reason != lastLoggedReason) {
            lastLoggedReason = reason
            if (reason != null) Log.w(TAG, "$label unavailable: $reason")
        }
        return if (reason == null) "LLM $label OK" else "LLM $label --"
    }

    companion object {
        private const val TAG = "SmolcaseLLM"
    }
}
