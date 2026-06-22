package com.example.guet_map.backend.db

import com.example.guet_map.backend.model.Notification
import com.example.guet_map.backend.model.User
import com.example.guet_map.backend.model.VerificationCode
import com.example.guet_map.backend.model.CodeType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UserRepository {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun findByEmail(email: String): User? {
        return transaction {
            Users.select { Users.email eq email }
                .map { row ->
                    User(
                        id = row[Users.id],
                        email = row[Users.email],
                        passwordHash = row[Users.passwordHash],
                        nickname = row[Users.nickname],
                        points = row[Users.points],
                        contributionCount = row[Users.contributionCount],
                        createdAt = parseDateTime(row[Users.createdAt]),
                        updatedAt = parseDateTime(row[Users.updatedAt])
                    )
                }
                .firstOrNull()
        }
    }
    
    fun findById(id: Long): User? {
        return transaction {
            Users.select { Users.id eq id }
                .map { row ->
                    User(
                        id = row[Users.id],
                        email = row[Users.email],
                        passwordHash = row[Users.passwordHash],
                        nickname = row[Users.nickname],
                        points = row[Users.points],
                        contributionCount = row[Users.contributionCount],
                        createdAt = parseDateTime(row[Users.createdAt]),
                        updatedAt = parseDateTime(row[Users.updatedAt])
                    )
                }
                .firstOrNull()
        }
    }
    
    fun create(email: String, passwordHash: String, nickname: String): User {
        return transaction {
            val now = LocalDateTime.now()
            val nowStr = now.format(dateFormatter)
            val id = Users.insert {
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[Users.nickname] = nickname
                it[Users.points] = 0
                it[Users.contributionCount] = 0
                it[Users.createdAt] = nowStr
                it[Users.updatedAt] = nowStr
            } get Users.id
            
            User(
                id = id,
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                points = 0,
                contributionCount = 0,
                createdAt = now,
                updatedAt = now
            )
        }
    }
    
    fun updatePoints(userId: Long, newPoints: Int) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.points] = newPoints
                it[Users.updatedAt] = LocalDateTime.now().format(dateFormatter)
            }
        }
    }

    fun updatePassword(userId: Long, passwordHash: String) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.passwordHash] = passwordHash
                it[Users.updatedAt] = LocalDateTime.now().format(dateFormatter)
            }
        }
    }
    
    fun incrementPoints(userId: Long, amount: Int) {
        transaction {
            val user = Users.select { Users.id eq userId }.firstOrNull() ?: return@transaction
            val currentPoints = user[Users.points]
            Users.update({ Users.id eq userId }) {
                it[Users.points] = currentPoints + amount
                it[Users.updatedAt] = LocalDateTime.now().format(dateFormatter)
            }
        }
    }
    
    fun incrementContributionCount(userId: Long) {
        transaction {
            val user = Users.select { Users.id eq userId }.firstOrNull() ?: return@transaction
            val currentCount = user[Users.contributionCount]
            Users.update({ Users.id eq userId }) {
                it[Users.contributionCount] = currentCount + 1
                it[Users.updatedAt] = LocalDateTime.now().format(dateFormatter)
            }
        }
    }
    
    fun emailExists(email: String): Boolean {
        return transaction {
            Users.select { Users.email eq email }.any()
        }
    }
    
    private fun parseDateTime(str: String): LocalDateTime {
        return try {
            LocalDateTime.parse(str, dateFormatter)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

class VerificationCodeRepository {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun save(verificationCode: VerificationCode): VerificationCode {
        return transaction {
            // 先标记该邮箱的旧验证码为已使用
            VerificationCodes.update(
                { (VerificationCodes.email eq verificationCode.email) and (VerificationCodes.type eq verificationCode.type.name) }
            ) {
                it[used] = true
            }
            
            // 创建新验证码
            val expiresAtStr = verificationCode.expiresAt.format(dateFormatter)
            val id = VerificationCodes.insert {
                it[email] = verificationCode.email
                it[code] = verificationCode.code
                it[type] = verificationCode.type.name
                it[expiresAt] = expiresAtStr
                it[used] = false
                it[createdAt] = LocalDateTime.now().format(dateFormatter)
            } get VerificationCodes.id
            
            verificationCode.copy(id = id)
        }
    }
    
    fun findValidCode(email: String, code: String, type: CodeType): VerificationCode? {
        return transaction {
            val now = LocalDateTime.now()
            val nowStr = now.format(dateFormatter)
            VerificationCodes.select {
                (VerificationCodes.email eq email) and
                (VerificationCodes.code eq code) and
                (VerificationCodes.type eq type.name) and
                (VerificationCodes.used eq false) and
                (VerificationCodes.expiresAt greater nowStr)
            }
                .map { row ->
                    VerificationCode(
                        id = row[VerificationCodes.id],
                        email = row[VerificationCodes.email],
                        code = row[VerificationCodes.code],
                        type = CodeType.valueOf(row[VerificationCodes.type]),
                        expiresAt = parseDateTime(row[VerificationCodes.expiresAt]),
                        used = row[VerificationCodes.used],
                        createdAt = parseDateTime(row[VerificationCodes.createdAt])
                    )
                }
                .firstOrNull()
        }
    }
    
    fun findValidCodeByEmail(email: String, type: CodeType): VerificationCode? {
        return transaction {
            val now = LocalDateTime.now()
            val nowStr = now.format(dateFormatter)
            VerificationCodes.select {
                (VerificationCodes.email eq email) and
                (VerificationCodes.type eq type.name) and
                (VerificationCodes.used eq false) and
                (VerificationCodes.expiresAt greater nowStr)
            }
                .orderBy(VerificationCodes.id, SortOrder.DESC)
                .map { row ->
                    VerificationCode(
                        id = row[VerificationCodes.id],
                        email = row[VerificationCodes.email],
                        code = row[VerificationCodes.code],
                        type = CodeType.valueOf(row[VerificationCodes.type]),
                        expiresAt = parseDateTime(row[VerificationCodes.expiresAt]),
                        used = row[VerificationCodes.used],
                        createdAt = parseDateTime(row[VerificationCodes.createdAt])
                    )
                }
                .firstOrNull()
        }
    }
    
    fun markAsUsed(id: Long) {
        transaction {
            VerificationCodes.update({ VerificationCodes.id eq id }) {
                it[used] = true
            }
        }
    }
    
    private fun parseDateTime(str: String): LocalDateTime {
        return try {
            LocalDateTime.parse(str, dateFormatter)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

class NotificationRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun createNotification(
        userId: Long,
        type: String,
        title: String,
        body: String,
        locationId: String? = null
    ): Notification {
        return transaction {
            val nowStr = LocalDateTime.now().format(dateFormatter)
            val id = UserNotifications.insert {
                it[UserNotifications.userId] = userId
                it[UserNotifications.type] = type
                it[UserNotifications.title] = title
                it[UserNotifications.body] = body
                it[UserNotifications.locationId] = locationId
                it[UserNotifications.isRead] = false
                it[UserNotifications.createdAt] = nowStr
            } get UserNotifications.id

            Notification(
                id = id,
                userId = userId,
                type = type,
                title = title,
                body = body,
                locationId = locationId,
                isRead = false,
                createdAt = LocalDateTime.now()
            )
        }
    }

    fun getUserNotifications(userId: Long): List<Notification> {
        return transaction {
            UserNotifications.select { UserNotifications.userId eq userId }
                .orderBy(UserNotifications.createdAt, SortOrder.DESC)
                .map { row ->
                    Notification(
                        id = row[UserNotifications.id],
                        userId = row[UserNotifications.userId],
                        type = row[UserNotifications.type],
                        title = row[UserNotifications.title],
                        body = row[UserNotifications.body],
                        locationId = row[UserNotifications.locationId],
                        isRead = row[UserNotifications.isRead],
                        createdAt = parseDateTime(row[UserNotifications.createdAt])
                    )
                }
        }
    }

    fun markAsRead(userId: Long, notificationId: Long) {
        transaction {
            UserNotifications.update(
                { (UserNotifications.id eq notificationId) and (UserNotifications.userId eq userId) }
            ) {
                it[UserNotifications.isRead] = true
            }
        }
    }

    fun markAllAsRead(userId: Long) {
        transaction {
            UserNotifications.update({ UserNotifications.userId eq userId }) {
                it[UserNotifications.isRead] = true
            }
        }
    }

    fun getUnreadCount(userId: Long): Int {
        return transaction {
            UserNotifications.select {
                (UserNotifications.userId eq userId) and (UserNotifications.isRead eq false)
            }.count().toInt()
        }
    }

    private fun parseDateTime(str: String): LocalDateTime {
        return try {
            LocalDateTime.parse(str, dateFormatter)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}
