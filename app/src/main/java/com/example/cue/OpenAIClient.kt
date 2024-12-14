package com.example.cue

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OpenAIClient(private val apiKey: String) {
    private val baseUrl = "https://api.openai.com/v1"
    companion object {
        const val TAG = "OpenAIClient"
    }

    fun createCompletion(
        model: String = "gpt-4o-mini",
        prompt: String,
        maxTokens: Int = 100,
        temperature: Double = 0.7,
    ): String {
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
                put("messages", messagesArray) // Use JSONArray instead of array
                put("max_tokens", maxTokens)
                put("temperature", temperature)
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

                val content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                Log.d(TAG, "Extracted content: $content")
                return content
            } else {
                // Read error response
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Error response: $errorResponse")
                throw RuntimeException("API returned error code: $responseCode, body: $errorResponse")
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
