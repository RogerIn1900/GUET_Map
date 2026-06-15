package com.example.guet_map.module.social.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentWeatherDetailBinding
import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherForecast
import com.example.guet_map.module.social.data.model.WeatherType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 天气预报详情页
 * 支持：当前天气 + 小时预报 + 7天预报 + 16天预报 + 温度趋势图 + 地点切换
 */
@AndroidEntryPoint
class WeatherDetailFragment : Fragment() {

    private var _binding: FragmentWeatherDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherDetailViewModel by viewModels()

    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter7: DailyForecastAdapter
    private lateinit var dailyAdapter16: DailyForecastAdapter
    private lateinit var locationSheet: LocationSelectBottomSheet

    private var cachedForecast: WeatherForecast? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupAdapters()
        setupTabs()
        setupSwipeRefresh()
        setupErrorRetry()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.ivLocationSwitch.setOnClickListener {
            showLocationSheet()
        }
    }

    private fun setupAdapters() {
        hourlyAdapter = HourlyForecastAdapter()
        binding.rvHourlyForecast.adapter = hourlyAdapter

        dailyAdapter7 = DailyForecastAdapter()
        binding.rvDailyForecast7.adapter = dailyAdapter7

        dailyAdapter16 = DailyForecastAdapter()
        binding.rvDailyForecast16.adapter = dailyAdapter16
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("小时预报"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("7 天"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("16 天"))

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                when (tab.position) {
                    0 -> showHourlyTab()
                    1 -> show7DayTab()
                    2 -> show16DayTab()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        // 默认显示小时预报
        showHourlyTab()
    }

    private fun showHourlyTab() {
        binding.rvHourlyForecast.visibility = View.VISIBLE
        binding.rvDailyForecast7.visibility = View.GONE
        binding.layoutMonthly.visibility = View.GONE
        cachedForecast?.let {
            hourlyAdapter.submitList(it.current.hourlyForecast)
        }
    }

    private fun show7DayTab() {
        binding.rvHourlyForecast.visibility = View.GONE
        binding.rvDailyForecast7.visibility = View.VISIBLE
        binding.layoutMonthly.visibility = View.GONE
        cachedForecast?.let {
            dailyAdapter7.submitList(it.dailyForecast.take(7))
        }
    }

    private fun show16DayTab() {
        binding.rvHourlyForecast.visibility = View.GONE
        binding.rvDailyForecast7.visibility = View.GONE
        binding.layoutMonthly.visibility = View.VISIBLE
        cachedForecast?.let {
            dailyAdapter16.submitList(it.dailyForecast)
            setupChart(it.dailyForecast)
        }
    }

    private fun setupChart(dailyForecast: List<DailyForecast>) {
        val dateLabels = dailyForecast.map { formatDateLabel(it.date) }
        binding.chartMonthly.setData(dailyForecast, dateLabels)
    }

    private fun formatDateLabel(date: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = SimpleDateFormat("MM/dd", Locale.US)
            output.format(input.parse(date)!!)
        } catch (_: Exception) {
            date.takeLast(5)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupErrorRetry() {
        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefresh.isRefreshing = false
                        when (state) {
                            is WeatherDetailUiState.Loading -> showLoading()
                            is WeatherDetailUiState.Success -> showSuccess(state.data)
                            is WeatherDetailUiState.Error -> showError(state.message)
                        }
                    }
                }
                launch {
                    viewModel.currentLocation.collect { location ->
                        binding.tvLocationName.text = location.name
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun showSuccess(data: WeatherForecast) {
        cachedForecast = data
        binding.progressBar.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE

        bindCurrentWeather(data.current)
        bindLocation(data.locationName)

        // 刷新当前 Tab
        when (binding.tabLayout.selectedTabPosition) {
            0 -> {
                hourlyAdapter.submitList(data.current.hourlyForecast)
            }
            1 -> {
                dailyAdapter7.submitList(data.dailyForecast.take(7))
            }
            2 -> {
                dailyAdapter16.submitList(data.dailyForecast)
                setupChart(data.dailyForecast)
            }
        }
    }

    private fun bindCurrentWeather(weather: Weather) {
        binding.tvCurrentTemp.text = "${weather.temperature}°"
        binding.tvCurrentDesc.text = weather.description
        binding.tvFeelsLike.text = "体感 ${weather.feelsLike}°"
        binding.tvHumidity.text = "湿度 ${weather.humidity}%"
        binding.tvWind.text = "${weather.windDirection} ${weather.windSpeed.toInt()}m/s"
        binding.tvUvIndex.text = "紫外线 ${weather.uvIndex ?: "--"}"

        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        binding.tvSunrise.text = "日出 ${timeFormat.format(weather.sunrise)}"
        binding.tvSunset.text = "日落 ${timeFormat.format(weather.sunset)}"

        val iconRes = getWeatherIconRes(weather.weatherType)
        binding.ivCurrentWeatherIcon.setImageResource(iconRes)

        if (!weather.alertMessage.isNullOrBlank()) {
            binding.tvAlertMessage.text = weather.alertMessage
            binding.tvAlertMessage.visibility = View.VISIBLE
        } else {
            binding.tvAlertMessage.visibility = View.GONE
        }
    }

    private fun bindLocation(name: String) {
        binding.tvLocationName.text = name
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.scrollView.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun showLocationSheet() {
        locationSheet = LocationSelectBottomSheet.newInstance(
            currentLocation = viewModel.currentLocation.value
        )
        locationSheet.onLocationSelected = { location ->
            viewModel.switchLocation(location)
        }
        locationSheet.show(childFragmentManager, "location_sheet")
    }

    private fun getWeatherIconRes(type: WeatherType): Int = when (type) {
        WeatherType.SUNNY -> R.drawable.ic_weather_sunny
        WeatherType.CLOUDY -> R.drawable.ic_weather_cloudy
        WeatherType.OVERCAST -> R.drawable.ic_weather_overcast
        WeatherType.LIGHT_RAIN -> R.drawable.ic_weather_light_rain
        WeatherType.MODERATE_RAIN -> R.drawable.ic_weather_moderate_rain
        WeatherType.HEAVY_RAIN -> R.drawable.ic_weather_moderate_rain
        WeatherType.THUNDERSTORM -> R.drawable.ic_weather_thunderstorm
        WeatherType.SNOW -> R.drawable.ic_weather_snow
        WeatherType.FOG -> R.drawable.ic_weather_fog
        WeatherType.WINDY -> R.drawable.ic_weather_windy
        WeatherType.UNKNOWN -> R.drawable.ic_weather_unknown
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
