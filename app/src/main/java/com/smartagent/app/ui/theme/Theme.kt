package com.smartagent.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColours = lightColorScheme(
    primary = Color(0xFF167A35),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9FFD2),
    onPrimaryContainer = Color(0xFF073B18),
    secondary = Color(0xFF3D6750),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9EDE0),
    onSecondaryContainer = Color(0xFF183729),
    tertiary = Color(0xFF4D6A52),
    background = Color(0xFFF4F7F4),
    onBackground = Color(0xFF161B17),
    surface = Color(0xFFFCFEFC),
    onSurface = Color(0xFF161B17),
    surfaceVariant = Color(0xFFE2E9E2),
    onSurfaceVariant = Color(0xFF4B554D),
    outline = Color(0xFF748078),
    outlineVariant = Color(0xFFC3CCC4),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColours = darkColorScheme(
    primary = Color(0xFF7CFF4F),
    onPrimary = Color(0xFF071006),
    primaryContainer = Color(0xFF173B1B),
    onPrimaryContainer = Color(0xFFC9FFC0),
    secondary = Color(0xFFA7D7B1),
    onSecondary = Color(0xFF102D18),
    secondaryContainer = Color(0xFF253C2A),
    onSecondaryContainer = Color(0xFFD3E8D6),
    tertiary = Color(0xFFB7CCB9),
    background = Color(0xFF090D0A),
    onBackground = Color(0xFFE8EEE9),
    surface = Color(0xFF111713),
    onSurface = Color(0xFFE8EEE9),
    surfaceVariant = Color(0xFF1B241D),
    onSurfaceVariant = Color(0xFFB8C3BA),
    outline = Color(0xFF829087),
    outlineVariant = Color(0xFF344038),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5F1714)
)

private val SmartAgentShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val SmartAgentTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun SmartAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColours else LightColours,
        typography = SmartAgentTypography,
        shapes = SmartAgentShapes,
        content = content
    )
}
