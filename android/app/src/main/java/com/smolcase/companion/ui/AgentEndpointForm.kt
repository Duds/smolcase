package com.smolcase.companion.ui

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.smolcase.companion.llm.LlmSettings

/**
 * Reusable form for configuring an OpenAI-compatible agent endpoint.
 * Used in Settings for the primary thinking backend (P1-4).
 *
 * Produces a vertical LinearLayout with:
 *   - Provider label (free text)
 *   - Base URL
 *   - API key (masked input)
 *   - Model override
 *   - Max tokens seekbar (64–1024)
 *   - Temperature seekbar (0–200 = 0.00–2.00)
 */
class AgentEndpointForm(
    context: Context,
    private val settings: LlmSettings
) : LinearLayout(context) {

    private val fields = mutableMapOf<String, ViewGroup>()
    private var providerLabelField: EditText
    private var baseUrlField: EditText
    private var apiKeyField: EditText
    private var modelField: EditText
    private val maxTokensValue: TextView
    private val maxTokensSeek: SeekBar
    private val temperatureValue: TextView
    private val temperatureSeek: SeekBar

    private val density = resources.displayMetrics.density
    private val vPad = (4 * density).toInt()

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        providerLabelField = addField("Provider label", settings.agentProviderLabel, InputType.TYPE_CLASS_TEXT)
        baseUrlField = addField("Base URL", settings.agentBaseUrl, InputType.TYPE_CLASS_TEXT)
        apiKeyField = addField("API key", settings.agentApiKey, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        modelField = addField("Model", settings.agentModel, InputType.TYPE_CLASS_TEXT)

        // ── Max tokens ──
        val maxTokensInitial = settings.agentMaxTokens
        maxTokensValue = TextView(context).apply {
            text = "Max tokens: $maxTokensInitial"
            setTextColor(SettingsTheme.VALUE_COLOR)
            textSize = 14f
        }
        maxTokensSeek = SeekBar(context).apply {
            max = 960 // 1024 - 64
            progress = maxTokensInitial - 64
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            )
            contentDescription = "Max tokens: $maxTokensInitial"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    val v = p + 64
                    maxTokensValue.text = "Max tokens: $v"
                    s.contentDescription = "Max tokens: $v"
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        addView(maxTokensValue)
        addView(maxTokensSeek)

        // ── Temperature ──
        val tempInitial = settings.agentTemperature
        temperatureValue = TextView(context).apply {
            text = "Temperature: ${"%.2f".format(tempInitial / 100.0)}"
            setTextColor(SettingsTheme.VALUE_COLOR)
            textSize = 14f
        }
        temperatureSeek = SeekBar(context).apply {
            max = 200
            progress = tempInitial
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            )
            contentDescription = "Temperature: ${"%.2f".format(tempInitial / 100.0)}"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    val v = "%.2f".format(p / 100.0)
                    temperatureValue.text = "Temperature: $v"
                    s.contentDescription = "Temperature: $v"
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        addView(temperatureValue)
        addView(temperatureSeek)
    }

    /** Read current values from the form and persist them to LlmSettings. */
    fun save() {
        settings.agentProviderLabel = providerLabelField.text.toString()
        settings.agentBaseUrl = baseUrlField.text.toString()
        settings.agentApiKey = apiKeyField.text.toString()
        settings.agentModel = modelField.text.toString()
        settings.agentMaxTokens = maxTokensSeek.progress + 64
        settings.agentTemperature = temperatureSeek.progress
    }

    /** Current form values for atomic save. */
    val currentBaseUrl: String get() = baseUrlField.text.toString()
    val currentApiKey: String get() = apiKeyField.text.toString()
    val currentModel: String get() = modelField.text.toString()
    val currentMaxTokens: Int get() = maxTokensSeek.progress + 64
    val currentTemperature: Int get() = temperatureSeek.progress

    /** Whether the form differs from the persisted settings. */
    fun hasChanges(): Boolean =
        providerLabelField.text.toString() != settings.agentProviderLabel ||
            baseUrlField.text.toString() != settings.agentBaseUrl ||
            apiKeyField.text.toString() != settings.agentApiKey ||
            modelField.text.toString() != settings.agentModel ||
            maxTokensSeek.progress + 64 != settings.agentMaxTokens ||
            temperatureSeek.progress != settings.agentTemperature

    private fun addField(
        labelText: String,
        initialValue: String,
        inputType: Int
    ): EditText {
        val label = TextView(context).apply {
            text = labelText
            setTextColor(SettingsTheme.LABEL_COLOR)
            textSize = 14f
            setPadding(0, vPad, 0, 0)
            contentDescription = labelText
        }
        addView(label)

        return EditText(context).apply {
            hint = labelText
            setText(initialValue)
            setTextColor(SettingsTheme.VALUE_COLOR)
            setHintTextColor(SettingsTheme.HINT_COLOR)
            this.inputType = inputType
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            contentDescription = "$labelText field. Current: $initialValue"
            this@AgentEndpointForm.addView(this)
        }
    }
}