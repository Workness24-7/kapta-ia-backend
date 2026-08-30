package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.remote.SyncState

/**
 * Custom 3D Location Pin Icon matching the KAPTA IA brand logo.
 */
@Composable
fun Kapta3DPinLogo(
    size: Dp = 36.dp
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        shadowElevation = 6.dp,
        modifier = Modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E3A8A),
                            Color(0xFF3B82F6),
                            Color(0xFF8B5CF6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.78f)
                    .clip(
                        GenericShape { sizePx, _ ->
                            val r = sizePx.width * 0.45f
                            val cx = sizePx.width / 2f
                            val cy = r
                            reset()
                            addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r))
                            moveTo(cx - r * 0.82f, cy + r * 0.35f)
                            lineTo(cx, sizePx.height)
                            lineTo(cx + r * 0.82f, cy + r * 0.35f)
                            close()
                        }
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF60A5FA),
                                Color(0xFF2563EB),
                                Color(0xFF1D4ED8)
                            )
                        )
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier.padding(top = (size.value * 0.08f).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "IA",
                        fontSize = (size.value * 0.28f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun KaptaLogoHeader(
    modifier: Modifier = Modifier,
    showSlogan: Boolean = true,
    fontSize: Int = 28,
    syncState: SyncState = SyncState.Idle
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                if (showSlogan) R.drawable.kapta_ia_logo_slogan else R.drawable.kapta_ia_logo
            ),
            contentDescription = "KAPTA IA",
            modifier = Modifier.height((fontSize * 1.5f).dp),
            contentScale = ContentScale.Fit
        )

        if (syncState is SyncState.Syncing) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        color = Color(0xFFD97706),
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (syncState as SyncState.Syncing).message,
                        fontSize = 10.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
