package com.example.guet_map.module.social.data.repository

import com.example.guet_map.model.Resource
import com.example.guet_map.module.social.data.local.dao.WeatherDao
import com.example.guet_map.module.social.data.local.entity.WeatherEntity
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherForecast
import com.example.guet_map.module.social.data.remote.OpenMeteoApiService
import com.example.guet_map.module.social.data.remote.dto.toDailyForecast
import com.example.guet_map.module.social.data.remote.dto.toWeather
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 天气数据仓库
 *
 * 使用 Open-Meteo（免费，无 key），按经纬度直接查询：
 * - 一次 `/v1/forecast` 同时拿 current + hourly(168h) + daily(16d)
 * - 缓存在 Room，30 分钟有效
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherDao: WeatherDao,
    private val openMeteoApi: OpenMeteoApiService,
    private val gson: Gson
) {

    /** 观察天气变化（从数据库） */
    fun observeWeather(): Flow<Weather?> {
        return weatherDao.observeLatestWeather().map { entity ->
            entity?.toDomain()
        }
    }

    /** 缓存优先 + 失败回退 */
    suspend fun getWeather(
        latitude: Double = DEFAULT_LATITUDE,
        longitude: Double = DEFAULT_LONGITUDE
    ): Resource<Weather> {
        return try {
            val cached = weatherDao.getLatestWeather()
            if (cached != null && isCacheValid(cached.cachedAt)) {
                return Resource.Success(cached.toDomain())
            }
            fetchAndCacheWeather(latitude, longitude)
        } catch (e: IOException) {
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

    /** 强制刷新 */
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

    /** 一次请求拿到 current + 16 天预报（详情页用） */
    suspend fun getWeatherForecast(
        latitude: Double = DEFAULT_LATITUDE,
        longitude: Double = DEFAULT_LONGITUDE,
        locationName: String = DEFAULT_LOCATION_NAME
    ): Resource<WeatherForecast> {
        return try {
            val resp = openMeteoApi.getForecast(
                latitude = latitude,
                longitude = longitude
            )
            val weather = resp.toWeather(locationName = locationName)
                ?: return Resource.Error("天气数据为空")

            val dailyForecast = resp.toDailyForecast(fallbackHumidity = weather.humidity)
            if (dailyForecast.isEmpty()) {
                return Resource.Error("预报数据为空")
            }

            weatherDao.insertWeather(weather.toEntity())

            Resource.Success(
                WeatherForecast(
                    current = weather,
                    dailyForecast = dailyForecast,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        } catch (e: IOException) {
            Resource.Error("网络不可用，请检查网络连接")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "获取天气预报失败")
        }
    }

    /** 从 API 获取天气并缓存 */
    private suspend fun fetchAndCacheWeather(
        latitude: Double,
        longitude: Double
    ): Resource<Weather> {
        val resp = openMeteoApi.getForecast(
            latitude = latitude,
            longitude = longitude
        )
        val weather = resp.toWeather()
            ?: return Resource.Error("天气数据为空")

        weatherDao.insertWeather(weather.toEntity())
        return Resource.Success(weather)
    }

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
        // 默认地点：桂林电子科技大学
        const val DEFAULT_LATITUDE = 25.27
        const val DEFAULT_LONGITUDE = 110.29
        const val DEFAULT_LOCATION_NAME = "桂林电子科技大学"

        // 缓存有效期: 30 分钟
        private const val CACHE_VALIDITY_MS = 30 * 60 * 1000L
    }
}
