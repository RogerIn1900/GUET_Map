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
    val avatar: String?,
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
    val userId: Long,
    val nickname: String,
    val avatar: String?,
    val email: String
)

data class FriendRequest(
    val id: Long,
    val fromUserId: Long,
    val toUserId: Long,
    val status: String,
    val message: String?,
    val createdAt: String
)

data class FriendRequestWithUser(
    val request: FriendRequest,
    val fromUserInfo: FriendInfo?
)

data class UserSearchResult(
    val user: FriendInfo,
    val isFriend: Boolean,
    val hasPendingRequest: Boolean
)

data class UserProfile(
    val userId: Long,
    val email: String,
    val nickname: String,
    val avatar: String?,
    val points: Int,
    val contributionCount: Int,
    val friendCount: Int,
    val unreadMessages: Int,
    val pendingFriendRequests: Int
)

// ========== 聊天消息模型 ==========

data class Message(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val type: String = "text",
    val isRead: Boolean = false,
    val createdAt: String
)

// ========== 朋友圈动态模型 ==========

data class Post(
    val id: Long,
    val userId: Long,
    val content: String,
    val locationId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val images: String?,
    val visibility: String = "public",
    val createdAt: String
)

data class PostWithDetails(
    val post: Post,
    val userInfo: FriendInfo?,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean
)

data class CreatePostRequest(
    val content: String,
    val locationId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val images: String? = null,
    val visibility: String = "public"
)

data class CreateCommentRequest(
    val content: String
)

// ========== 评论模型 ==========

data class Comment(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val content: String,
    val createdAt: String
)

data class CommentWithUser(
    val comment: Comment,
    val userInfo: FriendInfo?
)

// ========== 好友定位模型 ==========

data class FriendLocation(
    val userId: Long,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: String
)

data class UpdateLocationRequest(
    val latitude: Double,
    val longitude: Double
)

// ========== 好友申请请求 ==========

data class SendFriendRequestBody(
    val userId: Long,
    val message: String? = null
)

data class HandleFriendRequestBody(
    val accept: Boolean
)

// ========== 发送消息请求 ==========

data class SendMessageBody(
    val receiverId: Long,
    val content: String,
    val type: String = "text"
)

// ========== 通用响应 ==========

data class UnreadCountResponse(
    val count: Int
)

data class LikeResponse(
    val liked: Boolean
)
