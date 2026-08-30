package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSButton
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSListRow
import com.example.ui.components.iOSPill
import com.example.ui.components.iOSSectionHeader

@Composable
fun CompanyDetailScreen(
    viewModel: KaptaViewModel,
    company: CompanyEntity,
    onBack: () -> Unit,
    onNavigateToEdit: (CompanyEntity) -> Unit,
    onEnterAsAdmin: (CompanyEntity) -> Unit
) {
    val usersFlow: List<CompanyUserEntity> by viewModel.getUsersByCompanyCode(company.code, company.id).collectAsState(initial = emptyList())
    var notesText by remember { mutableStateOf(company.notes) }
    var passwordsVisible by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }

    // Estadísticas reales del negocio (desde el servidor)
    var ventasHoy by remember { mutableStateOf<Double?>(null) }
    var ventasMes by remember { mutableStateOf<Double?>(null) }
    var productosCount by remember { mutableStateOf<Int?>(null) }
    var clientesCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(company.code, company.id) {
        viewModel.ensureDefaultUsersForCompany(company.code, company.id)
        viewModel.cargarEstadisticasEmpresa(company.code) { hoy, mes, prods, clientes ->
            ventasHoy = hoy
            ventasMes = mes
            productosCount = prods
            clientesCount = clientes
        }
    }

    fun formatCurrency(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    EtherealBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                iOSLargeTitle(
                    title = company.name,
                    subtitle = "Detalles e información del negocio",
                    modifier = Modifier.weight(1f)
                )
            }

            // Main Company Banner Card
            iOSSectionHeader("Negocio")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = company.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Código: ${company.code}.kaptaia.com",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val effectiveStatus = company.getEffectiveStatus()
                        val statusColor = when {
                            effectiveStatus.contains("Acti", ignoreCase = true) -> Color(0xFF34C759)
                            effectiveStatus.contains("Venc", ignoreCase = true) -> Color(0xFFFF9F0A)
                            effectiveStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFFF453A)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        iOSPill(text = effectiveStatus, color = statusColor)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action row (Edit, Enter as Admin, Contact, Copy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iOSButton(
                            text = "Acceder al Negocio",
                            onClick = { onEnterAsAdmin(company) },
                            modifier = Modifier.weight(1.5f)
                        )
                        iOSButton(
                            text = "Editar",
                            onClick = { onNavigateToEdit(company) },
                            modifier = Modifier.weight(1f),
                            tinted = true
                        )
                        iOSButton(
                            text = "Copiar",
                            onClick = { viewModel.copyCredentials(company) },
                            modifier = Modifier.weight(0.9f),
                            tinted = true
                        )
                    }
                }
            }

            // Módulos y dock asignados realmente al negocio
            iOSSectionHeader("Módulos y dock (${company.plan})")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val modulos = remember(company.selectedModulesJson) {
                    try {
                        val arr = org.json.JSONArray(company.selectedModulesJson)
                        List(arr.length()) { arr.getString(it) }
                    } catch (_: Exception) { emptyList<String>() }
                }
                val funciones = remember(company.customFunctionsJson) {
                    try {
                        val arr = org.json.JSONArray(company.customFunctionsJson)
                        List(arr.length()) { arr.getJSONObject(it) }
                    } catch (_: Exception) { emptyList<org.json.JSONObject>() }
                }
                if (modulos.isEmpty() && funciones.isEmpty()) {
                    Text(
                        text = "Sin módulos ni funciones asignados a este negocio.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                modulos.forEachIndexed { index, m ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    iOSListRow(
                        title = "🧩 $m",
                        subtitle = "Módulo habilitado",
                        leadingIcon = Icons.Default.CheckCircle,
                        leadingTint = MaterialTheme.colorScheme.tertiary
                    )
                }
                funciones.forEachIndexed { index, f ->
                    if (modulos.isNotEmpty() || index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val nombre = f.optString("nombre", "Función")
                    val desc = f.optString("descripcion", "")
                    iOSListRow(
                        title = "🤖 $nombre",
                        subtitle = desc.ifBlank { "Función IA del negocio" },
                        leadingIcon = Icons.Default.AutoAwesome,
                        leadingTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Business Info Card
            iOSSectionHeader("Información del negocio")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val details = listOf(
                    "Plan contratado" to company.plan,
                    "Tipo de negocio" to company.businessType,
                    "NIT" to company.nit.ifBlank { "N/A" },
                    "País" to company.country.ifBlank { "Colombia" },
                    "Ciudad / Ubicación" to company.city,
                    "Dirección" to company.address,
                    "Celular 1 (Llamadas / WhatsApp)" to company.phone,
                    "Celular 2" to company.phone2.ifBlank { "N/A" },
                    "Correo empresa" to company.email,
                    "Administrador responsable" to company.adminName,
                    "Correo del Admin" to company.adminEmail,
                    "Fecha de registro" to company.creationDate,
                    "Último acceso al sistema" to company.lastAccess,
                    "Días para vencimiento" to "${company.expirationDays} días"
                )
                details.forEachIndexed { index, (label, value) ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(label, value)
                }
            }

            // Membership purchase / renewal
            iOSSectionHeader("Membresía y plan")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                var selectedPlan by remember { mutableStateOf(company.plan.ifBlank { "Básico" }) }
                var selectedTiempo by remember { mutableStateOf(if (company.durationTime.contains("Anual", true)) "Anual" else "Mensual") }
                var renovando by remember { mutableStateOf(false) }
                var resultado by remember { mutableStateOf<String?>(null) }

                val precio = when {
                    selectedTiempo == "Prueba 15 días" -> "$0 (prueba)"
                    selectedTiempo == "Anual" -> when (selectedPlan) {
                        "MAX IA" -> "$3.999.000"
                        "Premium" -> "$2.499.000"
                        else -> "$1.499.000"
                    }
                    else -> when (selectedPlan) {
                        "MAX IA" -> "$339.900"
                        "Premium" -> "$249.900"
                        else -> "$149.900"
                    }
                }

                Text(
                    text = "Plan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Básico", "Premium", "MAX IA").forEach { p ->
                        PlanSelectorChip(
                            label = p,
                            selected = selectedPlan == p,
                            onTap = { selectedPlan = p },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Duración",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mensual", "Anual", "Prueba 15 días").forEach { t ->
                        PlanSelectorChip(
                            label = t,
                            selected = selectedTiempo == t,
                            onTap = { selectedTiempo = t },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                iOSButton(
                    text = if (renovando) "Registrando..." else "Registrar compra y activar",
                    onClick = {
                        if (!renovando) {
                            renovando = true
                            viewModel.comprarPlan(
                                company = company,
                                plan = selectedPlan,
                                tiempo = selectedTiempo,
                                monto = precio.filter { it.isDigit() }.ifBlank { "0" }
                            ) { ok ->
                                renovando = false
                                resultado = if (ok) "Membresía actualizada y sincronizada" else "Error de conexión con el servidor"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tinted = false
                )
                resultado?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (it.startsWith("Error")) Color(0xFFDC2626) else Color(0xFF34C759)
                    )
                }
                }
            }

            // Users Card
            iOSSectionHeader("Usuarios de la empresa (${usersFlow.size})")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                usersFlow.forEachIndexed { index, user ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val isPassVisible = passwordsVisible[user.id] ?: false
                    iOSListRow(
                        title = user.name,
                        subtitle = "Usuario: ${user.username}",
                        trailing = {
                            iOSPill(text = user.role, color = MaterialTheme.colorScheme.primary)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    iOSListRow(
                        title = "Clave",
                        subtitle = if (isPassVisible) user.password else "••••••••",
                        trailing = {
                            IconButton(onClick = {
                                val current = passwordsVisible.toMutableMap()
                                current[user.id] = !isPassVisible
                                passwordsVisible = current
                            }) {
                                Icon(
                                    imageVector = if (isPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Pass",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Statistics Summary Card (datos reales del servidor)
            iOSSectionHeader("Estadísticas del negocio")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatBox("Ventas Hoy", ventasHoy?.let { formatCurrency(it) } ?: "...", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        MiniStatBox("Ventas Mes", ventasMes?.let { formatCurrency(it) } ?: "...", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatBox("Productos", productosCount?.let { "$it items" } ?: "...", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MiniStatBox("Clientes", clientesCount?.let { "$it reg." } ?: "...", Color(0xFFFF9F0A), Modifier.weight(1f))
                    }
                }
            }

            // Notes Editable Area
            iOSSectionHeader("Notas administrativas")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Escribe observaciones sobre esta empresa...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun MiniStatBox(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
private fun PlanSelectorChip(label: String, selected: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
