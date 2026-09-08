package com.example.reminderapp_siapa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Meminta Izin Notifikasi untuk Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Memasang 2 alarm otomatis (08:00 Pagi & 16:00 Sore)
        ReminderScheduler.setupDefaultAbsenAlarms(this)

        // SharedPreferences untuk menyimpan nama secara permanen di HP
        val sharedPref = getSharedPreferences("app_user_prefs", MODE_PRIVATE)
        val savedUserName = sharedPref.getString("KEY_USER_NAME", "") ?: ""

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                // Jika sudah ada nama tersimpan, langsung ke layar "home"
                var currentScreen by remember {
                    mutableStateOf(if (savedUserName.isNotBlank()) "home" else "login")
                }
                var userName by remember {
                    mutableStateOf(if (savedUserName.isNotBlank()) savedUserName else "User")
                }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = { inputName ->
                            val finalName = if (inputName.isNotBlank()) inputName else "User"
                            userName = finalName

                            // Simpan permanen ke SharedPreferences
                            sharedPref.edit().putString("KEY_USER_NAME", finalName).apply()

                            currentScreen = "home"
                        }
                    )
                    "home" -> HomeScreen(
                        userName = userName,
                        onLookPresentClick = { currentScreen = "look_present" }
                    )
                    "look_present" -> LookPresentScreen(onBackClick = { currentScreen = "home" })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit = {}
) {
    var nama by remember { mutableStateOf("") }

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

                Spacer(modifier = Modifier.height(28.dp))

                // Tombol Login (Soft Cyan Pill Button)
                Button(
                    onClick = { onLoginSuccess(nama) },
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
