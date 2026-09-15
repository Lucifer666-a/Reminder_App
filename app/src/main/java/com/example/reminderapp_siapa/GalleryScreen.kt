package com.example.reminderapp_siapa

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.reminderapp_siapa.ui.theme.Reminderapp_SIAPATheme
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun GalleryScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // Ambil daftar foto dari PhotoDatabaseHelper
    val photoList = remember(isPreview) {
        if (isPreview) {
            listOf(
                PhotoRecord(
                    id = 1,
                    filePath = "",
                    dateStr = "2025-02-24",
                    timeStr = "08:00:15 WIB",
                    photoType = "PAGI",
                    timestamp = System.currentTimeMillis()
                ),
                PhotoRecord(
                    id = 2,
                    filePath = "",
                    dateStr = "2025-02-24",
                    timeStr = "16:02:10 WIB",
                    photoType = "SORE",
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            val photoDb = PhotoDatabaseHelper(context)
            photoDb.getAllPhotos()
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
                    text = "Galeri Foto Presensi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (photoList.isEmpty()) {
                // Empty State jika belum ada foto
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "📷",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "Belum Ada Foto Presensi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C483A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Foto hasil tangkapan kamera saat absen akan otomatis tersimpan di sini.",
                            fontSize = 13.sp,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Daftar Foto Presensi dalam LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(photoList, key = { it.id }) { photo ->
                        PhotoCardItem(
                            photo = photo,
                            onDownloadClick = {
                                val success = saveImageToPublicGallery(context, File(photo.filePath))
                                if (success) {
                                    Toast.makeText(context, "Foto berhasil disimpan ke Galeri HP!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan foto atau file tidak ditemukan.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCardItem(
    photo: PhotoRecord,
    onDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tampilan Foto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFEDFAFD), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (photo.filePath.isNotBlank() && File(photo.filePath).exists()) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = "Foto Presensi ${photo.dateStr}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📷", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Preview Foto", fontSize = 12.sp, color = Color(0xFF7A97A0))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rincian Informasi Tanggal & Jam Foto Diambil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📅 ${photo.dateStr}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⏰ Jam: ${photo.timeStr}",
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C483A).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Shift: ${photo.photoType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C483A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Download ke Galeri HP
            Button(
                onClick = { onDownloadClick() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C483A)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💾 Simpan ke Galeri HP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Menyimpan file foto dari penyimpanan internal aplikasi ke Galeri Publik HP
 */
fun saveImageToPublicGallery(context: Context, imageFile: File): Boolean {
    return try {
        if (!imageFile.exists()) return false

        val fileName = "PRESENSI_${System.currentTimeMillis()}.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PresensiSIAPA")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

            resolver.openOutputStream(imageUri)?.use { outputStream ->
                FileInputStream(imageFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
            true
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "PresensiSIAPA").apply { mkdirs() }
            val destFile = File(appDir, fileName)

            FileInputStream(imageFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GalleryScreenPreview() {
    Reminderapp_SIAPATheme {
        GalleryScreen()
    }
}
