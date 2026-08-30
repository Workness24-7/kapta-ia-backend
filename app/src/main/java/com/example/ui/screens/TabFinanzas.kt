package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KaptaViewModel
import com.example.ui.components.DonutChart
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSListRow
import com.example.ui.components.iOSPill
import com.example.ui.components.iOSSectionHeader
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.iOSSeparatorDark
import com.example.ui.theme.iOSSeparatorLight
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TabFinanzas(viewModel: KaptaViewModel) {
    val activeSubTab by viewModel.financeSubTab.collectAsState()
    val startDate by viewModel.financeStartDate.collectAsState()
    val endDate by viewModel.financeEndDate.collectAsState()

    EtherealBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                iOSLargeTitle(
                    title = "Módulo Financiero",
                    subtitle = "Resumen de ingresos, egresos, cartera y suscripciones"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Resumen", "Ingresos", "Egresos", "Cartera", "Suscripciones", "Retiro", "Reportes").forEach { tab ->
                        val isSelected = activeSubTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.setFinanceSubTab(tab) }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (activeSubTab in listOf("Resumen", "Ingresos", "Egresos", "Retiro", "Reportes")) {
                    DateRangeFilterBlock(
                        startDate = startDate,
                        endDate = endDate,
                        onStartDateChange = { viewModel.setFinanceStartDate(it) },
                        onEndDateChange = { viewModel.setFinanceEndDate(it) },
                        onClearFilter = { viewModel.clearFinanceDateFilter() }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                when (activeSubTab) {
                    "Resumen" -> FinanzasResumenSection(viewModel)
                    "Ingresos" -> FinanzasIngresosSection(viewModel)
                    "Egresos" -> FinanzasEgresosSection(viewModel)
                    "Cartera" -> FinanzasCarteraSection(viewModel)
                    "Suscripciones" -> FinanzasSuscripcionesSection(viewModel)
                    "Retiro" -> FinanzasRetiroSection(viewModel)
                    "Reportes" -> FinanzasReportesSection(viewModel)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DateRangeFilterBlock(
    startDate: String,
    endDate: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onClearFilter: () -> Unit
) {
    val context = LocalContext.current

    fun openDatePicker(currentVal: String, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        if (currentVal.isNotBlank()) {
            val parts = currentVal.split("/", "-")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                val m = (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                val y = parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR)
                cal.set(y, m, d)
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                onSelected(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtro por Rango de Fechas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (startDate.isNotBlank() || endDate.isNotBlank()) {
                    TextButton(onClick = onClearFilter) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Limpiar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF453A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartDateChange,
                    label = { Text("Desde (dd/mm/aaaa)", fontSize = 11.sp) },
                    placeholder = { Text("01/07/2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker(startDate, onStartDateChange) }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Seleccionar fecha inicio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = onEndDateChange,
                    label = { Text("Hasta (dd/mm/aaaa)", fontSize = 11.sp) },
                    placeholder = { Text("31/07/2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker(endDate, onEndDateChange) }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Seleccionar fecha fin",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FinanzasResumenSection(viewModel: KaptaViewModel) {
    val companies by viewModel.companies.collectAsState()
    val finanzasKapta by viewModel.finanzasKapta.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshFinanzasKapta() }

    fun getPlanPrice(plan: String): Double {
        return when {
            plan.contains("MAX", ignoreCase = true) -> 399900.0
            plan.contains("Premium", ignoreCase = true) -> 249900.0
            else -> 149900.0
        }
    }

    // ponytail: regla de negocio del usuario — por vencer siempre suma;
    // suspendidos/vencidos solo si vencieron hace <= 7 días
    val carteraCompanies = companies.filter {
        val estado = it.getEffectiveStatus()
        when {
            estado.contains("Vencer", ignoreCase = true) -> true
            estado.contains("Suspend", ignoreCase = true) || estado.contains("Vencid", ignoreCase = true) ->
                it.expirationDays >= -7
            else -> it.expirationDays in 0..5
        }
    }
    val carteraTotal = carteraCompanies.sumOf { getPlanPrice(it.plan) }

    fun formatCurr(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "KAPTA IA Ingresos por Planes y Gastos Propios", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Datos reales desde el servidor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniFinanceCard("Ingresos (Planes)", formatCurr(finanzasKapta?.totalIngresos ?: 0.0), Color(0xFF34C759), Modifier.weight(1f))
                MiniFinanceCard("Gastos KAPTA", formatCurr(finanzasKapta?.totalEgresos ?: 0.0), Color(0xFFFF453A), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            val balanceKapta = finanzasKapta?.balance ?: 0.0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniFinanceCard("Balance Real", formatCurr(balanceKapta), if (balanceKapta >= 0) Color(0xFF34C759) else Color(0xFFFF453A), Modifier.weight(1f))
                MiniFinanceCard("Registros", "${finanzasKapta?.registros?.size ?: 0}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Balance General", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))

            val ingresosKapta = finanzasKapta?.totalIngresos ?: 0.0
            val egresosKapta = finanzasKapta?.totalEgresos ?: 0.0
            val balanceReal = finanzasKapta?.balance ?: 0.0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniFinanceCard("Ingresos Totales", formatCurr(ingresosKapta), Color(0xFF34C759), Modifier.weight(1f))
                MiniFinanceCard("Egresos Totales", formatCurr(egresosKapta), Color(0xFFFF453A), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniFinanceCard("Utilidad Neta", formatCurr(balanceReal), if (balanceReal >= 0) MaterialTheme.colorScheme.primary else Color(0xFFFF453A), Modifier.weight(1f))
                MiniFinanceCard("Cartera Pendiente", formatCurr(carteraTotal), Color(0xFFFF9F0A), Modifier.weight(1f))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Estado de Empresas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Toca un segmento para ver el detalle", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            val activos = companies.count {
                val s = it.getEffectiveStatus()
                s.contains("Activo", ignoreCase = true) && !s.contains("Vencer", ignoreCase = true)
            }
            val porVencer = companies.count { it.getEffectiveStatus().contains("Vencer", ignoreCase = true) }
            val suspendidos = companies.count {
                val s = it.getEffectiveStatus()
                s.contains("Suspend", ignoreCase = true) || s.contains("Vencid", ignoreCase = true)
            }
            val estados = listOf(
                "Activos" to activos,
                "Por vencer" to porVencer,
                "Suspendidos" to suspendidos
            ).filter { it.second > 0 }
            val coloresEstados = listOf(Color(0xFF34C759), Color(0xFFFF9F0A), Color(0xFFFF453A))

            var segmentoSeleccionado by remember { mutableStateOf(-1) }
            DonutChart(
                percentages = estados.map { it.second.toFloat() }.ifEmpty { listOf(1f) },
                colors = coloresEstados,
                centerText = if (segmentoSeleccionado >= 0 && segmentoSeleccionado < estados.size)
                    estados[segmentoSeleccionado].second.toString() else companies.size.toString(),
                centerSubtext = if (segmentoSeleccionado >= 0 && segmentoSeleccionado < estados.size)
                    estados[segmentoSeleccionado].first else "Total Empresas",
                onSegmentClick = { idx ->
                    segmentoSeleccionado = if (segmentoSeleccionado == idx) -1 else idx
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            estados.forEachIndexed { index, (nombre, cantidad) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            segmentoSeleccionado = if (segmentoSeleccionado == index) -1 else index
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(coloresEstados[index])
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nombre,
                            fontSize = 13.sp,
                            fontWeight = if (segmentoSeleccionado == index) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "$cantidad ${if (cantidad == 1) "empresa" else "empresas"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = coloresEstados[index]
                    )
                }
            }
        }
    }
}

@Composable
fun FinanzasIngresosSection(viewModel: KaptaViewModel) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val startDate by viewModel.financeStartDate.collectAsState()
    val endDate by viewModel.financeEndDate.collectAsState()
    val finanzasKapta by viewModel.finanzasKapta.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshFinanzasKapta() }

    val incomeItems = filteredTransactions.filter { !it.isExpense }
    val totalIncome = incomeItems.sumOf { it.amount }

    fun formatCurr(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    val dateInfo = if (startDate.isNotBlank() || endDate.isNotBlank()) {
        "Rango: ${startDate.ifBlank { "Inicio" }} - ${endDate.ifBlank { "Hoy" }}"
    } else {
        "Todas las fechas"
    }

    FinanceMetricCard(
        title = "Total Ingresos",
        amount = formatCurr(finanzasKapta?.totalIngresos ?: 0.0),
        caption = "${incomeItems.size} registros • $dateInfo",
        icon = Icons.Default.TrendingUp,
        accent = Color(0xFF34C759)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Ingresos reales por compra de planes (backend)
    val ingresosPlanes = finanzasKapta?.registros?.filter { it.tipo.equals("Ingreso", true) } ?: emptyList()
    iOSSectionHeader("Ingresos por Planes (${ingresosPlanes.size})")
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (ingresosPlanes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no hay planes comprados. Cuando una empresa compre o renueve un plan, el ingreso aparecerá aquí.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            ingresosPlanes.forEachIndexed { index, reg ->
                IncomeItem(
                    title = reg.concepto,
                    subtitle = fechaCortaEs(reg.fecha),
                    amount = "+${formatCurr(reg.monto)}",
                    time = reg.usuario.ifBlank { "KAPTA" },
                    isExpense = false
                )
                if (index != ingresosPlanes.lastIndex) {
                    iOSDivider()
                }
            }
        }
    }
}

@Composable
fun FinanzasEgresosSection(viewModel: KaptaViewModel) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val startDate by viewModel.financeStartDate.collectAsState()
    val endDate by viewModel.financeEndDate.collectAsState()
    val finanzasKapta by viewModel.finanzasKapta.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshFinanzasKapta() }

    var gastoConcepto by remember { mutableStateOf("") }
    var gastoMonto by remember { mutableStateOf("") }
    var gastoCategoria by remember { mutableStateOf("") }
    var gastoMetodo by remember { mutableStateOf("Transferencia") }
    var enviandoGasto by remember { mutableStateOf(false) }
    var gastoResultado by remember { mutableStateOf<String?>(null) }

    val expenseItems = filteredTransactions.filter { it.isExpense }
    val totalExpenses = expenseItems.sumOf { it.amount }

    fun formatCurr(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    val dateInfo = if (startDate.isNotBlank() || endDate.isNotBlank()) {
        "Rango: ${startDate.ifBlank { "Inicio" }} - ${endDate.ifBlank { "Hoy" }}"
    } else {
        "Todas las fechas"
    }

    FinanceMetricCard(
        title = "Total Egresos",
        amount = formatCurr(finanzasKapta?.totalEgresos ?: 0.0),
        caption = "${expenseItems.size} registros • $dateInfo",
        icon = Icons.Default.TrendingDown,
        accent = Color(0xFFFF453A)
    )

    Spacer(modifier = Modifier.height(14.dp))

    var mostrarFormularioGasto by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (mostrarFormularioGasto) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.12f))
                .border(
                    1.dp,
                    if (mostrarFormularioGasto) MaterialTheme.colorScheme.primary else Color(0xFFEF4444),
                    RoundedCornerShape(12.dp)
                )
                .clickable { mostrarFormularioGasto = !mostrarFormularioGasto }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (mostrarFormularioGasto) Icons.Default.ExpandLess else Icons.Default.Add,
                contentDescription = null,
                tint = if (mostrarFormularioGasto) MaterialTheme.colorScheme.primary else Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (mostrarFormularioGasto) "Ocultar formulario" else "Gasto",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (mostrarFormularioGasto) MaterialTheme.colorScheme.primary else Color(0xFFEF4444)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Registro de gasto propio de KAPTA IA (se guarda en el servidor)
    if (mostrarFormularioGasto) {
        iOSSectionHeader("Registrar Gasto de KAPTA IA")
        GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = gastoConcepto,
                onValueChange = { gastoConcepto = it },
                label = { Text("Concepto (ej. Publicidad, Servidores)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gastoMonto,
                    onValueChange = { gastoMonto = it.filter { c -> c.isDigit() } },
                    label = { Text("Monto") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = gastoCategoria,
                    onValueChange = { gastoCategoria = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Transferencia", "Efectivo", "Tarjeta").forEach { m ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (gastoMetodo == m) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (gastoMetodo == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { gastoMetodo = m }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = m,
                            fontSize = 12.sp,
                            fontWeight = if (gastoMetodo == m) FontWeight.Bold else FontWeight.Normal,
                            color = if (gastoMetodo == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (gastoConcepto.isNotBlank() && gastoMonto.isNotBlank() && !enviandoGasto) {
                        enviandoGasto = true
                        viewModel.registrarGastoKapta(
                            concepto = gastoConcepto.trim(),
                            monto = gastoMonto,
                            categoria = gastoCategoria.trim(),
                            metodoPago = gastoMetodo
                        ) { ok ->
                            enviandoGasto = false
                            if (ok) {
                                gastoConcepto = ""
                                gastoMonto = ""
                                gastoCategoria = ""
                                gastoResultado = "Gasto registrado en el servidor"
                            } else {
                                gastoResultado = "Error de conexión con el servidor"
                            }
                        }
                    }
                },
                enabled = gastoConcepto.isNotBlank() && gastoMonto.isNotBlank() && !enviandoGasto,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text(if (enviandoGasto) "Guardando..." else "Registrar Gasto", color = Color.White, fontWeight = FontWeight.Bold)
            }
            gastoResultado?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = if (it.startsWith("Error")) Color(0xFFDC2626) else Color(0xFF34C759)
                )
            }
        }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Gastos reales de KAPTA registrados desde la app
    val gastosKapta = finanzasKapta?.registros?.filter { it.tipo.equals("Egreso", true) } ?: emptyList()
    iOSSectionHeader("Gastos de KAPTA IA (${gastosKapta.size})")
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (gastosKapta.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay gastos registrados todavía.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            gastosKapta.forEachIndexed { index, reg ->
                IncomeItem(
                    title = reg.concepto,
                    subtitle = "${fechaCortaEs(reg.fecha)} - ${reg.metodoPago.ifBlank { "N/A" }}",
                    amount = "-${formatCurr(reg.monto)}",
                    time = reg.usuario.ifBlank { "KAPTA" },
                    isExpense = true
                )
                if (index != gastosKapta.lastIndex) {
                    iOSDivider()
                }
            }
        }
    }
}

@Composable
fun FinanzasCarteraSection(viewModel: KaptaViewModel) {
    val context = LocalContext.current
    val companies by viewModel.companies.collectAsState()

    fun getPlanPrice(plan: String): Double {
        return when {
            plan.contains("MAX", ignoreCase = true) -> 399900.0
            plan.contains("Premium", ignoreCase = true) -> 249900.0
            else -> 149900.0
        }
    }

    fun formatCurr(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    // ponytail: regla de negocio del usuario — por vencer siempre suma;
    // suspendidos/vencidos solo si vencieron hace <= 7 días
    val carteraCompanies = companies.filter {
        val estado = it.getEffectiveStatus()
        when {
            estado.contains("Vencer", ignoreCase = true) -> true
            estado.contains("Suspend", ignoreCase = true) || estado.contains("Vencid", ignoreCase = true) ->
                it.expirationDays >= -7
            else -> it.expirationDays in 0..5
        }
    }

    val totalCartera = carteraCompanies.sumOf { getPlanPrice(it.plan) }

    FinanceMetricCard(
        title = "Total Cartera Pendiente",
        amount = formatCurr(totalCartera),
        caption = "${carteraCompanies.size} empresas con cobro o vencimiento próximo",
        icon = Icons.Default.Receipt,
        accent = Color(0xFFFF9F0A)
    )

    Spacer(modifier = Modifier.height(14.dp))

    iOSSectionHeader("Detalle de Cartera por Empresa (${carteraCompanies.size})")

    if (carteraCompanies.isEmpty()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "No hay empresas registradas con saldo o vencimiento pendiente en cartera.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    } else {
        carteraCompanies.forEach { company ->
            val price = getPlanPrice(company.plan)
            val isExpired = company.status.contains("Suspend", ignoreCase = true) || company.expirationDays == 0
            val badgeColor = if (isExpired) Color(0xFFFF453A) else Color(0xFFFF9F0A)

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = company.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        iOSPill(
                            text = if (isExpired) "Suspendido / Vencido" else "Vence en ${company.expirationDays} días",
                            color = badgeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Plan: ${company.plan} • NIT: ${company.nit.ifBlank { "N/A" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Contacto: ${company.adminName} (${company.phone})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = formatCurr(price),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF453A)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val telefonoLimpio = company.phone.filter { c -> c.isDigit() }
                            val telefono = if (telefonoLimpio.length == 10) "57$telefonoLimpio" else telefonoLimpio
                            val mensaje = java.net.URLEncoder.encode(
                                "Hola ${company.adminName}, le escribimos de KAPTA IA. Le recordamos que el plan ${company.plan} de su negocio \"${company.name}\" presenta un saldo pendiente de ${formatCurr(price)}. Quedamos atentos para gestionar su pago. ¡Gracias!",
                                "UTF-8"
                            )
                            try {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://wa.me/$telefono?text=$mensaje")
                                    )
                                )
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "No se encontró WhatsApp instalado",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar Recordatorio por WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FinanzasSuscripcionesSection(viewModel: KaptaViewModel) {
    val companies by viewModel.companies.collectAsState()

    // ponytail: solo suscripciones activas cuentan para MRR — se excluyen
    // vencidas/suspendidas y empresas aún en período de prueba
    val suscripcionesActivas = companies.filter {
        val estado = it.getEffectiveStatus()
        !estado.contains("Suspend", ignoreCase = true) &&
            !estado.contains("Vencid", ignoreCase = true) &&
            !estado.contains("Prueba", ignoreCase = true)
    }

    val basicoList = suscripcionesActivas.filter { it.plan.contains("Básico", ignoreCase = true) }
    val premiumList = suscripcionesActivas.filter { it.plan.contains("Premium", ignoreCase = true) }
    val maxList = suscripcionesActivas.filter { it.plan.contains("MAX", ignoreCase = true) }

    val totalSuscripciones = suscripcionesActivas.size.coerceAtLeast(1)
    val totalMRR = (basicoList.size * 149900.0) + (premiumList.size * 249900.0) + (maxList.size * 399900.0)

    fun formatCurr(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    FinanceMetricCard(
        title = "Recaudación Mensual Estimada (MRR)",
        amount = formatCurr(totalMRR),
        caption = "${suscripcionesActivas.size} de ${companies.size} empresas con suscripción activa",
        icon = Icons.Default.Stars,
        accent = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(14.dp))

    iOSSectionHeader("Desglose por Tipo de Plan (${suscripcionesActivas.size} empresas activas)")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PlanBreakdownCard(
                planName = "Plan Básico",
                priceText = "$149,900 / mes",
                count = basicoList.size,
                percentage = (basicoList.size.toFloat() / totalSuscripciones) * 100,
                monthlyTotal = basicoList.size * 149900.0,
                companyNames = basicoList.map { it.name },
                accentColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlanBreakdownCard(
                planName = "Plan Premium",
                priceText = "$249,900 / mes",
                count = premiumList.size,
                percentage = (premiumList.size.toFloat() / totalSuscripciones) * 100,
                monthlyTotal = premiumList.size * 249900.0,
                companyNames = premiumList.map { it.name },
                accentColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlanBreakdownCard(
                planName = "Plan MAX IA",
                priceText = "$399,900 / mes",
                count = maxList.size,
                percentage = (maxList.size.toFloat() / totalSuscripciones) * 100,
                monthlyTotal = maxList.size * 399900.0,
                companyNames = maxList.map { it.name },
                accentColor = Color(0xFF34C759)
            )
        }
    }
}

@Composable
fun PlanBreakdownCard(
    planName: String,
    priceText: String,
    count: Int,
    percentage: Float,
    monthlyTotal: Double,
    companyNames: List<String>,
    accentColor: Color
) {
    fun formatCurr(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = accentColor.copy(alpha = 0.08f),
        borderColor = accentColor.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = planName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($priceText)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$count ${if (count == 1) "empresa" else "empresas"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "Participación: %.1f%%", percentage),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: ${formatCurr(monthlyTotal)} /mes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (companyNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    companyNames.forEach { name ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinanzasRetiroSection(viewModel: KaptaViewModel) {
    val context = LocalContext.current
    val companies by viewModel.companies.collectAsState()
    val startDate by viewModel.financeStartDate.collectAsState()
    val endDate by viewModel.financeEndDate.collectAsState()

    val withdrawnCompanies = companies.filter {
        it.status.contains("Suspend", ignoreCase = true) ||
        it.status.contains("Retirad", ignoreCase = true) ||
        it.status.contains("Inactiv", ignoreCase = true)
    }

    fun parseDateStr(dateStr: String): java.util.Date? {
        if (dateStr.isBlank()) return null
        val formats = listOf(
            java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
        for (fmt in formats) {
            try {
                fmt.isLenient = false
                val parsed = fmt.parse(dateStr.trim())
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    val startDateObj = parseDateStr(startDate)
    val endDateObj = parseDateStr(endDate)?.let {
        val cal = Calendar.getInstance()
        cal.time = it
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.time
    }

    val filteredList = withdrawnCompanies.filter { comp ->
        val refDateObj = parseDateStr("10/07/2026")
        if (refDateObj != null) {
            val afterStart = startDateObj == null || !refDateObj.before(startDateObj)
            val beforeEnd = endDateObj == null || !refDateObj.after(endDateObj)
            afterStart && beforeEnd
        } else {
            true
        }
    }

    val dateRangeText = if (startDate.isNotBlank() || endDate.isNotBlank()) {
        "Filtro: ${startDate.ifBlank { "Inicio" }} - ${endDate.ifBlank { "Hoy" }}"
    } else {
        "Histórico total"
    }

    FinanceMetricCard(
        title = "Empresas Retiradas / Suspendidas",
        amount = "${filteredList.size} ${if (filteredList.size == 1) "Empresa" else "Empresas"}",
        caption = dateRangeText,
        icon = Icons.Default.ExitToApp,
        accent = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    iOSSectionHeader("Registro de Bajas y Suspensiones (${filteredList.size})")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay empresas suspendidas o retiradas en el rango de fechas seleccionado.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            filteredList.forEach { company ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = company.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            iOSPill(text = company.status, color = Color(0xFFFF453A))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "NIT: ${company.nit.ifBlank { "N/A" }} • Ubicación: ${company.city}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Motivo: Suspensión por falta de pago / Inactividad (15 días)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Plan anterior: ${company.plan} • Contacto: ${company.adminName} (${company.phone})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "Solicitud de reactivación iniciada para ${company.name}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reactivar Cuenta / Ofrecer Oferta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Motivos de retiro históricos:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("Mora o atraso en pago de suscripción", "60%")
            DetailRow("Cierre temporal de local / negocio", "25%")
            DetailRow("Poco uso del sistema", "15%")
        }
    }
}

@Composable
fun FinanzasReportesSection(viewModel: KaptaViewModel) {
    val startDate by viewModel.financeStartDate.collectAsState()
    val endDate by viewModel.financeEndDate.collectAsState()

    // ponytail: condición obligatoria del usuario — sin rango de fechas no se exporta
    val filtroValido = startDate.isNotBlank() || endDate.isNotBlank()

    iOSSectionHeader("Exportar Reportes Financieros")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!filtroValido) {
                Text(
                    text = "Selecciona un rango de fechas en la parte superior para habilitar la exportación.",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9F0A),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Button(
                onClick = {},
                enabled = filtroValido,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Reporte Excel (.xlsx)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {},
                enabled = filtroValido,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF453A),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Reporte PDF (.pdf)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FinanceMetricCard(
    title: String,
    amount: String,
    caption: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        borderColor = accent.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .background(accent.copy(alpha = 0.08f))
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = amount,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = caption,
                    fontSize = 11.sp,
                    color = accent
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MiniFinanceCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = amount, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun IncomeItem(title: String, subtitle: String, amount: String, time: String, isExpense: Boolean = false) {
    iOSListRow(
        title = title,
        subtitle = if (time.isBlank()) subtitle else "$subtitle • $time",
        trailing = {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isExpense) Color(0xFFFF453A) else Color(0xFF34C759)
            )
        }
    )
}

// "2026-08-21 ..." o "21/08/2026 ..." → "21 Agosto"
fun fechaCortaEs(fecha: String): String {
    val mesesEs = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return try {
        val partes = fecha.trim().split(" ").first().split("-", "/")
        when {
            partes.size == 3 && partes[0].length == 4 ->
                "${partes[2].toInt()} ${mesesEs.getOrElse(partes[1].toInt() - 1) { "" }}"
            partes.size == 3 ->
                "${partes[0].toInt()} ${mesesEs.getOrElse(partes[1].toInt() - 1) { "" }}"
            else -> fecha
        }
    } catch (_: Exception) {
        fecha
    }
}

@Composable
private fun iOSDivider(modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkMode.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(if (isDark) iOSSeparatorDark else iOSSeparatorLight)
    )
}
