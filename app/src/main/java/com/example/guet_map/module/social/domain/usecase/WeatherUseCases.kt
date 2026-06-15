package com.example.guet_map.module.social.domain.usecase

import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.repository.WeatherRepository
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取天气用例
 */
class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /**
     * 获取天气
     * @param latitude 纬度，默认桂林电子科技大学
     * @param longitude 经度，默认桂林电子科技大学
     */
    suspend operator fun invoke(
        latitude: Double = WeatherRepository.DEFAULT_LATITUDE,
        longitude: Double = WeatherRepository.DEFAULT_LONGITUDE
    ): Resource<Weather> {
        return weatherRepository.getWeather(latitude, longitude)
    }

    /**
     * 观察天气变化（Flow）
     */
    fun observe(): Flow<Weather?> {
        return weatherRepository.observeWeather()
    }
}

/**
 * 刷新天气用例
 */
class RefreshWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /**
     * 强制刷新天气
     * @param latitude 纬度
     * @param longitude 经度
     */
    suspend operator fun invoke(
        latitude: Double = WeatherRepository.DEFAULT_LATITUDE,
        longitude: Double = WeatherRepository.DEFAULT_LONGITUDE
    ): Resource<Weather> {
        return weatherRepository.refreshWeather(latitude, longitude)
    }
}
