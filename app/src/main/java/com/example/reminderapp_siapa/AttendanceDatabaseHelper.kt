package com.example.reminderapp_siapa

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AttendanceDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "attendance_db.db"
        private const val DATABASE_VERSION = 2

        // Tabel Absen Pagi
        const val TABLE_ABSEN_PAGI = "absen_pagi"
        // Tabel Absen Sore
        const val TABLE_ABSEN_SORE = "absen_sore"

        const val COLUMN_ID = "id"
        const val COLUMN_DATE = "date_str" // Format: YYYY-MM-DD
        const val COLUMN_TIME = "time_str" // Format: HH:mm WIB
        const val COLUMN_STATUS = "status"   // PRESENT, ABSENT

        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm 'WIB'")
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTablePagiQuery = """
            CREATE TABLE $TABLE_ABSEN_PAGI (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT UNIQUE NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """.trimIndent()

        val createTableSoreQuery = """
            CREATE TABLE $TABLE_ABSEN_SORE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT UNIQUE NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createTablePagiQuery)
        db.execSQL(createTableSoreQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ABSEN_PAGI")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ABSEN_SORE")
        db.execSQL("DROP TABLE IF EXISTS attendance")
        onCreate(db)
    }

    // --- ABSEN PAGI ---

    // Simpan presensi pagi beserta jamnya
    fun markAttendancePagi(
        date: LocalDate,
        timeStr: String = LocalTime.now().format(timeFormatter),
        status: String = "PRESENT"
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date.toString())
            put(COLUMN_TIME, timeStr)
            put(COLUMN_STATUS, status)
        }

        val result = db.insertWithOnConflict(
            TABLE_ABSEN_PAGI,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        return result != -1L
    }

    // Ambil jam absen pagi untuk tanggal tertentu
    fun getPagiAttendanceTime(date: LocalDate): String? {
        var timeResult: String? = null
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_TIME FROM $TABLE_ABSEN_PAGI WHERE $COLUMN_DATE = ?",
                arrayOf(date.toString())
            )
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(COLUMN_TIME)
                if (columnIndex != -1) {
                    timeResult = cursor.getString(columnIndex)
                }
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return timeResult
    }

    // Ambil semua daftar tanggal absen pagi
    fun getAllPagiAttendanceDates(): Set<LocalDate> {
        return getAttendanceDatesFromTable(TABLE_ABSEN_PAGI)
    }

    // --- ABSEN SORE ---

    // Simpan presensi sore beserta jamnya
    fun markAttendanceSore(
        date: LocalDate,
        timeStr: String = LocalTime.now().format(timeFormatter),
        status: String = "PRESENT"
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date.toString())
            put(COLUMN_TIME, timeStr)
            put(COLUMN_STATUS, status)
        }

        val result = db.insertWithOnConflict(
            TABLE_ABSEN_SORE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        return result != -1L
    }

    // Ambil jam absen sore untuk tanggal tertentu
    fun getSoreAttendanceTime(date: LocalDate): String? {
        var timeResult: String? = null
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_TIME FROM $TABLE_ABSEN_SORE WHERE $COLUMN_DATE = ?",
                arrayOf(date.toString())
            )
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(COLUMN_TIME)
                if (columnIndex != -1) {
                    timeResult = cursor.getString(columnIndex)
                }
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return timeResult
    }

    // Ambil semua daftar tanggal absen sore
    fun getAllSoreAttendanceDates(): Set<LocalDate> {
        return getAttendanceDatesFromTable(TABLE_ABSEN_SORE)
    }

    // --- HELPER GENERIC ---

    private fun getAttendanceDatesFromTable(tableName: String): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COLUMN_DATE FROM $tableName", null)

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

    // --- BACKWARD COMPATIBILITY ---
    // (Agar kode lama di HomeScreen/AlarmTriggerActivity tetap bisa dikompilasi tanpa error sebelum di-update)
    fun markAttendance(date: LocalDate, status: String = "PRESENT"): Boolean {
        return markAttendancePagi(date = date, status = status)
    }

    fun getAllAttendanceDates(): Set<LocalDate> {
        return getAllPagiAttendanceDates()
    }
}
