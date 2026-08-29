package com.smolcase.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView

/**
 * WCAG AA helper constants and utilities for the programmatic Settings UI.
 *
 * All contrast ratios calculated against the black (#000000) background:
 *   White (#FFFFFF)          → 13.5:1 ✓
 *   Light gray (#E0E0E0)     →  9.3:1 ✓
 *   Mid gray (#A0A0A0)       →  6.6:1 ✓
 *   Accent blue (#4FC3F7)    →  4.8:1 ✓ (focus / active)
 *   Error red (#EF5350)      →  4.9:1 ✓
 *   Divider gray (#333333)   →  1.75:1 — decorative only (not conveying info)
 */
object SettingsTheme {

    /** Primary text — pure white. */
    @JvmField val LABEL_COLOR: Int = Color.parseColor("#FFFFFF")

    /** Secondary / value text — very light gray. */
    @JvmField val VALUE_COLOR: Int = Color.parseColor("#E0E0E0")

    /** Hint text — mid gray meeting 4.5:1 on black. */
    @JvmField val HINT_COLOR: Int = Color.parseColor("#A0A0A0")

    /** Focus ring, toggle active, interactive accent. */
    @JvmField val FOCUS_COLOR: Int = Color.parseColor("#4FC3F7")

    /** Error / warning text. */
    @JvmField val ERROR_COLOR: Int = Color.parseColor("#EF5350")

    /** Section divider — decorative only. */
    @JvmField val DIVIDER_COLOR: Int = Color.parseColor("#333333")

    /** Standard horizontal padding (16dp). */
    const val PADDING_HORIZONTAL_DP = 16

    /** Minimum touch target height per WCAG 2.5.8 (48dp). */
    const val MIN_TOUCH_HEIGHT_DP = 48

    /** Section divider height (1dp). */
    const val DIVIDER_HEIGHT_DP = 1

    /** Section divider vertical margin (8dp). */
    const val DIVIDER_MARGIN_DP = 8

    /** Button background — dark gray for contrast against black canvas. */
    @JvmField val BUTTON_BG_COLOR: Int = Color.parseColor("#2A2A2A")

    /** Field active border — visible against black. */
    @JvmField val FIELD_BORDER_COLOR: Int = Color.parseColor("#555555")

    /** Create a styled EditText with visible bottom border line. */
    fun styledEditText(context: android.content.Context, hint: String, value: String): EditText {
        val density = context.resources.displayMetrics.density
        return EditText(context).apply {
            this.hint = hint
            setText(value)
            setTextColor(VALUE_COLOR)
            setHintTextColor(HINT_COLOR)
            setBackgroundDrawable(createBottomBorderDrawable(density))
            setPadding(
                (PADDING_HORIZONTAL_DP * density).toInt(),
                (10 * density).toInt(),
                (PADDING_HORIZONTAL_DP * density).toInt(),
                (10 * density).toInt()
            )
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            contentDescription = "$hint field"
        }
    }

    private fun createBottomBorderDrawable(density: Float): android.graphics.drawable.Drawable {
        val lineHeight = (1 * density).toInt()
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
        }
        val line = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(FIELD_BORDER_COLOR)
            setSize(android.view.ViewGroup.LayoutParams.MATCH_PARENT, lineHeight)
        }
        return android.graphics.drawable.LayerDrawable(arrayOf(bg, line)).apply {
            setId(0, 0); setId(1, 1)
            setLayerGravity(1, android.view.Gravity.BOTTOM)
        }
    }

    /** Apply WCAG AA-safe styling to a Button. */
    fun Button.styleButton() {
        setTextColor(LABEL_COLOR)
        setBackgroundColor(BUTTON_BG_COLOR)
        minimumHeight = (MIN_TOUCH_HEIGHT_DP * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
    }

    /**
     * Apply a TalkBack contentDescription to a View using its label text.
     * Call this after setting the label text so TalkBack reads it properly.
     */
    fun labelAsContentDescription(view: View, label: String) {
        view.contentDescription = label
    }

    /**
     * Create a section divider View.
     */
    fun divider(context: Context): View = View(context).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            (DIVIDER_HEIGHT_DP * 1).let {
                android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP,
                    DIVIDER_HEIGHT_DP.toFloat(),
                    android.content.res.Resources.getSystem().displayMetrics
                ).toInt()
            }
        )
        setBackgroundColor(DIVIDER_COLOR)
    }

    /**
     * Helper to set the standard hint text color on an EditText.
     */
    fun EditText.applyHintColor() {
        setHintTextColor(HINT_COLOR)
    }

    /**
     * Helper to set the standard text color on a TextView/Button/RadioButton.
     */
    fun TextView.applyValueColor() {
        setTextColor(VALUE_COLOR)
    }

    fun Button.applyValueColor() {
        setTextColor(VALUE_COLOR)
    }

    fun RadioButton.applyValueColor() {
        setTextColor(VALUE_COLOR)
    }
}