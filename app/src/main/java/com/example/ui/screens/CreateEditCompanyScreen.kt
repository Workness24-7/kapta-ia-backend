package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.FuncionLib
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSButton
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.KaptaLogoHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class ModuleDefinition(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val glowColor: Color,
    val isDefault: Boolean = false,
    val minPlan: String = "Básico"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditCompanyScreen(
    viewModel: KaptaViewModel,
    companyToEdit: CompanyEntity? = null,
    onBack: () -> Unit
) {
    val isEditMode = companyToEdit != null
    val isSaving by viewModel.isSavingCompany.collectAsState()
    val scope = rememberCoroutineScope()

    // Theme colors captured for use inside non-composable remember/catch blocks
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeOnSurface = MaterialTheme.colorScheme.onSurface

    // Form inputs state
    var companyName by remember { mutableStateOf(companyToEdit?.name ?: "") }
    var accessCode by remember { mutableStateOf(companyToEdit?.code ?: "") }
    var logoUri by remember { mutableStateOf<String?>(companyToEdit?.logoUrl.takeIf { !it.isNullOrBlank() }) }
    var listIconUri by remember { mutableStateOf<String?>(companyToEdit?.listIconUrl.takeIf { !it.isNullOrBlank() }) }
    var country by remember { mutableStateOf(companyToEdit?.country ?: "Colombia") }
    var nit by remember { mutableStateOf(companyToEdit?.nit ?: "") }
    var city by remember { mutableStateOf(companyToEdit?.city ?: "") }
    var address by remember { mutableStateOf(companyToEdit?.address ?: "") }
    var phone1 by remember { mutableStateOf(companyToEdit?.phone ?: "") }
    var phone2 by remember { mutableStateOf(companyToEdit?.phone2 ?: "") }
    var email by remember { mutableStateOf(companyToEdit?.email ?: "") }
    var businessType by remember { mutableStateOf(companyToEdit?.businessType ?: "🍸 Bar") }
    var businessTypeOtro by remember { mutableStateOf(if (companyToEdit != null && businessType == "Otro") companyToEdit.businessType else "") }
    val yaTieneFunciones = companyToEdit?.customFunctionsJson?.isNotBlank() == true
    val tipoExistente = companyToEdit?.businessType?.takeIf { it.isNotBlank() && it != "Otro" }
    val initialStatus = remember(companyToEdit) {
        companyToEdit?.status?.replace(Regex("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ]"), "")?.trim()?.takeIf { it.isNotBlank() } ?: "Activo"
    }
    val initialPlan = remember(companyToEdit) {
        companyToEdit?.plan?.replace(Regex("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ]"), "")?.trim()?.takeIf { it.isNotBlank() } ?: "Básico"
    }
    var status by remember { mutableStateOf(initialStatus) }
    var plan by remember { mutableStateOf(initialPlan) }
    var durationTime by remember { mutableStateOf(companyToEdit?.durationTime ?: "Mensual") }
    val todayFormatted = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val creationDate = remember { companyToEdit?.creationDate ?: todayFormatted }
    val lastAccess = remember { companyToEdit?.lastAccess ?: "Aún no tiene acceso" }
    var adminName by remember { mutableStateOf(companyToEdit?.adminName ?: "") }
    var adminEmail by remember { mutableStateOf(companyToEdit?.adminEmail ?: "") }
    var adminPass by remember { mutableStateOf("") }
    var showAdminPass by remember { mutableStateOf(false) }
    // Dropdown visibility
    var statusExpanded by remember { mutableStateOf(false) }
    var planExpanded by remember { mutableStateOf(false) }
    var durationExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }
    var businessTypeExpanded by remember { mutableStateOf(false) }
    val countryList = remember {
        listOf("Colombia", "México", "Chile", "Perú", "Argentina", "Ecuador")
    }
    val durationList = remember {
        listOf("15 días", "1 mes", "Mensual", "Anual", "Permanente")
    }
    val businessTypesList = remember {
        listOf(
            "Bar",
            "Restaurante",
            "Cafetería",
            "Tienda de barrio",
            "Carnicería",
            "Tienda de mascotas",
            "Tienda de cosméticos",
            "Ferretería",
            "Licorería",
            "Otro"
        )
    }
    // Logo image launchers
    val context = LocalContext.current
    val logoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { logoUri = com.example.util.PhotoStore.save(context, it) ?: it.toString() }
    }
    val listIconPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { listIconUri = com.example.util.PhotoStore.save(context, it) ?: it.toString() }
    }
    // All available system modules
    val allSystemModules = remember {
        listOf(
            ModuleDefinition("Inventario", "Gestiona productos, categorías, stock y más.", Icons.Outlined.Inventory2, Color(0xFF34C759), isDefault = true, minPlan = "Básico"),
            ModuleDefinition("Registrar venta", "Realiza ventas de productos o servicios.", Icons.Outlined.ShoppingCart, themePrimary, isDefault = true, minPlan = "Básico"),
            ModuleDefinition("Cuentas por cobrar", "Controla clientes y cuentas pendientes.", Icons.Outlined.AccountBalanceWallet, Color(0xFFFF9F0A), isDefault = true, minPlan = "Premium"),
            ModuleDefinition("Módulo de reportes", "Visualiza reportes y estadísticas.", Icons.Outlined.PieChart, themePrimary, isDefault = true, minPlan = "Básico"),
            ModuleDefinition("Hacer inventario", "Realiza conteos y ajustes de inventario.", Icons.Outlined.Assignment, Color(0xFF34C759), isDefault = true, minPlan = "Básico"),
            ModuleDefinition("Facturación Electrónica", "Emisión de facturas y notas crédito DIAN.", Icons.Outlined.ReceiptLong, Color(0xFF06B6D4), minPlan = "Premium"),
            ModuleDefinition("Sucursales Sincronizadas", "Sincronización multi-sucursal en tiempo real.", Icons.Outlined.Store, themePrimary, minPlan = "MAX IA"),
            ModuleDefinition("Comandero / Mesas", "Atención de mesas y cocina para restaurantes.", Icons.Outlined.Restaurant, Color(0xFFF97316), minPlan = "Premium"),
            ModuleDefinition("Control de Turnos", "Apertura, arqueos y cierres de caja por turno.", Icons.Outlined.PointOfSale, Color(0xFF14B8A6), minPlan = "Básico"),
            ModuleDefinition("Análisis Predictivo", "Predicciones de ventas e inventarios con IA.", Icons.Outlined.ShowChart, Color(0xFFF43F5E), minPlan = "MAX IA"),
            ModuleDefinition("Pagos Recurrentes", "Gestión de suscripciones y cobros periódicos.", Icons.Outlined.Repeat, Color(0xFFA855F7), minPlan = "Premium")
        )
    }
    // Map of enabled modules
    var enabledModulesMap by remember {
        mutableStateOf(
            allSystemModules.associate { mod ->
                mod.title to (mod.isDefault && (mod.minPlan == "Básico" || plan != "Básico"))
            }.toMutableMap()
        )
    }
    val activeMatrix = remember(businessType) { getMatrixForBusinessType(businessType) }
    // Synchronize business type & plan matrix with enabled modules map
    LaunchedEffect(businessType, plan, durationTime) {
        val isPruebaState = durationTime == "15 días" || durationTime == "1 mes" || status.contains("Prueba", ignoreCase = true)
        if (isPruebaState) {
            plan = "MAX IA"
            status = "Prueba"
        }
        val matrix = getMatrixForBusinessType(businessType)
        val isBasic = plan.contains("Básico")
        val isPremium = plan.contains("Premium")
        val isMaxIa = plan.contains("MAX") || isPruebaState
        val updatedMap = enabledModulesMap.toMutableMap()
        // 1. Basic features: enabled for all plans
        matrix.basicFeatures.forEach { feat ->
            updatedMap[feat] = true
        }
        // 2. Premium features: enabled for Premium and MAX IA, disabled for Básico
        matrix.premiumFeatures.forEach { feat ->
            updatedMap[feat] = isPremium || isMaxIa
        }
        // 3. MAX IA features: enabled ONLY for MAX IA, disabled for Básico and Premium
        matrix.maxIaFeatures.forEach { feat ->
            updatedMap[feat] = isMaxIa
        }
        enabledModulesMap = updatedMap
    }
    // Other modules sheet state
    var showOtherModulesSheet by remember { mutableStateOf(false) }
    // Custom AI functions
    var aiFuncionNombre by remember { mutableStateOf("") }
    var aiFuncionDesc by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var isGeneratingTipo by remember { mutableStateOf(false) }
    val aiFunctionsList by viewModel.createdAiFunctions.collectAsState()
    val functionLibrary by viewModel.functionLibrary.collectAsState()
    LaunchedEffect(companyToEdit?.id) {
        viewModel.loadFunctionLibrary()
        viewModel.setCustomAiFunctionsFromCompany(companyToEdit?.customFunctionsJson)
    }
    // Login Preview & Advanced Customization Sheet
    var showCustomizationSheet by remember { mutableStateOf(false) }
    var primaryColorHex by remember { mutableStateOf(companyToEdit?.primaryColorHex ?: "#4F46E5") }
    var secondaryColorHex by remember { mutableStateOf(companyToEdit?.secondaryColorHex ?: "#3B82F6") }
    var tertiaryColorHex by remember { mutableStateOf(companyToEdit?.tertiaryColorHex ?: "#10B981") }
    var neutralColorHex by remember { mutableStateOf(companyToEdit?.neutralColorHex ?: "#0F172A") }
    var welcomeTitle by remember { mutableStateOf("¡Bienvenido!") }
    var activeColorRole by remember { mutableStateOf("Primario") } // "Primario", "Secundario", "Terciario", "Neutral"
    var isExtractingPalette by remember { mutableStateOf(false) }
    var paletteExtractedFromLogo by remember { mutableStateOf(false) }
    LaunchedEffect(logoUri) {
        val currentUri = logoUri
        if (!currentUri.isNullOrBlank()) {
            isExtractingPalette = true
            val palette = extractBrandPaletteFromLogo(context, currentUri)
            if (palette != null) {
                primaryColorHex = palette.primaryHex
                secondaryColorHex = palette.secondaryHex
                tertiaryColorHex = palette.tertiaryHex
                neutralColorHex = palette.neutralHex
                paletteExtractedFromLogo = true
            } else {
                paletteExtractedFromLogo = false
            }
            isExtractingPalette = false
        } else {
            paletteExtractedFromLogo = false
            if (!isEditMode) {
                primaryColorHex = "#4F46E5"
                secondaryColorHex = "#3B82F6"
                tertiaryColorHex = "#10B981"
                neutralColorHex = "#0F172A"
            }
        }
    }
    val primaryColorParsed = remember(primaryColorHex) {
        try { Color(android.graphics.Color.parseColor(primaryColorHex)) }
        catch (e: Exception) { themePrimary }
    }
    val secondaryColorParsed = remember(secondaryColorHex) {
        try { Color(android.graphics.Color.parseColor(secondaryColorHex)) }
        catch (e: Exception) { themePrimary }
    }
    val tertiaryColorParsed = remember(tertiaryColorHex) {
        try { Color(android.graphics.Color.parseColor(tertiaryColorHex)) }
        catch (e: Exception) { Color(0xFF34C759) }
    }
    val neutralColorParsed = remember(neutralColorHex) {
        try { Color(android.graphics.Color.parseColor(neutralColorHex)) }
        catch (e: Exception) { themeOnSurface }
    }
    val previewFontFamily = androidx.compose.ui.text.font.FontFamily.Default
    val compactTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )
    EtherealBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. App Bar Flotante (Glass style iOS)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.White.copy(alpha = 0.35f),
                borderColor = Color.White.copy(alpha = 0.80f),
                borderWidth = 1.2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón volver
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.50f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f)),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // Logo KAPTA IA
                    KaptaLogoHeader(fontSize = 20, showSlogan = false)
                    // Botón Cancelar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.40f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.60f)),
                        modifier = Modifier.clickable { onBack() }
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Título principal
            iOSLargeTitle(
                title = if (isEditMode) "Editar empresa" else "Crear nueva empresa",
                subtitle = "Completa la información para registrar un nuevo negocio en la plataforma."
            )
            Spacer(modifier = Modifier.height(18.dp))
            // 2. Sección 1: Información de la empresa
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Información de la empresa",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // 1. Doble icono/imagen del negocio
                    Text(
                        text = "Imágenes del negocio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Caja 1: Logo para Pantalla de Login
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                                .border(1.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(16.dp))
                                .clickable { logoPickerLauncher.launch("image/*") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoUri != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = logoUri,
                                        contentDescription = "Logo Login",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, Color.White, RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Logo Login ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                                    Text("Pantalla de acceso", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Logo Login", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Para pantalla acceso", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        // Caja 2: Miniatura/Icono para Lista de Empresas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                                .border(1.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(16.dp))
                                .clickable { listIconPickerLauncher.launch("image/*") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (listIconUri != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = listIconUri,
                                        contentDescription = "Icono Lista",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Icono Lista ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                                    Text("Miniatura general", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Outlined.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Icono Lista", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Miniatura en lista", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    // Status banner de Extracción Automática de Colores desde el Logo
                    if (isExtractingPalette) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                            modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Analizando logo y extrayendo colores automáticamente...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (paletteExtractedFromLogo) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = primaryColorParsed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, primaryColorParsed.copy(alpha = 0.40f)),
                            modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = primaryColorParsed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Colores extraídos del logo automáticamente",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColorParsed
                                        )
                                        Text(
                                            text = "Primario, secundario, terciario y neutral asignados",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primaryColorParsed,
                                    modifier = Modifier.clickable { showCustomizationSheet = true }
                                ) {
                                    Text(
                                        text = "Ajustar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    } else if (logoUri == null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.40f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sin logo: Estado por defecto neutral. Carga una imagen para extraer su paleta automáticamente.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Nombre de la empresa *
                    Text(text = "Nombre de la empresa *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = {
                            companyName = it
                            if (accessCode.isBlank() || accessCode == companyName.lowercase().replace(" ", "").dropLast(1)) {
                                accessCode = it.lowercase().replace(" ", "").replace("[^a-zA-Z0-9]".toRegex(), "")
                            }
                        },
                        placeholder = { Text("Ej. Bar El Sabor", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                        singleLine = true,
                        colors = compactTextFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // 1.2. Código de acceso al login *
                    Text(text = "Código de acceso al login *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Código con el que la empresa y sus usuarios ingresarán al sistema", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = accessCode,
                        onValueChange = { accessCode = it.lowercase().replace(" ", "") },
                        placeholder = { Text("Ej. barelsabor", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        },
                        colors = compactTextFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // 1.3. Tipo de negocio *
                    Text(text = "Tipo de negocio *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Selecciona el rubro o categoría comercial del establecimiento", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = businessType,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Ej. Bar / Restaurante", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            },
                            colors = compactTextFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { businessTypeExpanded = true }
                        )
                        DropdownMenu(
                            expanded = businessTypeExpanded,
                            onDismissRequest = { businessTypeExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            businessTypesList.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        if (type == "Otro" && yaTieneFunciones && tipoExistente != null) {
                                            businessType = tipoExistente
                                            viewModel.showToast("Este negocio ya tiene funciones; se mantiene el tipo '$tipoExistente'")
                                        } else {
                                            businessType = type
                                        }
                                        businessTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (businessType == "Otro") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = businessTypeOtro,
                            onValueChange = { businessTypeOtro = it },
                            placeholder = { Text("Describe tu tipo de negocio (ej. Taller de motos)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            colors = compactTextFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val tipo = businessTypeOtro.trim()
                                if (tipo.isNotBlank() && !yaTieneFunciones) {
                                    isGeneratingTipo = true
                                    scope.launch {
                                        try {
                                            val funcs = viewModel.generarFuncionesPorTipoNegocio(tipo)
                                            funcs.forEach { f ->
                                                viewModel.addFunctionToLibrary(f.copy(tipoNegocio = tipo))
                                                viewModel.addCustomAiFunction(f.nombre, f.descripcion)
                                            }
                                        } finally { isGeneratingTipo = false }
                                    }
                                }
                            },
                            enabled = !yaTieneFunciones && businessTypeOtro.isNotBlank() && !isGeneratingTipo,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(
                                when {
                                    yaTieneFunciones -> "Este negocio ya tiene funciones (se reutilizan)"
                                    isGeneratingTipo -> "Generando..."
                                    else -> "Generar funciones para este tipo de negocio"
                                }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 2. País y Ciudad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dropdown País
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "País *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = country,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    },
                                    colors = compactTextFieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { countryExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = countryExpanded,
                                    onDismissRequest = { countryExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    countryList.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                country = c
                                                countryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Ciudad
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Ciudad *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                placeholder = { Text("Ciudad, Departamento (Ej. Cali, Valle del Cauca)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                singleLine = true,
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Nit (opcional) y Dirección *
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Nit (opcional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = nit,
                                onValueChange = { nit = it },
                                placeholder = { Text("Ej. 9001123456-7", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                singleLine = true,
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Dirección *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                placeholder = { Text("Ej. Calle 10 # 5-30", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                singleLine = true,
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Celular 1 (Obligatorio) y Celular 2 (Opcional)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Celular 1 (Obligatorio) *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone1,
                                onValueChange = { phone1 = it },
                                placeholder = { Text("Ej. 300 123 4567", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "W", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Celular 2 (Opcional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone2,
                                onValueChange = { phone2 = it },
                                placeholder = { Text("Ej. 310 987 6543", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                singleLine = true,
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Correo electrónico
                    Text(text = "Correo electrónico *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Ej. contacto@barsabor.com", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                        singleLine = true,
                        colors = compactTextFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // 3. Estado y Plan Desplegables con reglas de negocio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Estado Dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Estado *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val statusDotColor = when {
                                    status.contains("Activ") -> Color(0xFF34C759)
                                    status.contains("Vencer") -> Color(0xFFFF9F0A)
                                    status.contains("Suspend") -> Color(0xFFFF453A)
                                    else -> MaterialTheme.colorScheme.primary // Prueba
                                }
                                OutlinedTextField(
                                    value = status,
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(statusDotColor)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    },
                                    colors = compactTextFieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { statusExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = statusExpanded,
                                    onDismissRequest = { statusExpanded = false },
                                    modifier = Modifier
                                        .shadow(14.dp, RoundedCornerShape(18.dp))
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Activo", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            status = "Activo"
                                            statusExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Por Vencer", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            status = "Por Vencer"
                                            statusExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Suspendido", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            status = "Suspendido"
                                            statusExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Prueba (15 días - MAX IA)", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            status = "Prueba"
                                            durationTime = "15 días"
                                            plan = "MAX IA"
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        // Plan Dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Plan *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = plan,
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (plan.contains("MAX", ignoreCase = true)) MaterialTheme.colorScheme.primary else if (plan.contains("Premium", ignoreCase = true)) MaterialTheme.colorScheme.primary else Color(0xFFFF9F0A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    },
                                    colors = compactTextFieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (durationTime != "15 días" && durationTime != "1 mes" && !status.contains("Prueba", ignoreCase = true)) {
                                                planExpanded = true
                                            }
                                        }
                                )
                                DropdownMenu(
                                    expanded = planExpanded,
                                    onDismissRequest = { planExpanded = false },
                                    modifier = Modifier
                                        .shadow(14.dp, RoundedCornerShape(18.dp))
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Básico", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        leadingIcon = { if (plan == "Básico") Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            plan = "Básico"
                                            planExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Premium", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        leadingIcon = { if (plan == "Premium") Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            plan = "Premium"
                                            planExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("MAX IA", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                        leadingIcon = { if (plan == "MAX IA") Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            plan = "MAX IA"
                                            planExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 4. Campo "Tiempo" (Debajo de Estado y Plan)
                    Text(text = "Tiempo *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = durationTime,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            },
                            colors = compactTextFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { durationExpanded = true }
                        )
                        DropdownMenu(
                            expanded = durationExpanded,
                            onDismissRequest = { durationExpanded = false },
                            modifier = Modifier
                                .shadow(14.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            durationList.forEach { dur ->
                                val isDurPrueba = dur == "15 días" || dur == "1 mes"
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isDurPrueba) "$dur (Prueba - MAX IA)" else dur,
                                            fontSize = 13.sp,
                                            fontWeight = if (isDurPrueba) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDurPrueba) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        durationTime = dur
                                        durationExpanded = false
                                        if (isDurPrueba) {
                                            plan = "MAX IA"
                                            status = "Prueba"
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (durationTime == "15 días" || durationTime == "1 mes") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Duración automática de 15 días de uso asignada exclusivamente con Plan MAX IA.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    // Card explicativa de los beneficios del plan seleccionado
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Beneficios incluidos en Plan $plan:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val benefitsList = when {
                                plan.contains("Básico") -> listOf(
                                    "• 1 Usuario de acceso",
                                    "• Control de inventario estándar (Máx. 40 productos)",
                                    "• Gestión de ventas, recibos y reporte diario básico",
                                    "• Soporte vía correo electrónico"
                                )
                                plan.contains("Premium") -> listOf(
                                    "• 5 Usuarios de acceso",
                                    "• Funciones de Inteligencia Artificial KAPTA IA",
                                    "• Catálogo de productos ilimitado",
                                    "• Reportes financieros detallados + Cuentas por cobrar",
                                    "• Soporte prioritario por WhatsApp"
                                )
                                else -> listOf(
                                    "• 15 Usuarios de acceso + Sincronización multi-sucursal",
                                    "• IA Avanzada con Generador de Funciones por Prompt",
                                    "• Análisis predictivo de ventas y demanda",
                                    "• Soporte 24/7 dedicado"
                                )
                            }
                            benefitsList.forEach { b ->
                                Text(text = b, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 4. Fecha de creación y Último acceso (Bloqueados)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Fecha de creación (Bloqueado)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = creationDate,
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                },
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Último acceso", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = lastAccess,
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                },
                                colors = compactTextFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // 5. Administrador - Edición completa de datos de Administrador
                    Text(text = "Administrador del Negocio *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val initials = adminName.trim().split(" ")
                                                .mapNotNull { it.firstOrNull()?.toString() }
                                                .take(2).joinToString("").ifEmpty { "AD" }
                                            Text(text = initials, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (adminName.isBlank()) "Sin asignar" else adminName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (adminEmail.isBlank()) "Escribe el correo del admin" else adminEmail,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    viewModel.copyCredentials(
                                        CompanyEntity(
                                            code = accessCode,
                                            name = companyName,
                                            adminName = adminName,
                                            adminEmail = adminEmail,
                                            email = email
                                        )
                                    )
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                            // Campos editables del admin (Sin label recortado)
                            Column {
                                Text(text = "Nombre Completo del Admin *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = adminName,
                                    onValueChange = { adminName = it },
                                    placeholder = { Text("Ej. Carlos Pérez", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = compactTextFieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                            Column {
                                Text(text = "Correo del Admin *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = adminEmail,
                                    onValueChange = { adminEmail = it },
                                    placeholder = { Text("Ej. admin@negocio.com", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = compactTextFieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                            Column {
                                Text(text = "Contraseña del Admin *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = adminPass,
                                    onValueChange = { adminPass = it },
                                    placeholder = { Text("Escribe una contraseña", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                                    singleLine = true,
                                    visualTransformation = if (showAdminPass) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showAdminPass = !showAdminPass }) {
                                            Icon(
                                                imageVector = if (showAdminPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    colors = compactTextFieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            // 6. Sección 2: Funciones y Módulos
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Funciones y Módulos",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Asignación automática según el rubro y plan activo.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                            modifier = Modifier.clickable { showOtherModulesSheet = true }
                        ) {
                            Text(
                                text = "+ Módulos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Matriz activa para $businessType • Plan $plan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val isBasicPlan = plan.contains("Básico")
                    val isPremiumPlan = plan.contains("Premium")
                    val isMaxIaPlan = plan.contains("MAX") || plan.contains("Prueba") || durationTime == "15 días" || durationTime == "1 mes"
                    // Group 1: Funciones Básico (✅ / ✅ / ✅) — visibles siempre
                    Text(
                        text = "📦 Funciones Incluidas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    activeMatrix.basicFeatures.forEach { featureName ->
                        FeatureMatrixItemRow(
                            featureName = featureName,
                            statusText = "✅ Incluido en $plan",
                            statusBg = Color(0xFF34C759).copy(alpha = 0.12f),
                            statusTextColor = Color(0xFF34C759),
                            isChecked = enabledModulesMap[featureName] ?: true,
                            isEnabled = true,
                            onCheckedChange = { checked ->
                                enabledModulesMap = enabledModulesMap.toMutableMap().apply { put(featureName, checked) }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Group 2: Funciones Avanzadas — solo Plan Premium y MAX IA
                    if (isPremiumPlan || isMaxIaPlan) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🚀 Funciones Avanzadas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        activeMatrix.premiumFeatures.forEach { featureName ->
                            FeatureMatrixItemRow(
                                featureName = featureName,
                                statusText = "✅ Incluido en $plan",
                                statusBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                statusTextColor = MaterialTheme.colorScheme.primary,
                                isChecked = enabledModulesMap[featureName] ?: true,
                                isEnabled = true,
                                onCheckedChange = { checked ->
                                    enabledModulesMap = enabledModulesMap.toMutableMap().apply { put(featureName, checked) }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    // Group 3: Funciones Exclusivas MAX IA — solo Plan MAX IA
                    if (isMaxIaPlan) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🤖 Funciones Inteligentes Exclusivas (Plan MAX IA)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        activeMatrix.maxIaFeatures.forEach { featureName ->
                            FeatureMatrixItemRow(
                                featureName = featureName,
                                statusText = "✨ Activo con IA",
                                statusBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                statusTextColor = MaterialTheme.colorScheme.primary,
                                isChecked = enabledModulesMap[featureName] ?: true,
                                isEnabled = true,
                                onCheckedChange = { checked ->
                                    enabledModulesMap = enabledModulesMap.toMutableMap().apply { put(featureName, checked) }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Botón catálogo completo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                            .border(1.dp, Color.White.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                            .clickable { showOtherModulesSheet = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Ver o personalizar catálogo completo de módulos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (plan.contains("MAX") || plan.contains("Prueba") || durationTime == "15 días" || durationTime == "1 mes") {
            Spacer(modifier = Modifier.height(20.dp))
            // 7. Sección 3: Funciones personalizadas con IA
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Funciones personalizadas con IA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "IA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Crea funciones personalizadas y automatiza procesos únicos para este negocio.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    run {
                        // Prompter de IA habilitado
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .border(1.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = aiFuncionNombre,
                                        onValueChange = { aiFuncionNombre = it },
                                        placeholder = { Text("Nombre de la función *", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = aiFuncionDesc,
                                        onValueChange = { aiFuncionDesc = it },
                                        placeholder = { Text("Describe qué hace la función...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                                            )
                                        )
                                        .clickable {
                                            if (aiFuncionNombre.isNotBlank() && aiFuncionDesc.isNotBlank() && !isGeneratingAi) {
                                                isGeneratingAi = true
                                                val nombre = aiFuncionNombre.trim()
                                                val desc = aiFuncionDesc.trim()
                                                scope.launch {
                                                    try {
                                                        val funcs = viewModel.generarFuncionesConIA("Nombre: $nombre. Descripción: $desc")
                                                        if (funcs.isEmpty()) {
                                                            viewModel.addFunctionToLibrary(FuncionLib(nombre, desc, planTier = "Básico", modulo = "General"))
                                                            viewModel.addCustomAiFunction(nombre, desc)
                                                        } else {
                                                            funcs.forEach {
                                                                viewModel.addFunctionToLibrary(it.copy(modulo = it.modulo.ifBlank { "General" }, planTier = it.planTier.ifBlank { "Básico" }))
                                                                viewModel.addCustomAiFunction(it.nombre, it.descripcion)
                                                            }
                                                        }
                                                    } finally {
                                                        isGeneratingAi = false
                                                        aiFuncionNombre = ""
                                                        aiFuncionDesc = ""
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (isGeneratingAi) "Generando..." else "+ Crear función con IA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        // Biblioteca global de funciones IA (SuperAdmin)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Biblioteca de funciones (${functionLibrary.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Creadas por SuperAdmin. Activa las que este negocio puede usar; el admin del negocio las asigna a sus usuarios.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (functionLibrary.isEmpty()) {
                                Text(
                                    text = "Aún no hay funciones en la biblioteca. Usa el campo de arriba para generar la primera con IA.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                functionLibrary.forEach { (nombre, desc) ->
                                    val activa = aiFunctionsList.any { it.first == nombre }
                                    val rol = rolDeDescripcion(desc)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = activa,
                                            onCheckedChange = { viewModel.toggleCustomAiFunction(nombre, desc) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            if (rol.isNotBlank()) Text(text = "Rol: $rol", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = "Eliminar",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFF453A),
                                            modifier = Modifier.clickable {
                                                scope.launch {
                                                    if (activa) viewModel.toggleCustomAiFunction(nombre, desc)
                                                    viewModel.removeFunctionFromLibrary(nombre)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            Spacer(modifier = Modifier.height(20.dp))
            // 8. Sección 4: Preview del negocio & Personalización Avanzada
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Preview del negocio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = previewFontFamily,
                                color = neutralColorParsed
                            )
                            Text(
                                text = "Personalización en tiempo real de marca",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = primaryColorParsed.copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, primaryColorParsed),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { showCustomizationSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Personalización avanzada de paleta y tipografía",
                                    tint = primaryColorParsed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Así lucirá la pantalla de inicio de sesión exclusiva para el negocio '${if (companyName.isBlank()) "Bar El Sabor" else companyName}' usando el código '${if (accessCode.isBlank()) "barelsabor" else accessCode}'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Dynamic Real-time Business Login Card Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(primaryColorParsed, secondaryColorParsed, tertiaryColorParsed)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = primaryColorParsed.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, primaryColorParsed.copy(alpha = 0.40f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(tertiaryColorParsed)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Código: ${if (accessCode.isBlank()) "barelsabor" else accessCode}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = previewFontFamily,
                                            color = primaryColorParsed
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = secondaryColorParsed.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, secondaryColorParsed.copy(alpha = 0.30f))
                                ) {
                                    Text(
                                        text = "KAPTA IA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = previewFontFamily,
                                        color = secondaryColorParsed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            // Business Avatar / Logo
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryColorParsed.copy(alpha = 0.25f),
                                                tertiaryColorParsed.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                                    .border(2.dp, primaryColorParsed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoUri != null) {
                                    AsyncImage(
                                        model = logoUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = if (companyName.isNotBlank()) companyName.take(1).uppercase() else "K",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = previewFontFamily,
                                        color = primaryColorParsed
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (companyName.isBlank()) "Bar El Sabor" else companyName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = previewFontFamily,
                                color = neutralColorParsed
                            )
                            Text(
                                text = "${if (accessCode.isBlank()) "barelsabor" else accessCode}.kaptaia.com",
                                fontSize = 11.sp,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            // Mini Input Fields
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = primaryColorParsed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Usuario administrador", fontSize = 11.sp, fontFamily = previewFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = primaryColorParsed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "••••••••••••", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            // Primary Action Button Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(primaryColorParsed, secondaryColorParsed)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Iniciar Sesión en ${if (companyName.isBlank()) "el negocio" else companyName}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = previewFontFamily
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Paleta visual activa chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Paleta Activa:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(primaryColorParsed).border(1.dp, Color.White, CircleShape))
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(secondaryColorParsed).border(1.dp, Color.White, CircleShape))
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(tertiaryColorParsed).border(1.dp, Color.White, CircleShape))
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(neutralColorParsed).border(1.dp, Color.White, CircleShape))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            var formError by remember { mutableStateOf<String?>(null) }
            if (formError != null) {
                Text(
                    text = formError ?: "",
                    color = Color(0xFFFF453A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            // 9. Botones Principales de Acción (Pie de página)
            Button(
                enabled = !isSaving,
                onClick = {
                    val faltan = mutableListOf<String>()
                    if (companyName.isBlank()) faltan.add("Nombre del negocio")
                    if (country.isBlank()) faltan.add("País")
                    if (plan.isBlank()) faltan.add("Plan")
                    if (adminEmail.isBlank()) faltan.add("Correo del administrador")
                    if (!isEditMode && adminPass.isBlank()) faltan.add("Contraseña del administrador")
                    if (faltan.isNotEmpty()) {
                        formError = "Campos obligatorios: " + faltan.joinToString(", ")
                        return@Button
                    }
                    formError = null
                    scope.launch {
                        val logoUriVal = logoUri
                        val listIconUriVal = listIconUri
                        val prevLogo = companyToEdit?.logoUrl ?: ""
                        val prevIcon = companyToEdit?.listIconUrl ?: ""
                        val finalLogo = if (logoUriVal != null && !logoUriVal.startsWith("http")) {
                            val up = viewModel.subirImagen(logoUriVal)
                            if (up.startsWith("http")) up else prevLogo
                        } else prevLogo
                        val finalIcon = if (listIconUriVal != null && !listIconUriVal.startsWith("http")) {
                            val up = viewModel.subirImagen(listIconUriVal)
                            if (up.startsWith("http")) up else prevIcon
                        } else prevIcon
                        viewModel.saveOrUpdateCompany(
                            companyId = companyToEdit?.id,
                            name = companyName,
                            code = accessCode,
                            logoUrl = finalLogo,
                            listIconUrl = finalIcon,
                            status = status,
                            plan = plan,
                            durationTime = durationTime,
                            country = country,
                            nit = nit,
                            city = city,
                            address = address,
                            phone = phone1,
                            phone2 = phone2,
                            email = email,
                            businessType = if (businessType == "Otro") businessTypeOtro.trim().ifBlank { "Otro" } else businessType,
                            adminName = adminName,
                            adminEmail = adminEmail,
                            adminPass = adminPass,
                            selectedModules = enabledModulesMap.filterValues { it }.keys.toList(),
                            primaryColor = primaryColorHex,
                            secondaryColor = secondaryColorHex,
                            tertiaryColor = tertiaryColorHex,
                            neutralColor = neutralColorHex,
                            fontType = "Inter",
                            onSuccess = { onBack() }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColorParsed),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "Creando empresa..." else if (isEditMode) "Guardar Cambios" else "Crear empresa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = previewFontFamily
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            iOSButton(
                text = "Cancelar",
                onClick = { onBack() },
                modifier = Modifier.fillMaxWidth(),
                tinted = true
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    // Modal Bottom Sheet: Otros Módulos disponibles
    if (showOtherModulesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOtherModulesSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Catálogo Completo de Módulos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Marca o desmarca los módulos que deseas habilitar para esta empresa.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                allSystemModules.forEach { mod ->
                    val isChecked = enabledModulesMap[mod.title] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                enabledModulesMap = enabledModulesMap.toMutableMap().apply {
                                    put(mod.title, !isChecked)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = mod.icon, contentDescription = null, tint = mod.glowColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = mod.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = mod.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                enabledModulesMap = enabledModulesMap.toMutableMap().apply {
                                    put(mod.title, checked)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showOtherModulesSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Confirmar Selección de Módulos", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
    // Modal Bottom Sheet: Personalización Avanzada del Login e Interfaz
    if (showCustomizationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizationSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Personalización Avanzada de Marca",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Define paletas de colores y tipografía de headlines y cuerpo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Selector de Rol de Color
                Text(text = "1. Selección de Colores de Marca:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                val roles = listOf("Primario", "Secundario", "Terciario", "Neutral")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roles.forEach { role ->
                        val isSelected = activeColorRole == role
                        val roleColor = when (role) {
                            "Primario" -> primaryColorParsed
                            "Secundario" -> secondaryColorParsed
                            "Terciario" -> tertiaryColorParsed
                            else -> neutralColorParsed
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) roleColor else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) roleColor else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { activeColorRole = role }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(roleColor))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = role,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                // Editor para el Rol Activo
                val currentHex = when (activeColorRole) {
                    "Primario" -> primaryColorHex
                    "Secundario" -> secondaryColorHex
                    "Terciario" -> tertiaryColorHex
                    else -> neutralColorHex
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Código HEX para color $activeColorRole:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = currentHex,
                            onValueChange = { input ->
                                when (activeColorRole) {
                                    "Primario" -> primaryColorHex = input
                                    "Secundario" -> secondaryColorHex = input
                                    "Terciario" -> tertiaryColorHex = input
                                    else -> neutralColorHex = input
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = primaryColorParsed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val activeParsed = when (activeColorRole) {
                        "Primario" -> primaryColorParsed
                        "Secundario" -> secondaryColorParsed
                        "Terciario" -> tertiaryColorParsed
                        else -> neutralColorParsed
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(activeParsed)
                            .border(2.dp, Color.White, CircleShape)
                            .shadow(2.dp, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ColorWheelPicker(
                    currentHex = currentHex,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onColorPicked = { hex ->
                        when (activeColorRole) {
                            "Primario" -> primaryColorHex = hex
                            "Secundario" -> secondaryColorHex = hex
                            "Terciario" -> tertiaryColorHex = hex
                            else -> neutralColorHex = hex
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Presets para el rol
                Text(text = "Gama de Tonos Guía:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                val presetColors = listOf(
                    "#4F46E5" to "Indigo",
                    "#3B82F6" to "Azul",
                    "#0284C7" to "Océano",
                    "#10B981" to "Esmeralda",
                    "#059669" to "Verde",
                    "#E11D48" to "Carmesí",
                    "#D97706" to "Ámbar",
                    "#7C3AED" to "Violeta",
                    "#EC4899" to "Rosa",
                    "#0F172A" to "Carbón"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetColors.size) { idx ->
                        val (hex, name) = presetColors[idx]
                        val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (currentHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                    color = if (currentHex.equals(hex, ignoreCase = true)) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    when (activeColorRole) {
                                        "Primario" -> primaryColorHex = hex
                                        "Secundario" -> secondaryColorHex = hex
                                        "Terciario" -> tertiaryColorHex = hex
                                        else -> neutralColorHex = hex
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentHex.equals(hex, ignoreCase = true)) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showCustomizationSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColorParsed),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Aplicar Personalización a la Marca", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
data class ExtractedBrandPalette(
    val primaryHex: String,
    val secondaryHex: String,
    val tertiaryHex: String,
    val neutralHex: String
)

@Composable
fun ColorWheelPicker(
    currentHex: String,
    modifier: Modifier = Modifier,
    onColorPicked: (String) -> Unit
) {
    var hsv by remember { mutableStateOf(FloatArray(3)) }
    var initialized by remember { mutableStateOf(false) }
    var lastEmitted by remember { mutableStateOf("") }
    LaunchedEffect(currentHex) {
        if (!initialized || !currentHex.equals(lastEmitted, ignoreCase = true)) {
            try {
                val ext = FloatArray(3)
                android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(currentHex), ext)
                hsv = ext
            } catch (_: Exception) { }
            initialized = true
            lastEmitted = currentHex
        }
    }
    fun applyPoint(off: Offset, w: Float, h: Float, innerPadPx: Float) {
        val cx = w / 2f; val cy = h / 2f
        val dx = off.x - cx; val dy = off.y - cy
        val innerR = (minOf(w, h) / 2f) - innerPadPx
        val hue = ((Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
        val sat = (kotlin.math.sqrt(dx * dx + dy * dy) / innerR).coerceIn(0f, 1f)
        hsv = floatArrayOf(hue, sat, hsv[2].coerceIn(0.15f, 1f))
        val hex = String.format("#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(hsv))
        lastEmitted = hex
        onColorPicked(hex)
    }
    Canvas(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { off -> applyPoint(off, size.width.toFloat(), size.height.toFloat(), 24.dp.toPx()) },
                    onDrag = { change, _ ->
                        change.consume()
                        applyPoint(change.position, size.width.toFloat(), size.height.toFloat(), 24.dp.toPx())
                    }
                )
            }
    ) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val center = Offset(cx, cy)
        val strokeW = 20.dp.toPx()
        val ringR = size.minDimension / 2f - strokeW / 2f
        var angle = 0f
        while (angle < 360f) {
            drawArc(
                color = Color.hsv(angle, 1f, 1f),
                startAngle = angle,
                sweepAngle = 3f,
                useCenter = false,
                style = Stroke(width = strokeW)
            )
            angle += 2f
        }
        val innerR = ringR - strokeW / 2f - 4.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.hsv(hsv[0], 1f, 1f)),
                center = center,
                radius = innerR
            ),
            radius = innerR,
            center = center
        )
        val rad = Math.toRadians(hsv[0].toDouble())
        val r = hsv[1] * innerR
        val knobC = Offset(cx + (kotlin.math.cos(rad) * r).toFloat(), cy + (kotlin.math.sin(rad) * r).toFloat())
        drawCircle(color = Color.hsv(hsv[0], hsv[1], hsv[2]), radius = 12.dp.toPx(), center = knobC)
        drawCircle(color = Color.White, radius = 12.dp.toPx(), center = knobC, style = Stroke(width = 2.5.dp.toPx()))
    }
}
suspend fun extractBrandPaletteFromLogo(context: Context, uriString: String): ExtractedBrandPalette? {
    return withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uriString)
                .allowHardware(false) // Hardware bitmaps cannot be read directly by Palette
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    return@withContext extractPaletteFromBitmap(bitmap)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
fun extractPaletteFromBitmap(bitmap: Bitmap): ExtractedBrandPalette {
    val palette = Palette.from(bitmap).generate()
    val swatches = palette.swatches.sortedByDescending { it.population }
    fun Int.toHex(): String = String.format("#%06X", (0xFFFFFF and this))
    fun isTooLightOrDark(colorInt: Int): Boolean {
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
        return luminance > 240 || luminance < 15
    }
    fun toDarkNeutral(colorInt: Int): String {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt, hsv)
        hsv[1] = (hsv[1] * 0.30f).coerceIn(0.05f, 0.30f)
        hsv[2] = 0.12f
        return android.graphics.Color.HSVToColor(hsv).toHex()
    }
    val primaryInt = palette.getVibrantColor(
        palette.getDominantColor(
            swatches.firstOrNull { !isTooLightOrDark(it.rgb) }?.rgb ?: android.graphics.Color.parseColor("#4F46E5")
        )
    )
    val secondaryCandidate = palette.getLightVibrantColor(
        palette.getMutedColor(
            swatches.getOrNull(1)?.rgb ?: primaryInt
        )
    )
    val secondaryInt = if (secondaryCandidate != primaryInt && !isTooLightOrDark(secondaryCandidate)) {
        secondaryCandidate
    } else {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryInt, hsv)
        hsv[0] = (hsv[0] + 35f) % 360f
        hsv[1] = (hsv[1] * 0.85f).coerceIn(0.3f, 0.9f)
        android.graphics.Color.HSVToColor(hsv)
    }
    val tertiaryCandidate = palette.getDarkVibrantColor(
        palette.getLightMutedColor(
            swatches.getOrNull(2)?.rgb ?: primaryInt
        )
    )
    val tertiaryInt = if (tertiaryCandidate != primaryInt && tertiaryCandidate != secondaryInt && !isTooLightOrDark(tertiaryCandidate)) {
        tertiaryCandidate
    } else {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryInt, hsv)
        hsv[0] = (hsv[0] + 120f) % 360f
        hsv[1] = 0.75f
        hsv[2] = 0.85f
        android.graphics.Color.HSVToColor(hsv)
    }
    val neutralCandidate = palette.getDarkMutedColor(
        swatches.lastOrNull()?.rgb ?: primaryInt
    )
    val neutralHex = toDarkNeutral(neutralCandidate)
    return ExtractedBrandPalette(
        primaryHex = primaryInt.toHex(),
        secondaryHex = secondaryInt.toHex(),
        tertiaryHex = tertiaryInt.toHex(),
        neutralHex = neutralHex
    )
}
data class PlanMatrixDefinition(
    val basicFeatures: List<String>,
    val premiumFeatures: List<String>,
    val maxIaFeatures: List<String>
)
fun getMatrixForBusinessType(businessType: String): PlanMatrixDefinition {
    val bType = businessType.lowercase()
    return when {
        bType.contains("bar") || bType.contains("discoteca") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Clientes", "Cuentas por cobrar", "Compras", "Caja", "Gastos administrativos", "Reportes básicos"),
            premiumFeatures = listOf("Gastos recurrentes", "Reportes avanzados", "Dashboard inteligente", "Control de turnos", "Varias sucursales", "Facturación electrónica", "Happy Hour", "Venta por mesa", "División de cuentas", "Control de propinas", "Combos y promociones", "Productos más y menos vendidos", "Horas de mayor venta"),
            maxIaFeatures = listOf("Predicción de ventas", "IA para promociones", "Recomendación automática de compras", "Agente IA del negocio")
        )
        bType.contains("restaurante") || bType.contains("gastronomía") || bType.contains("gastronomia") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Clientes", "Caja", "Compras", "Cuentas por cobrar", "Mesas", "Comandero", "Cocina", "División de cuentas", "Reserva de mesas", "Combos", "Menú del día", "Control de propinas"),
            premiumFeatures = listOf("Facturación electrónica", "Dashboard inteligente", "Varias sucursales", "Reportes avanzados", "Tiempo promedio de preparación", "Productos agotados", "Ingredientes por receta"),
            maxIaFeatures = listOf("Predicción de demanda", "IA para compras", "IA para tiempos de cocina", "Recomendación automática del menú", "Agente IA del restaurante")
        )
        bType.contains("cafetería") || bType.contains("cafeteria") || bType.contains("panadería") || bType.contains("panaderia") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Caja", "Clientes", "Compras", "Combos", "Promociones", "Programa de puntos"),
            premiumFeatures = listOf("Facturación electrónica", "Reportes avanzados", "Dashboard inteligente", "Varias sucursales", "Ventas por hora", "Productos adicionales"),
            maxIaFeatures = listOf("Predicción de ventas", "IA recomienda promociones", "Recomendación de productos", "Agente IA")
        )
        bType.contains("barrio") || bType.contains("minimarket") || bType.contains("supermercado") || bType.contains("tienda de barrio") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Caja", "Clientes", "Cuentas por cobrar", "Compras", "Fiados", "Abonos", "Código de barras"),
            premiumFeatures = listOf("Historial de compras", "Control de proveedores", "Alerta de productos agotados", "Productos por vencer", "Dashboard inteligente", "Facturación electrónica", "Varias sucursales", "Reportes avanzados"),
            maxIaFeatures = listOf("IA predice inventario", "IA recomienda pedidos", "IA detecta productos lentos", "Agente IA")
        )
        bType.contains("carnicería") || bType.contains("carniceria") || bType.contains("fruver") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Venta por peso", "Caja", "Clientes", "Compras", "Control de lotes", "Fecha de vencimiento", "Control de mermas", "Productos refrigerados", "Rendimiento de cortes"),
            premiumFeatures = listOf("Facturación electrónica", "Reportes avanzados"),
            maxIaFeatures = listOf("IA predice desperdicios", "IA recomienda compras", "IA analiza ventas por corte", "Alertas inteligentes")
        )
        bType.contains("mascota") || bType.contains("petshop") || bType.contains("veterinaria") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Caja", "Clientes", "Compras", "Historial de mascotas", "Agenda de citas", "Vacunas", "Servicios de peluquería", "Medicamentos", "Recordatorios"),
            premiumFeatures = listOf("Facturación electrónica", "Reportes avanzados"),
            maxIaFeatures = listOf("IA recuerda vacunas", "IA recomienda productos", "IA analiza clientes frecuentes")
        )
        bType.contains("cosmético") || bType.contains("cosmetico") || bType.contains("belleza") || bType.contains("moda") || bType.contains("ropa") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Caja", "Clientes", "Compras", "Variantes - color, tono", "Productos por marca", "Kits", "Programa de puntos", "Promociones", "Productos por vencer"),
            premiumFeatures = listOf("Facturación electrónica", "Reportes avanzados"),
            maxIaFeatures = listOf("IA recomienda productos", "IA analiza tendencias", "IA predice productos de moda")
        )
        bType.contains("ferretería") || bType.contains("ferreteria") || bType.contains("construcción") || bType.contains("construccion") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Caja", "Clientes", "Compras", "Cotizaciones", "Proveedores", "Variantes - tamaño, medida", "Venta por metro", "Varias bodegas", "Garantías", "Pedidos especiales"),
            premiumFeatures = listOf("Facturación electrónica", "Reportes avanzados"),
            maxIaFeatures = listOf("IA recomienda compras", "IA detecta productos sin rotación", "IA predice inventario")
        )
        bType.contains("licorería") || bType.contains("licoreria") || bType.contains("cigarrería") || bType.contains("cigarreria") -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Caja", "Clientes", "Cuentas por cobrar", "Compras", "Venta por caja o unidad", "Promociones", "Happy Hour", "Control de lotes", "Productos Premium", "Control de turnos"),
            premiumFeatures = listOf("Facturación electrónica", "Reportes avanzados"),
            maxIaFeatures = listOf("IA para promociones", "Predicción de ventas por temporada", "IA recomienda compras", "IA detecta productos de baja rotación")
        )
        else -> PlanMatrixDefinition(
            basicFeatures = listOf("Inventario", "Registrar ventas", "Clientes", "Cuentas por cobrar", "Compras", "Caja", "Gastos administrativos", "Reportes básicos"),
            premiumFeatures = listOf("Gastos recurrentes", "Reportes avanzados", "Dashboard inteligente", "Control de turnos", "Varias sucursales", "Facturación electrónica"),
            maxIaFeatures = listOf("Predicción de ventas", "IA para promociones", "Agente IA del negocio")
        )
    }
}
@Composable
fun FeatureMatrixItemRow(
    featureName: String,
    statusText: String,
    statusBg: Color,
    statusTextColor: Color,
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isEnabled) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f))
            .border(
                1.dp,
                if (isEnabled) Color.White.copy(alpha = 0.60f) else Color.Gray.copy(alpha = 0.20f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = isEnabled) { onCheckedChange(!isChecked) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isEnabled) statusBg else Color.Gray.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = if (!isEnabled) Icons.Default.Lock else when {
                            featureName.contains("IA", ignoreCase = true) || featureName.contains("Predicción", ignoreCase = true) -> Icons.Outlined.AutoAwesome
                            featureName.contains("venta", ignoreCase = true) || featureName.contains("Caja", ignoreCase = true) -> Icons.Outlined.ShoppingCart
                            featureName.contains("Inventario", ignoreCase = true) || featureName.contains("lotes", ignoreCase = true) -> Icons.Outlined.Inventory2
                            featureName.contains("Cliente", ignoreCase = true) || featureName.contains("mascota", ignoreCase = true) -> Icons.Outlined.Store
                            featureName.contains("Reporte", ignoreCase = true) || featureName.contains("Dashboard", ignoreCase = true) -> Icons.Outlined.PieChart
                            featureName.contains("Comandero", ignoreCase = true) || featureName.contains("Cocina", ignoreCase = true) || featureName.contains("Mesa", ignoreCase = true) -> Icons.Outlined.Restaurant
                            featureName.contains("Facturación", ignoreCase = true) -> Icons.Outlined.ReceiptLong
                            else -> Icons.Default.Check
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isEnabled) statusTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = featureName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isChecked && isEnabled,
                enabled = isEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

private fun rolDeDescripcion(descripcion: String): String {
    val idx = descripcion.indexOf("[Rol:", ignoreCase = true)
    if (idx < 0) return ""
    val end = descripcion.indexOf("]", startIndex = idx)
    return if (end > idx) descripcion.substring(idx + 6, end).trim() else ""
}
