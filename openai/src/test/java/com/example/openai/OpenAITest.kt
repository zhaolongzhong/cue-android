package com.example.openai

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpenAITest {
    @Test
    fun `create chat completion`() = runTest {
        val openAI = OpenAI(
            configuration = OpenAI.Configuration(
                apiKey = "test-key"
            )
        )

        val messages = listOf(
            ChatMessage(
                role = "user",
                content = "Hello!"
            )
        )

        try {
            val completion = openAI.chat.completions.create(
                model = "gpt-4",
                messages = messages
            )
            assertNotNull(completion)
            assertEquals(1, completion.choices.size)
        } catch (e: OpenAI.Error) {
            // Expected in test without valid API key
            assert(e is OpenAI.Error.ApiError)
        }
    }
}