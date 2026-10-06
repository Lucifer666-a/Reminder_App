package com.example.reminderapp_siapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.util.concurrent.TimeUnit

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                SettingsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) }

    var nama by remember { mutableStateOf(sharedPref.getString("KEY_USER_NAME", "") ?: "") }
    var jabatan by remember { mutableStateOf(sharedPref.getString("KEY_USER_POSITION", "") ?: "") }

    // 2 Pilihan suara alarm: "default" atau "custom"
    var soundMode by remember {
        mutableStateOf(sharedPref.getString("KEY_ALARM_SOUND_MODE", "default") ?: "default")
    }
    var customAlarmUriStr by remember {
        mutableStateOf(sharedPref.getString("KEY_CUSTOM_ALARM_URI", null))
    }

    // Toggle Mode Senyap Saat Apel (Senin 07:45 & Jumat 15:45)
    var apelSilentMode by remember {
        mutableStateOf(sharedPref.getBoolean("KEY_APEL_SILENT_MODE", true))
    }

    // Logika pembatasan 7 hari untuk ubah Nama & Jabatan (Anti-manipulasi)
    val lastUpdateTimestamp = remember { sharedPref.getLong("KEY_LAST_PROFILE_UPDATE_TIMESTAMP", 0L) }
    val sevenDaysMillis = TimeUnit.DAYS.toMillis(7)
    val currentTime = System.currentTimeMillis()
    val timeElapsed = currentTime - lastUpdateTimestamp
    val canUpdateProfile = lastUpdateTimestamp == 0L || timeElapsed >= sevenDaysMillis

    val daysRemaining = if (!canUpdateProfile) {
        val remainingMillis = sevenDaysMillis - timeElapsed
        TimeUnit.MILLISECONDS.toDays(remainingMillis) + 1
    } else {
        0L
    }

    // Launcher untuk memilih file audio kustom dari penyimpanan HP (.mp3, .wav, dll)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            customAlarmUriStr = it.toString()
            sharedPref.edit().putString("KEY_CUSTOM_ALARM_URI", customAlarmUriStr).apply()
            Toast.makeText(context, "File suara berhasil dipilih!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F5FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .clickable { onBackClick() }
                ) {
                    Text(text = "←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Pengaturan Aplikasi",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )
                    Text(
                        text = "Ubah profil, alarm, & mode apel",
                        fontSize = 12.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            // Card Pengaturan
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!canUpdateProfile) {
                        // Peringatan pembatasan 7 hari
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Nama dan Jabatan dikunci untuk mencegah manipulasi. Baru dapat diubah kembali dalam $daysRemaining hari.",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Field: Nama Lengkap
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Nama Lengkap",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = nama,
                            onValueChange = { if (canUpdateProfile) nama = it },
                            enabled = canUpdateProfile,
                            placeholder = { Text("Masukkan nama lengkap...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFEDFAFD),
                                disabledBorderColor = Color(0xFFC0E0D5),
                                disabledTextColor = Color(0xFF1E353F),
                                disabledLabelColor = Color(0xFF1E353F),
                                focusedContainerColor = Color(0xFFEDFAFD),
                                unfocusedContainerColor = Color(0xFFEDFAFD),
                                focusedBorderColor = Color(0xFF1C483A),
                                unfocusedBorderColor = Color(0xFFC0E0D5)
                            ),
                            singleLine = true
                        )
                    }

                    // Field: Jabatan / Instansi
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jabatan / Instansi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = jabatan,
                            onValueChange = { if (canUpdateProfile) jabatan = it },
                            enabled = canUpdateProfile,
                            placeholder = { Text("Masukkan jabatan...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFEDFAFD),
                                disabledBorderColor = Color(0xFFC0E0D5),
                                disabledTextColor = Color(0xFF1E353F),
                                disabledLabelColor = Color(0xFF1E353F),
                                focusedContainerColor = Color(0xFFEDFAFD),
                                unfocusedContainerColor = Color(0xFFEDFAFD),
                                focusedBorderColor = Color(0xFF1C483A),
                                unfocusedBorderColor = Color(0xFFC0E0D5)
                            ),
                            singleLine = true
                        )
                    }

                    // Mode Senyap Saat Apel (Toggle Switch)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { apelSilentMode = !apelSilentMode }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mode Senyap Saat Apel",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C483A)
                                )
                                Text(
                                    text = "Meredam alarm otomatis saat jam Apel Pagi (Senin 07:45) & Apel Sore (Jumat 15:45)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = apelSilentMode,
                                onCheckedChange = { apelSilentMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1C483A)
                                )
                            )
                        }
                    }

                    // Pilihan Suara Reminder (2 Opsi: Default & Custom)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pilihan Suara Reminder",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )

                        // Opsi 1: Default
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { soundMode = "default" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = soundMode == "default",
                                onClick = { soundMode = "default" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1C483A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "1. Default Alarm Bawaan HP", fontSize = 14.sp, color = Color(0xFF1E353F))
                        }

                        // Opsi 2: Custom Suara Reminder
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { soundMode = "custom" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = soundMode == "custom",
                                onClick = { soundMode = "custom" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1C483A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "2. Custom Suara Reminder (Pilih File Audio)", fontSize = 14.sp, color = Color(0xFF1E353F))
                        }

                        if (soundMode == "custom") {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (customAlarmUriStr.isNullOrBlank()) "📂 Pilih File Audio (.mp3 / .wav)" else "✅ File Audio Terpilih",
                                    color = Color(0xFF1C483A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Tombol Uji Suara Reminder (Tanpa Looping)
                    OutlinedButton(
                        onClick = {
                            if (soundMode == "custom" && customAlarmUriStr.isNullOrBlank()) {
                                Toast.makeText(context, "Harap pilih file audio terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }

                            sharedPref.edit()
                                .putString("KEY_ALARM_SOUND_MODE", soundMode)
                                .putString("KEY_CUSTOM_ALARM_URI", customAlarmUriStr)
                                .apply()

                            AlarmSoundPlayer.playSoundOnce(context)
                            Toast.makeText(context, "Memutar uji suara (sekali)...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔊 Uji Suara Reminder", color = Color(0xFF1C483A), fontWeight = FontWeight.Bold)
                    }

                    // Tombol Hentikan Suara Tes
                    TextButton(
                        onClick = {
                            AlarmSoundPlayer.stopSound()
                            Toast.makeText(context, "Suara dihentikan", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Hentikan Suara", color = Color(0xFFD32F2F), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Simpan Pengaturan
                    Button(
                        onClick = {
                            if (canUpdateProfile && nama.isBlank()) {
                                Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (soundMode == "custom" && customAlarmUriStr.isNullOrBlank()) {
                                Toast.makeText(context, "Harap pilih file audio (.mp3) terlebih dahulu untuk mode Custom!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val editor = sharedPref.edit()
                                .putString("KEY_ALARM_SOUND_MODE", soundMode)
                                .putString("KEY_CUSTOM_ALARM_URI", customAlarmUriStr)
                                .putBoolean("KEY_APEL_SILENT_MODE", apelSilentMode)

                            if (canUpdateProfile) {
                                editor.putString("KEY_USER_NAME", nama.trim())
                                    .putString("KEY_USER_POSITION", jabatan.trim())
                                    .putLong("KEY_LAST_PROFILE_UPDATE_TIMESTAMP", currentTime)
                            }

                            editor.apply()

                            AlarmSoundPlayer.stopSound()
                            Toast.makeText(context, "Pengaturan Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Simpan Pengaturan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tombol Logout / Keluar Akun
                    OutlinedButton(
                        onClick = {
                            sharedPref.edit()
                                .remove("KEY_USER_NAME")
                                .remove("KEY_USER_POSITION")
                                .apply()

                            AlarmSoundPlayer.stopSound()
                            Toast.makeText(context, "Berhasil Keluar Akun", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚪 Keluar Akun (Logout)", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}
