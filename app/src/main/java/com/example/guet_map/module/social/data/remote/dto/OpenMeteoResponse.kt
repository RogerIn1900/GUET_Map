package com.example.guet_map.module.social.data.remote.dto

import com.example.guet_map.module.social.data.model.HourlyWeather
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherType
import java.util.UUID

/**
 * Open-Meteo API 响应 DTO
 * 文档: https://open-meteo.com/en/docs
 */
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current: CurrentWeather?,
    val hourly: HourlyWeatherData?,
    val daily: DailyWeatherData?
) {
    data class CurrentWeather(
        val time: String,
        val temperature_2m: Double,
        val relative_humidity_2m: Int,
        val apparent_temperature: Double,
        val weather_code: Int,
        val wind_speed_10m: Double,
        val wind_direction_10m: Int,
        val is_day: Int
    )

    data class HourlyWeatherData(
        val time: List<String>,
        val temperature_2m: List<Double>,
        val relative_humidity_2m: List<Int>,
        val precipitation_probability: List<Int>,
        val weather_code: List<Int>
    )

    data class DailyWeatherData(
        val time: List<String>,
        val temperature_2m_max: List<Double>,
        val temperature_2m_min: List<Double>,
        val sunrise: List<String>,
        val sunset: List<String>,
        val uv_index_max: List<Double>
    )
}

/**
 * 将 Open-Meteo 响应转换为领域模型
 */
fun OpenMeteoResponse.toDomain(): Weather {
    val currentWeather = this.current
    val hourlyData = this.hourly
    val dailyData = this.daily

    // 解析小时预报 (取当前小时开始的24小时)
    val hourlyForecast = mutableListOf<HourlyWeather>()
    hourlyData?.let { hourly ->
        val startIndex = 0
        val endIndex = minOf(24, hourly.time.size)
        for (i in startIndex until endIndex) {
            val hourStr = hourly.time.getOrNull(i) ?: continue
            val hour = hourStr.substringAfter("T").substringBefore(":").toIntOrNull() ?: i
            hourlyForecast.add(
                HourlyWeather(
                    hour = hour,
                    temperature = hourly.temperature_2m.getOrNull(i)?.toInt() ?: 0,
                    weatherType = mapWeatherCode(hourly.weather_code.getOrNull(i) ?: 0),
                    precipitation = hourly.precipitation_probability.getOrNull(i) ?: 0
                )
            )
        }
    }

    // 解析日出日落时间 (取今天的数据)
    val sunrise = dailyData?.sunrise?.firstOrNull()?.let { parseIsoTime(it) } ?: System.currentTimeMillis()
    val sunset = dailyData?.sunset?.firstOrNull()?.let { parseIsoTime(it) } ?: System.currentTimeMillis()
    val uvIndex = dailyData?.uv_index_max?.firstOrNull()?.toInt() ?: 0

        val weatherCode = currentWeather?.weather_code ?: 0
        val temp = currentWeather?.temperature_2m ?: 0.0
        val precipProb = hourlyData?.precipitation_probability?.firstOrNull() ?: 0

        return Weather(
            id = UUID.randomUUID().toString(),
            temperature = currentWeather?.temperature_2m?.toInt() ?: 0,
            feelsLike = currentWeather?.apparent_temperature?.toInt() ?: 0,
            humidity = currentWeather?.relative_humidity_2m ?: 0,
            windSpeed = currentWeather?.wind_speed_10m?.toFloat() ?: 0f,
            windDirection = getWindDirection(currentWeather?.wind_direction_10m ?: 0),
            weatherType = mapWeatherCode(weatherCode),
            description = getWeatherDescription(weatherCode),
            aqi = null,  // Open-Meteo 基础版不含 AQI
            aqiLevel = null,
            uvIndex = uvIndex,
            sunrise = sunrise,
            sunset = sunset,
            hourlyForecast = hourlyForecast,
            alertMessage = buildAlertMessage(weatherCode, temp, precipProb)
        )
}

