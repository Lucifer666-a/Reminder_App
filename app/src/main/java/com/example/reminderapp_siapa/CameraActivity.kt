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
import androidx.camera.core.Preview as CameraXPreview
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

                        // 2. Tandai presensi di AttendanceDatabaseHelper lokal
                        val attendanceDb = AttendanceDatabaseHelper(this)
                        val today = LocalDate.now()
                        val isPagi = photoType.contains("PAGI", ignoreCase = true)
                        if (isPagi) {
                            attendanceDb.markAttendancePagi(today)
                        } else {
                            attendanceDb.markAttendanceSore(today)
                        }

                        // 3. Kirim REST API ke server kantor (absen_harian.php) SETELAH foto diambil
                        val sharedPref = getSharedPreferences("app_user_prefs", MODE_PRIVATE)
                        val userId = sharedPref.getInt("KEY_USER_ID", 1)
                        val aksiValue = if (isPagi) "checkin" else "checkout"
                        val sesiValue = if (isPagi) "pagi" else "sore"

                        lifecycleScope.launch {
                            try {
                                val request = ApelAbsenRequest(
                                    userId = userId,
                                    aksi = aksiValue,
                                    sesi = sesiValue,
                                    tandaTangan = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
                                )
                                val response = withContext(Dispatchers.IO) {
                                    ApiClient.apiService.submitAbsenHarian(request)
                                }
                                val body = response.body()
                                if (response.isSuccessful && body?.success != false) {
                                    Toast.makeText(this@CameraActivity, "✅ ${body?.message ?: "Presensi & Foto Berhasil Disimpan!"}", Toast.LENGTH_LONG).show()
                                } else {
                                    val errStr = parseErrorMessage(response)
                                    Toast.makeText(this@CameraActivity, "📸 Foto Disimpan Lokal ($errStr)", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this@CameraActivity, "📸 Presensi & Foto Disimpan!", Toast.LENGTH_SHORT).show()
                            } finally {
                                finish()
                            }
                        }
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
    onPhotoConfirmed: (File) -> Unit = {},
    onCancelClick: () -> Unit = {}
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Izin Kamera Dibutuhkan untuk Mengambil Foto Presensi",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedPhotoFile == null) {
            // Tampilan Preview Kamera Live
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = CameraXPreview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                ctx as LifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Header Overlay Kamera
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 48.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Presensi Foto $photoType",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Posisikan wajah Anda di dalam frame",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onCancelClick() }
                    ) {
                        Text(text = "✕", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tombol Jepret Kamera 📸
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(4.dp, Color(0xFF1C483A)),
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            val photoFile = File(
                                context.getExternalFilesDir(null),
                                "PRESENSI_${photoType}_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture?.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        capturedPhotoFile = photoFile
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        exc.printStackTrace()
                                    }
                                }
                            )
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF1C483A), CircleShape)
                        )
                    }
                }
            }
        } else {
            // Preview Hasil Foto yang Baru Saja Diambil
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Konfirmasi Foto Presensi $photoType",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Tampilan Foto
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    AsyncImage(
                        model = capturedPhotoFile,
                        contentDescription = "Hasil Foto Presensi",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Tombol Ulangi / Konfirmasi Foto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { capturedPhotoFile = null },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(text = "🔄 Foto Ulang", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            capturedPhotoFile?.let { file ->
                                onPhotoConfirmed(file)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(text = "✅ Gunakan Foto", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
