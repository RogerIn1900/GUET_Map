package com.example.guet_map.module.social.data.model

import com.example.guet_map.module.social.data.model.WeatherType

/**
 * 每日天气预报数据
 */
data class DailyForecast(
    val date: String,           // 日期，格式 yyyy-MM-dd
    val temperatureHigh: Int,   // 最高温度 °C
    val temperatureLow: Int,   // 最低温度 °C
    val weatherType: WeatherType,
    val description: String,
    val precipitation: Int,     // 降水概率 0-100
    val windSpeed: Float,       // 风速 m/s
    val windDirection: String,
    val humidity: Int,          // 湿度 %
    val uvIndex: Int            // 紫外线指数
)

/**
 * 天气预报（当前 + 小时 + 日报）
 */
data class WeatherForecast(
    val current: Weather,
    val dailyForecast: List<DailyForecast>,
    val locationName: String,
    val latitude: Double,
    val longitude: Double
)
