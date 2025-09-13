package ai.plusonelabs.agent.models

sealed class SimpleContentBlock {

    data class Text(
        val text: String,
        val type: String = "text",
    ) : SimpleContentBlock()

    data class ToolUse(
        val id: String,
        val name: String,
        val input: Map<String, Any>,
        val type: String = "tool_use",
    ) : SimpleContentBlock()

    data class ToolResult(
        val id: String,
        val content: String,
        val type: String = "tool_result",
    ) : SimpleContentBlock()

    data class Thinking(
        val thinking: String,
        val type: String = "thinking",
    ) : SimpleContentBlock()

    val contentType: String
        get() = when (this) {
            is Text -> type
            is ToolUse -> type
            is ToolResult -> type
            is Thinking -> type
        }
}
