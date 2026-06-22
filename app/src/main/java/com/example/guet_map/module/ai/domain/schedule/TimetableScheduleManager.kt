package com.example.guet_map.module.ai.domain.schedule

import com.example.guet_map.model.Location
import com.example.guet_map.module.ai.data.model.TimetableEntry
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * 当日智慧导航时机判断与导航建议生成器
 *
 * 纯业务逻辑层，不涉及 UI 和 ViewModel。
 *
 * 时机判断规则：
 * - BeforeClass30Min：课程开始前 30 分钟内
 * - DuringClass：正处于上课时间
 * - AfterClass：下课后 30 分钟内
 * - Idle：以上皆非
 */
@Singleton
class TimetableScheduleManager @Inject constructor() {

    companion object {
        private const val BEFORE_CLASS_THRESHOLD_MIN = 30
        private const val AFTER_CLASS_THRESHOLD_MIN = 30
        private const val WALKING_SPEED_MPS = 1.3
        private const val DEFAULT_WALKING_MINUTES = 8
        private const val PRE_DEPARTURE_BUFFER_MIN = 10

        private val PERIOD_START_MINUTES = mapOf(
            1 to 480, 2 to 525, 3 to 585, 4 to 645, 5 to 690,
            6 to 840, 7 to 885, 8 to 945, 9 to 1005, 10 to 1050,
            11 to 1170, 12 to 1215, 13 to 1260, 14 to 1305
        )

        private val PERIOD_END_MINUTES = mapOf(
            1 to 525, 2 to 585, 3 to 645, 4 to 690, 5 to 735,
            6 to 885, 7 to 945, 8 to 1005, 9 to 1050, 10 to 1095,
            11 to 1215, 12 to 1260, 13 to 1305, 14 to 1350
        )
    }

    /**
     * 导航时机枚举
     */
    enum class NavigationTiming {
        /** 课程开始前 30 分钟内 */
        BeforeClass30Min,
        /** 正处于上课时间 */
        DuringClass,
        /** 下课后 30 分钟内 */
        AfterClass,
        /** 非关键节点 */
        Idle
    }

    /**
     * 导航建议数据类
     */
    data class NavigationSuggestion(
        val entry: TimetableEntry,
        val timing: NavigationTiming,
        val departureTime: String,
        val arriveTime: String,
        val walkingMinutes: Int,
        val warningMessage: String
    )

