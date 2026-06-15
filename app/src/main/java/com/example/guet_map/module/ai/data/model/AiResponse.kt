package com.example.guet_map.module.ai.data.model

/**
 * DeepSeek 返回的统一 AI 响应结构
 */
data class AiResponse(
    val responseType: ResponseType,
    val text: String? = null,
    val action: AiAction? = null
) {
    enum class ResponseType {
        CHAT,
        ACTION
    }
}

data class AiAction(
    val action: ActionType,
    val payload: Map<String, Any?> = emptyMap()
) {
    enum class ActionType {
        NAVIGATE_TO,
        SHOW_ROUTE,
        ASK_PERMISSION,
        CLARIFY,
        SHOW_WEATHER
    }
}
