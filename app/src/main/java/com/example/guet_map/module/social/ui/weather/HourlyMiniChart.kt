package com.example.guet_map.module.social.ui.weather

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.guet_map.R
import com.example.guet_map.module.social.data.model.HourlyWeather
import kotlin.math.max

/**
 * 24h 温度小折线（行业标准：彩云/墨迹/Apple）。
 * 显示在小时列表顶部，作为"全天趋势"指示。
 */
class HourlyMiniChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<HourlyWeather> = emptyList()
    private var currentHour: Int = -1

    private val linePath = Path()
    private val fillPath = Path()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val currentDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val currentDotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val padding = 24f
    private val chartHeight = 56f

    fun setData(hourly: List<HourlyWeather>, currentHour: Int) {
        this.data = hourly.filter { it.dayIndex == 0 }.takeIf { it.isNotEmpty() } ?: hourly
        this.currentHour = currentHour
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val left = padding
        val right = w - padding
        val top = (h - chartHeight) / 2
        val bottom = top + chartHeight

        val temps = data.map { it.temperature }
        val minT = temps.min().toFloat() - 1f
        val maxT = temps.max().toFloat() + 1f
        val range = max(1f, maxT - minT)

        val stepX = (right - left) / max(1, data.size - 1)

        // 计算点位
        val points = data.mapIndexed { i, item ->
            val x = left + stepX * i
            val y = bottom - ((item.temperature - minT) / range) * chartHeight
            Triple(x, y, item.hour)
        }

        // 构建曲线（贝塞尔）
        linePath.reset()
        fillPath.reset()
        points.forEachIndexed { i, (x, y, _) ->
            if (i == 0) {
                linePath.moveTo(x, y)
            } else {
                val (px, py, _) = points[i - 1]
                val midX = (px + x) / 2
                linePath.cubicTo(midX, py, midX, y, x, y)
            }
        }

        // 填充路径
        fillPath.addPath(linePath)
        fillPath.lineTo(points.last().first, bottom)
        fillPath.lineTo(points.first().first, bottom)
        fillPath.close()

        // 渐变填充（蓝→红）
        val primary = ContextCompat.getColor(context, R.color.primary)
        val error = ContextCompat.getColor(context, R.color.error)
        fillPaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            (error and 0x33FFFFFF.toInt()),
            (primary and 0x10FFFFFF.toInt()),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // 折线
        linePaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            error, primary,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(linePath, linePaint)

        // 数据点
        points.forEach { (x, y, hour) ->
            if (hour == currentHour) {
                // 当前小时：大圆 + 白边
                currentDotPaint.color = ContextCompat.getColor(context, R.color.primary)
                currentDotStroke.color = ContextCompat.getColor(context, android.R.color.white)
                canvas.drawCircle(x, y, 7f, currentDotPaint)
                canvas.drawCircle(x, y, 7f, currentDotStroke)
            } else {
                dotPaint.color = primary
                canvas.drawCircle(x, y, 3.5f, dotPaint)
            }
        }
    }
}
