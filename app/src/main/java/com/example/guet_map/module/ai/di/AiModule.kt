package com.example.guet_map.module.ai.di

import com.example.guet_map.module.ai.data.remote.DeepSeekApi
import com.example.guet_map.module.ai.data.remote.DeepSeekConfigProvider
import com.example.guet_map.module.ai.data.remote.DeepSeekConstants
import com.example.guet_map.module.ai.data.remote.DefaultDeepSeekConfigProvider
import com.example.guet_map.module.ai.di.DeepSeekClient
import com.example.guet_map.module.ai.domain.service.AiService
import com.example.guet_map.module.ai.domain.service.AiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * AI 模块依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindAiService(impl: AiServiceImpl): AiService

    @Binds
    abstract fun bindDeepSeekConfigProvider(
        impl: DefaultDeepSeekConfigProvider
    ): DeepSeekConfigProvider

    companion object {
        @Provides
        @DeepSeekClient
        @Singleton
        fun provideDeepSeekOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(DeepSeekConstants.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DeepSeekConstants.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(DeepSeekConstants.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideDeepSeekApi(@DeepSeekClient okHttpClient: OkHttpClient): DeepSeekApi {
            return Retrofit.Builder()
                .baseUrl(DeepSeekConstants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeepSeekApi::class.java)
        }
    }
}
