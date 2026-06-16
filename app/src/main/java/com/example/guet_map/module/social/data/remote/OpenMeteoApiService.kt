package com.example.guet_map.module.social.data.remote

import com.example.guet_map.module.social.data.remote.dto.OpenMeteoForecastResponse
import com.example.guet_map.module.social.data.remote.dto.OpenMeteoGeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 天气 API
 *
 * 文档：https://open-meteo.com/en/docs
 *
 * - 天气预测：`/v1/forecast`，按经纬度直接查询
 * - 地理编码：`/v1/search`，输入地名→经纬度 + 行政区
 *
 * 免费层 10000 次/天，5000 次/小时，600 次/分钟，无需注册 key。
 */
interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = DEFAULT_CURRENT,
        @Query("hourly") hourly: String = DEFAULT_HOURLY,
        @Query("daily") daily: String = DEFAULT_DAILY,
        @Query("forecast_days") forecastDays: Int = 16,
        @Query("timezone") timezone: String = "auto",
        @Query("language") language: String = "zh"
    ): OpenMeteoForecastResponse

    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"

        /** 当前天气字段：温度、体感、湿度、降水、天气码、风、气压 */
        const val DEFAULT_CURRENT =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
                "precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m"

        /** 小时字段：温度、降水概率/量、天气码、湿度、UV、风 */
        const val DEFAULT_HOURLY =
            "temperature_2m,precipitation,precipitation_probability,weather_code," +
                "relative_humidity_2m,wind_speed_10m,uv_index"

        /** 日字段：天气码、高温、低温、降水、UV、风、日出日落 */
        const val DEFAULT_DAILY =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
                "precipitation_sum,precipitation_probability_max,uv_index_max,wind_speed_10m_max"
    }
}
