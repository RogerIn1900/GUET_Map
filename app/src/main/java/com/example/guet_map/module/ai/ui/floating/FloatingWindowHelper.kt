package com.example.guet_map.module.ai.ui.floating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * 悬浮窗权限管理
 */
object FloatingWindowHelper {

    /**
     * 检查是否有悬浮窗权限
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(context: Context) {
        if (!canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }

    /**
     * 启动悬浮窗服务
     * 需要先检查权限
     */
    fun startFloatingWindow(context: Context, requestPermissionIfNeeded: Boolean = true): Boolean {
        if (!canDrawOverlays(context)) {
            if (requestPermissionIfNeeded) {
                requestOverlayPermission(context)
                Toast.makeText(
                    context,
                    "请授予悬浮窗权限后重试",
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        }

        FloatingWindowService.start(context)
        return true
    }

    /**
     * 停止悬浮窗服务
     */
    fun stopFloatingWindow(context: Context) {
        FloatingWindowService.stop(context)
    }

    /**
     * 切换悬浮窗显示状态
     */
    fun toggleFloatingWindow(context: Context) {
        if (canDrawOverlays(context)) {
            // 发送广播或使用其他方式切换状态
            // 这里简化处理，直接启动或停止
            startFloatingWindow(context, requestPermissionIfNeeded = false)
        }
    }
}
