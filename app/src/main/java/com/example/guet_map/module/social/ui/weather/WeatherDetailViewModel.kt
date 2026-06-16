package com.example.guet_map.module.social.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.domain.usecase.GetWeatherForecastUseCase
import com.example.guet_map.module.social.data.repository.WeatherRepository
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 天气预报详情页 ViewModel
 */
@HiltViewModel
class WeatherDetailViewModel @Inject constructor(
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherDetailUiState>(WeatherDetailUiState.Loading)
    val uiState: StateFlow<WeatherDetailUiState> = _uiState.asStateFlow()

    private val _currentLocation = MutableStateFlow(WeatherLocation.default())
    val currentLocation: StateFlow<WeatherLocation> = _currentLocation.asStateFlow()

    init {
        loadWeather()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherDetailUiState.Loading
            val location = _currentLocation.value
            when (val result = getWeatherForecastUseCase(
                latitude = location.lat,
                longitude = location.lng,
                locationName = location.name
            )) {
                is Resource.Success -> {
                    _uiState.value = WeatherDetailUiState.Success(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = WeatherDetailUiState.Error(result.message)
                }
                is Resource.Loading -> {
                    _uiState.value = WeatherDetailUiState.Loading
                }
            }
        }
    }

    fun refresh() {
        loadWeather()
    }

    fun switchLocation(location: WeatherLocation) {
        _currentLocation.value = location
        loadWeather()
    }

    fun getDailyForDays(days: Int): List<DailyForecast> {
        val state = _uiState.value
        return if (state is WeatherDetailUiState.Success) {
            state.data.dailyForecast.take(days)
        } else emptyList()
    }
}

// ── UI 状态 ────────────────────────────────────────────────

sealed class WeatherDetailUiState {
    data object Loading : WeatherDetailUiState()
    data class Success(val data: com.example.guet_map.module.social.data.model.WeatherForecast) : WeatherDetailUiState()
    data class Error(val message: String) : WeatherDetailUiState()
}

// ── 地点 ──────────────────────────────────────────────────

data class WeatherLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val isCustom: Boolean = false
) {
    companion object {
        fun default() = WeatherLocation(
            name = WeatherRepository.DEFAULT_LOCATION_NAME,
            lat = WeatherRepository.DEFAULT_LATITUDE,
            lng = WeatherRepository.DEFAULT_LONGITUDE
        )

        fun custom(name: String, lat: Double, lng: Double) =
            WeatherLocation(name, lat, lng, isCustom = true)

        val PRESETS = listOf(
            default(),
            WeatherLocation("桂林市七星区", 25.28, 110.30),
            WeatherLocation("桂林市象山区", 25.26, 110.29),
            WeatherLocation("桂林市秀峰区", 25.29, 110.28),
            WeatherLocation("桂林市叠彩区", 25.31, 110.28),
            WeatherLocation("桂林市临桂区", 25.23, 110.20),
            WeatherLocation("桂林两江国际机场", 25.22, 110.04),
            WeatherLocation("桂林北站", 25.33, 110.29),
            WeatherLocation("桂林站", 25.27, 110.29),
            WeatherLocation("阳朔县", 24.78, 110.49),
        )
    }
}
