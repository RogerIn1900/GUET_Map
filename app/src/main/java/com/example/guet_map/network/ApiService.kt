package com.example.guet_map.network

import com.example.guet_map.model.AppNotification
import com.example.guet_map.model.Category
import com.example.guet_map.model.FavoriteRequest
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.LoginRequest
import com.example.guet_map.model.LoginResponse
import com.example.guet_map.model.MyGuideSubmission
import com.example.guet_map.model.RecentGuide
import com.example.guet_map.model.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

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

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}
