package com.smolcase.companion.llm

import com.smolcase.companion.MemoryStore
import com.smolcase.companion.PersonalityDials
import com.smolcase.companion.matrix.EyeExpressionState
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Agent backend — generic OpenAI-compatible chat completions with tool support.
 *
 * By default reads from `agent_*` settings. With `useReplyGen=true`, reads
 * from `cloud_reply_gen_*` settings instead — used by ConversationEngine
 * when cloud reply generation is enabled independently of the thinking backend.
 *
 * When [memory] is provided (non-null), the LLM gains access to 11 tools:
 * personality dials, reminders, fact storage, time, mood expressions, and
 * soul summary. Tools are defined in [AgentTool] and executed here in a
 * two-turn loop: initial request → tool calls → execute → second request
 * → final spoken reply.
 */
class AgentBackend(
    private val settings: LlmSettings,
    private val dials: PersonalityDials,
    private val useReplyGen: Boolean = false,
    /** When set, enables tool-calling. Can be shared with ConversationEngine. */
    val memory: MemoryStore? = null,
    /** Optional — enables the set_mood tool. */
    val expressionState: EyeExpressionState? = null
) : LlmBackend {

    private val baseUrl get() = if (useReplyGen) settings.cloudReplyGenBaseUrl else settings.agentBaseUrl
    private val apiKey get() = if (useReplyGen) settings.cloudReplyGenApiKey else settings.agentApiKey
    private val model get() = if (useReplyGen) settings.cloudReplyGenModel else settings.agentModel
    private val temperature get() = if (useReplyGen) 80 else settings.agentTemperature // reply gen uses fixed 0.80
    private val maxTokens get() = if (useReplyGen) 120 else settings.agentMaxTokens // reply gen uses shorter output

    override fun unavailableReason(): String? {
        if (useReplyGen) {
            return if (settings.cloudReplyGenConfigured) null
            else "Cloud reply API key not set (Settings -> Cloud Reply)"
        }
        return if (settings.agentConfigured) null else "Agent API key not set (long-press the face -> Settings)"
    }

    override fun reply(userText: String, soulContext: String, callback: (String?) -> Unit) {
        thread {
            try {
                val systemContent = CreaturePersona.prompt(dials.humor, dials.honesty) +
                    "\n\n" + soulContext +
                    toolCapabilityNote()

                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemContent))
                    .put(JSONObject().put("role", "user").put("content", userText))

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", temperature / 100.0)
                    .put("max_tokens", maxTokens)
                // Add tool definitions when memory is wired
                if (memory != null) {
                    body.put("tools", AgentTool.definitions())
                    body.put("tool_choice", "auto")
                }

                val response = sendRequest(body)
                val choice = response.getJSONArray("choices").getJSONObject(0)
                val message = choice.getJSONObject("message")
                val replyText = message.optString("content", "").trim()
                val toolCalls = message.optJSONArray("tool_calls")

                // No tool calls → single-turn reply
                if (toolCalls == null || toolCalls.length() == 0) {
                    callback(replyText.ifBlank { null })
                    return@thread
                }

                // ── Tool-call loop (2-turn max) ─────────────────────────
                // Build the assistant message with tool_calls
                val assistantMsg = JSONObject()
                    .put("role", "assistant")
                if (replyText.isNotBlank()) assistantMsg.put("content", replyText)
                assistantMsg.put("tool_calls", toolCalls)

                // Execute each tool call and build tool result messages
                val mem = memory ?: run { callback(null); return@thread }
                val toolMessages = JSONArray().apply {
                    for (i in 0 until toolCalls.length()) {
                        val tc = toolCalls.getJSONObject(i)
                        val id = tc.getString("id")
                        val func = tc.getJSONObject("function")
                        val funcName = func.getString("name")
                        val funcArgs = func.optJSONObject("arguments")
                            ?: JSONObject(func.optString("arguments", "{}"))

                        val result = try {
                            AgentTool.execute(funcName, funcArgs, mem, dials, expressionState)
                        } catch (e: Exception) {
                            "Error: ${e.message ?: "tool execution failed"}"
                        }

                        put(JSONObject()
                            .put("role", "tool")
                            .put("tool_call_id", id)
                            .put("content", result))
                    }
                }

                // Second request: original messages + assistant + tool results
                val messages2 = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemContent))
                    .put(JSONObject().put("role", "user").put("content", userText))
                    .put(assistantMsg)
                for (i in 0 until toolMessages.length()) messages2.put(toolMessages.get(i))

                val body2 = JSONObject()
                    .put("model", model)
                    .put("messages", messages2)
                    .put("temperature", temperature / 100.0)
                    .put("max_tokens", maxTokens)

                val response2 = sendRequest(body2)
                val finalText = response2
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
                    .trim()
                callback(finalText.ifBlank { null })

            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────

    /**
     * POST [body] to the chat completions endpoint and return the parsed
     * JSON response. Throws on HTTP errors.
     */
    private fun sendRequest(body: JSONObject): JSONObject {
        val url = URL(baseUrl + "/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw RuntimeException(err)
        }
        return JSONObject(conn.inputStream.bufferedReader().readText())
    }

    /**
     * Append a subtle capability hint to the system prompt so the LLM
     * knows it can perform actions via tool calls without breaking
     * character. Only added when tools are actually available.
     */
    private fun toolCapabilityNote(): String {
        if (memory == null) return ""
        return """
            
You have the ability to perform actions — adjust your dials, save reminders,
store and recall facts, check the time, and change your expression. Use them
when appropriate. Never mention the tools themselves.
""".trimEnd()
    }
}