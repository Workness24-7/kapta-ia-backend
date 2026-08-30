package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.ui.KaptaViewModel
import com.example.ui.theme.LocalIsDarkMode
import java.util.Locale

@Composable
fun GlobalSearchModal(
    viewModel: KaptaViewModel,
    onDismiss: () -> Unit,
    onNavigateToCompanyDetail: (CompanyEntity) -> Unit,
    onNavigateToEditCompany: (CompanyEntity) -> Unit,
    onEnterAsAdmin: (CompanyEntity) -> Unit
) {
    val companies by viewModel.companies.collectAsState()
    val users by viewModel.users.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = LocalIsDarkMode.current
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Search Bar & Header Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cardColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Buscador Global Inteligente",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )
                            Text(
                                text = "Empresas, Estados, Planes y Usuarios",
                                fontSize = 11.sp,
                                color = subtitleColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(borderColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Busca por nombre, estado, plan (ej. 'por vencer', 'MAX')...",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF0284C7)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = titleColor,
                        unfocusedTextColor = titleColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickSearchChip(
                        label = "Todas",
                        icon = Icons.Default.Business,
                        iconTint = Color(0xFF2563EB),
                        selected = searchQuery.isBlank(),
                        onClick = { searchQuery = "" }
                    )
                    QuickSearchChip(
                        label = "Por vencer",
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFD97706),
                        selected = searchQuery.contains("vencer", ignoreCase = true),
                        onClick = { searchQuery = "por vencer" }
                    )
                    QuickSearchChip(
                        label = "Suspendidos",
                        icon = Icons.Default.Cancel,
                        iconTint = Color(0xFFDC2626),
                        selected = searchQuery.contains("suspen", ignoreCase = true),
                        onClick = { searchQuery = "suspendido" }
                    )
                    QuickSearchChip(
                        label = "Activos",
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF059669),
                        selected = searchQuery.contains("acti", ignoreCase = true),
                        onClick = { searchQuery = "activo" }
                    )
                    QuickSearchChip(
                        label = "Plan MAX IA",
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFF7C3AED),
                        selected = searchQuery.contains("max", ignoreCase = true),
                        onClick = { searchQuery = "MAX" }
                    )
                    QuickSearchChip(
                        label = "Plan Premium",
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFFD97706),
                        selected = searchQuery.contains("premium", ignoreCase = true),
                        onClick = { searchQuery = "Premium" }
                    )
                    QuickSearchChip(
                        label = "Plan Básico",
                        icon = Icons.Default.Bookmark,
                        iconTint = Color(0xFF2563EB),
                        selected = searchQuery.contains("básico", ignoreCase = true) || searchQuery.contains("basico", ignoreCase = true),
                        onClick = { searchQuery = "Básico" }
                    )
                    QuickSearchChip(
                        label = "Usuarios",
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF7C3AED),
                        selected = searchQuery.contains("usuario", ignoreCase = true),
                        onClick = { searchQuery = "admin" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter logic
                val query = searchQuery.trim().lowercase()

                val matchingCompanies = companies.filter { company ->
                    if (company.status.equals("Eliminado", ignoreCase = true)) false
                    else if (query.isBlank()) true
                    else {
                        val effStatus = company.getEffectiveStatus().lowercase()
                        val rawStatus = company.status.lowercase()
                        val planName = company.plan.lowercase()
                        val name = company.name.lowercase()
                        val nit = company.nit.lowercase()
                        val city = company.city.lowercase()
                        val adminName = company.adminName.lowercase()
                        val adminEmail = company.adminEmail.lowercase()
                        val phone = company.phone.lowercase()

                        val isVencerQuery = query.contains("vencer") || query.contains("por vencer")
                        val isSuspendQuery = query.contains("suspen")
                        val isActivoQuery = query.contains("acti")
                        val isVencidoQuery = query.contains("vencid")

                        if (isVencerQuery && effStatus.contains("vencer")) return@filter true
                        if (isSuspendQuery && effStatus.contains("suspen")) return@filter true
                        if (isActivoQuery && effStatus.contains("acti")) return@filter true
                        if (isVencidoQuery && effStatus.contains("vencid")) return@filter true

                        val isMaxQuery = query.contains("max")
                        val isPremiumQuery = query.contains("premium")
                        val isBasicoQuery = query.contains("básic") || query.contains("basic")

                        if (isMaxQuery && planName.contains("max")) return@filter true
                        if (isPremiumQuery && planName.contains("premium")) return@filter true
                        if (isBasicoQuery && (planName.contains("básic") || planName.contains("basic"))) return@filter true

                        name.contains(query) ||
                        effStatus.contains(query) ||
                        rawStatus.contains(query) ||
                        planName.contains(query) ||
                        nit.contains(query) ||
                        city.contains(query) ||
                        adminName.contains(query) ||
                        adminEmail.contains(query) ||
                        phone.contains(query)
                    }
                }

                val matchingUsers = users.filter { user ->
                    if (query.isBlank()) false
                    else {
                        user.name.lowercase().contains(query) ||
                        user.username.lowercase().contains(query) ||
                        user.email.lowercase().contains(query) ||
                        user.role.lowercase().contains(query) ||
                        user.companyCode.lowercase().contains(query)
                    }
                }

                val totalResults = matchingCompanies.size + matchingUsers.size

                // Header Results Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (query.isBlank()) "Empresas registradas (${companies.size})" else "Resultados encontrados ($totalResults)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )

                    if (query.isNotBlank()) {
                        Text(
                            text = "Filtrado por '$searchQuery'",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Search Results
                if (matchingCompanies.isEmpty() && matchingUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No se encontraron resultados para '$searchQuery'",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Prueba buscar por 'por vencer', 'suspendido', 'MAX' o el nombre del negocio",
                                fontSize = 12.sp,
                                color = subtitleColor
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (matchingCompanies.isNotEmpty()) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EMPRESAS (${matchingCompanies.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                            }

                            items(matchingCompanies, key = { "comp_${it.id}" }) { company ->
                                SearchCompanyCard(
                                    company = company,
                                    onViewDetail = {
                                        onDismiss()
                                        viewModel.selectCompany(company)
                                        onNavigateToCompanyDetail(company)
                                    },
                                    onEdit = {
                                        onDismiss()
                                        onNavigateToEditCompany(company)
                                    },
                                    onEnterAsAdmin = {
                                        onDismiss()
                                        onEnterAsAdmin(company)
                                    }
                                )
                            }
                        }

                        if (matchingUsers.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "USUARIOS (${matchingUsers.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                }
                            }

                            items(matchingUsers, key = { "user_${it.id}" }) { user ->
                                val matchingComp = companies.find { it.id == user.companyId || it.code.equals(user.companyCode, ignoreCase = true) }
                                SearchUserCard(user = user, company = matchingComp)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchChip(
    label: String,
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFF2563EB),
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Color(0xFF2563EB)
                else if (isDark) Color(0xFF1E293B) else Color.White
            )
            .border(
                1.dp,
                if (selected) Color(0xFF2563EB) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
        }
    }
}

@Composable
private fun SearchCompanyCard(
    company: CompanyEntity,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onEnterAsAdmin: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val effStatus = company.getEffectiveStatus()
    val statusBg = when {
        effStatus.contains("Acti", ignoreCase = true) -> Color(0xFFECFDF5)
        effStatus.contains("Venc", ignoreCase = true) -> Color(0xFFFEF3C7)
        effStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFFEE2E2)
        else -> Color(0xFFEFF6FF)
    }
    val statusFg = when {
        effStatus.contains("Acti", ignoreCase = true) -> Color(0xFF059669)
        effStatus.contains("Venc", ignoreCase = true) -> Color(0xFFD97706)
        effStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFDC2626)
        else -> Color(0xFF2563EB)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = company.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Plan: ${company.plan} • NIT: ${company.nit.ifBlank { "N/A" }}",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (effStatus.contains("Venc", ignoreCase = true)) "Por vencer (${company.expirationDays}d)" else effStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Contacto: ${company.adminName} (${company.phone}) • ${company.city}",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewDetail,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver Detalle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onEnterAsAdmin,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchUserCard(user: CompanyUserEntity, company: CompanyEntity?) {
    val isDark = LocalIsDarkMode.current
    val compName = company?.name ?: user.companyCode
    val effStatus = company?.getEffectiveStatus() ?: "Activo"

    val statusBg = when {
        effStatus.contains("Acti", ignoreCase = true) -> Color(0xFFECFDF5)
        effStatus.contains("Venc", ignoreCase = true) -> Color(0xFFFEF3C7)
        effStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFFEE2E2)
        else -> Color(0xFFEFF6FF)
    }
    val statusFg = when {
        effStatus.contains("Acti", ignoreCase = true) -> Color(0xFF059669)
        effStatus.contains("Venc", ignoreCase = true) -> Color(0xFFD97706)
        effStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFDC2626)
        else -> Color(0xFF2563EB)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = statusFg,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = compName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusFg
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = user.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                    Text(
                        text = "${user.email} • ${user.role}",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = effStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusFg
                )
            }
        }
    }
}
