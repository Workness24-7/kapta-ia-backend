package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CompanyEntity
import com.example.ui.theme.LocalIsDarkMode

data class KaptaNotificacion(
    val empresa: CompanyEntity,
    val tipo: String,
    val mensaje: String,
    val color: Color
)

fun construirNotificaciones(companies: List<CompanyEntity>): List<KaptaNotificacion> {
    val notifs = mutableListOf<KaptaNotificacion>()
    for (c in companies) {
        if (c.status.equals("Eliminado", ignoreCase = true)) continue
        val estado = c.getEffectiveStatus()
        when {
            estado.contains("Suspend", ignoreCase = true) || estado.contains("Vencid", ignoreCase = true) ->
                notifs += KaptaNotificacion(
                    c, "suspendida",
                    "Membresía ${if (estado.contains("Vencid", true)) "vencida" else "suspendida"} hace ${-c.expirationDays} días",
                    Color(0xFFFF453A)
                )
            estado.contains("Vencer", ignoreCase = true) ->
                notifs += KaptaNotificacion(
                    c, "por vencer",
                    "Membresía por vencer en ${c.expirationDays} días",
                    Color(0xFFFF9F0A)
                )
        }
    }
    return notifs
}

/**
 * Dock flotante de notificaciones: panel glass translúcido (85%) anclado bajo la campana.
 * Tocar fuera lo cierra; no es una vista aparte.
 */
@Composable
fun NotificationsModal(
    companies: List<CompanyEntity>,
    onDismiss: () -> Unit,
    onCompanyClick: (CompanyEntity) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val notificaciones = construirNotificaciones(companies)
    val glassColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f)
                     else Color.White.copy(alpha = 0.85f)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(onClick = onDismiss, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
                .statusBarsPadding(),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 108.dp, end = 14.dp)
                    .width(310.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassColor)
                    .border(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) { }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notificaciones",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${notificaciones.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (notificaciones.isEmpty()) {
                    Text(
                        text = "No hay notificaciones. Todo en orden.",
                        fontSize = 12.sp,
                        color = subColor,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notificaciones.size) { i ->
                            val n = notificaciones[i]
                            val icono = if (n.tipo == "suspendida") Icons.Default.Warning else Icons.Default.Schedule
                            val accion = if (n.tipo == "suspendida") "Renovar" else "Ver"
                            Surface(
                                tonalElevation = 2.dp,
                                shape = RoundedCornerShape(16.dp),
                                color = (if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)).copy(alpha = 0.9f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCompanyClick(n.empresa) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(n.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icono, contentDescription = null, tint = n.color, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = n.empresa.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = titleColor,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = n.mensaje,
                                            fontSize = 11.sp,
                                            color = subColor,
                                            maxLines = 2
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(n.color.copy(alpha = 0.12f))
                                            .clickable { onCompanyClick(n.empresa) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(accion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = n.color)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
