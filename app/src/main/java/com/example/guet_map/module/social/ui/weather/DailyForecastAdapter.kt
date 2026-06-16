package com.example.guet_map.module.social.ui.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemDailyForecastBinding
import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.WeatherType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 每日天气预报 Adapter
 */
class DailyForecastAdapter : ListAdapter<DailyForecast, DailyForecastAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDailyForecastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyForecast) {
            binding.tvDate.text = formatDate(item.date)
            binding.tvWeekday.text = getWeekday(item.date)
            binding.tvTempHigh.text = "${item.temperatureHigh}°"
            binding.tvTempLow.text = "${item.temperatureLow}°"
            binding.tvPrecip.text = "${item.precipitation}%"
            binding.tvPrecip.alpha = if (item.precipitation > 0) 1f else 0.4f
            binding.tvUv.text = "UV ${item.uvIndex}"

            val iconRes = when (item.weatherType) {
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
            binding.ivWeatherIcon.setImageResource(iconRes)
        }

        private fun formatDate(date: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val output = SimpleDateFormat("MM月dd日", Locale.CHINA)
                output.format(input.parse(date)!!)
            } catch (_: Exception) {
                date
            }
        }

        private fun getWeekday(date: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val output = SimpleDateFormat("EEE", Locale.CHINA)
                output.format(input.parse(date)!!)
            } catch (_: Exception) {
                ""
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DailyForecast>() {
        override fun areItemsTheSame(oldItem: DailyForecast, newItem: DailyForecast) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: DailyForecast, newItem: DailyForecast) =
            oldItem == newItem
    }
}
