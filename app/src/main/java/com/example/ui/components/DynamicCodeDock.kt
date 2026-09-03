package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ClaveDinamica
import kotlinx.coroutines.delay

@Composable
fun DynamicCodeDock(
    clave: ClaveDinamica?,
    primaryColor: Color = Color(0xFF4F46E5),
    secondaryColor: Color = Color(0xFF3B82F6),
    modifier: Modifier = Modifier
) {
    if (clave == null || clave.codigo.isBlank()) return

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    // tick cada 200ms para animacion fluida
    LaunchedEffect(clave.codigo, clave.expira) {
        while (true) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }

    val totalMs = 60_000L
    val remainingMs = (clave.expira - now).coerceIn(0L, totalMs)
    val progress = 1f - (remainingMs.toFloat() / totalMs.toFloat())
    val clamped = progress.coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.35f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Circulo con progreso 60s
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    // fondo
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        style = Stroke(width = stroke)
                    )
                    // progreso desde arriba (-90 grados)
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                // punto decorativo secundario
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = clave.codigo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
