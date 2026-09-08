package com.example.reminderapp_siapa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LookPresentScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // State bulan yang sedang dipilih (Dinamis dengan Calendar API java.time)
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }

    // Ambil data tanggal kehadiran Pagi dan Sore secara terpisah dari SQLite
    val pagiDates = remember(currentYearMonth, isPreview) {
        if (isPreview) {
            val list = mutableSetOf<LocalDate>()
            for (day in 1..currentYearMonth.lengthOfMonth()) {
                if (day % 2 == 1 && day <= today.dayOfMonth) {
                    list.add(currentYearMonth.atDay(day))
                }
            }
            list
        } else {
            val db = AttendanceDatabaseHelper(context)
            db.getAllPagiAttendanceDates()
        }
    }

    val soreDates = remember(currentYearMonth, isPreview) {
        if (isPreview) {
            val list = mutableSetOf<LocalDate>()
            for (day in 1..currentYearMonth.lengthOfMonth()) {
                if ((day % 2 == 1 && day % 3 == 0) && day <= today.dayOfMonth) {
                    list.add(currentYearMonth.atDay(day))
                }
            }
            list
        } else {
            val db = AttendanceDatabaseHelper(context)
            db.getAllSoreAttendanceDates()
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
                .verticalScroll(rememberScrollState())
        ) {
            // Header Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                // Tombol Kembali (Back)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .clickable { onBackClick() }
                ) {
                    Text(
                        text = "←",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Daftar Hadir",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Month Navigation Bar (Navigasi Pindah Bulan)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C483A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Bulan Sebelumnya (<)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { currentYearMonth = currentYearMonth.minusMonths(1) }
                    ) {
                        Text("<", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Nama Bulan & Tahun (dinamis, misal: "Februari 2025")
                    val monthName = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID"))
                    Text(
                        text = "$monthName ${currentYearMonth.year}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Tombol Bulan Berikutnya (>)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { currentYearMonth = currentYearMonth.plusMonths(1) }
                    ) {
                        Text(">", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Tabel Kalender Per Bulan (Dinamis 7 Kolom)
            MonthCalendarTable(
                yearMonth = currentYearMonth,
                today = today,
                pagiDates = pagiDates,
                soreDates = soreDates
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tombol Download PDF Presensi
            Button(
                onClick = { /* Action Download PDF */ },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C483A)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📄 Download PDF Presensi",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun MonthCalendarTable(
    yearMonth: YearMonth,
    today: LocalDate,
    pagiDates: Set<LocalDate>,
    soreDates: Set<LocalDate>,
    modifier: Modifier = Modifier
) {
    // Dihitung otomatis dengan API java.time
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    
    // DayOfWeek: SUNDAY = 7, MONDAY = 1 ... -> Ubah agar Minggu berada di posisi paling awal (indeks 0)
    val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value % 7

    val dayHeaders = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

    val dayList = buildList<LocalDate?> {
        repeat(firstDayOfWeekIndex) { add(null) }
        for (day in 1..daysInMonth) {
            add(yearMonth.atDay(day))
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C483A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Nama Hari (Min, Sen, Sel, Rab, Kam, Jum, Sab)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA3C2B5),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Baris Grid Tanggal per Bulan (7 Kolom Per Baris)
            val rows = dayList.chunked(7)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (i in 0 until 7) {
                            val date = rowDays.getOrNull(i)
                            if (date != null) {
                                val hasPagi = date in pagiDates
                                val hasSore = date in soreDates
                                val attendanceCount = (if (hasPagi) 1 else 0) + (if (hasSore) 1 else 0)

                                val isToday = date == today
                                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

                                val bgColor = when {
                                    attendanceCount == 2 -> Color(0xFF18A86C) // Hijau Pekat (Lengkap 2x Absen)
                                    attendanceCount == 1 -> Color(0xFF4ADE80) // Hijau Muda (Baru 1x Absen)
                                    isToday -> Color(0xFFFFC107)               // Kuning Gold untuk Hari Ini
                                    isWeekend -> Color(0xFF224237)             // Warna Pudar untuk Weekend
                                    else -> Color(0xFF2B5948)                  // Dark Green Muted untuk Hari Kerja biasa
                                }

                                val textColor = when {
                                    attendanceCount == 1 -> Color(0xFF0F381D) // Teks Gelap Kontras di atas Hijau Muda
                                    isWeekend && attendanceCount == 0 && !isToday -> Color(0xFF8AA898) // Teks Pudar untuk Weekend
                                    else -> Color.White
                                }

                                val shape = if (isToday) CircleShape else RoundedCornerShape(8.dp)

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(bgColor, shape)
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            } else {
                                // Kotak kosong penyeimbang posisi hari
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LookPresentScreenPreview() {
    Reminderapp_SIAPATheme {
        LookPresentScreen()
    }
}
