package com.example.guet_map.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.FriendRequestWithUser
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<FriendRequestWithUser> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class FriendRequestsViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestsUiState())
    val uiState: StateFlow<FriendRequestsUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            socialRepository.getReceivedFriendRequests().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            requests = resource.data.filter { it.request.status == "pending" }
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = resource.message
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun handleRequest(requestId: Long, accept: Boolean) {
        viewModelScope.launch {
            socialRepository.handleFriendRequest(requestId, accept).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = if (accept) "已添加好友" else "已拒绝"
                        )
                        loadRequests()
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
