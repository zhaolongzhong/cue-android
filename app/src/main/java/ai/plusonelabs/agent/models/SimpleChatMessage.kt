package ai.plusonelabs.agent.models

import java.util.UUID

data class SimpleChatMessage(
    val id: String,
    val contentBlocks: List<SimpleContentBlock>,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
) {
    constructor(id: String, content: String, isUser: Boolean, isStreaming: Boolean = false) : this(
        id = id,
        contentBlocks = listOf(SimpleContentBlock.Text(content)),
        isUser = isUser,
        isStreaming = isStreaming,
    )

    val content: String
        get() = contentBlocks.mapNotNull { block ->
            when (block) {
                is SimpleContentBlock.Text -> block.text
                is SimpleContentBlock.Thinking -> block.thinking
                else -> null
            }
        }.joinToString("\n\n")

    companion object {
        fun user(content: String, id: String = UUID.randomUUID().toString()): SimpleChatMessage = SimpleChatMessage(id, content, isUser = true)

        fun assistant(content: String, id: String = UUID.randomUUID().toString()): SimpleChatMessage = SimpleChatMessage(id, content, isUser = false)
    }
}
