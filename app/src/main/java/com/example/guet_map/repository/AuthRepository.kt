package com.example.guet_map.repository

import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.*
import com.example.guet_map.network.ApiService
import com.example.guet_map.module.ai.data.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs,
    private val favoriteRepository: LegacyFavoriteRepository,
    private val timetableRepository: TimetableRepository
) {

    fun sendCode(email: String, type: String = "login"): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.sendCode(SendCodeRequest(email, type))
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "发送失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("发送失败: ${e.localizedMessage}"))
        }
    }

    fun register(email: String, code: String, nickname: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.register(
                RegisterRequest(email.trim(), code.trim(), nickname.trim(), password.trim())
            )
            if (response.success && response.data != null) {
                userPrefs.login(email, response.data)
                val uid = response.data.userId.takeIf { it > 0 }?.toString() ?: email.ifBlank { UserPrefs.GUEST_USER_ID }
                favoriteRepository.switchUser(uid)
                timetableRepository.refreshUserId()
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "注册失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("注册失败: ${e.localizedMessage}"))
        }
    }

    fun login(email: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.login(LoginRequest(email.trim(), password.trim()))
            if (response.success && response.data != null) {
                userPrefs.login(email, response.data)
                favoriteRepository.switchUser(userPrefs.userId)
                favoriteRepository.syncFromServer()
                timetableRepository.refreshUserId()
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("登录失败: ${e.localizedMessage}"))
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.resetPassword(
                ResetPasswordRequest(email.trim(), code.trim(), newPassword.trim())
            )
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "重置失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("重置失败: ${e.localizedMessage}"))
        }
    }

    fun logout() {
        userPrefs.clearAll()
        userPrefs.userId = UserPrefs.GUEST_USER_ID
        favoriteRepository.switchUser(UserPrefs.GUEST_USER_ID)
        timetableRepository.refreshUserId()
    }

    val isLoggedIn: Boolean get() = userPrefs.isLoggedIn
    val nickname: String get() = userPrefs.nickname
    val userId: String get() = userPrefs.userId
    val points: Int get() = userPrefs.points
}
