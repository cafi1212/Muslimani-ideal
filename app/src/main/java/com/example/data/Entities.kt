package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dhikr_logs")
data class DhikrLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phrase: String,
    val count: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_quotes")
data class FavoriteQuote(
    @PrimaryKey val id: String, // Unique identifier e.g. "quran_1"
    val quoteText: String,
    val source: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_logs", primaryKeys = ["dateStr", "prayerName"])
data class PrayerLog(
    val dateStr: String, // "YYYY-MM-DD"
    val prayerName: String, // "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"
    val status: String, // "none", "on_time", "late", "missed"
    val timestamp: Long = System.currentTimeMillis()
)
