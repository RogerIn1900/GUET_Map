package com.example.guet_map.util

import com.example.guet_map.model.Location

/** 在高德 POI 池中按名称/地址模糊搜索，不替换坐标。 */
object CampusSearchMatcher {

    fun filterAndSort(locations: List<Location>, rawQuery: String, limit: Int = 20): List<Location> {
        val query = rawQuery.trim().replace(Regex("\\s+"), "")
        if (query.isEmpty()) return locations.take(limit)

        return locations
            .map { it to score(it, query) }
            .filter { (_, s) -> s > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    fun resolveBest(locations: List<Location>, rawQuery: String): Location? =
        filterAndSort(locations, rawQuery, limit = 1).firstOrNull()

    fun bestMatch(locations: List<Location>, rawQuery: String): Location? =
        resolveBest(locations, rawQuery)

    private fun score(location: Location, query: String): Int {
        val name = location.name
        var s = 0
        if (name.equals(query, ignoreCase = true)) s += 120
        if (name.contains(query, ignoreCase = true)) s += 60
        CampusBuildingCatalog.findEntryByAlias(query)?.let { entry ->
            if (entry.matchesName(name)) s += 80
        }
        CampusDormitoryCatalog.findZoneByQuery(query)?.let { zone ->
            if (name.contains(zone.yuanName)) s += 90
            if (name.contains("${zone.zoneLetter}区", ignoreCase = true) &&
                (name.contains("苑") || location.category == "宿舍")
            ) {
                s += 70
            }
        }
        return s
    }
}
