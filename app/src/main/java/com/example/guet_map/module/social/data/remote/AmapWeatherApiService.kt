package com.example.guet_map.module.social.data.remote

import com.example.guet_map.module.social.data.remote.dto.AmapWeatherForecastResponse
import com.example.guet_map.module.social.data.remote.dto.AmapWeatherLiveResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 高德地图天气 Web API
 *
 * 文档：https://lbs.amap.com/api/webservice/guide/api/weatherinfo
 *
 * - extensions=base ：实时天气（lives[]）
 * - extensions=all  ：未来 4 天预报（forecasts[].casts[]）
 *
 * 国内服务器直连，无国际链路，校园/国内运营商下访问稳定。
 */
interface AmapWeatherApiService {

    @GET("v3/weather/weatherInfo")
    suspend fun getLiveWeather(
        @Query("city") city: String,
        @Query("key") key: String = DEFAULT_KEY,
        @Query("extensions") extensions: String = "base",
        @Query("output") output: String = "JSON"
    ): AmapWeatherLiveResponse

    @GET("v3/weather/weatherInfo")
    suspend fun getWeatherForecast(
        @Query("city") city: String,
        @Query("key") key: String = DEFAULT_KEY,
        @Query("extensions") extensions: String = "all",
        @Query("output") output: String = "JSON"
    ): AmapWeatherForecastResponse

    companion object {
        const val BASE_URL = "https://restapi.amap.com/"

        /**
         * 项目统一使用的高德 Web API Key，与 AndroidManifest 中
         * com.amap.api.v2.apikey meta-data 保持一致。
         * 注意：高德 Web API（restapi.amap.com）和 Android SDK Key 是同一个 Key。
         */
        const val DEFAULT_KEY = "c2f63f64da793bcba817e0e3767603dc"
    }
}
