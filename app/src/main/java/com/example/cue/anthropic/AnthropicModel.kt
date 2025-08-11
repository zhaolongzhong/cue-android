package com.example.cue.anthropic

import android.content.SharedPreferences
import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.AnthropicClientImpl
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.ClientOptions
import com.anthropic.core.http.StreamResponse
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.RawMessageStreamEvent
import com.example.cue.debug.DebugViewModel
import com.example.cue.debug.Provider
import com.example.cue.settings.apikeys.ApiKeyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AnthropicModel(
    private val apiKeyProvider: (() -> String?)? = null,
    private val staticApiKey: String? = null,
    private val baseUrl: String? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val useBackendProxy: Boolean = false,
    private val authTokenProvider: (() -> String?)? = null,
) {
    private fun getApiKey(): String? {
        val currentProvider = getCurrentProvider()
        return when (currentProvider) {
            Provider.CUE -> {
                // For backend proxy, use the auth token (JWT)
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

    private fun createClient(): AnthropicClient {
        val currentApiKey = getApiKey()
        val useProxy = shouldUseBackendProxy()
        val currentProvider = getCurrentProvider()

        Log.d(TAG, "Creating client with provider: $currentProvider, useBackendProxy: $useProxy")

        return if (useProxy && baseUrl != null) {
            Log.d(TAG, "Using AnthropicModel SDK implementation (backend proxy)")
            Log.d(TAG, "Backend URL: $baseUrl")
            Log.d(TAG, "Auth token: ${if (currentApiKey.isNullOrEmpty()) "NONE" else "SET (${currentApiKey?.take(10)}...)"}")

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
            Log.d(TAG, "Using AnthropicModel SDK implementation (direct API)")
            Log.d(TAG, "API key: ${if (currentApiKey.isNullOrEmpty()) "NONE" else "SET (${currentApiKey?.take(10)}...)"}")

            val builder = AnthropicOkHttpClient.builder()
            currentApiKey?.let { builder.apiKey(it) }
            if (currentProvider != Provider.ANTHROPIC) {
                Log.d(TAG, "Using custom base URL: $baseUrl")
                baseUrl?.let { builder.baseUrl(it) }
            } else {
                Log.d(TAG, "Using default Anthropic API URL: https://api.anthropic.com")
            }
            builder.build()
        }
    }

    companion object {
        const val TAG = "AnthropicModel"
        fun fromDynamicApiKey(apiKeyProvider: () -> String?): AnthropicModel = AnthropicModel(apiKeyProvider = apiKeyProvider)

        fun fromDynamicBackendProxy(
            baseUrlProvider: () -> String?,
            authTokenProvider: () -> String?,
            sharedPreferences: SharedPreferences? = null,
        ): AnthropicModel = AnthropicModel(
            apiKeyProvider = authTokenProvider,
            baseUrl = baseUrlProvider(),
            sharedPreferences = sharedPreferences,
            useBackendProxy = true,
            authTokenProvider = authTokenProvider,
        )

        fun fromClientMode(mode: ClientMode): AnthropicModel = when (mode) {
            is ClientMode.Direct -> AnthropicModel(
                staticApiKey = mode.apiKey,
                baseUrl = mode.baseUrl,
            )
            is ClientMode.Proxy -> AnthropicModel(
                staticApiKey = mode.authToken ?: "",
                baseUrl = mode.baseUrl,
            )
        }
    }

    suspend fun createCompletion(
        model: String = "claude-3-5-haiku-20241022",
        prompt: String,
        maxTokens: Long = 1000L,
        temperature: Double = 0.7,
        systemPrompt: String? = null,
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating completion with model: $model")
            Log.d(TAG, "Prompt: $prompt")

            val modelEnum = Model.of(model)

            val paramsBuilder = MessageCreateParams.builder()
                .maxTokens(maxTokens)
                .temperature(temperature)
                .addUserMessage(prompt)
                .model(modelEnum)

            systemPrompt?.let { paramsBuilder.system(it) }

            val params = paramsBuilder.build()

            val client = createClient()
            val message: Message = client.messages().create(params)

            val contentBuilder = StringBuilder()
            message.content().forEach { contentBlock ->
                contentBlock.text().ifPresent { textBlock ->
                    contentBuilder.append(textBlock.text())
                }
            }

            val content = if (contentBuilder.isNotEmpty()) {
                contentBuilder.toString()
            } else {
                "No response content found"
            }

            Log.d(TAG, "Response received: $content")
            content
        } catch (e: Exception) {
            Log.e(TAG, "Error creating completion: ${e.message}", e)
            throw RuntimeException("Failed to call Anthropic API: ${e.message}", e)
        }
    }

    fun createCompletionStream(
        model: String = "claude-3-5-haiku-20241022",
        prompt: String,
        maxTokens: Long = 1000L,
        temperature: Double = 0.7,
        systemPrompt: String? = null,
    ): Flow<String> = channelFlow {
        try {
            Log.d(TAG, "Creating streaming completion with model: $model")

            val modelEnum = Model.of(model)

            val paramsBuilder = MessageCreateParams.builder()
                .maxTokens(maxTokens)
                .temperature(temperature)
                .addUserMessage(prompt)
                .model(modelEnum)

            systemPrompt?.let { paramsBuilder.system(it) }

            val params = paramsBuilder.build()

            val client = createClient()
            val streamResponse: StreamResponse<RawMessageStreamEvent> =
                client.messages().createStreaming(params)

            streamResponse.use { response ->
                val textChunks = mutableListOf<String>()
                response.stream().forEach { event ->
                    val deltaEvent = event.contentBlockDelta().orElse(null)
                    if (deltaEvent != null) {
                        val textDelta = deltaEvent.delta().text().orElse(null)
                        if (textDelta != null) {
                            textDelta.text()?.let { text ->
                                textChunks.add(text)
                            }
                        }
                    }
                }
                textChunks.forEach { text ->
                    send(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in streaming completion: ${e.message}", e)
            send("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun createConversation(
        model: String = "claude-3-5-haiku-20241022",
        messages: List<ConversationMessage>,
        maxTokens: Long = 1000L,
        temperature: Double = 0.7,
        systemPrompt: String? = null,
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating conversation with model: $model")

            val modelEnum = Model.of(model)

            val paramsBuilder = MessageCreateParams.builder()
                .maxTokens(maxTokens)
                .temperature(temperature)
                .model(modelEnum)

            systemPrompt?.let { paramsBuilder.system(it) }

            messages.forEach { msg ->
                when (msg.role) {
                    "user" -> paramsBuilder.addUserMessage(msg.content)
                    "assistant" -> paramsBuilder.addAssistantMessage(msg.content)
                }
            }

            val params = paramsBuilder.build()
            val client = createClient()
            val message: Message = client.messages().create(params)

            val contentBuilder = StringBuilder()
            message.content().forEach { contentBlock ->
                contentBlock.text().ifPresent { textBlock ->
                    contentBuilder.append(textBlock.text())
                }
            }

            val content = if (contentBuilder.isNotEmpty()) {
                contentBuilder.toString()
            } else {
                "No response content found"
            }

            Log.d(TAG, "Conversation response received")
            content
        } catch (e: Exception) {
            Log.e(TAG, "Error in conversation: ${e.message}", e)
            throw RuntimeException("Failed to call Anthropic API: ${e.message}", e)
        }
    }

    data class ConversationMessage(
        val role: String,
        val content: String,
    )
}
