package com.example.guet_map.module.social.domain.usecase

import com.example.guet_map.module.social.data.model.WeatherForecast
import com.example.guet_map.module.social.data.repository.WeatherRepository
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取天气用例（当前 + 24h 预报）
 */
class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(
        latitude: Double = WeatherRepository.DEFAULT_LATITUDE,
        longitude: Double = WeatherRepository.DEFAULT_LONGITUDE
    ): Resource<com.example.guet_map.module.social.data.model.Weather> {
        return weatherRepository.getWeather(latitude, longitude)
    }

    fun observe(): Flow<com.example.guet_map.module.social.data.model.Weather?> {
        return weatherRepository.observeWeather()
    }
}

/**
 * 获取完整天气预报（当前 + 16 天预报 + 逐小时）
 */
class GetWeatherForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(
        latitude: Double = WeatherRepository.DEFAULT_LATITUDE,
        longitude: Double = WeatherRepository.DEFAULT_LONGITUDE,
        locationName: String = WeatherRepository.DEFAULT_LOCATION_NAME
    ): Resource<WeatherForecast> {
        return weatherRepository.getWeatherForecast(latitude, longitude, locationName)
    }
}

/**
 * 刷新天气用例
 */
class RefreshWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(
        latitude: Double = WeatherRepository.DEFAULT_LATITUDE,
        longitude: Double = WeatherRepository.DEFAULT_LONGITUDE
    ): Resource<com.example.guet_map.module.social.data.model.Weather> {
        return weatherRepository.refreshWeather(latitude, longitude)
    }
}
