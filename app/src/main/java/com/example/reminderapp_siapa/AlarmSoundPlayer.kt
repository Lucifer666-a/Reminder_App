package com.example.reminderapp_siapa

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

object AlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Membunyikan suara alarm berulang (looping) sampai dihentikan,
     * mendukung Default atau Custom Uri dari SharedPreferences dengan penanganan error yang aman.
     */
    fun playSound(context: Context) {
        if (mediaPlayer?.isPlaying == true) return
        stopSound()

        try {
            val sharedPref = context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE)
            val soundMode = sharedPref.getString("KEY_ALARM_SOUND_MODE", "default")
            val customUriStr = sharedPref.getString("KEY_CUSTOM_ALARM_URI", null)

            var alarmUri: Uri? = null
            if (soundMode == "custom" && !customUriStr.isNullOrBlank()) {
                try {
                    alarmUri = Uri.parse(customUriStr)
                    // Test apakah URI dapat diakses
                    context.contentResolver.openInputStream(alarmUri)?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    alarmUri = null // Fallback ke default jika URI custom tidak valid/izin hilang
                }
            }

            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Membunyikan suara alarm sekali saja (tidak looping) untuk keperluan Uji Suara di Settings.
     */
    fun playSoundOnce(context: Context) {
        stopSound()
        try {
            val sharedPref = context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE)
            val soundMode = sharedPref.getString("KEY_ALARM_SOUND_MODE", "default")
            val customUriStr = sharedPref.getString("KEY_CUSTOM_ALARM_URI", null)

            var alarmUri: Uri? = null
            if (soundMode == "custom" && !customUriStr.isNullOrBlank()) {
                try {
                    alarmUri = Uri.parse(customUriStr)
                    context.contentResolver.openInputStream(alarmUri)?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    alarmUri = null
                }
            }

            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = false // Tidak looping saat tes
                setOnCompletionListener { stopSound() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
}
