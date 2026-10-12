package com.example.reminderapp_siapa

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Helper universal untuk mengekstrak dan memasukkan tanggal riwayat (harian & apel dari server ke SQLite)
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
                obj.has("riwayat_apel") -> "riwayat_apel"
                obj.has("apel_pagi") -> "apel_pagi"
                obj.has("apel_sore") -> "apel_sore"
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
            if (obj.has("apel_bulan_ini") && obj.get("apel_bulan_ini").isJsonObject) {
                val apelObj = obj.getAsJsonObject("apel_bulan_ini")
                if (apelObj.has("pagi") && apelObj.get("pagi").isJsonArray) {
                    apelObj.getAsJsonArray("pagi").forEach {
                        try { db.markApelPagi(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                if (apelObj.has("sore") && apelObj.get("sore").isJsonArray) {
                    apelObj.getAsJsonArray("sore").forEach {
                        try { db.markApelSore(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                    }
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
                    val tipe = itemObj.get("tipe")?.asString?.lowercase() ?: ""
                    val kategori = itemObj.get("kategori")?.asString?.lowercase() ?: ""

                    if (jenis.contains("rapat") || jenis.contains("acara") || sesi.contains("rapat")) {
                        return@forEach
                    }

                    if (!dateStr.isNullOrBlank() && dateStr.lowercase() != "null") {
                        try {
                            val cleanDateStr = dateStr.trim().take(10)
                            val date = LocalDate.parse(cleanDateStr)

                            val isApel = jenis.contains("apel") || sesi.contains("apel") || tipe.contains("apel") || kategori.contains("apel")
                            val isSore = jenis.contains("sore") || sesi.contains("sore") || sesi.contains("pulang") || jenis.contains("pulang")

                            if (isApel) {
                                if (isSore) {
                                    db.markApelSore(date)
                                } else {
                                    db.markApelPagi(date)
                                }
                            } else {
                                if (isSore && checkout != null) {
                                    db.markAttendanceSore(date, checkout)
                                } else if (isPagi(jenis, sesi) && checkin != null) {
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

fun isPagi(jenis: String, sesi: String): Boolean {
    return jenis.contains("pagi") || jenis.contains("checkin") || jenis.contains("masuk") || sesi.contains("pagi") || sesi.contains("checkin")
}

fun getMondaysOfMonth(yearMonth: YearMonth): List<LocalDate> {
    val list = mutableListOf<LocalDate>()
    var date = yearMonth.atDay(1)
    while (date.month == yearMonth.month) {
        if (date.dayOfWeek == DayOfWeek.MONDAY) {
            list.add(date)
        }
        date = date.plusDays(1)
    }
    return list
}

fun getFridaysOfMonth(yearMonth: YearMonth): List<LocalDate> {
    val list = mutableListOf<LocalDate>()
    var date = yearMonth.atDay(1)
    while (date.month == yearMonth.month) {
        if (date.dayOfWeek == DayOfWeek.FRIDAY) {
            list.add(date)
        }
        date = date.plusDays(1)
    }
    return list
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
    val currentYearMonth = remember { YearMonth.now() }

    val sharedPref = remember { context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE) }
    val userId = remember { sharedPref.getInt("KEY_USER_ID", 1) }

    var syncTrigger by remember { mutableStateOf(0) }
    var rapatStatusText by remember { mutableStateOf("Memuat status rapat...") }

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

    // Narik riwayat & status harian dari server (GET /absen/api/user_status.php & acara.php)
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

                    // Cek Acara Rapat Aktif (GET /api/acara.php)
                    try {
                        val responseAcara = ApiClient.apiService.getAcara()
                        if (responseAcara.isSuccessful) {
                            val element = responseAcara.body()
                            val eventName = parseEventFromJson(element)
                            withContext(Dispatchers.Main) {
                                if (!eventName.isNullOrBlank() && eventName.lowercase() != "null" && !eventName.contains("Tidak ada", ignoreCase = true)) {
                                    rapatStatusText = "1 rapat sedang dibuka"
                                } else {
                                    rapatStatusText = "Tidak ada rapat yang sedang dibuka"
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                rapatStatusText = "Tidak ada rapat yang sedang dibuka"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            rapatStatusText = "Tidak ada rapat yang sedang dibuka"
                        }
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

    // Logika Status Time Window Masuk & Pulang
    val nowTime = currentTime.toLocalTime()

    val pagiStatusText = when {
        isPagiAttended -> "✓ Sudah"
        nowTime.isBefore(LocalTime.of(7, 0)) -> "Belum"
        nowTime.isAfter(LocalTime.of(8, 10)) -> "Tidak Absen"
        else -> "Saatnya Absen"
    }
    val pagiStatusColor = when {
        isPagiAttended -> Color(0xFF166534)
        nowTime.isAfter(LocalTime.of(8, 10)) -> Color(0xFF991B1B)
        else -> Color(0xFFD97706)
    }

    val soreStatusText = when {
        isSoreAttended -> "✓ Sudah"
        nowTime.isBefore(LocalTime.of(16, 15)) -> "Belum"
        nowTime.isAfter(LocalTime.of(17, 30)) -> "Tidak Absen"
        else -> "Saatnya Absen"
    }
    val soreStatusColor = when {
        isSoreAttended -> Color(0xFF166534)
        nowTime.isAfter(LocalTime.of(17, 30)) -> Color(0xFF991B1B)
        else -> Color(0xFFD97706)
    }

    // Apel Dates
    val apelPagiDates = remember(isPreview, syncTrigger) {
        if (isPreview) emptySet() else AttendanceDatabaseHelper(context).getAllApelPagiDates()
    }
    val apelSoreDates = remember(isPreview, syncTrigger) {
        if (isPreview) emptySet() else AttendanceDatabaseHelper(context).getAllApelSoreDates()
    }

    val mondays = remember(currentYearMonth) { getMondaysOfMonth(currentYearMonth) }
    val fridays = remember(currentYearMonth) { getFridaysOfMonth(currentYearMonth) }

    val totalApelCount = mondays.size + fridays.size
    val attendedApelCount = mondays.count { apelPagiDates.contains(it) } + fridays.count { apelSoreDates.contains(it) }

    // Dialog konfirmasi menunggu
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
            containerColor = Color.White,
            title = {
                Text(
                    text = "Konfirmasi Absensi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "Sudah absen di web resmi?",
                    fontSize = 15.sp,
                    color = Color.Black
                )
            },
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
                    Text("Sudah (Ambil Foto)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWaitingConfirmationDialog = false }
                ) {
                    Text("Belum / Tetap Menunggu", color = Color.Black, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Format Tanggal Header Ringkas
    val headerDateStr = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")))
    val monthNameStr = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID")))

    // Cek apakah jendela Apel sedang dibuka (Senin 07:45 - 08:30 atau Jumat 16:10 - 17:00)
    val isApelPagiWindow = today.dayOfWeek == DayOfWeek.MONDAY && currentTime.toLocalTime().isAfter(LocalTime.of(7, 45)) && currentTime.toLocalTime().isBefore(LocalTime.of(8, 30)) && !apelPagiDates.contains(today)
    val isApelSoreWindow = today.dayOfWeek == DayOfWeek.FRIDAY && currentTime.toLocalTime().isAfter(LocalTime.of(16, 10)) && currentTime.toLocalTime().isBefore(LocalTime.of(17, 0)) && !apelSoreDates.contains(today)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F5FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. HEADER RINGKAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerDateStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Halo, $userName",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .clickable { onSettingsClick() }
                ) {
                    Text(text = "⚙️", fontSize = 18.sp)
                }
            }

            // 2. KARTU "SEDANG DIBUKA" (Hanya muncul saat jendela apel aktif)
            if (isApelPagiWindow || isApelSoreWindow) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAEB)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sedang dibuka",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isApelPagiWindow) "Apel Pagi Senin" else "Apel Sore Jumat",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isApelPagiWindow) "07:45 - 08:30 WIB" else "16:10 - 17:00 WIB",
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent(context, RegularAttendanceActivity::class.java).apply {
                                    putExtra("EXTRA_DATE", today.toString())
                                    putExtra("EXTRA_TYPE", if (isApelPagiWindow) "APEL_PAGI" else "APEL_SORE")
                                }
                                context.startActivity(intent)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Isi Sekarang",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. KARTU "APEL OKTOBER 2026" (Chip Tanggal Senin & Jumat)
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Apel $monthNameStr",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "$attendedApelCount dari $totalApelCount hadir",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }

                    // Apel Pagi (Senin)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Apel pagi, setiap Senin",
                            fontSize = 13.sp,
                            color = Color(0xFF7A97A0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mondays.forEach { date ->
                                val isAttended = apelPagiDates.contains(date)
                                val isToday = date == today
                                val isPast = date.isBefore(today)

                                val bg: Color
                                val fg: Color
                                val label: String
                                val clickAction: () -> Unit

                                when {
                                    isAttended -> {
                                        bg = Color(0xFFDCFCE7)
                                        fg = Color(0xFF166534)
                                        label = "✓ Sudah"
                                        clickAction = { Toast.makeText(context, "Anda sudah absen", Toast.LENGTH_SHORT).show() }
                                    }
                                    isToday -> {
                                        val nowTime = currentTime.toLocalTime()
                                        val isApelPagiTimeWindow = nowTime.isAfter(LocalTime.of(7, 30)) && nowTime.isBefore(LocalTime.of(8, 15))
                                        if (isApelPagiTimeWindow) {
                                            bg = Color(0xFFF1F5F9)
                                            fg = Color(0xFF64748B)
                                            label = "Dibuka"
                                            clickAction = {
                                                val intent = Intent(context, RegularAttendanceActivity::class.java).apply {
                                                    putExtra("EXTRA_DATE", today.toString())
                                                    putExtra("EXTRA_TYPE", "APEL_PAGI")
                                                }
                                                context.startActivity(intent)
                                            }
                                        } else {
                                            bg = Color(0xFFF1F5F9)
                                            fg = Color(0xFF64748B)
                                            label = "Dibuka"
                                            clickAction = { Toast.makeText(context, "Absen apel pagi hanya bisa dilakukan pada jam 07:30 - 08:15", Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                    isPast -> {
                                        bg = Color(0xFFFEE2E2)
                                        fg = Color(0xFF991B1B)
                                        label = "✕ Lewat"
                                        clickAction = { Toast.makeText(context, "Apel sudah lewat", Toast.LENGTH_SHORT).show() }
                                    }
                                    else -> {
                                        bg = Color(0xFFF1F5F9)
                                        fg = Color(0xFF64748B)
                                        label = "Belum"
                                        clickAction = { Toast.makeText(context, "Belum waktunya apel", Toast.LENGTH_SHORT).show() }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clickable { clickAction() }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fg
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = fg
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Apel Sore (Jumat)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Apel sore, setiap Jumat",
                            fontSize = 13.sp,
                            color = Color(0xFF7A97A0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fridays.forEach { date ->
                                val isAttended = apelSoreDates.contains(date)
                                val isToday = date == today
                                val isPast = date.isBefore(today)

                                val bg: Color
                                val fg: Color
                                val label: String
                                val clickAction: () -> Unit

                                when {
                                    isAttended -> {
                                        bg = Color(0xFFDCFCE7)
                                        fg = Color(0xFF166534)
                                        label = "✓ Sudah"
                                        clickAction = { Toast.makeText(context, "Anda sudah absen", Toast.LENGTH_SHORT).show() }
                                    }
                                    isToday -> {
                                        val nowTime = currentTime.toLocalTime()
                                        val isApelSoreTimeWindow = nowTime.isAfter(LocalTime.of(16, 15)) && nowTime.isBefore(LocalTime.of(17, 30))
                                        if (isApelSoreTimeWindow) {
                                            bg = Color(0xFFF1F5F9)
                                            fg = Color(0xFF64748B)
                                            label = "Dibuka"
                                            clickAction = {
                                                val intent = Intent(context, RegularAttendanceActivity::class.java).apply {
                                                    putExtra("EXTRA_DATE", today.toString())
                                                    putExtra("EXTRA_TYPE", "APEL_SORE")
                                                }
                                                context.startActivity(intent)
                                            }
                                        } else {
                                            bg = Color(0xFFF1F5F9)
                                            fg = Color(0xFF64748B)
                                            label = "Dibuka"
                                            clickAction = { Toast.makeText(context, "Absen apel sore baru bisa dilakukan pada jam 16:15 - 17:30", Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                    isPast -> {
                                        bg = Color(0xFFFEE2E2)
                                        fg = Color(0xFF991B1B)
                                        label = "✕ Lewat"
                                        clickAction = { Toast.makeText(context, "Apel sudah lewat", Toast.LENGTH_SHORT).show() }
                                    }
                                    else -> {
                                        bg = Color(0xFFF1F5F9)
                                        fg = Color(0xFF64748B)
                                        label = "Belum"
                                        clickAction = { Toast.makeText(context, "Belum waktunya apel", Toast.LENGTH_SHORT).show() }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clickable { clickAction() }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fg
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = fg
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLookPresentClick() }
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Lihat detail dan foto ›",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C483A)
                        )
                    }
                }
            }

            // 4. KARTU "RAPAT" (Dinamis dari GET /api/acara.php)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rapat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rapatStatusText,
                            fontSize = 13.sp,
                            color = if (rapatStatusText.contains("sedang dibuka")) Color(0xFF1C483A) else Color(0xFF7A97A0),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { onMeetingAttendanceClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Absen Rapat",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 5. KARTU "PRESENSI KERJA HARI INI"
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
                    Text(
                        text = "Presensi kerja hari ini",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Kotak Masuk
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPagiAttended) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable {
                                    if (isPagiAttended) {
                                        Toast.makeText(context, "Owner / Anda sudah absen pagi", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onJumpWebClick()
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Masuk",
                                    fontSize = 12.sp,
                                    color = if (isPagiAttended) Color(0xFF166534) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = pagiTime ?: "--:--",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPagiAttended) Color(0xFF166534) else Color.Black
                                )
                                Text(
                                    text = pagiStatusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pagiStatusColor
                                )
                            }
                        }

                        // Kotak Pulang
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSoreAttended) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable {
                                    if (isSoreAttended) {
                                        Toast.makeText(context, "Anda sudah absen sore", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onJumpWebClick()
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pulang",
                                    fontSize = 12.sp,
                                    color = if (isSoreAttended) Color(0xFF166534) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = soreTime ?: "--:--",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSoreAttended) Color(0xFF166534) else Color.Black
                                )
                                Text(
                                    text = soreStatusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = soreStatusColor
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onJumpWebClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Buka Web Absensi Resmi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
