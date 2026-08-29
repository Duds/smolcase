package com.smolcase.companion.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.sqrt

/**
 * Background HandlerThread that polls enabled sensors and stores latest readings.
 *
 * Uses a dedicated HandlerThread so sensor callbacks never block the main thread.
 * Readings are atomically stored in-memory and accessible via the `latest*()` methods.
 *
 * Lifecycle: call start() on resume, stop() on pause.
 */
class CreatureSenses(context: Context, private val config: SensorConfig) {

    data class SensorReading(val x: Float, val y: Float, val z: Float)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile private var _latestHeading: Float? = null
    @Volatile private var _latestAccelerometer: SensorReading? = null
    @Volatile private var _latestGyroscope: SensorReading? = null
    @Volatile private var _latestLight: Float? = null
    @Volatile private var _latestTemperature: Float? = null
    @Volatile private var _latestProximity: Float? = null
    @Volatile private var _latestBarometer: Float? = null

    @Volatile private var _isShaking: Boolean = false
    @Volatile private var _wasPickedUp: Boolean = false
    private var lastAccelMag: Float = 0f
    private var accelSampleCount: Int = 0
    private var shakeAccumulator: Float = 0f

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            when (event.sensor.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    if (values.size >= 3) {
                        _latestAccelerometer?.let { accel ->
                            _latestHeading = computeHeading(values, accel)
                        }
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    if (values.size >= 3) {
                        val reading = SensorReading(values[0], values[1], values[2])
                        _latestAccelerometer = reading
                        detectMotion(reading)
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if (values.size >= 3) {
                        _latestGyroscope = SensorReading(values[0], values[1], values[2])
                    }
                }
                Sensor.TYPE_LIGHT -> _latestLight = values[0]
                Sensor.TYPE_AMBIENT_TEMPERATURE -> _latestTemperature = values[0]
                Sensor.TYPE_PROXIMITY -> _latestProximity = values[0]
                Sensor.TYPE_PRESSURE -> _latestBarometer = values[0]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun start() {
        if (handler != null) return
        thread = HandlerThread("creature-senses").apply { start() }
        handler = Handler(thread!!.looper)
        registerSensors()
    }

    fun stop() {
        unregisterSensors()
        handler?.removeCallbacksAndMessages(null)
        handler = null
        thread?.quitSafely()
        thread = null
    }

    fun latestHeading(): Float? = _latestHeading
    fun latestAccelerometer(): SensorReading? = _latestAccelerometer
    fun latestGyroscope(): SensorReading? = _latestGyroscope
    fun latestLight(): Float? = _latestLight
    fun latestTemperature(): Float? = _latestTemperature
    fun latestProximity(): Float? = _latestProximity
    fun latestBarometer(): Float? = _latestBarometer
    fun isShaking(): Boolean = _isShaking

    fun wasPickedUp(): Boolean {
        val v = _wasPickedUp
        _wasPickedUp = false
        return v
    }

    private fun registerSensors() {
        fun register(sensorType: Int, intervalMs: Int) {
            sensorManager.getDefaultSensor(sensorType)?.let { sensor ->
                sensorManager.registerListener(listener, sensor, intervalMs * 1000, handler)
            }
        }

        if (config.magnetometerEnabled) register(Sensor.TYPE_MAGNETIC_FIELD, config.magnetometerIntervalMs)
        if (config.accelerometerEnabled) register(Sensor.TYPE_ACCELEROMETER, config.accelerometerIntervalMs)
        if (config.gyroscopeEnabled) register(Sensor.TYPE_GYROSCOPE, config.gyroscopeIntervalMs)
        if (config.lightEnabled) register(Sensor.TYPE_LIGHT, config.lightIntervalMs)
        if (config.barometerEnabled) register(Sensor.TYPE_PRESSURE, config.barometerIntervalMs)
        if (config.proximityEnabled) register(Sensor.TYPE_PROXIMITY, config.proximityIntervalMs)
        if (config.ambientTemperatureEnabled) register(Sensor.TYPE_AMBIENT_TEMPERATURE, config.ambientTemperatureIntervalMs)
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(listener)
    }

    private fun computeHeading(mag: FloatArray, accel: SensorReading): Float {
        val accelValues = floatArrayOf(accel.x, accel.y, accel.z)
        val rot = FloatArray(9)
        if (SensorManager.getRotationMatrix(rot, null, accelValues, mag)) {
            val orient = FloatArray(3)
            SensorManager.getOrientation(rot, orient)
            return ((Math.toDegrees(orient[0].toDouble()) + 360.0) % 360.0).toFloat()
        }
        return _latestHeading ?: 0f
    }

    private fun detectMotion(reading: SensorReading) {
        val mag = sqrt(reading.x * reading.x + reading.y * reading.y + reading.z * reading.z)
        val diff = kotlin.math.abs(mag - lastAccelMag)
        lastAccelMag = mag

        accelSampleCount++
        shakeAccumulator += diff
        if (accelSampleCount >= 10) {
            _isShaking = (shakeAccumulator / accelSampleCount) > 3.0f
            shakeAccumulator = 0f
            accelSampleCount = 0
        }

        if (diff > 8.0f && mag > 12.0f) {
            _wasPickedUp = true
        }
    }
}