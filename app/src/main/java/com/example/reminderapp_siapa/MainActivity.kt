package com.example.reminderapp_siapa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Meminta Izin Notifikasi untuk Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Memasang 2 alarm otomatis (08:00 Pagi & 16:00 Sore)
        ReminderScheduler.setupDefaultAbsenAlarms(this)

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                var currentScreen by remember { mutableStateOf("login") }

                when (currentScreen) {
                    "login" -> LoginScreen(onLoginSuccess = { currentScreen = "home" })
                    "home" -> HomeScreen(onLookPresentClick = { currentScreen = "look_present" })
                    "look_present" -> LookPresentScreen(onBackClick = { currentScreen = "home" })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}
) {
    var nama by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF989694))
    ) {
        // Lingkaran dekoratif bulat tegas di background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Lingkaran KANAN ATAS (Bulat Tegas dengan Gradien Dalam)
            val radius1 = size.width * 0.45f
            val center1 = Offset(size.width * 0.9f, size.height * 0.22f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFF606060)),
                    center = Offset(center1.x - radius1 * 0.3f, center1.y - radius1 * 0.3f),
                    radius = radius1 * 1.3f
                ),
                center = center1,
                radius = radius1
            )

            // 2. Lingkaran KIRI BAWAH (Bulat Tegas)
            val radius2 = size.width * 0.45f
            val center2 = Offset(size.width * 0.05f, size.height * 0.72f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFF606060)),
                    center = Offset(center2.x - radius2 * 0.3f, center2.y - radius2 * 0.3f),
                    radius = radius2 * 1.3f
                ),
                center = center2,
                radius = radius2
            )

            // 3. Lingkaran KANAN BAWAH (Bulat Tegas)
            val radius3 = size.width * 0.40f
            val center3 = Offset(size.width * 0.92f, size.height * 0.95f)
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

        // Form Login di Tengah Screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .offset(y = (-280).dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Input Nama
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                placeholder = {
                    Text(
                        text = "Masukkan Nama....",
                        color = Color(0xFF757575),
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE2E2E2),
                    unfocusedContainerColor = Color(0xFFE2E2E2),
                    focusedBorderColor = Color(0xFF626060),
                    unfocusedBorderColor = Color(0xFFFAF5F5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Input Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "Masukkan Password....",
                        color = Color(0xFF757575),
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE2E2E2),
                    unfocusedContainerColor = Color(0xFFE2E2E2),
                    focusedBorderColor = Color(0xFF626060),
                    unfocusedBorderColor = Color(0xFFFAF5F5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tombol Login
            Button(
                onClick = { onLoginSuccess() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF989B96)
                ),
                border = BorderStroke(1.dp, Color.Black),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier
                    .width(150.dp)
                    .height(46.dp)
            ) {
                Text(
                    text = "Login",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    Reminderapp_SIAPATheme {
        LoginScreen()
    }
}

