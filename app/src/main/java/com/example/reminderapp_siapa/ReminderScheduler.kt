package com.example.reminderapp_siapa

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    /**
     * Menjadwalkan pengingat alarm dinamis berdasarkan hari (Senin vs Hari Biasa, Jumat vs Hari Biasa).
     * - Absen Pagi: Senin = 07:45, Selasa - Jumat = 07:55
     * - Absen Sore: Jumat = 16:10, Senin - Kamis = 16:30
     */
    fun scheduleReminder(
        context: Context,
        reminderId: Int,
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

        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)

        // Tentukan jam target awal berdasarkan hari ini
        val (targetHour, targetMinute) = if (reminderId == 101) {
            if (currentDay == Calendar.MONDAY) Pair(7, 45) else Pair(7, 55)
        } else {
            if (currentDay == Calendar.FRIDAY) Pair(16, 10) else Pair(16, 30)
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Jika waktu hari ini sudah lewat, atau hari ini libur (Sabtu/Minggu), cari hari kerja berikutnya
            if (timeInMillis <= System.currentTimeMillis() || currentDay == Calendar.SATURDAY || currentDay == Calendar.SUNDAY) {
                do {
                    add(Calendar.DAY_OF_YEAR, 1)
                } while (get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)

                val newDay = get(Calendar.DAY_OF_WEEK)
                val (newH, newM) = if (reminderId == 101) {
                    if (newDay == Calendar.MONDAY) Pair(7, 45) else Pair(7, 55)
                } else {
                    if (newDay == Calendar.FRIDAY) Pair(16, 10) else Pair(16, 30)
                }
                set(Calendar.HOUR_OF_DAY, newH)
                set(Calendar.MINUTE, newM)
            }
        }

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                calendar.timeInMillis,
                pendingIntent
            )
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menjadwalkan Alarm Susulan (Follow-up) 5 menit kemudian jika pegawai memilih "Belum".
     */
    fun scheduleFollowUpAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", "Konfirmasi Absen Susulan")
            putExtra("EXTRA_MESSAGE", "Sudah absen? Konfirmasi di sini")
            putExtra("EXTRA_ID", 999)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (5 * 60 * 1000L) // 5 menit kemudian
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menjadwalkan 2 Alarm Absen Otomatis.
     */
    fun setupDefaultAbsenAlarms(context: Context) {
        scheduleReminder(
            context = context,
            reminderId = 101,
            title = "Absen Masuk Pagi",
            message = "Waktunya presensi pagi!"
        )

        scheduleReminder(
            context = context,
            reminderId = 102,
            title = "Absen Pulang Sore",
            message = "Waktunya presensi sore!"
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
