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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LookPresentScreen(
    onBackClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onJumpWebClick: () -> Unit = {}
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

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
                        text = "Look Present",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )
                    Text(
                        text = "Kalender Kehadiran & Rekap Bulanan",
                        fontSize = 12.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            // Card Navigasi Bulan & Tahun
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C483A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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

                    // Nama Bulan & Tahun (Format Indonesia)
                    val monthYearStr = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID")))
                    Text(
                        text = monthYearStr,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Legenda / Keterangan Warna Kalender
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF18A86C), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("2x Absen", fontSize = 11.sp, color = Color(0xFF1C483A), fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF4ADE80), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1x Absen", fontSize = 11.sp, color = Color(0xFF1C483A), fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFC107), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hari Ini", fontSize = 11.sp, color = Color(0xFF1C483A), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Galeri Foto Presensi
            Button(
                onClick = { onGalleryClick() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C483A)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📷 Lihat Galeri Foto Presensi",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA3C2B5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val weeks = dayList.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until 7) {
                        val date = if (i < week.size) week[i] else null
                        if (date != null) {
                            val isPagi = pagiDates.contains(date)
                            val isSore = soreDates.contains(date)
                            val count = (if (isPagi) 1 else 0) + (if (isSore) 1 else 0)
                            val isToday = date == today

                            val bg = when {
                                count == 2 -> Color(0xFF18A86C) // 2x Absen (Dark Green)
                                count == 1 -> Color(0xFF4ADE80) // 1x Absen (Light Green)
                                isToday -> Color(0xFFFFC107)    // Hari Ini (Yellow)
                                else -> Color(0xFF14382D)       // Kosong
                            }

                            val textColor = if (isToday && count == 0) Color.Black else Color.White

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                   .weight(1f)
                                   .aspectRatio(1f)
                                   .padding(2.dp)
                                   .background(bg, RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
