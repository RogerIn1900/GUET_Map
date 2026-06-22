package com.example.guet_map.model

data class AppNotification(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val locationId: String? = null,
    val isRead: Boolean = false,
    val createdAt: String = "",
    val userId: String = ""
)
