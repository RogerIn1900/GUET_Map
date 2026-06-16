package com.example.guet_map.module.social.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentWeatherDetailBinding
import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.data.model.WeatherForecast
import com.example.guet_map.module.social.data.model.WeatherType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private var pendingChartIndex: Int = -1

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
        binding.rvHourlyForecast.layoutManager = LinearLayoutManager(
            requireContext(), RecyclerView.VERTICAL, false
        )
        binding.rvHourlyForecast.adapter = hourlyAdapter
        applyItemEnterAnimation(binding.rvHourlyForecast)
        binding.rvHourlyForecast.addItemDecoration(
            StickyHeaderItemDecoration(R.layout.view_sticky_header) { view ->
                view.findViewById<View>(R.id.miniChart).visibility = View.VISIBLE
                view.findViewById<View>(R.id.llHourlyHeader).visibility = View.VISIBLE
                view.findViewById<View>(R.id.llDailyHeader).visibility = View.GONE
                // 同步数据
                cachedForecast?.let { fc ->
                    val now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    (view.findViewById<View>(R.id.miniChart) as? HourlyMiniChart)
                        ?.setData(fc.current.hourlyForecast, now)
                }
            }
        )

        dailyAdapter7 = DailyForecastAdapter()
        binding.rvDailyForecast7.layoutManager = LinearLayoutManager(
            requireContext(), RecyclerView.VERTICAL, false
        )
        binding.rvDailyForecast7.adapter = dailyAdapter7
        applyItemEnterAnimation(binding.rvDailyForecast7)
        binding.rvDailyForecast7.addItemDecoration(
            StickyHeaderItemDecoration(R.layout.view_sticky_header) { view ->
                view.findViewById<View>(R.id.miniChart).visibility = View.GONE
                view.findViewById<View>(R.id.llHourlyHeader).visibility = View.GONE
                view.findViewById<View>(R.id.llDailyHeader).visibility = View.VISIBLE
            }
        )
        // 7日 list 滚 → chart 不联动（chart 不显示）
        binding.rvDailyForecast7.addOnScrollListener(object : RecyclerView.OnScrollListener() {})

        dailyAdapter16 = DailyForecastAdapter()
        binding.rvDailyForecast16.layoutManager = LinearLayoutManager(
            requireContext(), RecyclerView.VERTICAL, false
        )
        binding.rvDailyForecast16.adapter = dailyAdapter16
        applyItemEnterAnimation(binding.rvDailyForecast16)
        binding.rvDailyForecast16.addItemDecoration(
            StickyHeaderItemDecoration(R.layout.view_sticky_header) { view ->
                view.findViewById<View>(R.id.miniChart).visibility = View.GONE
                view.findViewById<View>(R.id.llHourlyHeader).visibility = View.GONE
                view.findViewById<View>(R.id.llDailyHeader).visibility = View.VISIBLE
            }
        )
        // 16日 list 滚 → chart 高亮同步
        binding.rvDailyForecast16.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (binding.chartMonthly.visibility != View.VISIBLE) return
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val first = lm.findFirstVisibleItemPosition()
                if (first != RecyclerView.NO_POSITION) {
                    binding.chartMonthly.setSelectedIndex(first)
                }
            }
        })
    }

    private fun applyItemEnterAnimation(rv: RecyclerView) {
        val anim = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        anim.duration = 350
        val controller = LayoutAnimationController(anim, 0.05f)
        rv.layoutAnimation = controller
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

        showHourlyTab()
    }

    private fun show16DayTab() {
        binding.rvHourlyForecast.visibility = View.GONE
        binding.rvDailyForecast7.visibility = View.GONE
        binding.layoutMonthly.visibility = View.VISIBLE
        binding.chartMonthly.visibility = View.VISIBLE
        binding.appBar.setExpanded(true, true)
        cachedForecast?.let { fc ->
            dailyAdapter16.submitList(fc.dailyForecast) {
                setupChart(fc.dailyForecast)
                val (gMin, gMax) = globalTempRange(fc.dailyForecast)
                dailyAdapter16.setGlobalTempRange(gMin, gMax)
            }
        }
    }

    private fun showHourlyTab() {
        binding.rvHourlyForecast.visibility = View.VISIBLE
        binding.rvDailyForecast7.visibility = View.GONE
        binding.layoutMonthly.visibility = View.GONE
        binding.chartMonthly.visibility = View.GONE
    }

    private fun show7DayTab() {
        binding.rvHourlyForecast.visibility = View.GONE
        binding.rvDailyForecast7.visibility = View.VISIBLE
        binding.layoutMonthly.visibility = View.GONE
        binding.chartMonthly.visibility = View.GONE
        cachedForecast?.let { fc ->
            dailyAdapter7.submitList(fc.dailyForecast.take(7)) {
                val (gMin, gMax) = globalTempRange(fc.dailyForecast)
                dailyAdapter7.setGlobalTempRange(gMin, gMax)
            }
        }
    }

    private fun setupChart(dailyForecast: List<DailyForecast>) {
        val dateLabels = dailyForecast.map { formatDateLabel(it.date) }
        binding.chartMonthly.setData(dailyForecast, dateLabels)
        binding.chartMonthly.setOnValueSelectedListener { index ->
            // chart 选点 → list 滚 + 高亮
            binding.rvDailyForecast16.smoothScrollToPosition(index)
            dailyAdapter16.setHighlightedPosition(index)
        }
        if (pendingChartIndex >= 0) {
            binding.chartMonthly.setSelectedIndex(pendingChartIndex)
            pendingChartIndex = -1
        }
    }

    private fun globalTempRange(daily: List<DailyForecast>): Pair<Int, Int> {
        if (daily.isEmpty()) return 0 to 0
        val highs = daily.map { it.temperatureHigh }
        val lows = daily.map { it.temperatureLow }
        return lows.min() to highs.max()
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
        binding.tabContent.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun showSuccess(data: WeatherForecast) {
        cachedForecast = data
        binding.progressBar.visibility = View.GONE
        binding.tabContent.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE

        bindCurrentWeather(data.current)
        bindLocation(data.locationName)

        // 设置昨日温度（用于 ↑↓ 升温降温箭头）
        // 实际应由 viewModel 提供，这里用上一日数据模拟
        val yesterdayTemps = if (data.dailyForecast.size >= 2) {
            // 用昨日同时段温度（简化处理：用前日日均温重复 24 次）
            val yAvg = (data.dailyForecast[1].temperatureHigh + data.dailyForecast[1].temperatureLow) / 2
            List(24) { yAvg }
        } else null
        hourlyAdapter.setYesterdayTemps(yesterdayTemps)

        when (binding.tabLayout.selectedTabPosition) {
            0 -> {
                hourlyAdapter.submitList(data.current.hourlyForecast)
                binding.chartMonthly.visibility = View.GONE
            }
            1 -> {
                dailyAdapter7.submitList(data.dailyForecast.take(7)) {
                    val (gMin, gMax) = globalTempRange(data.dailyForecast)
                    dailyAdapter7.setGlobalTempRange(gMin, gMax)
                }
                binding.chartMonthly.visibility = View.GONE
            }
            2 -> {
                dailyAdapter16.submitList(data.dailyForecast) {
                    val (gMin, gMax) = globalTempRange(data.dailyForecast)
                    dailyAdapter16.setGlobalTempRange(gMin, gMax)
                    setupChart(data.dailyForecast)
                }
                binding.chartMonthly.visibility = View.VISIBLE
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
        binding.tabContent.visibility = View.GONE
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
