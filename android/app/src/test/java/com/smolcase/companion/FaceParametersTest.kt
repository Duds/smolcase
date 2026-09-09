package com.smolcase.companion

import com.smolcase.companion.face.FaceParameters
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FaceParametersTest {
    private fun captured() = File("src/main/assets/face/parameters.json").readText()

    @Test fun `captured settings round trip without losing any control`() {
        val json = JSONObject(captured())
        val parameters = FaceParameters.fromJson(json.toString())
        val restored = JSONObject(parameters.toJson())
        assertEquals(json.keys().asSequence().toSet(), restored.keys().asSequence().toSet())
        for (key in json.keys()) {
            val value = json.get(key)
            if (value is Number) assertEquals(key, value.toDouble(), restored.getDouble(key), 0.000001)
            else assertEquals(key, value, restored.get(key))
        }
        assertEquals(0.234f, parameters.sphere.radius, 0.000001f)
        assertEquals(1.51f, parameters.optics.glow, 0.000001f)
        assertEquals(0.8f, parameters.optics.sourceTint, 0.000001f)
    }

    @Test fun `invalid or unrecognised captured controls are rejected`() {
        for ((key, value) in listOf("sphereRadius" to 0, "cols" to 0, "cols" to 38.5,
            "sourceTint" to 2, "mood" to "UNKNOWN", "sphereMode" to "true", "glwo" to 1)) {
            val json = JSONObject(captured()).put(key, value)
            assertThrows("$key=$value", IllegalArgumentException::class.java) {
                FaceParameters.fromJson(json.toString())
            }
        }
        assertThrows(IllegalArgumentException::class.java) { FaceParameters.fromJson("{}") }
    }
}
