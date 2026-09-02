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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CompanyEntity

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

private val gradTurquesaMorado = Brush.linearGradient(
    listOf(Color(0xFF5CE1E6), Color(0xFF8C52FF))
)
private val gradBlancoGris = Brush.linearGradient(
    listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6))
)

private data class ItemNotif(
    val tipo: String,          // "negocios" | "soporte"
    val titulo: String,        // nombre negocio (soporte) o tipo de alerta (negocios)
    val descripcion: String,   // descripción (soporte) o detalle (negocios)
    val numero: String = "",
    val gradiente: Brush,      // degradado del círculo
    val empresa: CompanyEntity? = null
)

private fun brushAvisoNegocio(tipo: String): Brush = when (tipo) {
    "suspendida" -> Brush.linearGradient(listOf(Color.White, Color(0xFFFF0000)))
    "pagos" -> gradTurquesaMorado
    else -> Brush.linearGradient(listOf(Color(0xFFFFE3C7), Color(0xFFFF8A00)))
}

private fun brushPlan(plan: String?): Brush = when {
    plan.isNullOrBlank() || plan.equals("Free", true) -> Brush.linearGradient(listOf(Color.White, Color(0xFFA6A6A6)))
    plan.equals("Esencial", true) -> Brush.linearGradient(listOf(Color(0xFFFFE3C7), Color(0xFFFF8A00)))
    else -> gradTurquesaMorado
}

/**
 * Panel flotante de notificaciones del SuperAdmin a pantalla completa con blur claro.
 * Mantiene los tabs Negocios / Soporte / Todo, items con degradado y filtrado en vivo.
 */
@Composable
fun NotificationsModal(
    companies: List<CompanyEntity>,
    soportes: List<Map<String, Any>> = emptyList(),
    onDismiss: () -> Unit,
    onCompanyClick: (CompanyEntity) -> Unit
) {
    var filtro by remember { mutableStateOf("todo") }

    val notifsNegocio = construirNotificaciones(companies)
    val itemsNegocio: List<ItemNotif> = remember(notifsNegocio) {
        notifsNegocio.map { n ->
            ItemNotif(
                tipo = "negocios",
                titulo = when (n.tipo) {
                    "suspendida" -> "Negocio suspendido"
                    else -> "Membresía por vencer"
                },
                descripcion = n.mensaje,
                numero = "1",
                gradiente = brushAvisoNegocio(n.tipo),
                empresa = n.empresa
            )
        }
    }

    val itemsSoporte: List<ItemNotif> = remember(soportes) {
        soportes.map { s ->
            val nombre = s["empresa_nombre"]?.toString()?.takeIf { it.isNotBlank() }
                ?: s["solicitante"]?.toString() ?: "Negocio"
            val plan = s["plan"]?.toString()
            ItemNotif(
                tipo = "soporte",
                titulo = nombre,
                descripcion = s["observaciones"]?.toString() ?: s["tipo_solicitud"]?.toString() ?: "",
                numero = "",
                gradiente = brushPlan(plan),
                empresa = companies.firstOrNull { it.name.equals(nombre, ignoreCase = true) }
            )
        }
    }

    val visibles = remember(filtro, itemsNegocio, itemsSoporte) {
        buildList {
            if (filtro == "todo" || filtro == "negocios") addAll(itemsNegocio)
            if (filtro == "todo" || filtro == "soporte") addAll(itemsSoporte)
        }
    }

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
                // Blur claro sin oscurecer: fondo translúcido muy liviano
                .background(Color(0x1AFFFFFF))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp)
                    .clickable(enabled = false) { }
            ) {
                // ---- Panel principal ----
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF8F8F8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .border(0.6.dp, gradBlancoGris, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Color(0xFF8C52FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notificaciones",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // ---- Tabs ----
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("negocios", "Negocios", itemsNegocio.size),
                                Triple("soporte", "Soporte", itemsSoporte.size),
                                Triple("todo", "Todo", itemsNegocio.size + itemsSoporte.size)
                            ).forEach { (id, label, count) ->
                                val activo = filtro == id
                                TabNotif(
                                    label = "$label  $count",
                                    activo = activo,
                                    onClick = { filtro = id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ---- Lista de notificaciones ----
                if (visibles.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFF8F8F8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .border(0.6.dp, gradBlancoGris, RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = "No hay notificaciones en esta categoría.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibles, key = { "${it.tipo}-${it.titulo}-${it.descripcion}" }) { item ->
                            NotifRow(item, onCompanyClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabNotif(
    label: String,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grad = if (activo) {
        listOf(Color(0xFF5CE1E6), Color(0xFF8C52FF))
    } else {
        listOf(Color(0xFF64748B), Color(0xFF64748B))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F8F8),
        border = null,
        modifier = modifier.border(0.6.dp, gradBlancoGris, RoundedCornerShape(14.dp))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)
        ) {
            if (activo) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(brush = gradTurquesaMorado)) { append(label) }
                    },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun NotifRow(item: ItemNotif, onCompanyClick: (CompanyEntity) -> Unit) {
    val grad = if (item.tipo == "soporte") item.gradiente else item.gradiente
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8F8F8),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.6.dp, gradBlancoGris, RoundedCornerShape(16.dp))
            .clickable(enabled = item.empresa != null) {
                item.empresa?.let(onCompanyClick)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo con degradado
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(grad),
                contentAlignment = Alignment.Center
            ) {
                if (item.tipo == "soporte") {
                    Text(
                        text = item.titulo.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.titulo.isNotBlank()) Color.White else Color.Transparent
                    )
                } else {
                    Text(
                        text = item.titulo.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                when (item.tipo) {
                    "negocios" -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.numero.ifBlank { "1" },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                lineHeight = 26.sp
                            )
                            Text(
                                text = item.titulo,
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = item.titulo,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.descripcion,
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
