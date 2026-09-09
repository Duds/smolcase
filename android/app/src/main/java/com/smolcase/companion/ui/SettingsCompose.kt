package com.smolcase.companion.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.smolcase.companion.CreatureSettings
import com.smolcase.companion.PersonalityDials
import com.smolcase.companion.llm.LlmSettings
import com.smolcase.companion.sensors.CreatureSenses
import com.smolcase.companion.sensors.SensorConfig
import com.smolcase.companion.ui.SettingsTokens.Cyan

private object SettingsTokens {
    val Background = Color(0xFF0F131C)
    val Surface = Color(0xFF0E1420)
    val Panel = Color(0xFF141C2B)
    val Field = Color(0xFF05070B)
    val Border = Color(0xFF1E2A3E)
    val Cyan = Color(0xFF00F0FF)
    val Coral = Color(0xFFF43F5E)
    val Label = Color(0xFFF1F5F9)
    val Value = Color(0xFFDFE2EE)
    val Hint = Color(0xFF94A3B8)
    val Dim = Color(0xFF64748B)
}

private val SettingsScheme = darkColorScheme(
    background = SettingsTokens.Background,
    surface = SettingsTokens.Surface,
    surfaceVariant = SettingsTokens.Panel,
    primary = Cyan,
    secondary = SettingsTokens.Coral,
    onBackground = SettingsTokens.Label,
    onSurface = SettingsTokens.Value,
    onSurfaceVariant = SettingsTokens.Hint,
    outline = SettingsTokens.Border
)

@Composable
fun SmolcaseSettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SettingsScheme, content = content)
}

data class SettingsDraft(
    val backend: LlmSettings.Backend,
    val agentProvider: String, val agentUrl: String, val agentKey: String, val agentModel: String,
    val agentTokens: Int, val agentTemperature: Int,
    val humor: Int, val honesty: Int,
    val ttsEnabled: Boolean, val ttsProvider: String, val ttsUrl: String, val ttsKey: String,
    val ttsVoiceId: String, val ttsModel: String, val ttsStability: Int, val ttsSimilarity: Int,
    val visionEnabled: Boolean, val visionUrl: String, val visionKey: String, val visionModel: String,
    val replyEnabled: Boolean, val replyUrl: String, val replyKey: String, val replyModel: String
)

@Composable
fun SettingsScreen(
    initial: SettingsDraft,
    dials: PersonalityDials,
    creatureSettings: CreatureSettings,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var baseline by remember { mutableStateOf(initial) }
    var showDiscard by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val dirty = draft != baseline
    val update: (SettingsDraft) -> Unit = { draft = it }

    BackHandler { if (dirty) showDiscard = true else onClose() }
    SmolcaseSettingsTheme {
        Scaffold(
            containerColor = SettingsTokens.Background,
            topBar = { SettingsTopBar(onClose) },
            bottomBar = {
                Row(
                    Modifier.fillMaxWidth().background(SettingsTokens.Field).navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { if (dirty) showDiscard = true else onClose() }, Modifier.weight(1f)) { Text("DISCARD") }
                    Button(
                        onClick = {
                            creatureSettings.saveAll(
                                draft.backend, draft.agentProvider, draft.agentUrl, draft.agentKey, draft.agentModel,
                                draft.agentTokens, draft.agentTemperature, draft.humor, draft.honesty,
                                draft.ttsEnabled, draft.ttsProvider, draft.ttsUrl, draft.ttsKey, draft.ttsVoiceId,
                                draft.ttsModel, draft.ttsStability, draft.ttsSimilarity, draft.visionEnabled,
                                draft.visionUrl, draft.visionKey, draft.visionModel, draft.replyEnabled,
                                draft.replyUrl, draft.replyKey, draft.replyModel
                            )
                            baseline = draft
                            saved = true
                            onSaved()
                        }, Modifier.weight(1.4f), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = SettingsTokens.Field)
                    ) { Text(if (saved) "FLASH COMPLETE" else "⚡  FLASH EEPROM") }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveMirror()
                Section("NEUROLOGY", "THINKING ENGINE", "SYS_01 // LLM") {
                    BackendPicker(draft.backend) { update(draft.copy(backend = it)) }
                    if (draft.backend == LlmSettings.Backend.AGENT) AgentFields(draft, update)
                }
                Section("MOOD", "PERSONALITY DIALS", "smolcase_dials") {
                    Dial("HUMOR BIAS", draft.humor, SettingsTokens.Coral, "SERIOUS LABORATORY", "UNHINGED SATIRE", onChange = { update(draft.copy(humor = it)) })
                    Dial("HONESTY & CANDOR", draft.honesty, Cyan, "POLITE DIPLOMACY", "BRUTAL TRUTH", onChange = { update(draft.copy(honesty = it)) })
                    Note("Local registers apply live adjustments to generation prompts.")
                }
                VoiceSection(draft, update, dials)
                Section("PHOTO_CAMERA", "MULTIMODAL CLOUD", "VISION // HOOK") { CloudFields(draft, update) }
                SensorSection()
                NamespaceBadges()
            }
        }
    }
    if (showDiscard) AlertDialog(
        onDismissRequest = { showDiscard = false },
        title = { Text("Discard changes?") },
        text = { Text("Unsaved configuration will be lost.") },
        confirmButton = { TextButton(onClick = onClose) { Text("DISCARD") } },
        dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("KEEP EDITING") } }
    )
}

