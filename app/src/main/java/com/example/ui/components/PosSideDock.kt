package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.theme.LocalIsDarkMode

// Dock lateral del POS (diseño Canva): strip colapsado de solo iconos + panel
// expandido que se abre SOBRE el contenido al posicionarse en una opción
// (press en táctil, hover en mouse) con transición. Pill selector #1400FF,
// iconos PNG blanco/negro según tema y selección, tarjeta de anuncio y buscador.
enum class PosDockKey(val label: String) {
    INICIO("Inicio"),
    VENTAS("Ventas"),
    PANEL("Panel"),
    DEUDORES("Deudores"),
    INVENTARIO("Inventario"),
    USUARIOS("Usuarios"),
    ASISTENTE("Asistente")
}

// Ítem de búsqueda global del negocio. La pantalla construye la lista ya
// filtrada por permisos del rol; el dock solo filtra por texto.
data class DockSearchItem(
    val section: String,
    val title: String,
    val subtitle: String,
    val key: String
)

private fun dockIconRes(key: PosDockKey, selected: Boolean, isDark: Boolean): Int {
    val light = selected || isDark
    return when (key) {
        PosDockKey.INICIO -> if (light) R.drawable.dock_inicio_light else R.drawable.dock_inicio_dark
        PosDockKey.VENTAS -> if (light) R.drawable.dock_venta_light else R.drawable.dock_venta_dark
        PosDockKey.PANEL -> if (light) R.drawable.dock_panel_light else R.drawable.dock_panel_dark
        PosDockKey.DEUDORES -> if (light) R.drawable.dock_deudores_light else R.drawable.dock_deudores_dark
        PosDockKey.INVENTARIO -> if (light) R.drawable.dock_inventario_light else R.drawable.dock_inventario_dark
        PosDockKey.USUARIOS -> if (light) R.drawable.dock_usuarios_light else R.drawable.dock_usuarios_dark
        PosDockKey.ASISTENTE -> if (light) R.drawable.dock_asistente_light else R.drawable.dock_asistente_dark
    }
}

private fun dockGlassBg(isDark: Boolean): Brush {
    val base = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    return Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.675f), base.copy(alpha = 0.39f)),
        start = Offset(0f, 0f),
        end = Offset(300f, 300f)
    )
}

private fun dockBorderBrush(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.67f), Color(0xFFA6A6A6).copy(alpha = 0.67f)),
    start = Offset(0f, 0f),
    end = Offset(300f, 300f)
)

private fun dockPillBg(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF1400FF).copy(alpha = 0.375f), Color(0xFF1400FF).copy(alpha = 0.66f)),
    start = Offset(0f, 0f),
    end = Offset(300f, 120f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosSideDock(
    items: List<PosDockKey>,
    selected: PosDockKey,
    onSelect: (PosDockKey) -> Unit,
    searchItems: List<DockSearchItem>,
    onSearchSelect: (DockSearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    var peek by remember { mutableStateOf(false) }
    var pinned by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    // Press en cualquier opción abre el panel; soltar lo cierra (peek).
    val pressSrc = remember { MutableInteractionSource() }
    LaunchedEffect(pressSrc) {
        pressSrc.interactions.collect {
            when (it) {
                is PressInteraction.Press -> peek = true
                is PressInteraction.Release, is PressInteraction.Cancel -> peek = false
            }
        }
    }
    // Hover (mouse/stylus) mantiene el panel abierto.
    val hoverSrc = remember { MutableInteractionSource() }
    val hovered by hoverSrc.collectIsHoveredAsState()
    val overlayVisible = peek || hovered || pinned

    val labelColor = if (isDark) Color.White.copy(alpha = 0.87f) else Color.Black
    val searchIconRes = if (isDark) R.drawable.dock_busqueda_light else R.drawable.dock_busqueda_dark
    fun pick(key: PosDockKey) {
        peek = false
        onSelect(key)
    }

    // zIndex: el panel expandido dibuja SOBRE el contenido de la vista.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 10.dp, bottom = 12.dp, end = 4.dp)
            .zIndex(1f)
    ) {
        // Strip colapsado: solo iconos.
        Column(
            modifier = Modifier
                .width(78.dp)
                .fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
                .clip(RoundedCornerShape(36.dp))
                .background(dockGlassBg(isDark))
                .border(1.dp, dockBorderBrush(), RoundedCornerShape(36.dp))
                .hoverable(hoverSrc)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pin: fija el panel expandido.
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f))
                    .clickable { pinned = !pinned },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (pinned) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                    contentDescription = if (pinned) "Fijar abierto" else "Solo iconos",
                    tint = labelColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items.forEach { key ->
                    val isSelected = key == selected
                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .size(54.dp)
                            .clip(CircleShape)
                            .then(if (isSelected) Modifier.background(dockPillBg()).border(1.dp, dockBorderBrush(), CircleShape) else Modifier)
                            .clickable(interactionSource = pressSrc, indication = null) { pick(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = dockIconRes(key, isSelected, isDark)),
                            contentDescription = key.label,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Anuncio vertical (placeholder "anuncio prox.").
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .weight(1f, fill = false)
                        .height(190.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF8B8BFA), Color(0xFF6E6EEF)))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf("an", "un", "cio", "pro", "x.").forEach { part ->
                            Text(text = part, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            IconButton(
                onClick = { showSearch = true },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(dockGlassBg(isDark))
                    .border(1.dp, dockBorderBrush(), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = searchIconRes),
                    contentDescription = "Buscar",
                    modifier = Modifier.size(24.dp),
                    alpha = 0.53f
                )
            }
        }

        // Panel expandido SOBRE el contenido, con transición.
        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { -it / 3 },
            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 3 },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            DockExpandedPanel(
                items = items,
                selected = selected,
                onPick = ::pick,
                onOpenSearch = { showSearch = true }
            )
        }
    }

    if (showSearch) {
        PosBusinessSearchSheet(
            searchItems = searchItems,
            onSelect = {
                showSearch = false
                onSearchSelect(it)
            },
            onDismiss = { showSearch = false }
        )
    }
}

