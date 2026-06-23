package com.example.guet_map.module.ai.data.remote

import com.example.guet_map.BuildConfig
import com.example.guet_map.data.UserPrefs
import javax.inject.Inject
import javax.inject.Singleton

interface DeepSeekConfigProvider {
    fun getApiKey(): String?
    fun isConfigured(): Boolean = getApiKey()?.isNotBlank() == true
    fun getBaseUrl(): String = DeepSeekConstants.BASE_URL
    fun getModel(): String = DeepSeekConstants.MODEL
}

@Singleton
class DefaultDeepSeekConfigProvider @Inject constructor(
    private val userPrefs: UserPrefs
) : DeepSeekConfigProvider {
    override fun getApiKey(): String? {
        // 优先读取用户在设置页填入的 Key（不为空才用）
        val userKey = userPrefs.deepSeekApiKey.takeIf { it.isNotBlank() }
        return userKey ?: BuildConfig.DEEPSEEK_API_KEY.takeIf { it.isNotBlank() }
    }
}
