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
    primary = Color(0xFF356B52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBE1),
    onPrimaryContainer = Color(0xFF173A29),
    secondary = Color(0xFF5C6F63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E9E5),
    onSecondaryContainer = Color(0xFF293A30),
    tertiary = Color(0xFF61706A),
    background = Color(0xFFF5F7F5),
    onBackground = Color(0xFF1A1D1B),
    surface = Color(0xFFFCFDFC),
    onSurface = Color(0xFF1A1D1B),
    surfaceVariant = Color(0xFFE6EAE7),
    onSurfaceVariant = Color(0xFF515A54),
    outline = Color(0xFF7A837D),
    outlineVariant = Color(0xFFC8CEC9),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColours = darkColorScheme(
    primary = Color(0xFF93BCA3),
    onPrimary = Color(0xFF102B1B),
    primaryContainer = Color(0xFF294235),
    onPrimaryContainer = Color(0xFFD0E7D6),
    secondary = Color(0xFFB1C2B7),
    onSecondary = Color(0xFF23342A),
    secondaryContainer = Color(0xFF35423A),
    onSecondaryContainer = Color(0xFFD9E4DC),
    tertiary = Color(0xFFB6C4BC),
    background = Color(0xFF0E1110),
    onBackground = Color(0xFFE7EAE8),
    surface = Color(0xFF161A17),
    onSurface = Color(0xFFE7EAE8),
    surfaceVariant = Color(0xFF222824),
    onSurfaceVariant = Color(0xFFBBC3BE),
    outline = Color(0xFF87918A),
    outlineVariant = Color(0xFF3B433E),
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
