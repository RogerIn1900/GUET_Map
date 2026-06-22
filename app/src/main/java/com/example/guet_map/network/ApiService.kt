package com.example.guet_map.network

import com.example.guet_map.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // ========== Auth APIs ==========

    @POST("api/v1/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): ApiResponse<Unit>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<LoginResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<Unit>

    // ========== Location APIs ==========

    @GET("api/v1/locations")
    suspend fun getLocations(): List<Location>

    @GET("api/v1/locations")
    suspend fun getLocationsByCategory(
        @Query("category") category: String
    ): List<Location>

    @GET("api/v1/locations/{locationId}")
    suspend fun getLocationDetail(
        @Path("locationId") locationId: String
    ): Location

    @GET("api/v1/locations/{locationId}/guides")
    suspend fun getGuideSteps(
        @Path("locationId") locationId: String
    ): List<GuideStep>

    @GET("api/v1/locations/{locationId}/guides")
    suspend fun getGuideStepsPaged(
        @Path("locationId") locationId: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): List<GuideStep>

    @Multipart
    @POST("api/v1/guides/upload")
    suspend fun uploadGuideStep(
        @Part("locationId") locationId: RequestBody,
        @Part("stepNumber") stepNumber: RequestBody,
        @Part("description") description: RequestBody,
        @Part image: MultipartBody.Part
    ): UploadResponse

    @GET("api/v1/categories")
    suspend fun getCategories(): List<Category>

    @GET("api/v1/favorites")
    suspend fun getFavorites(): List<Location>

    @POST("api/v1/favorites")
    suspend fun addFavorite(@Body body: FavoriteRequest): Location

    @DELETE("api/v1/favorites/{locationId}")
    suspend fun removeFavorite(@Path("locationId") locationId: String)

    @GET("api/v1/guides/recent")
    suspend fun getRecentGuides(): List<RecentGuide>

    @GET("api/v1/guides/mine")
    suspend fun getMyGuideSubmissions(
        @Query("status") status: String? = null
    ): List<MyGuideSubmission>

    @GET("api/v1/notifications")
    suspend fun getNotifications(): List<AppNotification>

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long)

    @PUT("api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead()

    // ========== 好友系统 API ==========

    @GET("api/v1/friends")
    suspend fun getFriends(): ApiResponse<List<FriendInfo>>

    @GET("api/v1/users/search")
    suspend fun searchUser(
        @Query("email") email: String? = null,
        @Query("userId") userId: Long? = null
    ): ApiResponse<UserSearchResult>

    @POST("api/v1/friend-requests")
    suspend fun sendFriendRequest(@Body body: SendFriendRequestBody): ApiResponse<FriendRequest>

    @GET("api/v1/friend-requests/received")
    suspend fun getReceivedFriendRequests(): ApiResponse<List<FriendRequestWithUser>>

    @PUT("api/v1/friend-requests/{id}")
    suspend fun handleFriendRequest(
        @Path("id") requestId: Long,
        @Body body: HandleFriendRequestBody
    ): ApiResponse<Unit>

    @DELETE("api/v1/friends/{id}")
    suspend fun removeFriend(@Path("id") friendId: Long): ApiResponse<Unit>

    // ========== 聊天消息 API ==========

    @GET("api/v1/messages/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int = 50
    ): ApiResponse<List<Message>>

    @POST("api/v1/messages")
    suspend fun sendMessage(@Body body: SendMessageBody): ApiResponse<Message>

    @GET("api/v1/messages/unread-count")
    suspend fun getUnreadMessageCount(): ApiResponse<UnreadCountResponse>

    @GET("api/v1/messages/unread-count/{userId}")
    suspend fun getUnreadMessageCountFrom(
        @Path("userId") userId: Long
    ): ApiResponse<UnreadCountResponse>

    // ========== 朋友圈帖子 API ==========

    @GET("api/v1/posts")
    suspend fun getPosts(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ApiResponse<List<PostWithDetails>>

    @GET("api/v1/posts/location/{locationId}")
    suspend fun getPostsByLocation(
        @Path("locationId") locationId: String
    ): ApiResponse<List<PostWithDetails>>

    @POST("api/v1/posts")
    suspend fun createPost(@Body body: CreatePostRequest): ApiResponse<Post>

    @DELETE("api/v1/posts/{id}")
    suspend fun deletePost(@Path("id") postId: Long): ApiResponse<Unit>

    @POST("api/v1/posts/{id}/like")
    suspend fun togglePostLike(@Path("id") postId: Long): ApiResponse<LikeResponse>

    @GET("api/v1/posts/{id}/comments")
    suspend fun getPostComments(@Path("id") postId: Long): ApiResponse<List<CommentWithUser>>

    @POST("api/v1/posts/{id}/comments")
    suspend fun addPostComment(
        @Path("id") postId: Long,
        @Body body: CreateCommentRequest
    ): ApiResponse<Comment>

    // ========== 用户位置 API ==========

    @PUT("api/v1/location")
    suspend fun updateMyLocation(@Body body: UpdateLocationRequest): ApiResponse<Unit>

    @GET("api/v1/locations/friends")
    suspend fun getFriendLocations(): ApiResponse<List<FriendLocation>>

    @GET("api/v1/locations/{userId}")
    suspend fun getFriendLocation(@Path("userId") userId: Long): ApiResponse<FriendLocation>

    // ========== 用户资料 API ==========

    @PUT("api/v1/user/avatar")
    suspend fun updateAvatar(@Body body: Map<String, String?>): ApiResponse<Unit>

    @PUT("api/v1/user/nickname")
    suspend fun updateNickname(@Body body: Map<String, String>): ApiResponse<Unit>

    @GET("api/v1/user/info")
    suspend fun getUserInfo(): ApiResponse<UserProfile>
}
