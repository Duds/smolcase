package com.smolcase.companion

import kotlin.math.roundToInt

/**
 * Pure geometry/state math for the TARS glyph-grid face.
 * No Android imports — unit-testable on the JVM.
 *
 * A grid is [cols] x [rows] monospace cells. The pupil is a 2x2 block of
 * full-block glyphs gliding inside the grid. The "lid" collapses rows
 * symmetrically toward the vertical middle (blink), droops (drowsy), or
 * leaves a single middle row (sleeping).
 */
class GlyphGrid(
    val cols: Int = DEFAULT_COLS,
    val rows: Int = DEFAULT_ROWS
) {

    /** Top-left cell of the 2x2 pupil block for a normalized gaze (-1..1). */
    fun pupilAnchor(nx: Float, ny: Float): Pair<Int, Int> {
        val maxCol = cols - PUPIL_COLS
        val maxRow = rows - PUPIL_ROWS
        val col = ((nx.coerceIn(-1f, 1f) + 1f) / 2f * maxCol).roundToInt().coerceIn(0, maxCol)
        val row = ((ny.coerceIn(-1f, 1f) + 1f) / 2f * maxRow).roundToInt().coerceIn(0, maxRow)
        return col to row
    }

    fun isPupilCell(col: Int, row: Int, anchor: Pair<Int, Int>): Boolean =
        col >= anchor.first && col < anchor.first + PUPIL_COLS &&
            row >= anchor.second && row < anchor.second + PUPIL_ROWS

    /**
     * Rows visible for [lid] (0 = open, 1 = collapsed). Collapse is
     * symmetric toward the middle; at least one thin middle row remains.
     */
    fun visibleRowCount(lid: Float): Int =
        (rows * (1f - lid.coerceIn(0f, 1f))).roundToInt().coerceIn(1, rows)

    /** First visible row index — visible rows center on the grid middle. */
    fun firstVisibleRow(lid: Float): Int = (rows - visibleRowCount(lid)) / 2

    fun isRowVisible(row: Int, lid: Float): Boolean {
        val first = firstVisibleRow(lid)
        return row >= first && row < first + visibleRowCount(lid)
    }

    /** Happy squint lifts bottom rows (the "^^" read). */
    fun squintRows(squint: Float): Int =
        (squint.coerceIn(0f, 1f) * SQUINT_MAX_ROWS).roundToInt()

    companion object {
        const val DEFAULT_COLS = 14
        const val DEFAULT_ROWS = 7
        const val PUPIL_COLS = 2
        const val PUPIL_ROWS = 2
        const val SQUINT_MAX_ROWS = 2
    }
}
