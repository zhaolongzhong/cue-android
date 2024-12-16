package com.example.openai

import kotlinx.serialization.Serializable

@Serializable
data class Tool(
    val type: String = "function",
    val function: Function
)

@Serializable
data class Function(
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