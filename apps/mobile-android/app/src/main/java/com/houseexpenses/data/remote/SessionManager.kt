package com.houseexpenses.data.remote

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("house_expenses_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACTIVE_HOUSEHOLD_ID = "active_household_id"
        private const val KEY_HOUSEHOLD_NAME = "household_name"
        private const val KEY_HOUSEHOLD_INVITE_CODE = "household_invite_code"
        private const val KEY_HOUSEHOLD_BILLING_CYCLE_DAY = "household_billing_cycle_day"
        private const val KEY_IS_HOUSEHOLD_ADMIN = "is_household_admin"
        private const val KEY_IS_DARK_MODE = "is_dark_mode"

        // Default: 10.0.2.2 for Android Studio Emulator
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080/api/v1/"
        const val USB_LOCAL_SERVER_URL = "http://localhost:8080/api/v1/"
        const val WIFI_LAN_SERVER_URL = "http://192.168.1.2:8080/api/v1/"
    }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "mobile-local-user") ?: "mobile-local-user"
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Usuario Móvil") ?: "Usuario Móvil"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString(KEY_SERVER_URL, formatted).apply()
        }

    var activeHouseholdId: String?
        get() = prefs.getString(KEY_ACTIVE_HOUSEHOLD_ID, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_HOUSEHOLD_ID, value).apply()

    var householdName: String?
        get() = prefs.getString(KEY_HOUSEHOLD_NAME, null)
        set(value) = prefs.edit().putString(KEY_HOUSEHOLD_NAME, value).apply()

    var householdInviteCode: String?
        get() = prefs.getString(KEY_HOUSEHOLD_INVITE_CODE, null)
        set(value) = prefs.edit().putString(KEY_HOUSEHOLD_INVITE_CODE, value).apply()

    var householdBillingCycleDay: Int
        get() = prefs.getInt(KEY_HOUSEHOLD_BILLING_CYCLE_DAY, 1)
        set(value) = prefs.edit().putInt(KEY_HOUSEHOLD_BILLING_CYCLE_DAY, value).apply()

    var isHouseholdAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_HOUSEHOLD_ADMIN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_HOUSEHOLD_ADMIN, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    val isLoggedIn: Boolean
        get() = !authToken.isNullOrBlank()

    fun clearHousehold() {
        prefs.edit()
            .remove(KEY_ACTIVE_HOUSEHOLD_ID)
            .remove(KEY_HOUSEHOLD_NAME)
            .remove(KEY_HOUSEHOLD_INVITE_CODE)
            .remove(KEY_HOUSEHOLD_BILLING_CYCLE_DAY)
            .remove(KEY_IS_HOUSEHOLD_ADMIN)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_ACTIVE_HOUSEHOLD_ID)
            .remove(KEY_HOUSEHOLD_NAME)
            .remove(KEY_HOUSEHOLD_INVITE_CODE)
            .remove(KEY_HOUSEHOLD_BILLING_CYCLE_DAY)
            .remove(KEY_IS_HOUSEHOLD_ADMIN)
            .apply()
    }
}
