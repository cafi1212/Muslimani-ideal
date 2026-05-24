package com.example.utils

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

data class CityPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String, // Dynamic GMT timezone ID (e.g. Europe/Skopje, Asia/Riyadh)
    val method: String = "ISNA" // ISNA, MWL, UmmAlQura
)

data class PrayerTimeItem(
    val name: String,
    val timeStr: String, // HH:MM
    val displayIcon: String, // Icon name
    val timestampMs: Long // Time in epoch millis
)

object PrayerTimesCalculator {

    val presets = listOf(
        CityPreset("Skopje, Macedonia", 41.9981, 21.4254, "Europe/Skopje", "Balkans10"),
        CityPreset("Mecca, KSA", 21.3891, 39.8579, "Asia/Riyadh", "UmmAlQura"),
        CityPreset("London, UK", 51.5074, -0.1278, "Europe/London", "MWL"),
        CityPreset("New York, USA", 40.7128, -74.0060, "America/New_York", "ISNA"),
        CityPreset("Kuala Lumpur, MYS", 3.1390, 101.6869, "Asia/Kuala_Lumpur", "MWL"),
        CityPreset("Cairo, Egypt", 30.0444, 31.2357, "Africa/Cairo", "MWL"),
        CityPreset("Istanbul, Turkey", 41.0082, 28.9784, "Europe/Istanbul", "MWL"),
        CityPreset("Medina, KSA", 24.4672, 39.6111, "Asia/Riyadh", "UmmAlQura"),
        CityPreset("Jakarta, Indonesia", -6.2088, 106.8456, "Asia/Jakarta", "MWL"),
        CityPreset("Sydney, Australia", -33.8688, 151.2093, "Australia/Sydney", "MWL")
    )

    fun getPreset(name: String): CityPreset {
        return presets.find { it.name == name } ?: presets[0]
    }

    // Dynamic Qibla direction algorithm based on spatial coordinates
    fun calculateQibla(latitude: Double, longitude: Double): Double {
        val meccaLat = Math.toRadians(21.3891)
        val meccaLon = Math.toRadians(39.8579)
        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)

        val y = sin(meccaLon - lonRad)
        val x = cos(latRad) * tan(meccaLat) - sin(latRad) * cos(meccaLon - lonRad)

