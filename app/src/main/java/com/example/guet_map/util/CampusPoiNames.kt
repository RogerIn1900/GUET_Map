package com.example.guet_map.util

import com.example.guet_map.model.Location

/** 透传高德 POI 列表（兼容旧调用点）。 */
object CampusPoiNames {

    fun isTrustedAmapName(name: String): Boolean =
        name.contains("桂林电子科技大学") || name.contains("桂电")

    fun fromAmapSdk(locations: List<Location>): List<Location> = locations
}
