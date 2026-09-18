package com.example.classschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.classschedule.domain.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6F64),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9E3),
    onPrimaryContainer = Color(0xFF123B33),
    secondary = Color(0xFFA17B35),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5C5),
    onSecondaryContainer = Color(0xFF3C2A08),
    background = Color(0xFFF4F6F4),
    onBackground = Color(0xFF19312A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF19312A),
    surfaceVariant = Color(0xFFE8EEEB),
    onSurfaceVariant = Color(0xFF52655F),
    outline = Color(0xFFCAD6D1),
    error = Color(0xFFB54A3A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93CBBF),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1F5147),
    onPrimaryContainer = Color(0xFFB6EADD),
    secondary = Color(0xFFE2BF78),
    onSecondary = Color(0xFF3A2A08),
    secondaryContainer = Color(0xFF5C4617),
    onSecondaryContainer = Color(0xFFFFDEA0),
    background = Color(0xFF151C19),
    onBackground = Color(0xFFE5EEE9),
    surface = Color(0xFF1D2723),
    onSurface = Color(0xFFE5EEE9),
    surfaceVariant = Color(0xFF35423E),
    onSurfaceVariant = Color(0xFFB7C9C2),
    outline = Color(0xFF53645E),
    error = Color(0xFFFFB4A5),
    onError = Color(0xFF5F160B)
)

@Composable
fun ScheduleTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
