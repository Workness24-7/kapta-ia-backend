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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.local.entity.CompanyEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.GlobalSearchModal
import com.example.ui.components.KaptaLogoHeader
import com.example.ui.components.SuperAdminScaffold
import kotlinx.coroutines.delay
import com.example.ui.components.UserProfileModal

@OptIn(ExperimentalMaterial3Api::class)
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
    var showSoporteModal by remember { mutableStateOf(false) }
    val allUsers by viewModel.users.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val notificationCount = com.example.ui.components.construirNotificaciones(companies).size + viewModel.soportes.value.size

    val activeCompCode by viewModel.activeCompanyCode.collectAsState()
    LaunchedEffect(activeCompCode) {
        viewModel.fetchRemoteCompanies()
        android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] SuperAdminDashboardScreen: role=SUPER_ADMIN, activeCompanyCode='$activeCompCode', salesCount=0, salesTotal=0")
        android.util.Log.d("KAPTA_UI_TOTAL", "[KAPTA_UI_TOTAL] role=SUPER_ADMIN, screen=SuperAdminDashboard, salesCount=0, salesTotal=0, displayedTotal=0")
    }
    LaunchedEffect(Unit) {
        viewModel.cargarSoportes()
        // Poll periódico para que las solicitudes de soporte del POS lleguen a notificaciones.
        while (true) {
            delay(15000)
            viewModel.cargarSoportes()
        }
    }

    if (showNotificationsModal) {
        com.example.ui.components.NotificationsModal(
            companies = companies,
            soportes = viewModel.soportes.value,
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

    if (showSoporteModal) {
        val lista = viewModel.soportes.value
        ModalBottomSheet(
            onDismissRequest = { showSoporteModal = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Solicitudes de Soporte", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                if (lista.isEmpty()) {
                    Text("Sin solicitudes por ahora.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    lista.forEach { s ->
                        val tipo = s["tipo_solicitud"]?.toString() ?: ""
                        val obs = s["observaciones"]?.toString() ?: ""
                        val sol = s["solicitante"]?.toString() ?: ""
                        val fecha = s["fecha_solicitud"]?.toString() ?: ""
                        val idS = s["id_soporte"]?.toString() ?: ""
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("$idS  •  $tipo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                Text("Solicitante: $sol", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Fecha: $fecha", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (obs.isNotBlank()) Text("Obs: $obs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { showSoporteModal = false }) { Text("Cerrar") }
            }
        }
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
                AppLogoHeaderWidget(
                    userEmail = "AdminMauricio@kaptaia.com",
                    notificationCount = notificationCount,
                    onNotificationClick = { showNotificationsModal = true },
                    onProfileClick = { showUserProfileModal = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val navBorderBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dock principal (cápsula) con los 4 iconos
                    Box(modifier = Modifier.weight(1f)) {
                        FloatingDockBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            isDarkMode = isDarkMode,
                            borderBrush = navBorderBrush
                        )
                    }

                    // Dock de búsqueda (isla circular independiente, misma altura que el dock)
                    val searchBorderBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
                        start = Offset(0f, 0f),
                        end = Offset(90f, 90f)
                    )
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
                            .clip(CircleShape)
                            .background(Color(0xFFF8F8F8))
                            .border(0.6.dp, searchBorderBrush, CircleShape)
                            .padding(7.dp)
                            .clickable { showGlobalSearchModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.lupa_icono),
                                contentDescription = "Buscar",
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        soporteCount = viewModel.soportes.value.size,
                        onVerSoportes = { showSoporteModal = true },
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
    isDarkMode: Boolean,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFA6A6A6)),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
) {
    val selectedBg = if (isDarkMode) Color(0xFF2A2A2E) else Color(0xFFEEF1F6)
    val selectedFg = if (isDarkMode) Color(0xFF8AB4FF) else Color(0xFF2563EB)
    val inactiveFg = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF8F8F8))
            .border(0.6.dp, borderBrush, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                painter = R.drawable.inicio_icono,
                contentDescription = "Inicio",
                selectedBg = selectedBg,
                selectedFg = selectedFg,
                inactiveFg = inactiveFg
            )
            DockItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                painter = R.drawable.empresas_icono,
                contentDescription = "Empresas",
                selectedBg = selectedBg,
                selectedFg = selectedFg,
                inactiveFg = inactiveFg
            )
            DockItem(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                painter = R.drawable.modulo_financiero_icono,
                contentDescription = "Finanzas",
                selectedBg = selectedBg,
                selectedFg = selectedFg,
                inactiveFg = inactiveFg
            )
            DockItem(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                painter = R.drawable.usuarios_icono,
                contentDescription = "Usuarios",
                selectedBg = selectedBg,
                selectedFg = selectedFg,
                inactiveFg = inactiveFg
            )
        }
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    onClick: () -> Unit,
    painter: Int,
    contentDescription: String,
    selectedBg: Color,
    selectedFg: Color,
    inactiveFg: Color
) {
    val bg by animateColorAsState(
        if (selected) selectedBg else Color.Transparent,
        animationSpec = tween(250)
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = Modifier.size(52.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(painter),
                contentDescription = contentDescription,
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
