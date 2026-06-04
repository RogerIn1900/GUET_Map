package com.example.guet_map.model

data class RecentGuide(
    val locationId: String,
    val locationName: String,
    val stepCount: Int,
    val contributor: String,
    val approvedAt: String
)
