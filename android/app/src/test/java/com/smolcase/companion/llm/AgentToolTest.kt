package com.smolcase.companion.llm

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolTest {

    @Test
    fun `definitions returns non-empty array`() {
        val defs = AgentTool.definitions()
        assertTrue(defs.length() > 0)
    }

    @Test
    fun `every tool has function name and type function`() {
        val defs = AgentTool.definitions()
        for (i in 0 until defs.length()) {
            val tool = defs.getJSONObject(i)
            assertEquals("function", tool.getString("type"))
            val fn = tool.getJSONObject("function")
            val name = fn.getString("name")
            assertTrue("Tool '$name' missing description", fn.has("description"))
            assertTrue("Tool '$name' missing parameters", fn.has("parameters"))
        }
    }

    @Test
    fun `every tool has parameters with type object`() {
        val defs = AgentTool.definitions()
        for (i in 0 until defs.length()) {
            val params = defs.getJSONObject(i)
                .getJSONObject("function")
                .getJSONObject("parameters")
            assertEquals("object", params.getString("type"))
            val name = defs.getJSONObject(i).getJSONObject("function").getString("name")
            assertTrue("Tool '$name' missing properties", params.has("properties"))
        }
    }

    @Test
    fun `all tool names are unique`() {
        val defs = AgentTool.definitions()
        val names = mutableSetOf<String>()
        for (i in 0 until defs.length()) {
            val name = defs.getJSONObject(i).getJSONObject("function").getString("name")
            assertTrue("Duplicate tool name: $name", names.add(name))
        }
    }

    @Test
    fun `required params field is a JSONArray`() {
        val defs = AgentTool.definitions()
        for (i in 0 until defs.length()) {
            val params = defs.getJSONObject(i)
                .getJSONObject("function")
                .getJSONObject("parameters")
            val required = params.optJSONArray("required")
            assertNotNull("Tool missing 'required' field", required)
        }
    }

    @Test
    fun `tool count matches expected`() {
        assertEquals(11, AgentTool.definitions().length())
    }

    @Test
    fun `tools with no required params have empty required array`() {
        val defs = AgentTool.definitions()
        for (i in 0 until defs.length()) {
            val fn = defs.getJSONObject(i).getJSONObject("function")
            val name = fn.getString("name")
            val params = fn.getJSONObject("parameters")
            val required = params.getJSONArray("required")
            // Non-param tools should have empty required list
            if (name in listOf("list_reminders", "clear_reminders", "get_current_time",
                    "list_facts", "get_soul_summary")) {
                assertEquals("Tool '$name' should have no required params", 0, required.length())
            } else {
                assertTrue("Tool '$name' should have required params", required.length() > 0)
            }
        }
    }

    @Test
    fun `set_dial has enum constraint on dial param`() {
        val defs = AgentTool.definitions()
        val setDial = findTool("set_dial", defs)
            .getJSONObject("parameters")
            .getJSONObject("properties")
            .getJSONObject("dial")
        assertTrue(setDial.has("enum"))
        val enums = setDial.getJSONArray("enum")
        assertEquals(2, enums.length())
        assertTrue(enums.toString().contains("humor"))
        assertTrue(enums.toString().contains("honesty"))
    }

    @Test
    fun `set_mood is the only tool with optional param`() {
        val defs = AgentTool.definitions()
        for (i in 0 until defs.length()) {
            val fn = defs.getJSONObject(i).getJSONObject("function")
            val name = fn.getString("name")
            val params = fn.getJSONObject("parameters")
            val props = params.getJSONObject("properties")
            val required = params.getJSONArray("required")
            if (name == "set_mood") {
                // mood is required, duration_ms is optional
                assertEquals(1, required.length())
                assertEquals("mood", required.getString(0))
                assertTrue(props.has("duration_ms"))
            }
        }
    }

    /** Find a tool by name in the definitions array. */
    private fun findTool(name: String, defs: org.json.JSONArray): JSONObject {
        for (i in 0 until defs.length()) {
            val fn = defs.getJSONObject(i).getJSONObject("function")
            if (fn.getString("name") == name) return fn
        }
        throw AssertionError("Tool '$name' not found in definitions")
    }
}