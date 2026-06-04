package com.example.guet_map.util

import android.content.Context
import android.content.pm.PackageManager
import com.amap.api.maps.model.LatLng
import com.example.guet_map.network.AmapPlaceResponse
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 按名称解析单个校内 POI 坐标；严格匹配楼号，避免「二教」误匹配「十五教操场」。
 */
@Singleton
class AmapPoiGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val amapHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val campusCenter = LatLng(CampusGeo.CENTER_LAT, CampusGeo.CENTER_LNG)

    suspend fun geocodeCampusPoi(displayName: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            geocodeCampusPoiInternal(displayName)
        } catch (e: Exception) {
            // #region agent log
            AgentDebugLog.log(
                "E1",
                "AmapPoiGeocoder.geocodeCampusPoi",
                "geocode exception",
                mapOf("name" to displayName, "error" to (e.javaClass.simpleName + ": " + (e.message ?: ""))),
                runId = "poi-sdk"
            )
            // #endregion
            null
        }
    }

    private fun geocodeCampusPoiInternal(displayName: String): LatLng? {
        val keywords = buildSearchKeywords(displayName)
        val key = readAmapKey()
        if (key.isBlank()) return null

        val encoded = URLEncoder.encode(keywords, Charsets.UTF_8.name())
        val url = buildString {
            append("https://restapi.amap.com/v3/place/text?")
            append("keywords=$encoded")
            append("&city=桂林&citylimit=true")
            append("&offset=10&page=1")
            append("&key=$key")
        }

        val response = amapHttpClient.newCall(Request.Builder().url(url).get().build()).execute()
        val body = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val parsed = gson.fromJson(body, AmapPlaceResponse::class.java)
        if (parsed.status != "1") return null

        val pois = parsed.pois.orEmpty().filter { isNearCampus(it) }
        if (pois.isEmpty()) return null

        val best = pois.maxByOrNull { scorePoi(it, displayName) } ?: return null
        if (scorePoi(best, displayName) < MIN_MATCH_SCORE) {
            // #region agent log
            AgentDebugLog.log(
                "P2",
                "AmapPoiGeocoder.geocodeCampusPoi",
                "no confident match",
                mapOf("query" to displayName, "best" to best.name, "score" to scorePoi(best, displayName)),
                runId = "poi-sdk"
            )
            // #endregion
            return null
        }
        return parseLocation(best.location)
    }

    private fun buildSearchKeywords(name: String): String = when {
        name.contains("桂林电子科技大学") -> name
        name == "南门" || name == "北门" || name == "西门" || name == "东门" ->
            "桂林电子科技大学花江校区$name"
        name == "二教" || name.contains("第二教学楼") ->
            "桂林电子科技大学花江校区第二教学楼"
        name.contains("第十二") || name.contains("12教") ->
            "桂林电子科技大学花江校区第十二教学楼"
        else -> "桂林电子科技大学花江校区$name"
    }

    private fun scorePoi(poi: com.example.guet_map.network.AmapPlacePoi, displayName: String): Int {
        val poiName = poi.name.orEmpty()
        var score = 0
        if (poiName.contains("花江") || poiName.contains("桂电")) score += 2
        if (poiName.contains(displayName.replace("花江校区", "").trim())) score += 5

        val queryNums = extractBuildingNumbers(displayName)
        val poiNums = extractBuildingNumbers(poiName)
        if (queryNums.isNotEmpty()) {
            if (queryNums.any { it in poiNums }) score += 10
            else if (poiNums.isNotEmpty()) score -= 8
        }

        if (displayName.contains("二教") || displayName.contains("第二")) {
            if (poiName.contains("第二") && poiName.contains("教学")) score += 8
            if (poiName.contains("十五") || poiName.contains("15")) score -= 12
        }
        if (displayName.contains("南门") && poiName.contains("南门")) score += 8
        if (displayName.contains("十二") && poiName.contains("第十二")) score += 8
        return score
    }

    private fun extractBuildingNumbers(text: String): Set<Int> {
        val nums = mutableSetOf<Int>()
        val cnMap = mapOf(
            "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5,
            "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10,
            "十一" to 11, "十二" to 12, "十三" to 13, "十四" to 14, "十五" to 15
        )
        cnMap.forEach { (cn, n) ->
            if (text.contains("${cn}教") || text.contains("第${cn}") || text.contains("第${cn}教学")) {
                nums.add(n)
            }
        }
        Regex("(\\d+)").findAll(text).forEach { m ->
            m.groupValues.getOrNull(1)?.toIntOrNull()?.let { nums.add(it) }
        }
        return nums
    }

    private fun isNearCampus(poi: com.example.guet_map.network.AmapPlacePoi): Boolean {
        val loc = parseLocation(poi.location) ?: return false
        return abs(loc.latitude - campusCenter.latitude) < 0.015 &&
            abs(loc.longitude - campusCenter.longitude) < 0.015
    }

    private fun parseLocation(raw: String?): LatLng? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(",")
        if (parts.size != 2) return null
        val lng = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        return LatLng(lat, lng)
    }

    private fun readAmapKey(): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        return appInfo.metaData?.getString("com.amap.api.v2.apikey").orEmpty()
    }

    suspend fun geocodeAllSequentially(names: List<String>): Map<String, LatLng> {
        val result = linkedMapOf<String, LatLng>()
        for (name in names) {
            geocodeCampusPoi(name)?.let { result[name] = it }
            delay(220)
        }
        return result
    }

    companion object {
        private const val MIN_MATCH_SCORE = 6
    }
}
