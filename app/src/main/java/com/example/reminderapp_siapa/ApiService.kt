package com.example.reminderapp_siapa

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // 1. Mengambil daftar acara yang buka
    @GET("acara.php")
    suspend fun getAcara(): Response<JsonElement>

    // 2. Mengirim data absensi meeting / reguler
    @POST("absen.php")
    suspend fun submitAbsen(@Body request: AbsenRequest): Response<AbsenResponse>

    // 3. Mengirim data presensi khusus apel (absen_apel.php tanpa PIN)
    @POST("absen_apel.php")
    suspend fun submitApelAbsen(@Body request: ApelAbsenRequest): Response<AbsenResponse>
}
