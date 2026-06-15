package com.example.guet_map.module.ai.data.remote

import com.example.guet_map.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

interface DeepSeekConfigProvider {
    fun getApiKey(): String?
    fun getBaseUrl(): String = DeepSeekConstants.BASE_URL
    fun getModel(): String = DeepSeekConstants.MODEL
}

@Singleton
class DefaultDeepSeekConfigProvider @Inject constructor() : DeepSeekConfigProvider {
    override fun getApiKey(): String? {
        return BuildConfig.DEEPSEEK_API_KEY.takeIf { it.isNotBlank() }
    }
}
