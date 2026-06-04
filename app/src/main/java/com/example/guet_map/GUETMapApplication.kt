package com.example.guet_map

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.example.guet_map.util.LocalNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GUETMapApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            val prefs = getSharedPreferences("map_privacy", MODE_PRIVATE)
            val agreed = prefs.getBoolean("privacy_agreed", false)

            MapsInitializer.updatePrivacyShow(this, true, true)
            MapsInitializer.updatePrivacyAgree(this, agreed)

            ServiceSettings.updatePrivacyShow(this, true, true)
            ServiceSettings.updatePrivacyAgree(this, agreed)
        } catch (_: Exception) {
            // 隐私接口在部分版本上可能不可用，不阻塞启动
        }
        LocalNotificationHelper.ensureChannel(this)
    }
}
