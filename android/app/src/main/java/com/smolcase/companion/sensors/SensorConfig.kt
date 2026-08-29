package com.smolcase.companion.sensors

import android.content.Context

/**
 * Per-sensor configuration stored in SharedPreferences.
 *
 * Follows the exact pattern from LlmSettings: property-backed reads/writes
 * via the "smolcase_sensors" preferences file with immediate persistence.
 */
class SensorConfig(context: Context) {

    private val prefs = context.getSharedPreferences("smolcase_sensors", Context.MODE_PRIVATE)

    var magnetometerEnabled: Boolean
        get() = prefs.getBoolean(KEY_MAG_ENABLED, DEFAULT_MAG_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_MAG_ENABLED, v).apply()

    var magnetometerIntervalMs: Int
        get() = prefs.getInt(KEY_MAG_INTERVAL, DEFAULT_MAG_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_MAG_INTERVAL, v.coerceIn(100, 10000)).apply()

    var accelerometerEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACCEL_ENABLED, DEFAULT_ACCEL_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_ACCEL_ENABLED, v).apply()

    var accelerometerIntervalMs: Int
        get() = prefs.getInt(KEY_ACCEL_INTERVAL, DEFAULT_ACCEL_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_ACCEL_INTERVAL, v.coerceIn(100, 10000)).apply()

    var gyroscopeEnabled: Boolean
        get() = prefs.getBoolean(KEY_GYRO_ENABLED, DEFAULT_GYRO_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_GYRO_ENABLED, v).apply()

    var gyroscopeIntervalMs: Int
        get() = prefs.getInt(KEY_GYRO_INTERVAL, DEFAULT_GYRO_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_GYRO_INTERVAL, v.coerceIn(100, 10000)).apply()

    var lightEnabled: Boolean
        get() = prefs.getBoolean(KEY_LIGHT_ENABLED, DEFAULT_LIGHT_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_LIGHT_ENABLED, v).apply()

    var lightIntervalMs: Int
        get() = prefs.getInt(KEY_LIGHT_INTERVAL, DEFAULT_LIGHT_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_LIGHT_INTERVAL, v.coerceIn(100, 10000)).apply()

    var barometerEnabled: Boolean
        get() = prefs.getBoolean(KEY_BARO_ENABLED, DEFAULT_BARO_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_BARO_ENABLED, v).apply()

    var barometerIntervalMs: Int
        get() = prefs.getInt(KEY_BARO_INTERVAL, DEFAULT_BARO_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_BARO_INTERVAL, v.coerceIn(100, 10000)).apply()

    var proximityEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROX_ENABLED, DEFAULT_PROX_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_PROX_ENABLED, v).apply()

    var proximityIntervalMs: Int
        get() = prefs.getInt(KEY_PROX_INTERVAL, DEFAULT_PROX_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_PROX_INTERVAL, v.coerceIn(100, 10000)).apply()

    var ambientTemperatureEnabled: Boolean
        get() = prefs.getBoolean(KEY_TEMP_ENABLED, DEFAULT_TEMP_ENABLED)
        set(v) = prefs.edit().putBoolean(KEY_TEMP_ENABLED, v).apply()

    var ambientTemperatureIntervalMs: Int
        get() = prefs.getInt(KEY_TEMP_INTERVAL, DEFAULT_TEMP_INTERVAL)
        set(v) = prefs.edit().putInt(KEY_TEMP_INTERVAL, v.coerceIn(100, 10000)).apply()

    companion object {
        const val DEFAULT_MAG_ENABLED = true
        const val DEFAULT_ACCEL_ENABLED = true
        const val DEFAULT_GYRO_ENABLED = true
        const val DEFAULT_LIGHT_ENABLED = false
        const val DEFAULT_BARO_ENABLED = false
        const val DEFAULT_PROX_ENABLED = true
        const val DEFAULT_TEMP_ENABLED = false

        const val DEFAULT_MAG_INTERVAL = 500
        const val DEFAULT_ACCEL_INTERVAL = 200
        const val DEFAULT_GYRO_INTERVAL = 200
        const val DEFAULT_LIGHT_INTERVAL = 2000
        const val DEFAULT_BARO_INTERVAL = 2000
        const val DEFAULT_PROX_INTERVAL = 500
        const val DEFAULT_TEMP_INTERVAL = 5000

        private const val KEY_MAG_ENABLED = "magnetometer_enabled"
        private const val KEY_MAG_INTERVAL = "magnetometer_interval_ms"
        private const val KEY_ACCEL_ENABLED = "accelerometer_enabled"
        private const val KEY_ACCEL_INTERVAL = "accelerometer_interval_ms"
        private const val KEY_GYRO_ENABLED = "gyroscope_enabled"
        private const val KEY_GYRO_INTERVAL = "gyroscope_interval_ms"
        private const val KEY_LIGHT_ENABLED = "light_enabled"
        private const val KEY_LIGHT_INTERVAL = "light_interval_ms"
        private const val KEY_BARO_ENABLED = "barometer_enabled"
        private const val KEY_BARO_INTERVAL = "barometer_interval_ms"
        private const val KEY_PROX_ENABLED = "proximity_enabled"
        private const val KEY_PROX_INTERVAL = "proximity_interval_ms"
        private const val KEY_TEMP_ENABLED = "ambient_temperature_enabled"
        private const val KEY_TEMP_INTERVAL = "ambient_temperature_interval_ms"
    }
}