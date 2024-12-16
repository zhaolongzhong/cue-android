package com.example.cue.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Schema definition for tool parameters
data class ParameterProperty(
    val type: String,
    val description: String,
    val items: ParameterItems? = null
)

data class ParameterItems(
    val type: String
)

data class ParameterSchema(
    val properties: Map<String, ParameterProperty>,
    val required: List<String>
)

// Base interface for tools
interface LocalTool {
    val name: String
    val description: String
    val parameterSchema: ParameterSchema
    
    suspend fun call(args: ToolArguments): String
}

// Safe wrapper for tool arguments
class ToolArguments(private val arguments: Map<String, Any>) {
    fun getString(key: String): String? = arguments[key]?.toString()
    fun getMap(): Map<String, Any> = arguments.toMap()
}

// Tool errors
sealed class ToolError : Exception() {
    data class ToolNotFound(val toolName: String) : ToolError() {
        override val message: String = "Tool not found: $toolName"
    }
    
    data class InvalidArguments(override val message: String) : ToolError()
}

// Utility function for IO operations
suspend fun <T> withIO(block: suspend () -> T): T {
    return withContext(Dispatchers.IO) { block() }
}