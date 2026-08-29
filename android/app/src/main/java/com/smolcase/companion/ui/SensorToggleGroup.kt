package com.smolcase.companion.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.smolcase.companion.sensors.SensorConfig
import com.smolcase.companion.sensors.CreatureSenses

/**
 * Reusable UI for a sensor row: toggle + label + description + interval seekbar + live readout.
 *
 * Each sensor shows what the creature learns from it (FR-6).
 */
class SensorToggleGroup(
    context: Context,
    private val config: SensorConfig,
    private val senses: CreatureSenses
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val minTouchPx = (SettingsTheme.MIN_TOUCH_HEIGHT_DP * density).toInt()
    private val vPad = (4 * density).toInt()

    private val readoutViews = mutableMapOf<String, TextView>()
    private val steps = listOf(100, 200, 500, 1000, 2000, 5000, 10000)

    private val uiHandler = Handler(Looper.getMainLooper())
    private val readoutUpdate = object : Runnable {
        override fun run() {
            updateReadouts()
            uiHandler.postDelayed(this, 1000)
        }
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        addSensorRow("magnetometer", "Magnetometer", SENSOR_DESCRIPTIONS["magnetometer"]!!,
            config.magnetometerEnabled, config.magnetometerIntervalMs,
            { config.magnetometerEnabled = it }, { config.magnetometerIntervalMs = it })
        addSensorRow("accelerometer", "Accelerometer", SENSOR_DESCRIPTIONS["accelerometer"]!!,
            config.accelerometerEnabled, config.accelerometerIntervalMs,
            { config.accelerometerEnabled = it }, { config.accelerometerIntervalMs = it })
        addSensorRow("gyroscope", "Gyroscope", SENSOR_DESCRIPTIONS["gyroscope"]!!,
            config.gyroscopeEnabled, config.gyroscopeIntervalMs,
            { config.gyroscopeEnabled = it }, { config.gyroscopeIntervalMs = it })
        addSensorRow("light", "Light", SENSOR_DESCRIPTIONS["light"]!!,
            config.lightEnabled, config.lightIntervalMs,
            { config.lightEnabled = it }, { config.lightIntervalMs = it })
        addSensorRow("barometer", "Barometer", SENSOR_DESCRIPTIONS["barometer"]!!,
            config.barometerEnabled, config.barometerIntervalMs,
            { config.barometerEnabled = it }, { config.barometerIntervalMs = it })
        addSensorRow("proximity", "Proximity", SENSOR_DESCRIPTIONS["proximity"]!!,
            config.proximityEnabled, config.proximityIntervalMs,
            { config.proximityEnabled = it }, { config.proximityIntervalMs = it })
        addSensorRow("ambient_temperature", "Ambient Temperature", SENSOR_DESCRIPTIONS["ambient_temperature"]!!,
            config.ambientTemperatureEnabled, config.ambientTemperatureIntervalMs,
            { config.ambientTemperatureEnabled = it }, { config.ambientTemperatureIntervalMs = it })
    }

    fun startReadouts() { uiHandler.post(readoutUpdate) }
    fun stopReadouts() { uiHandler.removeCallbacks(readoutUpdate) }
    fun save() {} // immediate persistence
    fun hasChanges(): Boolean = false

    private fun addSensorRow(
        key: String, label: String, description: String,
        enabled: Boolean, interval: Int,
        onEnabledChanged: (Boolean) -> Unit,
        onIntervalChanged: (Int) -> Unit
    ) {
        val toggleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        val toggle = Switch(context).apply {
            isChecked = enabled
            setTextColor(SettingsTheme.VALUE_COLOR)
            contentDescription = "$label sensor"
            setOnCheckedChangeListener { _, isChecked -> onEnabledChanged(isChecked) }
        }
        toggleRow.addView(toggle)

        // Label + description vertically stacked
        val labelColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val labelView = TextView(context).apply {
            text = label
            setTextColor(SettingsTheme.LABEL_COLOR)
            textSize = 14f
            contentDescription = label
        }
        labelColumn.addView(labelView)
        val descView = TextView(context).apply {
            text = description
            setTextColor(SettingsTheme.HINT_COLOR)
            textSize = 12f
        }
        labelColumn.addView(descView)
        toggleRow.addView(labelColumn)
        addView(toggleRow)

        val intervalLabel = TextView(context).apply {
            text = "Interval: ${interval}ms"
            setTextColor(SettingsTheme.VALUE_COLOR)
            textSize = 12f
        }
        addView(intervalLabel)

        val intervalSeek = SeekBar(context).apply {
            max = steps.size - 1
            progress = steps.indexOfFirst { it >= interval }.coerceAtLeast(0)
            minimumHeight = minTouchPx
            contentDescription = "$label interval"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    val ms = steps[p]
                    intervalLabel.text = "Interval: ${ms}ms"
                    onIntervalChanged(ms)
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        addView(intervalSeek)

        val readout = TextView(context).apply {
            text = ""
            setTextColor(SettingsTheme.HINT_COLOR)
            textSize = 12f
        }
        readoutViews[key] = readout
        addView(readout)
    }

    private fun updateReadouts() {
        fun fmt(key: String, value: String) {
            readoutViews[key]?.text = value
        }
        senses.latestHeading()?.let { fmt("magnetometer", "Heading: ${"%.1f".format(it)}°") }
            ?: fmt("magnetometer", "Heading: ---")
        senses.latestAccelerometer()?.let { fmt("accelerometer", "Accel: ${"%.2f".format(it.x)}, ${"%.2f".format(it.y)}, ${"%.2f".format(it.z)}") }
            ?: fmt("accelerometer", "Accel: ---")
        senses.latestGyroscope()?.let { fmt("gyroscope", "Gyro: ${"%.3f".format(it.x)}, ${"%.3f".format(it.y)}, ${"%.3f".format(it.z)}") }
            ?: fmt("gyroscope", "Gyro: ---")
        senses.latestLight()?.let { fmt("light", "Light: ${"%.0f".format(it)} lux") }
            ?: fmt("light", "Light: ---")
        senses.latestBarometer()?.let { fmt("barometer", "Pressure: ${"%.1f".format(it)} hPa") }
            ?: fmt("barometer", "Pressure: ---")
        senses.latestProximity()?.let { fmt("proximity", "Proximity: ${"%.0f".format(it)} cm") }
            ?: fmt("proximity", "Proximity: ---")
        senses.latestTemperature()?.let { fmt("ambient_temperature", "Temp: ${"%.1f".format(it)} °C") }
            ?: fmt("ambient_temperature", "Temp: ---")
    }

    private companion object {
        val SENSOR_DESCRIPTIONS = mapOf(
            "magnetometer" to "Learns which direction the creature faces",
            "accelerometer" to "Feels when the creature is moved or picked up",
            "gyroscope" to "Detects rotation and orientation changes",
            "light" to "Knows if the desk is lit or the room is dark",
            "barometer" to "Senses weather changes and altitude",
            "proximity" to "Notices when something is close to the face",
            "ambient_temperature" to "Feels the room temperature"
        )
    }
}