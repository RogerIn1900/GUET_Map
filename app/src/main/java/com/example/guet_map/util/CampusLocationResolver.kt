package com.example.guet_map.util

import com.example.guet_map.model.Location

/** 搜索/导航均使用高德 POI 原始坐标，不做覆盖或校正。 */
object CampusLocationResolver {

    fun preferAmapCoordinates(location: Location, pool: List<Location>): Location = location

    fun resolveForQuery(query: String, pool: List<Location>): Location? =
        CampusSearchMatcher.filterAndSort(pool, query, limit = 1).firstOrNull()
}
