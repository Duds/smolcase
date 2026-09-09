package com.smolcase.companion.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView

/** Visual tokens for the cybernetic companion settings deck. */
object SettingsTheme {
    @JvmField val BACKGROUND = Color.parseColor("#0F131C")
    @JvmField val OLED = Color.parseColor("#05070B")
    @JvmField val DECK = Color.parseColor("#0E1420")
    @JvmField val PANEL = Color.parseColor("#141C2B")
    @JvmField val BORDER = Color.parseColor("#1E2A3E")
    @JvmField val CYAN = Color.parseColor("#00F0FF")
    @JvmField val CORAL = Color.parseColor("#F43F5E")
    @JvmField val LABEL_COLOR = Color.parseColor("#F1F5F9")
    @JvmField val VALUE_COLOR = Color.parseColor("#DFE2EE")
    @JvmField val HINT_COLOR = Color.parseColor("#94A3B8")
    @JvmField val TEXT_DIM = Color.parseColor("#64748B")
    @JvmField val DIVIDER_COLOR = BORDER

    const val PADDING_HORIZONTAL_DP = 16
    const val MIN_TOUCH_HEIGHT_DP = 48

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun surface(color: Int, radiusPx: Int, stroke: Int = BORDER): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx.toFloat()
        setStroke(1, stroke)
    }

    fun card(context: Context): GradientDrawable = surface(DECK, dp(context, 8), BORDER)

    fun fieldBackground(context: Context): GradientDrawable = surface(OLED, dp(context, 4), BORDER)

    fun styledEditText(context: Context, hint: String, value: String): EditText = EditText(context).apply {
        this.hint = hint
        setText(value)
        setTextColor(VALUE_COLOR)
        setHintTextColor(HINT_COLOR)
        setTextSize(13f)
        setSingleLine(true)
        background = fieldBackground(context)
        setPadding(dp(context, 12), 0, dp(context, 12), 0)
        minimumHeight = dp(context, MIN_TOUCH_HEIGHT_DP)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, MIN_TOUCH_HEIGHT_DP))
        contentDescription = "$hint field"
    }

    fun styleSeekBar(seek: SeekBar) {
        seek.progressTintList = ColorStateList.valueOf(CYAN)
        seek.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#31353E"))
        seek.thumbTintList = ColorStateList.valueOf(CYAN)
        seek.splitTrack = false
        seek.minimumHeight = dp(seek.context, MIN_TOUCH_HEIGHT_DP)
    }

    fun styleButton(button: Button, background: Int = PANEL, foreground: Int = LABEL_COLOR) {
        button.setTextColor(foreground)
        button.background = surface(background, dp(button.context, 4), if (background == CYAN) CYAN else BORDER)
        button.minimumHeight = dp(button.context, MIN_TOUCH_HEIGHT_DP)
        button.setPadding(dp(button.context, 12), 0, dp(button.context, 12), 0)
    }

    fun divider(context: Context): View = View(context).apply {
        setBackgroundColor(DIVIDER_COLOR)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1))
    }
}