package com.example.guet_map.module.ai.data.remote

import com.google.gson.annotations.SerializedName

data class DeepSeekRequest(
    val model: String = DeepSeekConstants.MODEL,
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.2,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val id: String? = null,
    val choices: List<DeepSeekChoice>? = null,
    val usage: DeepSeekUsage? = null,
    val error: DeepSeekError? = null
)

data class DeepSeekChoice(
    val index: Int? = null,
    val message: DeepSeekMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class DeepSeekUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerializedName("completion_tokens")
    val completionTokens: Int? = null,
    @SerializedName("total_tokens")
    val totalTokens: Int? = null
)

data class DeepSeekError(
    val message: String? = null,
    val type: String? = null
)

object DeepSeekConstants {
    const val MODEL = "deepseek-chat"
    const val BASE_URL = "https://api.deepseek.com"
    const val CHAT_COMPLETIONS_PATH = "/chat/completions"
    const val TIMEOUT_MS = 10_000L
    const val MAX_RETRY = 1
}
