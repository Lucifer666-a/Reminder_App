package com.example.reminderapp_siapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RegularAttendanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateStr = intent.getStringExtra("EXTRA_DATE") ?: LocalDate.now().toString()
        val attendanceType = intent.getStringExtra("EXTRA_TYPE") ?: "APEL_PAGI"
        val targetDate = try { LocalDate.parse(dateStr) } catch (_: Exception) { LocalDate.now() }

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                RegularAttendanceScreen(
                    targetDate = targetDate,
                    attendanceType = attendanceType,
                    onBackClick = { finish() },
                    onAttendanceMarked = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RegularAttendanceScreen(
    targetDate: LocalDate,
    attendanceType: String,
    onBackClick: () -> Unit = {},
    onAttendanceMarked: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val today = LocalDate.now()

    // Validasi anti-manipulasi: pastikan targetDate == today
    val isToday = targetDate == today

    // Ambil nama dan jabatan user dari SharedPreferences (login awal)
    val sharedPref = remember { context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) }
    val savedUserName = remember { sharedPref.getString("KEY_USER_NAME", "User") ?: "User" }
    val savedUserPosition = remember { sharedPref.getString("KEY_USER_POSITION", "Pegawai") ?: "Pegawai" }

    val nama by remember { mutableStateOf(savedUserName) }
    val jabatan by remember { mutableStateOf(savedUserPosition) }

    // State untuk Nama Acara / Topik Rapat (diambil otomatis dari GET /api/acara.php)
    var namaAcara by remember { mutableStateOf("Memuat Acara Aktif...") }
    var isAcaraLoading by remember { mutableStateOf(true) }

    // Ambil daftar/acara aktif dari backend PHP (acara.php) saat pertama kali dibuka
    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.getAcara()
            }
            if (response.isSuccessful) {
                val element = response.body()
                val serverAcara = parseEventFromJson(element)
                if (!serverAcara.isNullOrBlank()) {
                    namaAcara = serverAcara
                } else {
                    namaAcara = "Tidak ada acara aktif (Belum dibuat admin)"
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                namaAcara = "HTTP Error ${response.code()}: $errorBody"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            namaAcara = "Exception: ${e.javaClass.simpleName} - ${e.localizedMessage}"
        } finally {
            isAcaraLoading = false
        }
    }

    val title = when (attendanceType) {
        "APEL_PAGI" -> "Apel Pagi (Senin)"
        "APEL_SORE" -> "Apel Sore (Jumat)"
        "PAGI" -> "Absen Masuk Pagi"
        "SORE" -> "Absen Pulang Sore"
        else -> "Presensi Kehadiran"
    }

    val dateFormatted = targetDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")))

    var isSubmitting by remember { mutableStateOf(false) }

    // Dua pilihan warna: Hitam dan Merah
    val colorOptions = listOf(
        Color.Black,
        Color(0xFFD32F2F) // Merah
    )
    var selectedColor by remember { mutableStateOf(colorOptions[0]) }

    // Data tanda tangan berbasis titik dengan batasan area box
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val currentStroke = remember { mutableStateListOf<Offset>() }

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
                        text = "$title (Online)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )
                    Text(
                        text = dateFormatted,
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
                    if (!isToday) {
                        // Peringatan Anti Manipulasi
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Peringatan: Hari ini bukan tanggal $dateFormatted. Presensi dikunci untuk mencegah manipulasi data.",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Field: Nama Acara / Jenis Kegiatan (Read-only)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jenis Presensi / Kegiatan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = "$title - $dateFormatted",
                            onValueChange = {},
                            enabled = false,
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

                    // Field: Nama (Read-only)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Nama Lengkap",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = nama,
                            onValueChange = {},
                            enabled = false,
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

                    // Field: Jabatan (Read-only)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jabatan / Instansi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = jabatan,
                            onValueChange = {},
                            enabled = false,
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

                    // Kolom Tanda Tangan (Signature Pad dengan batasan area)
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
                            TextButton(onClick = {
                                strokes.clear()
                                currentStroke.clear()
                            }) {
                                Text("Hapus", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFC0E0D5), RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .clipToBounds()
                                .pointerInput(selectedColor) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (isToday && offset.x >= 0 && offset.y >= 0 && offset.x <= size.width && offset.y <= size.height) {
                                                currentStroke.add(offset)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            if (isToday) {
                                                val pos = change.position
                                                if (pos.x >= 0 && pos.y >= 0 && pos.x <= size.width && pos.y <= size.height) {
                                                    currentStroke.add(pos)
                                                    change.consume()
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isToday && currentStroke.isNotEmpty()) {
                                                strokes.add(currentStroke.toList())
                                                currentStroke.clear()
                                            }
                                        },
                                        onDragCancel = {
                                            currentStroke.clear()
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                strokes.forEach { stroke ->
                                    for (i in 0 until stroke.size - 1) {
                                        drawLine(
                                            color = selectedColor,
                                            start = stroke[i],
                                            end = stroke[i + 1],
                                            strokeWidth = 6f
                                        )
                                    }
                                }
                                for (i in 0 until currentStroke.size - 1) {
                                    drawLine(
                                        color = selectedColor,
                                        start = currentStroke[i],
                                        end = currentStroke[i + 1],
                                        strokeWidth = 6f
                                    )
                                }
                            }
                            if (strokes.isEmpty() && currentStroke.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isToday) "Tandatangani di sini menggunakan jari Anda" else "Presensi Hari Lain Dikunci",
                                        fontSize = 13.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }
                        }
                    }

                    // Pemilihan Warna Tanda Tangan (Hitam & Merah)
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
                                    modifier = Modifier.clickable { if (isToday) selectedColor = color }
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

                    // Tombol Simpan & Kirim ke Server via POST absen_apel.php (Tanpa PIN)
                    Button(
                        onClick = {
                            if (!isToday) {
                                Toast.makeText(context, "Tidak dapat mengubah presensi hari lain (Mencegah manipulasi data)", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (strokes.isEmpty()) {
                                Toast.makeText(context, "Mohon buat tanda tangan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    val signatureBitmap = createSignatureBitmap(strokes, selectedColor)
                                    val signatureBase64 = bitmapToBase64(signatureBitmap)

                                    val colorName = if (selectedColor == Color.Black) "Hitam" else "Merah"
                                    val sesiValue = if (attendanceType == "APEL_PAGI" || attendanceType == "PAGI") "Pagi" else "Sore"
                                    val request = ApelAbsenRequest(
                                        nama = nama,
                                        jabatan = jabatan,
                                        acara = namaAcara,
                                        sesi = sesiValue,
                                        signature = signatureBase64,
                                        color = colorName,
                                        date = targetDate.toString(),
                                        time = LocalTime.now().toString()
                                    )

                                    // Kirim POST ke absen_apel.php (Tanpa PIN)
                                    val response = withContext(Dispatchers.IO) {
                                        ApiClient.apiService.submitApelAbsen(request)
                                    }

                                    val responseBody = response.body()
                                    if (response.isSuccessful && responseBody != null) {
                                        if (responseBody.success != false) {
                                            // Simpan ke SQLite lokal
                                            val db = AttendanceDatabaseHelper(context)
                                            when (attendanceType) {
                                                "APEL_PAGI" -> db.markApelPagi(targetDate)
                                                "APEL_SORE" -> db.markApelSore(targetDate)
                                                "PAGI" -> db.markAttendancePagi(targetDate)
                                                "SORE" -> db.markAttendanceSore(targetDate)
                                                else -> db.markAttendancePagi(targetDate)
                                            }

                                            Toast.makeText(context, "✅ ${responseBody.message ?: "Presensi Apel Berhasil Disimpan!"}", Toast.LENGTH_LONG).show()
                                            onAttendanceMarked()
                                        } else {
                                            Toast.makeText(context, "❌ Gagal: ${responseBody.message ?: "Gagal menyimpan"}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        val cleanError = parseErrorMessage(response)
                                        Toast.makeText(context, "❌ $cleanError", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "❌ Koneksi Gagal ke Server: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = isToday && !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isToday) Color(0xFF1C483A) else Color(0xFFCCCCCC)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isToday) "Kirim Presensi ke Server Kantor" else "Presensi Dikunci (Bukan Hari Ini)",
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
}
