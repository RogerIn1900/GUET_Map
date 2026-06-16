package com.example.guet_map.module.social.data.remote

import com.example.guet_map.module.social.data.repository.WeatherRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 把经纬度映射到高德天气 API 支持的 city 字段。
 *
 * 优先级：
 * 1. 若经纬度落在 [Guilin] 校园区附近（距离 5 km 以内），使用 campus adcode。
 * 2. 落在 [nearbyCities] 内，使用对应 adcode。
 * 3. 兜底：使用 repo 默认 city（桂林市 450300）。
 *
 * 注意：高德天气 Web API 仅支持按 adcode 名称/区号查询，精确经纬度反查需要
 * 额外调用 [逆地理] 接口；为了保持单次请求最低成本，这里采用城市半径映射。
 */
@Singleton
class CityAdcodeResolver @Inject constructor() {

    private val campusCenterLat = WeatherRepository.DEFAULT_LATITUDE
    private val campusCenterLng = WeatherRepository.DEFAULT_LONGITUDE

    private val nearbyCities: List<CityArea> = listOf(
        CityArea(name = "桂林市", adcode = "450300", lat = 25.274, lng = 110.290, radiusKm = 35.0),
        CityArea(name = "阳朔县", adcode = "450321", lat = 24.778, lng = 110.496, radiusKm = 30.0),
        CityArea(name = "南宁市", adcode = "450100", lat = 22.817, lng = 108.366, radiusKm = 60.0),
        CityArea(name = "柳州市", adcode = "450200", lat = 24.326, lng = 109.428, radiusKm = 60.0),
        CityArea(name = "北京市", adcode = "110000", lat = 39.904, lng = 116.407, radiusKm = 80.0),
        CityArea(name = "上海市", adcode = "310000", lat = 31.230, lng = 121.474, radiusKm = 80.0),
        CityArea(name = "广州市", adcode = "440100", lat = 23.129, lng = 113.264, radiusKm = 80.0),
        CityArea(name = "深圳市", adcode = "440300", lat = 22.543, lng = 114.058, radiusKm = 80.0),
        CityArea(name = "杭州市", adcode = "330100", lat = 30.274, lng = 120.155, radiusKm = 80.0),
        CityArea(name = "成都市", adcode = "510100", lat = 30.572, lng = 104.067, radiusKm = 80.0),
    )

    fun resolve(lat: Double, lng: Double): String {
        val nearestCampus = haversineKm(lat, lng, campusCenterLat, campusCenterLng)
        if (nearestCampus <= 5.0) {
            // 校园内或 5 km 内：直接走默认 adcode
            return WeatherRepository.DEFAULT_CITY_ADCODE
        }
        val match = nearbyCities
            .map { it to haversineKm(lat, lng, it.lat, it.lng) }
            .minByOrNull { it.second }
        return if (match != null && match.second <= match.first.radiusKm) {
            match.first.adcode
        } else {
            WeatherRepository.DEFAULT_CITY_ADCODE
        }
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private data class CityArea(
        val name: String,
        val adcode: String,
        val lat: Double,
        val lng: Double,
        val radiusKm: Double
    )
}
