package com.smolcase.companion

import android.app.AlertDialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.smolcase.companion.cloud.CloudVision
import com.smolcase.companion.llm.LlmSettings
import com.smolcase.companion.sensors.CreatureSenses
import com.smolcase.companion.sensors.SensorConfig
import com.smolcase.companion.ui.AgentEndpointForm
import com.smolcase.companion.ui.SensorToggleGroup
import com.smolcase.companion.ui.SettingsSection
import com.smolcase.companion.ui.SettingsTheme

/**
 * Creature settings — long-press the face to get here.
 *
 * Programmatic UI (no layout XML) with WCAG AA contrast, touch targets,
 * content descriptions, and section-based collapsible layout.
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var settings: LlmSettings
    private lateinit var creatureSettings: CreatureSettings
    private lateinit var dials: PersonalityDials

    // Voice picker state: a throwaway TTS engine to enumerate and preview
    private var voiceTts: TextToSpeech? = null
    private var englishVoices: List<Voice> = emptyList()
    private var voiceIndex = -1

    // Form fields for dirty tracking
    private lateinit var backendGroup: RadioGroup
    private lateinit var agentForm: AgentEndpointForm
    private var humorSeek: SeekBar? = null
    private var honestySeek: SeekBar? = null
    private var savedHumor = PersonalityDials.DEFAULT_HUMOR
    private var savedHonesty = PersonalityDials.DEFAULT_HONESTY
    private var savedBackend: LlmSettings.Backend = LlmSettings.Backend.AGENT
    private var dirty = false
    private var creatureSenses: com.smolcase.companion.sensors.CreatureSenses? = null
    private var sensorGroup: com.smolcase.companion.ui.SensorToggleGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = LlmSettings(this)
        creatureSettings = CreatureSettings(this)
        dials = PersonalityDials(this)

        savedHumor = dials.humor
        savedHonesty = dials.honesty
        savedBackend = settings.backend

        val density = resources.displayMetrics.density

        // ── Back-press unsaved warning ──
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!dirty) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
                AlertDialog.Builder(this@SettingsActivity).apply {
                    setTitle("Unsaved changes")
                    setMessage("You have unsaved settings. Discard them?")
                    setPositiveButton("Discard") { _, _ -> finish() }
                    setNegativeButton("Keep editing", null)
                    show()
                }
            }
        })

        // ── Root: ScrollView > LinearLayout ──
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(scrollContent)
        }.let { root.addView(it) }

        val hPad = (SettingsTheme.PADDING_HORIZONTAL_DP * density).toInt()
        val vPad = (16 * density).toInt()
        scrollContent.setPadding(hPad, vPad, hPad, vPad)

        // ── Helper: create a label ──
        fun label(text: String): TextView = TextView(this).apply {
            this.text = text
            setTextColor(SettingsTheme.LABEL_COLOR)
            textSize = 16f
            contentDescription = text
        }

        fun field(hint: String, value: String) = SettingsTheme.styledEditText(this, hint, value)

        // =================================================================
        // SECTION: Thinking Engine
        // =================================================================
        val thinkingSection = SettingsSection(this, "Thinking Engine", initiallyExpanded = true)

        val radioAgent = RadioButton(this).apply {
            id = android.view.View.generateViewId()
            text = "Agent (cloud, any OpenAI-compatible)"
            setTextColor(SettingsTheme.VALUE_COLOR)
            contentDescription = "Agent cloud endpoint"
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
        }
        val radioNano = RadioButton(this).apply {
            id = android.view.View.generateViewId()
            text = "Gemini Nano (on-device, private)"
            setTextColor(SettingsTheme.VALUE_COLOR)
            contentDescription = "Gemini Nano on device"
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
        }
        val radioGemma = RadioButton(this).apply {
            id = android.view.View.generateViewId()
            text = "Gemma 4 E2B (on-device, sideloaded — unstable)"
            setTextColor(android.graphics.Color.parseColor("#888888"))
            contentDescription = "Gemma 4 on device, use at your own risk"
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
        }
        val radioRules = RadioButton(this).apply {
            id = android.view.View.generateViewId()
            text = "Rules only (offline, no AI)"
            setTextColor(SettingsTheme.VALUE_COLOR)
            contentDescription = "Rules only backend"
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
        }
        backendGroup = RadioGroup(this).apply {
            addView(radioAgent); addView(radioNano); addView(radioGemma); addView(radioRules)
            setOnCheckedChangeListener { _, _ ->
                markDirty()
                if (::agentForm.isInitialized) {
                    agentForm.visibility = if (checkedRadioButtonId == radioAgent.id) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
        when (savedBackend) {
            LlmSettings.Backend.AGENT -> backendGroup.check(radioAgent.id)
            LlmSettings.Backend.NANO -> backendGroup.check(radioNano.id)
            LlmSettings.Backend.GEMMA -> backendGroup.check(radioGemma.id)
            LlmSettings.Backend.RULES -> backendGroup.check(radioRules.id)
        }
        thinkingSection.addContent(backendGroup)

        // Agent endpoint form (collapsible under the radio)
        // Note under AGENT radio
        val agentNote = TextView(this).apply {
            text = "Requires an API key from OpenRouter, Together, or any OpenAI-compatible provider"
            setTextColor(SettingsTheme.HINT_COLOR)
            textSize = 12f
            setPadding(0, 0, 0, (8 * density).toInt())
            contentDescription = "Agent requires API key"
        }
        thinkingSection.addContent(agentNote)

        agentForm = AgentEndpointForm(this, settings)
        agentForm.visibility = if (savedBackend == LlmSettings.Backend.AGENT) android.view.View.VISIBLE else android.view.View.GONE
        thinkingSection.addContent(agentForm)

        scrollContent.addView(thinkingSection) // auto-adds its own divider

        // =================================================================
        // SECTION: Personality Dials
        // =================================================================
        val dialsSection = SettingsSection(this, "Personality", initiallyExpanded = true)

        fun dialRow(labelText: String, initial: Int): Pair<SeekBar, TextView> {
            val valueLabel = label("$labelText: $initial%")
            val seek = SeekBar(this).apply {
                max = 100
                progress = initial
                minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
                contentDescription = "$labelText $initial percent"
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                        valueLabel.text = "$labelText: $p%"
                        s.contentDescription = "$labelText $p percent"
                        markDirty()
                    }
                    override fun onStartTrackingTouch(s: SeekBar) {}
                    override fun onStopTrackingTouch(s: SeekBar) {}
                })
            }
            dialsSection.addContent(valueLabel)
            dialsSection.addContent(seek)
            return seek to valueLabel
        }

        val (humor, _) = dialRow("Humor", savedHumor)
        humorSeek = humor
        val (honesty, _) = dialRow("Honesty", savedHonesty)
        honestySeek = honesty

        scrollContent.addView(dialsSection)

        // =================================================================
        // SECTION: Voice
        // =================================================================
        val voiceSection = SettingsSection(this, "Voice", initiallyExpanded = false)

        val voiceLabel = label("Voice: ${dials.voiceName ?: "auto (male)"}")
        voiceSection.addContent(voiceLabel)

        voiceTts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                voiceTts?.let { t ->
                    t.setPitch(0.85f)
                    t.setSpeechRate(0.92f)
                    englishVoices = t.voices
                        .filter { it.locale.language == "en" }
                        .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
                    voiceIndex = englishVoices.indexOfFirst { it.name == dials.voiceName }
                }
            }
        }

        val nextVoice = Button(this).apply {
            setTextColor(SettingsTheme.LABEL_COLOR)
            setBackgroundColor(SettingsTheme.BUTTON_BG_COLOR)
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            text = "Next voice"
            contentDescription = "Cycle to next voice"
            setOnClickListener {
                val t = voiceTts ?: return@setOnClickListener
                if (englishVoices.isEmpty()) return@setOnClickListener
                voiceIndex = (voiceIndex + 1) % englishVoices.size
                val v = englishVoices[voiceIndex]
                t.voice = v
                dials.voiceName = v.name // saved immediately, not on Save
                voiceLabel.text = "Voice: ${v.name}"
                voiceLabel.contentDescription = "Voice: ${v.name}"
                t.speak("Systems nominal.", TextToSpeech.QUEUE_FLUSH, null, "voice-sample")
            }
        }
        voiceSection.addContent(nextVoice)

        scrollContent.addView(voiceSection)

        // =================================================================
        // SECTION: Cloud TTS
        // =================================================================
        val cloudTtsSection = SettingsSection(this, "Cloud TTS", initiallyExpanded = false)

        val ttsToggle = android.widget.Switch(this).apply {
            isChecked = settings.cloudTtsEnabled
            text = "Enable cloud TTS"
            setTextColor(SettingsTheme.VALUE_COLOR)
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            contentDescription = "Enable cloud text to speech"
            setOnCheckedChangeListener { _, _ -> markDirty() }
            // Show/hide config when toggling
        }
        cloudTtsSection.addContent(ttsToggle)

        fun ttsField(hint: String, value: String) = SettingsTheme.styledEditText(this, hint, value)

        // Config fields: only visible when toggle is on
        val ttsConfig = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (settings.cloudTtsEnabled) android.view.View.VISIBLE else android.view.View.GONE
        }

        cloudTtsSection.addLabel("Base URL")
        val ttsUrlField = ttsField("https://api.elevenlabs.io/v1", settings.cloudTtsBaseUrl)
        ttsUrlField.setOnKeyListener { _, _, _ -> markDirty(); false }
        ttsConfig.addView(ttsUrlField)

        cloudTtsSection.addLabel("API key")
        val ttsKeyField = ttsField("xi-…", settings.cloudTtsApiKey).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        ttsConfig.addView(ttsKeyField)

        cloudTtsSection.addLabel("Voice ID")
        val ttsVoiceField = ttsField("21m00Tcm4TlvDq8ikWAM", settings.cloudTtsVoiceId)
        ttsConfig.addView(ttsVoiceField)

        cloudTtsSection.addLabel("Model ID")
        val ttsModelField = ttsField("eleven_monolingual_v1", settings.cloudTtsModelId)
        ttsConfig.addView(ttsModelField)

        cloudTtsSection.addLabel("Provider label")
        val ttsProviderField = ttsField("ElevenLabs", settings.cloudTtsProviderLabel)
        ttsConfig.addView(ttsProviderField)

        // Stability seekbar
        fun seekRow(labelText: String, initial: Int): SeekBar {
            val valueLabel = label("$labelText: $initial%")
            val seek = SeekBar(this).apply {
                max = 100
                progress = initial
                minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
                contentDescription = "$labelText $initial percent"
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                        valueLabel.text = "$labelText: $p%"
                        markDirty()
                    }
                    override fun onStartTrackingTouch(s: SeekBar) {}
                    override fun onStopTrackingTouch(s: SeekBar) {}
                })
            }
            cloudTtsSection.addContent(valueLabel)
            cloudTtsSection.addContent(seek)
            return seek
        }
        val ttsStabilitySeek = seekRow("Stability", settings.cloudTtsStability)
        val ttsSimilaritySeek = seekRow("Similarity", settings.cloudTtsSimilarity)

        cloudTtsSection.addContent(ttsConfig)

        ttsToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            ttsConfig.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            markDirty()
        }

        scrollContent.addView(cloudTtsSection)

        // =================================================================
        // SECTION: Cloud Vision
        // =================================================================
        val visionSection = SettingsSection(this, "Cloud Vision", initiallyExpanded = false)

        val visionToggle = android.widget.Switch(this).apply {
            isChecked = settings.cloudVisionEnabled
            text = "Enable cloud vision"
            setTextColor(SettingsTheme.VALUE_COLOR)
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            contentDescription = "Enable cloud vision analysis"
        }
        visionSection.addContent(visionToggle)

        val visionConfig = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (settings.cloudVisionEnabled) android.view.View.VISIBLE else android.view.View.GONE
        }

        fun visionField(hint: String, value: String) = SettingsTheme.styledEditText(this, hint, value)

        visionSection.addLabel("Base URL")
        val visionUrlField = visionField("https://openrouter.ai/api/v1", settings.cloudVisionBaseUrl)
        visionConfig.addView(visionUrlField)

        visionSection.addLabel("API key")
        val visionKeyField = visionField("sk-…", settings.cloudVisionApiKey).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        visionConfig.addView(visionKeyField)

        visionSection.addLabel("Model")
        val visionModelField = visionField("gpt-4o", settings.cloudVisionModel)
        visionConfig.addView(visionModelField)

        visionSection.addContent(visionConfig)

        visionToggle.setOnCheckedChangeListener { _, isChecked ->
            visionConfig.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            markDirty()
        }

        scrollContent.addView(visionSection)

        // =================================================================
        // SECTION: Cloud Reply Generation
        // =================================================================
        val replySection = SettingsSection(this, "Cloud Reply Gen", initiallyExpanded = false)

        val replyToggle = android.widget.Switch(this).apply {
            isChecked = settings.cloudReplyGenEnabled
            text = "Enable cloud reply generation"
            setTextColor(SettingsTheme.VALUE_COLOR)
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            contentDescription = "Use cloud AI for reply generation"
        }
        replySection.addContent(replyToggle)

        val replyConfig = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (settings.cloudReplyGenEnabled) android.view.View.VISIBLE else android.view.View.GONE
        }

        fun replyField(hint: String, value: String) = SettingsTheme.styledEditText(this, hint, value)

        replySection.addLabel("Base URL")
        val replyUrlField = replyField("https://openrouter.ai/api/v1", settings.cloudReplyGenBaseUrl)
        replyConfig.addView(replyUrlField)

        replySection.addLabel("API key")
        val replyKeyField = replyField("sk-…", settings.cloudReplyGenApiKey).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        replyConfig.addView(replyKeyField)

        replySection.addLabel("Model")
        val replyModelField = replyField("mistralai/mixtral-8x22b-instruct", settings.cloudReplyGenModel)
        replyConfig.addView(replyModelField)

        replySection.addContent(replyConfig)

        replyToggle.setOnCheckedChangeListener { _, isChecked ->
            replyConfig.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            markDirty()
        }

        scrollContent.addView(replySection)

        // =================================================================
        // SECTION: Sensors
        // =================================================================
        val sensorSection = SettingsSection(this, "Sensors", initiallyExpanded = false)

        val sensorConfig = SensorConfig(this)
        creatureSenses = CreatureSenses(this, sensorConfig)
        sensorGroup = SensorToggleGroup(this, sensorConfig, creatureSenses!!)
        sensorSection.addContent(sensorGroup!!)

        // Start live readouts when activity is created; stop on destroy
        creatureSenses!!.start()
        sensorGroup!!.startReadouts()

        scrollContent.addView(sensorSection)

        // =================================================================
        // Save button + status
        // =================================================================
        val status = TextView(this).apply {
            setTextColor(SettingsTheme.VALUE_COLOR)
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
        }
        val save = Button(this).apply {
            setTextColor(SettingsTheme.LABEL_COLOR)
            setBackgroundColor(SettingsTheme.BUTTON_BG_COLOR)
            minimumHeight = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
            text = "Save"
            contentDescription = "Save all settings"
            setOnClickListener {
                val backend = when (backendGroup.checkedRadioButtonId) {
                    radioNano.id -> LlmSettings.Backend.NANO
                    radioAgent.id -> LlmSettings.Backend.AGENT
                    radioGemma.id -> LlmSettings.Backend.GEMMA
                    else -> LlmSettings.Backend.RULES
                }
                val humor = humorSeek?.progress ?: 50
                val honesty = honestySeek?.progress ?: 50

                creatureSettings.saveAll(
                    backend = backend,
                    agentBaseUrl = agentForm.currentBaseUrl,
                    agentApiKey = agentForm.currentApiKey,
                    agentModel = agentForm.currentModel,
                    agentMaxTokens = agentForm.currentMaxTokens,
                    agentTemperature = agentForm.currentTemperature,
                    humor = humor,
                    honesty = honesty,
                    ttsEnabled = ttsToggle.isChecked,
                    ttsProvider = ttsProviderField.text.toString(),
                    ttsBaseUrl = ttsUrlField.text.toString(),
                    ttsApiKey = ttsKeyField.text.toString(),
                    ttsVoiceId = ttsVoiceField.text.toString(),
                    ttsModelId = ttsModelField.text.toString(),
                    ttsStability = ttsStabilitySeek.progress,
                    ttsSimilarity = ttsSimilaritySeek.progress,
                    visionEnabled = visionToggle.isChecked,
                    visionBaseUrl = visionUrlField.text.toString(),
                    visionApiKey = visionKeyField.text.toString(),
                    visionModel = visionModelField.text.toString(),
                    replyEnabled = replyToggle.isChecked,
                    replyBaseUrl = replyUrlField.text.toString(),
                    replyApiKey = replyKeyField.text.toString(),
                    replyModel = replyModelField.text.toString()
                )

                savedHumor = humor
                savedHonesty = honesty
                savedBackend = backend
                dirty = false
                status.text = "Saved."
            }
        }
        // Wrap save + status in a layout with bottom padding
        val bottomPad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (32 * density).toInt())
            addView(save)
            addView(status)
        }
        scrollContent.addView(bottomPad)

        setContentView(root)
    }

    /**
     * Mark the form as having unsaved changes.
     * Called by every input that affects the Save atom.
     */
    private fun markDirty() {
        dirty = true
    }

    override fun onDestroy() {
        voiceTts?.stop()
        voiceTts?.shutdown()
        sensorGroup?.stopReadouts()
        creatureSenses?.stop()
        super.onDestroy()
    }
}