    /**
     * 获取今日是星期几（1=周一 ... 7=周日）
     */
    fun getCurrentDayOfWeek(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        }
    }

    /**
     * 获取当前处于第几节课（1-14），若不在任何一节则返回 null
     */
    fun getCurrentPeriod(): Int? {
        val now = Calendar.getInstance()
        val totalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return PERIOD_START_MINUTES.entries.firstOrNull { (key, start) ->
            totalMinutes >= start && totalMinutes < (PERIOD_END_MINUTES[key] ?: return@firstOrNull false)
        }?.key
    }

    /**
     * 根据当前时间和课表，判断导航时机
     */
    fun getNavigationTiming(entries: List<TimetableEntry>): NavigationTiming {
        val now = Calendar.getInstance()
        val totalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val dayOfWeek = getCurrentDayOfWeek()

        val todayEntries = entries.filter { it.dayOfWeek == dayOfWeek }
        if (todayEntries.isEmpty()) return NavigationTiming.Idle

        for (entry in todayEntries) {
            val startMin = PERIOD_START_MINUTES[entry.startPeriod] ?: continue
            val endMin = PERIOD_END_MINUTES[entry.endPeriod] ?: continue

            when {
                totalMinutes in (startMin - BEFORE_CLASS_THRESHOLD_MIN)..<startMin -> {
                    return NavigationTiming.BeforeClass30Min
                }
                totalMinutes in startMin..endMin -> {
                    return NavigationTiming.DuringClass
                }
                totalMinutes in (endMin + 1)..<(endMin + AFTER_CLASS_THRESHOLD_MIN) -> {
                    return NavigationTiming.AfterClass
                }
            }
        }
        return NavigationTiming.Idle
    }

    /**
     * 构建导航建议，若无课表数据或非关键时机则返回 null
     */
    fun buildSuggestion(
        entries: List<TimetableEntry>,
        currentLat: Double,
        currentLng: Double
    ): NavigationSuggestion? {
        if (entries.isEmpty()) return null

        val timing = getNavigationTiming(entries)
        if (timing == NavigationTiming.Idle) return null

        val suggestion = getUpcomingSuggestion(entries, currentLat, currentLng) ?: return null

        val warningMessage = when (timing) {
            NavigationTiming.BeforeClass30Min -> "课程即将开始，请尽快出发"
            NavigationTiming.DuringClass -> "课程进行中，是否需要导航？"
            NavigationTiming.AfterClass -> "下课后离场，是否需要导航？"
            NavigationTiming.Idle -> "暂无当前课程"
        }

        return suggestion.copy(timing = timing, warningMessage = warningMessage)
    }

    /**
     * 获取最近一条可导航建议（优先当前课程，其次下一节）
     */
    fun getUpcomingSuggestion(
        entries: List<TimetableEntry>,
        currentLat: Double,
        currentLng: Double
    ): NavigationSuggestion? {
        val dayOfWeek = getCurrentDayOfWeek()
        val todayEntries = entries.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startPeriod }
        if (todayEntries.isEmpty()) return null

        val now = Calendar.getInstance()
        val totalMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val currentPeriod = getCurrentPeriod()

        val selected = if (currentPeriod != null) {
            todayEntries.firstOrNull { entry ->
                totalMinutes >= PERIOD_START_MINUTES[entry.startPeriod]!! &&
                        totalMinutes < PERIOD_END_MINUTES[entry.endPeriod]!!
            } ?: todayEntries.firstOrNull { it.startPeriod >= currentPeriod }
        } else {
            todayEntries.firstOrNull()
        } ?: return null

        val location = selected.location
        val targetPeriod = selected.startPeriod
        val courseStartMinutes = PERIOD_START_MINUTES[targetPeriod] ?: return null
        val walkingMinutes = if (location != null) {
            estimateWalkingMinutes(currentLat, currentLng, location.latitude, location.longitude)
        } else {
            DEFAULT_WALKING_MINUTES
        }

        val departureMinutes = courseStartMinutes - walkingMinutes - PRE_DEPARTURE_BUFFER_MIN
        val arriveMinutes = courseStartMinutes - PRE_DEPARTURE_BUFFER_MIN

        val timing = when {
            currentPeriod != null && currentPeriod > targetPeriod -> NavigationTiming.DuringClass
            currentPeriod != null && currentPeriod == targetPeriod -> NavigationTiming.DuringClass
            else -> NavigationTiming.BeforeClass30Min
        }

        val warningMessage = when {
            currentPeriod == null -> "请按时到达教室"
            currentPeriod > targetPeriod -> "课程已开始，请尽快前往"
            currentPeriod == targetPeriod -> "课程正在进行中"
            else -> "请按时上课"
        }

        return NavigationSuggestion(
            entry = selected,
            timing = timing,
            departureTime = formatTime(departureMinutes),
            arriveTime = formatTime(arriveMinutes),
            walkingMinutes = walkingMinutes,
            warningMessage = warningMessage
        )
    }

    /**
     * 估算步行时间（分钟），基于简化的平面距离
     */
    private fun estimateWalkingMinutes(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Int {
        val dx = (toLat - fromLat) * 111000.0
        val dy = (toLng - fromLng) * 85000.0
        val distance = sqrt(dx * dx + dy * dy)
        return (distance / WALKING_SPEED_MPS / 60).toInt().coerceAtLeast(4)
    }

    private fun formatTime(totalMinutes: Int): String {
        val normalized = totalMinutes.mod(24 * 60)
        val hour = normalized / 60
        val minute = normalized % 60
        return "%02d:%02d".format(hour, minute)
    }
}
