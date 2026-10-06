package com.example.reminderapp_siapa

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.time.LocalDate
import java.time.LocalTime

class AlarmTriggerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()
        handleAlarmIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupWindowFlags()
        handleAlarmIntent(intent)
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun handleAlarmIntent(currentIntent: Intent) {
        val today = LocalDate.now()
        val currentHour = LocalTime.now().hour
        val db = AttendanceDatabaseHelper(this)

        // Cek jika sudah absen untuk shift saat ini, tidak perlu pop-up
        val isAlreadyAttended = if (currentHour < 12) {
            db.getPagiAttendanceTime(today) != null
        } else {
            db.getSoreAttendanceTime(today) != null
        }

        if (isAlreadyAttended) {
            AlarmSoundPlayer.stopSound()
            finishAndRemoveTask()
            return
        }

        val title = currentIntent.getStringExtra("EXTRA_TITLE") ?: "Waktunya Absen!"
        val message = currentIntent.getStringExtra("EXTRA_MESSAGE") ?: "Apakah Anda sudah melakukan absen?"

        AlarmSoundPlayer.playSound(this)

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                AlarmTriggerPopUp(
                    title = title,
                    message = message,
                    onSudahClick = {
                        AlarmSoundPlayer.stopSound()
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancelAll()

                        // Buka Kamera untuk Ambil Foto Verifikasi Presensi
                        val photoType = if (currentHour < 12) "PAGI" else "SORE"
                        val cameraIntent = Intent(this, CameraActivity::class.java).apply {
                            putExtra("EXTRA_PHOTO_TYPE", photoType)
                        }
                        startActivity(cameraIntent)
                        finishAndRemoveTask()
                    },
                    onBelumClick = {
                        AlarmSoundPlayer.stopSound()
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancelAll()

                        // Jump to Web Link (https://sikep.mahkamahagung.go.id/site/login)
                        try {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sikep.mahkamahagung.go.id/site/login")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(webIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundPlayer.stopSound()
    }
}

@Composable
fun AlarmTriggerPopUp(
    title: String,
    message: String,
    onSudahClick: () -> Unit = {},
    onBelumClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEDFAFD),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⏰", fontSize = 32.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C483A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Apakah Sudah Absen?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Belum (🌐 Jump to Web)
                    Button(
                        onClick = { onBelumClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = "Belum (Web)",
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Tombol Sudah (📸 Buka Kamera Foto Absen)
                    Button(
                        onClick = { onSudahClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 6.dp)
                    ) {
                        Text(
                            text = "Sudah",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
