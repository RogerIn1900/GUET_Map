package com.example.guet_map.repository

import com.example.guet_map.model.*
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val apiService: ApiService
) {
    // ========== 好友系统 ==========

    fun getFriends(): Flow<Resource<List<FriendInfo>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getFriends()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取好友列表失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun searchUser(email: String? = null, userId: Long? = null): Flow<Resource<UserSearchResult>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.searchUser(email, userId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "用户不存在"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun sendFriendRequest(userId: Long, message: String? = null): Flow<Resource<FriendRequest>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.sendFriendRequest(SendFriendRequestBody(userId, message))
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "发送好友申请失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getReceivedFriendRequests(): Flow<Resource<List<FriendRequestWithUser>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getReceivedFriendRequests()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取好友申请失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun handleFriendRequest(requestId: Long, accept: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.handleFriendRequest(requestId, HandleFriendRequestBody(accept))
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "处理好友申请失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun removeFriend(friendId: Long): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.removeFriend(friendId)
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "删除好友失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    // ========== 聊天消息 ==========

    fun getMessages(userId: Long, limit: Int = 50): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getMessages(userId, limit)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取消息失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun sendMessage(receiverId: Long, content: String, type: String = "text"): Flow<Resource<Message>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.sendMessage(SendMessageBody(receiverId, content, type))
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "发送消息失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getUnreadMessageCount(): Flow<Resource<Int>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getUnreadMessageCount()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.count))
            } else {
                emit(Resource.Error(response.message ?: "获取未读消息数失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    // ========== 朋友圈帖子 ==========

    fun getPosts(limit: Int = 50, offset: Int = 0): Flow<Resource<List<PostWithDetails>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPosts(limit, offset)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取帖子失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getPostsByLocation(locationId: String): Flow<Resource<List<PostWithDetails>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPostsByLocation(locationId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取帖子失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun createPost(
        content: String,
        locationId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        images: String? = null,
        visibility: String = "public"
    ): Flow<Resource<Post>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createPost(
                CreatePostRequest(content, locationId, latitude, longitude, images, visibility)
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "发布帖子失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun deletePost(postId: Long): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deletePost(postId)
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "删除帖子失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun togglePostLike(postId: Long): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.togglePostLike(postId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.liked))
            } else {
                emit(Resource.Error(response.message ?: "操作失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getPostComments(postId: Long): Flow<Resource<List<CommentWithUser>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPostComments(postId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取评论失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun addPostComment(postId: Long, content: String): Flow<Resource<Comment>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.addPostComment(postId, CreateCommentRequest(content))
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "发表评论失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    // ========== 用户位置 ==========

    fun updateMyLocation(latitude: Double, longitude: Double): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateMyLocation(UpdateLocationRequest(latitude, longitude))
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "更新位置失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getFriendLocations(): Flow<Resource<List<FriendLocation>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getFriendLocations()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取好友位置失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getFriendLocation(userId: Long): Flow<Resource<FriendLocation>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getFriendLocation(userId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取位置失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    // ========== 用户资料 ==========

    fun updateAvatar(avatarPath: String?): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateAvatar(mapOf("avatar" to avatarPath))
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "更新头像失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun updateNickname(nickname: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateNickname(mapOf("nickname" to nickname))
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "更新昵称失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }

    fun getUserInfo(): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getUserInfo()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "获取用户信息失败"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "网络错误"))
        }
    }
}
