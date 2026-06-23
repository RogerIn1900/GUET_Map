package com.example.guet_map.module.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 模块本地课表数据
 */
@Entity(tableName = "timetable_entries")
data class TimetableEntryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val courseName: String,
    val teacherName: String?,
    val classroomName: String,
    val locationId: String?,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekRange: String,
    val semester: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): com.example.guet_map.module.ai.data.model.TimetableEntry {
        return com.example.guet_map.module.ai.data.model.TimetableEntry(
            id = id,
            userId = userId,
            courseName = courseName,
            teacherName = teacherName,
            classroomName = classroomName,
            locationId = locationId,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            weekRange = weekRange,
            semester = semester,
            latitude = latitude,
            longitude = longitude
        )
    }
}
