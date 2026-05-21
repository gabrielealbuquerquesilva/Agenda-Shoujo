package com.example.data.network

import android.content.Context
import android.content.SharedPreferences

class OAuthTokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("oauth_prefs", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String?, email: String?) {
        prefs.edit().apply {
            putString("access_token", accessToken)
            if (refreshToken != null) {
                putString("refresh_token", refreshToken)
            }
            if (email != null) {
                putString("user_email", email)
            }
            putLong("saved_at", System.currentTimeMillis())
            apply()
        }
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun isAuthorized(): Boolean {
        return getAccessToken() != null
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
