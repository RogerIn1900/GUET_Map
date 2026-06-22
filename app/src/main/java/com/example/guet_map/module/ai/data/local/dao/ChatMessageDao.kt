package com.example.guet_map.module.ai.data.local.dao

import androidx.room.*
import com.example.guet_map.module.ai.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 对话消息 DAO
 */
@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND userId = :userId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: String, userId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(sessionId: String, userId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun deleteSession(sessionId: String, userId: String)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
