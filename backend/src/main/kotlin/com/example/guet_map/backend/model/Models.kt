package com.example.guet_map.backend.model

import java.time.LocalDateTime

data class User(
    val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val nickname: String,
    val points: Int = 0,
    val contributionCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class VerificationCode(
    val id: Long = 0,
    val email: String,
    val code: String,
    val type: CodeType,
    val expiresAt: LocalDateTime,
    val used: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class CodeType {
    REGISTER,
    RESET_PASSWORD,
    LOGIN
}

data class Notification(
    val id: Long = 0,
    val userId: Long,
    val type: String, // review, points, announcement, system
    val title: String,
    val body: String,
    val locationId: String? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
