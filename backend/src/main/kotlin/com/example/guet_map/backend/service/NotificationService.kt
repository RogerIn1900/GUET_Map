package com.example.guet_map.backend.service

import com.example.guet_map.backend.db.NotificationRepository
import com.example.guet_map.backend.db.UserRepository
import com.example.guet_map.backend.model.Notification

class NotificationService(
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val userRepository: UserRepository = UserRepository()
) {
    
    fun getUserNotifications(userId: Long): List<Notification> {
        return notificationRepository.getUserNotifications(userId)
    }
    
    fun markAsRead(userId: Long, notificationId: Long) {
        notificationRepository.markAsRead(userId, notificationId)
    }
    
    fun markAllAsRead(userId: Long) {
        notificationRepository.markAllAsRead(userId)
    }
    
    fun getUnreadCount(userId: Long): Int {
        return notificationRepository.getUnreadCount(userId)
    }
    
    fun createNotification(userId: Long, type: String, title: String, body: String, locationId: String? = null): Notification {
        return notificationRepository.createNotification(userId, type, title, body, locationId)
    }
    
    fun createGuideApprovedNotification(userId: Long, locationName: String, points: Int, locationId: String?) {
        val user = userRepository.findById(userId)
        val currentPoints = user?.points ?: 0
        notificationRepository.createNotification(
            userId = userId,
            type = "review",
            title = "指引审核通过",
            body = "您提交的「$locationName」步骤已通过审核！+${points}积分，当前积分：${currentPoints + points}",
            locationId = locationId
        )
    }
    
    fun createPointsNotification(userId: Long, reason: String, points: Int) {
        val user = userRepository.findById(userId)
        val currentPoints = user?.points ?: 0
        notificationRepository.createNotification(
            userId = userId,
            type = "points",
            title = "积分到账",
            body = "$reason +$points 积分，当前积分：${currentPoints + points}",
            locationId = null
        )
    }
}
