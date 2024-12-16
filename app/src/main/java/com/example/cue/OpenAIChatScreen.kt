package com.example.cue

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TAG = "OpenAIScreen"

@Composable
fun OpenAIScreen(modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val openAIClient = remember {
        OpenAIClient(BuildConfig.OPENAI_API_KEY)
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = {
                prompt = it
                Log.d(TAG, "Prompt updated: $it")
            },
            label = { Text("Enter your prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    Log.d(TAG, "Button clicked, starting API call")
                    isLoading = true
                    try {
                        Log.d(TAG, "Making API call with prompt: $prompt")
                        response = withContext(Dispatchers.IO) {
                            openAIClient.createCompletion(
                                prompt = prompt,
                                maxTokens = 100,
                            )
                        }
                        Log.d(TAG, "API call successful, response: $response")
                    } catch (e: Exception) {
                        Log.e(TAG, "API call failed: ${e.message}", e)

                        response = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                        Log.d(TAG, "API call completed, loading state reset")
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isLoading,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Text("Get Response")
        }

        if (isLoading) {
            CircularProgressIndicator()
            Log.d(TAG, "Showing loading indicator")
        }

        if (response.isNotEmpty()) {
            Text(
                text = response,
                modifier = Modifier.padding(top = 16.dp),
            )
            Log.d(TAG, "Displaying response: $response")
        }
    }
}
