package com.smolcase.companion.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.marginTop

/**
 * A collapsible section for the programmatic Settings UI.
 *
 * Usage:
 *   val section = SettingsSection(context, "Thinking Engine", initiallyExpanded = true)
 *   section.addContent(childView1)
 *   section.addContent(childView2)
 *   root.addView(section)
 */
class SettingsSection(context: android.content.Context, title: String, initiallyExpanded: Boolean = true) :
    LinearLayout(context) {

    private val contentContainer: LinearLayout
    private var expanded = initiallyExpanded

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // ── Section header (tappable to expand/collapse) ──
        val density = resources.displayMetrics.density
        val hPad = (SettingsTheme.PADDING_HORIZONTAL_DP * density).toInt()
        val vPad = (12 * density).toInt()

        val header = TextView(context).apply {
            text = "▾ $title"
            setTextColor(SettingsTheme.LABEL_COLOR)
            textSize = 18f
            setPadding(hPad, vPad, hPad, vPad)
            contentDescription = "Section: $title. Double-tap to toggle."
            setOnClickListener { toggle() }
        }
        addView(header)

        // ── Collapsible content wrapper ──
        contentContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(
                hPad,
                0,
                hPad,
                (16 * density).toInt()
            )
        }
        addView(contentContainer)

        // ── Section divider ──
        addView(SettingsTheme.divider(context))

        if (!initiallyExpanded) {
            contentContainer.visibility = GONE
        }
    }

    /** Add a child view to the collapsible content area. */
    fun addContent(child: View) {
        contentContainer.addView(child)
    }

    /** Add a label inside the content area. */
    fun addLabel(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(SettingsTheme.VALUE_COLOR)
        textSize = 14f
        val density = resources.displayMetrics.density
        setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        contentDescription = text
        contentContainer.addView(this)
    }

    /** Toggle between expanded and collapsed. */
    fun toggle() {
        expanded = !expanded
        contentContainer.visibility = if (expanded) VISIBLE else GONE
        // Update header indicator
        val header = getChildAt(0) as? TextView ?: return
        val rawTitle = header.text.toString().removePrefix("▾ ").removePrefix("▸ ")
        header.text = if (expanded) "▾ $rawTitle" else "▸ $rawTitle"
    }

    /** Programmatically expand this section. */
    fun expand() {
        if (!expanded) toggle()
    }

    /** Programmatically collapse this section. */
    fun collapse() {
        if (expanded) toggle()
    }

    val isExpanded: Boolean get() = expanded

    /** Remove all content views from this section. */
    fun clearContent() {
        contentContainer.removeAllViews()
    }
}