package com.example.guet_map.network

import com.example.guet_map.util.CampusBuildingCatalog
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 开发阶段 Mock 拦截器。Release 构建通过 [BuildConfig.USE_MOCK_API] 禁用。
 */
class MockInterceptor : Interceptor {

    private val jsonMediaType = "application/json".toMediaType()
    private val gson = Gson()
    /** 地图 POI 仅来自高德 SDK，Mock 不再注入目录占位坐标 */
    private val locationsJson: String by lazy {
        gson.toJson(emptyList<Any>())
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val category = request.url.queryParameter("category")

        val body = when {
            path == "/api/v1/auth/login" && method == "POST" -> LOGIN_RESPONSE_JSON
            path == "/api/v1/categories" -> CATEGORIES_JSON
            path == "/api/v1/favorites" && method == "GET" -> FAVORITES_JSON
            path.matches(Regex("/api/v1/favorites/[^/]+")) && method == "DELETE" ->
                """{"success":true}"""
            path == "/api/v1/favorites" && method == "POST" ->
                LOCATION_DETAIL_JSON["library"]!!
            path == "/api/v1/guides/recent" -> RECENT_GUIDES_JSON
            path == "/api/v1/guides/mine" -> MY_GUIDES_JSON
            path == "/api/v1/notifications" -> NOTIFICATIONS_JSON
            path == "/api/v1/guides/upload" && method == "POST" -> UPLOAD_RESPONSE_JSON
            path.matches(Regex("/api/v1/locations/[^/]+/guides")) -> {
                val parts = path.split("/")
                val locationId = parts.getOrNull(4) ?: "default"
                GUIDE_STEPS_JSON[locationId] ?: GUIDE_STEPS_JSON["default"]!!
            }
            path.matches(Regex("/api/v1/locations/[^/]+")) && !path.endsWith("/guides") -> {
                val locationId = path.substringAfterLast("/")
                LOCATION_DETAIL_JSON[locationId]
                    ?: CampusBuildingCatalog.toMockLocations()
                        .find { it.locationId == locationId }
                        ?.let { gson.toJson(it) }
                    ?: """{"locationId":"$locationId","name":"未知地点","latitude":25.3070,"longitude":110.4185,"category":"教室","rating":0,"openingHours":"","imageUrl":"","hasGuide":false}"""
            }
            path == "/api/v1/locations" -> {
                if (category.isNullOrBlank()) locationsJson
                else filterLocationsByCategory(category)
            }
            else -> null
        }

        return if (body != null) {
            mockResponse(request, body)
        } else {
            chain.proceed(request)
        }
    }

    private fun filterLocationsByCategory(category: String): String {
        return gson.toJson(emptyList<Any>())
    }

    private fun extractLocationId(raw: String): String? {
        val match = Regex("locationId=([\\w_]+)").find(raw)
        return match?.groupValues?.getOrNull(1)
    }

    private fun mockResponse(request: okhttp3.Request, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Mock)")
            .body(body.toResponseBody(jsonMediaType))
            .build()
    }
}

private val CATEGORIES_JSON = """
[
  {"id":"classroom","name":"教室"},
  {"id":"canteen","name":"食堂"},
  {"id":"cafe","name":"咖啡"},
  {"id":"library","name":"图书馆"},
  {"id":"dorm","name":"宿舍"},
  {"id":"gate","name":"校门"},
  {"id":"store","name":"商店"},
  {"id":"sports","name":"运动场"}
]
""".trimIndent()

private val FAVORITES_JSON = """
[
  {"locationId":"library","name":"校图书馆","latitude":25.2870,"longitude":110.4110,"category":"图书馆","rating":4.7,"openingHours":"08:00-22:00","imageUrl":"https://example.com/img/library.jpg","hasGuide":true}
]
""".trimIndent()

private val RECENT_GUIDES_JSON = """
[
  {"locationId":"building_11b","locationName":"第十一教学楼B区","stepCount":4,"contributor":"同学A","approvedAt":"2026-06-01"},
  {"locationId":"gate_south","locationName":"南门","stepCount":2,"contributor":"同学B","approvedAt":"2026-05-28"}
]
""".trimIndent()

private val MY_GUIDES_JSON = """
[
  {"id":101,"locationId":"canteen_1","locationName":"第一学生食堂","status":"pending","stepNumber":1,"description":"从主路右转进入","rejectReason":null,"submittedAt":"2026-06-03"},
  {"id":102,"locationId":"building_11a","locationName":"第十一教学楼A区","status":"approved","stepNumber":2,"description":"电梯至2楼","rejectReason":null,"submittedAt":"2026-05-20"}
]
""".trimIndent()

private val NOTIFICATIONS_JSON = """
[
  {"id":1,"type":"review","title":"指引审核通过","body":"您提交的「第十一教学楼A区」步骤已通过，+5积分","locationId":"building_11a","isRead":false,"createdAt":"2026-06-02T10:00:00"},
  {"id":2,"type":"points","title":"积分到账","body":"您的校园贡献积分已更新为 15","locationId":null,"isRead":false,"createdAt":"2026-06-01T18:30:00"},
  {"id":3,"type":"announcement","title":"欢迎使用 GUET Map","body":"花江校区实景导航已上线，欢迎贡献指路！","locationId":null,"isRead":true,"createdAt":"2026-05-30T09:00:00"}
]
""".trimIndent()

private val LOGIN_RESPONSE_JSON = """
{"token":"mock_jwt_token_guet","nickname":"桂电学子","points":15,"contributionCount":3}
""".trimIndent()

private val LOCATION_DETAIL_JSON = mapOf(
    "building_11b" to """{"locationId":"building_11b","name":"第十一教学楼B区","latitude":25.30750,"longitude":110.41780,"category":"教室","rating":4.5,"openingHours":"07:00-22:30","imageUrl":"https://example.com/img/11b.jpg","hasGuide":true}""",
    "library" to """{"locationId":"library","name":"校图书馆","latitude":25.2870,"longitude":110.4110,"category":"图书馆","rating":4.7,"openingHours":"08:00-22:00","imageUrl":"https://example.com/img/library.jpg","hasGuide":true}"""
)

private val GUIDE_STEPS_JSON = mapOf(
    "building_11b" to """
[
  {"id":1,"locationId":"building_11b","stepNumber":1,"description":"从南门进入校园，沿主干道直行约200米","imageUrl":"https://example.com/guide/11b_01.jpg"},
  {"id":2,"locationId":"building_11b","stepNumber":2,"description":"看到「创新大楼」指示牌后左转","imageUrl":"https://example.com/guide/11b_02.jpg"},
  {"id":3,"locationId":"building_11b","stepNumber":3,"description":"走到创维半岛大厦西塔大门","imageUrl":"https://example.com/guide/11b_03.jpg"},
  {"id":4,"locationId":"building_11b","stepNumber":4,"description":"乘坐电梯至3楼，出门后右侧即为B区入口","imageUrl":"https://example.com/guide/11b_04.jpg"}
]
""".trimIndent(),
    "default" to """
[
  {"id":1,"locationId":"unknown","stepNumber":1,"description":"暂无详细指引，请联系管理员添加","imageUrl":""}
]
""".trimIndent()
)

private val UPLOAD_RESPONSE_JSON = """
{"success":true,"message":"上传成功，待审核通过后将发放积分","pointsAwarded":5}
""".trimIndent()
