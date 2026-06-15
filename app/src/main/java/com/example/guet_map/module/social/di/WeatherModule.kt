package com.example.guet_map.module.social.di

import com.example.guet_map.module.social.data.remote.OpenMeteoApiService
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
 */
@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    @Provides
    @Singleton
    @Named("openmeteo")
    fun provideOpenMeteoRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OpenMeteoApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(@Named("openmeteo") retrofit: Retrofit): OpenMeteoApiService {
        return retrofit.create(OpenMeteoApiService::class.java)
    }
}
