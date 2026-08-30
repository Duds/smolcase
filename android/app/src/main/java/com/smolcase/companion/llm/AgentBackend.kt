package com.smolcase.companion.llm

import com.smolcase.companion.PersonalityDials
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Agent backend — generic OpenAI-compatible chat completions.
 *
 * By default reads from `agent_*` settings. With `useReplyGen=true`, reads
 * from `cloud_reply_gen_*` settings instead — used by ConversationEngine
 * when cloud reply generation is enabled independently of the thinking backend.
 */
class AgentBackend(
    private val settings: LlmSettings,
    private val dials: PersonalityDials,
    private val useReplyGen: Boolean = false
) : LlmBackend {

    private val baseUrl get() = if (useReplyGen) settings.cloudReplyGenBaseUrl else settings.agentBaseUrl
    private val apiKey get() = if (useReplyGen) settings.cloudReplyGenApiKey else settings.agentApiKey
    private val model get() = if (useReplyGen) settings.cloudReplyGenModel else settings.agentModel
    private val temperature get() = if (useReplyGen) 80 else settings.agentTemperature // reply gen uses fixed 0.80
    private val maxTokens get() = if (useReplyGen) 120 else settings.agentMaxTokens // reply gen uses shorter output

    override fun unavailableReason(): String? {
        if (useReplyGen) {
            return if (settings.cloudReplyGenConfigured) null
            else "Cloud reply API key not set (Settings → Cloud Reply)"
        }
        return if (settings.agentConfigured) null else "Agent API key not set (long-press the face → Settings)"
    }

    override fun reply(userText: String, soulContext: String, callback: (String?) -> Unit) {
        thread {
            try {
                val url = URL(baseUrl + "/chat/completions")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${apiKey}")
                    doOutput = true
                }

                val messages = JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put(
                                "content",
                                CreaturePersona.prompt(dials.humor, dials.honesty) +
                                    "\n\n" + soulContext
                            )
                    )
                    .put(JSONObject().put("role", "user").put("content", userText))

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", temperature / 100.0)
                    .put("max_tokens", maxTokens)

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.errorStream?.bufferedReader()?.readText()
                    callback(null)
                    return@thread
                }

                val text = conn.inputStream.bufferedReader().readText()
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