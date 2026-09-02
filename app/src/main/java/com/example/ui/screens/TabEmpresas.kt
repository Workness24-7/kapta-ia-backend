package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSButton
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSPill
import com.example.ui.components.iOSSectionHeader
import com.example.ui.theme.LocalIsDarkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabEmpresas(
    viewModel: KaptaViewModel,
    onNavigateToCreateCompany: () -> Unit,
    onNavigateToEditCompany: (CompanyEntity) -> Unit,
    onNavigateToCompanyDetail: (CompanyEntity) -> Unit,
    onEnterAsAdmin: (CompanyEntity) -> Unit,
    onLogoutToRedirection: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val companies by viewModel.filteredCompanies.collectAsState()
    val allCompanies by viewModel.companies.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showFilterModal by remember { mutableStateOf(false) }
    var selectedCompanyForOptions by remember { mutableStateOf<CompanyEntity?>(null) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var companyForResetPassword by remember { mutableStateOf<CompanyEntity?>(null) }
    var showRenewPaymentDialog by remember { mutableStateOf<CompanyEntity?>(null) }
    var newPasswordInput by remember { mutableStateOf("") }

    val activeCount = allCompanies.count { it.getEffectiveStatus().equals("Activo", ignoreCase = true) }
    val expCount = allCompanies.count { it.getEffectiveStatus().equals("Por vencer", ignoreCase = true) }
    val suspendedCount = allCompanies.count { it.getEffectiveStatus().equals("Suspendido", ignoreCase = true) }
    val trialCount = allCompanies.count { it.status.contains("Prueba") || it.plan.contains("Prueba") }

    EtherealBackground {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    iOSLargeTitle(
                        title = "Empresas",
                        subtitle = "Administra todos los negocios registrados"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CompaniesSummaryHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        selectedFilter = selectedFilter,
                        onSelectFilter = { viewModel.setFilter(it) },
                        onOpenFilterModal = { showFilterModal = true },
                        activeCount = activeCount,
                        expCount = expCount,
                        suspendedCount = suspendedCount,
                        trialCount = trialCount,
                        totalCount = allCompanies.size
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    iOSSectionHeader("Empresas registradas")
                }

                // List of companies
                items(companies, key = { it.id }) { company ->
                    CompanyListItemCard(
                        company = company,
                        onVer = {
                            viewModel.selectCompany(company)
                            onNavigateToCompanyDetail(company)
                        },
                        onEditar = {
                            viewModel.selectCompany(company)
                            onNavigateToEditCompany(company)
                        },
                        onOptionsClick = {
                            selectedCompanyForOptions = company
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Floating action pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 16.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onNavigateToCreateCompany() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Crear empresa",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crear empresa",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Modal Sheet 1: Filtros (KaptaIA_empresas_filtros.jpg)
    if (showFilterModal) {
        ModalBottomSheet(
            onDismissRequest = { showFilterModal = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.25f),
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtros",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showFilterModal = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filterOptions = listOf(
                        "Todos", "Recientes", "Nuevo", "Por vencer", "Vencido", "Prueba", "Activo", "Por suspender"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(filterOptions) { opt ->
                            val isSelected = selectedFilter == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.setFilter(opt)
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        iOSButton(
                            text = "Limpiar",
                            onClick = {
                                viewModel.setFilter("Todos")
                                showFilterModal = false
                            },
                            tinted = true
                        )
                        iOSButton(
                            text = "Aplicar",
                            onClick = { showFilterModal = false }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet 2: Opciones de Empresa (...) (KaptaIA_empresas_opciones.jpg)
    if (selectedCompanyForOptions != null) {
        val company = selectedCompanyForOptions!!
        ModalBottomSheet(
            onDismissRequest = { selectedCompanyForOptions = null },
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.25f),
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(0.6.dp, Color.White.copy(alpha = if (isDarkMode) 0.12f else 0.55f), RoundedCornerShape(32.dp))
                    .padding(vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = company.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )
                    Text(
                        text = "Opciones para la empresa",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OptionMenuItem(
                        title = "Ver información",
                        icon = Icons.Default.Visibility,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val c = company
                            selectedCompanyForOptions = null
                            viewModel.selectCompany(c)
                            onNavigateToCompanyDetail(c)
                        }
                    )

                    OptionMenuItem(
                        title = "Editar empresa",
                        icon = Icons.Default.Edit,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val c = company
                            selectedCompanyForOptions = null
                            viewModel.selectCompany(c)
                            onNavigateToEditCompany(c)
                        }
                    )

                    OptionMenuItem(
                        title = "Ingresar como administrador",
                        icon = Icons.Default.Login,
                        iconColor = Color(0xFF34C759),
                        onClick = {
                            val c = company
                            selectedCompanyForOptions = null
                            onEnterAsAdmin(c)
                        }
                    )

                    OptionMenuItem(
                        title = "Restablecer contraseña",
                        icon = Icons.Default.LockReset,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            companyForResetPassword = company
                            selectedCompanyForOptions = null
                            showResetPasswordDialog = true
                        }
                    )

                    OptionMenuItem(
                        title = "Copiar credenciales",
                        icon = Icons.Default.ContentCopy,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            viewModel.copyCredentials(company)
                            selectedCompanyForOptions = null
                        }
                    )

                    OptionMenuItem(
                        title = "Renovar membresía",
                        icon = Icons.Default.Refresh,
                        iconColor = Color(0xFFFF9F0A),
                        onClick = {
                            val c = company
                            selectedCompanyForOptions = null
                            showRenewPaymentDialog = c
                        }
                    )

                    OptionMenuItem(
                        title = "Cambiar Plan",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFF9F0A),
                        onClick = {
                            selectedCompanyForOptions = null
                            onNavigateToPlans()
                        }
                    )

                    OptionMenuItem(
                        title = if (company.status == "Suspendido") "Reactivar empresa" else "Suspender empresa",
                        icon = Icons.Default.Pause,
                        iconColor = Color(0xFFFF453A),
                        onClick = {
                            viewModel.suspendCompany(company)
                            selectedCompanyForOptions = null
                        }
                    )

                    OptionMenuItem(
                        title = "Eliminar empresa",
                        icon = Icons.Default.Delete,
                        iconColor = Color(0xFFFF453A),
                        onClick = {
                            viewModel.deleteCompany(company)
                            selectedCompanyForOptions = null
                        }
                    )

                    OptionMenuItem(
                        title = "Cierre de sesión",
                        icon = Icons.Default.Logout,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            selectedCompanyForOptions = null
                            onLogoutToRedirection()
                        }
                    )
                }
            }
        }
    }

    // Renew Membership Confirmation Dialog
    if (showRenewPaymentDialog != null) {
        val company = showRenewPaymentDialog!!
        AlertDialog(
            onDismissRequest = { showRenewPaymentDialog = null },
            title = { Text("Confirmar Pago de Renovación") },
            text = {
                Text("¿Confirmar recepción de pago para renovar la membresía de ${company.name} por 30 días más?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renewMembership(company)
                        showRenewPaymentDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                ) {
                    Text("Confirmar Pago", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenewPaymentDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Reset Password Dialog
    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            title = { Text("Restablecer Contraseña") },
            text = {
                Column {
                    Text("Selecciona o escribe la nueva contraseña para el administrador de la empresa:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Nueva Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPasswordInput.isNotBlank()) {
                            companyForResetPassword?.let { company ->
                                val adminUser = CompanyUserEntity(
                                    companyId = company.id,
                                    companyCode = company.code,
                                    name = company.adminName,
                                    role = "Administrador",
                                    email = company.adminEmail,
                                    username = company.adminEmail.substringBefore("@").ifBlank { company.adminName },
                                    password = newPasswordInput,
                                    isSynced = true
                                )
                                viewModel.resetUserPassword(adminUser, newPasswordInput)
                            }
                            showResetPasswordDialog = false
                            newPasswordInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun StatusSummaryChip(title: String, count: String, dotColor: Color, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun CompanyListItemCard(
    company: CompanyEntity,
    onVer: () -> Unit,
    onEditar: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveStatus = company.getEffectiveStatus()
    val accentColor = when {
        effectiveStatus.contains("Acti", ignoreCase = true) -> Color(0xFF34C759) // Green
        effectiveStatus.contains("Venc", ignoreCase = true) -> Color(0xFFFF9F0A) // Amber
        effectiveStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFFF453A) // Red
        else -> MaterialTheme.colorScheme.primary // Trial
    }

    val logoIcon = when {
        company.name.contains("Supermercado", ignoreCase = true) || company.name.contains("14", ignoreCase = true) -> Icons.Outlined.Storefront
        company.name.contains("Ferretería", ignoreCase = true) || company.name.contains("López", ignoreCase = true) -> Icons.Outlined.Build
        company.name.contains("prueba", ignoreCase = true) -> Icons.Outlined.Science
        else -> Icons.Outlined.Storefront
    }

    GlassCard(
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Border
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Header Row: Logo, Title, Badges & Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Circular Logo Container
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(1.5.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = logoIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = company.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Badge
                                iOSPill(
                                    text = if (effectiveStatus.contains("Venc", ignoreCase = true)) "Por vencer (Vence en ${company.expirationDays} días)" else effectiveStatus,
                                    color = accentColor
                                )

                                // Plan Badge
                                val planClean = company.plan.lowercase()
                                val isMaxAi = planClean.contains("max") || planClean.contains("ia")
                                val isPremiumPlan = planClean.contains("premium")

                                if (isMaxAi) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary, // Purple
                                                        Color(0xFFF97316)  // Orange
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = company.plan,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else if (isPremiumPlan) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Diamond,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = company.plan,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    // Básico / Estándar / Default
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF34C759).copy(alpha = 0.14f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.StarOutline,
                                                contentDescription = null,
                                                tint = Color(0xFF34C759),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = company.plan,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34C759)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info Row: Administrator & Expiration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Administrador",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = company.adminName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (company.getEffectiveStatus().equals("Suspendido", ignoreCase = true)) {
                        // Suspendida = no pagó: se cuentan los días que lleva vencida, no los restantes
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "En vencido",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${company.diasEnVencido()} días",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    } else if (company.expirationDays > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Vence en",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${company.expirationDays} días",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row (Ver, Editar, Contactar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ver Button (Soft Outlined style)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { onVer() }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ver",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Editar Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { onEditar() }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Editar",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Contactar Button (WhatsApp directo al teléfono de la empresa)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF25D366).copy(alpha = 0.14f))
                            .border(0.6.dp, Color(0xFF25D366), RoundedCornerShape(12.dp))
                            .clickable {
                                val numero = company.phone.filter { it.isDigit() }
                                val full = if (numero.length == 10) "57$numero" else numero
                                val msg = java.net.URLEncoder.encode(
                                    "Hola ${company.name}, le contactamos de KAPTA IA.",
                                    "UTF-8"
                                )
                                try {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://wa.me/$full?text=$msg")
                                        )
                                    )
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context, "No se encontró WhatsApp instalado",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAddAlt1,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contactar",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF25D366)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompaniesSummaryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onOpenFilterModal: () -> Unit,
    activeCount: Int,
    expCount: Int,
    suspendedCount: Int,
    trialCount: Int,
    totalCount: Int
) {
    val isDark = LocalIsDarkMode.current
    val searchFieldBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE9E9EB)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Barra de Búsqueda y Filtros (Fila Superior)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(searchFieldBg)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar empresa, NIT, correo o administrador...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Filter Button
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(searchFieldBg)
                    .clickable { onOpenFilterModal() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filtros",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (selectedFilter == "Todos") "Filtros" else selectedFilter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Fila Compacta de Métricas por Estado (Interactiva y con resaltado visual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Activas
            val isActivasSelected = selectedFilter.contains("Activ", ignoreCase = true)
            MetricIconChip(
                icon = Icons.Outlined.Storefront,
                iconColor = Color(0xFF34C759),
                count = "$activeCount",
                isSelected = isActivasSelected,
                onClick = {
                    if (isActivasSelected) onSelectFilter("Todos") else onSelectFilter("Activo")
                },
                modifier = Modifier.weight(1f)
            )

            // Card 2: Por vencer
            val isExpSelected = selectedFilter.contains("Vencer", ignoreCase = true)
            MetricIconChip(
                icon = Icons.Outlined.CalendarToday,
                iconColor = Color(0xFFFF9F0A),
                count = "$expCount",
                isSelected = isExpSelected,
                onClick = {
                    if (isExpSelected) onSelectFilter("Todos") else onSelectFilter("Por vencer")
                },
                modifier = Modifier.weight(1f)
            )

            // Card 3: Suspendidas
            val isSuspendedSelected = selectedFilter.contains("Suspend", ignoreCase = true)
            MetricIconChip(
                icon = Icons.Outlined.PauseCircle,
                iconColor = Color(0xFFFF453A),
                count = "$suspendedCount",
                isSelected = isSuspendedSelected,
                onClick = {
                    if (isSuspendedSelected) onSelectFilter("Todos") else onSelectFilter("Suspendidos")
                },
                modifier = Modifier.weight(1f)
            )

            // Card 4: De prueba
            val isTrialSelected = selectedFilter.contains("Prueba", ignoreCase = true)
            MetricIconChip(
                icon = Icons.Outlined.Science,
                iconColor = MaterialTheme.colorScheme.primary,
                count = "$trialCount",
                isSelected = isTrialSelected,
                onClick = {
                    if (isTrialSelected) onSelectFilter("Todos") else onSelectFilter("Prueba")
                },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Tarjeta de Total de Empresas (Compacta, interactiva)
        val isTotalSelected = selectedFilter.equals("Todos", ignoreCase = true) || selectedFilter.isEmpty()
        val totalBgColor = if (isTotalSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        val totalBorderColor = if (isTotalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        val totalBorderWidth = if (isTotalSelected) 2.dp else 1.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(totalBgColor)
                .border(totalBorderWidth, totalBorderColor, RoundedCornerShape(16.dp))
                .clickable { onSelectFilter("Todos") }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isTotalSelected) 0.25f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Domain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Total de empresas",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isTotalSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isTotalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MetricIconChip(
    icon: ImageVector,
    iconColor: Color,
    count: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) iconColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) iconColor else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) iconColor.copy(alpha = 0.28f) else iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(iconColor)
                        .border(0.6.dp, Color.White, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) iconColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun OptionMenuItem(title: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
