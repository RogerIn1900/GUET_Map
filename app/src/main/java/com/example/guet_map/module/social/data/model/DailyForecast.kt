package com.example.guet_map.module.social.data.model

import com.example.guet_map.module.social.data.model.WeatherType

/**
 * 每日天气预报数据
 */
data class DailyForecast(
    val date: String,                    // 日期，格式 yyyy-MM-dd
    val temperatureHigh: Int,            // 最高温度 °C
    val temperatureLow: Int,             // 最低温度 °C
    val weatherType: WeatherType,
    val description: String,
    val precipitation: Int,              // 降水概率 0-100
    val precipitationAmount: Float = 0f, // 降水量 mm
    val windSpeed: Float,                // 风速 m/s
    val windDirection: String,
    val windLevel: String = "",          // 风力等级（1-2级）
    val humidity: Int,                   // 湿度 %
    val uvIndex: Int,                    // 紫外线指数
    val uvLevel: String = "",            // 紫外线等级（弱/中/强）
    val sunrise: Long = 0,               // 日出时间戳
    val sunset: Long = 0,                // 日落时间戳
    val feelsLikeHigh: Int = temperatureHigh, // 体感高温
    val feelsLikeLow: Int = temperatureLow,    // 体感低温
    val dayIcon: WeatherType = weatherType,    // 白天图标
    val nightIcon: WeatherType = weatherType,   // 夜间图标
    val moonPhase: String = ""               // 月相（新月/上弦月/满月…）
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
