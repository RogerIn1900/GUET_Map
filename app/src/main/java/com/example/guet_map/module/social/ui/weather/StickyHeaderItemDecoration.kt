package com.example.guet_map.module.social.ui.weather

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 吸顶表头 ItemDecoration。
 * 整个列表共用一个表头（由 [headerResId] 决定），
 * 列表向上滚时，表头先跟随 firstChild 一起滚；
 * 当 firstChild 顶部到达表头位置时，表头钉在 rv 顶部（吸顶）。
 *
 * 使用：val deco = StickyHeaderItemDecoration(R.layout.view_header_hourly) { view ->
 *     // 可选：在 view 创建时绑定一次数据（多数 header 是静态的）
 * }
 * rv.addItemDecoration(deco)
 */
class StickyHeaderItemDecoration(
    private val headerResId: Int,
    private val onBindHeader: (View) -> Unit = {}
) : RecyclerView.ItemDecoration() {

    private var headerView: View? = null
    private var headerHeight = 0

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.childCount == 0) return
        if (parent.adapter == null || parent.adapter!!.itemCount == 0) return

        if (headerView == null) {
            val hv = LayoutInflater.from(parent.context)
                .inflate(headerResId, parent, false)
            onBindHeader(hv)
            measureView(parent, hv)
            headerHeight = hv.measuredHeight
            headerView = hv
        }

        val firstChild = parent.getChildAt(0) ?: return
        val firstPos = parent.getChildAdapterPosition(firstChild)
        if (firstPos == RecyclerView.NO_POSITION) return

        val translationY = if (firstChild.top < headerHeight) {
            0f
        } else {
            (firstChild.top - headerHeight).toFloat()
        }

        c.save()
        c.translate(0f, translationY)
        headerView!!.draw(c)
        c.restore()
    }

    private fun measureView(parent: RecyclerView, view: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
}
