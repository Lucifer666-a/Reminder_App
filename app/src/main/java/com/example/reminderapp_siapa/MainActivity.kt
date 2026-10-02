package com.example.reminderapp_siapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme

class MainActivity : ComponentActivity() {

    private var lastSavedName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Meminta Izin Notifikasi untuk Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Meminta Izin Display Over Other Apps agar Pop-Up Alarm bisa langsung muncul di atas layar saat HP aktif
        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Memasang 2 alarm otomatis (08:00 Pagi & 16:00 Sore)
        ReminderScheduler.setupDefaultAbsenAlarms(this)

        // SharedPreferences untuk menyimpan nama dan jabatan secara permanen di HP
        val sharedPref = getSharedPreferences("app_user_prefs", MODE_PRIVATE)
        val savedUserName = sharedPref.getString("KEY_USER_NAME", "") ?: ""
        lastSavedName = savedUserName

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                val context = LocalContext.current
                var backPressedTime by remember { mutableStateOf(0L) }

                // Jika sudah ada nama tersimpan, langsung ke layar "home"
                var currentScreen by remember {
                    mutableStateOf(if (savedUserName.isNotBlank()) "home" else "login")
                }
                var userName by remember {
                    mutableStateOf(if (savedUserName.isNotBlank()) savedUserName else "User")
                }

                when (currentScreen) {
                    "login" -> {
                        BackHandler {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - backPressedTime < 2000L) {
                                (context as? ComponentActivity)?.finish()
                            } else {
                                backPressedTime = currentTime
                                Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                            }
                        }
                        LoginScreen(
                            onLoginSuccess = { inputName, inputJabatan ->
                                val finalName = if (inputName.isNotBlank()) inputName else "User"
                                val finalJabatan = if (inputJabatan.isNotBlank()) inputJabatan else "Pegawai"
                                userName = finalName

                                // Simpan permanen ke SharedPreferences
                                sharedPref.edit()
                                    .putString("KEY_USER_NAME", finalName)
                                    .putString("KEY_USER_POSITION", finalJabatan)
                                    .apply()

                                currentScreen = "home"
                            }
                        )
                    }
                    "home" -> {
                        // Logika double-tap back untuk keluar aplikasi pada Home Screen
                        BackHandler {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - backPressedTime < 2000L) {
                                (context as? ComponentActivity)?.finish()
                            } else {
                                backPressedTime = currentTime
                                Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                            }
                        }
                        HomeScreen(
                            userName = userName,
                            onLookPresentClick = { currentScreen = "look_present" },
                            onMeetingAttendanceClick = {
                                val intent = Intent(this, MeetingAttendanceActivity::class.java)
                                startActivity(intent)
                            },
                            onSettingsClick = {
                                val intent = Intent(this, SettingsActivity::class.java)
                                startActivity(intent)
                            },
                            onJumpWebClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sikep.mahkamahagung.go.id/site/login"))
                                startActivity(intent)
                            }
                        )
                    }
                    "look_present" -> {
                        // Tekan kembali dari Look Present -> kembali ke home
                        BackHandler {
                            currentScreen = "home"
                        }
                        LookPresentScreen(
                            onBackClick = { currentScreen = "home" },
                            onGalleryClick = { currentScreen = "gallery" },
                            onJumpWebClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sikep.mahkamahagung.go.id/site/login"))
                                startActivity(intent)
                            }
                        )
                    }
                    "gallery" -> {
                        // Tekan kembali dari Gallery -> kembali ke look_present
                        BackHandler {
                            currentScreen = "look_present"
                        }
                        GalleryScreen(onBackClick = { currentScreen = "look_present" })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_user_prefs", MODE_PRIVATE)
        val currentSavedName = sharedPref.getString("KEY_USER_NAME", "") ?: ""
        if (currentSavedName.isNotBlank() && currentSavedName != lastSavedName) {
            lastSavedName = currentSavedName
            recreate() // Refresh activity agar nama baru dan pengaturan alarm langsung diterapkan
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit = { _, _ -> }
) {
    var nama by remember { mutableStateOf("") }
    var jabatan by remember { mutableStateOf("") }

    // Outer Background Cyan Pastel Soft
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F5FA))
    ) {
        // Kartu Putih Utama
        Card(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input TextField Nama (Soft Cyan Capsule)
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    placeholder = {
                        Text(
                            text = "Masukkan Nama....",
                            color = Color(0xFF7A97A0),
                            fontSize = 15.sp
                        )
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFEDFAFD),
                        unfocusedContainerColor = Color(0xFFEDFAFD),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1E353F),
                        unfocusedTextColor = Color(0xFF1E353F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input TextField Jabatan di bawah Nama (Soft Cyan Capsule)
                OutlinedTextField(
                    value = jabatan,
                    onValueChange = { jabatan = it },
                    placeholder = {
                        Text(
                            text = "Masukkan Jabatan....",
                            color = Color(0xFF7A97A0),
                            fontSize = 15.sp
                        )
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFEDFAFD),
                        unfocusedContainerColor = Color(0xFFEDFAFD),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1E353F),
                        unfocusedTextColor = Color(0xFF1E353F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Tombol Login (Soft Cyan Pill Button)
                Button(
                    onClick = { onLoginSuccess(nama, jabatan) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0F7FC)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Login",
                        color = Color(0xFF2B4A55),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    Reminderapp_SIAPATheme {
        LoginScreen()
    }
}
