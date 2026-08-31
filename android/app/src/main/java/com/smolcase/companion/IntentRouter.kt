package com.smolcase.companion

/**
 * Tiny intent router for the Stage 3 command set.
 * Deliberately small vocabulary — the creature is charming because it's
 * simple, not because it's smart. (Kimi LLM layer comes later.)
 */
object IntentRouter {

    data class Reply(
        val say: String,
        val excitement: Float = 0.3f,
        val happy: Float = 0.5f,
        val blinks: Int = 1
    )

    /**
     * Route known commands. Returns null for unrecognized speech so the
     * caller can fall through to the LLM layer.
     */
    fun route(rawText: String, memory: MemoryStore, dials: PersonalityDials): Reply? {
        val text = rawText.lowercase().trim()

        // Personality dials: "set humor to 90" / "honesty 50 percent"
        DialCommand.parse(text)?.let { result ->
            return when (result) {
                is DialCommand.Result.Set -> {
                    when (result.dial) {
                        DialCommand.Dial.HUMOR -> dials.humor = result.value
                        DialCommand.Dial.HONESTY -> dials.honesty = result.value
                    }
                    val name = if (result.dial == DialCommand.Dial.HUMOR) "Humor" else "Honesty"
                    Reply(
                        "$name setting at ${result.value} percent.",
                        excitement = 0.2f, happy = 0.2f, blinks = 0
                    )
                }
                is DialCommand.Result.Rejected ->
                    Reply(
                        "Dials run zero to one hundred. ${result.value} is not on the dial.",
                        excitement = 0.1f, happy = 0.1f, blinks = 0
                    )
            }
        }

        // "remind me to call mum" / "remind me call mum"
        val remindMatch = Regex("""remind me(?: to)? (.+)""").find(text)
        if (remindMatch != null) {
            val what = remindMatch.groupValues[1].trim()
            memory.addReminder(what)
            return Reply("Got it. I'll remember to $what.", happy = 0.8f, blinks = 2)
        }

        return when {
            // "clear my reminders" / "clear"+"reminder" must match before
            // "my reminders" below, or the user can never clear their list.
            text.contains("clear") && text.contains("reminder") ||
                text.contains("forget everything") -> {
                memory.clearReminders()
                Reply("Done. Fresh start.", happy = 0.7f, blinks = 2)
            }

            text.contains("what's next") || text.contains("whats next") ||
                text.contains("what do i have") || text.contains("my reminders") -> {
                val reminders = memory.getReminders()
                if (reminders.isEmpty()) {
                    Reply("Nothing on your list. You're free.", happy = 0.6f)
                } else {
                    val list = reminders.take(3).joinToString(", then ")
                    Reply("Next up: $list.", happy = 0.5f)
                }
            }

            text.contains("good morning") ->
                Reply("Good morning!", excitement = 1.0f, happy = 1.0f, blinks = 3)

            text.contains("good night") ->
                Reply("Good night.", excitement = 0.2f, happy = 0.8f, blinks = 1)

            text == "hello" || text == "hi" || text.startsWith("hey") || text.contains("hello") ->
                Reply("Hi!", excitement = 0.6f, happy = 0.9f, blinks = 2)

            text.contains("how are you") ->
                Reply("Better now that you're here.", excitement = 0.5f, happy = 1.0f, blinks = 1)

            text.contains("thank") ->
                Reply("You're welcome.", excitement = 0.4f, happy = 1.0f, blinks = 1)

            else -> null // unknown — let the LLM think
        }
    }

    /** The dumb-but-charming offline fallback when no LLM is available. */
    fun unknownReply(): Reply =
        Reply("You'll have to rephrase that.", excitement = 0.2f, happy = 0.1f, blinks = 1)
}
