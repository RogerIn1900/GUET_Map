package com.example.guet_map.backend.routes

import com.example.guet_map.backend.db.*
import com.example.guet_map.backend.model.*
import com.example.guet_map.backend.service.JwtService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import com.google.gson.Gson

// ========== 认证辅助函数 ==========

private suspend fun ApplicationCall.authenticate(jwtService: JwtService): Long? {
    val authHeader = request.header("Authorization") ?: ""
    if (!authHeader.startsWith("Bearer ")) {
        respond(HttpStatusCode.Unauthorized, errorResponse("未授权"))
        return null
    }
    
    val token = authHeader.removePrefix("Bearer ").trim()
    val payload = jwtService.verifyToken(token)
    
    if (payload == null) {
        respond(HttpStatusCode.Unauthorized, errorResponse("无效的token"))
        return null
    }
    
    return payload.userId
}

// ========== 好友相关路由 ==========

fun Route.friendRoutes(jwtService: JwtService) {
    
    // 获取好友列表
    get("/api/v1/friends") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val friends = friendRepository.getFriends(userId)
        call.respond(successResponse(friends))
    }
    
    // 搜索用户（通过邮箱）
    get("/api/v1/users/search") {
        val currentUserId = call.authenticate(jwtService) ?: return@get
        
        val email = call.parameters["email"]
        val userId = call.parameters["userId"]?.toLongOrNull()
        
        val user = when {
            email != null -> friendRepository.findUserByEmail(email)
            userId != null -> friendRepository.findUserById(userId)
            else -> {
                call.respond(HttpStatusCode.BadRequest, errorResponse("需要提供邮箱或用户ID"))
                return@get
            }
        }
        
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, errorResponse("用户不存在"))
            return@get
        }
        
        // 检查是否已经是好友
        val isFriend = friendRepository.isFriend(currentUserId, user.userId)
        val hasPendingRequest = friendRepository.getReceivedRequests(currentUserId).any { 
            it.fromUserInfo?.userId == user.userId 
        }
        
        call.respond(successResponse(mapOf(
            "user" to user,
            "isFriend" to isFriend,
            "hasPendingRequest" to hasPendingRequest
        )))
    }
    
    // 发送好友申请
    post("/api/v1/friend-requests") {
        val userId = call.authenticate(jwtService) ?: return@post

        val request = call.receive<Map<String, @JvmSuppressWildcards Any>>()
        val toUserId = (request["userId"] as? Number)?.toLong()
        val message = request["message"] as? String

        if (toUserId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("需要提供目标用户ID"))
            return@post
        }

        if (toUserId == userId) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("不能添加自己为好友"))
            return@post
        }

        val result = friendRepository.sendFriendRequest(userId, toUserId, message)
        when (result) {
            is FriendRepository.SendFriendRequestResult.AlreadyFriends -> {
                call.respond(HttpStatusCode.BadRequest, errorResponse("你们已经是好友了，请检查好友列表"))
            }
            is FriendRepository.SendFriendRequestResult.AlreadySent -> {
                call.respond(HttpStatusCode.BadRequest, errorResponse("你已经发送过好友申请了，请等待对方处理"))
            }
            is FriendRepository.SendFriendRequestResult.ReceivedPending -> {
                call.respond(HttpStatusCode.BadRequest, errorResponse("对方已向你发送好友申请，请到好友请求页面接受"))
            }
            is FriendRepository.SendFriendRequestResult.Success -> {
                call.respond(successResponse(result.request))
            }
        }
    }
    
    // 获取收到的好友申请
    get("/api/v1/friend-requests/received") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val requests = friendRepository.getReceivedRequests(userId)
        call.respond(successResponse(requests))
    }
    
    // 处理好友申请
    put("/api/v1/friend-requests/{id}") {
        val userId = call.authenticate(jwtService) ?: return@put
        
        val requestId = call.parameters["id"]?.toLongOrNull()
        if (requestId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的申请ID"))
            return@put
        }
        
        val requestBody = call.receive<Map<String, String>>()
        val accept = requestBody["accept"]?.toBoolean() ?: true
        
        val success = friendRepository.handleFriendRequest(requestId, accept)
        if (!success) {
            call.respond(HttpStatusCode.NotFound, errorResponse("申请不存在"))
            return@put
        }
        
        call.respond(successResponse())
    }
    
    // 删除好友
    delete("/api/v1/friends/{id}") {
        val userId = call.authenticate(jwtService) ?: return@delete
        
        val friendId = call.parameters["id"]?.toLongOrNull()
        if (friendId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的好友ID"))
            return@delete
        }
        
        friendRepository.removeFriend(userId, friendId)
        call.respond(successResponse())
    }
}

