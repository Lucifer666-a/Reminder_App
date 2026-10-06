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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime

class MeetingAttendanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                MeetingAttendanceScreen(
                    onBackClick = { finish() },
                    onSavedSuccess = {
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

// Helper universal untuk membaca nama event dari format JSON apa pun
fun parseEventFromJson(element: JsonElement?): String? {
    if (element == null) return null
    try {
        if (element.isJsonPrimitive) {
            val str = element.asString
            if (str.isNotBlank() && str.lowercase() != "null") return str
        } else if (element.isJsonObject) {
            val obj = element.asJsonObject
            val keys = listOf("acara", "nama_acara", "title", "name", "event", "nama", "topik", "kegiatan")
            for (key in keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    val valStr = obj.get(key).asString
                    if (valStr.isNotBlank() && valStr.lowercase() != "null") return valStr
                }
            }
            if (obj.has("data") && !obj.get("data").isJsonNull) {
                val nested = parseEventFromJson(obj.get("data"))
                if (!nested.isNullOrBlank()) return nested
            }
        } else if (element.isJsonArray) {
            val arr = element.asJsonArray
            if (arr.size() > 0) {
                return parseEventFromJson(arr.get(0))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

// Helper universal untuk mengekstrak ID acara aktif dari JSON
fun parseEventIdFromJson(element: JsonElement?): Int? {
    if (element == null) return null
    try {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val keys = listOf("acara_id", "id_acara", "id", "event_id")
            for (key in keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    return obj.get(key).asInt
                }
            }
            if (obj.has("data") && !obj.get("data").isJsonNull) {
                return parseEventIdFromJson(obj.get("data"))
            }
        } else if (element.isJsonArray) {
            val arr = element.asJsonArray
            if (arr.size() > 0) {
                return parseEventIdFromJson(arr.get(0))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

// Helper untuk mengekstrak pesan error bersih dari errorBody server
fun parseErrorMessage(response: Response<*>?): String {
    if (response == null) return "Terjadi kesalahan pada server"
    try {
        val errorBodyStr = response.errorBody()?.string()
        if (!errorBodyStr.isNullOrBlank()) {
            val jsonObject = Gson().fromJson(errorBodyStr, JsonObject::class.java)
            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull) {
                return jsonObject.get("message").asString
            }
            if (jsonObject.has("error") && !jsonObject.get("error").isJsonNull) {
                return jsonObject.get("error").asString
            }
            if (jsonObject.has("status") && !jsonObject.get("status").isJsonNull) {
                return jsonObject.get("status").asString
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "Gagal (Error ${response.code()})"
}

// Helper untuk mengubah coretan Canvas menjadi Bitmap, lalu dikonversi ke Base64
fun createSignatureBitmap(strokes: List<List<Offset>>, color: Color, width: Int = 600, height: Int = 200): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    strokes.forEach { stroke ->
        for (i in 0 until stroke.size - 1) {
            val start = stroke[i]
            val end = stroke[i + 1]
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }

    return bitmap
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT).replace("\n", "")
}

@Composable
fun MeetingAttendanceScreen(
    onBackClick: () -> Unit = {},
    onSavedSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Ambil nama dan jabatan user dari SharedPreferences (login awal)
    val sharedPref = remember { context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) }
    val savedUserName = remember { sharedPref.getString("KEY_USER_NAME", "User") ?: "User" }
    val savedUserPosition = remember { sharedPref.getString("KEY_USER_POSITION", "Pegawai") ?: "Pegawai" }

    val nama by remember { mutableStateOf(savedUserName) }
    val jabatan by remember { mutableStateOf(savedUserPosition) }

    // State untuk Nama & ID Acara Rapat (diambil otomatis dari GET /api/acara.php)
    var namaAcara by remember { mutableStateOf("Memuat Acara Aktif...") }
    var currentAcaraId by remember { mutableStateOf<Int?>(null) }
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
                val serverAcaraId = parseEventIdFromJson(element)
                if (!serverAcara.isNullOrBlank()) {
                    namaAcara = serverAcara
                    currentAcaraId = serverAcaraId
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

    // State untuk PIN Absen (diverifikasi oleh backend)
    var pin by remember { mutableStateOf("") }
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
                        text = "Absensi Rapat (Online)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )
                    Text(
                        text = "Terhubung ke database server kantor",
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
                    // Field: Nama Acara / Topik Rapat (Read-only / Diatur oleh Admin via acara.php)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Nama Acara / Topik Rapat (Dari Admin)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = namaAcara,
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
                                            if (offset.x >= 0 && offset.y >= 0 && offset.x <= size.width && offset.y <= size.height) {
                                                currentStroke.add(offset)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            val pos = change.position
                                            if (pos.x >= 0 && pos.y >= 0 && pos.x <= size.width && pos.y <= size.height) {
                                                currentStroke.add(pos)
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            if (currentStroke.isNotEmpty()) {
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
                                        text = "Tandatangani di sini menggunakan jari Anda",
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

                    // Field PIN Absen Rapat (Diverifikasi oleh server PHP)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PIN Absen Rapat (Dari Admin)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            placeholder = { Text("Masukkan PIN dari admin...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFEDFAFD),
                                unfocusedContainerColor = Color(0xFFEDFAFD),
                                focusedBorderColor = Color(0xFF1C483A),
                                unfocusedBorderColor = Color(0xFFC0E0D5)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Simpan & Kirim ke Server via POST absen.php
                    Button(
                        onClick = {
                            if (isAcaraLoading || namaAcara.isBlank() || namaAcara.contains("Memuat") || namaAcara.contains("HTTP") || namaAcara.contains("Exception") || namaAcara.contains("Tidak ada") || namaAcara.contains("Gagal")) {
                                Toast.makeText(context, "Acara aktif belum berhasil dimuat dari server!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (pin.isBlank()) {
                                Toast.makeText(context, "PIN absen wajib diisi!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (strokes.isEmpty()) {
                                Toast.makeText(context, "Mohon buat tanda tangan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    val userId = sharedPref.getInt("KEY_USER_ID", 1)
                                    val signatureBitmap = createSignatureBitmap(strokes, selectedColor)
                                    val signatureBase64 = bitmapToBase64(signatureBitmap)
                                    val signaturePayload = "data:image/png;base64,$signatureBase64"

                                    val request = AbsenRequest(
                                        userId = userId,
                                        pin = pin,
                                        acaraId = currentAcaraId, // ID acara dinamis dari GET /api/acara.php!
                                        tandaTangan = signaturePayload
                                    )

                                    // Kirim POST ke absen.php
                                    val response = withContext(Dispatchers.IO) {
                                        ApiClient.apiService.submitAbsen(request)
                                    }

                                    val responseBody = response.body()
                                    if (response.isSuccessful && responseBody != null) {
                                        if (responseBody.success != false) {
                                            // Backup lokal SQLite
                                            val db = AttendanceDatabaseHelper(context)
                                            val colorHex = String.format("#%06X", (0xFFFFFF and selectedColor.hashCode()))
                                            db.saveMeetingAttendance(nama, jabatan, null, colorHex)

                                            Toast.makeText(context, "✅ ${responseBody.message ?: "Absen Berhasil!"}", Toast.LENGTH_LONG).show()
                                            onSavedSuccess()
                                        } else {
                                            Toast.makeText(context, "❌ Gagal: ${responseBody.message ?: "PIN Salah"}", Toast.LENGTH_LONG).show()
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
                        enabled = !isSubmitting && !isAcaraLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
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
                                text = "Kirim Absen ke Server Kantor",
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
