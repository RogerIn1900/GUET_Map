package com.example.guet_map.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.FriendInfo
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendInfo> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            socialRepository.getFriends().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            friends = resource.data
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

    fun removeFriend(friendId: Long) {
        viewModelScope.launch {
            socialRepository.removeFriend(friendId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            friends = _uiState.value.friends.filter { it.userId != friendId },
                            message = "已删除好友"
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
}
