package com.example.guet_map.module.social.ui.weather

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.guet_map.R
import com.example.guet_map.module.social.data.model.DailyForecast
import kotlin.math.max
import kotlin.math.min

/**
 * 自定义温度趋势图（纯 Canvas 实现，无外部图表库依赖）
 * 支持双线（高温/低温）+ 点击显示数值
 */
class WeatherLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dailyForecast: List<DailyForecast> = emptyList()
    private var labels: List<String> = emptyList()

    private val highPath = Path()
    private val lowPath = Path()
    private val highFillPath = Path()
    private val lowFillPath = Path()

    private val highPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.error)
    }

    private val lowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val highFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val lowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.error)
    }

    private val dotLowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.text_secondary)
    }

    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.text_primary)
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.white)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = ContextCompat.getColor(context, R.color.divider)
    }

    private val paddingLeft = 40f
    private val paddingRight = 20f
    private val paddingTop = 20f
    private val paddingBottom = 60f

    private var selectedIndex = -1
    private var onValueSelected: ((Int, Int, Int) -> Unit)? = null

    fun setData(data: List<DailyForecast>, dateLabels: List<String>) {
        this.dailyForecast = data
        this.labels = dateLabels
        selectedIndex = -1
        invalidate()
    }

    fun setOnValueSelectedListener(listener: (Int, Int, Int) -> Unit) {
        onValueSelected = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dailyForecast.isEmpty()) return

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val chartLeft = paddingLeft
        val chartTop = paddingTop
        val chartBottom = chartTop + chartHeight

        val temps = dailyForecast.flatMap { listOf(it.temperatureHigh, it.temperatureLow) }
        val minTemp = (temps.minOrNull() ?: 0) - 2
        val maxTemp = (temps.maxOrNull() ?: 40) + 2
        val tempRange = maxTemp - minTemp

        // 绘制水平网格线
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = chartTop + (chartHeight * i / gridCount)
            canvas.drawLine(chartLeft, y, chartLeft + chartWidth, y, gridPaint)
            val tempLabel = maxTemp - (tempRange * i / gridCount)
            canvas.drawText(
                "${tempLabel}°",
                paddingLeft - 8,
                y + 4,
                labelPaint.apply { textAlign = Paint.Align.RIGHT }
            )
        }

        // 绘制日期标签
        val stepX = chartWidth / max(1, dailyForecast.size - 1)
        for (i in labels.indices) {
            val x = chartLeft + (if (dailyForecast.size > 1) stepX * i else chartWidth / 2)
            if (i % max(1, labels.size / 8) == 0 || labels.size <= 8) {
                canvas.drawText(labels.getOrElse(i) { "" }, x, height - 8f, labelPaint)
            }
        }

        // 计算点位
        val points = dailyForecast.mapIndexed { i, day ->
            val x = chartLeft + (if (dailyForecast.size > 1) stepX * i else chartWidth / 2)
            val highY = chartBottom - ((day.temperatureHigh - minTemp).toFloat() / tempRange * chartHeight)
            val lowY = chartBottom - ((day.temperatureLow - minTemp).toFloat() / tempRange * chartHeight)
            Pair(x, Pair(highY, lowY))
        }

        // 绘制高温线
        buildPath(highPath, points, isHigh = true)
        buildFillPath(highFillPath, points, chartBottom, isHigh = true)
        canvas.drawPath(highFillPath, highFillPaint.apply {
            shader = LinearGradient(
                0f, chartTop, 0f, chartBottom,
                ContextCompat.getColor(context, R.color.error) and 0x40FFFFFF,
                0,
                Shader.TileMode.CLAMP
            )
        })
        canvas.drawPath(highPath, highPaint)

        // 绘制低温线
        buildPath(lowPath, points, isHigh = false)
        buildFillPath(lowFillPath, points, chartBottom, isHigh = false)
        canvas.drawPath(lowFillPath, lowFillPaint.apply {
            shader = LinearGradient(
                0f, chartTop, 0f, chartBottom,
                ContextCompat.getColor(context, R.color.primary) and 0x40FFFFFF,
                0,
                Shader.TileMode.CLAMP
            )
        })
        canvas.drawPath(lowPath, lowPaint)

        // 绘制数据点
        points.forEach { (x, y) ->
            canvas.drawCircle(x, y.first, 6f, dotPaint)
            canvas.drawCircle(x, y.second, 6f, dotLowPaint)
        }

        // 高亮选中点
        if (selectedIndex in points.indices) {
            val (x, y) = points[selectedIndex]
            canvas.drawCircle(x, y.first, 12f, dotPaint.apply { alpha = 80 })
            canvas.drawCircle(x, y.second, 12f, dotLowPaint.apply { alpha = 80 })

            // 绘制 tooltip
            val day = dailyForecast[selectedIndex]
            val tooltip = "${day.temperatureHigh}° / ${day.temperatureLow}°"
            val label = labels.getOrElse(selectedIndex) { "" }
            val tooltipX = x.coerceIn(chartLeft + 40, chartLeft + chartWidth - 40)
            val tooltipY = min(y.first, y.second) - 16

            val tw = tooltipPaint.measureText(tooltip)
            val tr = 12f
            canvas.drawRoundRect(
                tooltipX - tw / 2 - 16, tooltipY - 44,
                tooltipX + tw / 2 + 16, tooltipY - 4,
                tr, tr, tooltipPaint
            )
            canvas.drawText(tooltip, tooltipX, tooltipY - 20, tooltipTextPaint)
            canvas.drawText(label, tooltipX, tooltipY - 2, labelPaint.apply {
                textAlign = Paint.Align.CENTER
                textSize = 22f
            })
        }
    }

    private fun buildPath(path: Path, points: List<Pair<Float, Pair<Float, Float>>>, isHigh: Boolean) {
        path.reset()
        if (points.isEmpty()) return
        points.forEachIndexed { i, (x, y) ->
            val py = if (isHigh) y.first else y.second
            if (i == 0) {
                path.moveTo(x, py)
            } else {
                val (px, py2) = points[i - 1]
                val prevY = if (isHigh) py2.first else py2.second
                val midX = (px + x) / 2
                path.cubicTo(midX, prevY, midX, py, x, py)
            }
        }
    }

    private fun buildFillPath(path: Path, points: List<Pair<Float, Pair<Float, Float>>>, bottom: Float, isHigh: Boolean) {
        path.reset()
        if (points.isEmpty()) return
        buildPath(path, points, isHigh)
        val lastX = points.last().first
        path.lineTo(lastX, bottom)
        val firstX = points.first().first
        path.lineTo(firstX, bottom)
        path.close()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (dailyForecast.isEmpty()) return false

        val chartWidth = width - paddingLeft - paddingRight
        val stepX = if (dailyForecast.size > 1) chartWidth / (dailyForecast.size - 1) else chartWidth / 2
        val touchX = event.x

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val index = if (dailyForecast.size > 1) {
                    ((touchX - paddingLeft) / stepX).toInt().coerceIn(0, dailyForecast.size - 1)
                } else {
                    0
                }
                if (index != selectedIndex) {
                    selectedIndex = index
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                selectedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
