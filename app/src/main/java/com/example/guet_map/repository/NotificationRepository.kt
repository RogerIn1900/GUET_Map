package com.example.guet_map.repository

import com.example.guet_map.core.dao.NotificationDao
import com.example.guet_map.core.entity.NotificationEntity
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.AppNotification
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao,
    private val userPrefs: UserPrefs
) {

    private val currentUserId: String
        get() = userPrefs.userId.ifBlank { UserPrefs.GUEST_USER_ID }

    fun observeNotifications(): Flow<List<AppNotification>> =
        notificationDao.observeAll(currentUserId).map { list -> list.map { it.toDomain() } }

    fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount(currentUserId)

    suspend fun refresh() {
        if (!userPrefs.isLoggedIn) return
        
        try {
            val remote = apiService.getNotifications()
            notificationDao.deleteAllForUser(currentUserId)
            notificationDao.insertAll(remote.map { it.toEntity() })
        } catch (_: Exception) {
            // 使用缓存
        }
    }

    suspend fun markAllRead() {
        notificationDao.markAllRead(currentUserId)
        // 同步到服务器
        if (userPrefs.isLoggedIn) {
            try {
                apiService.markAllNotificationsRead()
            } catch (_: Exception) {
                // 忽略错误，本地已更新
            }
        }
    }

    suspend fun markRead(id: Long) {
        notificationDao.markRead(id, currentUserId)
        // 同步到服务器
        if (userPrefs.isLoggedIn) {
            try {
                apiService.markNotificationRead(id)
            } catch (_: Exception) {
                // 忽略错误，本地已更新
            }
        }
    }

    private fun AppNotification.toEntity() = NotificationEntity(
        id = id,
        type = type,
        title = title,
        body = body,
        locationId = locationId,
        isRead = isRead,
        createdAt = createdAt,
        userId = currentUserId
    )

    private fun NotificationEntity.toDomain() = AppNotification(
        id = id,
        type = type,
        title = title,
        body = body,
        locationId = locationId,
        isRead = isRead,
        createdAt = createdAt,
        userId = userId
    )
}
