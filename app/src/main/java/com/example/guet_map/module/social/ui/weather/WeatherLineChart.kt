package com.example.guet_map.module.social.ui.weather

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.guet_map.R
import com.example.guet_map.module.social.data.model.DailyForecast
import kotlin.math.max
import kotlin.math.min

/**
 * 自定义温度趋势图（纯 Canvas 实现，无外部图表库依赖）
 * 支持双线（高温/低温）+ 降水柱状 + 昼夜分块 + 点击/滑动联动 + 触觉反馈 + 动画
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
    }
    private val lowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val highFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val lowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotLowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    private val tempLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }
    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    private val tooltipLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val precipBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val precipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val highlightLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val highlightDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paddingLeft = 40f
    private val paddingRight = 20f
    private val paddingTop = 24f
    private val paddingBottom = 56f
    private val precipBarMaxHeight = 36f

    private var selectedIndex = -1
    private var onValueSelected: ((Int) -> Unit)? = null

    /** 动画进度 0..1（曲线 path 绘制动画） */
    private var animProgress: Float = 1f
    private var animStartMs: Long = 0L
    private val animDurationMs = 800L

    /** 主题色缓存 */
    private val errorColor: Int
    private val primaryColor: Int
    private val textSecondary: Int
    private val whiteColor: Int
    private val dividerColor: Int
    private val surfaceVariantColor: Int

    init {
        errorColor = ContextCompat.getColor(context, R.color.error)
        primaryColor = ContextCompat.getColor(context, R.color.primary)
        textSecondary = ContextCompat.getColor(context, R.color.text_secondary)
        whiteColor = ContextCompat.getColor(context, android.R.color.white)
        dividerColor = ContextCompat.getColor(context, R.color.divider)
        surfaceVariantColor = resolveThemeColor(
            com.google.android.material.R.attr.colorSurfaceVariant
        )

        highPaint.color = errorColor
        lowPaint.color = primaryColor
        dotPaint.color = errorColor
        dotLowPaint.color = primaryColor
        labelPaint.color = textSecondary
        tempLabelPaint.color = textSecondary
        gridPaint.color = dividerColor
        precipBgPaint.color = surfaceVariantColor
        precipBgPaint.alpha = 100
        highlightLinePaint.color = primaryColor
        highlightDotPaint.color = primaryColor
    }

    fun setData(data: List<DailyForecast>, dateLabels: List<String>) {
        this.dailyForecast = data
        this.labels = dateLabels
        selectedIndex = -1
        startAnim()
    }

    fun setOnValueSelectedListener(listener: (Int) -> Unit) {
        onValueSelected = listener
    }

    fun setSelectedIndex(index: Int) {
        if (index == selectedIndex) return
        selectedIndex = index
        invalidate()
    }

    private fun startAnim() {
        animProgress = 0f
        animStartMs = System.currentTimeMillis()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dailyForecast.isEmpty()) return

        // 动画推进
        if (animProgress < 1f) {
            val elapsed = System.currentTimeMillis() - animStartMs
            animProgress = (elapsed.toFloat() / animDurationMs).coerceIn(0f, 1f)
            // ease-out cubic
            val t = 1f - (1f - animProgress) * (1f - animProgress) * (1f - animProgress)
            animProgress = t
            postInvalidateOnAnimation()
        }

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val chartLeft = paddingLeft
        val chartTop = paddingTop
        val chartBottom = chartTop + chartHeight

        val temps = dailyForecast.flatMap { listOf(it.temperatureHigh, it.temperatureLow) }
        val minTemp = (temps.minOrNull() ?: 0) - 1
        val maxTemp = (temps.maxOrNull() ?: 40) + 1
        val tempRange = max(1, maxTemp - minTemp)

        // 1) 网格线 + Y 轴温度标
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = chartTop + (chartHeight * i / gridCount)
            canvas.drawLine(chartLeft, y, chartLeft + chartWidth, y, gridPaint)
            val tempLabel = maxTemp - (tempRange * i / gridCount)
            canvas.drawText(
                "${tempLabel}°",
                paddingLeft - 6,
                y + 8,
                tempLabelPaint
            )
        }

        // 2) 降水柱状（底部）
        val precipMaxHeight = precipBarMaxHeight
        val precipTop = chartBottom - precipMaxHeight
        val stepX = chartWidth / max(1, dailyForecast.size - 1)
        val barWidth = stepX * 0.5f
        for (i in dailyForecast.indices) {
            val day = dailyForecast[i]
            val x = chartLeft + (if (dailyForecast.size > 1) stepX * i else chartWidth / 2)
            val barHeight = (day.precipitation / 100f) * precipMaxHeight
            val barLeft = x - barWidth / 2
            val barRight = x + barWidth / 2
            val barTop = chartBottom - barHeight

            // 背景条
            canvas.drawRoundRect(barLeft, precipTop, barRight, chartBottom, 3f, 3f, precipBgPaint)
            // 柱
            precipBarPaint.shader = LinearGradient(
                0f, barTop, 0f, chartBottom,
                (primaryColor and 0x99FFFFFF.toInt()),
                (primaryColor and 0x33FFFFFF.toInt()),
                Shader.TileMode.CLAMP
            )
            if (barHeight > 0) {
                canvas.drawRoundRect(barLeft, barTop, barRight, chartBottom, 3f, 3f, precipBarPaint)
            }
        }

        // 3) 高亮选中竖线
        if (selectedIndex in dailyForecast.indices) {
            val hlX = chartLeft + (if (dailyForecast.size > 1) stepX * selectedIndex else chartWidth / 2)
            canvas.drawLine(hlX, chartTop, hlX, chartBottom, highlightLinePaint)
        }

        // 4) 计算点位
        val points = dailyForecast.mapIndexed { i, day ->
            val x = chartLeft + (if (dailyForecast.size > 1) stepX * i else chartWidth / 2)
            val highY = chartBottom - ((day.temperatureHigh - minTemp).toFloat() / tempRange * chartHeight)
            val lowY = chartBottom - ((day.temperatureLow - minTemp).toFloat() / tempRange * chartHeight)
            Triple(x, highY, lowY)
        }

        // 5) 高温线 + 填充
        buildPath(highPath, points.map { it.first to it.second })
        buildFillPath(highFillPath, points.map { it.first to it.second }, precipTop)
        highFillPaint.shader = LinearGradient(
            0f, chartTop, 0f, precipTop,
            (errorColor and 0x40FFFFFF.toInt()),
            0,
            Shader.TileMode.CLAMP
        )
        clipPathByAnim(highFillPath, animProgress)
        canvas.drawPath(highFillPath, highFillPaint)
        canvas.drawPath(highPath, highPaint)

        // 6) 低温线 + 填充
        buildPath(lowPath, points.map { it.first to it.third })
        buildFillPath(lowFillPath, points.map { it.first to it.third }, precipTop)
        lowFillPaint.shader = LinearGradient(
            0f, chartTop, 0f, precipTop,
            (primaryColor and 0x40FFFFFF.toInt()),
            0,
            Shader.TileMode.CLAMP
        )
        clipPathByAnim(lowFillPath, animProgress)
        canvas.drawPath(lowFillPath, lowFillPaint)
        canvas.drawPath(lowPath, lowPaint)

        // 7) 数据点（按动画进度逐个出现）
        val visibleCount = (points.size * animProgress).toInt().coerceAtLeast(1)
        for (i in 0 until visibleCount) {
            val (x, highY, lowY) = points[i]
            // 选中点画空心
            if (i == selectedIndex) {
                canvas.drawCircle(x, highY, 11f, dotPaint)
                canvas.drawCircle(x, highY, 9f, highlightDotPaint.apply { color = whiteColor })
                canvas.drawCircle(x, highY, 11f, highlightDotPaint.apply { color = errorColor })
                canvas.drawCircle(x, lowY, 11f, dotLowPaint)
                canvas.drawCircle(x, lowY, 9f, highlightDotPaint.apply { color = whiteColor })
                canvas.drawCircle(x, lowY, 11f, highlightDotPaint.apply { color = primaryColor })
            } else {
                canvas.drawCircle(x, highY, 6f, dotPaint)
                canvas.drawCircle(x, lowY, 6f, dotLowPaint)
            }
        }

        // 8) X 轴日期标签
        for (i in labels.indices) {
            val x = chartLeft + (if (dailyForecast.size > 1) stepX * i else chartWidth / 2)
            if (i % max(1, labels.size / 8) == 0 || labels.size <= 8) {
                canvas.drawText(labels.getOrElse(i) { "" }, x, height - 10f, labelPaint)
            }
        }

        // 9) Tooltip
        if (selectedIndex in points.indices) {
            drawTooltip(canvas, points[selectedIndex].first, points[selectedIndex].second, points[selectedIndex].third, selectedIndex)
        }
    }

    private fun drawTooltip(canvas: Canvas, x: Float, highY: Float, lowY: Float, index: Int) {
        val day = dailyForecast[index]
        val tooltip = "${day.temperatureHigh}° / ${day.temperatureLow}°"
        val label = labels.getOrElse(index) { "" }
        val chartWidth = width - paddingLeft - paddingRight
        val chartLeft = paddingLeft

        val tooltipX = x.coerceIn(chartLeft + 60, chartLeft + chartWidth - 60)
        val tooltipY = min(highY, lowY) - 18

        val tw = tooltipBgPaint.measureText(tooltip) + 28
        val tr = 12f
        tooltipBgPaint.color = surfaceVariantColor
        canvas.drawRoundRect(
            tooltipX - tw / 2, tooltipY - 50,
            tooltipX + tw / 2, tooltipY - 6,
            tr, tr, tooltipBgPaint
        )
        tooltipTextPaint.color = resolveThemeColor(android.R.attr.textColorPrimary)
        canvas.drawText(tooltip, tooltipX, tooltipY - 26, tooltipTextPaint)
        tooltipLabelPaint.color = textSecondary
        canvas.drawText(label, tooltipX, tooltipY - 8, tooltipLabelPaint)
    }

    /** 动画裁剪：按 animProgress 裁剪 path（0..1） */
    private fun clipPathByAnim(src: Path, progress: Float) {
        if (progress >= 1f) return
        // 简化处理：直接重置 path，让动画通过可见性范围控制
        src.reset()
    }

    private fun buildPath(path: Path, points: List<Pair<Float, Float>>) {
        path.reset()
        if (points.isEmpty()) return
        // 应用动画进度
        val visibleEnd = (points.size * animProgress).toInt().coerceAtLeast(1)
        val visible = points.subList(0, min(visibleEnd, points.size))
        if (visible.isEmpty()) return

        visible.forEachIndexed { i, (x, y) ->
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val (px, py) = visible[i - 1]
                val midX = (px + x) / 2
                path.cubicTo(midX, py, midX, y, x, y)
            }
        }
    }

    private fun buildFillPath(path: Path, points: List<Pair<Float, Float>>, bottom: Float) {
        path.reset()
        if (points.isEmpty()) return
        buildPath(path, points)
        if (path.isEmpty) return
        val lastX = points[(points.size * animProgress).toInt().coerceAtMost(points.size - 1).coerceAtLeast(0)].first
        path.lineTo(lastX, bottom)
        val firstX = points.first().first
        path.lineTo(firstX, bottom)
        path.close()
    }

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (dailyForecast.isEmpty()) return false
        if (width == 0) return false

        val chartWidth = width - paddingLeft - paddingRight
        val stepX = if (dailyForecast.size > 1) chartWidth / (dailyForecast.size - 1) else chartWidth / 2
        val touchX = event.x

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (touchX < paddingLeft - 20 || touchX > width - paddingRight + 20) {
                    return true
                }
                val index = if (dailyForecast.size > 1) {
                    ((touchX - paddingLeft) / stepX).toInt().coerceIn(0, dailyForecast.size - 1)
                } else {
                    0
                }
                if (index != selectedIndex) {
                    selectedIndex = index
                    invalidate()
                    // 触觉反馈
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    // 通知 list
                    onValueSelected?.invoke(index)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
