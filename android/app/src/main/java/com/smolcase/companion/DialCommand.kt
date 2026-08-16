package com.smolcase.companion

/**
 * Parses personality-dial voice commands: "set humor to 90",
 * "honesty 50 percent". Pure Kotlin, unit-tested; no Android imports.
 */
object DialCommand {

    enum class Dial { HUMOR, HONESTY }

    sealed class Result {
        data class Set(val dial: Dial, val value: Int) : Result()
        data class Rejected(val dial: Dial, val value: Int) : Result()
    }

    private val pattern = Regex(
        """(?:set\s+)?(humou?r|honesty)(?:\s+to)?\s+(\d{1,3})(?:\s*(?:percent|%))?"""
    )

    /** Parse a dial command, or null if the speech isn't one. */
    fun parse(rawText: String): Result? {
        val match = pattern.find(rawText.lowercase().trim()) ?: return null
        val dial = if (match.groupValues[1].startsWith("humo")) Dial.HUMOR else Dial.HONESTY
        val value = match.groupValues[2].toInt()
        return if (value in 0..100) Result.Set(dial, value) else Result.Rejected(dial, value)
    }
}
