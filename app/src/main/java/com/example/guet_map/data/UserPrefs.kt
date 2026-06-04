package com.example.guet_map.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var points: Int
        get() = prefs.getInt(KEY_POINTS, 0)
        set(value) = prefs.edit().putInt(KEY_POINTS, value).apply()

    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var contributionCount: Int
        get() = prefs.getInt(KEY_CONTRIBUTION_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_CONTRIBUTION_COUNT, value).apply()

    fun addPoints(earned: Int) {
        points += earned
    }

    fun login(response: com.example.guet_map.model.LoginResponse) {
        isLoggedIn = true
        authToken = response.token
        nickname = response.nickname
        points = response.points
        contributionCount = response.contributionCount
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_POINTS = "points"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CONTRIBUTION_COUNT = "contribution_count"
    }
}
