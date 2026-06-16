package com.example.guet_map.module.social.di

import com.example.guet_map.module.social.data.remote.OpenMeteoApiService
import com.example.guet_map.module.social.data.remote.OpenMeteoGeocodingService
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
 * 天气模块依赖注入配置
 *
 * 使用 Open-Meteo API（免费，无需 key）：
 * - https://api.open-meteo.com/                 天气预报
 * - https://geocoding-api.open-meteo.com/      地名→经纬度
 */
@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    @Provides
    @Singleton
    @Named("open_meteo_forecast")
    fun provideOpenMeteoForecastRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OpenMeteoApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("open_meteo_geocoding")
    fun provideOpenMeteoGeocodingRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OpenMeteoApiService.GEOCODING_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(
        @Named("open_meteo_forecast") retrofit: Retrofit
    ): OpenMeteoApiService {
        return retrofit.create(OpenMeteoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenMeteoGeocodingService(
        @Named("open_meteo_geocoding") retrofit: Retrofit
    ): OpenMeteoGeocodingService {
        return retrofit.create(OpenMeteoGeocodingService::class.java)
    }
}
