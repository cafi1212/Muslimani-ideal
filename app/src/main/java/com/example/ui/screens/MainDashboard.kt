package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.WarmGold
import com.example.utils.CityPreset
import com.example.utils.PrayerTimeItem
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Mosque Dome Logo",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Muslimani Ideal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Navigation Bar with consistent active pill indicators
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = viewModel.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == 0) Icons.Default.AccessTimeFilled else Icons.Outlined.AccessTime,
                            contentDescription = "Prayer Times Screen"
                        )
                    },
                    label = { Text("Prayers") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = viewModel.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == 1) Icons.AutoMirrored.Filled.MenuBook else Icons.Outlined.Book,
                            contentDescription = "Daily Quotes Screen"
                        )
                    },
                    label = { Text("Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = viewModel.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (viewModel.selectedTab == 2) Icons.Default.Fingerprint else Icons.Default.TouchApp,
                            contentDescription = "Tasbeeh subhah Counter Screen"
                        )
                    },
                    label = { Text("Tasbeeh") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewModel.selectedTab) {
                0 -> PrayerScreen(viewModel = viewModel)
                1 -> QuotesScreen(viewModel = viewModel)
                2 -> TasbeehScreen(viewModel = viewModel)
            }
        }
    }

    // Interactive custom congratulations popup when tasbeeh limit is achieved
    if (viewModel.showTargetReachedDialog) {
        TargetAchievedDialog(
            dhikrPhrase = if (viewModel.selectedDhikrPhrase == "Custom") viewModel.customDhikrInput else viewModel.selectedDhikrPhrase,
            targetCount = viewModel.tasbeehTarget,
            onDismiss = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.showTargetReachedDialog = false
                viewModel.currentTasbeehCount = 0
            }
        )
    }
}

// ====================== PRAYER TIMES SCREEN ======================