@Composable private fun SettingsTopBar(onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SettingsTokens.Field)) {
        Text("●  SYS_CHASSIS // DEV:READY                         120MHz   94%", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("‹", fontSize = 32.sp, color = SettingsTokens.Label) }
            Text("RETURN TO FACE\nSMOLCASE CONFIG", color = SettingsTokens.Label, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("REV_3", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(end = 16.dp))
        }
    }
}

@Composable private fun LiveMirror() = CardSurface {
    Text("●  STATUS: ONLINE // LISTENING                         CORE_V3.8", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    Text("LED MATRIX [38x68 PIXEL PROJECTION]", color = SettingsTokens.Dim, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    Text("   ● ● ●                 ● ● ●\n   ● ● ●       ·         ● ● ●\n   · ● ·                 · ● ·", Modifier.fillMaxWidth().background(SettingsTokens.Field).padding(12.dp), color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 25.sp)
    Text("Hold hardware face screen anytime to return to Companion Mode.", color = SettingsTokens.Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable private fun Section(icon: String, title: String, code: String, content: @Composable () -> Unit) = CardSurface {
    Text("$icon   $title                         $code", color = SettingsTokens.Label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp)); content()
}

@Composable private fun CardSurface(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.fillMaxWidth().background(SettingsTokens.Surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

@Composable private fun BackendPicker(value: LlmSettings.Backend, onChange: (LlmSettings.Backend) -> Unit) {
    listOf(
        LlmSettings.Backend.AGENT to "AGENT       Cloud / OpenAI-compatible",
        LlmSettings.Backend.NANO to "GEMINI NANO On-device & private",
        LlmSettings.Backend.GEMMA to "GEMMA 4 E2B Sideloaded / Tensor G3 / UNSTABLE",
        LlmSettings.Backend.RULES to "RULES ONLY  Deterministic / No AI"
    ).forEach { (backend, label) -> Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == backend, onClick = { onChange(backend) })
        Text(label, color = if (value == backend) Cyan else SettingsTokens.Value, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    } }
}

@Composable private fun AgentFields(draft: SettingsDraft, update: (SettingsDraft) -> Unit) {
    Field("PROVIDER LABEL", draft.agentProvider) { update(draft.copy(agentProvider = it)) }
    Field("BASE URL", draft.agentUrl) { update(draft.copy(agentUrl = it)) }
    Field("API KEY", draft.agentKey, secret = true) { update(draft.copy(agentKey = it)) }
    Field("MODEL", draft.agentModel) { update(draft.copy(agentModel = it)) }
    Dial("MAX TOKENS", draft.agentTokens - 64, Cyan, "64", "1024", { update(draft.copy(agentTokens = it + 64)) }, 960)
    Dial("TEMPERATURE", draft.agentTemperature, Cyan, "0.00", "2.00", { update(draft.copy(agentTemperature = it)) }, 200)
}

@Composable private fun VoiceSection(draft: SettingsDraft, update: (SettingsDraft) -> Unit, dials: PersonalityDials) = Section("GRAPHIC_EQ", "VOICE & SPEECH", "SPEECH_CORE") {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var voiceIndex by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                voices = engineVoices(tts).filter { it.locale.language == "en" }
                voiceIndex = voices.indexOfFirst { it.name == dials.voiceName }
            }
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    Text("ANDROID LOCAL TTS ENGINE\n${dials.voiceName ?: "Automatic male voice"}", color = SettingsTokens.Value, modifier = Modifier.fillMaxWidth().background(SettingsTokens.Field).padding(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { tts?.speak("Systems nominal.", TextToSpeech.QUEUE_FLUSH, null, "sample") }, Modifier.weight(1f)) { Text("▶ SAMPLE") }
        OutlinedButton(onClick = {
            if (voices.isNotEmpty()) { voiceIndex = (voiceIndex + 1) % voices.size; val voice = voices[voiceIndex]; dials.voiceName = voice.name; tts?.voice = voice; tts?.speak("Systems nominal.", TextToSpeech.QUEUE_FLUSH, null, "sample") }
        }, Modifier.weight(1f)) { Text("NEXT VOICE") }
    }
    Toggle("CLOUD HIGH-FIDELITY TTS", "ElevenLabs streaming pipeline", draft.ttsEnabled) { update(draft.copy(ttsEnabled = it)) }
    Field("TTS PROVIDER", draft.ttsProvider) { update(draft.copy(ttsProvider = it)) }
    Field("TTS BASE URL", draft.ttsUrl) { update(draft.copy(ttsUrl = it)) }
    Field("TTS API KEY", draft.ttsKey, secret = true) { update(draft.copy(ttsKey = it)) }
    Field("TTS VOICE ID", draft.ttsVoiceId) { update(draft.copy(ttsVoiceId = it)) }
    Field("TTS MODEL ID", draft.ttsModel) { update(draft.copy(ttsModel = it)) }
    Dial("TTS STABILITY", draft.ttsStability, Cyan, "0%", "100%", onChange = { update(draft.copy(ttsStability = it)) })
    Dial("TTS SIMILARITY", draft.ttsSimilarity, Cyan, "0%", "100%", onChange = { update(draft.copy(ttsSimilarity = it)) })
}

private fun engineVoices(tts: TextToSpeech?): List<Voice> = tts?.voices?.sortedBy { it.name }.orEmpty()

@Composable private fun CloudFields(draft: SettingsDraft, update: (SettingsDraft) -> Unit) {
    Toggle("CLOUD VISION ANALYSIS", "Single-frame optical telemetry", draft.visionEnabled) { update(draft.copy(visionEnabled = it)) }
    Note("PRIVACY PROTOCOL\nCaptures one camera frame only when explicitly triggered. Continuous cloud streaming is prohibited.")
    Field("VISION BASE URL", draft.visionUrl) { update(draft.copy(visionUrl = it)) }
    Field("VISION API KEY", draft.visionKey, secret = true) { update(draft.copy(visionKey = it)) }
    Field("VISION MODEL", draft.visionModel) { update(draft.copy(visionModel = it)) }
    Toggle("CLOUD REPLY GENERATION", "Dedicated auxiliary reasoning route", draft.replyEnabled) { update(draft.copy(replyEnabled = it)) }
    Field("REPLY BASE URL", draft.replyUrl) { update(draft.copy(replyUrl = it)) }
    Field("REPLY API KEY", draft.replyKey, secret = true) { update(draft.copy(replyKey = it)) }
    Field("REPLY MODEL", draft.replyModel) { update(draft.copy(replyModel = it)) }
}

@Composable private fun Dial(label: String, value: Int, color: Color, low: String, high: String, onChange: (Int) -> Unit, max: Int = 100) {
    Text("$label                                      ${if (max == 200) "%.2f".format(value / 100.0) else "$value%"}", color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt().coerceIn(0, max)) }, valueRange = 0f..max.toFloat(), colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
    Text("0% $low                                      100% $high", color = SettingsTokens.Dim, fontSize = 10.sp)
}

@Composable private fun Field(label: String, value: String, secret: Boolean = false, onChange: (String) -> Unit) = OutlinedTextField(
    value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
)

@Composable private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Switch(checked = checked, onCheckedChange = onChange); Spacer(Modifier.width(8.dp)); Column { Text(title, color = SettingsTokens.Value); Text(subtitle, color = SettingsTokens.Hint, fontSize = 12.sp) }
}

@Composable private fun Note(text: String) = Text(text, color = SettingsTokens.Hint, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().background(SettingsTokens.Field).padding(8.dp))

@Composable private fun SensorSection() = Section("SENSORS", "HARDWARE TELEMETRY", "I2C_BUS LIVE") {
    val context = LocalContext.current
    val config = remember { SensorConfig(context) }
    val senses = remember { CreatureSenses(context, config) }
    val group = remember { SensorToggleGroup(context, config, senses) }
    DisposableEffect(Unit) { senses.start(); group.startReadouts(); onDispose { group.stopReadouts(); senses.stop() } }
    Note("Tactile environmental perception units. Tap an interval to reconfigure sampling rate.")
    AndroidView(factory = { group }, modifier = Modifier.fillMaxWidth())
}

@Composable private fun NamespaceBadges() = Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
    Text("HARDWARE MEMORY BUS // NAMESPACES", color = SettingsTokens.Dim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("smolcase_llm", "smolcase_dials", "smolcase_sensors").forEach { Text("$it\nPERSISTED LOCAL", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.weight(1f).background(SettingsTokens.Field).padding(6.dp)) } }
}
