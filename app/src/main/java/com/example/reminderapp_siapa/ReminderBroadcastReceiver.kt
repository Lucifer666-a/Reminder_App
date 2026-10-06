package com.example.reminderapp_siapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Pengingat Absen"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Waktunya melakukan presensi/absen!"
        val notificationId = intent.getIntExtra("EXTRA_ID", 1001)

        // Jika ini adalah Alarm Susulan / Follow-up (ID 999) -> Munculkan Notifikasi Saja Tanpa Alarm Berdering
        if (notificationId == 999) {
            val sharedPref = context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("KEY_ATTENDANCE_STATUS_TODAY", "MENUNGGU_KONFIRMASI").apply()

            showNotification(context, title, message, notificationId, false)
            return
        }

        // Jadwalkan ulang alarm berikutnya secara otomatis
        ReminderScheduler.scheduleReminder(context, notificationId, title, message)

        // Cek apakah hari ini Weekend (Sabtu atau Minggu)
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            // Hari Libur (Weekend): Jangan bunyikan alarm & jangan buka pop-up/notifikasi
            return
        }

        // Cek apakah hari ini untuk shift tersebut (Pagi atau Sore) pengguna sudah melakukan absen di SQLite
        val db = AttendanceDatabaseHelper(context)
        val isPagiShift = notificationId == 101 || LocalTime.now().hour < 12
        val isAlreadyAttended = if (isPagiShift) {
            db.getPagiAttendanceTime(today) != null
        } else {
            db.getSoreAttendanceTime(today) != null
        }

        // Cek apakah ini waktu Apel (Senin Pagi atau Jumat Sore) dan Mode Senyap Apel aktif di Settings
        val isMondayMorning = dayOfWeek == DayOfWeek.MONDAY && isPagiShift
        val isFridayEvening = dayOfWeek == DayOfWeek.FRIDAY && !isPagiShift

        val sharedPref = context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE)
        val isApelSilentMode = sharedPref.getBoolean("KEY_APEL_SILENT_MODE", true) // Default true agar tidak mengganggu apel
        val shouldSkipAlarmSound = (isMondayMorning || isFridayEvening) && isApelSilentMode

        // Jika BELUM absen dan BUKAN waktu apel bersenyap: Bunyikan alarm & Buka Pop-Up AlarmTriggerActivity
        if (!isAlreadyAttended && !shouldSkipAlarmSound) {
            AlarmSoundPlayer.playSound(context)

            val alarmIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_MESSAGE", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            try {
                context.startActivity(alarmIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Tampilkan Notifikasi tanpa suara
        showNotification(context, title, message, notificationId, isAlreadyAttended)
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        isAlreadyAttended: Boolean
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "REMINDER_SILENT_CHANNEL_ID"
        val channelName = "Pengingat Absen (Tanpa Suara)"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Saluran Notifikasi Pengingat Absen Tanpa Suara"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val targetIntent = if (isAlreadyAttended) {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(context, AlarmTriggerActivity::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_MESSAGE", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 500,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isAlreadyAttended) "$title (Sudah Absen)" else title)
            .setContentText(if (isAlreadyAttended) "Anda telah menyelesaikan presensi hari ini." else message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (!isAlreadyAttended) {
            notificationBuilder.setFullScreenIntent(contentPendingIntent, true)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
