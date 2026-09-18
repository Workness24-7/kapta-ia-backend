package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalIsDarkMode

/**
 * Fondos del sistema: un wallpaper por modo (claro/oscuro), sorteado en cada
 * inicio de sesión. Blur 29 + velo del color base para legibilidad. Al cambiar
 * de modo a mitad de sesión el fondo cruza con transición suave (Crossfade).
 */
object KaptaWallpaper {
    private const val PREFS = "kapta_wallpaper"
    private val light = listOf(
        R.drawable.wallpaper_light_one,
        R.drawable.wallpaper_light_two,
        R.drawable.wallpaper_light_three
    )
    private val dark = listOf(
        R.drawable.wallpaper_dark_one,
        R.drawable.wallpaper_dark_two,
        R.drawable.wallpaper_dark_three
    )

    fun rollOnLogin(context: android.content.Context) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putInt("light_idx", light.indices.random())
            .putInt("dark_idx", dark.indices.random())
            .apply()
    }

    fun currentRes(context: android.content.Context, isDark: Boolean): Int {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val key = if (isDark) "dark_idx" else "light_idx"
        val list = if (isDark) dark else light
        var idx = prefs.getInt(key, -1)
        if (idx !in list.indices) {
            idx = list.indices.random()
            prefs.edit().putInt(key, idx).apply()
        }
        return list[idx]
    }
}

/**
 * iOS grouped list background: flat systemGroupedBackground (#F2F2F7 light / black dark),
 * with the two soft accent glows preserved as liquid-glass ambience.
 */
@Composable
fun EtherealBackground(
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current
    // tintColor = Neutral de la paleta de la empresa (solo modo claro); oscuro siempre negro
    val bg = tintColor?.takeIf { !isDark } ?: if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
    val context = LocalContext.current
    val wallpaperRes = remember(isDark) { KaptaWallpaper.currentRes(context, isDark) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Wallpaper del modo actual con blur 29; Crossfade si cambia el modo.
        Crossfade(
            targetState = wallpaperRes,
            animationSpec = tween(700),
            modifier = Modifier.fillMaxSize()
        ) { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(29.dp)
            )
        }
        // Velo del color base para que el contenido siga legible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg.copy(alpha = if (isDark) 0.55f else 0.45f))
        )
        content()
    }
}

/**
 * iOS card: white rounded rectangle (insetGrouped), subtle shadow, hairline border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val effectiveBg = backgroundColor ?: if (isDark) Color(0xFF1C1C1E) else Color.White
    val effectiveBorder = borderColor ?: if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f)
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(effectiveBg)
            .border(
                width = borderWidth,
                color = effectiveBorder,
                shape = shape
            )
            .then(clickableModifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}
