package com.example.reminderapp_siapa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.time.LocalDate

@Composable
fun HomeScreen(
    userName: String = "Nama",
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF989694))
    ) {
        // Lingkaran dekoratif bulat tegas di background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Lingkaran KANAN ATAS
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

            // 2. Lingkaran KIRI BAWAH
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

            // 3. Lingkaran KANAN BAWAH
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

        // Layout Konten Utama
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 20.dp)
        ) {
            // Title Header "Halo, Nama..."
            Text(
                text = "Halo, $userName ",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp, bottom = 18.dp)
            )

            // Outer Container Card (Bingkai Utama dengan Border Hitam)
            Card(
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.5.dp, Color.Black),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF9E9C9A).copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Section 1: Status Absen
                    Text(
                        text = "Status Absen",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2 Kotak Terpisah (Kiri: Absen Masuk, Kanan: Absen Pulang)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Kotak 1: Kiri (Absen Masuk Pagi)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .background(Color(0xFF6E6E6E), RoundedCornerShape(22.dp))
                                .border(
                                    2.dp,
                                    if (isPagiAttended) Color(0xFF39FF14) else Color(0xFF626060),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Absen Masuk (08:00)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD0D0D0)
                                )
                                Text(
                                    text = pagiTime ?: "--:-- WIB",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPagiAttended) Color(0xFF39FF14).copy(alpha = 0.2f) else Color(0xFFFFC107).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (isPagiAttended) Color(0xFF39FF14) else Color(0xFFFFC107))
                                ) {
                                    Text(
                                        text = if (isPagiAttended) "✅ Sudah" else "⏳ Belum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPagiAttended) Color(0xFF39FF14) else Color(0xFFFFC107),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Kotak 2: Kanan (Absen Pulang Sore)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .background(Color(0xFF6E6E6E), RoundedCornerShape(22.dp))
                                .border(
                                    2.dp,
                                    if (isSoreAttended) Color(0xFF39FF14) else Color(0xFF626060),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Absen Pulang (16:00)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD0D0D0)
                                )
                                Text(
                                    text = soreTime ?: "--:-- WIB",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSoreAttended) Color(0xFF39FF14).copy(alpha = 0.2f) else Color(0xFFFFC107).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (isSoreAttended) Color(0xFF39FF14) else Color(0xFFFFC107))
                                ) {
                                    Text(
                                        text = if (isSoreAttended) "✅ Sudah" else "⏳ Belum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSoreAttended) Color(0xFF39FF14) else Color(0xFFFFC107),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 2: Absen Selanjutnya
                    Text(
                        text = "Absen Selanjutnya",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card Absen Selanjutnya (Abu-Abu)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF6E6E6E), RoundedCornerShape(22.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Bar Container (Kapsul Putih di Bawah)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFD0D0D0), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol 1: jump to web
                    Button(
                        onClick = { /* Action Jump To Web */ },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDCE6D9)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF888888)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = "jump to web",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Tombol 2: look present
                    Button(
                        onClick = { onLookPresentClick() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDCE6D9)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF888888)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    ) {
                        Text(
                            text = "look present",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
