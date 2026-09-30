package com.example.reminderapp_siapa

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // 1. Mengambil daftar acara yang buka (mengembalikan JsonElement agar fleksibel untuk semua format JSON PHP)
    @GET("acara.php")
    suspend fun getAcara(): Response<JsonElement>

    // 2. Mengirim data absensi
    @POST("absen.php")
    suspend fun submitAbsen(@Body request: AbsenRequest): Response<AbsenResponse>
}
