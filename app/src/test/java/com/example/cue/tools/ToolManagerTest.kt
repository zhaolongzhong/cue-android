package com.example.cue.tools

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ToolManagerTest {
    private lateinit var toolManager: ToolManager

    @Before
    fun setup() {
        toolManager = ToolManager()
    }

    @Test
    fun `getTools returns list of available tools`() {
        val tools = toolManager.getTools()
        
        assertNotNull(tools)
        assertTrue(tools.isNotEmpty())
        
        val weatherTool = tools.first { it.function.name == "get_current_weather" }
        assertEquals("Get the current weather in a given location", weatherTool.function.description)
        assertTrue(weatherTool.function.parameters.required.contains("location"))
    }

    @Test
    fun `callTool executes weather tool successfully`() = runTest {
        val result = toolManager.callTool(
            "get_current_weather",
            mapOf(
                "location" to "San Francisco, CA",
                "unit" to "F"
            )
        )
        
        assertEquals("61F in San Francisco, CA", result)
    }

    @Test(expected = ToolError.ToolNotFound::class)
    fun `callTool throws ToolNotFound for non-existent tool`() = runTest {
        toolManager.callTool(
            "non_existent_tool",
            mapOf("dummy" to "value")
        )
    }

    @Test(expected = ToolError.InvalidArguments::class)
    fun `callTool throws InvalidArguments when missing required parameter`() = runTest {
        toolManager.callTool(
            "get_current_weather",
            mapOf("unit" to "F")
        )
    }
}