package com.example.guet_map.module.social.data.remote

import com.example.guet_map.module.social.data.remote.dto.OpenMeteoGeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 地理编码服务（与 [OpenMeteoApiService] baseUrl 不同，单独成接口）
 *
 * 文档：https://open-meteo.com/en/docs/geocoding-api
 */
interface OpenMeteoGeocodingService {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}
