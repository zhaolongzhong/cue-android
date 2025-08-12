package com.example.cue.agent

data class Agent(
    val model: String = "claude-3-5-haiku-20241022",
    val maxTurns: Int = 25,
    val systemPrompt: String? = null,
    val maxTokens: Long = 4096,
)
