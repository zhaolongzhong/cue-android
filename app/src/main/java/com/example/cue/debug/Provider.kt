package com.example.cue.debug

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class Provider {
    OPENAI,
    ANTHROPIC,
    CUE,
    ;

    val displayName: String
        get() = when (this) {
            OPENAI -> "OpenAI"
            ANTHROPIC -> "Anthropic"
            CUE -> "Cue"
        }

    val icon: ImageVector
        get() = when (this) {
            OPENAI -> Icons.Default.Memory
            ANTHROPIC -> Icons.Default.Person
            CUE -> Icons.Default.Android
        }

    val description: String
        get() = when (this) {
            OPENAI -> "Direct OpenAI API"
            ANTHROPIC -> "Direct Anthropic API"
            CUE -> "Cue Backend Server"
        }

    companion object {
        fun fromString(value: String): Provider? = try {
            valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
