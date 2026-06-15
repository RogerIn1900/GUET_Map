package com.example.guet_map.module.ai.domain.service

import com.example.guet_map.model.Resource
import com.example.guet_map.module.ai.data.model.AiResponse
import com.example.guet_map.module.ai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * AI 导航动作回调接口
 */
interface AiNavigationCallback {
    /**
     * 执行导航到指定地点
     */
    suspend fun navigateTo(locationId: String?, targetName: String, fallbackQuery: String?)

    /**
     * 展示路线
     */
    suspend fun showRoute(locationId: String?, targetName: String, fallbackQuery: String?)
}

/**
 * AI 服务接口
 * 定义 AI 对话的核心能力
 */
interface AiService {

    /**
     * 发送消息并获取 AI 回复。
     *
     * 保留该接口用于兼容当前聊天 UI，返回值是可直接展示和保存的聊天消息。
     */
    suspend fun sendMessage(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null,
        navigationCallback: AiNavigationCallback? = null
    ): Resource<ChatMessage>

    /**
     * 发送消息并获取结构化 AI 响应。
     *
     * 用于识别 navigate_to、show_route、ask_permission、clarify 等 action。
     */
    suspend fun sendStructuredMessage(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null
    ): Resource<AiResponse>

    /**
     * 流式发送消息（SSE）
     */
    fun sendMessageStream(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null
    ): Flow<Resource<String>>  // 流式返回片段

    /**
     * 生成引导性问题
     */
    suspend fun generateGuidedQuestions(locationId: String?): Resource<List<String>>
}
