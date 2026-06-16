package com.example.guet_map.module.social.data.remote.dto

import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.HourlyWeather
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherType
import com.example.guet_map.module.social.data.remote.WmoWeatherCodeMapper
import java.util.Calendar
import java.util.UUID

/**
 * Open-Meteo 响应 → 领域模型。
 *
 * 一次请求即可同时拿到 current / hourly / daily 三个数据块。
 * 当前时间是 24h 数组里时间最接近"现在"的那一条。
 */
fun OpenMeteoForecastResponse.toWeather(
    locationName: String = ""
): Weather? {
    val current = current ?: return null
    val temp = current.temperature?.toInt() ?: 0
    val feelsLike = current.apparentTemperature?.toInt() ?: temp
    val humidity = current.humidity ?: 0
    val windSpeed = current.windSpeed?.toFloat() ?: 0f
    val windDirection = degreeToCompass(current.windDirection)
    val weatherType = WmoWeatherCodeMapper.toWeatherType(current.weatherCode)
    val description = WmoWeatherCodeMapper.toDescription(current.weatherCode)

    val hourlyList = extractHourly(hourly, current.time)
    val sunrise = parseIsoToTodayMillis(daily?.sunrise?.firstOrNull(), sunriseMillis = true)
    val sunset = parseIsoToTodayMillis(daily?.sunset?.firstOrNull(), sunriseMillis = false)

    return Weather(
        id = UUID.randomUUID().toString(),
        temperature = temp,
        feelsLike = feelsLike,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherType = weatherType,
        description = description,
        aqi = null,
        aqiLevel = null,
        uvIndex = hourlyList.firstOrNull()?.let { null }, // 当前小时 UV 已在 hourly 列表
        sunrise = sunrise,
        sunset = sunset,
        hourlyForecast = hourlyList,
        alertMessage = buildAlertMessage(weatherType, temp, windSpeed)
    )
}

/**
 * Open-Meteo 响应 → 每日预报列表（最多 16 天）。
 */
fun OpenMeteoForecastResponse.toDailyForecast(
    fallbackHumidity: Int = 0
): List<DailyForecast> {
    val daily = daily ?: return emptyList()
    val time = daily.time ?: return emptyList()
    val size = time.size
    return (0 until size).mapNotNull { i ->
        val date = time.getOrNull(i) ?: return@mapNotNull null
        val code = daily.weatherCode?.getOrNull(i)
        val max = daily.temperatureMax?.getOrNull(i)?.toInt() ?: return@mapNotNull null
        val min = daily.temperatureMin?.getOrNull(i)?.toInt() ?: max
        val precip = daily.precipitationProbabilityMax?.getOrNull(i) ?: 0
        val uv = daily.uvIndexMax?.getOrNull(i)?.toInt() ?: 0
        val windMax = daily.windSpeedMax?.getOrNull(i)?.toFloat() ?: 0f

        DailyForecast(
            date = date,
            temperatureHigh = max,
            temperatureLow = min,
            weatherType = WmoWeatherCodeMapper.toWeatherType(code),
            description = WmoWeatherCodeMapper.toDescription(code),
            precipitation = precip,
            windSpeed = windMax,
            windDirection = "—",
            humidity = fallbackHumidity,
            uvIndex = uv
        )
    }
}

/**
 * 提取从"今天 0:00"开始的小时数据。
 *
 * 关键：
 * - 起点 = 今日 0:00（而非当前小时）
 * - 当日（dayIndex=0）= 今日 0:00 ~ 23:00（24 行）
 * - 0:00 之后（即 next day 0:00 ~ 23:00）属于 dayIndex=1（明天）
 * - 跨日规则：hour 由非 0 → 0 切换时，dayIndex += 1
 */
private fun extractHourly(
    hourly: OpenMeteoForecastResponse.Hourly?,
    currentTime: String?
): List<HourlyWeather> {
    if (hourly == null) return emptyList()
    val time = hourly.time ?: return emptyList()
    if (time.isEmpty()) return emptyList()

    // 起点 = 今日 0:00（不是当前小时）
    val startIdx = findTodayZeroHourIndex(time)
    if (startIdx < 0) return emptyList()

    return (startIdx until time.size).mapNotNull { i ->
        val t = time.getOrNull(i) ?: return@mapNotNull null
        val temp = hourly.temperature?.getOrNull(i)?.toInt() ?: return@mapNotNull null
        val code = hourly.weatherCode?.getOrNull(i)
        val precip = hourly.precipitationProbability?.getOrNull(i) ?: 0
        val hour = parseHour(t) ?: return@mapNotNull null
        // 跨日：hour 从非 0 变到 0 时，dayIndex += 1
        val isDayRollover = i > startIdx && hour == 0
        val baseDay = (i - startIdx) / 24
        val dayIndex = if (isDayRollover) baseDay + 1 else baseDay
        HourlyWeather(
            hour = hour,
            dayIndex = dayIndex,
            temperature = temp,
            weatherType = WmoWeatherCodeMapper.toWeatherType(code),
            precipitation = precip
        )
    }
}

