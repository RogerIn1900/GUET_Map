package com.example.guet_map.backend.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 191).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val nickname = varchar("nickname", 100)
    val avatar = varchar("avatar", 500).nullable() // 本地头像路径
    val points = integer("points").default(0)
    val contributionCount = integer("contribution_count").default(0)
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())
    val updatedAt = varchar("updated_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

object VerificationCodes : Table("verification_codes") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 191)
    val code = varchar("code", 10)
    val type = varchar("type", 50) // REGISTER, RESET_PASSWORD
    val expiresAt = varchar("expires_at", 50)
    val used = bool("used").default(false)
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

object UserNotifications : Table("user_notifications") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val type = varchar("type", 50) // review, points, announcement, system
    val title = varchar("title", 200)
    val body = varchar("body", 500)
    val locationId = varchar("location_id", 50).nullable()
    val isRead = bool("is_read").default(false)
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 好友关系 ==========
object Friends : Table("friends") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val friendId = long("friend_id")
    val status = varchar("status", 20).default("accepted") // pending, accepted, rejected
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 好友申请 ==========
object FriendRequests : Table("friend_requests") {
    val id = long("id").autoIncrement()
    val fromUserId = long("from_user_id")
    val toUserId = long("to_user_id")
    val status = varchar("status", 20).default("pending") // pending, accepted, rejected
    val message = varchar("message", 200).nullable() // 申请留言
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 聊天消息 ==========
object Messages : Table("messages") {
    val id = long("id").autoIncrement()
    val senderId = long("sender_id")
    val receiverId = long("receiver_id")
    val content = varchar("content", 2000)
    val type = varchar("type", 20).default("text") // text, image
    val isRead = bool("is_read").default(false)
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 朋友圈帖子 ==========
object Posts : Table("posts") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val content = varchar("content", 2000)
    val locationId = varchar("location_id", 50).nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val images = varchar("images", 2000).nullable() // JSON 数组存储图片路径
    val visibility = varchar("visibility", 20).default("public") // public, friends_only
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 帖子评论 ==========
object PostComments : Table("post_comments") {
    val id = long("id").autoIncrement()
    val postId = long("post_id")
    val userId = long("user_id")
    val content = varchar("content", 500)
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 帖子点赞 ==========
object PostLikes : Table("post_likes") {
    val id = long("id").autoIncrement()
    val postId = long("post_id")
    val userId = long("user_id")
    val createdAt = varchar("created_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}

// ========== 用户位置 ==========
object UserLocations : Table("user_locations") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").uniqueIndex()
    val latitude = double("latitude")
    val longitude = double("longitude")
    val updatedAt = varchar("updated_at", 50).default(LocalDateTime.now().toString())

    override val primaryKey = PrimaryKey(id)
}
