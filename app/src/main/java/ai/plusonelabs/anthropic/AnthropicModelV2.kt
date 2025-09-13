package ai.plusonelabs.anthropic

import ai.plusonelabs.agent.Agent
import ai.plusonelabs.agent.ModelResponse
import ai.plusonelabs.agent.NextStep
import ai.plusonelabs.agent.Session
import ai.plusonelabs.agent.SingleStepResult
import ai.plusonelabs.agent.models.SimpleChatMessage
import ai.plusonelabs.agent.models.SimpleContentBlock
import ai.plusonelabs.debug.DebugViewModel
import ai.plusonelabs.debug.Provider
import ai.plusonelabs.settings.apikeys.ApiKeyType
import android.content.SharedPreferences
import com.anthropic.client.AnthropicClient
import com.anthropic.client.AnthropicClientImpl
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.ClientOptions
import com.anthropic.core.http.StreamResponse
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.RawMessageStreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ai.plusonelabs.utils.AppLog as Log

class AnthropicModelV2(
    private val apiKeyProvider: (() -> String?)? = null,
    private val staticApiKey: String? = null,
    private val baseUrl: String? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val useBackendProxy: Boolean = false,
    private val authTokenProvider: (() -> String?)? = null,
) {

    suspend fun getResponse(
        agent: Agent,
        session: Session<SimpleChatMessage>,
    ): SingleStepResult<Message> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "AnthropicModelV2: Creating response with agent model: ${agent.model}")

            val client = createClient()
            val params = createMessageParams(agent, session)
            val message: Message = client.messages().create(params)

            val modelResponse = ModelResponse(
                output = message,
                usage = message.usage(),
                responseId = message.id(),
            )

            Log.d(TAG, "AnthropicModelV2: Response received")
            SingleStepResult(
                modelResponse = modelResponse,
                nextStep = NextStep.FinalOutput(message),
            )
        } catch (e: Exception) {
            Log.e(TAG, "AnthropicModelV2: Error getting response: ${e.message}", e)
            throw RuntimeException("Failed to call Anthropic API: ${e.message}", e)
        }
    }

    fun streamResponse(
        agent: Agent,
        session: Session<SimpleChatMessage>,
    ): Flow<StreamEvent> = channelFlow {
        try {
            Log.d(TAG, "AnthropicModelV2: Starting stream response with agent model: ${agent.model}")

            val client = createClient()
            val params = createMessageParams(agent, session)

            val useProxy = shouldUseBackendProxy()

            if (useProxy) {
                // For backend proxy, use createStreaming() with MessageAccumulator following official docs
                Log.d(TAG, "AnthropicModelV2: Using backend proxy with proper streaming")

                try {
                    val messageAccumulator = MessageAccumulator.create()
                    val streamResponse: StreamResponse<RawMessageStreamEvent> =
                        client.messages().createStreaming(params)

                    streamResponse.use { response ->
                        Log.d(TAG, "AnthropicModelV2: Backend proxy streaming started")

                        // Collect text chunks first, then send them
                        val textChunks = mutableListOf<String>()

                        Log.d(TAG, "AnthropicModelV2: About to call response.stream()")
                        val stream = response.stream()
                        Log.d(TAG, "AnthropicModelV2: Got stream object: $stream")

                        try {
                            // Follow the official docs pattern: accumulate and collect text deltas
                            stream
                                .peek { event ->
                                    Log.d(TAG, "AnthropicModelV2: Backend proxy raw event: $event")
                                    messageAccumulator.accumulate(event)
                                }
                                .flatMap { event -> event.contentBlockDelta().stream() }
                                .flatMap { deltaEvent -> deltaEvent.delta().text().stream() }
                                .forEach { textDelta ->
                                    val text = textDelta.text()
                                    Log.d(TAG, "AnthropicModelV2: Backend proxy text chunk: '$text'")
                                    textChunks.add(text)
                                }

                            Log.d(TAG, "AnthropicModelV2: Stream processing completed, collected ${textChunks.size} chunks")
                        } catch (e: Exception) {
                            Log.e(TAG, "AnthropicModelV2: Error during stream processing: ${e.message}", e)
                            throw e
                        }

                        // Send all collected chunks as individual events
                        textChunks.forEach { text ->
                            send(StreamEvent.ContentDelta(text))
                        }

                        // Now get the accumulated message (stream is complete)
                        val accumulatedMessage = messageAccumulator.message()
                        Log.d(TAG, "AnthropicModelV2: Backend proxy accumulated message: ${accumulatedMessage.id()}")

                        // Extract complete text from accumulated message
                        val fullText = StringBuilder()
                        accumulatedMessage.content().forEach { contentBlock ->
                            contentBlock.text().ifPresent { textBlock ->
                                fullText.append(textBlock.text())
                            }
                        }

                        val responseText = fullText.toString()
                        Log.d(TAG, "AnthropicModelV2: Backend proxy complete text: '$responseText'")

                        // Send completion event
                        send(StreamEvent.MessageCompleteWithText(responseText, accumulatedMessage.id()))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AnthropicModelV2: Error in backend proxy streaming: ${e.message}", e)
                    send(StreamEvent.Error(e))
                }
            } else {
                // Use true streaming for direct Anthropic API
                val streamResponse: StreamResponse<RawMessageStreamEvent> =
                    client.messages().createStreaming(params)

                streamResponse.use { response ->
                    val textChunks = mutableListOf<String>()
                    var messageId: String? = null

                    Log.d(TAG, "AnthropicModelV2: About to start streaming, response: $response")
                    val stream = response.stream()
                    Log.d(TAG, "AnthropicModelV2: Got stream: $stream")

                    stream.forEach { event ->
                        Log.d(TAG, "AnthropicModelV2: response.stream().forEach { event ->: $event")
                        // Collect message start info
                        val messageStart = event.messageStart().orElse(null)
                        if (messageStart != null) {
                            messageId = messageStart.message().id()
                            Log.d(TAG, "AnthropicModelV2: Stream started with message id: $messageId")
                        }

                        // Collect content deltas
                        val deltaEvent = event.contentBlockDelta().orElse(null)
                        Log.d(TAG, "AnthropicModelV2: Stream deltaEvent: $deltaEvent")
                        if (deltaEvent != null) {
                            val textDelta = deltaEvent.delta().text().orElse(null)
                            if (textDelta != null) {
                                val text = textDelta.text()
                                if (text != null) {
                                    textChunks.add(text)
                                    Log.d(TAG, "AnthropicModelV2: Collected text chunk: '$text'")
                                }
                            }
                        }

                        // Check if message is complete
                        val messageStop = event.messageStop().orElse(null)
                        if (messageStop != null) {
                            Log.d(TAG, "AnthropicModelV2: Stream message complete")
                        }

                        // Also check message delta for completion
                        val messageDelta = event.messageDelta().orElse(null)
                        if (messageDelta != null && messageDelta.delta().stopReason().isPresent) {
                            Log.d(TAG, "AnthropicModelV2: Stream completed via messageDelta stopReason")
                        }
                    }

                    // Send all collected chunks as individual events for real-time display
                    textChunks.forEach { text ->
                        send(StreamEvent.ContentDelta(text))
                    }

                    // Create a simple message completion event without MessageAccumulator
                    Log.d(TAG, "AnthropicModelV2: Creating simple completion event with ${textChunks.size} text chunks")
                    val fullText = textChunks.joinToString("")

                    // For now, send a completion event with null message since MessageAccumulator is problematic
                    // In the future, we could construct our own Message object here
                    send(StreamEvent.MessageCompleteWithText(fullText, messageId ?: "unknown"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AnthropicModelV2: Error in streaming: ${e.message}", e)
            send(StreamEvent.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    private fun createMessageParams(
        agent: Agent,
        session: Session<SimpleChatMessage>,
    ): MessageCreateParams {
        val paramsBuilder = MessageCreateParams.builder()
            .maxTokens(agent.maxTokens)
            .model(Model.of(agent.model))

        agent.systemPrompt?.let { paramsBuilder.system(it) }

        session.getAllMessages().forEach { message ->
            val content = convertToAnthropicContent(message)
            when (message.isUser) {
                true -> paramsBuilder.addUserMessage(content)
                false -> paramsBuilder.addAssistantMessage(content)
            }
        }

        return paramsBuilder.build()
    }

    private fun convertToAnthropicContent(message: SimpleChatMessage): String = message.contentBlocks.mapNotNull { block ->
        when (block) {
            is SimpleContentBlock.Text -> block.text
            is SimpleContentBlock.Thinking -> block.thinking
            else -> null
        }
    }.joinToString("\n\n")

    private fun createClient(): AnthropicClient {
        val currentApiKey = getApiKey()
        val useProxy = shouldUseBackendProxy()
        val currentProvider = getCurrentProvider()

        Log.d(TAG, "Creating client with provider: $currentProvider, useBackendProxy: $useProxy")

        return if (useProxy && baseUrl != null) {
            Log.d(TAG, "Using AnthropicModelV2 SDK implementation (backend proxy)")
            val authToken = authTokenProvider?.invoke() ?: currentApiKey ?: ""
            val customHttpClient = BackendProxyHttpClient(
                baseUrl = baseUrl,
                authToken = authToken,
                sharedPreferences = sharedPreferences,
            )
            val clientOptions = ClientOptions.builder()
                .httpClient(customHttpClient)
                .build()

            AnthropicClientImpl(clientOptions)
        } else {
            Log.d(TAG, "Using AnthropicModelV2 SDK implementation (direct API)")

            val builder = AnthropicOkHttpClient.builder()
            currentApiKey?.let { builder.apiKey(it) }
            if (currentProvider != Provider.ANTHROPIC) {
                baseUrl?.let { builder.baseUrl(it) }
            }
            builder.build()
        }
    }

    private fun getApiKey(): String? {
        val currentProvider = getCurrentProvider()
        return when (currentProvider) {
            Provider.CUE -> {
                val authToken = authTokenProvider?.invoke() ?: apiKeyProvider?.invoke() ?: staticApiKey
                Log.d(TAG, "Using auth token for backend proxy (${authToken?.take(10)}...)")
                authToken
            }
            Provider.ANTHROPIC -> {
                val userApiKey = sharedPreferences?.getString(ApiKeyType.ANTHROPIC.key, "")
                if (!userApiKey.isNullOrEmpty()) {
                    Log.d(TAG, "Using user's Anthropic API key from settings (${userApiKey.take(10)}...)")
                    userApiKey
                } else {
                    Log.d(TAG, "No user API key found, using provider key")
                    apiKeyProvider?.invoke() ?: staticApiKey
                }
            }
            else -> {
                apiKeyProvider?.invoke() ?: staticApiKey
            }
        }
    }

    private fun getCurrentProvider(): Provider {
        val providerString = sharedPreferences?.getString(DebugViewModel.KEY_CURRENT_PROVIDER, null)
        return if (providerString != null) {
            Provider.fromString(providerString) ?: Provider.ANTHROPIC
        } else {
            if (useBackendProxy) Provider.CUE else Provider.ANTHROPIC
        }
    }

    private fun shouldUseBackendProxy(): Boolean = getCurrentProvider() == Provider.CUE

    companion object {
        const val TAG = "AnthropicModelV2"

        fun fromDynamicApiKey(apiKeyProvider: () -> String?): AnthropicModelV2 = AnthropicModelV2(apiKeyProvider = apiKeyProvider)

        fun fromDynamicBackendProxy(
            baseUrlProvider: () -> String?,
            authTokenProvider: () -> String?,
            sharedPreferences: SharedPreferences? = null,
        ): AnthropicModelV2 = AnthropicModelV2(
            apiKeyProvider = authTokenProvider,
            baseUrl = baseUrlProvider(),
            sharedPreferences = sharedPreferences,
            useBackendProxy = true,
            authTokenProvider = authTokenProvider,
        )
    }
}

sealed class StreamEvent {
    data class ContentDelta(val text: String) : StreamEvent()
    data class MessageComplete(val message: Message) : StreamEvent()
    data class MessageCompleteWithText(val text: String, val messageId: String) : StreamEvent()
    data class Error(val exception: Throwable) : StreamEvent()
}
