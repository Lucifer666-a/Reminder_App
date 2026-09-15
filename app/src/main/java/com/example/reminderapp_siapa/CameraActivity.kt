package com.example.reminderapp_siapa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val photoType = intent.getStringExtra("EXTRA_PHOTO_TYPE") ?: if (LocalTime.now().hour < 12) "PAGI" else "SORE"

        enableEdgeToEdge()
        setContent {
            Reminderapp_SIAPATheme {
                CameraScreen(
                    photoType = photoType,
                    cameraExecutor = cameraExecutor,
                    onPhotoConfirmed = { photoFile ->
                        // 1. Simpan foto ke PhotoDatabaseHelper
                        val photoDb = PhotoDatabaseHelper(this)
                        photoDb.savePhoto(
                            filePath = photoFile.absolutePath,
                            photoType = photoType
                        )

                        // 2. Tandai presensi di AttendanceDatabaseHelper
                        val attendanceDb = AttendanceDatabaseHelper(this)
                        val today = LocalDate.now()
                        if (photoType.contains("PAGI", ignoreCase = true)) {
                            attendanceDb.markAttendancePagi(today)
                        } else {
                            attendanceDb.markAttendanceSore(today)
                        }

                        Toast.makeText(this, "Presensi & Foto Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onCancelClick = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraScreen(
    photoType: String,
    cameraExecutor: ExecutorService,
    onPhotoConfirmed: (File) -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Layar Permintaan Izin Kamera
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE6F5FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "📷 Izin Kamera Diperlukan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C483A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aplikasi membutuhkan akses kamera untuk mengambil foto verifikasi presensi.",
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A))
                ) {
                    Text("Izinkan Kamera", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Fitur Kamera Utama & Review Foto
        CameraContent(
            photoType = photoType,
            cameraExecutor = cameraExecutor,
            onPhotoConfirmed = onPhotoConfirmed,
            onCancelClick = onCancelClick
        )
    }
}

@Composable
fun CameraContent(
    photoType: String,
    cameraExecutor: ExecutorService,
    onPhotoConfirmed: (File) -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedPhotoFile == null) {
            // --- TAMPILAN 1: KAMERA LIVE PREVIEW ---
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Header Top Bar Kamera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Batal/Tutup
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable { onCancelClick() }
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Foto Absen ($photoType)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Tombol Switch Kamera Depan/Belakang
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        }
                ) {
                    Text("🔄", fontSize = 18.sp)
                }
            }

            // Tombol Capture Foto 📷 di Bawah
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color(0xFF1C483A), CircleShape)
                        .clickable {
                            val photoFile = File(
                                context.filesDir,
                                "presensi_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions
                                .Builder(photoFile)
                                .build()

                            imageCapture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        capturedPhotoFile = photoFile
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF1C483A), CircleShape)
                    )
                }
            }
        } else {
            // --- TAMPILAN 2: REVIEW FOTO (RETAKE ATAU LANJUT) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE6F5FA))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Konfirmasi Foto Presensi",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )

                    // Kartu Preview Foto
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = capturedPhotoFile,
                                contentDescription = "Foto Presensi",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(28.dp))
                            )
                        }
                    }

                    // 2 Tombol: Foto Ulang (Retake) & Lanjut & Simpan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tombol 1: Foto Ulang (Retake)
                        Button(
                            onClick = {
                                capturedPhotoFile?.delete()
                                capturedPhotoFile = null
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🔄 Foto Ulang",
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Tombol 2: Lanjut & Simpan
                        Button(
                            onClick = {
                                capturedPhotoFile?.let { onPhotoConfirmed(it) }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "✅ Lanjut & Simpan",
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun CameraScreenPreview() {
    Reminderapp_SIAPATheme {
        CameraScreen(
            photoType = "PAGI",
            cameraExecutor = Executors.newSingleThreadExecutor(),
            onPhotoConfirmed = {},
            onCancelClick = {}
        )
    }
}
