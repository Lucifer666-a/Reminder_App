package com.example.reminderapp_siapa

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    // 1. API Login User (Pegawai)
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // 2. API Status & Riwayat Harian User (GET & POST absen_harian.php)
    @GET("absen_harian.php")
    suspend fun getAbsenHarian(@Query("user_id") userId: Int): Response<JsonElement>

    @POST("absen_harian.php")
    suspend fun submitAbsenHarian(@Body request: ApelAbsenRequest): Response<AbsenResponse>

    // 3. API Status & Riwayat Harian Alternatif
    @GET("user_status.php")
    suspend fun getUserStatus(@Query("user_id") userId: Int): Response<JsonElement>

    // 4. API Presensi Apel (Pagi & Sore)
    @POST("absen_apel.php")
    suspend fun submitApelAbsen(@Body request: ApelAbsenRequest): Response<AbsenResponse>

    // 5. API Presensi Rapat / Acara
    @POST("absen.php")
    suspend fun submitAbsen(@Body request: AbsenRequest): Response<AbsenResponse>

    // 6. API Daftar Acara Rapat yang Buka
    @GET("acara.php")
    suspend fun getAcara(): Response<JsonElement>
}
