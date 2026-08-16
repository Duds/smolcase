package com.smolcase.companion.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.smolcase.companion.PersonalityDials
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gemma 4 E2B on-device via LiteRT-LM (litertlm-android 0.16.0).
 * The .litertlm model is sideloaded over adb — no AICore, no Google
 * account, no network. CPU backend only: the Mali-G715 GPU path has a
 * documented number/contraction mangling bug (LiteRT-LM issue #2202).
 *
 * See docs/06-specs/2026-08-17-gemma-backend-design.md
 */
class GemmaBackend(context: Context, private val dials: PersonalityDials) : LlmBackend {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val modelFile = File(context.getExternalFilesDir(null), "models/$MODEL_FILENAME")

    @Volatile private var ready = false
    @Volatile private var unavailable: String? = null
    @Volatile private var engine: Engine? = null

    init {
        if (!modelFile.isFile) {
            unavailable = missingFileReason(modelFile.absolutePath)
        } else {
            // Warm up at startup: initialize() can take ~10s cold, and the
            // first spoken question shouldn't have to wait for it.
            unavailable = "Gemma is warming up (model load)"
            scope.launch {
                try {
                    val e = Engine(
                        EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = Backend.CPU(),
                            cacheDir = context.cacheDir.path
                        )
                    )
                    e.initialize()
                    engine = e
                    ready = true
                    unavailable = null
                } catch (t: Throwable) {
                    unavailable = "Gemma failed to load: ${t.message}"
                    Log.w(TAG, "Gemma engine init failed", t)
                }
            }
        }
    }

    override fun unavailableReason(): String? =
        if (ready) null else (unavailable ?: "Gemma not ready yet")

    override fun reply(userText: String, soulContext: String, callback: (String?) -> Unit) {
        val e = engine
        if (!ready || e == null) {
            callback(null)
            return
        }
        scope.launch {
            try {
                // Single-turn, stateless — same contract as the other backends.
                e.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(
                            CreaturePersona.prompt(dials.humor, dials.honesty)
                        )
                    )
                ).use { conversation ->
                    val response = conversation.sendMessage(
                        "Context from the creature's memory:\n" + soulContext +
                            "\n\nThe human says: " + userText
                    )
                    val text = response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                        .trim()
                    callback(text.ifBlank { null })
                }
            } catch (t: Throwable) {
                unavailable = "Gemma reply failed: ${t.message}"
                Log.w(TAG, "Gemma reply failed", t)
                callback(null)
            }
        }
    }

    fun shutdown() {
        try {
            engine?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "Gemma engine close failed", t)
        }
        engine = null
        ready = false
    }

    companion object {
        private const val TAG = "SmolcaseLLM"
        const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

        /** Actionable missing-model reason, JVM-testable without a Context. */
        fun missingFileReason(path: String): String =
            "Gemma model not sideloaded: adb push $MODEL_FILENAME to " +
                path.removeSuffix(MODEL_FILENAME)
    }
}