/**
 * 找到数组中"今日 0:00"对应的索引。
 * 时间数组里任何一条 hour=0 且日期 = 今日（或最新过去）的项。
 */
private fun findTodayZeroHourIndex(times: List<String>): Int {
    if (times.isEmpty()) return -1
    val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val todayYear = Calendar.getInstance().get(Calendar.YEAR)
    // 优先找今天日期 + 0:00
    val todayZero = times.indexOfFirst { t ->
        val d = parseDayOfYear(t) ?: return@indexOfFirst false
        val h = parseHour(t) ?: return@indexOfFirst false
        d.first == todayYear && d.second == today && h == 0
    }
    if (todayZero >= 0) return todayZero
    // 兜底：找数组里第一个 hour=0
    return times.indexOfFirst { parseHour(it) == 0 }.takeIf { it >= 0 } ?: 0
}

private fun parseDayOfYear(iso: String?): Pair<Int, Int>? {
    if (iso.isNullOrBlank()) return null
    val datePart = iso.substringBefore('T', missingDelimiterValue = "")
    val parts = datePart.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
    val d = parts.getOrNull(2)?.toIntOrNull() ?: return null
    val cal = Calendar.getInstance().apply { set(y, m - 1, d) }
    return y to cal.get(Calendar.DAY_OF_YEAR)
}

/**
 * 找到数组中与"现在"匹配的索引。
 * 优先按 ISO 时间字符串匹配；匹配不到则取当天 0 点之后的最近一条。
 */
private fun findCurrentHourIndex(
    times: List<String>,
    currentTime: String?
): Int {
    if (times.isEmpty()) return -1
    val cur = parseHour(currentTime) ?: return 0
    return times.indexOfFirst { parseHour(it) == cur }
        .takeIf { it >= 0 } ?: 0
}

private fun parseHour(iso: String?): Int? {
    if (iso.isNullOrBlank()) return null
    // 格式 "2026-06-15T18:00" 或 "2026-06-15T18:00:00"
    val timePart = iso.substringAfter('T', missingDelimiterValue = "").substringBefore('+')
    val hh = timePart.substringBefore(':').toIntOrNull() ?: return null
    return hh.coerceIn(0, 23)
}

/**
 * 把 ISO 字符串解析为当天的 0 点 + 偏移毫秒。
 * Open-Meteo 的 sunrise/sunset 形如 "2026-06-15T05:45"。
 * 我们只关心时分，所以直接用 1970-01-01 作 base 再加时差，但项目已有 sunrise: Long 字段（毫秒），
 * 这里把日期统一视为"今天"，避免时区差异。
 */
private fun parseIsoToTodayMillis(iso: String?, sunriseMillis: Boolean): Long {
    if (iso.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        val timePart = iso.substringAfter('T', missingDelimiterValue = "")
            .substringBefore('+')
        val parts = timePart.split(":")
        val hh = parts.getOrNull(0)?.toIntOrNull() ?: return System.currentTimeMillis()
        val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hh)
            set(Calendar.MINUTE, mm)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

/**
 * 风向角度 → 中文方位。
 */
private fun degreeToCompass(deg: Int?): String {
    if (deg == null) return "—"
    return when ((deg + 22) / 45 % 8) {
        0 -> "北风"
        1 -> "东北风"
        2 -> "东风"
        3 -> "东南风"
        4 -> "南风"
        5 -> "西南风"
        6 -> "西风"
        7 -> "西北风"
        else -> "—"
    }
}

/**
 * 极端天气安全提示。
 */
private fun buildAlertMessage(type: WeatherType, temp: Int, windSpeed: Float): String? {
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
    if (windSpeed >= 10) alerts.add("风力较强（${windSpeed.toInt()} m/s），请减少户外活动")
    return alerts.takeIf { it.isNotEmpty() }?.joinToString("；")
}
