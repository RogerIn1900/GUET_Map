package com.example.guet_map.module.social.di

import com.example.guet_map.module.social.data.remote.AmapWeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Weather 模块依赖注入配置
 *
 * 使用高德 Web 天气 API（国内直连稳定），与 Android 端 AMap SDK 共用同一个 Key。
 */
@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    @Provides
    @Singleton
    @Named("amap_weather")
    fun provideAmapWeatherRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AmapWeatherApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAmapWeatherApiService(
        @Named("amap_weather") retrofit: Retrofit
    ): AmapWeatherApiService {
        return retrofit.create(AmapWeatherApiService::class.java)
    }
}
