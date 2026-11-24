package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val NabdDarkColorScheme = darkColorScheme(
    primary = NabdViolet,
    onPrimary = Color.White,
    primaryContainer = NabdSurfaceElevated,
    onPrimaryContainer = NabdCyan,
    secondary = NabdCyan,
    onSecondary = NabdBg,
    secondaryContainer = Color(0xFF12324A),
    onSecondaryContainer = NabdCyan,
    tertiary = NabdPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3E1531),
    onTertiaryContainer = NabdPink,
    background = NabdBg,
    onBackground = NabdTextPrimary,
    surface = NabdSurface,
    onSurface = NabdTextPrimary,
    surfaceVariant = NabdSurfaceElevated,
    onSurfaceVariant = NabdTextSecondary,
    outline = NabdBorder,
    outlineVariant = Color(0xFF1B223C),
    error = NabdRed,
    onError = Color.White
)

val NabdShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun NabdTvTheme(
    languageCode: String = "ar",
    content: @Composable () -> Unit
) {
    val layoutDirection = if (languageCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = if (languageCode == "ar") ArabicStrings else EnglishStrings

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = NabdDarkColorScheme,
            typography = NabdTypography,
            shapes = NabdShapes,
            content = content
        )
    }
}
