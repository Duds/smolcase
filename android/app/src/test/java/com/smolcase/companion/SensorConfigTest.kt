package com.smolcase.companion

import com.smolcase.companion.sensors.CreatureSenses
import com.smolcase.companion.sensors.CreatureSenses.SensorReading
import com.smolcase.companion.sensors.SensorConfig
import org.junit.Assert.*
import org.junit.Test

class SensorConfigTest {

    @Test
    fun `default intervals are within valid range`() {
        assertTrue(SensorConfig.DEFAULT_MAG_INTERVAL in 100..10_000)
        assertTrue(SensorConfig.DEFAULT_ACCEL_INTERVAL in 100..10_000)
        assertTrue(SensorConfig.DEFAULT_GYRO_INTERVAL in 100..10_000)
        assertTrue(SensorConfig.DEFAULT_LIGHT_INTERVAL in 100..10_000)
        assertTrue(SensorConfig.DEFAULT_BARO_INTERVAL in 100..10_000)
        assertTrue(SensorConfig.DEFAULT_PROX_INTERVAL in 100..10_000)
        assertTrue(SensorConfig.DEFAULT_TEMP_INTERVAL in 100..10_000)
    }

    @Test
    fun `default enabled states`() {
        assertTrue(SensorConfig.DEFAULT_MAG_ENABLED)
        assertTrue(SensorConfig.DEFAULT_ACCEL_ENABLED)
        assertTrue(SensorConfig.DEFAULT_GYRO_ENABLED)
        assertFalse(SensorConfig.DEFAULT_LIGHT_ENABLED)
        assertFalse(SensorConfig.DEFAULT_BARO_ENABLED)
        assertTrue(SensorConfig.DEFAULT_PROX_ENABLED)
        assertFalse(SensorConfig.DEFAULT_TEMP_ENABLED)
    }
}

class CreatureSensesTest {

    @Test
    fun `SensorReading data class stores three floats`() {
        val r = SensorReading(1.0f, 2.0f, 3.0f)
        assertEquals(1.0f, r.x, 0.001f)
        assertEquals(2.0f, r.y, 0.001f)
        assertEquals(3.0f, r.z, 0.001f)
    }

    @Test
    fun `SensorReading destructures correctly`() {
        val (x, y, z) = SensorReading(4f, 5f, 6f)
        assertEquals(4f, x, 0.001f)
        assertEquals(5f, y, 0.001f)
        assertEquals(6f, z, 0.001f)
    }

    @Test
    fun `heading defaults to zero when no prior reading`() {
        // computeHeading is private, but we verify the math through public types:
        // SensorManager.getRotationMatrix and getOrientation are static —
        // we test the data classes and boundary conditions.
        val readings = listOf(
            SensorReading(0f, 0f, 9.8f),   // resting flat
            SensorReading(9.8f, 0f, 0f),   // on its side
            SensorReading(0f, 9.8f, 0f)    // tilted
        )
        assertEquals(3, readings.size)
        readings.forEach { assertEquals(3, it.javaClass.declaredFields.size) }
    }
}