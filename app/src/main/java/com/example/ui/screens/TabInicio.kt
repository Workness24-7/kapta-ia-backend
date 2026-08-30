package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CompanyEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSPill
import com.example.ui.components.iOSSectionHeader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import coil.compose.AsyncImage
import com.example.R

@Composable
fun TabInicio(
    viewModel: KaptaViewModel,
    onNavigateToCreateCompany: () -> Unit,
    onNavigateToAdministrators: () -> Unit,
    onNavigateToMemberships: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToEmpresasTab: () -> Unit,
    onNavigateToFinanzasTab: () -> Unit = {},
    soporteCount: Int = 0,
    onVerSoportes: () -> Unit = {},
    onCompanySelected: (CompanyEntity) -> Unit
) {
    val remoteCompanies by viewModel.remoteCompanies.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val finanzasKapta by viewModel.finanzasKapta.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchRemoteCompanies()
        viewModel.refreshFinanzasKapta()
    }

    val displayCompanies: List<CompanyEntity> = if (remoteCompanies.isNotEmpty()) {
        remoteCompanies.filter { !it.estado.equals("Eliminado", ignoreCase = true) }.map { r ->
            val codeVal = r.codigo.ifBlank { r.idEmpresa }
            val foundLocal = companies.find { it.code.equals(codeVal, ignoreCase = true) }
            foundLocal ?: CompanyEntity(
                code = codeVal,
                name = r.nombre.ifBlank { codeVal },
                country = r.pais.ifBlank { "Colombia" },
                status = "Activo"
            )
        }
    } else {
        companies.filter { !it.status.equals("Eliminado", ignoreCase = true) }
    }

    val totalCompaniesCount = displayCompanies.size
    val activeCompaniesCount = displayCompanies.count { it.getEffectiveStatus().equals("Activo", ignoreCase = true) }
    val expiringCompaniesCount = displayCompanies.count {
        it.getEffectiveStatus().contains("Vencer", ignoreCase = true)
    }
    val suspendedCompaniesCount = displayCompanies.count {
        val s = it.getEffectiveStatus()
        s.contains("Suspend", ignoreCase = true) || s.contains("Vencid", ignoreCase = true)
    }
    // ponytail: misma regla de cartera de Finanzas — vencidos solo si hace <= 7 días
    val carteraCount = displayCompanies.count {
        val estado = it.getEffectiveStatus()
        when {
            estado.contains("Vencer", ignoreCase = true) -> true
            estado.contains("Suspend", ignoreCase = true) || estado.contains("Vencid", ignoreCase = true) ->
                it.expirationDays >= -7
            else -> it.expirationDays in 0..5
        }
    }

    fun formatCurrency(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

    val primary = MaterialTheme.colorScheme.primary
    val amber = Color(0xFFFF9F0A)

    EtherealBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Section Header: Resumen general
            iOSSectionHeader(text = "Resumen general")

            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Negocios
                    GradientSummaryCard(
                        borderColors = listOf(Color(0xFFFFFFFF), Color(0xFF000CAD)),
                        iconRes = R.drawable.empresas_dashboard,
                        onClick = {
                            viewModel.setFilter("Todos")
                            onNavigateToEmpresasTab()
                        },
                        modifier = Modifier.weight(1f).height(122.dp),
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(totalCompaniesCount.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont, lineHeight = 64.sp)
                                Spacer(Modifier.width(10.dp))
                                Text("Negocios", fontSize = 17.sp, color = Color.Black, fontFamily = CardFont)
                            }
                            Text("En línea $activeCompaniesCount", fontSize = 14.sp, color = Color(0xFF08B414), fontFamily = CardFont)
                        }
                    )

                    // Card 2: Membresías por vencer
                    GradientSummaryCard(
                        borderColors = listOf(Color(0xFFFFFFFF), Color(0xFFFF9C28)),
                        iconRes = R.drawable.por_vencer_dashboard,
                        onClick = {
                            viewModel.setFilter("Por vencer")
                            onNavigateToEmpresasTab()
                        },
                        modifier = Modifier.weight(1f).height(122.dp),
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(expiringCompaniesCount.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont, lineHeight = 64.sp)
                                Spacer(Modifier.width(10.dp))
                                Text("Subs", fontSize = 17.sp, color = Color.Black, fontFamily = CardFont)
                            }
                            Text("por vencer", fontSize = 17.sp, color = Color.Black, fontFamily = CardFont)
                            Text("5 o menos días", fontSize = 14.sp, color = Color(0xFFFF8A00), fontFamily = CardFont)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 3: Ingresos
                    GradientSummaryCard(
                        borderColors = listOf(Color(0xFFFFFFFF), Color(0xFF009261)),
                        iconRes = R.drawable.ingresos_dashboard,
                        onClick = {
                            viewModel.setFinanceSubTab("Ingresos")
                            onNavigateToFinanzasTab()
                        },
                        modifier = Modifier.weight(1f).height(122.dp),
                        content = {
                            Text("Ingresos", fontSize = 17.sp, color = Color.Black, fontFamily = CardFont)
                            val ing = formatCurrency(finanzasKapta?.totalIngresos ?: 0.0)
                            val ingSym = ing.takeWhile { !it.isDigit() }
                            val ingNum = ing.dropWhile { !it.isDigit() }.ifEmpty { ing }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (ingSym.isNotEmpty()) Text(ingSym, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont)
                                Spacer(Modifier.width(4.dp))
                                Text(ingNum, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont, lineHeight = 36.sp)
                            }
                            Text("Planes Comprados", fontSize = 14.sp, color = Color(0xFF08B414), fontFamily = CardFont)
                        }
                    )

                    // Card 4: Egresos
                    GradientSummaryCard(
                        borderColors = listOf(Color(0xFFFFFFFF), Color(0xFFFF0000)),
                        iconRes = R.drawable.egreso_dashboard,
                        onClick = {
                            viewModel.setFinanceSubTab("Egresos")
                            onNavigateToFinanzasTab()
                        },
                        modifier = Modifier.weight(1f).height(122.dp),
                        content = {
                            Text("Egresos", fontSize = 17.sp, color = Color.Black, fontFamily = CardFont)
                            val egr = formatCurrency(finanzasKapta?.totalEgresos ?: 0.0)
                            val egrSym = egr.takeWhile { !it.isDigit() }
                            val egrNum = egr.dropWhile { !it.isDigit() }.ifEmpty { egr }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (egrSym.isNotEmpty()) Text(egrSym, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont)
                                Spacer(Modifier.width(4.dp))
                                Text(egrNum, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont, lineHeight = 36.sp)
                            }
                            Text("Gastos Kapta", fontSize = 14.sp, color = Color(0xFFFF0000), fontFamily = CardFont)
                        }
                    )
                }

                // Card 5: Solicitudes de Soporte
                GradientSummaryCard(
                    borderColors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
                    iconRes = R.drawable.soporte_dashboard,
                    onClick = onVerSoportes,
                    modifier = Modifier.fillMaxWidth().height(122.dp),
                    content = {
                        Text("Solicitudes de soporte", fontSize = 17.sp, color = Color.Black, fontFamily = CardFont)
                        Text(soporteCount.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = CardFont, lineHeight = 64.sp)
                        if (soporteCount == 0) {
                            Text("Sin pendientes", fontSize = 14.sp, color = Color(0xFF08B414), fontFamily = CardFont)
                        } else {
                            Text("$soporteCount solicitudes pendientes", fontSize = 14.sp, color = Color(0xFFFF8A00), fontFamily = CardFont)
                        }
                    }
                )
            }

            // Section Header: Acciones rápidas
            iOSSectionHeader(text = "Acciones rápidas")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardActionCard(
                    iconRes = R.drawable.quick_crear_negocio,
                    onClick = onNavigateToCreateCompany,
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
                DashboardActionCard(
                    iconRes = R.drawable.quick_admins,
                    onClick = onNavigateToAdministrators,
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
                DashboardActionCard(
                    iconRes = R.drawable.quick_planes,
                    onClick = onNavigateToPlans,
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
                DashboardActionCard(
                    iconRes = R.drawable.quick_reportes,
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
            }

            // Section Header: Negocios recientes
            iOSSectionHeader(text = "Negocios recientes")

            FlatBorderedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                // Header inside card
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Negocios recientes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = onNavigateToEmpresasTab,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Ver todos",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Company Items List
                    displayCompanies.take(5).forEachIndexed { index, company ->
                        // Determine Category Icon & Colors based on businessType
                        val (categoryIcon, iconBgColor, iconTint) = when {
                            company.businessType.contains("Supermercado", ignoreCase = true) || company.name.contains("Supermercado", ignoreCase = true) ->
                                Triple(Icons.Default.Storefront, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                            company.businessType.contains("Ferretería", ignoreCase = true) || company.name.contains("Ferretería", ignoreCase = true) ->
                                Triple(Icons.Default.Build, Color(0xFFFF9F0A).copy(alpha = 0.14f), Color(0xFFFF9F0A))
                            company.businessType.contains("Panadería", ignoreCase = true) || company.name.contains("Panadería", ignoreCase = true) ->
                                Triple(Icons.Default.BakeryDining, Color(0xFFE11D48).copy(alpha = 0.14f), Color(0xFFE11D48))
                            else ->
                                Triple(Icons.Default.Business, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                        }

                        // Determine Status Dot Color
                        val isSuspended = company.status.equals("Suspendido", ignoreCase = true)
                        val isExpiringSoon = company.expirationDays <= 5 && !isSuspended
                        val statusDotColor = when {
                            isSuspended -> Color(0xFFFF453A)
                            isExpiringSoon -> Color(0xFFFF9F0A)
                            else -> Color(0xFF34C759)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onCompanySelected(company) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Indicador circular de plan (degradado 135°)
                                PlanIndicatorCircle(company.plan, company.status, Modifier.size(14.dp))

                                Spacer(modifier = Modifier.width(10.dp))

                                // Recuadro del logo de la empresa (borde degradado, #f8f8f8)
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .border(2.dp, logoBorderBrush, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFF8F8F8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val logoUrl = company.logoUrl
                                    if (logoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = logoUrl,
                                            contentDescription = company.name,
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            imageVector = categoryIcon,
                                            contentDescription = company.businessType,
                                            tint = iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Business Info Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = company.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val daysText = if (company.expirationDays == 4) "4 días" else "${company.expirationDays} días"
                                    val subtitle = when {
                                        isSuspended -> "Suspendido"
                                        company.status.contains("Prueba", ignoreCase = true) -> "Prueba vence en $daysText"
                                        else -> "Vence en $daysText"
                                    }
                                    val subtitleColor = if (isSuspended) Color(0xFFFF453A) else MaterialTheme.colorScheme.onSurfaceVariant
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = subtitleColor,
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onCompanySelected(company) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_tres_puntos),
                                    contentDescription = "Opciones",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (index < 2) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card Footer Button
                    TextButton(
                        onClick = onNavigateToEmpresasTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Ver todos los negocios",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

            // Section Header: Alertas
            iOSSectionHeader(text = "Alertas")

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Alertas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Alertas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    AlertBulletItem(
                        text = "$expiringCompaniesCount ${if (expiringCompaniesCount == 1) "membresía por vencer" else "membresías por vencer"}",
                        color = Color(0xFFFF9F0A)
                    )
                    AlertBulletItem(
                        text = "$suspendedCompaniesCount ${if (suspendedCompaniesCount == 1) "negocio suspendido" else "negocios suspendidos"}",
                        color = Color(0xFFFF453A)
                    )
                    AlertBulletItem(
                        text = "$carteraCount ${if (carteraCount == 1) "pago pendiente de cobro" else "pagos pendientes de cobro"}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Section Header: Ingresos últimos 6 meses
            iOSSectionHeader(text = "Ingresos últimos 6 meses")

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Ingresos últimos 6 meses",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val ingresosMensuales = remember(finanzasKapta) {
                        calcularIngresosUltimos6Meses(finanzasKapta?.registros.orEmpty())
                    }
                    IngresosMensualesChart(datos = ingresosMensuales)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KAPTA AI © 2026",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Todos los derechos reservados.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * iOS summary card (insetGrouped)
 */
@Composable
private fun GlassSummaryCard(
    title: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    icon: ImageVector,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = subtextColor
            )
        }
    }
}

/**
 * iOS quick action button (chips row)
 */
@Composable
private fun GlassQuickActionButton(
    label: String,
    icon: ImageVector,
    bubbleBgColor: Color,
    glowColor: Color,
    iconTint: Color,
    isCustomContent: Boolean = false,
    customContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(82.dp)
            .height(96.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bubbleBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCustomContent && customContent != null) {
                    customContent()
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun DashboardActionCard(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
        start = Offset.Zero,
        end = Offset(1000f, 1000f)
    )
    Box(
        modifier = modifier
            .border(2.dp, borderBrush, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8F8F8))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.5f),
            contentScale = ContentScale.Fit
        )
    }
}

private val logoBorderBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
    start = Offset.Zero,
    end = Offset(1000f, 1000f)
)

private fun planColorPair(plan: String, status: String): Pair<Color, Color> = when {
    status.contains("Prueba", ignoreCase = true) -> Color(0xFF5CE1E6) to Color(0xFF0012FF)
    plan.contains("MAX", ignoreCase = true) -> Color(0xFF8C52FF) to Color(0xFFFF7A00)
    plan.contains("Premium", ignoreCase = true) -> Color(0xFF5CE1E6) to Color(0xFF8C52FF)
    else -> Color(0xFFFFFFFF) to Color(0xFF5CE1E6)
}

@Composable
private fun PlanIndicatorCircle(plan: String, status: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.size(14.dp)) {
        val px = maxWidth.value * LocalDensity.current.density
        val (c1, c2) = planColorPair(plan, status)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(c1, c2),
                        start = Offset(0f, 0f),
                        end = Offset(px, px)
                    )
                )
        )
    }
}