@Composable
fun PrayerScreen(viewModel: MainViewModel) {
    val times by remember { derivedStateOf { viewModel.todayPrayerTimes.value } }
    val nextInfo by remember { derivedStateOf { viewModel.nextPrayerInfo.value } }
    val prayerLogs by viewModel.todayPrayerLogs.collectAsStateWithLifecycle()
    var showCityMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prayer_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP Gradient Banner with real-time countdown clock and active preset selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("countdown_banner_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val brush = Brush.linearGradient(
                                colors = listOf(
                                    EmeraldGreen,
                                    Color(0xFF064E3B)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                            drawRect(brush = brush)
                        }
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Location Picker Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { showCityMenu = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("city_picker_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Active Location",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = viewModel.selectedCityName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Expand Cities List",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Presets Dropdown
                        DropdownMenu(
                            expanded = showCityMenu,
                            onDismissRequest = { showCityMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            PrayerTimesCalculator.presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        viewModel.selectCity(preset.name)
                                        showCityMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Current Digital Live Time Ticker
                        val formattedTime = remember(viewModel.currentTimeLong, viewModel.selectedCityName, viewModel.useDeviceTimezoneForClock, viewModel.is24HourFormat) {
                            val preset = PrayerTimesCalculator.getPreset(viewModel.selectedCityName)
                            val pattern = if (viewModel.is24HourFormat) "HH:mm:ss" else "hh:mm:ss a"
                            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                                timeZone = if (viewModel.useDeviceTimezoneForClock) {
                                    TimeZone.getDefault()
                                } else {
                                    TimeZone.getTimeZone(preset.timezoneId)
                                }
                            }
                            sdf.format(Date(viewModel.currentTimeLong))
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        val activeTzLabel = remember(viewModel.selectedCityName, viewModel.useDeviceTimezoneForClock) {
                            if (viewModel.useDeviceTimezoneForClock) {
                                "Device Time (${TimeZone.getDefault().id})"
                            } else {
                                val preset = PrayerTimesCalculator.getPreset(viewModel.selectedCityName)
                                "${preset.name} Time (${preset.timezoneId})"
                            }
                        }
                        Text(
                            text = activeTzLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Next Prayer Banner
                        Text(
                            text = "Next: ${nextInfo.name} • Ends in ${nextInfo.countdown}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Next Prayer Linear Countdown Track
                        LinearProgressIndicator(
                            progress = { nextInfo.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Hijri Date and Qibla Row
        item {
            val preset = remember(viewModel.selectedCityName) {
                PrayerTimesCalculator.getPreset(viewModel.selectedCityName)
            }
            val qiblaAngle = remember(preset) {
                PrayerTimesCalculator.calculateQibla(preset.latitude, preset.longitude)
            }
            
            // Format dynamic Hijri date natively
            val hijriStr = remember(viewModel.currentTimeLong, viewModel.selectedCityName) {
                try {
                    val preset = PrayerTimesCalculator.getPreset(viewModel.selectedCityName)
                    val zoneId = java.time.ZoneId.of(preset.timezoneId)
                    val localDate = java.time.LocalDate.now(zoneId)
                    val hijriDate = java.time.chrono.HijrahDate.from(localDate)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
                    formatter.format(hijriDate) + " AH"
                } catch (e: Throwable) {
                    "15 Dhul-Qi'dah 1447 AH" // Safe elegant fallback
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Dynamic Hijri Date
                Card(
                    modifier = Modifier.weight(1f).height(115.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HIJRI DATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        
                        Text(
                            text = hijriStr,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 2
                        )
                        
                        Text(
                            text = "Balkan Region Calendar",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Right: Dynamic Qibla Direction
                Card(
                    modifier = Modifier.weight(1.1f).height(115.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "QIBLA DIRECTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f° SE", qiblaAngle),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "From Mecca",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Drawing a beautiful small compass pointing to Qibla
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val compassColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            val pointerColor = MaterialTheme.colorScheme.tertiary // Gold
                            val primaryColor = MaterialTheme.colorScheme.primary

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw compass ring
                                drawCircle(
                                    color = compassColor,
                                    radius = size.width / 2.0f - 2.dp.toPx(),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Draw center dot
                                drawCircle(
                                    color = primaryColor,
                                    radius = 3.dp.toPx()
                                )
                                // Draw North marker
                                drawLine(
                                    color = primaryColor,
                                    start = Offset(size.width / 2f, 2.dp.toPx()),
                                    end = Offset(size.width / 2f, 6.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )

                                // Draw Qibla pointer line at calculated angle
                                val angleRad = Math.toRadians(qiblaAngle - 90.0)
                                val pointerLen = size.width / 2f - 4.dp.toPx()
                                val tipX = (size.width / 2f + pointerLen * cos(angleRad)).toFloat()
                                val tipY = (size.height / 2f + pointerLen * sin(angleRad)).toFloat()

                                drawLine(
                                    color = pointerColor,
                                    start = Offset(size.width / 2f, size.height / 2f),
                                    end = Offset(tipX, tipY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Checklist Prayer Tracker Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checklist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today's Prayer Logs",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Tap circle to track",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }

        // List out the 6 daily prayers with alarms toggles and status logs
        items(times) { item ->
            // Check if this prayer matches the incoming logged state
            val storedLog = prayerLogs.find { it.prayerName == item.name }
            val status = storedLog?.status ?: "none"

            val isCurrentActive = nextInfo.name == item.name

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_row_${item.name.lowercase()}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentActive) 3.dp else 1.dp),
                border = if (isCurrentActive) {
                    CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.radialGradient(
                            colors = listOf(WarmGold, MaterialTheme.colorScheme.primary)
                        ),
                        width = 1.8.dp
                    )
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leftside Time Signifier Info
                    val actualVector = when (item.name) {
                        "Fajr" -> Icons.Default.WbSunny
                        "Sunrise" -> Icons.Default.BrightnessMedium
                        "Dhuhr" -> Icons.Default.WbSunny
                        "Asr" -> Icons.Default.Cloud
                        "Maghrib" -> Icons.Default.BrightnessMedium
                        else -> Icons.Default.Nightlight
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actualVector,
                            contentDescription = item.name,
                            tint = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        val displayedTime = remember(item.timeStr, viewModel.is24HourFormat) {
                            try {
                                val parts = item.timeStr.split(":")
                                if (parts.size != 2) item.timeStr
                                else {
                                    val h = parts[0].toInt()
                                    val m = parts[1].toInt()
                                    if (viewModel.is24HourFormat) {
                                        String.format(Locale.US, "%02d:%02d", h, m)
                                    } else {
                                        val amPm = if (h >= 12) "PM" else "AM"
                                        val displayH = when {
                                            h == 0 -> 12
                                            h > 12 -> h - 12
                                            else -> h
                                        }
                                        String.format(Locale.US, "%d:%02d %s", displayH, m, amPm)
                                    }
                                }
                            } catch (e: Exception) {
                                item.timeStr
                            }
                        }
                        Text(
                            text = displayedTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Middle Bell Volume Settings Alarm Clicker
                    val alarmEnabled = viewModel.notificationStates[item.name] ?: false
                    IconButton(
                        onClick = { viewModel.togglePrayerNotification(item.name) },
                        modifier = Modifier
                            .size(48.dp) // Accessibility Target Size
                            .testTag("bell_toggle_${item.name.lowercase()}"),
                    ) {
                        Icon(
                            imageVector = if (alarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = if (alarmEnabled) "Mute alert" else "Unmute alert",
                            tint = if (alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Right checklist tracker trigger button
                    val trackerIcon = when (status) {
                        "on_time" -> Icons.Filled.CheckCircle
                        "late" -> Icons.Filled.AccessTime
                        "missed" -> Icons.Filled.Cancel
                        else -> Icons.Outlined.RadioButtonUnchecked
                    }
                    val trackerColor = when (status) {
                        "on_time" -> Color(0xFF2E7D32) // Soft Green
                        "late" -> Color(0xFFE65100) // Deep Orange
                        "missed" -> Color(0xFFC62828) // Deep Red
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    }
                    val trackerLabel = when (status) {
                        "on_time" -> "On Time"
                        "late" -> "Late"
                        "missed" -> "Missed"
                        else -> "Log"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.cyclePrayerStatus(item.name) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("log_prayer_${item.name.lowercase()}"),
                    ) {
                        Icon(
                            imageVector = trackerIcon,
                            contentDescription = "Prayer Status $trackerLabel",
                            tint = trackerColor,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = trackerLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = trackerColor.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }

        // Additional Settings section for Jurisprudence & Preferences
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Calculation & Time Settings",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Asr Juristic Method",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.4f)
                        )
                        LazyRow(
                            modifier = Modifier.weight(0.6f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = viewModel.asrJuristicMethod == "Standard",
                                    onClick = { viewModel.asrJuristicMethod = "Standard" },
                                    label = { Text("Standard") },
                                    modifier = Modifier.testTag("asr_method_standard")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.asrJuristicMethod == "Hanafi",
                                    onClick = { viewModel.asrJuristicMethod = "Hanafi" },
                                    label = { Text("Hanafi") },
                                    modifier = Modifier.testTag("asr_method_hanafi")
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fajr Calculation",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.4f)
                        )
                        LazyRow(
                            modifier = Modifier.weight(0.6f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = viewModel.calculationMethod == "MWL",
                                    onClick = { viewModel.calculationMethod = "MWL" },
                                    label = { Text("MWL") },
                                    modifier = Modifier.testTag("calc_method_mwl")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.calculationMethod == "ISNA",
                                    onClick = { viewModel.calculationMethod = "ISNA" },
                                    label = { Text("ISNA") },
                                    modifier = Modifier.testTag("calc_method_isna")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.calculationMethod == "Balkans12",
                                    onClick = { viewModel.calculationMethod = "Balkans12" },
                                    label = { Text("Balkans12") },
                                    modifier = Modifier.testTag("calc_method_balkans12")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.calculationMethod == "Balkans10",
                                    onClick = { viewModel.calculationMethod = "Balkans10" },
                                    label = { Text("Balkans10") },
                                    modifier = Modifier.testTag("calc_method_balkans10")
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fajr Time Adjust",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.4f)
                        )
                        LazyRow(
                            modifier = Modifier.weight(0.6f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = viewModel.fajrOffsetMins == 0,
                                    onClick = { viewModel.fajrOffsetMins = 0 },
                                    label = { Text("None") },
                                    modifier = Modifier.testTag("fajr_offset_0")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.fajrOffsetMins == 15,
                                    onClick = { viewModel.fajrOffsetMins = 15 },
                                    label = { Text("+15m") },
                                    modifier = Modifier.testTag("fajr_offset_15")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.fajrOffsetMins == 30,
                                    onClick = { viewModel.fajrOffsetMins = 30 },
                                    label = { Text("+30m") },
                                    modifier = Modifier.testTag("fajr_offset_30")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.fajrOffsetMins == 45,
                                    onClick = { viewModel.fajrOffsetMins = 45 },
                                    label = { Text("+45m") },
                                    modifier = Modifier.testTag("fajr_offset_45")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = viewModel.fajrOffsetMins == 60,
                                    onClick = { viewModel.fajrOffsetMins = 60 },
                                    label = { Text("+60m") },
                                    modifier = Modifier.testTag("fajr_offset_60")
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time Format Preference",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            FilterChip(
                                selected = !viewModel.is24HourFormat,
                                onClick = { viewModel.is24HourFormat = false },
                                label = { Text("12-Hour") },
                                modifier = Modifier.testTag("time_format_12h")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = viewModel.is24HourFormat,
                                onClick = { viewModel.is24HourFormat = true },
                                label = { Text("24-Hour") },
                                modifier = Modifier.testTag("time_format_24h")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Main Clock Sync",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            FilterChip(
                                selected = viewModel.useDeviceTimezoneForClock,
                                onClick = { viewModel.useDeviceTimezoneForClock = true },
                                label = { Text("Device") },
                                modifier = Modifier.testTag("tz_sync_device")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = !viewModel.useDeviceTimezoneForClock,
                                onClick = { viewModel.useDeviceTimezoneForClock = false },
                                label = { Text("Selected City") },
                                modifier = Modifier.testTag("tz_sync_city")
                            )
                        }
                    }
                }
            }
        }
    }
}


// ====================== DAILY ISLAMIC QUOTES SCREEN ======================

@Composable
fun QuotesScreen(viewModel: MainViewModel) {
    val systemFavorites by viewModel.favoriteQuotes.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val quoteOfTheDay = remember(viewModel.currentTimeLong) {
        QuoteLibrary.getQuoteOfTheDay(viewModel.currentTimeLong)
    }

    val displayQuotes = remember(viewModel.selectedQuoteCategory, viewModel.quotesSearchQuery, systemFavorites) {
        val baseList = if (viewModel.selectedQuoteCategory == "Favorites") {
            // Map Favorites DB Entity back to Quotes Model list
            systemFavorites.map {
                Quote(
                    id = it.id,
                    text = it.quoteText,
                    source = it.source,
                    category = it.category
                )
            }
        } else {
            QuoteLibrary.quotes
        }

        // Apply category filter
        val categoryFiltered = if (viewModel.selectedQuoteCategory != "All" && viewModel.selectedQuoteCategory != "Favorites") {
            baseList.filter { it.category.equals(viewModel.selectedQuoteCategory, ignoreCase = true) }
        } else {
            baseList
        }

        // Apply search query filter
        if (viewModel.quotesSearchQuery.isNotBlank()) {
            categoryFiltered.filter {
                it.text.contains(viewModel.quotesSearchQuery, ignoreCase = true) ||
                        it.source.contains(viewModel.quotesSearchQuery, ignoreCase = true)
            }
        } else {
            categoryFiltered
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quotes_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Quote of the Day
        item {
            val isFavOfTheDay = systemFavorites.any { it.id == quoteOfTheDay.id }
            QuoteOfTheDayCard(
                quote = quoteOfTheDay,
                isFavorited = isFavOfTheDay,
                onToggleFavorite = { viewModel.toggleQuoteBookmark(quoteOfTheDay, isFavOfTheDay) }
            )
        }

        // Search text-field
        item {
            TextField(
                value = viewModel.quotesSearchQuery,
                onValueChange = { viewModel.quotesSearchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quotes_search_input"),
                placeholder = { Text("Search Quran, Hadith, or Wisdom...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (viewModel.quotesSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.quotesSearchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // Horizontal Category Selectors Slider
        item {
            val categories = listOf("All", "Quran", "Hadith", "Wisdom", "Favorites")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = viewModel.selectedQuoteCategory == cat
                    val count = if (cat == "Favorites") systemFavorites.size else 0

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedQuoteCategory = cat },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat)
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Empty Search Results Placeholder
        if (displayQuotes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPasteOff,
                        contentDescription = "Empty list placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Reminders Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Try clearing search keywords or adding favorites.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Render individual quote cards with share sheets
            items(displayQuotes) { item ->
                val isBookmarked = systemFavorites.any { it.id == item.id }
                QuoteCardItem(
                    quote = item,
                    isFavorited = isBookmarked,
                    onToggleFavorite = { viewModel.toggleQuoteBookmark(item, isBookmarked) }
                )
            }
        }
    }
}

// Visual Quote Card representing featured Daily quote
@Composable
fun QuoteOfTheDayCard(
    quote: Quote,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clippy = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quote_of_the_day_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFCA8A04), // Warm amber gold
                            Color(0xFF9A3412) // Reddish-sunset orange
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    )
                    drawRect(brush = brush)
                }
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brightness5,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "QUOTE OF THE DAY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = quote.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Render beautiful Arabic verses if present
                if (quote.arabic.isNotBlank()) {
                    Text(
                        text = quote.arabic,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Normal,
                            lineHeight = 36.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Text(
                    text = "“${quote.text}”",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                        textAlign = if (quote.arabic.isNotBlank()) TextAlign.Center else TextAlign.Start
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = quote.source,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (quote.arabic.isNotBlank()) TextAlign.Center else TextAlign.Start
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.White.copy(alpha = 0.2f))

                // Bottom operations actions row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clippy.setText(AnnotatedString("${quote.text} \n— ${quote.source}"))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Quote of Day",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "“${quote.text}”\n— ${quote.source}\n(Sent via Muslimani Ideal app)")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Islamic Quote"))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Quote of Day",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Quote of Day",
                            tint = if (isFavorited) Color.Red else Color.White
                        )
                    }
                }
            }
        }
    }
}

// Simple Quote Catalog List Row component card
@Composable
fun QuoteCardItem(
    quote: Quote,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clippy = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quote_item_card_${quote.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = quote.category.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (quote.arabic.isNotBlank()) {
                Text(
                    text = quote.arabic,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }

            Text(
                text = "“${quote.text}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    textAlign = if (quote.arabic.isNotBlank()) TextAlign.Center else TextAlign.Start
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quote.source,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (quote.arabic.isNotBlank()) TextAlign.Center else TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clippy.setText(AnnotatedString("${quote.text} \n— ${quote.source}"))
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Quote text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "“${quote.text}”\n— ${quote.source}\n(Sent via Muslimani Ideal app)")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Islamic Quote"))
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Quote",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark quote",
                        tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// ====================== TASBEEH SUBHAH COUNTER SCREEN ======================

@Composable
fun TasbeehScreen(viewModel: MainViewModel) {
    val hapticFeedback = LocalHapticFeedback.current
    val lifetimeLogs by viewModel.dhikrLogs.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Smooth squish scale effects trigger for tactile feedback
    var countBtnScale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = countBtnScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "subhah_button_scale"
    )

    // Common Dhikr selection phrases configuration list
    val phrases = listOf("SubhanAllah", "Alhamdulillah", "AllahuAkbar", "Astaghfirullah", "Custom")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasbeeh_screen_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal preset chips configuration
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select Dhikr",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(phrases) { phrase ->
                        val selected = viewModel.selectedDhikrPhrase == phrase
                        FilterChip(
                            selected = selected,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectDhikr(phrase)
                            },
                            label = { Text(phrase) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // Expanded Custom Dhikr Phrase typing field
        if (viewModel.selectedDhikrPhrase == "Custom") {
            item {
                OutlinedTextField(
                    value = viewModel.customDhikrInput,
                    onValueChange = { viewModel.customDhikrInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_dhikr_input"),
                    label = { Text("Type custom Dhikr phrase...") },
                    placeholder = { Text("E.g., La ilaha illallah") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Subhah bead cycles targets setup row buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target Goal:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val targets = listOf(33, 99, 100, 0) // 0 implies Continuous
                    targets.forEach { target ->
                        val selected = viewModel.tasbeehTarget == target
                        OutlinedButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.tasbeehTarget = target
                                viewModel.currentTasbeehCount = 0
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (target == 0) "Continuous" else target.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Master Tactical bead counter UI circle clicker
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .scale(animatedScale)
                    .size(240.dp)
                    .clip(CircleShape)
                    .testTag("tasbeeh_clicker_circle")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 120.dp, color = MaterialTheme.colorScheme.primary)
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.handleTasbeehClick()

                        // Trigger scale bounce spring effect
                        scope.launch {
                            countBtnScale = 0.90f
                            delay(40)
                            countBtnScale = 1.0f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background visual outer rings using Canvas
                val progressAngle = remember(viewModel.currentTasbeehCount, viewModel.tasbeehTarget) {
                    if (viewModel.tasbeehTarget > 0) {
                        (viewModel.currentTasbeehCount.toFloat() / viewModel.tasbeehTarget.toFloat()).coerceIn(0f, 1f) * 360f
                    } else {
                        // Continuous rotation animation Mocked base value
                        (viewModel.currentTasbeehCount % 33) / 33f * 360f
                    }
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val accentGold = MaterialTheme.colorScheme.tertiary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Raw Track background ring
                    drawCircle(
                        color = trackColor,
                        radius = size.width / 2.0f - 12.dp.toPx(),
                        style = Stroke(width = 10.dp.toPx())
                    )

                    // Draw completed progress sweep track
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = progressAngle,
                        useCenter = false,
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            width = size.width - 24.dp.toPx(),
                            height = size.height - 24.dp.toPx()
                        ),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw moving subhah bead on progress point for immersive feel
                    val angleRad = (progressAngle - 90f) * PI / 180f
                    val r = size.width / 2f - 12.dp.toPx()
                    val beadX = (size.width / 2f + r * cos(angleRad)).toFloat()
                    val beadY = (size.height / 2f + r * sin(angleRad)).toFloat()

                    drawCircle(
                        color = accentGold,
                        radius = 8.dp.toPx(),
                        center = Offset(beadX, beadY)
                    )
                }

                // Internal text elements inside the clicker
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (viewModel.selectedDhikrPhrase == "Custom") {
                            if (viewModel.customDhikrInput.isNotBlank()) viewModel.customDhikrInput else "Dhikr"
                        } else {
                            viewModel.selectedDhikrPhrase
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = viewModel.currentTasbeehCount.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    if (viewModel.tasbeehTarget > 0) {
                        Text(
                            text = "/ ${viewModel.tasbeehTarget}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "Continuous",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TAP HERE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }

        // Subhah quick control settings buttons: manual reset, increment sets
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear reset
                OutlinedButton(
                    onClick = {
                        if (viewModel.currentTasbeehCount > 0) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.forceResetTasbeeh()
                        }
                    },
                    modifier = Modifier.testTag("tasbeeh_reset_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = viewModel.currentTasbeehCount > 0
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partial Save & Reset")
                }
            }
        }

        // Tracker visual log header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Dhikr History Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (lifetimeLogs.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearDhikrLogs() },
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Text(
                            text = "Clear All",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // History logs empty lists placeholders
        if (lifetimeLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HistoryEdu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No history recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Successfully complete targets or hit reset to save sets.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            // Render logs in sequence
            items(lifetimeLogs) { log ->
                DhikrLogItem(log = log, onDelete = { viewModel.deleteDhikrLog(log.id) })
            }
        }
    }
}

// Immersive targeted congratulations dialog when subhah rounds are fully finalized
@Composable
fun TargetAchievedDialog(
    dhikrPhrase: String,
    targetCount: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("target_achieved_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gold Star circular visual background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Target Achieved Success",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SubhanAllah!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You completed your goal of $targetCount repetitions of:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "“$dhikrPhrase”",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_dismiss_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Alhamdulillah",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }
}

// Render row inside list of daily dhikr completions
@Composable
fun DhikrLogItem(
    log: DhikrLog,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dhikr_log_item_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = log.phrase,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                val formattedTime = remember(log.timestamp) {
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    sdf.format(Date(log.timestamp))
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "x${log.count}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete dhikr item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
