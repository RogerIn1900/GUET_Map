package com.example.guet_map.module.social.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo 地理编码响应
 * 文档：https://open-meteo.com/en/docs/geocoding-api
 *
 * 示例：
 * {
 *   "results": [
 *     { "id": 1809858, "name": "桂林市", "latitude": 25.28, "longitude": 110.29,
 *       "country_code": "CN", "admin1": "广西壮族自治区", "admin2": "桂林市" }
 *   ]
 * }
 */
data class OpenMeteoGeocodingResponse(
    @SerializedName("results") val results: List<GeocodingResult>? = null,
    @SerializedName("generationtime_ms") val generationtimeMs: Double? = null
)

data class GeocodingResult(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("admin1") val admin1: String? = null,
    @SerializedName("admin2") val admin2: String? = null,
    @SerializedName("admin3") val admin3: String? = null,
    @SerializedName("admin4") val admin4: String? = null,
    @SerializedName("population") val population: Int? = null,
    @SerializedName("feature_code") val featureCode: String? = null
) {
    /**
     * 拼接完整显示名，例如 "桂林市 / 广西 / 中国"
     */
    fun displayName(): String {
        val parts = listOfNotNull(name, admin1, country).filter { it.isNotBlank() }
        return parts.joinToString(" / ")
    }
}
