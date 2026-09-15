package com.example.reminderapp_siapa

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
     * Simpan data foto hasil tangkapan kamera ke database SQLite.
     */
    fun savePhoto(
        filePath: String,
        photoType: String = "PRESENSI",
        date: LocalDate = LocalDate.now(),
        timeStr: String = LocalTime.now().format(timeFormatter)
    ): Long {
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
     * Ambil semua daftar foto presensi yang tersimpan di database.
     */
    fun getAllPhotos(): List<PhotoRecord> {
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
     * Hapus rekaman foto berdasarkan ID.
     */
    fun deletePhoto(id: Long): Boolean {
        return try {
            val db = writableDatabase
            val deletedRows = db.delete(TABLE_PHOTOS, "$COLUMN_ID = ?", arrayOf(id.toString()))
            db.close()
            deletedRows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
