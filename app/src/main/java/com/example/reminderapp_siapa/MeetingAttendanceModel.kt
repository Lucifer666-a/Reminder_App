package com.example.reminderapp_siapa

import com.google.gson.annotations.SerializedName

// 1. Model Auth Login
data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean?,
    val message: String?,
    @SerializedName("user_id") val userId: Int?,
    val nama: String?,
    val jabatan: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val token: String?
)

// 2. Model Status & Riwayat Harian User
data class UserStatusResponse(
    val success: Boolean?,
    val message: String?,
    @SerializedName("pagi_attended") val pagiAttended: Boolean?,
    @SerializedName("sore_attended") val soreAttended: Boolean?,
    @SerializedName("pagi_time") val pagiTime: String?,
    @SerializedName("sore_time") val soreTime: String?,
    @SerializedName("pagi_dates") val pagiDates: List<String>?,
    @SerializedName("sore_dates") val soreDates: List<String>?
)

// 3. Model Presensi Apel & Harian dengan Dukungan Foto Base64
data class ApelAbsenRequest(
    @SerializedName("user_id") val userId: Int,
    val aksi: String, // "checkin" atau "checkout"
    val sesi: String? = null, // "pagi" atau "sore"
    val foto: String? = null,
    @SerializedName("foto_absen") val fotoAbsen: String? = null,
    @SerializedName("tanda_tangan") val tandaTangan: String? = null
)

// 4. Model Presensi Rapat / Acara
data class AbsenRequest(
    @SerializedName("user_id") val userId: Int,
    val pin: String,
    @SerializedName("acara_id") val acaraId: Int?,
    @SerializedName("tanda_tangan") val tandaTangan: String
)

data class AbsenResponse(
    val success: Boolean?,
    val message: String?
)

// 5. Model Acara Rapat
data class AcaraItem(
    @SerializedName("acara_id") val acaraId: Int?,
    val id: Int?,
    @SerializedName("nama_acara") val namaAcara: String?,
    val title: String?,
    val status: String?
) {
    fun getEventId(): Int {
        return acaraId ?: id ?: 1
    }
    fun getEventName(): String {
        return namaAcara ?: title ?: "Acara Rapat"
    }
}
