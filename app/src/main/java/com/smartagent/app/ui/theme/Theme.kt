package com.smartagent.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColours = lightColorScheme(
    primary = Color(0xFF125B50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1EDE7),
    onPrimaryContainer = Color(0xFF073C34),
    secondary = Color(0xFFB75B17),
    secondaryContainer = Color(0xFFFFDBBF),
    background = Color(0xFFFFF8EC),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFE9E2D7),
    error = Color(0xFFBA1A1A)
)

private val DarkColours = darkColorScheme(
    primary = Color(0xFF8FD5C8),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0B4D43),
    secondary = Color(0xFFFFB77F),
    secondaryContainer = Color(0xFF733300),
    background = Color(0xFF15130F),
    surface = Color(0xFF1D1B17),
    surfaceVariant = Color(0xFF4A4540),
    error = Color(0xFFFFB4AB)
)

@Composable
fun SmartAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColours else LightColours,
        content = content
    )
}
