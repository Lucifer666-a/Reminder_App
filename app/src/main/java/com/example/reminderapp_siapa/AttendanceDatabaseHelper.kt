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
        private const val DATABASE_VERSION = 3

        // Tabel Absen Reguler
        const val TABLE_ABSEN_PAGI = "absen_pagi"
        const val TABLE_ABSEN_SORE = "absen_sore"

        // Tabel Absen Apel (Senin Pagi & Jumat Sore)
        const val TABLE_APEL_PAGI = "absen_apel_pagi"
        const val TABLE_APEL_SORE = "absen_apel_sore"

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

        val createTableApelPagiQuery = """
            CREATE TABLE $TABLE_APEL_PAGI (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT UNIQUE NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """.trimIndent()

        val createTableApelSoreQuery = """
            CREATE TABLE $TABLE_APEL_SORE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT UNIQUE NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createTablePagiQuery)
        db.execSQL(createTableSoreQuery)
        db.execSQL(createTableApelPagiQuery)
        db.execSQL(createTableApelSoreQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ABSEN_PAGI")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ABSEN_SORE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APEL_PAGI")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APEL_SORE")
        db.execSQL("DROP TABLE IF EXISTS attendance")
        onCreate(db)
    }

    // --- ABSEN REGULER PAGI ---

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

    fun getPagiAttendanceTime(date: LocalDate): String? {
        return getTimeFromTable(TABLE_ABSEN_PAGI, date)
    }

    fun getAllPagiAttendanceDates(): Set<LocalDate> {
        return getAttendanceDatesFromTable(TABLE_ABSEN_PAGI)
    }

    // --- ABSEN REGULER SORE ---

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

    fun getSoreAttendanceTime(date: LocalDate): String? {
        return getTimeFromTable(TABLE_ABSEN_SORE, date)
    }

    fun getAllSoreAttendanceDates(): Set<LocalDate> {
        return getAttendanceDatesFromTable(TABLE_ABSEN_SORE)
    }

    // --- ABSEN APEL PAGI (Khusus Hari Senin Pagi) ---

    fun markApelPagi(
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
            TABLE_APEL_PAGI,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        return result != -1L
    }

    fun getApelPagiTime(date: LocalDate): String? {
        return getTimeFromTable(TABLE_APEL_PAGI, date)
    }

    fun getAllApelPagiDates(): Set<LocalDate> {
        return getAttendanceDatesFromTable(TABLE_APEL_PAGI)
    }

    // --- ABSEN APEL SORE (Khusus Hari Jumat Sore) ---

    fun markApelSore(
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
            TABLE_APEL_SORE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        return result != -1L
    }

    fun getApelSoreTime(date: LocalDate): String? {
        return getTimeFromTable(TABLE_APEL_SORE, date)
    }

    fun getAllApelSoreDates(): Set<LocalDate> {
        return getAttendanceDatesFromTable(TABLE_APEL_SORE)
    }

    // --- HELPER GENERIC ---

    private fun getTimeFromTable(tableName: String, date: LocalDate): String? {
        var timeResult: String? = null
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_TIME FROM $tableName WHERE $COLUMN_DATE = ?",
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
    fun markAttendance(date: LocalDate, status: String = "PRESENT"): Boolean {
        return markAttendancePagi(date = date, status = status)
    }

    fun getAllAttendanceDates(): Set<LocalDate> {
        return getAllPagiAttendanceDates()
    }
}
