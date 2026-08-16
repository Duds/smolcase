package com.smolcase.companion

import kotlin.random.Random

/**
 * The scrolling text living inside the glyph grids: real telemetry lines
 * when sources have them, weighted filler glyphs otherwise — quiet
 * instrumentation, never an error state. Pure Kotlin; time and randomness
 * are injected so it runs in JVM unit tests.
 */
class TelemetryFeed(
    private val lineLength: Int,
    private val sources: List<() -> String?> = emptyList(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val random: Random = Random.Default,
    private val clockLine: () -> String = { "" }
) {

    enum class Mode { NORMAL, ALL_FILLER, ALL_TELEMETRY, FROZEN }

    @Volatile var mode: Mode = Mode.NORMAL
    @Volatile var sleeping: Boolean = false

    private val lines = ArrayDeque<String>() // oldest first
    private var nextAdvanceAt = 0L
    private var sourceCursor = 0

    /** Called every frame by the view; advances the scroll when due. */
    fun tick() {
        if (mode == Mode.FROZEN) return
        val now = nowMs()
        if (nextAdvanceAt == 0L || now >= nextAdvanceAt) {
            advance()
            nextAdvanceAt = now + intervalMs()
        }
    }

    /** Push the next line onto the scroll. */
    fun advance() {
        lines.addLast(nextLine())
        while (lines.size > MAX_LINES) lines.removeFirst()
    }

    /** The newest [height] lines, oldest first, padded with blanks. */
    fun window(height: Int): List<String> {
        val tail = lines.toList().takeLast(height)
        return List(height - tail.size) { "" } + tail
    }

    fun nextLine(): String {
        if (sleeping) return if (random.nextInt(4) == 0) fillerLine() else clockLine()
        return when (mode) {
            Mode.ALL_FILLER -> fillerLine()
            Mode.ALL_TELEMETRY -> telemetryLine() ?: ""
            Mode.NORMAL -> telemetryLine() ?: fillerLine()
            Mode.FROZEN -> lines.lastOrNull() ?: ""
        }
    }

    /** Next non-blank source line (round-robin). Any failure → null. */
    private fun telemetryLine(): String? {
        if (sources.isEmpty()) return null
        repeat(sources.size) {
            val src = sources[sourceCursor % sources.size]
            sourceCursor++
            val line = try { src() } catch (e: Exception) { null }
            if (!line.isNullOrBlank()) return line.take(lineLength)
        }
        return null
    }

    /** Weighted low-density filler: ▓▒░:· with occasional hex pairs. */
    fun fillerLine(): String = buildString {
        while (length < lineLength) {
            if (random.nextInt(8) == 0 && length + 3 <= lineLength) {
                append(random.nextInt(256).toString(16).uppercase().padStart(2, '0'))
                append(' ')
            } else {
                append(fillerChar())
            }
        }
    }.take(lineLength)

    private fun fillerChar(): Char {
        val total = FILLER_WEIGHTS.sumOf { it.second }
        var roll = random.nextInt(total)
        for ((ch, weight) in FILLER_WEIGHTS) {
            if (roll < weight) return ch
            roll -= weight
        }
        return '·'
    }

    private fun intervalMs(): Long = if (sleeping) 2_000L else 500L // ~2 rows/s awake

    companion object {
        private const val MAX_LINES = 32
        private val FILLER_WEIGHTS = listOf(
            '░' to 40, '·' to 25, ':' to 15, '▒' to 12, '▓' to 5, ' ' to 3
        )
    }
}
