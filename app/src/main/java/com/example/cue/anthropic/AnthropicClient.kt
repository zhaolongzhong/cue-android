package com.example.cue.anthropic

import android.content.SharedPreferences
import com.example.cue.network.ErrorResponseParser
import com.example.cue.settings.apikeys.ApiKeyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.example.cue.utils.AppLog as Log

class AnthropicClient(
    private val accessToken: String,
    private val baseUrl: String,
    private val sharedPreferences: SharedPreferences? = null,
) {
    private val errorParser = ErrorResponseParser()

    companion object {
        const val TAG = "AnthropicClient"
    }

    suspend fun createCompletion(
        model: String = "claude-3-5-haiku-20241022",
        prompt: String,
        maxTokens: Int = 1000,
        temperature: Double = 0.7,
    ): String = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/chat/completions"
        Log.d(TAG, "Making request to endpoint: $endpoint")
        Log.d(TAG, "Using model: $model")
        Log.d(TAG, "Prompt: $prompt")

        val connection = URL(endpoint).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val userApiKey = sharedPreferences?.getString(ApiKeyType.ANTHROPIC.key, "")
            if (!userApiKey.isNullOrEmpty()) {
                connection.setRequestProperty("X-Anthropic-API-Key", userApiKey)
                Log.d(TAG, "Added user's Anthropic API key to request")
            } else {
                Log.w(TAG, "No Anthropic API key found in user settings")
            }

            connection.doOutput = true

            Log.d(TAG, "Request method: ${connection.requestMethod}")
            Log.d(TAG, "Headers set: Content-Type, Authorization, Accept, X-Anthropic-API-Key")

            val messagesArray = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    },
                )
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", maxTokens)
                put("temperature", temperature)
                put("stream", false)
            }

            Log.d(TAG, "Request body prepared: $requestBody")

            try {
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                    Log.d(TAG, "Request body written successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing request body: ${e.message}")
                throw e
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Raw response: $response")

                val jsonResponse = JSONObject(response)
                Log.d(TAG, "Parsed JSON response successfully")

                val content = if (jsonResponse.has("content")) {
                    jsonResponse.getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
                } else if (jsonResponse.has("choices")) {
                    jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    "No response content found"
                }

                content
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Error response: $errorResponse")

                val errorMessage = errorParser.parseErrorResponse(
                    responseCode,
                    errorResponse,
                    ErrorResponseParser.ApiProvider.ANTHROPIC,
                )

                throw RuntimeException(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw RuntimeException("Failed to call backend API: ${e.message}", e)
        } finally {
            connection.disconnect()
            Log.d(TAG, "Connection disconnected")
        }
    }
}
