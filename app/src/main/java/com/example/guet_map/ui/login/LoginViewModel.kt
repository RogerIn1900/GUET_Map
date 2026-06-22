package com.example.guet_map.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginMode {
    LOGIN,
    REGISTER,
    RESET_PASSWORD
}

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val nickname: String = "",
    val email: String = "",
    val points: Int = 0,
    val contributionCount: Int = 0,
    val loading: Boolean = false,
    val sendingCode: Boolean = false,
    val message: String? = null,
    val mode: LoginMode = LoginMode.LOGIN,
    val countdown: Int = 0,
    val resetPasswordSuccess: Boolean = false,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(refreshState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun refresh() {
        _uiState.value = refreshState()
    }

    fun setMode(mode: LoginMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            message = null,
            resetPasswordSuccess = false
        )
    }

    fun sendCode(email: String) {
        if (email.isBlank()) return

        val type = when (_uiState.value.mode) {
            LoginMode.REGISTER -> "register"
            LoginMode.RESET_PASSWORD -> "reset_password"
            else -> "login"
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendingCode = true, message = null)
            authRepository.sendCode(email.trim(), type).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(sendingCode = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            sendingCode = false,
                            message = "验证码已发送，请查收邮件"
                        )
                        startCountdown()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            sendingCode = false,
                            message = resource.message
                        )
                    }
                }
            }
        }
    }

    fun requestResetPassword(email: String) {
        _uiState.value = _uiState.value.copy(mode = LoginMode.RESET_PASSWORD)
        sendCode(email)
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 60 downTo 0) {
                _uiState.value = _uiState.value.copy(countdown = i)
                if (i == 0) break
                delay(1000)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            authRepository.login(email.trim(), password.trim()).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(loading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = refreshState().copy(
                            loading = false,
                            loginSuccess = true,
                            message = "欢迎回来，${resource.data.nickname}"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            message = resource.message
                        )
                    }
                }
            }
        }
    }

    fun register(email: String, code: String, nickname: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            authRepository.register(email.trim(), code.trim(), nickname.trim(), password.trim()).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(loading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = refreshState().copy(
                            loading = false,
                            loginSuccess = true,
                            message = "注册成功，欢迎 ${resource.data.nickname}"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            message = resource.message
                        )
                    }
                }
            }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            authRepository.resetPassword(email.trim(), code.trim(), newPassword.trim()).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(loading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            resetPasswordSuccess = true,
                            message = "密码重置成功，请使用新密码登录",
                            mode = LoginMode.LOGIN
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            loading = false,
                            message = resource.message
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = refreshState().copy(message = "已退出登录")
    }

    private fun refreshState(): LoginUiState = LoginUiState(
        isLoggedIn = authRepository.isLoggedIn,
        nickname = authRepository.nickname,
        email = userPrefs.email,
        points = userPrefs.points,
        contributionCount = userPrefs.contributionCount
    )

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
