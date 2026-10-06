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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Memasang 2 alarm otomatis (07:45 Pagi & 15:45 Sore)
        ReminderScheduler.setupDefaultAbsenAlarms(this)

        // SharedPreferences untuk menyimpan nama, jabatan, dan user_id secara permanen di HP
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
                            onLoginSuccess = { inputUserId, inputName, inputJabatan ->
                                val finalName = if (inputName.isNotBlank()) inputName else "User"
                                val finalJabatan = if (inputJabatan.isNotBlank()) inputJabatan else "Pegawai"
                                userName = finalName

                                // Simpan permanen user_id, nama, dan jabatan ke SharedPreferences
                                sharedPref.edit()
                                    .putInt("KEY_USER_ID", inputUserId)
                                    .putString("KEY_USER_NAME", finalName)
                                    .putString("KEY_USER_POSITION", finalJabatan)
                                    .apply()

                                currentScreen = "home"
                            }
                        )
                    }
                    "home" -> {
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
            recreate()
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (Int, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F5FA))
    ) {
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
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Login Akun RemindMe",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C483A)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Masuk menggunakan Username/NIP & Password dari admin",
                    fontSize = 12.sp,
                    color = Color(0xFF7A97A0)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Username / NIP Input
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Masukkan Username / NIP...", color = Color(0xFF7A97A0), fontSize = 14.sp) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFEDFAFD),
                        unfocusedContainerColor = Color(0xFFEDFAFD),
                        focusedBorderColor = Color(0xFF1C483A),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1E353F),
                        unfocusedTextColor = Color(0xFF1E353F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Masukkan Password...", color = Color(0xFF7A97A0), fontSize = 14.sp) },
                    singleLine = true,
                    shape = CircleShape,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFEDFAFD),
                        unfocusedContainerColor = Color(0xFFEDFAFD),
                        focusedBorderColor = Color(0xFF1C483A),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1E353F),
                        unfocusedTextColor = Color(0xFF1E353F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Tombol Submit (POST /absen/api/login.php)
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Username dan Password wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    ApiClient.apiService.login(LoginRequest(username.trim(), password.trim()))
                                }
                                val body = response.body()
                                if (response.isSuccessful && body?.success != false) {
                                    Toast.makeText(context, "✅ Login Berhasil!", Toast.LENGTH_SHORT).show()
                                    val resUserId = body?.userId ?: 1
                                    val resNama = body?.nama ?: username.trim()
                                    val resJabatan = body?.jabatan ?: "Pegawai"
                                    onLoginSuccess(resUserId, resNama, resJabatan)
                                } else {
                                    val errMessage = parseErrorMessage(response)
                                    Toast.makeText(context, "❌ Login Gagal: $errMessage", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "⚠️ Error Server: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Masuk (Login)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
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
