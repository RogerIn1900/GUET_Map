package com.example.guet_map.ui.map

import android.content.Context
import android.location.Location
import android.speech.tts.TextToSpeech
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.example.guet_map.R
import com.example.guet_map.model.WalkRouteInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 导航循迹器
 * 职责：
 * 1. 接收 GPS 位置更新
 * 2. 计算与规划路线的偏差，偏航时发出事件
 * 3. 实时计算剩余距离和预计到达时间
 * 4. 跟踪当前导航段落，触发 TTS 语音播报
 * 5. 在地图上绘制用户位置和导航进度
 */
class NavigationTracker(
    private val context: Context
) {
    private var currentRoute: WalkRouteInfo? = null
    private var currentPolyline: List<LatLng> = emptyList()

    private var userMarker: Marker? = null
    private var progressPolyline: Polyline? = null
    private var headingArrowMarker: Marker? = null
    private var lastSpokenStepIndex = -1

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // GPS 位置流
    private val _gpsPosition = MutableStateFlow<LatLng?>(null)
    val gpsPosition: StateFlow<LatLng?> = _gpsPosition.asStateFlow()

    // 剩余距离（米）
    private val _remainingDistance = MutableStateFlow(0)
    val remainingDistance: StateFlow<Int> = _remainingDistance.asStateFlow()

    // 剩余时间（秒）
    private val _remainingTime = MutableStateFlow(0)
    val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

    // 当前步骤（导航段索引）
    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    // 偏航事件
    private val _deviationEvent = MutableSharedFlow<DeviationEvent>()
    val deviationEvent: SharedFlow<DeviationEvent> = _deviationEvent.asSharedFlow()

    // 到达目的地事件
    private val _arrivalEvent = MutableSharedFlow<Unit>()
    val arrivalEvent: SharedFlow<Unit> = _arrivalEvent.asSharedFlow()

    // 导航是否在进行中
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    /**
     * 偏差阈值（米），超过此值视为偏航
     */
    var deviationThresholdMeters = 20f

    /**
     * 到达判定半径（米）
     */
    var arrivalThresholdMeters = 15f

    /**
     * 开始跟踪导航路线
     */
    fun startTracking(
        aMap: AMap,
        route: WalkRouteInfo,
        initialPosition: LatLng
    ) {
        if (_isTracking.value) return

        currentRoute = route
        currentPolyline = route.polyline
        _remainingDistance.value = route.distanceMeters
        _remainingTime.value = route.durationSeconds
        lastSpokenStepIndex = -1
        _currentStepIndex.value = 0

        initUserMarker(aMap, initialPosition)
        initProgressPolyline(aMap)
        initTts()
        startHeadingArrow(aMap)

        _isTracking.value = true
    }

    /**
     * 更新 GPS 位置
     */
    fun onGpsUpdate(aMap: AMap, location: Location, amapLocation: com.amap.api.location.AMapLocation) {
        if (!_isTracking.value) return

        val pos = LatLng(location.latitude, location.longitude)
        _gpsPosition.value = pos

        updateUserMarker(aMap, pos)
        updateHeading(amapLocation.bearing, aMap, pos)
        val deviation = calculateDeviation(pos)
        val remaining = calculateRemainingDistance(pos)
        val stepIndex = findCurrentStepIndex(pos)

        _remainingDistance.value = remaining.toInt().coerceAtLeast(0)
        _remainingTime.value = estimateRemainingTime(remaining)
        _currentStepIndex.value = stepIndex

        updateProgressPolyline(aMap, pos)
        updateFollowCamera(aMap, pos, amapLocation.bearing)

        if (stepIndex != lastSpokenStepIndex) {
            lastSpokenStepIndex = stepIndex
            speakCurrentStep(stepIndex)
        }

        if (deviation > deviationThresholdMeters) {
            return
        }

        if (remaining <= arrivalThresholdMeters) {
            onArrived(aMap)
        }
    }

    /**
     * 停止跟踪
     */
    fun stopTracking(aMap: AMap) {
        _isTracking.value = false
        userMarker?.remove()
        userMarker = null
        progressPolyline?.remove()
        progressPolyline = null
        headingArrowMarker?.remove()
        headingArrowMarker = null
        currentRoute = null
        currentPolyline = emptyList()
        _gpsPosition.value = null
        _remainingDistance.value = 0
        _remainingTime.value = 0
        lastSpokenStepIndex = -1
    }

    private fun initUserMarker(aMap: AMap, position: LatLng) {
        userMarker = aMap.addMarker(
            MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location))
                .anchor(0.5f, 0.5f)
                .zIndex(100f)
                .title("我的位置")
        )
    }

    private fun updateUserMarker(aMap: AMap, position: LatLng) {
        if (userMarker == null) {
            initUserMarker(aMap, position)
        } else {
            userMarker?.position = position
        }
    }

    private fun initProgressPolyline(aMap: AMap) {
        progressPolyline = aMap.addPolyline(
            PolylineOptions()
                .color(0xFF6200EE.toInt())
                .width(14f)
                .zIndex(50f)
        )
    }

    private fun updateProgressPolyline(aMap: AMap, currentPos: LatLng) {
        progressPolyline?.remove()
        val points = buildProgressPolyline(currentPos)
        progressPolyline = aMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(0xFF6200EE.toInt())
                .width(14f)
                .zIndex(50f)
        )
    }

    private fun buildProgressPolyline(currentPos: LatLng): List<LatLng> {
        val polyline = currentPolyline
        if (polyline.isEmpty()) return listOf(currentPos)

        val index = findClosestPolylineIndex(currentPos)
        return if (index < 0) {
            listOf(currentPos)
        } else {
            polyline.subList(index, polyline.size)
        }
    }

    private fun initHeadingArrow(aMap: AMap, pos: LatLng, bearing: Float) {
        headingArrowMarker = aMap.addMarker(
            MarkerOptions()
                .position(pos)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_navigate))
                .anchor(0.5f, 0.5f)
                .rotateAngle(bearing)
                .zIndex(101f)
        )
    }

    private fun updateHeading(bearing: Float, aMap: AMap, pos: LatLng) {
        if (headingArrowMarker == null) {
            initHeadingArrow(aMap, pos, bearing)
        } else {
            headingArrowMarker?.position = pos
            headingArrowMarker?.rotateAngle = bearing
        }
    }

    private fun startHeadingArrow(aMap: AMap) {
        // 方向由 GPS bearing 提供，已在 updateHeading 处理
    }

    private fun initTts() {
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(1.1f)
                isTtsReady = true
            }
        }
    }

    private fun speakCurrentStep(stepIndex: Int) {
        if (!isTtsReady) return
        val polyline = currentPolyline
        if (polyline.isEmpty() || stepIndex >= polyline.size) return

        if (stepIndex == polyline.size - 1) {
            tts?.speak("前方到达目的地", TextToSpeech.QUEUE_FLUSH, null, "arrival")
            return
        }

        val next = polyline[stepIndex + 1]
        val dir = getDirectionDescription(polyline[stepIndex], next)
        val dist = if (stepIndex == 0) {
            "出发"
        } else {
            val d = distanceBetween(polyline[stepIndex - 1], next)
            if (d >= 100) "${(d / 100).toInt()}百米" else "${d.toInt()}米"
        }
        tts?.speak("${dist}后，$dir", TextToSpeech.QUEUE_FLUSH, null, "step_$stepIndex")
    }

    private fun getDirectionDescription(from: LatLng, to: LatLng): String {
        val bearing = bearingBetween(from, to)
        return when {
            bearing < -45 && bearing >= -135 -> "左转"
            bearing < 45 || bearing >= 315 -> "直行"
            bearing in 45.0..135.0 -> "右转"
            else -> "直行"
        }
    }

    /**
     * 计算 GPS 位置到最近路线点的距离（米）
     */
    fun calculateDeviation(gpsPos: LatLng): Float {
        val polyline = currentPolyline
        if (polyline.isEmpty()) return 0f

        var minDist = Float.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val dist = pointToSegmentDistance(gpsPos, polyline[i], polyline[i + 1])
            if (dist < minDist) minDist = dist
        }
        return minDist
    }

    private fun findClosestPolylineIndex(pos: LatLng): Int {
        val polyline = currentPolyline
        if (polyline.isEmpty()) return -1

        var minDist = Float.MAX_VALUE
        var closestIndex = 0
        for (i in polyline.indices) {
            val dist = distanceBetween(pos, polyline[i])
            if (dist < minDist) {
                minDist = dist
                closestIndex = i
            }
        }
        return closestIndex
    }

    private fun findCurrentStepIndex(pos: LatLng): Int {
        return findClosestPolylineIndex(pos)
    }

    private fun calculateRemainingDistance(pos: LatLng): Float {
        val polyline = currentPolyline
        if (polyline.isEmpty()) return 0f

        val startIndex = findClosestPolylineIndex(pos)
        if (startIndex < 0 || startIndex >= polyline.size) return 0f

        var total = 0f
        for (i in startIndex until polyline.size - 1) {
            total += distanceBetween(polyline[i], polyline[i + 1])
        }
        total += distanceBetween(pos, polyline[startIndex])
        return total
    }

    private fun estimateRemainingTime(remainingDistance: Float): Int {
        return (remainingDistance / 1.4f).toInt().coerceAtLeast(0)
    }

    private fun updateFollowCamera(aMap: AMap, pos: LatLng, bearing: Float) {
        aMap.animateCamera(
            com.amap.api.maps.CameraUpdateFactory.newCameraPosition(
                com.amap.api.maps.model.CameraPosition(pos, 18f, 0f, bearing)
            )
        )
    }

    private fun onArrived(aMap: AMap) {
        tts?.speak("已到达目的地，祝您校园生活愉快", TextToSpeech.QUEUE_FLUSH, null, "done")
        stopTracking(aMap)
    }

    /**
     * 释放 TTS 资源
     */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    // ── 几何计算工具 ────────────────────────────────────────────

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val x = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(x), sqrt(1 - x))
        return (earthRadius * c).toFloat()
    }

    private fun bearingBetween(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360 - 180).toFloat()
    }

    private fun pointToSegmentDistance(p: LatLng, a: LatLng, b: LatLng): Float {
        val ax = a.longitude
        val ay = a.latitude
        val bx = b.longitude
        val by = b.latitude
        val px = p.longitude
        val py = p.latitude

        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy

        if (len2 == 0.0) return distanceBetween(p, a)

        val t = (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
        val projX = ax + t * dx
        val projY = ay + t * dy

        val proj = LatLng(projY, projX)
        return distanceBetween(p, proj)
    }
}

/**
 * 偏航事件
 */
data class DeviationEvent(
    val deviationMeters: Float,
    val currentPosition: LatLng,
    val nearestRoutePoint: LatLng
)
