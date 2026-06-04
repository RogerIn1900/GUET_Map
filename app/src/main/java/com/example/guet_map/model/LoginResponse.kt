package com.example.guet_map.model

data class LoginResponse(
    val token: String,
    val nickname: String,
    val points: Int,
    val contributionCount: Int = 0
)
