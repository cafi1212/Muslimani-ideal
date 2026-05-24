package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DhikrLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DhikrLog)

    @Query("DELETE FROM dhikr_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM dhikr_logs")
    suspend fun deleteAllLogs()
}

@Dao
interface FavoriteQuoteDao {
    @Query("SELECT * FROM favorite_quotes ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteQuote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteQuote)

    @Query("DELETE FROM favorite_quotes WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)
}

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE dateStr = :dateStr")
    fun getLogsForDate(dateStr: String): Flow<List<PrayerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: PrayerLog)

    @Query("DELETE FROM prayer_logs WHERE dateStr = :dateStr AND prayerName = :prayerName")
    suspend fun deleteLog(dateStr: String, prayerName: String)
}

@Database(entities = [DhikrLog::class, FavoriteQuote::class, PrayerLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun favoriteQuoteDao(): FavoriteQuoteDao
    abstract fun prayerLogDao(): PrayerLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "islamic_reminder_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class IslamicAppRepository(private val db: AppDatabase) {
    val dhikrDao = db.dhikrDao()
    val favoriteQuoteDao = db.favoriteQuoteDao()
    val prayerLogDao = db.prayerLogDao()

    // Dhikr Logs
    val allDhikrLogs: Flow<List<DhikrLog>> = dhikrDao.getAllLogs()

    suspend fun insertDhikrLog(log: DhikrLog) {
        dhikrDao.insertLog(log)
    }

    suspend fun deleteDhikrLogById(id: Long) {
        dhikrDao.deleteLogById(id)
    }

    suspend fun clearAllDhikrLogs() {
        dhikrDao.deleteAllLogs()
    }

    // Favorite Quotes
    val allFavoriteQuotes: Flow<List<FavoriteQuote>> = favoriteQuoteDao.getAllFavorites()

    suspend fun addFavoriteQuote(id: String, text: String, source: String, category: String) {
        favoriteQuoteDao.insertFavorite(FavoriteQuote(id, text, source, category))
    }

    suspend fun removeFavoriteQuote(id: String) {
        favoriteQuoteDao.deleteFavoriteById(id)
    }

    // Prayer Tracker Logs
    fun getPrayerLogs(dateStr: String): Flow<List<PrayerLog>> = prayerLogDao.getLogsForDate(dateStr)

    suspend fun updatePrayerLog(dateStr: String, prayerName: String, status: String) {
        prayerLogDao.insertOrUpdateLog(PrayerLog(dateStr, prayerName, status))
    }
}
