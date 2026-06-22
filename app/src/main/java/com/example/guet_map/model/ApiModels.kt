package com.example.guet_map.model

import com.google.gson.annotations.SerializedName

// ========== Auth 相关 ==========

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val nickname: String,
    val email: String,
    val points: Int,
    val contributionCount: Int = 0,
    val userId: Long = 0
)

data class SendCodeRequest(
    val email: String,
    val type: String = "login"
)

data class RegisterRequest(
    val email: String,
    val code: String,
    val nickname: String,
    val password: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

// ========== 好友系统模型 ==========

data class FriendInfo(
    val id: Long,
    val friendId: Long,
    val nickname: String,
    val email: String,
    val status: String,
    val createdAt: String
)

data class UserSearchResult(
    val userId: Long,
    val nickname: String,
    val email: String,
    val userCode: String
)

// ========== 朋友圈动态模型 ==========

data class Post(
    val id: Long,
    val userId: Long,
    val nickname: String,
    val content: String,
    val locationId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val createdAt: String,
    val commentCount: Int
)

data class CreatePostRequest(
    val content: String,
    val locationId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)

data class CreateCommentRequest(
    val content: String
)

// ========== 评论模型 ==========

data class Comment(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val nickname: String,
    val content: String,
    val createdAt: String
)

// ========== 好友定位模型 ==========

data class FriendLocation(
    val userId: Long,
    val nickname: String,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: String
)

data class UpdateLocationRequest(
    val latitude: Double,
    val longitude: Double
)
