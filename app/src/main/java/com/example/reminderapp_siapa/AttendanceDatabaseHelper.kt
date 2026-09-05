package com.example.reminderapp_siapa

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class AttendanceDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "attendance_db.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ATTENDANCE = "attendance"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date_str" // Format: YYYY-MM-DD
        private const val COLUMN_STATUS = "status"   // PRESENT, ABSENT
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_ATTENDANCE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT UNIQUE NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTENDANCE")
        onCreate(db)
    }

    // Simpan / Tandai tanggal presensi
    fun markAttendance(date: LocalDate, status: String = "PRESENT"): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date.toString())
            put(COLUMN_STATUS, status)
        }

        val result = db.insertWithOnConflict(
            TABLE_ATTENDANCE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        return result != -1L
    }

    // Hapus / Batal presensi untuk tanggal tertentu
    fun removeAttendance(date: LocalDate): Boolean {
        val db = writableDatabase
        val result = db.delete(
            TABLE_ATTENDANCE,
            "$COLUMN_DATE = ?",
            arrayOf(date.toString())
        )
        db.close()
        return result > 0
    }

    // Ambil semua daftar tanggal yang sudah berstatus presensi
    fun getAllAttendanceDates(): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COLUMN_DATE FROM $TABLE_ATTENDANCE", null)

            if (cursor.moveToFirst()) {
                val dateColumnIndex = cursor.getColumnIndex(COLUMN_DATE)
                if (dateColumnIndex != -1) {
                    do {
                        val dateStr = cursor.getString(dateColumnIndex)
                        try {
                            dates.add(LocalDate.parse(dateStr))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } while (cursor.moveToNext())
                }
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dates
    }
}
