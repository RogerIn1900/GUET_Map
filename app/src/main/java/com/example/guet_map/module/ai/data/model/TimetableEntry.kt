package com.example.guet_map.module.ai.data.model

import com.example.guet_map.model.Location

/**
 * 课表条目
 */
data class TimetableEntry(
    val id: String,
    val userId: String,
    val courseName: String,
    val teacherName: String?,
    val classroomName: String,
    val locationId: String?,
    val dayOfWeek: Int,        // 1=周一, 7=周日
    val startPeriod: Int,       // 第几节课开始 (1-based)
    val endPeriod: Int,        // 第几节课结束
    val weekRange: String,      // e.g. "1-16", "3,5,7"
    val semester: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val location: Location?
        get() = if (latitude != null && longitude != null) {
            Location(
                locationId = locationId ?: "",
                name = classroomName,
                latitude = latitude,
                longitude = longitude,
                category = "教学楼",
                hasGuide = false
            )
        } else null

    fun formatTime(): String {
        val startTime = periodToTime(startPeriod)
        val endTime = periodToTime(endPeriod)
        return "$startTime - $endTime"
    }

    fun formatDayOfWeek(): String {
        return when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> "未知"
        }
    }

    companion object {
        private val PERIOD_TIMES = mapOf(
            1 to "08:00", 2 to "08:45", 3 to "09:45", 4 to "10:30",
            5 to "11:15", 6 to "14:00", 7 to "14:45", 8 to "15:45",
            9 to "16:30", 10 to "17:15", 11 to "19:30", 12 to "20:15",
            13 to "21:00", 14 to "21:45"
        )

        fun periodToTime(period: Int): String = PERIOD_TIMES[period] ?: "??:??"
    }
}
