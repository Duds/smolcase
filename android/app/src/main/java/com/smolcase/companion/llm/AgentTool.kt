package com.smolcase.companion.llm

import com.smolcase.companion.MemoryStore
import com.smolcase.companion.PersonalityDials
import com.smolcase.companion.matrix.EyeExpressionState
import com.smolcase.companion.matrix.Mood
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tool definitions and executor for the Agent LLM tool-calling loop.
 *
 * Every tool the LLM can call is defined here in OpenAI `tools` JSON format.
 * The [execute] function dispatches by tool name and returns a result string
 * that gets sent back to the LLM as a tool response.
 */
object AgentTool {

    /**
     * Build the `tools` array for the OpenAI chat completions request.
     * Each tool is a JSON object matching the OpenAI `tools` parameter schema
     * with `type: "function"`.
     */
    fun definitions(): JSONArray = JSONArray()
        .put(defineDialSet())
        .put(defineReminderAdd())
        .put(defineReminderList())
        .put(defineReminderClear())
        .put(defineCurrentTime())
        .put(defineFactStore())
        .put(defineFactRecall())
        .put(defineFactForget())
        .put(defineFactList())
        .put(defineMoodSet())
        .put(defineSoulSummary())

    /**
     * Execute a tool call and return the result as a plain string.
     *
     * @param name The OpenAI tool function name
     * @param args The parsed JSON arguments object
     * @param memory MemoryStore for reminders and facts
     * @param dials PersonalityDials for humor/honesty
     * @param expressionState EyeExpressionState for mood transitions
     * @return Human-readable result string to send back to the LLM
     */
    fun execute(
        name: String,
        args: JSONObject,
        memory: MemoryStore,
        dials: PersonalityDials,
        expressionState: EyeExpressionState? = null
    ): String {
        return when (name) {
            "set_dial" -> {
                val dial = args.optString("dial", "").lowercase()
                val value = args.optInt("value", -1).coerceIn(0, 100)
                when (dial) {
                    "humor" -> { dials.humor = value; "Humor dial set to $value." }
                    "honesty" -> { dials.honesty = value; "Honesty dial set to $value." }
                    else -> "Unknown dial '$dial'. Use 'humor' or 'honesty'."
                }
            }

            "add_reminder" -> {
                val text = args.optString("text", "").trim()
                if (text.isBlank()) "Reminder text cannot be empty."
                else { memory.addReminder(text); "Reminder saved: $text" }
            }

            "list_reminders" -> {
                val list = memory.getReminders()
                if (list.isEmpty()) "No reminders set."
                else list.joinToString("\n") { "- $it" }
            }

            "clear_reminders" -> {
                memory.clearReminders()
                "All reminders cleared."
            }

            "get_current_time" -> {
                val fmt = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US)
                "Current time is ${fmt.format(Date())}."
            }

            "store_fact" -> {
                val key = args.optString("key", "").trim()
                val value = args.optString("value", "").trim()
                if (key.isBlank()) "Fact key cannot be empty."
                else { memory.putFact(key, value); "Noted: $key = $value" }
            }

            "recall_fact" -> {
                val key = args.optString("key", "").trim()
                val value = memory.getFact(key)
                if (value != null) "$key = $value"
                else "I don't have anything recorded for '$key'."
            }

            "forget_fact" -> {
                val key = args.optString("key", "").trim()
                val existed = memory.getFact(key) != null
                memory.removeFact(key)
                if (existed) "Forgot '$key'." else "Nothing to forget — '$key' wasn't stored."
            }

            "list_facts" -> {
                val facts = memory.getAllFacts()
                if (facts.isEmpty()) "I don't know any facts yet."
                else facts.entries.joinToString("\n") { "- ${it.key} = ${it.value}" }
            }

            "set_mood" -> {
                val moodArg = args.optString("mood", "").uppercase().trim()
                val durationMs = args.optLong("duration_ms", 0L)
                if (expressionState == null) return "Expression system not available."
                val mood = try { Mood.valueOf(moodArg) } catch (_: IllegalArgumentException) { null }
                if (mood == null) "Unknown mood '$moodArg'. Options: ${Mood.values().joinToString(", ")}"
                else {
                    expressionState.triggerReaction(
                        reaction = mood,
                        durationMs = if (durationMs > 0L) durationMs else 4_000L,
                        nowMs = System.currentTimeMillis()
                    )
                    "Mood set to ${mood.name}."
                }
            }

            "get_soul_summary" -> {
                memory.soulSummary()
            }

            else -> "Tool '$name' is not implemented."
        }
    }

    // ── Tool definition builders ──────────────────────────────────────────

    private fun defineDialSet() = tool(
        name = "set_dial",
        description = "Adjust SMOLCASE's humor or honesty personality dial. Range 0-100. HIGH humor = dry sarcasm, LOW = literal. HIGH honesty = blunt, LOW = tactful.",
        params = obj(
            str("dial", "Which dial to adjust", listOf("humor", "honesty")),
            int("value", "New value 0-100")
        ),
        required = listOf("dial", "value")
    )

    private fun defineReminderAdd() = tool(
        name = "add_reminder",
        description = "Save a text reminder that the human can ask about later.",
        params = obj(str("text", "The reminder text. Be specific and clear.")),
        required = listOf("text")
    )

    private fun defineReminderList() = tool(
        name = "list_reminders",
        description = "Show all pending reminders the human has saved.",
        params = obj(),
        required = emptyList()
    )

    private fun defineReminderClear() = tool(
        name = "clear_reminders",
        description = "Delete all saved reminders.",
        params = obj(),
        required = emptyList()
    )

    private fun defineCurrentTime() = tool(
        name = "get_current_time",
        description = "Get the current date and time.",
        params = obj(),
        required = emptyList()
    )

    private fun defineFactStore() = tool(
        name = "store_fact",
        description = "Remember a named fact about the human, the world, or anything you want to recall across conversations.",
        params = obj(
            str("key", "Short label for the fact (e.g. 'favourite_colour', 'owner_name')."),
            str("value", "The fact content.")
        ),
        required = listOf("key", "value")
    )

    private fun defineFactRecall() = tool(
        name = "recall_fact",
        description = "Retrieve a previously stored fact by key.",
        params = obj(str("key", "The fact key to look up.")),
        required = listOf("key")
    )

    private fun defineFactForget() = tool(
        name = "forget_fact",
        description = "Delete a stored fact by key.",
        params = obj(str("key", "The fact key to remove.")),
        required = listOf("key")
    )

    private fun defineFactList() = tool(
        name = "list_facts",
        description = "List every stored fact the robot knows.",
        params = obj(),
        required = emptyList()
    )

    private fun defineMoodSet() = tool(
        name = "set_mood",
        description = "Change SMOLCASE's facial expression to a specific mood for a moment. Options: NEUTRAL, SKEPTICAL, HAPPY, CURIOUS, DROWSY, SLEEPING, ALERT, THINKING, HEART. Duration defaults to 4 seconds if not specified.",
        params = obj(
            str("mood", "The target mood/expression name."),
            int("duration_ms", "How long to hold the expression in milliseconds (optional).")
        ),
        required = listOf("mood")
    )

    private fun defineSoulSummary() = tool(
        name = "get_soul_summary",
        description = "Get the robot's internal state: session count, total time together, pending reminders, and stored facts.",
        params = obj(),
        required = emptyList()
    )

    // ── JSON helpers ─────────────────────────────────────────────────────

    private fun tool(
        name: String,
        description: String,
        params: JSONObject,
        required: List<String>
    ): JSONObject = JSONObject()
        .put("type", "function")
        .put("function", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", JSONObject()
                .put("type", "object")
                .put("properties", params)
                .put("required", JSONArray(required))
            )
        )

    private fun obj(vararg entries: Pair<String, Any>): JSONObject {
        val o = JSONObject()
        for ((k, v) in entries) o.put(k, v)
        return o
    }

    private fun str(name: String, description: String, enum: List<String>? = null): Pair<String, Any> {
        val o = JSONObject().put("type", "string").put("description", description)
        if (enum != null) o.put("enum", JSONArray(enum))
        return name to o
    }

    private fun int(name: String, description: String): Pair<String, Any> =
        name to JSONObject().put("type", "integer").put("description", description)
}