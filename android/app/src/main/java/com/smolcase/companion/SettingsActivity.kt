package com.smolcase.companion

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smolcase.companion.llm.LlmSettings
import com.smolcase.companion.ui.SettingsDraft
import com.smolcase.companion.ui.SettingsScreen

/** Cybernetic companion configuration deck. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val settings = LlmSettings(this)
        val creatureSettings = CreatureSettings(this)
        val dials = PersonalityDials(this)
        setContent {
            SettingsScreen(
                initial = settings.readDraft(dials),
                dials = dials,
                creatureSettings = creatureSettings,
                onClose = ::finish,
                onSaved = { Toast.makeText(this, "EEPROM FLASH COMPLETE", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun LlmSettings.readDraft(dials: PersonalityDials) = SettingsDraft(
        backend = backend,
        agentProvider = agentProviderLabel,
        agentUrl = agentBaseUrl,
        agentKey = agentApiKey,
        agentModel = agentModel,
        agentTokens = agentMaxTokens,
        agentTemperature = agentTemperature,
        humor = dials.humor,
        honesty = dials.honesty,
        ttsEnabled = cloudTtsEnabled,
        ttsProvider = cloudTtsProviderLabel,
        ttsUrl = cloudTtsBaseUrl,
        ttsKey = cloudTtsApiKey,
        ttsVoiceId = cloudTtsVoiceId,
        ttsModel = cloudTtsModelId,
        ttsStability = cloudTtsStability,
        ttsSimilarity = cloudTtsSimilarity,
        visionEnabled = cloudVisionEnabled,
        visionUrl = cloudVisionBaseUrl,
        visionKey = cloudVisionApiKey,
        visionModel = cloudVisionModel,
        replyEnabled = cloudReplyGenEnabled,
        replyUrl = cloudReplyGenBaseUrl,
        replyKey = cloudReplyGenApiKey,
        replyModel = cloudReplyGenModel
    )
}