// ========== 聊天消息路由 ==========

fun Route.messageRoutes(jwtService: JwtService) {
    
    // 获取与某个用户的聊天记录
    get("/api/v1/messages/{userId}") {
        val currentUserId = call.authenticate(jwtService) ?: return@get
        
        val otherUserId = call.parameters["userId"]?.toLongOrNull()
        if (otherUserId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的用户ID"))
            return@get
        }
        
        val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
        val messages = messageRepository.getMessages(currentUserId, otherUserId, limit)
        
        // 标记消息为已读
        messageRepository.markAsRead(currentUserId, otherUserId)
        
        call.respond(successResponse(messages))
    }
    
    // 发送消息
    post("/api/v1/messages") {
        val senderId = call.authenticate(jwtService) ?: return@post
        
        val request = call.receive<Map<String, String>>()
        val receiverId = request["receiverId"]?.toLongOrNull()
        val content = request["content"]
        val type = request["type"] ?: "text"
        
        if (receiverId == null || content.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("需要提供接收者ID和消息内容"))
            return@post
        }
        
        if (receiverId == senderId) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("不能给自己发消息"))
            return@post
        }
        
        val message = messageRepository.sendMessage(senderId, receiverId, content, type)
        call.respond(successResponse(message))
    }
    
    // 获取未读消息数量
    get("/api/v1/messages/unread-count") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val count = messageRepository.getUnreadCount(userId)
        call.respond(successResponse(mapOf("count" to count)))
    }
    
    // 获取与某人的未读消息数
    get("/api/v1/messages/unread-count/{userId}") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val fromUserId = call.parameters["userId"]?.toLongOrNull()
        if (fromUserId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的用户ID"))
            return@get
        }
        
        val count = messageRepository.getUnreadCountFrom(userId, fromUserId)
        call.respond(successResponse(mapOf("count" to count)))
    }
}

// ========== 朋友圈帖子路由 ==========

fun Route.postRoutes(jwtService: JwtService) {
    val gson = Gson()
    
    // 获取朋友圈帖子
    get("/api/v1/posts") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
        val offset = call.parameters["offset"]?.toIntOrNull() ?: 0
        
        val friendIds = friendRepository.getFriends(userId).map { it.userId }
        val posts = postRepository.getPosts(userId, friendIds, limit, offset)
        
        call.respond(successResponse(posts))
    }
    
    // 获取某个地点的帖子
    get("/api/v1/posts/location/{locationId}") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val friendIds = friendRepository.getFriends(userId).map { it.userId }
        val posts = postRepository.getPostsByLocation(
            call.parameters["locationId"] ?: "", userId, friendIds
        )
        
        call.respond(successResponse(posts))
    }
    
    // 发布帖子
    post("/api/v1/posts") {
        val userId = call.authenticate(jwtService) ?: return@post
        
        val request = call.receive<Map<String, Any?>>()
        val content = request["content"] as? String
        val locationId = request["locationId"] as? String
        val latitude = (request["latitude"] as? Number)?.toDouble()
        val longitude = (request["longitude"] as? Number)?.toDouble()
        val images = request["images"]?.let { 
            if (it is List<*>) gson.toJson(it) else it.toString() 
        }
        val visibility = request["visibility"] as? String ?: "public"
        
        if (content.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("内容不能为空"))
            return@post
        }
        
        val post = postRepository.createPost(
            userId = userId,
            content = content,
            locationId = locationId,
            latitude = latitude,
            longitude = longitude,
            images = images,
            visibility = visibility
        )
        
        call.respond(successResponse(post))
    }
    
    // 删除帖子
    delete("/api/v1/posts/{id}") {
        val userId = call.authenticate(jwtService) ?: return@delete
        
        val postId = call.parameters["id"]?.toLongOrNull()
        if (postId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的帖子ID"))
            return@delete
        }
        
        val success = postRepository.deletePost(postId, userId)
        if (!success) {
            call.respond(HttpStatusCode.NotFound, errorResponse("帖子不存在或无权限删除"))
            return@delete
        }
        
        call.respond(successResponse())
    }
    
    // 点赞/取消点赞
    post("/api/v1/posts/{id}/like") {
        val userId = call.authenticate(jwtService) ?: return@post
        
        val postId = call.parameters["id"]?.toLongOrNull()
        if (postId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的帖子ID"))
            return@post
        }
        
        val liked = postRepository.toggleLike(postId, userId)
        call.respond(successResponse(mapOf("liked" to liked)))
    }
    
    // 获取帖子评论
    get("/api/v1/posts/{id}/comments") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val postId = call.parameters["id"]?.toLongOrNull()
        if (postId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的帖子ID"))
            return@get
        }
        
        val comments = postRepository.getComments(postId)
        call.respond(successResponse(comments))
    }
    
    // 添加评论
    post("/api/v1/posts/{id}/comments") {
        val userId = call.authenticate(jwtService) ?: return@post
        
        val postId = call.parameters["id"]?.toLongOrNull()
        if (postId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的帖子ID"))
            return@post
        }
        
        val request = call.receive<Map<String, String>>()
        val content = request["content"]
        
        if (content.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("评论内容不能为空"))
            return@post
        }
        
        val comment = postRepository.addComment(postId, userId, content)
        call.respond(successResponse(comment))
    }
}

