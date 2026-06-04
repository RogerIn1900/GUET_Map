package com.example.guet_map.model

data class MyGuideSubmission(
    val id: Long,
    val locationId: String,
    val locationName: String,
    val status: String,
    val stepNumber: Int,
    val description: String,
    val rejectReason: String? = null,
    val submittedAt: String
)
