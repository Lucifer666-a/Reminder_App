package com.example.reminderapp_siapa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // Otomatis mendaftarkan ulang 2 alarm (08:00 & 16:00) saat HP selesai direstart
            ReminderScheduler.setupDefaultAbsenAlarms(context)
        }
    }
}
