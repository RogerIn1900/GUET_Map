package com.example.guet_map.module.social.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo /v1/forecast 响应
 *
 * current: 当前时刻的瞬时天气
 * hourly : 逐小时预报（数组并列，与 time[i] 对应）
 * daily  : 逐日预报（数组并列，与 time[i] 对应）
 *
 * 示例（节选）：
 * {
 *   "latitude": 25.27, "longitude": 110.29,
 *   "current": {
 *     "time": "2026-06-15T18:00",
 *     "temperature_2m": 31.4,
 *     "relative_humidity_2m": 47,
 *     "weather_code": 3
 *   },
 *   "hourly": {
 *     "time": ["2026-06-15T00:00", "2026-06-15T01:00", ...],
 *     "temperature_2m": [26.1, 25.8, ...],
 *     "precipitation_probability": [10, 15, 0, ...]
 *   },
 *   "daily": {
 *     "time": ["2026-06-15", "2026-06-16", ...],
 *     "temperature_2m_max": [33.0, 32.4, ...],
 *     "temperature_2m_min": [25.0, 24.6, ...]
 *   }
 * }
 */
data class OpenMeteoForecastResponse(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("current") val current: Current? = null,
    @SerializedName("hourly") val hourly: Hourly? = null,
    @SerializedName("daily") val daily: Daily? = null
) {

    data class Current(
        @SerializedName("time") val time: String? = null,
        @SerializedName("temperature_2m") val temperature: Double? = null,
        @SerializedName("relative_humidity_2m") val humidity: Int? = null,
        @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,
        @SerializedName("is_day") val isDay: Int? = null,
        @SerializedName("precipitation") val precipitation: Double? = null,
        @SerializedName("weather_code") val weatherCode: Int? = null,
        @SerializedName("surface_pressure") val surfacePressure: Double? = null,
        @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
        @SerializedName("wind_direction_10m") val windDirection: Int? = null
    )

    data class Hourly(
        @SerializedName("time") val time: List<String>? = null,
        @SerializedName("temperature_2m") val temperature: List<Double>? = null,
        @SerializedName("precipitation") val precipitation: List<Double>? = null,
        @SerializedName("precipitation_probability") val precipitationProbability: List<Int>? = null,
        @SerializedName("weather_code") val weatherCode: List<Int>? = null,
        @SerializedName("relative_humidity_2m") val humidity: List<Int>? = null,
        @SerializedName("wind_speed_10m") val windSpeed: List<Double>? = null,
        @SerializedName("uv_index") val uvIndex: List<Double>? = null
    )

    data class Daily(
        @SerializedName("time") val time: List<String>? = null,
        @SerializedName("weather_code") val weatherCode: List<Int>? = null,
        @SerializedName("temperature_2m_max") val temperatureMax: List<Double>? = null,
        @SerializedName("temperature_2m_min") val temperatureMin: List<Double>? = null,
        @SerializedName("sunrise") val sunrise: List<String>? = null,
        @SerializedName("sunset") val sunset: List<String>? = null,
        @SerializedName("precipitation_sum") val precipitationSum: List<Double>? = null,
        @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
        @SerializedName("uv_index_max") val uvIndexMax: List<Double>? = null,
        @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double>? = null
    )
}
