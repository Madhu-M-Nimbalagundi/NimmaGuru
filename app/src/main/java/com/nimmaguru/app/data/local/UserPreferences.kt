package com.nimmaguru.app.data.local

import android.content.Context
import com.nimmaguru.app.domain.model.UserRole

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveRole(uid: String, role: UserRole) {
        preferences.edit().putString(roleKey(uid), role.name).apply()
    }

    fun getRole(uid: String): UserRole? {
        return UserRole.fromStorage(preferences.getString(roleKey(uid), null))
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setThemeMode(mode: String) {
        preferences.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(): String {
        return preferences.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
    }

    private fun roleKey(uid: String): String = "$KEY_ROLE_PREFIX$uid"

    private companion object {
        const val PREFS_NAME = "nimma_guru_user_preferences"
        const val KEY_ROLE_PREFIX = "selected_role_"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val THEME_LIGHT = "light"
    }
}
