package com.example.guet_map.network

import com.example.guet_map.data.UserPrefs
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val userPrefs: UserPrefs
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = userPrefs.authToken
        val request = if (token.isNotBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
