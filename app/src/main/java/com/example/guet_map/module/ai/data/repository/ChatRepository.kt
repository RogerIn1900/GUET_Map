package com.example.guet_map.module.ai.data.repository

import com.example.guet_map.module.ai.data.local.dao.ChatMessageDao
import com.example.guet_map.module.ai.data.local.entity.ChatMessageEntity
import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.data.model.ChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 对话仓库
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val userPrefs: com.example.guet_map.data.UserPrefs
) {

    private val activeUserId: String
        get() = userPrefs.userId.ifBlank { com.example.guet_map.data.UserPrefs.GUEST_USER_ID }

    fun getMessages(sessionId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesBySession(sessionId, activeUserId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveMessage(
        sessionId: String,
        role: ChatRole,
        content: String,
        locationId: String? = null
    ): ChatMessage {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = role,
            content = content,
            locationId = locationId
        )
        chatMessageDao.insertMessage(message.toEntity(sessionId, activeUserId))
        return message
    }

    suspend fun deleteSession(sessionId: String) {
        chatMessageDao.deleteSession(sessionId, activeUserId)
    }

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        role = ChatRole.valueOf(role),
        content = content,
        timestamp = timestamp,
        locationId = locationId
    )

    private fun ChatMessage.toEntity(sessionId: String, userId: String) = ChatMessageEntity(
        id = id,
        role = role.name,
        content = content,
        timestamp = timestamp,
        locationId = locationId,
        sessionId = sessionId,
        userId = userId
    )
}
