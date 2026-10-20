package com.example.reminderapp_siapa

import android.content.Context
import android.util.Log
import com.google.gson.JsonElement
import java.time.LocalDate

object AttendanceSyncHelper {
    suspend fun syncWithServer(context: Context, userId: Int) {
        try {
            val db = AttendanceDatabaseHelper(context)

            val responseStatus = ApiClient.apiService.getUserStatus(userId)
            Log.d("ATTENDANCE_SYNC", "user_status code: ${responseStatus.code()}, body: ${responseStatus.body()}")
            if (responseStatus.isSuccessful && responseStatus.body() != null) {
                parseAndSaveJson(responseStatus.body(), db)
            }

            val responseHarian = ApiClient.apiService.getAbsenHarian(userId)
            Log.d("ATTENDANCE_SYNC", "absen_harian code: ${responseHarian.code()}, body: ${responseHarian.body()}")
            if (responseHarian.isSuccessful && responseHarian.body() != null) {
                parseAndSaveJson(responseHarian.body(), db)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ATTENDANCE_SYNC", "Error syncing: ${e.localizedMessage}")
        }
    }

    fun parseAndSaveJson(element: JsonElement?, db: AttendanceDatabaseHelper) {
        if (element == null) return
        try {
            if (element.isJsonObject) {
                val obj = element.asJsonObject

                val dataKey = when {
                    obj.has("data") -> "data"
                    obj.has("riwayat_terbaru") -> "riwayat_terbaru"
                    obj.has("history") -> "history"
                    obj.has("rows") -> "rows"
                    obj.has("result") -> "result"
                    obj.has("absensi") -> "absensi"
                    obj.has("presensi_apel") -> "presensi_apel"
                    obj.has("apel") -> "apel"
                    obj.has("riwayat_apel") -> "riwayat_apel"
                    else -> null
                }

                if (dataKey != null) {
                    parseAndSaveJson(obj.get(dataKey), db)
                }

                if (obj.has("pagi_dates") && obj.get("pagi_dates").isJsonArray) {
                    obj.getAsJsonArray("pagi_dates").forEach {
                        try { db.markAttendancePagi(LocalDate.parse(it.asString.take(10)), "07:55 WIB") } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                if (obj.has("sore_dates") && obj.get("sore_dates").isJsonArray) {
                    obj.getAsJsonArray("sore_dates").forEach {
                        try { db.markAttendanceSore(LocalDate.parse(it.asString.take(10)), "16:30 WIB") } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                if (obj.has("apel_pagi_dates") && obj.get("apel_pagi_dates").isJsonArray) {
                    obj.getAsJsonArray("apel_pagi_dates").forEach {
                        try { db.markApelPagi(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                if (obj.has("apel_sore_dates") && obj.get("apel_sore_dates").isJsonArray) {
                    obj.getAsJsonArray("apel_sore_dates").forEach {
                        try { db.markApelSore(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                if (obj.has("apel_bulan_ini") && obj.get("apel_bulan_ini").isJsonObject) {
                    val apelObj = obj.getAsJsonObject("apel_bulan_ini")
                    if (apelObj.has("pagi") && apelObj.get("pagi").isJsonArray) {
                        apelObj.getAsJsonArray("pagi").forEach {
                            try { db.markApelPagi(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                    if (apelObj.has("sore") && apelObj.get("sore").isJsonArray) {
                        apelObj.getAsJsonArray("sore").forEach {
                            try { db.markApelSore(LocalDate.parse(it.asString.take(10))) } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
            } else if (element.isJsonArray) {
                element.asJsonArray.forEach { item ->
                    if (item.isJsonObject) {
                        val itemObj = item.asJsonObject
                        val dateStr = itemObj.get("tanggal")?.asString ?: itemObj.get("date")?.asString ?: itemObj.get("tgl")?.asString
                        val checkin = itemObj.get("waktu_checkin")?.asString 
                            ?: itemObj.get("checkin")?.asString 
                            ?: itemObj.get("jam_masuk")?.asString
                            ?: itemObj.get("waktu")?.asString
                        val checkout = itemObj.get("waktu_checkout")?.asString 
                            ?: itemObj.get("checkout")?.asString 
                            ?: itemObj.get("jam_keluar")?.asString

                        val jenis = itemObj.get("jenis")?.asString?.lowercase() ?: ""
                        val sesi = itemObj.get("sesi")?.asString?.lowercase() ?: ""
                        val judul = itemObj.get("judul")?.asString?.lowercase() ?: ""

                        if (jenis.contains("rapat") || jenis.contains("acara") || sesi.contains("rapat") || judul.contains("rapat")) {
                            return@forEach
                        }

                        if (!dateStr.isNullOrBlank() && dateStr.lowercase() != "null") {
                            try {
                                val cleanDateStr = dateStr.trim().take(10)
                                val date = LocalDate.parse(cleanDateStr)

                                val isApel = jenis.contains("apel") || sesi.contains("apel") || judul.contains("apel")
                                val isSore = jenis.contains("sore") || sesi.contains("sore") || sesi.contains("pulang") || judul.contains("sore")

                                if (isApel) {
                                    if (isSore) {
                                        db.markApelSore(date)
                                    } else {
                                        db.markApelPagi(date)
                                    }
                                } else {
                                    if (isSore && checkout != null) {
                                        db.markAttendanceSore(date, checkout)
                                    } else if (checkin != null) {
                                        db.markAttendancePagi(date, checkin)
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
