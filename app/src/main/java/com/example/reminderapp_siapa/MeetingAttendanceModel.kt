package com.example.reminderapp_siapa

import com.google.gson.annotations.SerializedName

data class AbsenRequest(
    val nama: String,
    val jabatan: String,
    val acara: String,
    val signature: String,
    val color: String,
    val pin: String,
    val date: String,
    val time: String
)

data class ApelAbsenRequest(
    val nama: String,
    val jabatan: String,
    val acara: String,
    val sesi: String,
    val signature: String,
    val color: String,
    val date: String,
    val time: String
)

data class AbsenResponse(
    val success: Boolean?,
    val message: String?
)

data class AcaraResponse(
    val success: Boolean?,
    val acara: String?,
    @SerializedName("nama_acara") val namaAcara: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("event") val event: String?,
    val message: String?
) {
    fun getEventName(): String? {
        return acara ?: namaAcara ?: title ?: name ?: event
    }
}
