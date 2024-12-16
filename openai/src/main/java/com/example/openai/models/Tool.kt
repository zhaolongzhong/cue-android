package com.example.openai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tool(
    val type: String = "function",
    val function: FunctionDefinition
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: Parameters
)

@Serializable
data class Parameters(
    val type: String = "object",
    val properties: Map<String, Property>,
    val required: List<String> = emptyList()
)

@Serializable
data class Property(
    val type: String,
    val description: String,
    val enum: List<String>? = null,
    val items: Items? = null
)

@Serializable
data class Items(
    val type: String
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: Function
)

@Serializable
data class Function(
    val name: String,
    val arguments: String
)