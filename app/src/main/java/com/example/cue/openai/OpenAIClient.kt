package com.example.cue.openai

import com.example.cue.network.ErrorResponseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import com.example.cue.utils.AppLog as Log

class OpenAIClient(private val apiKey: String) {
    private val baseUrl = "https://api.openai.com/v1"
    private val errorParser = ErrorResponseParser()

    companion object {
        const val TAG = "OpenAIClient"
    }

    // Simulated weather function
    private fun getWeather(location: String): JSONObject {
        // Simulate weather data
        val temperature = Random.nextInt(0, 35)
        val conditions = listOf("sunny", "cloudy", "rainy", "partly cloudy").random()
        val humidity = Random.nextInt(30, 90)

        return JSONObject().apply {
            put("location", location)
            put("temperature", temperature)
            put("conditions", conditions)
            put("humidity", humidity)
        }
    }

    private fun getWeatherTools(): JSONArray = JSONArray().apply {
        put(
            JSONObject().apply {
                put("type", "function")
                put(
                    "function",
                    JSONObject().apply {
                        put("name", "get_weather")
                        put("description", "Get the current weather in a given location")
                        put(
                            "parameters",
                            JSONObject().apply {
                                put("type", "object")
                                put(
                                    "properties",
                                    JSONObject().apply {
                                        put(
                                            "location",
                                            JSONObject().apply {
                                                put("type", "string")
                                                put("description", "The city and state, e.g. San Francisco, CA")
                                            },
                                        )
                                    },
                                )
                                put("required", JSONArray().apply { put("location") })
                            },
                        )
                    },
                )
            },
        )
    }

    suspend fun createCompletion(
        model: String = "gpt-4o-mini",
        prompt: String,
        maxTokens: Int = 100,
        temperature: Double = 0.7,
    ): String = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/chat/completions"
        Log.d(TAG, "Making request to endpoint: $endpoint")
        Log.d(TAG, "Using model: $model")
        Log.d(TAG, "Prompt: $prompt")

        val connection = URL(endpoint).openConnection() as HttpURLConnection

        try {
            // Configure the request
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true

            Log.d(TAG, "Request method: ${connection.requestMethod}")
            Log.d(TAG, "Headers set: Content-Type and Authorization")

            // Create messages array correctly
            val messagesArray = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    },
                )
            }

            // Prepare request body
            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", maxTokens)
                put("temperature", temperature)
                put("tools", getWeatherTools())
            }

            Log.d(TAG, "Request body prepared: $requestBody")

            // Send request
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

            // Check response code
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            // Read response
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Raw response: $response")

                val jsonResponse = JSONObject(response)
                Log.d(TAG, "Parsed JSON response successfully")

                // Check for tool calls
                val message = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")

                val result = if (message.has("tool_calls")) {
                    // Handle tool calls
                    val toolCalls = message.getJSONArray("tool_calls")
                    val results = mutableListOf<String>()

                    for (i in 0 until toolCalls.length()) {
                        val toolCall = toolCalls.getJSONObject(i)
                        if (toolCall.getString("type") == "function") {
                            val function = toolCall.getJSONObject("function")
                            if (function.getString("name") == "get_weather") {
                                val args = JSONObject(function.getString("arguments"))
                                val location = args.getString("location")
                                val weatherData = getWeather(location)
                                results.add(
                                    "Weather in $location: ${weatherData.getString("conditions")}, " +
                                        "${weatherData.getInt("temperature")}°C, " +
                                        "Humidity: ${weatherData.getInt("humidity")}%",
                                )
                            }
                        }
                    }
                    results.joinToString("\n")
                } else {
                    message.getString("content")
                }

                result
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Error response: $errorResponse")

                val errorMessage = errorParser.parseErrorResponse(
                    responseCode,
                    errorResponse,
                    ErrorResponseParser.ApiProvider.OPENAI,
                )

                throw RuntimeException(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw RuntimeException("Failed to call OpenAI API: ${e.message}", e)
        } finally {
            connection.disconnect()
            Log.d(TAG, "Connection disconnected")
        }
    }
}
