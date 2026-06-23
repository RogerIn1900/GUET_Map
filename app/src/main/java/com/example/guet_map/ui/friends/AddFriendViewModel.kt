package com.example.guet_map.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.FriendRequestWithUser
import com.example.guet_map.model.Resource
import com.example.guet_map.model.UserSearchResult
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddFriendUiState(
    val isLoading: Boolean = false,
    val searchResult: UserSearchResult? = null,
    val pendingRequests: List<FriendRequestWithUser> = emptyList(),
    val message: String? = null,
    val requestSent: Boolean = false
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

    init {
        loadPendingRequests()
    }

    fun searchUser(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "请输入邮箱地址")
            return
        }

        viewModelScope.launch {
            socialRepository.searchUser(email = email).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            searchResult = resource.data,
                            requestSent = false
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            searchResult = null,
                            message = resource.message
                        )
                    }
                }
            }
        }
    }

    fun sendFriendRequest(userId: Long, message: String? = null) {
        viewModelScope.launch {
            socialRepository.sendFriendRequest(userId, message).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = "好友申请已发送",
                            requestSent = true,
                            searchResult = _uiState.value.searchResult?.copy(isFriend = false, hasPendingRequest = true)
                        )
                        loadPendingRequests()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(message = resource.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun handleFriendRequest(requestId: Long, accept: Boolean) {
        viewModelScope.launch {
            socialRepository.handleFriendRequest(requestId, accept).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = if (accept) "已添加好友" else "已拒绝"
                        )
                        loadPendingRequests()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(message = resource.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private fun loadPendingRequests() {
        viewModelScope.launch {
            socialRepository.getReceivedFriendRequests().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            pendingRequests = resource.data.filter { it.request.status == "pending" }
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearSearchResult() {
        _uiState.value = _uiState.value.copy(searchResult = null, requestSent = false)
    }
}
