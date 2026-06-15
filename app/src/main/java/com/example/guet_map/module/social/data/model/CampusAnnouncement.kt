package com.example.guet_map.module.social.data.model

data class CampusAnnouncement(
    val id: String,
    val title: String,
    val content: String,
    val category: AnnouncementCategory,
    val priority: Int = 0,
    val publishTime: Long,
    val author: String,
    val viewCount: Int = 0,
    val isPinned: Boolean = false,
    val isRead: Boolean = false
)

enum class AnnouncementCategory(val displayName: String) {
    GENERAL("综合通知"),
    ACADEMIC("学术讲座"),
    ACTIVITY("校园活动"),
    CAREER("就业招聘"),
    MAINTENANCE("系统维护"),
    EMERGENCY("紧急通知")
}
