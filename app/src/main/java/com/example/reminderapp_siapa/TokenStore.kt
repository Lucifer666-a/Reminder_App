package com.example.reminderapp_siapa

import android.content.Context
import android.content.SharedPreferences

/**
 * Tempat menyimpan "tiket" (token) di HP.
 * Dipakai oleh ApiClient. Panggil TokenStore.save(...) setelah login berhasil,
 * dan TokenStore.clear() saat logout.
 */
object TokenStore {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext
                .getSharedPreferences("siapa_auth", Context.MODE_PRIVATE)
        }
    }

    val accessToken: String?
        get() = prefs?.getString("access_token", null)

    val refreshToken: String?
        get() = prefs?.getString("refresh_token", null)

    fun save(access: String, refresh: String) {
        prefs?.edit()
            ?.putString("access_token", access)
            ?.putString("refresh_token", refresh)
            ?.apply()
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    fun isLoggedIn(): Boolean = !refreshToken.isNullOrEmpty()
}
