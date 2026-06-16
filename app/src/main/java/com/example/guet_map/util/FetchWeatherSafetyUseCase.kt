package com.example.guet_map.util

import com.example.guet_map.module.social.data.repository.WeatherRepository
import com.example.guet_map.model.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取天气安全提示文案。
 *
 * 原由 AI 模块（AiServiceImpl）依赖；该类在原仓库中缺失，会导致编译失败。
 * 此处提供一个最小实现：从 [WeatherRepository] 拉取当前天气，使用其
 * alertMessage 字段作为安全提示。
 */
@Singleton
class FetchWeatherSafetyUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(
        latitude: Double = WeatherRepository.DEFAULT_LATITUDE,
        longitude: Double = WeatherRepository.DEFAULT_LONGITUDE
    ): String {
        return when (val result = weatherRepository.getWeather(latitude, longitude)) {
            is Resource.Success -> result.data.alertMessage.orEmpty()
            is Resource.Error, Resource.Loading -> ""
        }
    }
}
