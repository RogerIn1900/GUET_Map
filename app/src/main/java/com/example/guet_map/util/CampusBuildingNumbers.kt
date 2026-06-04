package com.example.guet_map.util

/** 解析教学楼楼号（1–17 为花江校区目录内编号） */
object CampusBuildingNumbers {

    const val MIN_CATALOG = 1
    const val MAX_CATALOG = 17

    fun parse(text: String): Int? {
        if (text.contains("四十三") || Regex("(?<![0-9])43(?![0-9])").containsMatchIn(text)) {
            if (text.contains("教") || text.contains("教学")) return 43
        }
        Regex("(\\d{1,2})\\s*教").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        Regex("第(\\d{1,2})教学").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }

        val cnMap = listOf(
            "十七" to 17, "十六" to 16, "十五" to 15, "十四" to 14, "十三" to 13,
            "十二" to 12, "十一" to 11, "十" to 10,
            "九" to 9, "八" to 8, "七" to 7, "六" to 6, "五" to 5,
            "四" to 4, "三" to 3, "二" to 2, "一" to 1
        )
        for ((cn, n) in cnMap) {
            if (text.contains("第${cn}教学") || text.contains("${cn}教")) return n
        }
        return null
    }

    fun isCatalogBuilding(n: Int): Boolean = n in MIN_CATALOG..MAX_CATALOG

    fun shouldDropAmapTeachingPoi(name: String): Boolean {
        val n = parse(name) ?: return false
        return !isCatalogBuilding(n)
    }
}
