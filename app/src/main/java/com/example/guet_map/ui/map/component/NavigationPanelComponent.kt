package com.example.guet_map.ui.map.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.guet_map.R
import com.example.guet_map.databinding.LayoutNavigationPanelBinding
import com.example.guet_map.model.Location
import com.example.guet_map.model.WalkRouteInfo

/**
 * 导航面板组件
 * 负责：导航面板显示、路线信息展示、外部导航跳转、循迹状态更新
 */
class NavigationPanelComponent(
    private val context: Context,
    private val parent: ViewGroup
) {
    private var binding: LayoutNavigationPanelBinding? = null
    private var currentRoute: WalkRouteInfo? = null
    private var currentTarget: Location? = null

    var onCloseNavigation: (() -> Unit)? = null
    var onStartNavigation: ((Location) -> Unit)? = null
    var onReroute: (() -> Unit)? = null

    init {
        inflate()
    }

    private fun inflate() {
        val navPanelView = LayoutNavigationPanelBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        binding = navPanelView
        parent.addView(navPanelView.root)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding?.btnCloseNavigation?.setOnClickListener {
            hide()
            onCloseNavigation?.invoke()
        }
    }

    /**
     * 显示导航面板
     */
    fun show(target: Location, route: WalkRouteInfo? = null) {
        currentTarget = target
        currentRoute = route

        binding?.apply {
            cardNavigationPanel.visibility = View.VISIBLE
            tvNextStep.text = context.getString(R.string.route_planning)
        }
    }

    /**
     * 更新路线信息
     */
    fun updateRoute(route: WalkRouteInfo) {
        currentRoute = route
        binding?.apply {
            val distanceText = if (route.distanceMeters >= 1000) {
                String.format("%.1f", route.distanceMeters / 1000f)
            } else {
                route.distanceMeters.toString()
            }
            val unitText = if (route.distanceMeters >= 1000) "公里" else "米"

            tvRouteDistance.text = distanceText
            tvRouteDuration.text = "${(route.durationSeconds / 60).coerceAtLeast(1)}"
            tvNextStep.text = "预计步行 ${(route.durationSeconds / 60).coerceAtLeast(1)} 分钟"
        }
    }

    /**
     * 实时更新循迹数据（GPS 位置更新时调用）
     */
    fun updateTrackingProgress(remainingDistance: Int, remainingTime: Int, stepHint: String) {
        binding?.apply {
            val distText = if (remainingDistance >= 1000) {
                String.format("%.1f公里", remainingDistance / 1000f)
            } else {
                "${remainingDistance}米"
            }
            tvRouteDistance.text = remainingDistance.toString()
            tvNextStep.text = stepHint
        }
    }

    /**
     * 显示偏航提示
     */
    fun showDeviationAlert() {
        binding?.apply {
            tvNextStep.text = "您已偏离路线，正在重新规划…"
        }
    }

    /**
     * 到达目的地
     */
    fun showArrival() {
        binding?.apply {
            tvNextStep.text = "已到达目的地！"
            tvRouteDistance.text = "0"
            tvRouteDuration.text = "到达"
        }
    }

    /**
     * 显示加载中
     */
    fun showLoading() {
        binding?.apply {
            tvNextStep.text = context.getString(R.string.route_planning)
        }
    }

    /**
     * 隐藏导航面板
     */
    fun hide() {
        binding?.cardNavigationPanel?.visibility = View.GONE
        currentRoute = null
        currentTarget = null
    }

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean =
        binding?.cardNavigationPanel?.visibility == View.VISIBLE

    /**
     * 打开外部导航（高德地图）
     */
    fun openExternalNavigation(target: Location) {
        val uriBuilder = StringBuilder("androidamap://route/plan/?")
        uriBuilder.append("dlat=${target.latitude}&dlon=${target.longitude}")
        uriBuilder.append("&dname=${Uri.encode(target.name)}")
        uriBuilder.append("&dev=0&t=2")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
            setPackage("com.autonavi.minimap")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 高德未安装，尝试通用 geo 协议
            openGenericMap(target)
        }
    }

    private fun openGenericMap(target: Location) {
        val geoUri = Uri.parse(
            "geo:${target.latitude},${target.longitude}?q=${Uri.encode(target.name)}"
        )
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)

        if (geoIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(geoIntent, context.getString(R.string.nav_amap_app)))
        } else {
            // 最后备选：复制坐标
            copyToClipboard(target)
        }
    }

    private fun copyToClipboard(target: Location) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(
            "坐标",
            "${target.name}: ${target.latitude}, ${target.longitude}"
        )
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(
            context,
            "未找到导航应用，坐标已复制",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    /**
     * 释放资源
     */
    fun destroy() {
        parent.removeView(binding?.root)
        binding = null
    }
}
