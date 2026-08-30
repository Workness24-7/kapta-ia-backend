package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Fuente global de la app: DM Sans (variable, res/font/dm_sans.ttf)
val DmSans = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

// Tipografía estilo SF Pro (iOS) mapeada a estilos Material3
val Typography = Typography(
    displayLarge = TextStyle( // Large Title 34
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.0.sp
    ),
    displayMedium = TextStyle( // Title 1 28
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.0.sp
    ),
    displaySmall = TextStyle( // Title 2 22
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.0.sp
    ),
    headlineLarge = TextStyle( // Title 3 20
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.0.sp
    ),
    headlineMedium = TextStyle( // Headline 17
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.0.sp
    ),
    titleLarge = TextStyle( // 17 semibold (Headline alternative)
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.0.sp
    ),
    titleMedium = TextStyle( // 16 semibold
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.0.sp
    ),
    titleSmall = TextStyle( // 14 semibold
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.0.sp
    ),
    bodyLarge = TextStyle( // Body 17
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.0.sp
    ),
    bodyMedium = TextStyle( // Callout 16
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.0.sp
    ),
    bodySmall = TextStyle( // Subheadline 15
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.0.sp
    ),
    labelLarge = TextStyle( // 15 medium
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.0.sp
    ),
    labelMedium = TextStyle( // Footnote 13
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.0.sp
    ),
    labelSmall = TextStyle( // Caption 2 11
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.0.sp
    )
)