@Composable
private fun DockExpandedPanel(
    items: List<PosDockKey>,
    selected: PosDockKey,
    onPick: (PosDockKey) -> Unit,
    onOpenSearch: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val labelColor = if (isDark) Color.White.copy(alpha = 0.87f) else Color.Black
    val searchIconRes = if (isDark) R.drawable.dock_busqueda_light else R.drawable.dock_busqueda_dark

    Column(
        modifier = Modifier
            .width(208.dp)
            .fillMaxHeight()
            .shadow(10.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black.copy(alpha = 0.16f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(RoundedCornerShape(36.dp))
            .background(dockGlassBg(isDark))
            .border(1.dp, dockBorderBrush(), RoundedCornerShape(36.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Espacio del toggle del strip para alinear las filas con los iconos.
            Spacer(modifier = Modifier.height(34.dp))
            items.forEach { key ->
                val isSelected = key == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .then(if (isSelected) Modifier.background(dockPillBg()).border(1.dp, dockBorderBrush(), RoundedCornerShape(36.dp)) else Modifier)
                        .clickable { onPick(key) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = dockIconRes(key, isSelected, isDark)),
                        contentDescription = key.label,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = key.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta de anuncio (placeholder "anuncio prox.").
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF8B8BFA), Color(0xFF6E6EEF)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "anuncio prox.",
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(dockGlassBg(isDark))
                .border(1.dp, dockBorderBrush(), RoundedCornerShape(36.dp))
                .clickable { onOpenSearch() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = searchIconRes),
                contentDescription = "Buscar",
                modifier = Modifier.size(22.dp),
                alpha = 0.53f
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Busqueda",
                fontSize = 13.sp,
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.47f),
                maxLines = 1
            )
        }
    }
}

private val searchSectionOrder = listOf("Productos", "Ventas", "Deudores", "Gastos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosBusinessSearchSheet(
    searchItems: List<DockSearchItem>,
    onSelect: (DockSearchItem) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    var query by remember { mutableStateOf("") }
    val results = remember(query, searchItems) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else searchItems.filter {
            it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }.take(60)
    }
    val grouped = remember(results) {
        results.groupBy { it.section }.toSortedMap(compareBy { searchSectionOrder.indexOf(it).takeIf { i -> i >= 0 } ?: 99 })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Búsqueda del negocio", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Productos, ventas, deudores y gastos según tu acceso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Busca cliente, producto, gasto...", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(36.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1400FF),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (query.isBlank()) {
                    item {
                        Text(
                            text = "Escribe para buscar en todo el sistema del negocio.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else if (grouped.isEmpty()) {
                    item {
                        Text(
                            text = "Sin resultados para '$query' (o sin acceso a esa información).",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    grouped.forEach { (section, itemsInSection) ->
                        item(key = "header_$section") {
                            Text(
                                text = "${section.uppercase()} (${itemsInSection.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1400FF).takeIf { !isDark } ?: Color(0xFF8B8BFA),
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(itemsInSection, key = { it.key }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelect(item) }
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
