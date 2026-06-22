package com.example.guet_map.module.ai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.Resource
import com.example.guet_map.module.ai.data.model.AiAction
import com.example.guet_map.module.ai.data.model.AiResponse
import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.domain.usecase.GetChatHistoryUseCase
import com.example.guet_map.module.ai.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * AI 对话 ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    // 对话会话 ID
    private val sessionId = UUID.randomUUID().toString()

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            getChatHistoryUseCase(sessionId).collect { history ->
                _messages.value = history
                _uiState.value = if (history.isEmpty()) {
                    ChatUiState.Empty
                } else {
                    ChatUiState.Success(history)
                }
            }
        }
    }

    fun sendMessage(content: String, locationId: String? = null) {
        sendStructuredMessage(content, locationId)
    }

    fun sendStructuredMessage(content: String, locationId: String? = null) {
        if (content.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true

            when (val result = sendMessageUseCase.sendStructured(sessionId, content, locationId)) {
                is Resource.Success -> {
                    handleAiResponse(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = ChatUiState.Error(result.message)
                    _events.emit(ChatUiEvent.ShowMessage(result.message))
                }
                is Resource.Loading -> {
                    // 已在加载中
                }
            }

            _isLoading.value = false
        }
    }

    private suspend fun handleAiResponse(response: AiResponse) {
        val action = response.action
        if (response.responseType == AiResponse.ResponseType.CHAT || action == null) {
            response.text?.takeIf { it.isNotBlank() }?.let {
                _events.emit(ChatUiEvent.ShowMessage(it))
            }
            return
        }

        when (action.action) {
            AiAction.ActionType.NAVIGATE_TO -> emitNavigateEvent(action.payload)
            AiAction.ActionType.SHOW_ROUTE -> emitRouteEvent(action.payload)
            AiAction.ActionType.ASK_PERMISSION -> emitPermissionEvent(action.payload)
            AiAction.ActionType.CLARIFY -> emitClarifyEvent(action.payload)
            AiAction.ActionType.SHOW_WEATHER -> emitWeatherEvent(action.payload)
            AiAction.ActionType.SHOW_TIMETABLE_NAVIGATION -> emitTimetableNavigationEvent(action.payload)
        }
    }

    private suspend fun emitNavigateEvent(payload: Map<String, Any?>) {
        _events.emit(
            ChatUiEvent.NavigateTo(
                targetName = payload["targetName"]?.toString(),
                targetLocationId = payload["targetLocationId"]?.toString(),
                fallbackQuery = payload["fallbackQuery"]?.toString(),
                mode = payload["mode"]?.toString() ?: "walking"
            )
        )
    }

    private suspend fun emitRouteEvent(payload: Map<String, Any?>) {
        val route = payload["route"] as? Map<*, *>
        _events.emit(
            ChatUiEvent.ShowRoute(
                summary = route?.get("summary")?.toString() ?: "已生成路线方案。",
                distance = route?.get("distance")?.toString()?.toDoubleOrNull()?.toInt(),
                durationMin = route?.get("durationMin")?.toString()?.toDoubleOrNull()?.toInt()
            )
        )
    }

    private suspend fun emitPermissionEvent(payload: Map<String, Any?>) {
        _events.emit(
            ChatUiEvent.AskPermission(
                permission = payload["permission"]?.toString() ?: "location",
                reason = payload["reason"]?.toString() ?: "需要相关权限才能继续。"
            )
        )
    }

    private suspend fun emitClarifyEvent(payload: Map<String, Any?>) {
        _events.emit(
            ChatUiEvent.ShowClarifyQuestion(
                question = payload["question"]?.toString() ?: "请补充更完整的信息。"
            )
        )
    }

    private suspend fun emitWeatherEvent(payload: Map<String, Any?>) {
        _events.emit(
            ChatUiEvent.ShowWeather(
                summary = payload["summary"]?.toString() ?: "天气详情",
                temperature = payload["temperature"]?.toString()?.toIntOrNull() ?: 0,
                description = payload["description"]?.toString() ?: "未知",
                feelsLike = payload["feelsLike"]?.toString()?.toIntOrNull() ?: 0,
                humidity = payload["humidity"]?.toString()?.toIntOrNull() ?: 0,
                windSpeed = payload["windSpeed"]?.toString()?.toFloatOrNull() ?: 0f,
                windDirection = payload["windDirection"]?.toString() ?: "未知",
                aqi = payload["aqi"]?.toString()?.toIntOrNull(),
                aqiLevel = payload["aqiLevel"]?.toString(),
                uvIndex = payload["uvIndex"]?.toString()?.toIntOrNull(),
                alertMessage = payload["alertMessage"]?.toString(),
                safetyTips = payload["safetyTips"]?.toString()
            )
        )
    }

    private suspend fun emitTimetableNavigationEvent(payload: Map<String, Any?>) {
        _events.emit(
            ChatUiEvent.ShowTimetableNavigationCard(
                courseName = payload["courseName"]?.toString() ?: "",
                classroomName = payload["classroomName"]?.toString() ?: "",
                dayOfWeek = payload["dayOfWeek"]?.toString() ?: "",
                formatTime = payload["formatTime"]?.toString() ?: "",
                departureTime = payload["departureTime"]?.toString() ?: "",
                arriveTime = payload["arriveTime"]?.toString() ?: "",
                walkingMinutes = payload["walkingMinutes"]?.toString()?.toIntOrNull() ?: 0,
                warningMessage = payload["warningMessage"]?.toString() ?: "",
                timing = payload["timing"]?.toString() ?: "",
                targetLocationId = payload["targetLocationId"]?.toString(),
                targetLatitude = payload["targetLatitude"]?.toString()?.toDoubleOrNull(),
                targetLongitude = payload["targetLongitude"]?.toString()?.toDoubleOrNull()
            )
        )
    }

    fun clearSession() {
        viewModelScope.launch {
            _messages.value = emptyList()
            _uiState.value = ChatUiState.Empty
        }
    }
}

sealed class ChatUiState {
    data object Loading : ChatUiState()
    data object Empty : ChatUiState()
    data class Success(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
