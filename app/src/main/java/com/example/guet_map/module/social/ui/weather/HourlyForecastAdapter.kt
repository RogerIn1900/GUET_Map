package com.example.guet_map.module.social.ui.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemHourlyForecastBinding
import com.example.guet_map.module.social.data.model.HourlyWeather
import com.example.guet_map.module.social.data.model.WeatherType

class HourlyForecastAdapter : ListAdapter<HourlyWeather, HourlyForecastAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemHourlyForecastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyWeather) {
            binding.tvHour.text = String.format("%02d:00", item.hour)
            binding.tvHourlyTemp.text = "${item.temperature}°"
            binding.tvPrecip.text = "${item.precipitation}%"
            binding.tvPrecip.alpha = if (item.precipitation > 0) 1f else 0.5f

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
            binding.ivHourlyIcon.setImageResource(iconRes)
        }
    }

    private object Diff : DiffUtil.ItemCallback<HourlyWeather>() {
        override fun areItemsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather) =
            oldItem.hour == newItem.hour

        override fun areContentsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather) =
            oldItem == newItem
    }
}