// ========== 用户位置路由 ==========

fun Route.locationRoutes(jwtService: JwtService) {
    
    // 更新自己的位置
    put("/api/v1/location") {
        val userId = call.authenticate(jwtService) ?: return@put
        
        val request = call.receive<Map<String, Number>>()
        val latitude = request["latitude"]?.toDouble()
        val longitude = request["longitude"]?.toDouble()
        
        if (latitude == null || longitude == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("需要提供经纬度"))
            return@put
        }
        
        userLocationRepository.updateLocation(userId, latitude, longitude)
        call.respond(successResponse())
    }
    
    // 获取好友位置
    get("/api/v1/locations/friends") {
        val userId = call.authenticate(jwtService) ?: return@get
        
        val friendIds = friendRepository.getFriends(userId).map { it.userId }
        if (friendIds.isEmpty()) {
            call.respond(successResponse(emptyList<UserLocation>()))
            return@get
        }
        
        val locations = userLocationRepository.getLocations(friendIds)
        call.respond(successResponse(locations))
    }
    
    // 获取某个用户的位置
    get("/api/v1/locations/{userId}") {
        val currentUserId = call.authenticate(jwtService) ?: return@get
        
        val targetUserId = call.parameters["userId"]?.toLongOrNull()
        if (targetUserId == null) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("无效的用户ID"))
            return@get
        }
        
        // 只有好友才能查看位置
        if (!friendRepository.isFriend(currentUserId, targetUserId)) {
            call.respond(HttpStatusCode.Forbidden, errorResponse("只能查看好友的位置"))
            return@get
        }
        
        val location = userLocationRepository.getLocation(targetUserId)
        if (location == null) {
            call.respond(HttpStatusCode.NotFound, errorResponse("该用户未开启位置共享"))
            return@get
        }
        
        call.respond(successResponse(location))
    }
}

// ========== 用户资料路由 ==========

fun Route.userRoutes(jwtService: JwtService) {
    
    // 更新头像
    put("/api/v1/user/avatar") {
        val userId = call.authenticate(jwtService) ?: return@put
        
        val request = call.receive<Map<String, String?>>()
        val avatarPath = request["avatar"]
        
        val userRepo = UserRepository()
        userRepo.updateAvatar(userId, avatarPath)
        
        call.respond(successResponse())
    }
    
    // 更新昵称
    put("/api/v1/user/nickname") {
        val userId = call.authenticate(jwtService) ?: return@put
        
        val request = call.receive<Map<String, String>>()
        val nickname = request["nickname"]
        
        if (nickname.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorResponse("昵称不能为空"))
            return@put
        }
        
        val userRepo = UserRepository()
        userRepo.updateNickname(userId, nickname)
        
        call.respond(successResponse())
    }
    
    // 获取用户信息
    get("/api/v1/user/info") {
        val currentUserId = call.authenticate(jwtService) ?: return@get
        
        val userRepo = UserRepository()
        val user = userRepo.findById(currentUserId)
        
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, errorResponse("用户不存在"))
            return@get
        }
        
        val friendIds = friendRepository.getFriends(currentUserId).map { it.userId }
        val unreadMessages = messageRepository.getUnreadCount(currentUserId)
        val pendingRequests = friendRepository.getReceivedRequests(currentUserId).count { it.request.status == "pending" }
        
        call.respond(successResponse(mapOf(
            "userId" to user.id,
            "email" to user.email,
            "nickname" to user.nickname,
            "avatar" to user.avatar,
            "points" to user.points,
            "contributionCount" to user.contributionCount,
            "friendCount" to friendIds.size,
            "unreadMessages" to unreadMessages,
            "pendingFriendRequests" to pendingRequests
        )))
    }
}
