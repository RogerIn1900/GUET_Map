package com.example.guet_map.backend.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 191).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val nickname = varchar("nickname", 100)
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
