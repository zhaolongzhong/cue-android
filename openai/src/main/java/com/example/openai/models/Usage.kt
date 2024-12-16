package com.example.openai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
    @SerialName("completion_tokens_details")
    val completionTokensDetails: TokenDetails,
    @SerialName("prompt_tokens_details")
    val promptTokensDetails: PromptTokenDetails
)

@Serializable
data class TokenDetails(
    @SerialName("rejected_prediction_tokens")
    val rejectedPredictionTokens: Int,
    @SerialName("audio_tokens")
    val audioTokens: Int,
    @SerialName("accepted_prediction_tokens")
    val acceptedPredictionTokens: Int,
    @SerialName("reasoning_tokens")
    val reasoningTokens: Int
)

@Serializable
data class PromptTokenDetails(
    @SerialName("cached_tokens")
    val cachedTokens: Int,
    @SerialName("audio_tokens")
    val audioTokens: Int
)