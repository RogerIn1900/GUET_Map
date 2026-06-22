package com.example.guet_map.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.FriendInfo
import com.example.guet_map.model.FriendLocation
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendLocationUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendInfo> = emptyList(),
    val friendLocations: Map<Long, FriendLocation> = emptyMap(),
    val message: String? = null
)

@HiltViewModel
class FriendLocationViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendLocationUiState())
    val uiState: StateFlow<FriendLocationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 加载好友列表
            socialRepository.getFriends().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(friends = resource.data)
                        loadLocations(resource.data.map { it.userId })
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

    private fun loadLocations(friendIds: List<Long>) {
        viewModelScope.launch {
            socialRepository.getFriendLocations().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val locationMap = resource.data.associateBy { it.userId }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            friendLocations = locationMap
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

    fun updateMyLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            socialRepository.updateMyLocation(latitude, longitude).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(message = "位置已更新")
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
