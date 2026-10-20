package com.example.reminderapp_siapa

import android.app.Application

class ReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inisialisasi global ApiClient & TokenStore saat aplikasi pertama kali dijalankan
        ApiClient.init(this)
    }
}
