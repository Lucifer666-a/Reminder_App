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

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.example.reminderapp_siapa.ACTION_MARK_ATTENDANCE"
        const val ACTION_DISMISS_ALARM = "com.example.reminderapp_siapa.ACTION_DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Tangani aksi dari tombol Notifikasi Banner ("Sudah" atau "Belum")
        if (action == ACTION_MARK_ATTENDANCE) {
            AlarmSoundPlayer.stopSound()
            val db = AttendanceDatabaseHelper(context)
            val currentHour = LocalTime.now().hour
            if (currentHour < 12) {
                db.markAttendancePagi(LocalDate.now())
            } else {
                db.markAttendanceSore(LocalDate.now())
            }
            val notificationId = intent.getIntExtra("EXTRA_ID", 1001)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            return
        } else if (action == ACTION_DISMISS_ALARM) {
            AlarmSoundPlayer.stopSound()
            val notificationId = intent.getIntExtra("EXTRA_ID", 1001)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            return
        }

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

        // 1. Membunyikan suara alarm secara teratur
        AlarmSoundPlayer.playSound(context)

        // 2. Tampilkan Notifikasi dengan Suara, Getaran, dan Tombol Pilihan
        showNotification(context, title, message, notificationId)

        // 3. Buka Pop-Up UI AlarmTriggerActivity langsung di layar
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

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
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

        // Fullscreen Intent & Content Intent keduanya MENGARAH KE AlarmTriggerActivity (Pop-Up UI)
        val alarmActivityIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val popupPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 500,
            alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent untuk Tombol Action "Sudah" di Banner Notifikasi
        val markIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_MARK_ATTENDANCE
            putExtra("EXTRA_ID", notificationId)
        }
        val markPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 600,
            markIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent untuk Tombol Action "Belum" di Banner Notifikasi
        val dismissIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra("EXTRA_ID", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 700,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Membangun Notifikasi
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(popupPendingIntent, true)
            .setContentIntent(popupPendingIntent) // Mengeklik notifikasi AKAN MEMBUKA POP-UP AlarmTriggerActivity
            .addAction(0, "Sudah", markPendingIntent)
            .addAction(0, "Belum", dismissPendingIntent)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
