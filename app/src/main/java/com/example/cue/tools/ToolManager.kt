package com.example.cue.tools

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolManager @Inject constructor() {
    private val localTools: List<LocalTool> = listOf(
        WeatherTool()
        // Add more tools here as needed
    )

    /**
     * Get all available tools with their schemas for AI model consumption
     */
    fun getTools(): List<Tool> = localTools.map { tool ->
        Tool(
            function = FunctionDefinition(
                name = tool.name,
                description = tool.description,
                parameters = tool.parameterSchema
            )
        )
    }

    /**
     * Call a tool by name with the given arguments
     * @throws ToolError.ToolNotFound if the tool doesn't exist
     * @throws ToolError.InvalidArguments if the arguments are invalid
     */
    suspend fun callTool(name: String, arguments: Map<String, Any>): String {
        val tool = localTools.firstOrNull { it.name == name }
            ?: throw ToolError.ToolNotFound(name)
        
        return tool.call(ToolArguments(arguments))
    }
}

/**
 * Tool representation for AI model consumption
 */
data class Tool(
    val function: FunctionDefinition
)

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: ParameterSchema
)