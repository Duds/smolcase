package com.smolcase.companion

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.smolcase.companion.llm.LlmSettings

/**
 * Creature settings — long-press the face to get here.
 * Programmatic UI (no layout XML): this screen is a tool, not a showcase.
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var settings: LlmSettings
    private lateinit var dials: PersonalityDials

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = LlmSettings(this)
        dials = PersonalityDials(this)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad, pad, pad)
        }

        fun label(text: String): TextView = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
        }

        fun field(hint: String, value: String): EditText = EditText(this).apply {
            this.hint = hint
            setText(value)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        root.addView(label("Who does the thinking?"))

        val radioRules = RadioButton(this).apply { text = "Rules only (offline, dumb but charming)" }
        val radioNano = RadioButton(this).apply { text = "Gemini Nano (on-device, private)" }
        val radioKimi = RadioButton(this).apply { text = "Kimi (cloud, smartest)" }
        val group = RadioGroup(this).apply {
            addView(radioRules); addView(radioNano); addView(radioKimi)
        }
        when (settings.backend) {
            LlmSettings.Backend.RULES -> group.check(radioRules.id)
            LlmSettings.Backend.NANO -> group.check(radioNano.id)
            LlmSettings.Backend.KIMI -> group.check(radioKimi.id)
        }
        listOf(radioRules, radioNano, radioKimi).forEach { it.setTextColor(Color.LTGRAY) }
        root.addView(group)

        root.addView(label("Kimi base URL"))
        val baseUrlField = field("https://api.moonshot.ai/v1", settings.kimiBaseUrl)
        root.addView(baseUrlField)

        root.addView(label("Kimi API key"))
        val apiKeyField = field("sk-…", settings.kimiApiKey)
        root.addView(apiKeyField)

        root.addView(label("Kimi model"))
        val modelField = field(LlmSettings.DEFAULT_MODEL, settings.kimiModel)
        root.addView(modelField)

        fun dialRow(labelText: String, initial: Int): Pair<SeekBar, TextView> {
            val valueLabel = label("$labelText: $initial%")
            val seek = SeekBar(this).apply {
                max = 100
                progress = initial
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                        valueLabel.text = "$labelText: $p%"
                    }
                    override fun onStartTrackingTouch(s: SeekBar) {}
                    override fun onStopTrackingTouch(s: SeekBar) {}
                })
            }
            root.addView(valueLabel)
            root.addView(seek)
            return seek to valueLabel
        }

        val (humorSeek, _) = dialRow("Humor", dials.humor)
        val (honestySeek, _) = dialRow("Honesty", dials.honesty)

        val status = TextView(this).apply { setTextColor(Color.GREEN) }
        val save = Button(this).apply {
            text = "Save"
            setOnClickListener {
                settings.backend = when (group.checkedRadioButtonId) {
                    radioNano.id -> LlmSettings.Backend.NANO
                    radioKimi.id -> LlmSettings.Backend.KIMI
                    else -> LlmSettings.Backend.RULES
                }
                settings.kimiBaseUrl = baseUrlField.text.toString()
                settings.kimiApiKey = apiKeyField.text.toString()
                settings.kimiModel = modelField.text.toString()
                dials.humor = humorSeek.progress
                dials.honesty = honestySeek.progress
                status.text = "Saved."
            }
        }
        root.addView(save)
        root.addView(status)

        setContentView(root)
    }
}
