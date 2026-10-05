package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rider_driver_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        set(value) = prefs.edit().putString("user_id", value).apply()

    var phone: String
        get() = prefs.getString("phone", "") ?: ""
        set(value) = prefs.edit().putString("phone", value).apply()

    var fullName: String
        get() = prefs.getString("full_name", "") ?: ""
        set(value) = prefs.edit().putString("full_name", value).apply()

    var isDemoMode: Boolean
        get() = prefs.getBoolean("is_demo_mode", false)
        set(value) = prefs.edit().putBoolean("is_demo_mode", value).apply()

    val deviceId: String
        get() {
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = "android-" + UUID.randomUUID().toString().take(12)
                prefs.edit().putString("device_id", id).apply()
            }
            return id
        }

    val isLoggedIn: Boolean
        get() = userId.isNotEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
