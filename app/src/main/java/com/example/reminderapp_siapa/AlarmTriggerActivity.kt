package com.example.reminderapp_siapa

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.time.LocalDate
import java.time.LocalTime

class AlarmTriggerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi agar layar otomatis menyala & MUNCUL LANGSUNG DI ATAS LOCKSCREEN TANPA MINTA PIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Waktunya Absen!"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Apakah Anda sudah melakukan absen?"

        // Membunyikan suara alarm saat pop-up terbuka
        AlarmSoundPlayer.playSound(this)

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                AlarmTriggerPopUp(
                    title = title,
                    message = message,
                    onSudahClick = {
                        // Matikan suara alarm & simpan presensi (pagi / sore tergantung jam saat ini)
                        AlarmSoundPlayer.stopSound()
                        val db = AttendanceDatabaseHelper(this)
                        val currentHour = LocalTime.now().hour
                        if (currentHour < 12) {
                            db.markAttendancePagi(LocalDate.now())
                        } else {
                            db.markAttendanceSore(LocalDate.now())
                        }
                        finish()
                    },
                    onBelumClick = {
                        // Matikan suara alarm & tutup
                        AlarmSoundPlayer.stopSound()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Pastikan suara alarm mati saat layar ditutup
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
            .background(Color.Black.copy(alpha = 0.45f)), // Transparan gelap blur sehingga layar belakang tetap terlihat
        contentAlignment = Alignment.Center
    ) {
        // Pop Up Card Alarm
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE2E2E2)
            ),
            border = BorderStroke(1.5.dp, Color.Black),
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
                // Ikon Alarm ⏰
                Text(
                    text = "⏰",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Judul
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pertanyaan / Pesan
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = Color(0xFF444444),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pertanyaan "Sudah Absen apa Belum?"
                Text(
                    text = "Apakah Sudah Absen?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Row 2 Tombol (Belum & Sudah)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol 1: Belum
                    Button(
                        onClick = { onBelumClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5A9A9) // Merah Lembut
                        ),
                        border = BorderStroke(1.dp, Color.Black),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Belum",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Tombol 2: Sudah
                    Button(
                        onClick = { onSudahClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC5FA96) // Hijau Cerah
                        ),
                        border = BorderStroke(1.dp, Color.Black),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Sudah",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmTriggerPopUpPreview() {
    Reminderapp_SIAPATheme {
        AlarmTriggerPopUp(
            title = "Absen Masuk Pagi",
            message = "Waktunya melakukan presensi/absen masuk pagi (08:00)!"
        )
    }
}
