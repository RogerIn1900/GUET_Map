package com.example.guet_map.module.social.data.repository

import com.example.guet_map.model.Resource
import com.example.guet_map.module.social.data.local.dao.WeatherDao
import com.example.guet_map.module.social.data.local.entity.WeatherEntity
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.remote.OpenMeteoApiService
import com.example.guet_map.module.social.data.remote.dto.toDomain
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 天气数据仓库
 * 采用缓存优先策略：先返回缓存，后台静默刷新
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherDao: WeatherDao,
    private val openMeteoApi: OpenMeteoApiService,
    private val gson: Gson
) {

    /**
     * 观察天气变化（从数据库）
     */
    fun observeWeather(): Flow<Weather?> {
        return weatherDao.observeLatestWeather().map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * 获取天气
     * 策略：缓存有效则直接返回，否则请求 API
     */
    suspend fun getWeather(
        latitude: Double = DEFAULT_LATITUDE,
        longitude: Double = DEFAULT_LONGITUDE
    ): Resource<Weather> {
        return try {
            // 检查缓存
            val cached = weatherDao.getLatestWeather()
            if (cached != null && isCacheValid(cached.cachedAt)) {
                return Resource.Success(cached.toDomain())
            }

            // 请求 API
            fetchAndCacheWeather(latitude, longitude)
        } catch (e: IOException) {
            // 网络错误，尝试返回缓存
            val cached = weatherDao.getLatestWeather()
            if (cached != null) {
                Resource.Success(cached.toDomain())
            } else {
                Resource.Error("网络不可用，请检查网络连接")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "获取天气失败")
        }
    }

    /**
     * 强制刷新天气
     */
    suspend fun refreshWeather(
        latitude: Double = DEFAULT_LATITUDE,
        longitude: Double = DEFAULT_LONGITUDE
    ): Resource<Weather> {
        return try {
            fetchAndCacheWeather(latitude, longitude)
        } catch (e: IOException) {
            Resource.Error("网络不可用，请检查网络连接")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "刷新天气失败")
        }
    }

    /**
     * 从 API 获取天气并缓存
     */
    private suspend fun fetchAndCacheWeather(
        latitude: Double,
        longitude: Double
    ): Resource<Weather> {
        val response = openMeteoApi.getWeatherForecast(
            latitude = latitude,
            longitude = longitude
        )

        if (response.current == null) {
            return Resource.Error("获取天气数据失败")
        }

        val weather = response.toDomain()
        weatherDao.insertWeather(weather.toEntity())
        return Resource.Success(weather)
    }

    /**
     * 检查缓存是否有效 (30分钟有效期)
     */
    private fun isCacheValid(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt < CACHE_VALIDITY_MS
    }

    private fun WeatherEntity.toDomain() = Weather(
        id = id,
        temperature = temperature,
        feelsLike = feelsLike,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherType = try {
            com.example.guet_map.module.social.data.model.WeatherType.valueOf(weatherType)
        } catch (e: Exception) {
            com.example.guet_map.module.social.data.model.WeatherType.UNKNOWN
        },
        description = description,
        aqi = if (aqi > 0) aqi else null,
        aqiLevel = aqiLevel.takeIf { it.isNotEmpty() },
        uvIndex = if (uvIndex > 0) uvIndex else null,
        sunrise = sunrise,
        sunset = sunset,
        hourlyForecast = gson.fromJson(hourlyForecast, object : TypeToken<List<com.example.guet_map.module.social.data.model.HourlyWeather>>() {}.type) ?: emptyList(),
        alertMessage = alertMessage.takeIf { it.isNotEmpty() }
    )

    private fun Weather.toEntity() = WeatherEntity(
        id = id,
        temperature = temperature,
        feelsLike = feelsLike,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherType = weatherType.name,
        description = description,
        aqi = aqi ?: 0,
        aqiLevel = aqiLevel ?: "",
        uvIndex = uvIndex ?: 0,
        sunrise = sunrise,
        sunset = sunset,
        hourlyForecast = gson.toJson(hourlyForecast),
        alertMessage = alertMessage ?: ""
    )

    companion object {
        // 默认桂林电子科技大学坐标
        const val DEFAULT_LATITUDE = 25.27
        const val DEFAULT_LONGITUDE = 110.29

        // 缓存有效期: 30分钟
        private const val CACHE_VALIDITY_MS = 30 * 60 * 1000L
    }
}
