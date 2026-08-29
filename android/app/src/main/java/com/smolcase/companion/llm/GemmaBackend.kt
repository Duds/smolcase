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
                val e = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        maxNumTokens = 1024, // one-two sentence replies; caps KV cache
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
        const val MODEL_DIR = "/storage/emulated/0/models"

        /** Actionable missing-model reason, JVM-testable without a Context. */
        fun missingFileReason(): String =
            "Gemma model not sideloaded: adb push $MODEL_FILENAME to $MODEL_DIR/"
    }
}
