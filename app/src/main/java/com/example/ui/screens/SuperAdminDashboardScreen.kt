package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.entity.CompanyEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.GlobalSearchModal
import com.example.ui.components.KaptaLogoHeader
import com.example.ui.components.SuperAdminScaffold
import com.example.ui.components.UserProfileModal

@Composable
fun SuperAdminDashboardScreen(
    viewModel: KaptaViewModel,
    onNavigateToCreateCompany: () -> Unit,
    onNavigateToEditCompany: (CompanyEntity) -> Unit,
    onNavigateToCompanyDetail: (CompanyEntity) -> Unit,
    onNavigateToAdministrators: () -> Unit,
    onNavigateToMemberships: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToReports: () -> Unit,
    onEnterAsAdmin: (CompanyEntity) -> Unit,
    onLogoutToRedirection: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inicio, 1: Empresas, 2: Finanzas, 3: Usuarios
    var showGlobalSearchModal by remember { mutableStateOf(false) }
    var showUserProfileModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    val allUsers by viewModel.users.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val notificationCount = com.example.ui.components.construirNotificaciones(companies).size

    val activeCompCode by viewModel.activeCompanyCode.collectAsState()
    LaunchedEffect(activeCompCode) {
        viewModel.fetchRemoteCompanies()
        android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] SuperAdminDashboardScreen: role=SUPER_ADMIN, activeCompanyCode='$activeCompCode', salesCount=0, salesTotal=0")
        android.util.Log.d("KAPTA_UI_TOTAL", "[KAPTA_UI_TOTAL] role=SUPER_ADMIN, screen=SuperAdminDashboard, salesCount=0, salesTotal=0, displayedTotal=0")
    }

    var headerCollapsePx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    // Solo colapsa el saludo (58dp): el dock sube bajo el logo pero NUNCA sale de la vista
    var maxCollapsePx by remember { mutableFloatStateOf(with(density) { 58.dp.toPx() }) }
    // Altura real del saludo (58dp -> 0dp): layout de verdad, el contenido sube con el dock sin huecos
    val saludoAltura = with(density) { (maxCollapsePx + headerCollapsePx).coerceAtLeast(0f).toDp() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) { // Scrolling down -> collapse greeting + dock first
                    val oldOffset = headerCollapsePx
                    val newOffset = (headerCollapsePx + delta).coerceIn(-maxCollapsePx, 0f)
                    val consumed = newOffset - oldOffset
                    headerCollapsePx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0) { // Scrolling up at top -> expand again
                    val oldOffset = headerCollapsePx
                    val newOffset = (headerCollapsePx + delta).coerceIn(-maxCollapsePx, 0f)
                    val consumedCustom = newOffset - oldOffset
                    headerCollapsePx = newOffset
                    return Offset(0f, consumedCustom)
                }
                return Offset.Zero
            }
        }
    }

    if (showNotificationsModal) {
        com.example.ui.components.NotificationsModal(
            companies = companies,
            onDismiss = { showNotificationsModal = false },
            onCompanyClick = { company ->
                showNotificationsModal = false
                viewModel.selectCompany(company)
                onNavigateToCompanyDetail(company)
            }
        )
    }

    if (showGlobalSearchModal) {
        GlobalSearchModal(
            viewModel = viewModel,
            onDismiss = { showGlobalSearchModal = false },
            onNavigateToCompanyDetail = onNavigateToCompanyDetail,
            onNavigateToEditCompany = onNavigateToEditCompany,
            onEnterAsAdmin = onEnterAsAdmin
        )
    }

    if (showUserProfileModal) {
        val sesion by viewModel.currentUser.collectAsState()
        UserProfileModal(
            userName = sesion?.name ?: "SuperAdmin (Kapta IA)",
            userRole = sesion?.role ?: "Administrador",
            userEmail = sesion?.email ?: "AdminMauricio@kaptaia.com",
            userPhone = "",
            userLanguage = "Español",
            isDarkMode = isDarkMode,
            companyUsers = allUsers,
            companies = companies,
            viewerIsSuperAdmin = true,
            onDarkModeToggle = { viewModel.toggleDarkMode(it) },
            onDeleteUser = { userToDelete -> viewModel.deleteUser(userToDelete) },
            onSaveUser = { nuevo -> viewModel.createOrUpdateUser(nuevo) },
            onDismiss = { showUserProfileModal = false },
            onLogout = { onLogoutToRedirection() }
        )
    }

    SuperAdminScaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fixed App Logo Header Container (KAPTA IA + Notification Bell + Profile Avatar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(2f)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    AppLogoHeaderWidget(
                        userName = "Brayam",
                        userRole = "AI",
                        notificationCount = notificationCount,
                        onNotificationClick = { showNotificationsModal = true },
                        onProfileClick = { showUserProfileModal = true }
                    )
                }

                // Saludo colapsable: su ALTURA REAL se encoge con el scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(saludoAltura)
                        .clipToBounds(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Greeting ("Buenos días, Brayam 👋" & "Panel de Administración")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        GreetingHeaderWidget(
                            userName = "Brayam"
                        )
                    }
                }

                // Floating Dock Bar (Inicio, Empresas, Finanzas, Usuarios + Lupa)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    FloatingDockBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onSearchClick = { showGlobalSearchModal = true },
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> TabInicio(
                        viewModel = viewModel,
                        onNavigateToCreateCompany = onNavigateToCreateCompany,
                        onNavigateToAdministrators = onNavigateToAdministrators,
                        onNavigateToMemberships = onNavigateToMemberships,
                        onNavigateToPlans = onNavigateToPlans,
                        onNavigateToReports = onNavigateToReports,
                        onNavigateToEmpresasTab = { selectedTab = 1 },
                        onNavigateToFinanzasTab = { selectedTab = 2 },
                        onCompanySelected = { company ->
                            viewModel.selectCompany(company)
                            onNavigateToCompanyDetail(company)
                        }
                    )

                    1 -> TabEmpresas(
                        viewModel = viewModel,
                        onNavigateToCreateCompany = onNavigateToCreateCompany,
                        onNavigateToEditCompany = onNavigateToEditCompany,
                        onNavigateToCompanyDetail = onNavigateToCompanyDetail,
                        onEnterAsAdmin = onEnterAsAdmin,
                        onLogoutToRedirection = onLogoutToRedirection,
                        onNavigateToPlans = onNavigateToPlans
                    )

                    2 -> TabFinanzas(viewModel = viewModel)

                    3 -> TabUsuarios(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun FloatingDockBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    isDarkMode: Boolean
) {
    val dockBg = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val dockBorder = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB)
    val selectedBg = if (isDarkMode) Color(0xFF2A2A2E) else Color(0xFFF1F4F9)
    val inactiveFg = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val selectedFg = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            color = dockBg,
            border = BorderStroke(1.dp, dockBorder),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DockItem(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        isDarkMode = isDarkMode
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DockItem(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        icon = Icons.Default.Business,
                        label = "Empresas",
                        isDarkMode = isDarkMode
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DockItem(
                        selected = selectedTab == 2,
                        onClick = { onTabSelected(2) },
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Finanzas",
                        isDarkMode = isDarkMode
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DockItem(
                        selected = selectedTab == 3,
                        onClick = { onTabSelected(3) },
                        icon = Icons.Default.People,
                        label = "Usuarios",
                        isDarkMode = isDarkMode
                    )
                }
            }
        }

        // Botón de búsqueda circular independiente
        Surface(
            onClick = { onSearchClick() },
            shape = CircleShape,
            color = dockBg,
            border = BorderStroke(1.dp, dockBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = inactiveFg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isDarkMode: Boolean
) {
    val selectedBg = if (isDarkMode) Color(0xFF2A2A2E) else Color(0xFFF1F4F9)
    val inactiveFg = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val selectedFg = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        if (selected) selectedBg else Color.Transparent,
        animationSpec = tween(250)
    )
    val fg by animateColorAsState(
        if (selected) selectedFg else inactiveFg,
        animationSpec = tween(250)
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
