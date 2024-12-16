package com.example.cue.tools

/**
 * Represents a tool that can be displayed in the UI
 */
data class LocalTool(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>
)

/**
 * Represents a parameter for a tool
 */
data class ToolParameter(
    val name: String,
    val description: String,
    val type: String,
    val required: Boolean = false
)