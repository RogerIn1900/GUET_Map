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
}
