package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import com.example.data.local.entity.CompanyEntity

val LocalIsDarkMode = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = KaptaAccentLight,
    onPrimary = iOSLabelDark,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = KaptaAccentLight,
    onSecondary = iOSLabelDark,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFF34C759),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3F9D8),
    onTertiaryContainer = Color(0xFF052E16),
    background = iOSSystemGroupedBackgroundLight,
    onBackground = iOSLabelLight,
    surface = iOSSecondaryGroupedBackgroundLight,
    onSurface = iOSLabelLight,
    surfaceVariant = iOSTertiaryGroupedBackgroundLight,
    onSurfaceVariant = iOSSecondaryLabelLight,
    surfaceContainerLowest = iOSSecondaryGroupedBackgroundLight,
    surfaceContainerLow = Color(0xFFF8F8FA),
    surfaceContainer = iOSTertiaryGroupedBackgroundLight,
    surfaceContainerHigh = Color(0xFFE9E9ED),
    surfaceContainerHighest = Color(0xFFDEDEE2),
    outline = Color(0x1F000000),
    outlineVariant = iOSSeparatorLight,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFD6D2),
    onErrorContainer = Color(0xFF4A0000),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF9DA8FF),
    scrim = Color(0x99000000),
    surfaceTint = KaptaAccentLight
)

private val DarkColorScheme = darkColorScheme(
    primary = KaptaAccentDark,
    onPrimary = Color(0xFF16153D),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = KaptaAccentDark,
    onSecondary = Color(0xFF16153D),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF30D158),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF0E5A23),
    onTertiaryContainer = Color(0xFFD3F9D8),
    background = iOSSystemGroupedBackgroundDark,
    onBackground = iOSLabelDark,
    surface = iOSSecondaryGroupedBackgroundDark,
    onSurface = iOSLabelDark,
    surfaceVariant = iOSTertiaryGroupedBackgroundDark,
    onSurfaceVariant = iOSSecondaryLabelDark,
    surfaceContainerLowest = Color(0xFF161618),
    surfaceContainerLow = Color(0xFF242426),
    surfaceContainer = iOSTertiaryGroupedBackgroundDark,
    surfaceContainerHigh = Color(0xFF353538),
    surfaceContainerHighest = Color(0xFF444446),
    outline = Color(0x28FFFFFF),
    outlineVariant = iOSSeparatorDark,
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF7A2A26),
    onErrorContainer = Color(0xFFFFD6D2),
    inverseSurface = Color(0xFFEBE8EA),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = Color(0xFF4F46E5),
    scrim = Color(0xCC000000),
    surfaceTint = KaptaAccentDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalIsDarkMode provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun parseBrandColor(hex: String?, fallback: Color): Color = try {
    Color(AndroidColor.parseColor(hex))
} catch (e: Exception) {
    fallback
}

private fun isLightColor(color: Color): Boolean {
    if (color.alpha < 0.01f) return true
    val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return luminance > 0.6f
}

fun businessColorScheme(company: CompanyEntity): ColorScheme {
    val primary = parseBrandColor(company.primaryColorHex, Color(0xFF4F46E5))
    val secondary = parseBrandColor(company.secondaryColorHex, Color(0xFF3B82F6))
    val tertiary = parseBrandColor(company.tertiaryColorHex, Color(0xFF10B981))
    val neutral = parseBrandColor(company.neutralColorHex, Color(0xFF0F172A))
    val onPrimary = if (isLightColor(primary)) Color.Black else Color.White
    val onSecondary = if (isLightColor(secondary)) Color.Black else Color.White
    val onTertiary = if (isLightColor(tertiary)) Color.Black else Color.White
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondary.copy(alpha = 0.12f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiary.copy(alpha = 0.12f),
        onTertiaryContainer = tertiary,
        background = Color.White,
        onBackground = neutral,
        surface = Color.White,
        onSurface = neutral,
        surfaceVariant = neutral.copy(alpha = 0.08f),
        onSurfaceVariant = neutral.copy(alpha = 0.65f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF8F8FA),
        surfaceContainer = Color(0xFFF2F2F5),
        surfaceContainerHigh = Color(0xFFE9E9ED),
        surfaceContainerHighest = Color(0xFFDEDEE2),
        outline = neutral.copy(alpha = 0.3f),
        outlineVariant = neutral.copy(alpha = 0.15f),
        error = Color(0xFFFF3B30),
        onError = Color.White,
        errorContainer = Color(0xFFFFD6D2),
        onErrorContainer = Color(0xFF4A0000),
        inverseSurface = neutral,
        inverseOnSurface = if (isLightColor(neutral)) Color.Black else Color.White,
        inversePrimary = primary,
        scrim = Color(0x99000000),
        surfaceTint = primary
    )
}

@Composable
fun BusinessTheme(company: CompanyEntity, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = businessColorScheme(company),
        typography = Typography
    ) {
        content()
    }
}