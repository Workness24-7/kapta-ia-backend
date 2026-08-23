package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsDarkMode

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        content = content
    )
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
