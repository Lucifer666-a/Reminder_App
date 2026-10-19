package com.example.reminderapp_siapa

import android.content.Context
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Alamat server. Tanpa spasi di depan, dan wajib diakhiri "/".
    private const val BASE_URL = "http://192.168.5.100/WebAbsenPa/absen/api/"

    private val refreshLock = Any()

    /** Wajib dipanggil sekali saat aplikasi dibuka (di MainActivity.onCreate). */
    fun init(context: Context) {
        TokenStore.init(context)
    }

    // Client polos (tanpa token) khusus untuk menukar token yang hampir habis
    private val plainClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** True kalau access token sudah habis atau habis dalam < 60 detik. */
    private fun isExpiring(token: String): Boolean {
        return try {
            val payload = token.split(".")[1]
            val json = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            )
            val exp = JSONObject(json).getLong("exp")
            exp - System.currentTimeMillis() / 1000 < 60
        } catch (e: Exception) {
            false
        }
    }

    /** Tukar refresh token dengan sepasang token baru. True kalau berhasil. */
    private fun refreshTokens(): Boolean {
        val refresh = TokenStore.refreshToken ?: return false
        return try {
            val body = JSONObject().put("refresh_token", refresh).toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL + "refresh_token.php")
                .post(body)
                .header("ngrok-skip-browser-warning", "true")
                .build()

            plainClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    false
                } else {
                    val json = JSONObject(response.body?.string() ?: "")
                    val newAccess = json.optString("access_token")
                    val newRefresh = json.optString("refresh_token")
                    if (newAccess.isNotEmpty() && newRefresh.isNotEmpty()) {
                        TokenStore.save(newAccess, newRefresh)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val path = original.url.encodedPath
                val isAuthEndpoint =
                    path.endsWith("login.php") || path.endsWith("refresh_token.php")

                val builder = original.newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .addHeader("User-Agent", "ReminderAppSIAPA/1.0")

                if (!isAuthEndpoint) {
                    // Perpanjang token dulu kalau hampir habis
                    synchronized(refreshLock) {
                        val current = TokenStore.accessToken
                        if (current != null && isExpiring(current)) {
                            refreshTokens()
                        }
                    }
                    // Pasang tiket di setiap permintaan
                    TokenStore.accessToken?.let { token ->
                        builder.header("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
