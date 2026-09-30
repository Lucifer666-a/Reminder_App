package com.example.reminderapp_siapa

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme

class MeetingAttendanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                MeetingAttendanceScreen(
                    onBackClick = { finish() },
                    onSavedSuccess = {
                        Toast.makeText(this, "Absensi Rapat Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

data class SignaturePath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float = 6f
)

@Composable
fun MeetingAttendanceScreen(
    onBackClick: () -> Unit = {},
    onSavedSuccess: () -> Unit = {}
) {
    val context = LocalContext.current

    // Ambil nama dan jabatan user dari SharedPreferences (login awal) secara statis / read-only
    val sharedPref = remember { context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) }
    val savedUserName = remember { sharedPref.getString("KEY_USER_NAME", "User") ?: "User" }
    val savedUserPosition = remember { sharedPref.getString("KEY_USER_POSITION", "Pegawai") ?: "Pegawai" }

    val nama by remember { mutableStateOf(savedUserName) }
    val jabatan by remember { mutableStateOf(savedUserPosition) }

    // Dua pilihan warna: Hitam dan Merah
    val colorOptions = listOf(
        Color.Black,
        Color(0xFFD32F2F) // Merah
    )
    var selectedColor by remember { mutableStateOf(colorOptions[0]) }

    val paths = remember { mutableStateListOf<SignaturePath>() }

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
                        text = "Absensi Rapat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )
                    Text(
                        text = "Isi form dan tanda tangan digital",
                        fontSize = 12.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            // Form Card
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
                    // Field: Nama (Read-only / Static dari login awal)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Nama Lengkap (Sesuai Akun)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = nama,
                            onValueChange = {},
                            enabled = false, // Tidak bisa diubah (statis)
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFEDFAFD),
                                disabledBorderColor = Color(0xFFC0E0D5),
                                disabledTextColor = Color(0xFF1E353F),
                                disabledLabelColor = Color(0xFF1E353F)
                            ),
                            singleLine = true
                        )
                    }

                    // Field: Jabatan (Read-only / Static dari login awal)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jabatan / Instansi (Sesuai Akun)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = jabatan,
                            onValueChange = {},
                            enabled = false, // Tidak bisa diubah (statis)
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFEDFAFD),
                                disabledBorderColor = Color(0xFFC0E0D5),
                                disabledTextColor = Color(0xFF1E353F),
                                disabledLabelColor = Color(0xFF1E353F)
                            ),
                            singleLine = true
                        )
                    }

                    // Kolom Tanda Tangan (Signature Pad)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kolom Tanda Tangan (Tulis dengan jari)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C483A)
                            )
                            TextButton(onClick = { paths.clear() }) {
                                Text("Hapus", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFC0E0D5), RoundedCornerShape(14.dp))
                                .pointerInput(selectedColor) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                            paths.add(SignaturePath(newPath, selectedColor))
                                        },
                                        onDrag = { change, _ ->
                                            val lastPath = paths.lastOrNull()
                                            lastPath?.path?.lineTo(change.position.x, change.position.y)
                                            if (paths.isNotEmpty()) {
                                                val last = paths.removeAt(paths.size - 1)
                                                paths.add(last)
                                            }
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                paths.forEach { sigPath ->
                                    drawPath(
                                        path = sigPath.path,
                                        color = sigPath.color,
                                        style = Stroke(width = sigPath.strokeWidth)
                                    )
                                }
                            }
                            if (paths.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tandatangani di sini menggunakan jari Anda",
                                        fontSize = 13.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }
                        }
                    }

                    // Pemilihan Warna Tanda Tangan (Di bawah kolom tanda tangan, hanya Hitam & Merah)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pilih Warna Tanda Tangan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorOptions.forEach { color ->
                                val isSelected = selectedColor == color
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable { selectedColor = color }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color, CircleShape)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) Modifier.border(3.dp, Color(0xFF1C483A), CircleShape)
                                                else Modifier
                                            )
                                    )
                                    Text(
                                        text = if (color == Color.Black) "Hitam" else "Merah",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E353F)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Simpan
                    Button(
                        onClick = {
                            if (nama.isBlank()) {
                                Toast.makeText(context, "Nama akun kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (jabatan.isBlank()) {
                                Toast.makeText(context, "Jabatan akun kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (paths.isEmpty()) {
                                Toast.makeText(context, "Mohon buat tanda tangan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            try {
                                val db = AttendanceDatabaseHelper(context)
                                val colorHex = String.format("#%06X", (0xFFFFFF and selectedColor.hashCode()))
                                db.saveMeetingAttendance(
                                    nama = nama,
                                    jabatan = jabatan,
                                    signaturePath = null,
                                    colorHex = colorHex
                                )
                                onSavedSuccess()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Simpan Absensi Rapat",
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
