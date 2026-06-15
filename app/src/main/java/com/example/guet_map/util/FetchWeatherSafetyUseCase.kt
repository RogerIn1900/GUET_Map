package com.example.guet_map.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchWeatherSafetyUseCase @Inject constructor() {

    operator fun invoke(latitude: Double, longitude: Double): String {
        return "桂林近期天气多变，出行请关注实时天气，注意防晒补水。"
    }
}
