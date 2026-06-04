package com.example.guet_map.repository

import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.LoginRequest
import com.example.guet_map.model.LoginResponse
import com.example.guet_map.model.Resource
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs
) {

    fun login(username: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.login(LoginRequest(username, password))
            userPrefs.login(response)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("登录失败: ${e.localizedMessage}"))
        }
    }

    fun logout() {
        userPrefs.clearAll()
    }

    val isLoggedIn: Boolean get() = userPrefs.isLoggedIn
    val nickname: String get() = userPrefs.nickname
}
