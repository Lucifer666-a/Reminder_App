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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme

@Composable
fun HomeScreen(
    onLookPresentClick: () -> Unit = {}
) {
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
                text = "Halo, Nama...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp, bottom = 20.dp)
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

                    // Card Status Absen (Border Hijau Cerah)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFF6E6E6E), RoundedCornerShape(22.dp))
                            .border(2.dp, Color(0xFF39FF14), RoundedCornerShape(22.dp))
                    )

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
