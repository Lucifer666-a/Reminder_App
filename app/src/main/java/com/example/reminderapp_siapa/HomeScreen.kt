package com.example.reminderapp_siapa

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String = "Admin",
    onLookPresentClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val today = remember { LocalDate.now() }

    // Ambil jam absen dari database lokal (pagi dan sore)
    val pagiTime = remember(isPreview, today) {
        if (isPreview) null else {
            val db = AttendanceDatabaseHelper(context)
            db.getPagiAttendanceTime(today)
        }
    }

    val soreTime = remember(isPreview, today) {
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

    val pagiWeeklyCount = remember(isPreview, today) {
        if (isPreview) 4 else {
            val db = AttendanceDatabaseHelper(context)
            val dates = db.getAllPagiAttendanceDates()
            dates.count { !it.isBefore(monday) && !it.isAfter(sunday) }
        }
    }

    val soreWeeklyCount = remember(isPreview, today) {
        if (isPreview) 3 else {
            val db = AttendanceDatabaseHelper(context)
            val dates = db.getAllSoreAttendanceDates()
            dates.count { !it.isBefore(monday) && !it.isAfter(sunday) }
        }
    }

    // State untuk ticking countdown per detik
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(isPreview) {
        if (!isPreview) {
            while (true) {
                currentTime = LocalDateTime.now()
                delay(1000L)
            }
        }
    }

    // Logika menentukan target absen selanjutnya & interval awal/akhir untuk progress
    val (nextTitle, nextTargetTime, startTime) = remember(isPagiAttended, isSoreAttended, currentTime) {
        val todayDate = currentTime.toLocalDate()
        val pagiTarget = LocalDateTime.of(todayDate, LocalTime.of(8, 0))
        val soreTarget = LocalDateTime.of(todayDate, LocalTime.of(16, 0))
        val besokPagiTarget = LocalDateTime.of(todayDate.plusDays(1), LocalTime.of(8, 0))

        if (!isPagiAttended) {
            if (currentTime.isBefore(pagiTarget)) {
                Triple("Absen Masuk (08:00 WIB)", pagiTarget, LocalDateTime.of(todayDate, LocalTime.of(0, 0)))
            } else if (currentTime.isBefore(soreTarget)) {
                Triple("Absen Pulang (16:00 WIB)", soreTarget, pagiTarget)
            } else {
                Triple("Absen Masuk Besok (08:00 WIB)", besokPagiTarget, soreTarget)
            }
        } else if (!isSoreAttended) {
            if (currentTime.isBefore(soreTarget)) {
                Triple("Absen Pulang (16:00 WIB)", soreTarget, pagiTarget)
            } else {
                Triple("Absen Masuk Besok (08:00 WIB)", besokPagiTarget, soreTarget)
            }
        } else {
            Triple("Absen Masuk Besok (08:00 WIB)", besokPagiTarget, soreTarget)
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

    // Format Hari & Tanggal
    val dayOfWeek = today.dayOfWeek
    val isWorkingDay = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
    val statusLabel = if (isWorkingDay) "Hari Kerja" else "Hari Libur"
    val statusColor = if (isWorkingDay) Color(0xFF22C55E) else Color(0xFFEF4444)

    val dayNumberStr = today.dayOfMonth.toString()
    val monthNameStr = remember(today) {
        today.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("id-ID")))
    }

    // Root Container dengan Background Cyan Pastel Terang
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
            // 1. HEADER HERO UTAMA ATAS (PUTIH LEBAR SAMPAI UJUNG)
            Card(
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 52.dp, bottom = 32.dp)
                ) {
                    // Row Atas: Halo Admin & Status Hari Kerja
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Halo,",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = userName,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.Black
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Tanggal Besar (Angka & Bulan) Dikebwahin Lagi
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

            // KONTEN BAWAH (DENGAN PADDING SAMPING)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. KARTU TENGAH (HIJAU MUDA) - Status Absen & Countdown
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

                        Text(
                            text = nextTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1B4D2E).copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = countdownText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F381D)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF1B4D2E),
                            trackColor = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // 3. GRID BAWAH (2 KOLOM: Kiri Riwayat, Kanan Absen Masuk & Pulang)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Kolom Kiri: Riwayat Absen (Hijau Tua Pekat)
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
                                // Stat 1: Pagi
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

                                // Stat 2: Sore
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

                    // Kolom Kanan: 2 Card Bertumpuk (Absen Masuk & Absen Pulang)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card Absen Masuk (Teal Green)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A9376)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
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

                        // Card Absen Pulang (Emerald Green)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18A86C)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
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

                // Tombol Akses Cepat di Bagian Bawah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { /* Action Jump to Web */ },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFC0E0D5)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Jump to Web",
                            color = Color(0xFF1C483A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { onLookPresentClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Look Present",
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
