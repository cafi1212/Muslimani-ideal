package com.example

import com.example.utils.PrayerTimesCalculator
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {
  @Test
  fun testSkopjePrayerTimes() {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Skopje")).apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.MAY)
      set(Calendar.DAY_OF_MONTH, 24)
    }
    
    // Let's test different Fajr angles from 12.0 to 18.0
    for (angle in listOf(12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0)) {
      // Modify calculation directly or via custom simulation inside test
      val preset = PrayerTimesCalculator.getPreset("Skopje, Macedonia")
      val tz = TimeZone.getTimeZone(preset.timezoneId)
      val dynamicOffset = tz.getOffset(cal.timeInMillis) / 3600000.0
      
      // Let's simulate the calculation for a custom Fajr angle
      val year = cal.get(Calendar.YEAR)
      val month = cal.get(Calendar.MONTH) + 1
      val day = cal.get(Calendar.DAY_OF_MONTH)
      
      val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
      if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
          daysInMonth[1] = 29
      }
      var dayCount = day
      for (i in 0 until month - 1) {
          dayCount += daysInMonth[i]
      }
      val d = dayCount - 1
      val g = (357.529 + 0.98560028 * d) * Math.PI / 180.0
      val q = (280.459 + 0.98564736 * d) % 360.0
      val L = (q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2.0 * g)) * Math.PI / 180.0
      val epsilon = (23.439 - 0.00000036 * d) * Math.PI / 180.0
      val declination = Math.asin(Math.sin(epsilon) * Math.sin(L))
      var ra = Math.atan2(Math.cos(epsilon) * Math.sin(L), Math.cos(L))
      if (ra < 0) ra += 2.0 * Math.PI
      ra = (ra * 180.0 / Math.PI) / 15.0
      val julianLongitude = (q * Math.PI / 180.0)
      val eot = (julianLongitude * 180.0 / Math.PI) / 15.0 - ra
      val solarNoonBase = 12.0 - preset.longitude / 15.0 + dynamicOffset - eot
      val dhuhrHour = solarNoonBase + (1.0 / 60.0)
      
      val latRad = preset.latitude * Math.PI / 180.0
      
      val num = Math.sin(-angle * Math.PI / 180.0) - Math.sin(latRad) * Math.sin(declination)
      val den = Math.cos(latRad) * Math.cos(declination)
      val ratio = num / den
      val fajrH = if (ratio in -1.0..1.0) Math.acos(ratio) * 180.0 / Math.PI / 15.0 else Double.NaN
      
      val fajrDec = dhuhrHour - fajrH
      val totalMins = (fajrDec * 60.0).roundToInt()
      val h = totalMins / 60
      val m = totalMins % 60
      println("ANGLE $angle -> FAJR TIME: ${String.format("%02d:%02d", h, m)}")
    }
  }

  private fun Double.roundToInt() = Math.round(this).toInt()
}
