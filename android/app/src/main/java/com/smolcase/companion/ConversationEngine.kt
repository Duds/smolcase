package com.smolcase.companion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smolcase.companion.cloud.CloudVision
import com.smolcase.companion.llm.AgentBackend
import com.smolcase.companion.llm.GemmaBackend
import com.smolcase.companion.llm.GeminiNanoBackend
import com.smolcase.companion.llm.LlmBackend
import com.smolcase.companion.llm.LlmSettings
import androidx.camera.lifecycle.ProcessCameraProvider

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
    // Lazy: only the selected backend is ever constructed. Constructing all
    // three meant the ML Kit AICore client AND the Gemma engine were alive
    // in every session — impossible to attribute native CPU burn, and the
    // 2.4 GB Gemma load shouldn't happen for RULES/KIMI users anyway.
    private val nano by lazy { GeminiNanoBackend(context.applicationContext, dials) }
    private val kimi by lazy { AgentBackend(settings, dials) }
    private val gemma by lazy { GemmaBackend(context.applicationContext, dials) }

    // Cloud reply gen: independent AgentBackend using cloud_reply_gen_* settings
    private val cloudReplyBackend by lazy { AgentBackend(settings, dials, useReplyGen = true) }

    // Cloud Vision: set externally by MainActivity when CameraProvider is available
    var cloudVision: CloudVision? = null

    /** Vision context injected into the next LLM reply. Set by triggerVision(). */
    @Volatile var visionContext: String? = null

    /**
     * Trigger a single vision analysis frame. Result sets [visionContext]
     * and calls [onResult] with the description. Call from MainActivity.
     */
    fun triggerVision(provider: ProcessCameraProvider, onResult: (String?) -> Unit) {
        cloudVision?.analyze(provider) { description ->
            visionContext = description
            if (description != null) {
                android.util.Log.i(TAG, "Vision: $description")
            }
            onResult(description)
        }
    }

    fun handle(text: String, onReply: (IntentRouter.Reply) -> Unit) {
        // Known commands always win — instant and offline
        IntentRouter.route(text, memory, dials)?.let { onReply(it); return }

        // Build soul context with optional vision description
        val vision = visionContext
        visionContext = null // one-shot — cleared after each handle()
        val contextSummary = memory.soulSummary() +
            (if (vision != null) "\n\nWhat the creature sees: $vision" else "")

        // Cloud reply gen overrides the selected backend if enabled
        if (settings.cloudReplyGenEnabled && settings.cloudReplyGenConfigured) {
            cloudReplyBackend.reply(text, contextSummary) { llmText ->
                mainHandler.post {
                    onReply(
                        if (llmText != null) IntentRouter.Reply(llmText, 0.5f, 0.7f, 1)
                        else IntentRouter.unknownReply()
                    )
                }
            }
            return
        }

        val backend: LlmBackend? = when (settings.backend) {
            LlmSettings.Backend.NANO -> nano
            LlmSettings.Backend.AGENT -> kimi
            LlmSettings.Backend.GEMMA -> gemma
            LlmSettings.Backend.RULES -> null
        }

        if (backend == null) {
            onReply(IntentRouter.unknownReply())
            return
        }
        if (settings.backend == LlmSettings.Backend.AGENT && !settings.agentConfigured) {
            onReply(
                IntentRouter.Reply(
                    "I can't answer that yet — set up an API key in Settings.",
                    excitement = 0.2f, happy = 0.1f, blinks = 0
                )
            )
            return
        }

        backend.reply(text, contextSummary) { llmText ->
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
                } else {
                    onReply(IntentRouter.unknownReply())
                }
            }
        }
    }

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
            LlmSettings.Backend.AGENT -> "AGENT" to
                if (settings.agentConfigured) kimi.unavailableReason() else "API key not set"
            LlmSettings.Backend.GEMMA -> "GEMMA" to gemma.unavailableReason()
        }
        if (reason != lastLoggedReason) {
            lastLoggedReason = reason
            if (reason != null) Log.w(TAG, "$label unavailable: $reason")
        }
        return if (reason == null) "LLM $label OK" else "LLM $label --"
    }

    /** Release the Gemma engine (large native model) on the way out. */
    fun shutdown() {
        gemma.shutdown()
        cloudVision?.shutdown()
    }

    companion object {
        private const val TAG = "SmolcaseLLM"
    }
}
