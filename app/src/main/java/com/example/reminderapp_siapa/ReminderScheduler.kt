package com.example.reminderapp_siapa

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    /**
     * Menjadwalkan pengingat alarm pada jam dan menit tertentu.
     */
    fun scheduleReminder(
        context: Context,
        reminderId: Int,
        hour: Int,
        minute: Int,
        title: String = "Pengingat Absen",
        message: String = "Waktunya melakukan presensi/absen!"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_ID", reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Atur waktu alarm pada jam dan menit yang ditentukan
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Jika jam/menit yang ditentukan sudah lewat hari ini, jadwalkan untuk besok
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Mengecek izin exact alarm pada Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Menjadwalkan 2 Alarm Absen Otomatis:
     * 1. Alarm Pagi: Jam 08:00
     * 2. Alarm Sore: Jam 16:00
     */
    fun setupDefaultAbsenAlarms(context: Context) {
        // Alarm 1: Jam 08:00 Pagi (Absen Masuk)
        scheduleReminder(
            context = context,
            reminderId = 101,
            hour = 8,
            minute = 0,
            title = "Absen Masuk Pagi",
            message = "Waktunya melakukan presensi/absen masuk pagi (08:00)!"
        )

        // Alarm 2: Jam 16:00 Sore (Absen Pulang)
        scheduleReminder(
            context = context,
            reminderId = 102,
            hour = 16,
            minute = 0,
            title = "Absen Pulang Sore",
            message = "Waktunya melakukan presensi/absen pulang sore (16:00)!"
        )
    }

    /**
     * Membatalkan pengingat alarm berdasarkan ID.
     */
    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
