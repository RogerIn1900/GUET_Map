package com.example.guet_map.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.guet_map.R

object LocalNotificationHelper {

    const val CHANNEL_ID = "guet_map_events"
    private var nextId = 2000

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GUET Map 通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "审核结果、积分变动与系统公告"
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = nextId++
    ) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            ?.notify(notificationId, notification)
    }
}
