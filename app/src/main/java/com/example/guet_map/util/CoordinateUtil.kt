package com.example.guet_map.util

import android.content.Context
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.model.LatLng

/**
 * 系统 GPS 为 WGS-84，高德地图使用 GCJ-02，不转换会产生明显偏移。
 */
object CoordinateUtil {

    fun wgs84ToGcj02(context: Context, latitude: Double, longitude: Double): LatLng {
        return try {
            val converter = CoordinateConverter(context)
            converter.from(CoordinateConverter.CoordType.GPS)
            converter.coord(LatLng(latitude, longitude))
            converter.convert()
        } catch (_: Exception) {
            LatLng(latitude, longitude)
        }
    }
}
