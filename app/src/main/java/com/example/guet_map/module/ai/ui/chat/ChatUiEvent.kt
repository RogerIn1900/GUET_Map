package com.example.guet_map.module.ai.ui.chat

/**
 * AI 聊天页面的一次性事件。
 *
 * 这些事件只表达 AI 模块生成的意图，不直接调用地图、定位或其他模块内部实现。
 */
sealed class ChatUiEvent {
    data class NavigateTo(
        val targetName: String?,
        val targetLocationId: String?,
        val fallbackQuery: String?,
        val mode: String = "walking"
    ) : ChatUiEvent()

    data class ShowRoute(
        val summary: String,
        val distance: Int?,
        val durationMin: Int?
    ) : ChatUiEvent()

    data class AskPermission(
        val permission: String,
        val reason: String
    ) : ChatUiEvent()

    data class ShowClarifyQuestion(
        val question: String
    ) : ChatUiEvent()

    data class ShowMessage(
        val message: String
    ) : ChatUiEvent()

    data class ShowWeather(
        val summary: String,
        val temperature: Int,
        val description: String,
        val feelsLike: Int,
        val humidity: Int,
        val windSpeed: Float,
        val windDirection: String,
        val aqi: Int?,
        val aqiLevel: String?,
        val uvIndex: Int?,
        val alertMessage: String?,
        val safetyTips: String?
    ) : ChatUiEvent()
}
