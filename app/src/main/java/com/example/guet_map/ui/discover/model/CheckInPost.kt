package com.example.guet_map.ui.discover.model

data class CheckInPost(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val locationId: String,
    val locationName: String,
    val content: String,
    val imageUrls: List<String>,
    val topics: List<String>,
    val timestamp: Long,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false
)
