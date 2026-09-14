package com.example.reminderapp_siapa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
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

        // Jadwalkan ulang alarm berikutnya untuk esok hari agar terus berjalan otomatis
        val hour = if (notificationId == 101) 8 else 16
        ReminderScheduler.scheduleReminder(context, notificationId, hour, 0, title, message)

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

        // Jika BELUM absen: Bunyikan alarm & Buka Pop-Up AlarmTriggerActivity secara otomatis
        if (!isAlreadyAttended) {
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

        // Tampilkan Notifikasi (Akan membuka AlarmTriggerActivity jika belum absen, atau MainActivity jika sudah)
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

        val channelId = "REMINDER_CHANNEL_ID"
        val channelName = "Pengingat Absen & Reminder"

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Saluran Notifikasi Pengingat Alarm Absen"
                enableVibration(true)
                setSound(alarmSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tentukan target Intent saat notifikasi diklik:
        // - Jika belum absen: Buka Pop-Up AlarmTriggerActivity
        // - Jika sudah absen: Buka Home Screen (MainActivity)
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

        // Membangun Notifikasi Tanpa Tombol Action Tambahan
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isAlreadyAttended) "$title (Sudah Absen)" else title)
            .setContentText(if (isAlreadyAttended) "Anda telah menyelesaikan presensi hari ini." else message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)

        if (!isAlreadyAttended) {
            notificationBuilder.setFullScreenIntent(contentPendingIntent, true)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
