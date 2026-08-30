package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimpleLineChart(
    dataPoints: List<Float> = listOf(20f, 38f, 32f, 62f, 75f, 98f),
    labels: List<String> = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun"),
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6366F1)
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.isEmpty()) return@Canvas
                val width = size.width
                val height = size.height - 10.dp.toPx()
                val spacing = width / (dataPoints.size - 1)
                val maxVal = dataPoints.maxOrNull() ?: 1f

                val path = Path()
                val fillPath = Path()
                val points = mutableListOf<Offset>()

                dataPoints.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = height - (value / maxVal * (height - 15.dp.toPx()))
                    points.add(Offset(x, y))
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prev = points[index - 1]
                        val controlX1 = prev.x + spacing / 2
                        val controlY1 = prev.y
                        val controlX2 = x - spacing / 2
                        val controlY2 = y
                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.lineTo(0f, height)
                fillPath.close()

                // Draw translucent filled area under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.0f)
                        )
                    )
                )

                // Draw main curve
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw data point circles
                points.forEachIndexed { idx, point ->
                    val isLast = idx == points.size - 1
                    val pointRadius = if (isLast) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(color = lineColor, radius = pointRadius, center = point)
                    drawCircle(color = Color.White, radius = pointRadius / 2f, center = point)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == labels.size - 1
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    percentages: List<Float> = listOf(77f, 58f, 13f, 6f, 4f),
    colors: List<Color> = listOf(
        Color(0xFF10B981),
        Color(0xFF3B82F6),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF8B5CF6)
    ),
    centerText: String = "320",
    centerSubtext: String = "Empresas",
    onSegmentClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(160.dp)
            .then(
                if (onSegmentClick != null) {
                    Modifier.pointerInput(percentages) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            if (kotlin.math.sqrt(dx * dx + dy * dy) <= minOf(size.width, size.height) / 2f) {
                                var ang = Math.toDegrees(
                                    kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                                ).toFloat() + 90f
                                if (ang < 0f) ang += 360f
                                val total = percentages.sum().coerceAtLeast(0.001f)
                                var acumulado = 0f
                                var elegido = percentages.lastIndex
                                for (i in percentages.indices) {
                                    acumulado += (percentages[i] / total) * 360f
                                    if (ang <= acumulado) {
                                        elegido = i
                                        break
                                    }
                                }
                                onSegmentClick(elegido)
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = percentages.sum()
            var startAngle = -90f
            val strokeWidth = 22.dp.toPx()

            percentages.forEachIndexed { index, value ->
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = colors.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 3f, // gap
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
                startAngle += sweepAngle
            }
        }

        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = centerSubtext,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