        var qibla = Math.toDegrees(atan2(y, x))
        if (qibla < 0) qibla += 360.0
        return qibla
    }

    // Calculates prayer times for a given day, coordinates, and method
    fun calculatePrayerTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffset: Double,
        method: String,
        asrMethod: String = "Standard", // "Standard" (Shafi'i, Maliki, Hanbali) or "Hanafi"
        offsetsMinutes: Map<String, Int> = emptyMap() // Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha
    ): List<PrayerTimeItem> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Step 1: Calculate Day of Year
        val dayOfYear = getDayOfYear(year, month, day)

        // Step 2: Basic Astronomical Calculations
        val d = dayOfYear - 1 // 0-indexed days
        val g = (357.529 + 0.98560028 * d) * PI / 180.0
        val q = (280.459 + 0.98564736 * d) % 360.0
        val L = (q + 1.915 * sin(g) + 0.020 * sin(2.0 * g)) * PI / 180.0
        val epsilon = (23.439 - 0.00000036 * d) * PI / 180.0
        
        // Sun Declination
        val declination = asin(sin(epsilon) * sin(L))
        
        // Right Ascension
        var ra = atan2(cos(epsilon) * sin(L), cos(L))
        if (ra < 0) ra += 2.0 * PI
        ra = (ra * 180.0 / PI) / 15.0 // Convert to hours

        // Equation of Time (EoT) in hours
        val julianLongitude = (q * PI / 180.0)
        val eot = (julianLongitude * 180.0 / PI) / 15.0 - ra

        // Solar Noon (Midday) in local time hours
        val solarNoonBase = 12.0 - longitude / 15.0 + timezoneOffset - eot
        val dhuhrHour = solarNoonBase + (1.0 / 60.0) // 1 minute after noon is safer to avoid exact zenith

        // Coordinate adjustments
        val latRad = latitude * PI / 180.0

        // Helper to calculate hour angle (H) for a given altitude angle (alpha)
        fun getHourAngle(alpha: Double): Double {
            val num = sin(alpha * PI / 180.0) - sin(latRad) * sin(declination)
            val den = cos(latRad) * cos(declination)
            val ratio = num / den
            return if (ratio in -1.0..1.0) {
                acos(ratio) * 180.0 / PI / 15.0 // Convert to hours
            } else {
                Double.NaN
            }
        }

        // Methods angle configurations
        val fajrAngle = when (method) {
            "UmmAlQura" -> -18.5
            "MWL" -> -18.0
            "ISNA" -> -15.0
            "Balkans12" -> -12.0
            "Balkans10" -> -10.0
            else -> -18.0
        }
        
        val ishaAngleOrDiff = when (method) {
            "UmmAlQura" -> 90.0 // 90 mins after Maghrib
            "MWL" -> -17.0
            "ISNA" -> -15.0
            "Balkans12" -> -12.0
            "Balkans10" -> -10.0
            else -> -18.0
        }

        // Calculate Hour Angles
        val sunriseH = getHourAngle(-0.833) // Standard sunrise altitude angle
        val sunsetH = sunriseH // Sunset hour angle is symmetric

        val fajrH = getHourAngle(fajrAngle)
        
        val ishaH = if (method == "UmmAlQura") {
            Double.NaN
        } else {
            getHourAngle(ishaAngleOrDiff)
        }

        // Asr Calculation
        // Shadow ratio for Asr
        val shadowRatio = if (asrMethod == "Hanafi") 2.0 else 1.0
        val tempValue = shadowRatio + tan(abs(latRad - declination))
        val asrAngleRad = atan(1.0 / tempValue)
        val asrAngle = asrAngleRad * 180.0 / PI
        val asrH = getHourAngle(asrAngle)

        // Prayer Times in decimal hours
        var fajrDec = dhuhrHour - fajrH
        var sunriseDec = dhuhrHour - sunriseH
        var asrDec = dhuhrHour + asrH
        var maghribDec = dhuhrHour + sunsetH
        var ishaDec = if (method == "UmmAlQura") {
            maghribDec + (90.0 / 60.0) // 90 minutes after Maghrib
        } else {
            dhuhrHour + ishaH
        }

        // Fallback for Polar regions (if calculations return NaN)
        if (fajrDec.isNaN()) fajrDec = dhuhrHour - 1.5
        if (sunriseDec.isNaN()) sunriseDec = dhuhrHour - 1.0
        if (asrDec.isNaN()) asrDec = dhuhrHour + 2.5
        if (maghribDec.isNaN()) maghribDec = dhuhrHour + 6.0
        if (ishaDec.isNaN()) ishaDec = maghribDec + 1.5

        // Put times into list
        val itemsList = mutableListOf<String>()

        // Helper to format decimal hour to HH:MM and apply offset
        fun formatAndOffsetTime(baseDecimal: Double, key: String): String {
            val offsetMins = offsetsMinutes[key] ?: 0
            val totalMins = (baseDecimal * 60.0).roundToInt() + offsetMins
            val normMins = (totalMins + 1440) % 1440
            val h = normMins / 60
            val m = normMins % 60
            return String.format(Locale.US, "%02d:%02d", h, m)
        }

        val fajrTime = formatAndOffsetTime(fajrDec, "Fajr")
        val sunriseTime = formatAndOffsetTime(sunriseDec, "Sunrise")
        
        // For Skopje, Macedonia (Balkan / Diyanet convention), Dhuhr (Zuhur) is fixed to 13:00 in summer (+2) and 12:00 in winter (+1)
        val dhuhrHourForDisplay = if (latitude > 41.9 && latitude < 42.1 && longitude > 21.3 && longitude < 21.5) {
            if (timezoneOffset >= 1.5) 13.0 else 12.0
        } else {
            dhuhrHour
        }
        val dhuhrTime = formatAndOffsetTime(dhuhrHourForDisplay, "Dhuhr")
        
        val asrTime = formatAndOffsetTime(asrDec, "Asr")
        val maghribTime = formatAndOffsetTime(maghribDec, "Maghrib")
        val ishaTime = formatAndOffsetTime(ishaDec, "Isha")

        // Helper to construct timestamp for today's dynamic calendar date
        fun getTimestampForTime(timeStr: String): Long {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val tCal = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return tCal.timeInMillis
        }

        return listOf(
            PrayerTimeItem("Fajr", fajrTime, "wb_sunny", getTimestampForTime(fajrTime)),
            PrayerTimeItem("Sunrise", sunriseTime, "wb_twilight", getTimestampForTime(sunriseTime)),
            PrayerTimeItem("Dhuhr", dhuhrTime, "sunny", getTimestampForTime(dhuhrTime)),
            PrayerTimeItem("Asr", asrTime, "cloud", getTimestampForTime(asrTime)),
            PrayerTimeItem("Maghrib", maghribTime, "flare", getTimestampForTime(maghribTime)),
            PrayerTimeItem("Isha", ishaTime, "nightlight", getTimestampForTime(ishaTime))
        )
    }

    private fun getDayOfYear(year: Int, month: Int, day: Int): Int {
        val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
            daysInMonth[1] = 29 // leap year
        }
        var dayCount = day
        for (i in 0 until month - 1) {
            dayCount += daysInMonth[i]
        }
        return dayCount
    }
}
