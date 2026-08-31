package com.smolcase.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val memory by lazy { MemoryStore(this) }
    private val dials by lazy { PersonalityDials(this) }
    private val conversationLog by lazy { ConversationLog(this) }
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val utteranceCounter = java.util.concurrent.atomic.AtomicInteger(0)

    private val tapHandler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private val tapTimeout = Runnable {
        if (tapCount in 1..2) ears?.toggleMute()
        tapCount = 0
    }

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
        conversation = ConversationEngine(this, memory)
        conversation.expressionState = eyesView.expressionState
        setContentView(eyesView)

        eyesView.setDials(dials.humor, dials.honesty)

        conversationLog.startSession()
        Log.i(TAG, "Session ${conversationLog.sessionId} started (build ${BuildConfig.VERSION_CODE} - ${BuildConfig.VERSION_NAME})")

        val bm = getSystemService(BatteryManager::class.java)
        val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        eyesView.setTelemetry(battery = batteryLevel, ble = false)

        // Tap = mute/unmute (350ms delay); triple-tap = face debug cycle;
        // long-press = settings
        eyesView.setOnClickListener {
            tapCount++
            tapHandler.removeCallbacks(tapTimeout)
            if (tapCount >= 3) {
                tapCount = 0
                eyesView.cycleDebugMode()
            } else {
                tapHandler.postDelayed(tapTimeout, 350)
            }
        }
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
                val uid = utteranceCounter.incrementAndGet()
                val heardAt = System.currentTimeMillis()
                Log.i(TAG, "[#$uid] heard: $text")
                conversationLog.heard(text)
                conversation.handle(text) { reply ->
                    val llmMs = System.currentTimeMillis() - heardAt
                    Log.i(TAG, "[#$uid] LLM reply ($llmMs ms): ${reply.say}")
                    conversationLog.replied(reply.say, llmMs)
                    eyesView.express(reply.excitement, reply.happy, reply.blinks)
                    voice?.say(reply.say, uid)
                }
            },
            onMuteChanged = { muted ->
                eyesView.setMicMuted(muted)
            }
        ).also { it.start() }
        // Voice uses internal volume management (no stream-level muting).
        // Post-TTS echo prevention handled by VoiceEars.cooldown.
        voice = CreatureVoice(this, dials, eyesView, onTtsDone = { ears?.cooldown() })
    }

    override fun onDestroy() {
        faceTracker?.stop()
        ears?.stop()
        brain.stop()
        voice?.shutdown()
        conversation.shutdown() // release the Gemma engine's native memory
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SmolcaseMain"
    }
}
