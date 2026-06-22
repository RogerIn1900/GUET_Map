package com.example.guet_map.backend.model

import com.google.gson.annotations.SerializedName

// ========== 请求模型 ==========

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("password") val password: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("newPassword") val newPassword: String
)

data class SendCodeRequest(
    @SerializedName("email") val email: String,
    @SerializedName("type") val type: String // "login" or "register"
)

// ========== 响应模型 ==========

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("points") val points: Int,
    @SerializedName("contributionCount") val contributionCount: Int = 0,
    @SerializedName("userId") val userId: Long = 0
)

// ========== 通知模型 ==========

data class NotificationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("locationId") val locationId: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String
)

// ========== 工具函数 ==========

fun <T> successResponse(data: T): ApiResponse<T> = ApiResponse(
    success = true,
    data = data
)

fun successResponse(): ApiResponse<Unit> = ApiResponse(
    success = true
)

fun errorResponse(message: String): ApiResponse<Nothing> = ApiResponse(
    success = false,
    message = message
)