@Composable
private fun FlatBorderedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
        start = Offset.Zero,
        end = Offset(1000f, 1000f)
    )
    Box(
        modifier = modifier
            .border(2.dp, borderBrush, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8F8F8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun AlertBulletItem(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Ingresos (tipo=Ingreso) agrupados por mes calendario, últimos 6 meses
fun calcularIngresosUltimos6Meses(registros: List<com.example.data.remote.FinanzaKapta>): List<Pair<String, Double>> {
    val mesesEs = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val cal = java.util.Calendar.getInstance()
    val resultado = mutableListOf<Pair<String, Double>>()
    for (i in 5 downTo 0) {
        val c = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -i) }
        val prefijo = String.format("%04d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1)
        val total = registros
            .filter { it.tipo.equals("Ingreso", true) && it.fecha.startsWith(prefijo) }
            .sumOf { it.monto }
        resultado += mesesEs[c.get(java.util.Calendar.MONTH)] to total
    }
    return resultado
}

@Composable
private fun IngresosMensualesChart(datos: List<Pair<String, Double>>) {
    if (datos.all { it.second <= 0.0 }) {
        Text(
            text = "Aún no hay ingresos registrados en los últimos 6 meses.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    val maxVal = datos.maxOf { it.second }.coerceAtLeast(1.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            datos.forEach { (_, total) ->
                val frac = ((total / maxVal).toFloat()).coerceIn(0.03f, 1f)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = montoCompacto(total),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            datos.forEachIndexed { idx, (mes, _) ->
                Text(
                    text = mes,
                    fontSize = 11.sp,
                    fontWeight = if (idx == datos.lastIndex) FontWeight.Bold else FontWeight.Medium,
                    color = if (idx == datos.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun montoCompacto(amount: Double): String =
    when {
        amount >= 1_000_000 -> "$${String.format("%.1f", amount / 1_000_000)}M"
        amount >= 1_000 -> "$${(amount / 1_000).toInt()}K"
        else -> "$${amount.toInt()}"
    }

@Composable
fun AppLogoHeaderWidget(
    userName: String = "Brayam",
    userRole: String = "AI",
    userEmail: String = "AdminMauricio@kaptaia.com",
    notificationCount: Int = 3,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kapta_perfil", android.content.Context.MODE_PRIVATE) }
    var photoUrl by remember(userEmail) { mutableStateOf(prefs.getString("foto_$userEmail", "") ?: "") }

    val dockBorderBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
    // Degradado 135° acotado para botones pequeños (el de 1000px se veía todo blanco)
    val notifBorderBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
        start = Offset(0f, 0f),
        end = Offset(90f, 90f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8F8F8))
            .border(1.dp, dockBorderBrush, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo KAPTA IA (imagen de marca actual)
            Image(
                painter = painterResource(R.drawable.kapta_ia_logo),
                contentDescription = "KAPTA IA",
                modifier = Modifier.height(34.dp),
                contentScale = ContentScale.Fit
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón de notificaciones circular con borde degradado
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(CircleShape)
                        .background(Color(0xFFF8F8F8))
                        .border(1.dp, notifBorderBrush, CircleShape)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.notificacion),
                        contentDescription = "Notificaciones",
                        modifier = Modifier.size(22.dp).rotate(-45f),
                        contentScale = ContentScale.Fit
                    )

                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF5CE1E6), Color(0xFF8C52FF))
                                    )
                                )
                        )
                    }
                }

                // Foto de perfil del Super Admin con borde degradado + badge IA
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFF8F8F8))
                            .border(1.5.dp, notifBorderBrush, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Image(
                        painter = painterResource(R.drawable.recuadro_kapta_ia_mini),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GreetingHeaderWidget(
    userName: String = "Brayam",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = "Buenos días, $userName 👋",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Panel de Administración",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DashboardHeaderWidget(
    userName: String = "Brayam",
    userRole: String = "AI",
    notificationCount: Int = 3,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppLogoHeaderWidget(
            userName = userName,
            userRole = userRole,
            notificationCount = notificationCount,
            onNotificationClick = onNotificationClick,
            onProfileClick = onProfileClick
        )
        Spacer(modifier = Modifier.height(12.dp))
        GreetingHeaderWidget(
            userName = userName
        )
    }
}

// Fuente del dashboard: DM Sans (variable) ubicada en res/font/dm_sans.ttf.
private val CardFont = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

@Composable
private fun GradientSummaryCard(
    borderColors: List<Color>,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .defaultMinSize(minHeight = 122.dp)
            .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        val wpx = maxWidth.value * LocalDensity.current.density
        val hpx = maxHeight.value * LocalDensity.current.density
        val borderBrush = Brush.linearGradient(
            colorStops = arrayOf(0f to borderColors[0], 1f to borderColors[1]),
            start = Offset(0f, 0f),
            end = Offset(wpx, hpx)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(borderBrush)
                .padding(2.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF8F8F8))
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Columna izquierda (60%): contenido de la tarjeta.
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    content()
                }
                // Columna derecha (40%): icono marca de agua grande, cortado.
                Box(modifier = Modifier.weight(0.4f).fillMaxSize()) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(1f)
                            .aspectRatio(1f)
                            .offset(x = 30.dp)
                            .alpha(0.25f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}