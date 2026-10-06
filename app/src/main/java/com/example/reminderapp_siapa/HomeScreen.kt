package com.example.reminderapp_siapa

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Helper universal untuk mengekstrak dan memasukkan tanggal riwayat (harian & apel secara spesifik) ke SQLite
fun syncServerStatusToDb(element: JsonElement?, db: AttendanceDatabaseHelper) {
    if (element == null) return
    try {
        Log.d("SYNC_SERVER", "Response JSON: $element")
        if (element.isJsonObject) {
            val obj = element.asJsonObject

            val dataKey = when {
                obj.has("data") -> "data"
                obj.has("riwayat_terbaru") -> "riwayat_terbaru"
                obj.has("history") -> "history"
                obj.has("rows") -> "rows"
                obj.has("result") -> "result"
                obj.has("absensi") -> "absensi"
                obj.has("presensi_apel") -> "presensi_apel"
                obj.has("apel") -> "apel"
                else -> null
            }

            if (dataKey != null) {
                syncServerStatusToDb(obj.get(dataKey), db)
            }

            if (obj.has("pagi_dates") && obj.get("pagi_dates").isJsonArray) {
                obj.getAsJsonArray("pagi_dates").forEach {
                    try { db.markAttendancePagi(LocalDate.parse(it.asString.take(10)), "07:55 WIB") } catch (e: Exception) { e.printStackTrace() }
                }
            }
            if (obj.has("sore_dates") && obj.get("sore_dates").isJsonArray) {
                obj.getAsJsonArray("sore_dates").forEach {
                    try { db.markAttendanceSore(LocalDate.parse(it.asString.take(10)), "16:30 WIB") } catch (e: Exception) { e.printStackTrace() }
                }
            }
            if (obj.has("apel_pagi_dates") && obj.get("apel_pagi_dates").isJsonArray) {
                obj.getAsJsonArray("apel_pagi_dates").forEach {
                    try { db.markApelPagi(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                }
            }
            if (obj.has("apel_sore_dates") && obj.get("apel_sore_dates").isJsonArray) {
                obj.getAsJsonArray("apel_sore_dates").forEach {
                    try { db.markApelSore(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } else if (element.isJsonArray) {
            element.asJsonArray.forEach { item ->
                if (item.isJsonObject) {
                    val itemObj = item.asJsonObject
                    val dateStr = itemObj.get("tanggal")?.asString ?: itemObj.get("date")?.asString ?: itemObj.get("tgl")?.asString
                    val checkin = itemObj.get("waktu_checkin")?.asString 
                        ?: itemObj.get("checkin")?.asString 
                        ?: itemObj.get("jam_masuk")?.asString
                        ?: itemObj.get("waktu")?.asString
                    val checkout = itemObj.get("waktu_checkout")?.asString 
                        ?: itemObj.get("checkout")?.asString 
                        ?: itemObj.get("jam_keluar")?.asString

                    val jenis = itemObj.get("jenis")?.asString?.lowercase() ?: ""
                    val sesi = itemObj.get("sesi")?.asString?.lowercase() ?: ""

                    // Jangan biarkan absen rapat (absen.php) tercampur ke absensi harian pagi/sore!
                    if (jenis.contains("rapat") || jenis.contains("acara") || sesi.contains("rapat")) {
                        return@forEach
                    }

                    if (!dateStr.isNullOrBlank() && dateStr.lowercase() != "null") {
                        try {
                            val cleanDateStr = dateStr.trim().take(10)
                            val date = LocalDate.parse(cleanDateStr)

                            val isSore = jenis.contains("sore") || jenis.contains("checkout") || jenis.contains("pulang") || sesi.contains("sore") || !checkout.isNullOrBlank()
                            val isPagi = jenis.contains("pagi") || jenis.contains("checkin") || jenis.contains("masuk") || sesi.contains("pagi") || !checkin.isNullOrBlank()

                            if (jenis.contains("apel")) {
                                if (isSore) db.markApelSore(date) else db.markApelPagi(date)
                            } else {
                                if (isSore && checkout != null) {
                                    db.markAttendanceSore(date, checkout)
                                }
                                if (isPagi && checkin != null) {
                                    db.markAttendancePagi(date, checkin)
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun HomeScreen(
    userName: String = "Admin",
    onLookPresentClick: () -> Unit = {},
    onMeetingAttendanceClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onJumpWebClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val today = remember { LocalDate.now() }

    val sharedPref = remember { context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) }
    val userId = remember { sharedPref.getInt("KEY_USER_ID", 1) }

    // Dialog konfirmasi "Sudah absen di web resmi?" saat pegawai kembali ke aplikasi
    var showWaitingConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val status = sharedPref.getString("KEY_ATTENDANCE_STATUS_TODAY", "")
        if (status == "MENUNGGU_KONFIRMASI") {
            showWaitingConfirmationDialog = true
        }
    }

    if (showWaitingConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showWaitingConfirmationDialog = false },
            title = { Text("Konfirmasi Absensi", fontWeight = FontWeight.Bold, color = Color(0xFF1C483A)) },
            text = { Text("Sudah absen di web resmi?") },
            confirmButton = {
                Button(
                    onClick = {
                        showWaitingConfirmationDialog = false
                        sharedPref.edit().remove("KEY_ATTENDANCE_STATUS_TODAY").apply()
                        val session = sharedPref.getString("KEY_ATTENDANCE_SESSION", "pagi") ?: "pagi"
                        val photoType = if (session == "pagi") "PAGI" else "SORE"
                        val intent = Intent(context, CameraActivity::class.java).apply {
                            putExtra("EXTRA_PHOTO_TYPE", photoType)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                    shape = CircleShape
                ) {
                    Text("Sudah (Ambil Foto)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWaitingConfirmationDialog = false }
                ) {
                    Text("Belum / Tetap Menunggu", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    var syncTrigger by remember { mutableStateOf(0) }

    // State untuk ticking countdown per detik
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(isPreview) {
        if (!isPreview) {
            while (true) {
                currentTime = LocalDateTime.now()
                delay(1.seconds)
            }
        }
    }

    // Narik riwayat & status harian dari server (GET /absen/api/user_status.php?user_id=X)
    LaunchedEffect(userId) {
        if (!isPreview) {
            withContext(Dispatchers.IO) {
                try {
                    val db = AttendanceDatabaseHelper(context)

                    val responseStatus = ApiClient.apiService.getUserStatus(userId)
                    if (responseStatus.isSuccessful && responseStatus.body() != null) {
                        syncServerStatusToDb(responseStatus.body(), db)
                    }

                    try {
                        val responseHarian = ApiClient.apiService.getAbsenHarian(userId)
                        if (responseHarian.isSuccessful && responseHarian.body() != null) {
                            syncServerStatusToDb(responseHarian.body(), db)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        syncTrigger++
                    }
                }
            }
        }
    }

    // Ambil jam absen dari database lokal (pagi dan sore)
    val pagiTime = remember(isPreview, today, currentTime.second, syncTrigger) {
        if (isPreview) null else {
            val db = AttendanceDatabaseHelper(context)
            db.getPagiAttendanceTime(today)
        }
    }

    val soreTime = remember(isPreview, today, currentTime.second, syncTrigger) {
        if (isPreview) null else {
            val db = AttendanceDatabaseHelper(context)
            db.getSoreAttendanceTime(today)
        }
    }

    val isPagiAttended = pagiTime != null
    val isSoreAttended = soreTime != null

    // Hitung ringkasan mingguan (Senin - Minggu minggu ini)
    val monday = remember(today) { today.with(DayOfWeek.MONDAY) }
    val sunday = remember(today) { today.with(DayOfWeek.SUNDAY) }

    val pagiWeeklyCount = remember(isPreview, today, currentTime.second, syncTrigger) {
        if (isPreview) 4 else {
            val db = AttendanceDatabaseHelper(context)
            val dates = db.getAllPagiAttendanceDates()
            dates.count { !it.isBefore(monday) && !it.isAfter(sunday) }
        }
    }

    val soreWeeklyCount = remember(isPreview, today, currentTime.second, syncTrigger) {
        if (isPreview) 3 else {
            val db = AttendanceDatabaseHelper(context)
            val dates = db.getAllSoreAttendanceDates()
            dates.count { !it.isBefore(monday) && !it.isAfter(sunday) }
        }
    }

    // Cek apakah saat ini Hari Libur (Sabtu, Minggu, atau Jumat setelah jam/selesai absen sore 16:10)
    val todayDayOfWeek = today.dayOfWeek
    val isFridayAfterSore = (todayDayOfWeek == DayOfWeek.FRIDAY && (isSoreAttended || currentTime.toLocalTime().isAfter(LocalTime.of(16, 10))))
    val isSaturday = (todayDayOfWeek == DayOfWeek.SATURDAY)
    val isSundayDaytime = (todayDayOfWeek == DayOfWeek.SUNDAY && currentTime.hour < 18)

    val isWeekendHoliday = isSaturday || isSundayDaytime || isFridayAfterSore

    // Logika menentukan target absen selanjutnya & interval awal/akhir untuk progress (Dinamis berdasarkan Hari)
    val (nextTitle, nextTargetTime, startTime) = remember(isPagiAttended, isSoreAttended, currentTime) {
        val todayDate = currentTime.toLocalDate()
        val tomorrowDate = todayDate.plusDays(1)

        val pagiTimeTarget = if (todayDate.dayOfWeek == DayOfWeek.MONDAY) LocalTime.of(7, 45) else LocalTime.of(7, 55)
        val pagiTarget = LocalDateTime.of(todayDate, pagiTimeTarget)
        val pagiTitle = "Absen Masuk (${pagiTimeTarget.format(DateTimeFormatter.ofPattern("HH:mm"))} WIB)"

        val soreTimeTarget = if (todayDate.dayOfWeek == DayOfWeek.FRIDAY) LocalTime.of(16, 10) else LocalTime.of(16, 30)
        val soreTarget = LocalDateTime.of(todayDate, soreTimeTarget)
        val soreTitle = "Absen Pulang (${soreTimeTarget.format(DateTimeFormatter.ofPattern("HH:mm"))} WIB)"

        var nextWorkDate = tomorrowDate
        while (nextWorkDate.dayOfWeek == DayOfWeek.SATURDAY || nextWorkDate.dayOfWeek == DayOfWeek.SUNDAY) {
            nextWorkDate = nextWorkDate.plusDays(1)
        }
        val nextWorkPagiTime = if (nextWorkDate.dayOfWeek == DayOfWeek.MONDAY) LocalTime.of(7, 45) else LocalTime.of(7, 55)
        val besokPagiTarget = LocalDateTime.of(nextWorkDate, nextWorkPagiTime)
        val dayName = nextWorkDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("id-ID")))
        val besokPagiTitle = "Absen Masuk $dayName (${nextWorkPagiTime.format(DateTimeFormatter.ofPattern("HH:mm"))} WIB)"

        if (!isPagiAttended) {
            if (currentTime.isBefore(pagiTarget)) {
                Triple(pagiTitle, pagiTarget, LocalDateTime.of(todayDate, LocalTime.of(0, 0)))
            } else if (currentTime.isBefore(soreTarget)) {
                Triple(soreTitle, soreTarget, pagiTarget)
            } else {
                Triple(besokPagiTitle, besokPagiTarget, soreTarget)
            }
        } else if (!isSoreAttended) {
            if (currentTime.isBefore(soreTarget)) {
                Triple(soreTitle, soreTarget, pagiTarget)
            } else {
                Triple(besokPagiTitle, besokPagiTarget, soreTarget)
            }
        } else {
            Triple(besokPagiTitle, besokPagiTarget, soreTarget)
        }
    }

    // Hitung Progress (0.0f - 1.0f)
    val progressFloat = if (isPreview) {
        0.65f
    } else {
        val totalIntervalSec = Duration.between(startTime, nextTargetTime).seconds.coerceAtLeast(1L)
        val elapsedSec = Duration.between(startTime, currentTime).seconds.coerceIn(0L, totalIntervalSec)
        (elapsedSec.toFloat() / totalIntervalSec.toFloat()).coerceIn(0f, 1f)
    }

    // Hitung format Teks Countdown Sisa Waktu
    val countdownText = if (isPreview) {
        "07 jam 45 menit 00 detik"
    } else {
        val duration = Duration.between(currentTime, nextTargetTime)
        val totalSeconds = duration.seconds
        if (totalSeconds <= 0) {
            "Waktunya Absen!"
        } else {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            String.format(Locale.getDefault(), "%02d jam %02d menit %02d detik", hours, minutes, seconds)
        }
    }

    val dayOfWeek = today.dayOfWeek
    val isWorkingDay = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
    val statusLabel = if (isWorkingDay) "Hari Kerja" else "Hari Libur"
    val statusColor = if (isWorkingDay) Color(0xFF22C55E) else Color(0xFFEF4444)

    val dayNumberStr = today.dayOfMonth.toString()
    val monthNameStr = remember(today) {
        today.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("id-ID")))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F5FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Halo,",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            val displayName = if (userName.length > 8) "${userName.take(8)}..." else userName
                            Text(
                                text = displayName,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFE6F5FA), CircleShape)
                                    .clickable { onSettingsClick() }
                            ) {
                                Text(text = "⚙️", fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = dayNumberStr,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 76.sp
                    )
                    Text(
                        text = monthNameStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF98D882)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Status Absen",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B4D2E)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val displayTitle = if (isWeekendHoliday) "Hari Libur (Weekend)" else nextTitle
                        val displayCountdownText = if (isWeekendHoliday) "Selamat menikmati hari liburmu! 🎉" else countdownText
                        val displayProgress = if (isWeekendHoliday) 1.0f else progressFloat

                        Text(
                            text = displayTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1B4D2E).copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = displayCountdownText,
                            fontSize = if (isWeekendHoliday) 18.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F381D)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { displayProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF1B4D2E),
                            trackColor = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C483A)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(260.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Riwayat Absen",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA3C2B5)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Absen Masuk",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA3C2B5)
                                    )
                                    Text(
                                        text = "$pagiWeeklyCount / 5 Hari",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Absen Pulang",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA3C2B5)
                                    )
                                    Text(
                                        text = "$soreWeeklyCount / 5 Hari",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "Minggu Ini",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A9376)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                                .clickable { onJumpWebClick() }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Absen Masuk",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = pagiTime ?: "--:-- WIB",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isPagiAttended) "✅ Sudah" else "⏳ Belum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18A86C)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                                .clickable { onJumpWebClick() }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Absen Pulang",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = soreTime ?: "--:-- WIB",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isSoreAttended) "✅ Sudah" else "⏳ Belum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onLookPresentClick() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFC0E0D5)),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Absen Apel",
                                color = Color(0xFF1C483A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { onMeetingAttendanceClick() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "✍Absen Rapat",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onJumpWebClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🌐 Menuju Web Resmi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    Reminderapp_SIAPATheme {
        HomeScreen()
    }
}
