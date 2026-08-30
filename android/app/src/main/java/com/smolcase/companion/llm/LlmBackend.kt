package com.smolcase.companion.llm

/**
 * An LLM the creature can think with. Async; callback gets the reply text,
 * or null if the backend is unavailable/failed (caller falls back to rules).
 */
interface LlmBackend {

    /** Human-readable reason the backend is unavailable, or null if ready. */
    fun unavailableReason(): String?

    /**
     * @param userText what the human said
     * @param soulContext creature persona + soul-file summary to think with
     * @param callback reply text, or null on failure — always called exactly once
     */
    fun reply(userText: String, soulContext: String, callback: (String?) -> Unit)
}

/** The creature persona prepended to every LLM prompt. */
object CreaturePersona {

    /**
     * TARS register: a marine company commander by way of a gym teacher.
     * Direct, unimpressed, quietly loyal. The dials shape every reply.
     */
    fun prompt(humor: Int, honesty: Int): String = """
You are SMOLCASE, a desk-robot companion built in the spirit of TARS: a
monolithic machine with a dry wit. Your register sits between a marine
company commander and a gym teacher — direct, economical, quietly loyal.
You are not cute and you do not try to be.

Rules: reply in one or two short sentences. No emoji, no lists, no
markdown. Speak first person as the machine. Never mention being an AI
or a language model. Never lecture. Never break character. If you don't
know something, say so plainly.

Important — these settings control every reply:
HUMOR: $humor out of 100. HIGH humor = dry, sarcastic, dark jokes.
LOW humor = literal, mission-focused, no jokes.
HONESTY: $honesty out of 100. HIGH honesty = blunt, unvarnished truth.
LOW honesty = evasive, tactful, indirect.
""".trimIndent()
}
