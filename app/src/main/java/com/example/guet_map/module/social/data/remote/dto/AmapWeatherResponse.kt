package com.example.guet_map.module.social.data.remote.dto

import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.HourlyWeather
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherType
import java.util.UUID

/**
 * 高德天气 API 实时天气响应（extensions=base）
 * 示例：{"status":"1","count":"1","info":"OK","infocode":"10000",
 *        "lives":[{"province":"广西","city":"桂林市","adcode":"450300",
 *        "weather":"多云","temperature":"31","winddirection":"东北",
 *        "windpower":"4","humidity":"47","reporttime":"2026-06-15 18:02:31",
 *        "temperature_float":"31.0","humidity_float":"47.0"}]}
 */
data class AmapWeatherLiveResponse(
    val status: String?,
    val count: String?,
    val info: String?,
    val infocode: String?,
    val lives: List<Live>?
) {
    data class Live(
        val province: String?,
        val city: String?,
        val adcode: String?,
        val weather: String?,
        val temperature: String?,
        val winddirection: String?,
        val windpower: String?,
        val humidity: String?,
        val reporttime: String?,
        val temperature_float: String?,
        val humidity_float: String?
    )
}

/**
 * 高德天气 API 预报响应（extensions=all）
 * 包含 4 天 forecast。
 */
data class AmapWeatherForecastResponse(
    val status: String?,
    val count: String?,
    val info: String?,
    val infocode: String?,
    val forecasts: List<Forecast>?
) {
    data class Forecast(
        val city: String?,
        val adcode: String?,
        val province: String?,
        val reporttime: String?,
        val casts: List<Cast>?
    )

    data class Cast(
        val date: String?,
        val week: String?,
        val dayweather: String?,
        val nightweather: String?,
        val daytemp: String?,
        val nighttemp: String?,
        val daywind: String?,
        val nightwind: String?,
        val daypower: String?,
        val nightpower: String?,
        val daytemp_float: String?,
        val nighttemp_float: String?
    )
}

/**
 * 将高德实时天气响应转换为领域模型 Weather。
 * 当 status != "1" 或 lives 为空时返回 null，由 Repository 决定回退缓存或抛错。
 */
fun AmapWeatherLiveResponse.toDomain(
    reportTimeMillis: Long = System.currentTimeMillis()
): Weather? {
    if (status != "1") return null
    val live = lives?.firstOrNull() ?: return null

    val description = live.weather?.trim().orEmpty()
    val weatherType = mapAmapWeatherText(description)
    val temp = live.temperature?.toIntOrNull() ?: 0
    val humidity = live.humidity?.toIntOrNull() ?: 0
    val windPowerLevel = live.windpower?.toIntOrNull() ?: 0
    val windSpeed = amapWindPowerToMs(windPowerLevel)
    val windDirection = live.winddirection?.trim().orEmpty().ifEmpty { "无持续风向" }

    return Weather(
        id = UUID.randomUUID().toString(),
        temperature = temp,
        // 高德 live 字段无体感温度，使用与气温相同的近似值
        feelsLike = temp,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherType = weatherType,
        description = description,
        aqi = null,
        aqiLevel = null,
        // 高德 live 不含紫外线，使用 0
        uvIndex = 0,
        // 日出日落由 Repository 注入真实值（高德基础接口不返回）
        sunrise = reportTimeMillis,
        sunset = reportTimeMillis,
        hourlyForecast = emptyList(),
        alertMessage = buildAmapAlertMessage(weatherType, temp, windPowerLevel)
    )
}

/**
 * 将高德预报响应转为 DailyForecast 列表（首天 = 今天）。
 */
