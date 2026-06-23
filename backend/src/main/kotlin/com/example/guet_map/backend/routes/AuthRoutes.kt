package com.example.guet_map.backend.routes

import com.example.guet_map.backend.model.*
import com.example.guet_map.backend.service.AuthService
import com.example.guet_map.backend.service.JwtService
import com.example.guet_map.backend.service.NotificationService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*

fun Route.authRoutes(authService: AuthService, jwtService: JwtService, notificationService: NotificationService) {
    
    // 发送验证码
    post("/api/v1/auth/send-code") {
        val request = call.receive<SendCodeRequest>()
        
        if (request.email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("邮箱不能为空"))
            return@post
        }
        
        val result = authService.sendVerificationCode(request.email.trim(), request.type)
        
        if (result.isSuccess) {
            call.respond(successResponse())
        } else {
            call.respond(HttpStatusCode.BadRequest, errorResponse(result.exceptionOrNull()?.message ?: "发送验证码失败"))
        }
    }
    
    // 注册
    post("/api/v1/auth/register") {
        val request = call.receive<RegisterRequest>()
        
        if (request.email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("邮箱不能为空"))
            return@post
        }
        
        if (request.code.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("验证码不能为空"))
            return@post
        }
        
        if (request.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("密码不能为空"))
            return@post
        }
        
        if (request.nickname.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("昵称不能为空"))
            return@post
        }
        
        val result = authService.register(
            request.email.trim(),
            request.code.trim(),
            request.password,
            request.nickname.trim()
        )
        
        if (result.isSuccess) {
            call.respond(successResponse(result.getOrThrow()))
        } else {
            call.respond(HttpStatusCode.BadRequest, errorResponse(result.exceptionOrNull()?.message ?: "注册失败"))
        }
    }
    
    // 登录
    post("/api/v1/auth/login") {
        val request = call.receive<LoginRequest>()

        if (request.email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("邮箱不能为空"))
            return@post
        }

        if (request.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("密码不能为空"))
            return@post
        }

        val result = authService.login(request.email.trim(), request.password.trim())

        if (result.isSuccess) {
            call.respond(successResponse(result.getOrThrow()))
        } else {
            call.respond(HttpStatusCode.BadRequest, errorResponse(result.exceptionOrNull()?.message ?: "登录失败"))
        }
    }

    // 重置密码
    post("/api/v1/auth/reset-password") {
        val request = call.receive<ResetPasswordRequest>()

        if (request.email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("邮箱不能为空"))
            return@post
        }

        if (request.code.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("验证码不能为空"))
            return@post
        }

        if (request.newPassword.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("新密码不能为空"))
            return@post
        }

        val result = authService.resetPassword(
            request.email.trim(),
            request.code.trim(),
            request.newPassword
        )

        if (result.isSuccess) {
            call.respond(successResponse())
        } else {
            call.respond(HttpStatusCode.BadRequest, errorResponse(result.exceptionOrNull()?.message ?: "重置失败"))
        }
    }
    
    // 获取当前用户信息
    get("/api/v1/auth/me") {
        val authHeader = call.request.header("Authorization") ?: ""
        if (!authHeader.startsWith("Bearer ")) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("未授权"))
            return@get
        }
        
        val token = authHeader.removePrefix("Bearer ").trim()
        val payload = jwtService.verifyToken(token)
        
        if (payload == null) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("无效的token"))
            return@get
        }
        
        val user = authService.getUserById(payload.userId)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, errorResponse("用户不存在"))
            return@get
        }
        
        call.respond(successResponse(mapOf(
            "userId" to user.id,
            "email" to user.email,
            "nickname" to user.nickname,
            "points" to user.points,
            "contributionCount" to user.contributionCount
        )))
    }
}

fun Route.notificationRoutes(notificationService: NotificationService, jwtService: JwtService) {
    
    // 获取用户通知列表
    get("/api/v1/notifications") {
        val authHeader = call.request.header("Authorization") ?: ""
        if (!authHeader.startsWith("Bearer ")) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("未授权"))
            return@get
        }
        
        val token = authHeader.removePrefix("Bearer ").trim()
        val payload = jwtService.verifyToken(token)
        
        if (payload == null) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("无效的token"))
            return@get
        }
        
        val notifications = notificationService.getUserNotifications(payload.userId)
        call.respond(successResponse(notifications.map { it.toResponse() }))
    }
    
    // 标记通知已读
    put("/api/v1/notifications/{id}/read") {
        val authHeader = call.request.header("Authorization") ?: ""
        if (!authHeader.startsWith("Bearer ")) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("未授权"))
            return@put
        }
        
        val token = authHeader.removePrefix("Bearer ").trim()
        val payload = jwtService.verifyToken(token)
        
        if (payload == null) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("无效的token"))
            return@put
        }
        
        val notificationId = call.parameters["id"]?.toLongOrNull()
        if (notificationId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的通知ID"))
            return@put
        }
        
        notificationService.markAsRead(payload.userId, notificationId)
        call.respond(successResponse())
    }
    
    // 标记全部已读
    put("/api/v1/notifications/read-all") {
        val authHeader = call.request.header("Authorization") ?: ""
        if (!authHeader.startsWith("Bearer ")) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("未授权"))
            return@put
        }
        
        val token = authHeader.removePrefix("Bearer ").trim()
        val payload = jwtService.verifyToken(token)
        
        if (payload == null) {
            call.respond(HttpStatusCode.Unauthorized, errorResponse("无效的token"))
            return@put
        }
        
        notificationService.markAllAsRead(payload.userId)
        call.respond(successResponse())
    }
}

private fun com.example.guet_map.backend.model.Notification.toResponse() = NotificationResponse(
    id = id,
    type = type,
    title = title,
    body = body,
    locationId = locationId,
    isRead = isRead,
    createdAt = createdAt.toString()
)
