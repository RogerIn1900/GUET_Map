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
import com.example.guet_map.databinding.ItemDailyForecastBinding
import com.example.guet_map.module.social.data.model.DailyForecast
import com.example.guet_map.module.social.data.model.WeatherType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyForecastAdapter : ListAdapter<DailyForecast, DailyForecastAdapter.ViewHolder>(Diff) {

    /** 高亮行（chart 联动） */
    private var highlightedPosition: Int = -1

    /** 展开行（点击展开） */
    private var expandedPosition: Int = -1

    /** 全局 min/max 温度（用于温度范围条计算） */
    private var globalMinTemp: Int = Int.MAX_VALUE
    private var globalMaxTemp: Int = Int.MIN_VALUE

    private var onItemClick: ((Int) -> Unit)? = null

    fun setOnItemClickListener(l: (Int) -> Unit) {
        onItemClick = l
    }

    fun setHighlightedPosition(pos: Int) {
        if (pos == highlightedPosition) return
        val old = highlightedPosition
        highlightedPosition = pos
        if (old >= 0) notifyItemChanged(old, PAYLOAD_HIGHLIGHT)
        if (pos >= 0) notifyItemChanged(pos, PAYLOAD_HIGHLIGHT)
    }

    fun setGlobalTempRange(min: Int, max: Int) {
        globalMinTemp = min
        globalMaxTemp = max
        notifyItemRangeChanged(0, itemCount, PAYLOAD_TEMP_BAR)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick = { pos ->
            toggleExpand(pos)
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position, highlightedPosition, expandedPosition, globalMinTemp, globalMaxTemp)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        if (payloads.contains(PAYLOAD_HIGHLIGHT)) {
            holder.updateHighlight(position == highlightedPosition)
        }
        if (payloads.contains(PAYLOAD_TEMP_BAR)) {
            holder.updateTempRange(getItem(position), globalMinTemp, globalMaxTemp)
        }
    }

    private fun toggleExpand(pos: Int) {
        val old = expandedPosition
        expandedPosition = if (old == pos) -1 else pos
        if (old >= 0) notifyItemChanged(old, PAYLOAD_EXPAND)
        notifyItemChanged(pos, PAYLOAD_EXPAND)
    }

    class ViewHolder(
        private val binding: ItemDailyForecastBinding,
        onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val onSurfaceVariantColor: Int = resolveThemeColor(
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        init {
            binding.root.setOnClickListener {
                @Suppress("DEPRECATION")
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(pos)
            }
        }

        fun bind(
            item: DailyForecast,
            position: Int,
            highlighted: Int,
            expanded: Int,
            globalMin: Int,
            globalMax: Int
        ) {
            val isToday = position == 0
            val isExpanded = position == expanded
            val isHighlighted = position == highlighted

            // 1) 日期 + 周几（"今天"/"明天"/"周三 06/16"）
            if (isToday) {
                binding.tvDate.text = "今天"
                binding.tvDate.setTypeface(null, Typeface.BOLD)
            } else {
                binding.tvDate.text = getRelativeDay(item.date)
                binding.tvDate.setTypeface(null, Typeface.NORMAL)
            }
            binding.tvWeekday.text = getWeekdayWithDate(item.date)

            // 2) 天气图标（用 dayIcon：白天/夜间区分）
            val iconRes = getWeatherIconRes(item.dayIcon)
            binding.ivWeatherIcon.setImageResource(iconRes)

            // 3) 降水
            binding.tvPrecip.text = "${item.precipitation}%"
            binding.tvPrecip.alpha = if (item.precipitation > 0) 1f else 0.4f

            // 4) 高低温 + 温度范围条
            binding.tvTempHigh.text = "${item.temperatureHigh}°"
            binding.tvTempLow.text = "${item.temperatureLow}°"
            updateTempRange(item, globalMin, globalMax)

            // 5) UV（带颜色等级）
            binding.tvUv.text = "UV${item.uvIndex}"
            binding.tvUv.setTextColor(getUvColor(item.uvIndex))

            // 6) 当日高亮 + 高亮联动
            binding.viewTodayHighlight.visibility = if (isToday) View.VISIBLE else View.GONE
            updateHighlight(isHighlighted)

            // 7) 展开详情
            binding.layoutExpandDetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
            if (isExpanded) {
                binding.tvExpandWind.text = "${item.windDirection} ${item.windLevel.ifEmpty() { "${item.windSpeed.toInt()}m/s" }}"
                binding.tvExpandHumidity.text = "${item.humidity}%"
                binding.tvExpandFeels.text = "${item.feelsLikeLow}°~${item.feelsLikeHigh}°"
                binding.tvExpandSunrise.text = formatTime(item.sunrise)
                binding.tvExpandSunset.text = formatTime(item.sunset)
                binding.tvExpandMoon.text = item.moonPhase.ifEmpty { "--" }
            }
        }

        fun updateHighlight(isHighlighted: Boolean) {
            binding.root.setBackgroundResource(
                if (isHighlighted) R.drawable.bg_row_highlight
                else android.R.color.transparent
            )
        }

        fun updateTempRange(item: DailyForecast, globalMin: Int, globalMax: Int) {
            if (globalMin >= globalMax) return
            val parent = binding.viewTempRange.parent as View
            val parentWidth = (parent.layoutParams.width.takeIf { it > 0 } ?: parent.width).toFloat()
            if (parentWidth == 0f) {
                parent.post {
                    updateTempRange(item, globalMin, globalMax)
                }
                return
            }
            val range = (globalMax - globalMin).toFloat().coerceAtLeast(1f)
            val widthRatio = ((item.temperatureHigh - item.temperatureLow).toFloat() / range)
                .coerceIn(0.05f, 1f)
            val leftRatio = ((item.temperatureLow - globalMin).toFloat() / range)
                .coerceIn(0f, 1f)
            val barWidth = (parentWidth * widthRatio).toInt()
            val barLeft = (parentWidth * leftRatio).toInt()
            val lp = binding.viewTempRange.layoutParams
            lp.width = barWidth
            binding.viewTempRange.layoutParams = lp
            binding.viewTempRange.translationX = barLeft.toFloat()
        }

        private fun getRelativeDay(date: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val d = input.parse(date) ?: return date
                val today = Calendar.getInstance()
                val target = Calendar.getInstance().apply { time = d }
                val days = (target.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)
                when (days.toInt()) {
                    0 -> "今天"
                    1 -> "明天"
                    2 -> "后天"
                    else -> {
                        val out = SimpleDateFormat("MM/dd", Locale.CHINA)
                        out.format(d)
                    }
                }
            } catch (_: Exception) {
                date
            }
        }

        private fun getWeekdayWithDate(date: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val d = input.parse(date) ?: return ""
                val cal = Calendar.getInstance().apply { time = d }
                val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                "${weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]} ${formatShortDate(date)}"
            } catch (_: Exception) {
                ""
            }
        }

        private fun formatShortDate(date: String): String = try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = SimpleDateFormat("MM/dd", Locale.CHINA)
            output.format(input.parse(date)!!)
        } catch (_: Exception) {
            ""
        }

        private fun formatTime(ts: Long): String {
            if (ts == 0L) return "--:--"
            val sdf = SimpleDateFormat("HH:mm", Locale.CHINA)
            return sdf.format(ts)
        }

        private fun getUvColor(uv: Int): Int = when {
            uv <= 2 -> 0xFF66BB6A.toInt()   // 弱 绿
            uv <= 5 -> 0xFFFFA726.toInt()   // 中 橙
            uv <= 7 -> 0xFFEF5350.toInt()   // 强 红
            uv <= 10 -> 0xFFAB47BC.toInt()  // 很强 紫
            else -> 0xFF8D6E63.toInt()      // 极强 棕
        }

        private fun resolveThemeColor(attr: Int): Int {
            val tv = TypedValue()
            val ctx = itemView.context
            ctx.theme.resolveAttribute(attr, tv, true)
            return tv.data
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

    private object Diff : DiffUtil.ItemCallback<DailyForecast>() {
        override fun areItemsTheSame(oldItem: DailyForecast, newItem: DailyForecast) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: DailyForecast, newItem: DailyForecast) =
            oldItem == newItem
    }

    companion object {
        const val PAYLOAD_HIGHLIGHT = "highlight"
        const val PAYLOAD_EXPAND = "expand"
        const val PAYLOAD_TEMP_BAR = "temp_bar"
    }
}
