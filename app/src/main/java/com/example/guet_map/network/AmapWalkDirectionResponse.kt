package com.example.guet_map.network

import com.google.gson.annotations.SerializedName

/** 高德 Web 服务：步行路径规划 https://lbs.amap.com/api/webservice/guide/api/direction */
data class AmapWalkDirectionResponse(
    val status: String,
    val info: String? = null,
    val route: AmapWalkRoute? = null
)

data class AmapWalkRoute(
    val paths: List<AmapWalkPath>? = null
)

data class AmapWalkPath(
    val distance: String? = null,
    val duration: String? = null,
    val steps: List<AmapWalkStep>? = null
)

data class AmapWalkStep(
    val polyline: String? = null,
    @SerializedName("step_distance")
    val stepDistance: String? = null
)
