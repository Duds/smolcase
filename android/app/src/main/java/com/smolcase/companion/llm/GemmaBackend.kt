package com.smolcase.companion.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
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
    // Sideload source: /sdcard/models/ — a public dir the owner can see in
    // the Files app. Requires MANAGE_EXTERNAL_STORAGE, granted once via
    // appops (see the spec). The model is copied into app-internal storage
    // on first run because /sdcard is FUSE — LiteRT-LM memory-maps the
    // model file, and mmap page faults through the FUSE daemon make engine
    // init take tens of minutes instead of seconds.
    private val sideloadFile = File(MODEL_DIR, MODEL_FILENAME)
    private val internalFile = File(context.filesDir, "models/$MODEL_FILENAME")

    @Volatile private var ready = false
    @Volatile private var unavailable: String? = null
    @Volatile private var engine: Engine? = null
    private var lastExchange: Pair<String, String>? = null // (user, reply) for multi-turn
    private var engineConfigCacheDir: String = ""

    init {
        Engine.setNativeMinLogSeverity(LogSeverity.INFO) // engine progress in logcat
        scope.launch {
            try {
                val modelPath = when {
                    internalFile.isFile -> internalFile.absolutePath
                    sideloadFile.isFile -> {
                        unavailable = "Gemma first-run copy to fast storage (~2 min)"
                        copyModel(sideloadFile, internalFile)
                        internalFile.absolutePath
                    }
                    else -> {
                        unavailable = missingFileReason()
                        return@launch
                    }
                }
                // Warm up at startup: initialize() takes seconds from ext4,
                // and the first spoken question shouldn't have to wait.
                unavailable = "Gemma is warming up (model load)"
                val cacheDir = context.cacheDir.path
                engineConfigCacheDir = cacheDir
                val e = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        maxNumTokens = 512, // context window fits system prompt + memory + user + short reply
                        cacheDir = cacheDir
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

    /** Plain stream copy (no mmap), via a .partial file so a kill mid-copy
     *  never leaves a truncated model looking complete. */
    private fun copyModel(src: File, dst: File) {
        dst.parentFile?.mkdirs()
        val tmp = File(dst.path + ".partial")
        src.inputStream().buffered(1 shl 20).use { input ->
            tmp.outputStream().buffered(1 shl 20).use { output ->
                input.copyTo(output)
            }
        }
        if (tmp.length() != src.length()) {
            tmp.delete()
            throw IllegalStateException("Model copy truncated")
        }
        if (!tmp.renameTo(dst)) throw IllegalStateException("Model copy rename failed")
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
                val persona = CreaturePersona.prompt(dials.humor, dials.honesty)
                val last = lastExchange
                val pastContext = if (last != null)
                    "\n\nPrevious exchange:\nPerson: ${last.first}\nYou: ${last.second}"
                else ""

                e.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(persona)
                    )
                ).use { conversation ->
                    val inferStart = System.nanoTime()
                    val response = conversation.sendMessage(
                        soulContext + pastContext + "\n\nThe person says: " + userText
                    )
                    val inferMs = (System.nanoTime() - inferStart) / 1_000_000
                    Log.i(TAG, "Gemma inference: ${inferMs}ms")
                    val reply = extractText(response)
                    if (reply != null) {
                        lastExchange = Pair(userText, reply)
                    }
                    callback(filterReply(reply))
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Gemma reply failed, reinitializing engine", t)
                // Engine state is corrupted — destroy and recreate
                shutdown()
                reinitialize()
                callback(null)
            }
        }
    }

    /**
     * Re-create the engine from scratch after a native crash.
     */
    private fun reinitialize() {
        scope.launch {
            try {
                val modelPath = when {
                    internalFile.isFile -> internalFile.absolutePath
                    sideloadFile.isFile -> {
                        unavailable = "Gemma first-run copy to fast storage (~2 min)"
                        copyModel(sideloadFile, internalFile)
                        internalFile.absolutePath
                    }
                    else -> {
                        unavailable = missingFileReason()
                        return@launch
                    }
                }
                unavailable = "Gemma is warming up (model load)"
                val e = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        maxNumTokens = 512,
                        cacheDir = engineConfigCacheDir
                    )
                )
                e.initialize()
                engine = e
                ready = true
                unavailable = null
            } catch (t: Throwable) {
                unavailable = "Gemma failed to load: ${t.message}"
                Log.w(TAG, "Gemma engine reinit failed", t)
            }
        }
    }

    /** Extract text from a sendMessage response. */
    private fun extractText(msg: com.google.ai.edge.litertlm.Message): String? {
        val startNanos = System.nanoTime()
        val text = msg.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString("") { it.text }
            .trim()
        if (text.isBlank()) {
            Log.w(TAG, "Gemma returned empty response (${(System.nanoTime() - startNanos) / 1_000_000}ms)")
        } else {
            Log.i(TAG, "Gemma raw reply (${(System.nanoTime() - startNanos) / 1_000_000}ms): $text")
        }
        return text.ifBlank { null }
    }

    /** Banned words that indicate the model is leaking meta-prompt language. */
    private fun containsBlockedWord(text: String): Boolean {
        val lower = text.lowercase()
        return blockedWords.any { lower.contains(it) }
    }

    /** Post-process the reply: strip meta-language. */
    private fun filterReply(text: String?): String? {
        if (text == null) return null
        if (containsBlockedWord(text)) {
            Log.w(TAG, "Blocked reply with meta-language: $text")
            return null
        }
        return text
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
        const val MODEL_DIR = "/storage/emulated/0/models"

        /** Words the model must never utter — signs of meta-prompt leakage. */
        private val blockedWords = listOf("directives", "directive", "system prompt")

        /** Actionable missing-model reason, JVM-testable without a Context. */
        fun missingFileReason(): String =
            "Gemma model not sideloaded: adb push $MODEL_FILENAME to $MODEL_DIR/"
    }
}
