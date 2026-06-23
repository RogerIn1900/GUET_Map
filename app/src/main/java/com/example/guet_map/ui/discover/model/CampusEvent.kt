package com.example.guet_map.ui.discover.model

enum class EventCategory(val displayName: String, val emoji: String) {
    CULTURE("文娱", "🎭"),
    SPORTS("体育", "⚽"),
    ACADEMIC("学术", "📚"),
    VOLUNTEER("志愿", "❤️"),
    CAREER("就业", "💼"),
    OTHER("其他", "📌")
}

enum class EventStatus {
    UPCOMING,
    ONGOING,
    ENDED
}

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: Long,
    val endTime: Long,
    val organizer: String,
    val category: EventCategory,
    val status: EventStatus,
    val attendeeCount: Int,
    val maxAttendees: Int?,
    val registrationRequired: Boolean = false,
    val isFull: Boolean = false
)
