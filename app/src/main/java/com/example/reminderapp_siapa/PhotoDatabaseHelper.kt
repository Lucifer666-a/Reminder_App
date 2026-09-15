package com.example.reminderapp_siapa

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class PhotoRecord(
    val id: Long = 0,
    val filePath: String,
    val dateStr: String,
    val timeStr: String,
    val photoType: String,
    val timestamp: Long
)

class PhotoDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "photo_attendance_db.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_PHOTOS = "attendance_photos"
        const val COLUMN_ID = "id"
        const val COLUMN_FILE_PATH = "file_path"
        const val COLUMN_DATE = "date_str"
        const val COLUMN_TIME = "time_str"
        const val COLUMN_TYPE = "photo_type"
        const val COLUMN_TIMESTAMP = "timestamp"

        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss 'WIB'")
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_PHOTOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FILE_PATH TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHOTOS")
        onCreate(db)
    }

    /**
     * Otomatis membersihkan foto dan rekaman SQLite yang umurnya sudah melebihi batas hari (Default: 90 Hari).
     * File fisik foto di penyimpanan aplikasi akan dihapus agar memori internal HP tidak penuh.
     */
    fun cleanupOldPhotos(daysLimit: Long = 90) {
        try {
            val cutoffTimestamp = System.currentTimeMillis() - (daysLimit * 24 * 60 * 60 * 1000L)
            val db = writableDatabase

            // 1. Cari file foto yang umurnya > 90 hari dan hapus file fisiknya dari disk
            val cursor = db.rawQuery(
                "SELECT $COLUMN_FILE_PATH FROM $TABLE_PHOTOS WHERE $COLUMN_TIMESTAMP < ?",
                arrayOf(cutoffTimestamp.toString())
            )

            if (cursor.moveToFirst()) {
                val pathIdx = cursor.getColumnIndex(COLUMN_FILE_PATH)
                if (pathIdx != -1) {
                    do {
                        val filePath = cursor.getString(pathIdx)
                        try {
                            val file = File(filePath)
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } while (cursor.moveToNext())
                }
            }
            cursor.close()

            // 2. Hapus baris rekaman dari tabel SQLite
            db.delete(TABLE_PHOTOS, "$COLUMN_TIMESTAMP < ?", arrayOf(cutoffTimestamp.toString()))
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Simpan data foto hasil tangkapan kamera ke database SQLite (otomatis bersihkan foto > 90 hari).
     */
    fun savePhoto(
        filePath: String,
        photoType: String = "PRESENSI",
        date: LocalDate = LocalDate.now(),
        timeStr: String = LocalTime.now().format(timeFormatter)
    ): Long {
        cleanupOldPhotos(90)

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FILE_PATH, filePath)
            put(COLUMN_DATE, date.toString())
            put(COLUMN_TIME, timeStr)
            put(COLUMN_TYPE, photoType)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }

        val id = db.insert(TABLE_PHOTOS, null, values)
        db.close()
        return id
    }

    /**
     * Ambil daftar foto berdasarkan tanggal tertentu.
     */
    fun getPhotosForDate(date: LocalDate): List<PhotoRecord> {
        val photoList = mutableListOf<PhotoRecord>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_PHOTOS WHERE $COLUMN_DATE = ? ORDER BY $COLUMN_TIMESTAMP DESC",
                arrayOf(date.toString())
            )

            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(COLUMN_ID)
                val pathIdx = cursor.getColumnIndex(COLUMN_FILE_PATH)
                val dateIdx = cursor.getColumnIndex(COLUMN_DATE)
                val timeIdx = cursor.getColumnIndex(COLUMN_TIME)
                val typeIdx = cursor.getColumnIndex(COLUMN_TYPE)
                val tsIdx = cursor.getColumnIndex(COLUMN_TIMESTAMP)

                do {
                    photoList.add(
                        PhotoRecord(
                            id = cursor.getLong(idIdx),
                            filePath = cursor.getString(pathIdx),
                            dateStr = cursor.getString(dateIdx),
                            timeStr = cursor.getString(timeIdx),
                            photoType = cursor.getString(typeIdx),
                            timestamp = cursor.getLong(tsIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return photoList
    }

    /**
     * Ambil semua daftar foto presensi yang tersimpan di database (otomatis bersihkan foto > 90 hari).
     */
    fun getAllPhotos(): List<PhotoRecord> {
        cleanupOldPhotos(90)

        val photoList = mutableListOf<PhotoRecord>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_PHOTOS ORDER BY $COLUMN_TIMESTAMP DESC",
                null
            )

            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(COLUMN_ID)
                val pathIdx = cursor.getColumnIndex(COLUMN_FILE_PATH)
                val dateIdx = cursor.getColumnIndex(COLUMN_DATE)
                val timeIdx = cursor.getColumnIndex(COLUMN_TIME)
                val typeIdx = cursor.getColumnIndex(COLUMN_TYPE)
                val tsIdx = cursor.getColumnIndex(COLUMN_TIMESTAMP)

                do {
                    photoList.add(
                        PhotoRecord(
                            id = cursor.getLong(idIdx),
                            filePath = cursor.getString(pathIdx),
                            dateStr = cursor.getString(dateIdx),
                            timeStr = cursor.getString(timeIdx),
                            photoType = cursor.getString(typeIdx),
                            timestamp = cursor.getLong(tsIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return photoList
    }

    /**
     * Hapus rekaman foto berdasarkan ID beserta file fisiknya dari disk.
     */
    fun deletePhoto(id: Long): Boolean {
        return try {
            val db = writableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_FILE_PATH FROM $TABLE_PHOTOS WHERE $COLUMN_ID = ?",
                arrayOf(id.toString())
            )

            if (cursor.moveToFirst()) {
                val pathIdx = cursor.getColumnIndex(COLUMN_FILE_PATH)
                if (pathIdx != -1) {
                    val filePath = cursor.getString(pathIdx)
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            cursor.close()

            val deletedRows = db.delete(TABLE_PHOTOS, "$COLUMN_ID = ?", arrayOf(id.toString()))
            db.close()
            deletedRows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