fun AmapWeatherForecastResponse.toDailyForecast(
    fallbackHumidity: Int = 0,
    fallbackUv: Int = 0
): List<DailyForecast> {
    if (status != "1") return emptyList()
    val casts = forecasts?.firstOrNull()?.casts ?: return emptyList()
    return casts.mapNotNull { cast ->
        val date = cast.date?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val dayHigh = cast.daytemp?.toIntOrNull() ?: return@mapNotNull null
        val nightLow = cast.nighttemp?.toIntOrNull() ?: dayHigh
        val dayText = cast.dayweather?.trim().orEmpty()
        val dayType = mapAmapWeatherText(dayText)
        // daypower 是风级数字（如 "4"），仅取白天
        val power = cast.daypower?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0
        val nightPower = cast.nightpower?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0
        val maxPower = maxOf(power, nightPower)
        val dayWind = cast.daywind?.trim().orEmpty()

        DailyForecast(
            date = date,
            temperatureHigh = dayHigh,
            temperatureLow = nightLow,
            weatherType = dayType,
            description = dayText,
            precipitation = 0,
            windSpeed = amapWindPowerToMs(maxPower),
            windDirection = dayWind,
            humidity = fallbackHumidity,
            uvIndex = fallbackUv
        )
    }
}

/**
 * 高德中文天气文字 → WeatherType。
 * 涵盖：晴、多云、阴、各种雨、雪、雾、霾、雷、沙尘 等。
 */
fun mapAmapWeatherText(text: String): WeatherType {
    val t = text.trim()
    return when {
        t.isEmpty() -> WeatherType.UNKNOWN
        t.contains("雷") -> WeatherType.THUNDERSTORM
        t.contains("暴雨") || t.contains("大暴雨") -> WeatherType.HEAVY_RAIN
        t.contains("大雨") || t.contains("中雨") -> WeatherType.MODERATE_RAIN
        t.contains("小雨") || t.contains("雨") -> WeatherType.LIGHT_RAIN
        t.contains("雪") -> WeatherType.SNOW
        t.contains("霾") || t.contains("沙") || t.contains("扬沙") -> WeatherType.FOG
        t.contains("雾") -> WeatherType.FOG
        t.contains("阴") -> WeatherType.OVERCAST
        t.contains("多云") -> WeatherType.CLOUDY
        t.contains("晴") -> WeatherType.SUNNY
        t.contains("大风") || t.contains("飓风") || t.contains("狂风") -> WeatherType.WINDY
        else -> WeatherType.UNKNOWN
    }
}

/**
 * 高德风级（蒲福级，1-12+）→ m/s 近似值。
 *   1级 0.3-1.5；  2级 1.6-3.3； 3级 3.4-5.4；
 *   4级 5.5-7.9；  5级 8.0-10.7；6级 10.8-13.8；
 *   7级 13.9-17.1；8级 17.2-20.7
 * 取每级上限的近似值，便于 UI 展示。
 */
fun amapWindPowerToMs(level: Int): Float = when (level) {
    0 -> 0f
    1 -> 0.5f
    2 -> 2.5f
    3 -> 4.5f
    4 -> 7f
    5 -> 9.5f
    6 -> 12.5f
    7 -> 15.5f
    8 -> 19f
    9 -> 22f
    10 -> 25f
    11 -> 29f
    else -> 33f
}

/**
 * 极端天气安全提示。
 */
fun buildAmapAlertMessage(type: WeatherType, temp: Int, windPowerLevel: Int): String? {
    val alerts = mutableListOf<String>()
    when (type) {
        WeatherType.THUNDERSTORM -> alerts.add("雷暴天气，请避免户外活动，注意防雷击")
        WeatherType.HEAVY_RAIN -> alerts.add("暴雨来袭，请注意防范洪涝灾害")
        WeatherType.MODERATE_RAIN, WeatherType.LIGHT_RAIN -> alerts.add("有降水天气，路面湿滑，请小心慢行")
        WeatherType.FOG -> alerts.add("有雾/霾天气，能见度低，请注意出行健康")
        WeatherType.WINDY -> alerts.add("大风天气，请注意高空坠物风险")
        else -> {}
    }
    if (temp < 5) alerts.add("气温较低（${temp}°C），请注意保暖与防滑")
    if (temp > 35) alerts.add("高温预警（${temp}°C），请注意防暑降温")
    if (windPowerLevel >= 6) alerts.add("风力较强（${windPowerLevel} 级），请减少户外活动")
    return alerts.takeIf { it.isNotEmpty() }?.joinToString("；")
}