/**
 * 将 WMO 天气码映射为天气类型
 * WMO Weather interpretation codes: https://open-meteo.com/en/docs
 */
fun mapWeatherCode(code: Int): WeatherType {
    return when (code) {
        0 -> WeatherType.SUNNY           // Clear sky
        1 -> WeatherType.SUNNY           // Mainly clear
        2 -> WeatherType.CLOUDY          // Partly cloudy
        3 -> WeatherType.OVERCAST        // Overcast
        45, 48 -> WeatherType.FOG       // Fog
        51, 53, 55 -> WeatherType.LIGHT_RAIN    // Drizzle
        56, 57 -> WeatherType.LIGHT_RAIN         // Freezing drizzle
        61, 63 -> WeatherType.LIGHT_RAIN         // Rain slight, moderate
        65 -> WeatherType.MODERATE_RAIN          // Rain heavy
        66, 67 -> WeatherType.LIGHT_RAIN         // Freezing rain
        71, 73 -> WeatherType.SNOW               // Snow slight, moderate
        75 -> WeatherType.SNOW                   // Snow heavy
        77 -> WeatherType.SNOW                   // Snow grains
        80, 81 -> WeatherType.LIGHT_RAIN         // Rain showers
        82 -> WeatherType.MODERATE_RAIN          // Rain showers violent
        85, 86 -> WeatherType.SNOW               // Snow showers
        95 -> WeatherType.THUNDERSTORM           // Thunderstorm
        96, 99 -> WeatherType.THUNDERSTORM       // Thunderstorm with hail
        else -> WeatherType.UNKNOWN
    }
}

/**
 * 获取天气描述
 */
fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "晴"
        1 -> "晴间多云"
        2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63, 66, 67 -> "小雨"
        65 -> "中雨"
        71, 73, 77 -> "小雪"
        75 -> "大雪"
        80, 81 -> "阵雨"
        82 -> "暴雨"
        85, 86 -> "阵雪"
        95 -> "雷阵雨"
        96, 99 -> "雷暴冰雹"
        else -> "未知天气"
    }
}

/**
 * 将风向角度转换为文字描述
 */
fun getWindDirection(degrees: Int): String {
    val directions = listOf("北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风")
    val index = ((degrees + 22.5) / 45).toInt() % 8
    return directions[index]
}

/**
 * 解析 ISO 格式时间字符串为时间戳
 */
private fun parseIsoTime(isoString: String): Long {
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
        format.parse(isoString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

/**
 * 根据天气码、温度、降水概率生成安全提示
 * 覆盖 WX-03 极端天气预警功能
 */
private fun buildAlertMessage(weatherCode: Int, temp: Double, precipProb: Int): String? {
    val alerts = mutableListOf<String>()

    // 雷暴 / 暴雨
    if (weatherCode in listOf(95, 96, 99)) {
        alerts.add("雷暴天气，请避免户外活动，注意防雷击")
    } else if (weatherCode == 82) {
        alerts.add("暴雨来袭，请注意防范洪涝灾害")
    }

    // 大雨 / 中雨
    if (weatherCode in listOf(61, 63, 65, 80, 81)) {
        alerts.add("有降水天气，路面湿滑，请小心慢行")
    }

    // 低温预警 (低于 5°C)
    if (temp < 5) {
        alerts.add("气温较低（${temp.toInt()}°C），请注意保暖与防滑")
    }

    // 高温预警 (高于 35°C)
    if (temp > 35) {
        alerts.add("高温预警（${temp.toInt()}°C），请注意防暑降温")
    }

    // 大风 (风速 > 10 m/s，对应 6 级以上风)
    // 注意：当前数据中没有直接的风速，这里用天气码辅助判断
    if (weatherCode == 95 || weatherCode == 96 || weatherCode == 99) {
        alerts.add("强对流天气，请注意防范大风")
    }

    // 雾天
    if (weatherCode in listOf(45, 48)) {
        alerts.add("有雾天气，能见度低，请注意出行安全")
    }

    return alerts.takeIf { it.isNotEmpty() }?.joinToString("；")
}
