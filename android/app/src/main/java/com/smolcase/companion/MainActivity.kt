package com.smolcase.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * SMOLCASE Companion — Stage 3: Voice.
 *
 * Fullscreen creature face. Eyes track you (Stage 1), it remembers and
 * greets you (Stage 2), and it listens and talks in character (Stage 3).
 * Tap the screen to mute/unmute the mic — the desk privacy switch.
 *
 * See docs/05-design-thinking/ for the prototype plan.
 */
class MainActivity : ComponentActivity() {

    private lateinit var eyesView: TarsFaceView
    private lateinit var brain: CreatureBrain
    private lateinit var conversation: ConversationEngine
    private var voice: CreatureVoice? = null
    private var faceTracker: FaceTracker? = null
    private var ears: VoiceEars? = null

    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants[Manifest.permission.CAMERA] == true) startTracking()
            if (grants[Manifest.permission.RECORD_AUDIO] == true) startListening()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The face fills everything; the creature never sleeps the screen itself
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        eyesView = TarsFaceView(this)
        brain = CreatureBrain(this, eyesView)
        brain.onGreetingLine = { line -> voice?.say(line) }
        conversation = ConversationEngine(this, MemoryStore(this))
        setContentView(eyesView)

        // Tap the creature = mute/unmute its ears; long-press = settings
        eyesView.setOnClickListener { ears?.toggleMute() }
        eyesView.setOnLongClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
            true
        }

        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.RECORD_AUDIO)

        if (needed.isEmpty()) {
            startTracking()
            startListening()
        } else {
            permissions.launch(needed.toTypedArray())
        }
    }

    private fun startTracking() {
        if (faceTracker != null) return
        brain.start()
        faceTracker = FaceTracker(this, this) { nx, ny ->
            brain.onFace(nx, ny)
        }.also { it.start() }
    }

    private fun startListening() {
        if (ears != null) return
        ears = VoiceEars(
            context = this,
            onHeard = { text ->
                conversation.handle(text) { reply ->
                    eyesView.express(reply.excitement, reply.happy, reply.blinks)
                    voice?.say(reply.say)
                }
            },
            onMuteChanged = { muted ->
                eyesView.setMicMuted(muted)
            }
        ).also { it.start() }
        // Voice unmutes the streams (via VoiceEars.allowSpeech) only while speaking
        voice = CreatureVoice(this, eyesView) { speaking ->
            ears?.allowSpeech(speaking)
        }
    }

    override fun onDestroy() {
        faceTracker?.stop()
        ears?.stop()
        brain.stop()
        voice?.shutdown()
        super.onDestroy()
    }
}
