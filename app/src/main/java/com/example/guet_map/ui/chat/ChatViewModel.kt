package com.example.guet_map.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.Message
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val friendId: Long = 0,
    val friendName: String = "",
    val messages: List<Message> = emptyList(),
    val message: String? = null,
    val messageSent: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userPrefs: UserPrefs,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val currentUserId: Long
        get() = userPrefs.userId.toLongOrNull() ?: 0

    init {
        val friendId = savedStateHandle.get<Long>("friendId") ?: 0
        val friendName = savedStateHandle.get<String>("friendName") ?: ""
        _uiState.value = _uiState.value.copy(friendId = friendId, friendName = friendName)
        loadMessages()
    }

    fun loadMessages() {
        val friendId = _uiState.value.friendId
        if (friendId == 0L) return

        viewModelScope.launch {
            socialRepository.getMessages(friendId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messages = resource.data.sortedBy { it.createdAt }
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = resource.message
                        )
                    }
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _uiState.value.friendId == 0L) return

        viewModelScope.launch {
            socialRepository.sendMessage(_uiState.value.friendId, content).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val newMessage = resource.data
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + newMessage,
                            messageSent = true
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(message = resource.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun isOwnMessage(message: Message): Boolean {
        return message.senderId == currentUserId
    }
}
