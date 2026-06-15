package com.example.guet_map.module.social.data.remote

import com.example.guet_map.module.social.data.remote.dto.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 天气 API 服务
 * 文档: https://open-meteo.com/en/docs
 */
interface OpenMeteoApiService {

    /**
     * 获取当前天气和预报
     * @param latitude 纬度
     * @param longitude 经度
     * @param current 需要获取的当前天气变量
     * @param hourly 需要获取的小时预报变量
     * @param daily 需要获取的每日预报变量
     * @param timezone 时区
     * @param forecastDays 预报天数 (1-16)
     */
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_VARIABLES,
        @Query("hourly") hourly: String = HOURLY_VARIABLES,
        @Query("daily") daily: String = DAILY_VARIABLES,
        @Query("timezone") timezone: String = "Asia/Shanghai",
        @Query("forecast_days") forecastDays: Int = 3
    ): OpenMeteoResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        // 当前天气变量
        const val CURRENT_VARIABLES =
            "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code," +
            "wind_speed_10m,wind_direction_10m,is_day"

        // 小时预报变量
        const val HOURLY_VARIABLES =
            "temperature_2m,relative_humidity_2m,precipitation_probability,weather_code"

        // 每日预报变量
        const val DAILY_VARIABLES =
            "temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max"
    }
}
