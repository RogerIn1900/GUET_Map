package com.example.guet_map.module.social.data.remote

import com.example.guet_map.module.social.data.model.WeatherType

/**
 * WMO Weather interpretation codes (Open-Meteo 使用)
 *
 * 完整表格：https://open-meteo.com/en/docs#weathervariables
 *
 * 0          晴
 * 1, 2, 3    多云（少量→阴天）
 * 45, 48     雾
 * 51, 53, 55 毛毛雨（轻/中/浓）
 * 56, 57     冻毛毛雨
 * 61, 63, 65 雨（轻/中/大）
 * 66, 67     冻雨
 * 71, 73, 75 雪（轻/中/大）
 * 77         米雪
 * 80, 81, 82 阵雨（轻/中/大）
 * 85, 86     阵雪
 * 95         雷暴（轻/中）
 * 96, 99     雷暴伴冰雹
 */
object WmoWeatherCodeMapper {

    fun toWeatherType(code: Int?): WeatherType = when (code) {
        null -> WeatherType.UNKNOWN
        0 -> WeatherType.SUNNY
        1, 2 -> WeatherType.CLOUDY
        3 -> WeatherType.OVERCAST
        45, 48 -> WeatherType.FOG
        in 51..57 -> WeatherType.LIGHT_RAIN
        61, 66 -> WeatherType.LIGHT_RAIN
        63 -> WeatherType.MODERATE_RAIN
        65, 67 -> WeatherType.HEAVY_RAIN
        in 71..77, 85, 86 -> WeatherType.SNOW
        80 -> WeatherType.LIGHT_RAIN
        81 -> WeatherType.MODERATE_RAIN
        82 -> WeatherType.HEAVY_RAIN
        95 -> WeatherType.THUNDERSTORM
        96, 99 -> WeatherType.THUNDERSTORM
        else -> WeatherType.UNKNOWN
    }

    fun toDescription(code: Int?): String = when (code) {
        null -> "未知"
        0 -> "晴"
        1 -> "少云"
        2 -> "多云"
        3 -> "阴"
        45 -> "雾"
        48 -> "雾凇"
        51 -> "毛毛雨"
        53 -> "毛毛雨"
        55 -> "浓毛毛雨"
        56, 57 -> "冻毛毛雨"
        61 -> "小雨"
        63 -> "中雨"
        65 -> "大雨"
        66, 67 -> "冻雨"
        71 -> "小雪"
        73 -> "中雪"
        75 -> "大雪"
        77 -> "米雪"
        80 -> "阵雨"
        81 -> "阵雨"
        82 -> "强阵雨"
        85, 86 -> "阵雪"
        95 -> "雷暴"
        96, 99 -> "雷暴伴冰雹"
        else -> "未知"
    }
}
