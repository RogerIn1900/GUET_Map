package com.example.guet_map.module.ai.domain.usecase

import com.example.guet_map.model.Resource
import com.example.guet_map.module.ai.data.model.AiResponse
import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.domain.service.AiService
import javax.inject.Inject

/**
 * 发送消息用例
 */
class SendMessageUseCase @Inject constructor(
    private val aiService: AiService
) {
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        locationId: String? = null
    ): Resource<ChatMessage> {
        if (message.isBlank()) {
            return Resource.Error("请输入内容")
        }
        return aiService.sendMessage(sessionId, message.trim(), locationId)
    }

    suspend fun sendStructured(
        sessionId: String,
        message: String,
        locationId: String? = null
    ): Resource<AiResponse> {
        if (message.isBlank()) {
            return Resource.Error("请输入内容")
        }
        return aiService.sendStructuredMessage(sessionId, message.trim(), locationId)
    }
}
