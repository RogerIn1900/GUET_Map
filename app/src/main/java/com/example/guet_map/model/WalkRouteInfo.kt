package com.example.guet_map.model

import com.amap.api.maps.model.LatLng

/** 高德步行路径规划结果（GCJ-02） */
data class WalkRouteInfo(
    val targetName: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polyline: List<LatLng>
)
