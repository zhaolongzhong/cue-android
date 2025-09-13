package ai.plusonelabs.agent

class Session<T>(
    val sessionId: String,
    private val messages: MutableList<T> = mutableListOf(),
) {
    fun addMessage(message: T) {
        messages.add(message)
    }

    fun getAllMessages(): List<T> = messages.toList()

    fun isEmpty(): Boolean = messages.isEmpty()

    fun size(): Int = messages.size

    fun clear() {
        messages.clear()
    }
}
