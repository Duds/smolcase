package com.smolcase.companion.llm

import com.smolcase.companion.PersonalityDials
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Kimi backend — OpenAI-compatible chat completions.
 *
 * Default endpoint: Moonshot public API (https://api.moonshot.ai/v1).
 * Base URL, API key, and model are configurable in Settings, so this can be
 * pointed at any compatible server (including a local one) without a rebuild.
 */
class KimiBackend(private val settings: LlmSettings, private val dials: PersonalityDials) : LlmBackend {

    override fun unavailableReason(): String? =
        if (settings.kimiConfigured) null else "Kimi API key not set (long-press the face → Settings)"

    override fun reply(userText: String, soulContext: String, callback: (String?) -> Unit) {
        thread {
            try {
                val url = URL(settings.kimiBaseUrl + "/chat/completions")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${settings.kimiApiKey}")
                    doOutput = true
                }

                val messages = JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put(
                                "content",
                                CreaturePersona.prompt(dials.humor, dials.honesty) +
                                    "\n\nContext from the creature's memory:\n" + soulContext
                            )
                    )
                    .put(JSONObject().put("role", "user").put("content", userText))

                val body = JSONObject()
                    .put("model", settings.kimiModel)
                    .put("messages", messages)
                    .put("temperature", 0.8)
                    .put("max_tokens", 120)

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                val text = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText()
                        ?: "HTTP $code"
                    callback(null)
                    return@thread
                }

                val content = JSONObject(text)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                callback(content.ifBlank { null })
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
}
