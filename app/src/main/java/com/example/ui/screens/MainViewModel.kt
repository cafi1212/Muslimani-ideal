package com.example.ui.screens

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.CityPreset
import com.example.utils.PrayerTimeItem
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = IslamicAppRepository(db)

    // Current time dynamic ticker state
    var currentTimeLong by mutableLongStateOf(System.currentTimeMillis())
        private set

    // Active screen navigation tab
    var selectedTab by mutableStateOf(0)
        private set

    // Selected location settings
    var selectedCityName by mutableStateOf("Skopje, Macedonia")
        private set

    // Time Format Preference and Timezone Preferences
    var is24HourFormat by mutableStateOf(false)
    var useDeviceTimezoneForClock by mutableStateOf(false)

    // Calculation settings & offsets
    var calculationMethod by mutableStateOf("Balkans10") // "UmmAlQura", "MWL", "ISNA", "Balkans12", "Balkans10"
    var asrJuristicMethod by mutableStateOf("Standard") // "Standard" (Shafi'i/others), "Hanafi"
    var fajrOffsetMins by mutableStateOf(0)

    // Set of prayers that have custom alert/notification sounds enabled (visual toggle)
    var notificationStates by mutableStateOf(mapOf(
        "Fajr" to true,
        "Sunrise" to false,
        "Dhuhr" to true,
        "Asr" to true,
        "Maghrib" to true,
        "Isha" to true
    ))
        private set

    // --- Interactive Quotes States ---
    var quotesSearchQuery by mutableStateOf("")
    var selectedQuoteCategory by mutableStateOf("All") // "All", "Quran", "Hadith", "Wisdom", "Favorites"

    // Fav quotes stream from DB
    val favoriteQuotes: StateFlow<List<FavoriteQuote>> = repository.allFavoriteQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Tasbeeh Subhah States ---
    var currentTasbeehCount by mutableStateOf(0)
    var tasbeehTarget by mutableStateOf(33) // 33, 99, 100, 0 (Infinite)
    var selectedDhikrPhrase by mutableStateOf("SubhanAllah") // SubhanAllah, Alhamdulillah, AllahuAkbar, Astaghfirullah
    var customDhikrInput by mutableStateOf("")

    // Shows interactive celebration when subhah counter reaches target limit
    var showTargetReachedDialog by mutableStateOf(false)

    // Tasbeeh lifetime logs stream from DB
    val dhikrLogs: StateFlow<List<DhikrLog>> = repository.allDhikrLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Prayer Daily Tracker Sync ---
    val todayDateStr = derivedStateOf {
        val preset = PrayerTimesCalculator.getPreset(selectedCityName)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(preset.timezoneId)
        }
        sdf.format(Date(currentTimeLong))
    }

    // Hot flow for today's logs
    val todayPrayerLogs: StateFlow<List<PrayerLog>> = snapshotFlow { todayDateStr.value }
        .flatMapLatest { date -> repository.getPrayerLogs(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run a constant ticking coroutine to fuel calculations at 1-second ticks
        viewModelScope.launch {
            while (true) {
                currentTimeLong = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    fun selectTab(tab: Int) {
        selectedTab = tab
    }

    fun selectCity(name: String) {
        val preset = PrayerTimesCalculator.getPreset(name)
        selectedCityName = preset.name
        calculationMethod = preset.method
    }

    fun togglePrayerNotification(prayerName: String) {
        val current = notificationStates[prayerName] ?: false
        notificationStates = notificationStates.toMutableMap().apply {
            put(prayerName, !current)
        }
    }

    // Calculates today's detailed prayer times
    val todayPrayerTimes = derivedStateOf {
        val preset = PrayerTimesCalculator.getPreset(selectedCityName)
        val tz = TimeZone.getTimeZone(preset.timezoneId)
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = currentTimeLong
        val dynamicOffset = tz.getOffset(cal.timeInMillis) / 3600000.0
        val offsetsMap = mapOf("Fajr" to fajrOffsetMins)
        PrayerTimesCalculator.calculatePrayerTimes(
            calendar = cal,
            latitude = preset.latitude,
            longitude = preset.longitude,
            timezoneOffset = dynamicOffset,
            method = calculationMethod,
            asrMethod = asrJuristicMethod,
            offsetsMinutes = offsetsMap
        )
    }

    // Calculates tomorrow's detailed prayer times for accurate late-night wrap bounds
    val tomorrowPrayerTimes = derivedStateOf {
        val preset = PrayerTimesCalculator.getPreset(selectedCityName)
        val tz = TimeZone.getTimeZone(preset.timezoneId)
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = currentTimeLong
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dynamicOffset = tz.getOffset(cal.timeInMillis) / 3600000.0
        val offsetsMap = mapOf("Fajr" to fajrOffsetMins)
        PrayerTimesCalculator.calculatePrayerTimes(
            calendar = cal,
            latitude = preset.latitude,
            longitude = preset.longitude,
            timezoneOffset = dynamicOffset,
            method = calculationMethod,
            asrMethod = asrJuristicMethod,
            offsetsMinutes = offsetsMap
        )
    }

    // Calculates which prayer is next & the countdown time remaining
    val nextPrayerInfo = derivedStateOf {
        calculateNextPrayerInfo()
    }

    private fun calculateNextPrayerInfo(): NextPrayerDetails {
        val now = currentTimeLong
        val times = todayPrayerTimes.value
        val tomorrowTimes = tomorrowPrayerTimes.value

        // Find chronological next prayer
        var nextItem: PrayerTimeItem? = null
        var lastItem: PrayerTimeItem? = null

        // Sort items by timestamp
        val sortedToday = times.sortedBy { it.timestampMs }

        for (i in sortedToday.indices) {
            val item = sortedToday[i]
            if (item.timestampMs > now) {
                nextItem = item
                lastItem = if (i > 0) sortedToday[i - 1] else null
                break
            }
        }

        // If no today times are after now, next prayer is tomorrow's Fajr
        if (nextItem == null) {
            nextItem = tomorrowTimes.firstOrNull { it.name == "Fajr" } ?: tomorrowTimes.firstOrNull() ?: PrayerTimeItem("Fajr", "04:00", "wb_sunny", now + 14400000)
            lastItem = times.lastOrNull { it.name == "Isha" } ?: times.lastOrNull()
        }

        val nextTimeMs = nextItem.timestampMs
        val diffMs = maxOf(0L, nextTimeMs - now)

        val hrs = diffMs / (1000 * 60 * 60)
        val mins = (diffMs / (1000 * 60)) % 60
        val secs = (diffMs / 1000) % 60

        val countdownStr = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)

        // Calculate progress percentage
        val elapsedPercent = if (lastItem != null) {
            val totalSpan = nextTimeMs - lastItem.timestampMs
            val elapsed = now - lastItem.timestampMs
            if (totalSpan > 0) {
                (elapsed.toFloat() / totalSpan.toFloat()).coerceIn(0f, 1f)
            } else 0f
        } else {
            0.5f // Neutral fallback
        }

        return NextPrayerDetails(
            name = nextItem.name,
            countdown = countdownStr,
            progress = elapsedPercent
        )
    }

    // --- Prayer Daily Tracker Cycles ---
    fun cyclePrayerStatus(prayerName: String) {
        viewModelScope.launch {
            val date = todayDateStr.value
            val existing = todayPrayerLogs.value.find { it.prayerName == prayerName }
            val nextStatus = when (existing?.status) {
                null, "none" -> "on_time"
                "on_time" -> "late"
                "late" -> "missed"
                else -> "none"
            }
            repository.updatePrayerLog(date, prayerName, nextStatus)
        }
    }

    fun setPrayerStatus(prayerName: String, status: String) {
        viewModelScope.launch {
            repository.updatePrayerLog(todayDateStr.value, prayerName, status)
        }
    }

    // --- Quotes Features ---
    fun toggleQuoteBookmark(quote: Quote, isBookmarked: Boolean) {
        viewModelScope.launch {
            if (isBookmarked) {
                repository.removeFavoriteQuote(quote.id)
            } else {
                repository.addFavoriteQuote(quote.id, quote.text, quote.source, quote.category)
            }
        }
    }

    fun removeFavoriteQuoteDirect(id: String) {
        viewModelScope.launch {
            repository.removeFavoriteQuote(id)
        }
    }

    // --- Tasbeeh Counters Events ---
    fun selectDhikr(phrase: String) {
        selectedDhikrPhrase = phrase
        currentTasbeehCount = 0
    }

    fun handleTasbeehClick() {
        if (tasbeehTarget > 0) {
            if (currentTasbeehCount < tasbeehTarget) {
                currentTasbeehCount++
                if (currentTasbeehCount == tasbeehTarget) {
                    // target reached!
                    showTargetReachedDialog = true
                    saveCompletedDhikrCycle()
                }
            }
        } else {
            currentTasbeehCount++ // Infinite mode
        }
    }

    fun forceResetTasbeeh() {
        if (currentTasbeehCount > 0) {
            saveCompletedDhikrCycle(partial = true)
        }
        currentTasbeehCount = 0
    }

    private fun saveCompletedDhikrCycle(partial: Boolean = false) {
        val finalPhrase = if (selectedDhikrPhrase == "Custom") {
            if (customDhikrInput.isNotBlank()) customDhikrInput else "Custom Dhikr"
        } else {
            selectedDhikrPhrase
        }
        val loggedCount = currentTasbeehCount
        if (loggedCount > 0) {
            viewModelScope.launch {
                repository.insertDhikrLog(
                    DhikrLog(
                        phrase = finalPhrase,
                        count = loggedCount,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun clearDhikrLogs() {
        viewModelScope.launch {
            repository.clearAllDhikrLogs()
        }
    }

    fun deleteDhikrLog(id: Long) {
        viewModelScope.launch {
            repository.deleteDhikrLogById(id)
        }
    }
}

data class NextPrayerDetails(
    val name: String,
    val countdown: String,
    val progress: Float
)
