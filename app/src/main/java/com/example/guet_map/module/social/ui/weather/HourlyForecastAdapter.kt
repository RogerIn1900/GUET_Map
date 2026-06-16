package com.example.guet_map.module.social.ui.weather

import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemHourlyForecastBinding
import com.example.guet_map.module.social.data.model.HourlyWeather
import com.example.guet_map.module.social.data.model.WeatherType
import java.util.Calendar

class HourlyForecastAdapter : ListAdapter<HourlyWeather, HourlyForecastAdapter.ViewHolder>(Diff) {

    /** 昨日同时刻温度列表（与今天 hour 顺序一致，长度=24）。null=不显示对比 */
    private var yesterdayTemps: List<Int>? = null

    fun setYesterdayTemps(temps: List<Int>?) {
        yesterdayTemps = temps
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val prevDayIndex = if (position > 0) getItem(position - 1).dayIndex else -1
        val yestTemp = yesterdayTemps?.getOrNull(item.hour)
        holder.bind(item, prevDayIndex, yestTemp)
    }

    class ViewHolder(
        private val binding: ItemHourlyForecastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val primaryColor: Int = resolveThemeColor(
            com.google.android.material.R.attr.colorPrimary
        )
        private val onSurfaceVariantColor: Int = resolveThemeColor(
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        private val infoColor: Int = androidx.core.content.ContextCompat.getColor(
            itemView.context, com.example.guet_map.R.color.info
        )
        private val errorColor: Int = androidx.core.content.ContextCompat.getColor(
            itemView.context, com.example.guet_map.R.color.error
        )

        fun bind(item: HourlyWeather, prevDayIndex: Int, yesterdayTemp: Int?) {
            val isToday = item.dayIndex == 0
            val isDayStart = item.dayIndex != prevDayIndex
            // 1) 时间文本
            if (isToday && isCurrentHour(item.hour)) {
                binding.tvHour.text = "现在"
                binding.tvHour.setTextColor(primaryColor)
                binding.tvHour.setTypeface(null, Typeface.BOLD)
            } else {
                binding.tvHour.text = String.format("%02d:00", item.hour)
                binding.tvHour.setTextColor(onSurfaceVariantColor)
                binding.tvHour.setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)
            }

            // 2) 跨日 chip
            if (isDayStart && !isToday) {
                binding.tvDayLabel.visibility = View.VISIBLE
                binding.tvDayLabel.text = getDayLabel(item.dayIndex)
            } else {
                binding.tvDayLabel.visibility = View.GONE
            }

            // 3) 当日高亮（轻量化：仅底色 + 加粗时间）
            binding.viewTodayHighlight.visibility = if (isToday) View.VISIBLE else View.GONE

            // 4) 温度 + 体感
            binding.tvHourlyTemp.text = "${item.temperature}°"
            binding.tvHourlyTemp.setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)
            binding.tvFeelsLike.text = "体感${item.feelsLike}°"

            // 5) 风向风速（带 ↑↓ 升温降温箭头）
            if (yesterdayTemp != null) {
                val diff = item.temperature - yesterdayTemp
                when {
                    diff > 0 -> {
                        binding.tvWindDir.text = "↑"
                        binding.tvWindDir.setTextColor(errorColor)
                    }
                    diff < 0 -> {
                        binding.tvWindDir.text = "↓"
                        binding.tvWindDir.setTextColor(infoColor)
                    }
                    else -> {
                        binding.tvWindDir.text = "→"
                        binding.tvWindDir.setTextColor(onSurfaceVariantColor)
                    }
                }
                binding.tvWindSpeed.text = "${kotlin.math.abs(diff)}°"
            } else {
                binding.tvWindDir.text = item.windDirection.take(1).ifEmpty { "·" }
                binding.tvWindDir.setTextColor(onSurfaceVariantColor)
                binding.tvWindSpeed.text = if (item.windLevel.isNotEmpty()) item.windLevel else "${item.windSpeed.toInt()}m/s"
            }

            // 6) 降水（按强度变色）
            binding.tvPrecip.text = "${item.precipitation}%"
            val precipColor = when {
                item.precipitation >= 70 -> errorColor
                item.precipitation >= 30 -> infoColor
                else -> onSurfaceVariantColor
            }
            binding.tvPrecip.setTextColor(precipColor)
            binding.tvPrecip.alpha = if (item.precipitation > 0) 1f else 0.5f

            // 7) 跨日 alpha 渐变
            binding.root.alpha = 1f - 0.15f * item.dayIndex.coerceAtMost(3)

            // 8) 图标
            val iconRes = getWeatherIconRes(item.weatherType)
            binding.ivHourlyIcon.setImageResource(iconRes)
        }

        private fun isCurrentHour(hour: Int): Boolean {
            val now = Calendar.getInstance()
            return now.get(Calendar.HOUR_OF_DAY) == hour
        }

        private fun resolveThemeColor(attr: Int): Int {
            val tv = TypedValue()
            val ctx = itemView.context
            ctx.theme.resolveAttribute(attr, tv, true)
            return tv.data
        }

        private fun getDayLabel(dayIndex: Int): String {
            // 非当日显示具体日期 MM/dd
            if (dayIndex <= 0) return ""
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, dayIndex)
            return String.format("%02d/%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
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
    }

    private object Diff : DiffUtil.ItemCallback<HourlyWeather>() {
        override fun areItemsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather) =
            oldItem.hour == newItem.hour && oldItem.dayIndex == newItem.dayIndex

        override fun areContentsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather) =
            oldItem == newItem
    }
}
