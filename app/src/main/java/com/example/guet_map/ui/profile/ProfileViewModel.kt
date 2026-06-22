package com.example.guet_map.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.Resource
import com.example.guet_map.model.UserProfile
import com.example.guet_map.repository.AuthRepository
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val nickname: String = "",
    val email: String = "",
    val avatar: String? = null,
    val points: Int = 0,
    val contributionCount: Int = 0,
    val friendCount: Int = 0,
    val pendingFriendRequests: Int = 0,
    val message: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = ProfileUiState(
            isLoggedIn = authRepository.isLoggedIn,
            nickname = authRepository.nickname,
            email = userPrefs.email,
            avatar = userPrefs.avatar,
            points = userPrefs.points,
            contributionCount = userPrefs.contributionCount
        )
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            socialRepository.getUserInfo().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        val profile = resource.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            nickname = profile.nickname,
                            email = profile.email,
                            avatar = profile.avatar,
                            points = profile.points,
                            contributionCount = profile.contributionCount,
                            friendCount = profile.friendCount,
                            pendingFriendRequests = profile.pendingFriendRequests
                        )
                        // 更新本地缓存
                        userPrefs.avatar = profile.avatar
                        userPrefs.points = profile.points
                        userPrefs.contributionCount = profile.contributionCount
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

    fun logout() {
        authRepository.logout()
        _uiState.value = ProfileUiState(isLoggedIn = false, message = "已退出登录")
    }

    fun updateAvatar(avatarPath: String) {
        viewModelScope.launch {
            socialRepository.updateAvatar(avatarPath).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            avatar = avatarPath,
                            message = "头像已更新"
                        )
                        userPrefs.avatar = avatarPath
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            message = resource.message
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            socialRepository.updateNickname(nickname).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            nickname = nickname,
                            message = "昵称已更新"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            message = resource.message
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
}
