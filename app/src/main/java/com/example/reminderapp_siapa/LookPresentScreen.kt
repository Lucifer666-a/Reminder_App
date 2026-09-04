package com.example.reminderapp_siapa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LookPresentScreen(
    onBackClick: () -> Unit = {}
) {
    // State bulan yang sedang dipilih (Dinamis dengan Calendar API java.time)
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }

    // Contoh data tanggal kehadiran dinamis
    val presentDates = remember(currentYearMonth) {
        val list = mutableSetOf<LocalDate>()
        for (day in 1..currentYearMonth.lengthOfMonth()) {
            if (day % 2 == 1 && day <= today.dayOfMonth) {
                list.add(currentYearMonth.atDay(day))
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF989694))
    ) {
        // Background Lingkaran Dekoratf 3D
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius1 = size.width * 0.45f
            val center1 = Offset(size.width * 0.9f, size.height * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFF606060)),
                    center = Offset(center1.x - radius1 * 0.3f, center1.y - radius1 * 0.3f),
                    radius = radius1 * 1.3f
                ),
                center = center1,
                radius = radius1
            )

            val radius2 = size.width * 0.45f
            val center2 = Offset(size.width * 0.05f, size.height * 0.68f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFF606060)),
                    center = Offset(center2.x - radius2 * 0.3f, center2.y - radius2 * 0.3f),
                    radius = radius2 * 1.3f
                ),
                center = center2,
                radius = radius2
            )

            val radius3 = size.width * 0.40f
            val center3 = Offset(size.width * 0.92f, size.height * 0.92f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFF606060)),
                    center = Offset(center3.x - radius3 * 0.3f, center3.y - radius3 * 0.3f),
                    radius = radius3 * 1.3f
                ),
                center = center3,
                radius = radius3
            )
        }

        // Konten Utama Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 44.dp, bottom = 20.dp)
        ) {
            // Header Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Tombol Kembali (Back)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                        .clickable { onBackClick() }
                ) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Daftar Hadir",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Month Navigation Bar (Navigasi Pindah Bulan)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BorderStroke(1.dp, Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Bulan Sebelumnya (<)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF333333), CircleShape)
                            .clickable { currentYearMonth = currentYearMonth.minusMonths(1) }
                    ) {
                        Text("<", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Nama Bulan & Tahun (dinamis, misal: "Oktober 2024")
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
                            .background(Color(0xFF333333), CircleShape)
                            .clickable { currentYearMonth = currentYearMonth.plusMonths(1) }
                    ) {
                        Text(">", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Tabel Kalender Per Bulan (Dinamis 7 Kolom) - Mengikuti Tinggi Konten
            MonthCalendarTable(
                yearMonth = currentYearMonth,
                today = today,
                presentDates = presentDates
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Download PDF Presensi
            Button(
                onClick = { /* Action Download PDF */ },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDCE6D9)
                ),
                border = BorderStroke(1.dp, Color.Black),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📄 Download PDF Presensi",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}

@Composable
fun MonthCalendarTable(
    yearMonth: YearMonth,
    today: LocalDate,
    presentDates: Set<LocalDate>,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.5.dp, Color.Black),
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
                        color = Color.White,
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
                                val isToday = date == today
                                val isPresent = date in presentDates

                                val bgColor = when {
                                    isToday -> Color(0xFFFFC107)    // Kuning untuk Hari Ini
                                    isPresent -> Color(0xFF4CAF50)  // Hijau untuk Hadir
                                    else -> Color(0xFF424242)       // Abu-abu untuk Absen/Libur
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
                                        color = Color.White
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
