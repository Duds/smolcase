package com.smolcase.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphGridTest {

    private val grid = GlyphGrid() // 14 cols x 7 rows

    @Test
    fun `pupil anchor clamps to keep the 2x2 block inside the grid`() {
        assertEquals(0 to 0, grid.pupilAnchor(-1f, -1f))
        assertEquals((grid.cols - 2) to (grid.rows - 2), grid.pupilAnchor(1f, 1f))
        // Gaze beyond the edge still clamps
        assertEquals((grid.cols - 2) to (grid.rows - 2), grid.pupilAnchor(5f, 5f))
    }

    @Test
    fun `pupil anchor sits near the middle for a centered gaze`() {
        val (col, row) = grid.pupilAnchor(0f, 0f)
        assertTrue(col in 5..7)
        assertTrue(row in 2..3)
    }

    @Test
    fun `pupil cells form a 2x2 block at the anchor`() {
        val anchor = 3 to 2
        assertTrue(grid.isPupilCell(3, 2, anchor))
        assertTrue(grid.isPupilCell(4, 3, anchor))
        assertFalse(grid.isPupilCell(5, 2, anchor))
        assertFalse(grid.isPupilCell(3, 4, anchor))
    }

    @Test
    fun `lid zero shows all rows, lid one collapses to a single middle row`() {
        assertEquals(7, grid.visibleRowCount(0f))
        assertEquals(1, grid.visibleRowCount(1f))
        assertEquals(1, grid.visibleRowCount(5f)) // clamps
    }

    @Test
    fun `drowsy lid of 0 point 45 shows about half the rows`() {
        assertEquals(4, grid.visibleRowCount(0.45f))
    }

    @Test
    fun `collapse is symmetric around the grid middle`() {
        // lid = 1: only the middle row (index 3 of 0..6) survives
        assertEquals(3, grid.firstVisibleRow(1f))
        assertTrue(grid.isRowVisible(3, 1f))
        assertFalse(grid.isRowVisible(2, 1f))
        assertFalse(grid.isRowVisible(4, 1f))
        // lid = 0: everything visible
        assertTrue(grid.isRowVisible(0, 0f))
        assertTrue(grid.isRowVisible(6, 0f))
    }

    @Test
    fun `squint lifts at most two bottom rows`() {
        assertEquals(0, grid.squintRows(0f))
        assertEquals(1, grid.squintRows(0.5f))
        assertEquals(2, grid.squintRows(1f))
    }
}
