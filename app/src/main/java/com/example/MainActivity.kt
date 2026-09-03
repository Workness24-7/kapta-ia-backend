package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.KaptaViewModel
import com.example.ui.screens.AdministratorsScreen
import com.example.ui.screens.CompanyDetailScreen
import com.example.ui.screens.CompanyLoginScreen
import com.example.ui.screens.CreateEditCompanyScreen
import com.example.ui.screens.PlansMembershipsScreen
import com.example.ui.screens.RedirectionLoginScreen
import com.example.ui.screens.SuperAdminDashboardScreen
import com.example.ui.screens.SuperAdminLoginScreen
import com.example.ui.screens.TenantPosScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.BusinessTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.ReporteErrores.instalar(this)
        com.example.util.Notificaciones.crearCanal(this)
        enableEdgeToEdge()
        setContent {
            KaptaApp()
        }
    }
}

@Composable
fun KaptaApp() {
    val navController = rememberNavController()
    val viewModel: KaptaViewModel = viewModel()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Strictly exclude login screens and POS from dark mode so they retain their original look
    val forceLightScreen = currentRoute == "redirection" ||
            currentRoute == "admin_login" ||
            currentRoute == "tenant_pos" ||
            (currentRoute != null && (currentRoute.startsWith("company_login") || currentRoute.startsWith("consentimiento")))

    val effectiveDarkMode = isDarkMode && !forceLightScreen

    // Inicio predeterminado: si hay un negocio usado recientemente, abrir su login directo.
    // Volver al login de redirección solo es posible pulsando atrás 5 veces desde el login del negocio.
    val context = androidx.compose.ui.platform.LocalContext.current
    val ultimoCodigo = remember {
        context.getSharedPreferences("kapta_session", android.content.Context.MODE_PRIVATE)
            .getString("ultimo_codigo_negocio", null)
    }
    val startDestination = if (!ultimoCodigo.isNullOrBlank()) "company_login/$ultimoCodigo" else "redirection"

    // Reporte de error pendiente de un cierre anterior: ofrecer enviarlo a soporte.
    var reportePendiente by remember { mutableStateOf(com.example.util.ReporteErrores.pendiente(context)) }

    MyApplicationTheme(darkTheme = effectiveDarkMode) {
        if (reportePendiente != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { reportePendiente = null },
                title = { androidx.compose.material3.Text("Se detectó un error") },
                text = { androidx.compose.material3.Text("La app se cerró inesperadamente la última vez. ¿Deseas enviar el reporte a soporte?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        val traza = reportePendiente ?: ""
                        reportePendiente = null
                        com.example.util.ReporteErrores.limpiar(context)
                        val u = viewModel.currentUser.value
                        val codigo = u?.companyCode?.ifBlank { ultimoCodigo } ?: ultimoCodigo ?: ""
                        if (codigo.isNotBlank()) {
                            viewModel.enviarSoporte(codigo, "Error de aplicación", traza.take(2000))
                        }
                    }) { androidx.compose.material3.Text("Enviar reporte") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        reportePendiente = null
                        com.example.util.ReporteErrores.limpiar(context)
                    }) { androidx.compose.material3.Text("Descartar") }
                }
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
        // Screen 1: Redirection Login (Country + Code input)
        composable("redirection") {
            RedirectionLoginScreen(
                viewModel = viewModel,
                onNavigateToAdminLogin = {
                    navController.navigate("admin_login")
                },
                onNavigateToCompanyLogin = { code ->
                    navController.navigate("company_login/$code")
                }
            )
        }

        // Screen 2: Super Admin Login (AdminMauricio@kaptaia.com / M4ur1C10*)
        composable("admin_login") {
            SuperAdminLoginScreen(
                onLoginSuccess = {
                    viewModel.setSuperAdminSession(true)
                    viewModel.setCurrentUser(
                        com.example.data.local.entity.CompanyUserEntity(
                            companyId = 0,
                            companyCode = "",
                            name = "Brayam (SuperAdmin)",
                            role = "SuperAdmin",
                            email = "AdminMauricio@kaptaia.com",
                            username = "superadmin",
                            password = ""
                        )
                    )
                    viewModel.selectCompany(null)
                    viewModel.selectRemoteCompany(null)
                    navController.navigate("admin_dashboard") {
                        popUpTo("redirection") { inclusive = true }
                    }
                },
                onBackToRedirection = {
                    viewModel.logout()
                    navController.popBackStack()
                }
            )
        }

        // Screen 3: Submenu Login for Company
        composable(
            route = "company_login/{companyCode}",
            arguments = listOf(navArgument("companyCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("companyCode") ?: "la14"
            CompanyLoginScreen(
                viewModel = viewModel,
                companyCode = code,
                onLoginToPosSuccess = { company ->
                    viewModel.selectCompany(company)
                    viewModel.setSuperAdminSession(false)
                    // Consentimiento legal: usuarios nuevos (o sin registro) lo aceptan antes de entrar.
                    val u = viewModel.currentUser.value
                    val userKey = com.example.ui.screens.claveUsuarioConsentimiento(
                        u?.name ?: "", u?.email ?: "", u?.username ?: ""
                    )
                    val ctx = navController.context
                    if (com.example.ui.screens.tieneConsentimiento(ctx, company.code, userKey)) {
                        navController.navigate("tenant_pos")
                    } else {
                        navController.navigate("consentimiento/${company.code}")
                    }
                },
                onBackToRedirection = {
                    navController.navigate("redirection")
                }
            )
        }

        // Screen 3b: Consentimiento legal (solo primera vez por usuario+negocio)
        composable(
            route = "consentimiento/{companyCode}",
            arguments = listOf(navArgument("companyCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("companyCode") ?: ""
            val comp = viewModel.companies.value.find { it.code.equals(code, ignoreCase = true) }
            val u = viewModel.currentUser.value
            com.example.ui.screens.ConsentimientoScreen(
                nombreNegocio = comp?.name ?: code,
                nombreUsuario = u?.name ?: "usuario",
                onAceptar = {
                    val ctx = navController.context
                    com.example.ui.screens.guardarConsentimiento(
                        ctx, code,
                        com.example.ui.screens.claveUsuarioConsentimiento(u?.name ?: "", u?.email ?: "", u?.username ?: "")
                    )
                    navController.navigate("tenant_pos") {
                        popUpTo("consentimiento/$code") { inclusive = true }
                    }
                }
            )
        }

        // Screen 4: Super Admin Dashboard
        composable("admin_dashboard") {
            SuperAdminDashboardScreen(
                viewModel = viewModel,
                onNavigateToCreateCompany = {
                    viewModel.selectCompany(null)
                    navController.navigate("create_company")
                },
                onNavigateToEditCompany = { company ->
                    viewModel.selectCompany(company)
                    navController.navigate("edit_company")
                },
                onNavigateToCompanyDetail = { company ->
                    viewModel.selectCompany(company)
                    navController.navigate("company_detail")
                },
                onNavigateToAdministrators = {
                    navController.navigate("administrators")
                },
                onNavigateToMemberships = {
                    navController.navigate("plans")
                },
                onNavigateToPlans = {
                    navController.navigate("plans")
                },
                onNavigateToReports = {
                    // Navigate to dashboard tab
                },
                onEnterAsAdmin = { company ->
                    viewModel.selectCompany(company)
                    viewModel.setSuperAdminSession(true)
                    viewModel.setCurrentUser(
                        com.example.data.local.entity.CompanyUserEntity(
                            companyId = company.id,
                            companyCode = company.code,
                            name = company.adminName.ifBlank { "SuperAdmin (Soporte)" },
                            role = "Administrador",
                            email = company.adminEmail.ifBlank { "admin@${company.code}.com" },
                            username = "superadmin_shadow",
                            password = ""
                        )
                    )
                    navController.navigate("tenant_pos")
                },
                onLogoutToRedirection = {
                    viewModel.logout()
                    navController.navigate("redirection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Screen 5: Create Company Form
        composable("create_company") {
            CreateEditCompanyScreen(
                viewModel = viewModel,
                companyToEdit = null,
                onBack = { navController.popBackStack() }
            )
        }

        // Screen 6: Edit Company Form
        composable("edit_company") {
            val selectedComp by viewModel.selectedCompany.collectAsState()
            CreateEditCompanyScreen(
                viewModel = viewModel,
                companyToEdit = selectedComp,
                onBack = { navController.popBackStack() }
            )
        }

        // Screen 7: Company Detail Screen
        composable("company_detail") {
            val selectedComp by viewModel.selectedCompany.collectAsState()
            selectedComp?.let { comp ->
                CompanyDetailScreen(
                    viewModel = viewModel,
                    company = comp,
                    onBack = { navController.popBackStack() },
                    onNavigateToEdit = {
                        navController.navigate("edit_company")
                    },
                    onEnterAsAdmin = {
                        viewModel.setSuperAdminSession(true)
                        selectedComp?.let { comp ->
                            viewModel.setCurrentUser(
                                com.example.data.local.entity.CompanyUserEntity(
                                    companyId = comp.id,
                                    companyCode = comp.code,
                                    name = comp.adminName.ifBlank { "SuperAdmin (Soporte)" },
                                    role = "Administrador",
                                    email = comp.adminEmail.ifBlank { "admin@${comp.code}.com" },
                                    username = "superadmin_shadow",
                                    password = ""
                                )
                            )
                        }
                        navController.navigate("tenant_pos")
                    }
                )
            }
        }

        // Screen 8: Administrators Screen
        composable("administrators") {
            AdministratorsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Screen 9: Plans & Memberships Screen
        composable("plans") {
            PlansMembershipsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCreateCompany = {
                    navController.navigate("create_company")
                }
            )
        }

        // Screen 10: Tenant POS Screen
        composable("tenant_pos") {
            val selectedComp by viewModel.selectedCompany.collectAsState()
            selectedComp?.let { comp ->
                BusinessTheme(company = comp) {
                    TenantPosScreen(
                        viewModel = viewModel,
                        company = comp,
                        onExitPos = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
}
}


