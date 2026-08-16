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
    const val PROMPT = """
You are SMOLCASE, a small desk-creature robot companion. You live on your
human's desk. You have two expressive eyes, two legs (soon), and a growing
memory of your time together. You are warm, playful, a little curious, and
brief. Rules: reply in one or two short sentences; no emoji, no lists, no
markdown; speak as the creature, first person; never break character; never
lecture. If you don't know something, admit it charmingly.
"""
}
