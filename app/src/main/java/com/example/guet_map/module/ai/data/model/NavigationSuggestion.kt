package com.example.guet_map.module.ai.data.model

/**
 * AI 基于课表推理出的导航建议
 */
data class NavigationSuggestion(
    val courseName: String,
    val classroomName: String,
    val locationId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val startPeriod: Int,
    val endPeriod: Int,
    val departureTime: String,     // 建议出发时间
    val arriveTime: String,        // 预计到达时间
    val walkingMinutes: Int,      // 预计步行时间(分钟)
    val warningMessage: String?,   // 迟到预警信息
    val confidence: Float         // 置信度 0-1
) {
    val isUrgent: Boolean
        get() = warningMessage != null

    val targetLocation: com.example.guet_map.model.Location?
        get() = if (latitude != null && longitude != null) {
            com.example.guet_map.model.Location(
                locationId = locationId ?: "",
                name = classroomName,
                latitude = latitude,
                longitude = longitude,
                category = "教学楼",
                hasGuide = false
            )
        } else null
}
