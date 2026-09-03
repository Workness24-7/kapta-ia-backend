package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalIsDarkMode

data class UserRoleItem(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val pin: String = "1234",
    val companyId: Int = 0,
    val companyCode: String = "",
    val dockAccess: Map<String, Boolean> = mapOf("Inicio" to true, "Ventas" to true, "Finanzas" to true, "Inventario" to true),
    val moduleAccess: Map<String, Boolean> = mapOf(
        "Reportes y Analytics" to true,
        "Control de Turnos y Caja" to true,
        "Facturación Electrónica DIAN" to true,
        "Happy Hour & Promociones" to true,
        "Venta por Mesa & Comandero" to true,
        "División de Cuentas (Split)" to true,
        "Agente IA Kapta Assistant" to true
    ),
    val functionsJson: String = "",
    val secciones: UserSecciones = UserSecciones()
)

/**
 * Codifica los permisos del usuario en un único JSON para assignedFunctionsJson.
 * Formato: {"dock": {tab: bool}, "functions": [nombres...], "caps": [...], "secciones": {...}}.
 * Si functions está vacío y no hay dock, devuelve null para conservar el formato
 * legacy (array de nombres).
 */
private fun encodeUserPermissions(saved: UserRoleItem): String? {
    val functions = try {
        val arr = org.json.JSONArray(saved.functionsJson)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) { val n = arr.optString(i); if (n.isNotBlank()) out.add(n) }
        out
    } catch (_: Exception) { return saved.functionsJson }
    val dockEnabled = saved.dockAccess.any { it.value }
    if (functions.isEmpty() && !dockEnabled) return null
    // Capacidades internas derivadas tal cual de los interruptores otorgados.
    val caps = mutableSetOf<String>()
    if (saved.dockAccess["Ventas"] == true) caps.addAll(listOf("ventas", "deudores", "clientes"))
    if (saved.dockAccess["Finanzas"] == true) caps.addAll(listOf("gastos", "reporte"))
    if (saved.dockAccess["Inventario"] == true) caps.add("inventario")
    if (saved.moduleAccess["Control de Turnos y Caja"] == true) caps.add("gastos")
    if (saved.moduleAccess["Reportes y Analytics"] == true) caps.add("reporte")
    if (saved.moduleAccess["Facturación Electrónica DIAN"] == true) caps.add("facturacion")
    if (saved.moduleAccess["Venta por Mesa & Comandero"] == true) caps.add("ventas")
    if (saved.moduleAccess["División de Cuentas (Split)"] == true) caps.add("ventas")
    if (saved.moduleAccess["Happy Hour & Promociones"] == true) caps.add("ventas")
    return org.json.JSONObject().apply {
        put("dock", org.json.JSONObject(saved.dockAccess))
        put("functions", org.json.JSONArray().apply { functions.forEach { put(it) } })
        put("caps", org.json.JSONArray().apply { caps.forEach { put(it) } })
        put("secciones", saved.secciones.toJson())
    }.toString()
}

/**
 * Extrae el mapa de acceso al dock (Inicio/Ventas/Finanzas/Inventario) desde el
 * assignedFunctionsJson codificado. Devuelve null si no está presente (formato legacy).
 */
fun decodeDockAccessFromJson(json: String?): Map<String, Boolean>? {
    if (json.isNullOrBlank()) return null
    return try {
        if (json.trimStart().startsWith("{")) {
            val o = org.json.JSONObject(json)
            if (o.has("dock")) {
                val d = o.getJSONObject("dock")
                buildMap { d.keys().forEach { key -> put(key, d.optBoolean(key, false)) } }
            } else null
        } else null
    } catch (_: Exception) { null }
}

/**
 * Extrae las capacidades otorgadas ("caps") desde el assignedFunctionsJson
 * codificado. Devuelve null si no están presentes (formato legacy).
 */
fun decodeCapsFromJson(json: String?): Set<String>? {
    if (json.isNullOrBlank() || !json.trimStart().startsWith("{")) return null
    return try {
        val o = org.json.JSONObject(json)
        if (!o.has("caps")) null
        else {
            val arr = o.getJSONArray("caps")
            val out = mutableSetOf<String>()
            for (i in 0 until arr.length()) { val c = arr.optString(i); if (c.isNotBlank()) out.add(c) }
            out
        }
    } catch (_: Exception) { null }
}

// ---------------------------------------------------------------------------
// Permisos granulares por vista/sección ("Funciones" al crear usuario).
// Para agregar una función nueva: añade su ItemPermiso a la lista de su
// sección (aparece sola en el diálogo) y respeta su clave en el POS.
// ---------------------------------------------------------------------------
data class ItemPermiso(val key: String, val titulo: String)

val RESUMEN_INICIO_ITEMS = listOf(
    ItemPermiso("ventas", "Ventas del día"),
    ItemPermiso("gastos", "Gastos del mes"),
    ItemPermiso("deudores", "Deudores"),
    ItemPermiso("clientes", "Clientes Activos")
)
val ACCIONES_INICIO_ITEMS = listOf(
    ItemPermiso("venta", "Venta"),
    ItemPermiso("gasto", "Gasto"),
    ItemPermiso("agregar", "Agregar"),
    ItemPermiso("deudores", "Deudores")
)
val RESUMEN_VENTAS_ITEMS = listOf(
    ItemPermiso("hoy", "Ventas Hoy"),
    ItemPermiso("semana", "Esta Semana"),
    ItemPermiso("mes", "Este Mes")
)
val FILTROS_FINANZAS_ITEMS = listOf(
    ItemPermiso("dia", "Día"),
    ItemPermiso("mes", "Mes"),
    ItemPermiso("rango", "Rango de fechas")
)

/** Todo activado por defecto: una función nueva aparece visible hasta que se desactive. */
data class UserSecciones(
    val resumen: Set<String> = setOf("ventas", "gastos", "deudores", "clientes"),
    val acciones: Set<String> = setOf("venta", "gasto", "agregar", "deudores"),
    val alertas: Boolean = true,
    val ventasResumen: Set<String> = setOf("hoy", "semana", "mes"),
    val ventasRanking: Boolean = true,
    val ventasVerMas: Boolean = true,
    val ventasVerInventario: Boolean = true,
    val finPdf: Boolean = true,
    val finFiltros: Set<String> = setOf("dia", "mes", "rango"),
    val finVentas: Boolean = true,
    val finGastos: Boolean = true,
    val finRegistrar: Boolean = true,
    val invCarga: Boolean = true,
    val invMovimientos: Boolean = true,
    val invCrear: Boolean = true,
    val invEditar: Boolean = true,
    val invEliminar: Boolean = true,
    val invGuardar: Boolean = true,
    val invHacer: Boolean = true,
    val invLectura: Boolean = false
)

private fun jsonStrSet(o: org.json.JSONObject, key: String, def: Set<String>): Set<String> {
    if (!o.has(key)) return def
    return try {
        val arr = o.getJSONArray(key)
        val out = mutableSetOf<String>()
        for (i in 0 until arr.length()) { val s = arr.optString(i); if (s.isNotBlank()) out.add(s) }
        out
    } catch (_: Exception) { def }
}

/** Lee "secciones" explícitas; null si no existen (legado: el POS usa sus reglas anteriores). */
fun decodeSecciones(json: String?): UserSecciones? {
    if (json.isNullOrBlank() || !json.trimStart().startsWith("{")) return null
    return try {
        val o = org.json.JSONObject(json)
        if (!o.has("secciones")) return null
        val s = o.getJSONObject("secciones")
        val def = UserSecciones()
        UserSecciones(
            resumen = jsonStrSet(s, "resumen", def.resumen),
            acciones = jsonStrSet(s, "acciones", def.acciones),
            alertas = s.optBoolean("alertas", def.alertas),
            ventasResumen = jsonStrSet(s, "ventasResumen", def.ventasResumen),
            ventasRanking = s.optBoolean("ventasRanking", def.ventasRanking),
            ventasVerMas = s.optBoolean("ventasVerMas", def.ventasVerMas),
            ventasVerInventario = s.optBoolean("ventasVerInventario", def.ventasVerInventario),
            finPdf = s.optBoolean("finPdf", def.finPdf),
            finFiltros = jsonStrSet(s, "finFiltros", def.finFiltros),
            finVentas = s.optBoolean("finVentas", def.finVentas),
            finGastos = s.optBoolean("finGastos", def.finGastos),
            finRegistrar = s.optBoolean("finRegistrar", def.finRegistrar),
            invCarga = s.optBoolean("invCarga", def.invCarga),
            invMovimientos = s.optBoolean("invMovimientos", def.invMovimientos),
            invCrear = s.optBoolean("invCrear", def.invCrear),
            invEditar = s.optBoolean("invEditar", def.invEditar),
            invEliminar = s.optBoolean("invEliminar", def.invEliminar),
            invGuardar = s.optBoolean("invGuardar", def.invGuardar),
            invHacer = s.optBoolean("invHacer", def.invHacer),
            invLectura = s.optBoolean("invLectura", def.invLectura)
        )
    } catch (_: Exception) { null }
}

fun UserSecciones.toJson(): org.json.JSONObject = org.json.JSONObject().apply {
    fun putSet(key: String, v: Set<String>) = put(key, org.json.JSONArray().apply { v.forEach { put(it) } })
    putSet("resumen", resumen)
    putSet("acciones", acciones)
    put("alertas", alertas)
    putSet("ventasResumen", ventasResumen)
    put("ventasRanking", ventasRanking)
    put("ventasVerMas", ventasVerMas)
    put("ventasVerInventario", ventasVerInventario)
    put("finPdf", finPdf)
    putSet("finFiltros", finFiltros)
    put("finVentas", finVentas)
    put("finGastos", finGastos)
    put("finRegistrar", finRegistrar)
    put("invCarga", invCarga)
    put("invMovimientos", invMovimientos)
    put("invCrear", invCrear)
    put("invEditar", invEditar)
    put("invEliminar", invEliminar)
    put("invGuardar", invGuardar)
    put("invHacer", invHacer)
    put("invLectura", invLectura)
}

/** Secciones por defecto según rol al crear usuario (el admin ajusta una a una). */
fun seccionesPorRol(rol: String): UserSecciones = when {
    rol.equals("Administrador", ignoreCase = true) || rol.equals("Supervisor", ignoreCase = true) -> UserSecciones()
    rol.equals("Cajero", ignoreCase = true) -> UserSecciones(
        acciones = setOf("venta", "gasto", "deudores"),
        finFiltros = setOf("dia", "mes", "rango"),
        invCarga = false, invMovimientos = false, invCrear = false,
        invEditar = false, invEliminar = false, invGuardar = false, invHacer = false,
        invLectura = true
    )
    rol.equals("Mesero", ignoreCase = true) || rol.equals("Barman", ignoreCase = true) -> UserSecciones(
        acciones = setOf("venta", "deudores"),
        finPdf = false, finFiltros = emptySet(), finVentas = false, finGastos = false, finRegistrar = false,
        invCarga = false, invMovimientos = false, invCrear = false,
        invEditar = false, invEliminar = false, invGuardar = false, invHacer = false,
        invLectura = true
    )
    else -> UserSecciones(
        acciones = setOf("venta", "deudores"),
        finPdf = false, finFiltros = emptySet(), finVentas = false, finGastos = false, finRegistrar = false,
        invCarga = false, invMovimientos = false, invCrear = false,
        invEditar = false, invEliminar = false, invGuardar = false, invHacer = false,
        invLectura = true
    )
}

// ---------------------------------------------------------------------------
// Validación de correo y requisitos de contraseña al crear usuario.
// ---------------------------------------------------------------------------
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun emailValido(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

fun pinTieneMayus(pin: String): Boolean = pin.any { it.isUpperCase() }
fun pinTieneMinus(pin: String): Boolean = pin.any { it.isLowerCase() }
fun pinTieneNumero(pin: String): Boolean = pin.any { it.isDigit() }
fun pinTieneEspecial(pin: String): Boolean = pin.any { !it.isLetterOrDigit() }
fun pinCumpleRequisitos(pin: String): Boolean =
    pinTieneMayus(pin) && pinTieneMinus(pin) && pinTieneNumero(pin) && pinTieneEspecial(pin)

// ---------------------------------------------------------------------------
// Roles personalizados + su configuración, persistidos por negocio.
// ---------------------------------------------------------------------------
private fun rolesPrefs(context: android.content.Context) =
    context.getSharedPreferences("kapta_roles", android.content.Context.MODE_PRIVATE)

fun cargarRolesPersonalizados(context: android.content.Context, companyCode: String): Set<String> =
    rolesPrefs(context).getStringSet("roles_$companyCode", emptySet()) ?: emptySet()

fun guardarRolPersonalizado(context: android.content.Context, companyCode: String, rol: String) {
    val actual = cargarRolesPersonalizados(context, companyCode).toMutableSet()
    if (actual.add(rol.trim())) {
        rolesPrefs(context).edit().putStringSet("roles_$companyCode", actual).apply()
    }
}

fun guardarCfgRol(
    context: android.content.Context,
    companyCode: String,
    rol: String,
    dock: Map<String, Boolean>,
    modulos: Map<String, Boolean>,
    funciones: List<String>,
    secciones: UserSecciones
) {
    val cfg = org.json.JSONObject().apply {
        put("dock", org.json.JSONObject(dock))
        put("modulos", org.json.JSONObject(modulos))
        put("functions", org.json.JSONArray().apply { funciones.forEach { put(it) } })
        put("secciones", secciones.toJson())
    }
    rolesPrefs(context).edit().putString("cfg_${companyCode}_$rol", cfg.toString()).apply()
}

data class CfgRol(
    val dock: Map<String, Boolean>? = null,
    val modulos: Map<String, Boolean>? = null,
    val funciones: List<String>? = null,
    val secciones: UserSecciones? = null
)

fun cargarCfgRol(context: android.content.Context, companyCode: String, rol: String): CfgRol? {
    val raw = rolesPrefs(context).getString("cfg_${companyCode}_$rol", null) ?: return null
    return try {
        val o = org.json.JSONObject(raw)
        fun mapaDe(obj: org.json.JSONObject?): Map<String, Boolean>? {
            if (obj == null) return null
            val m = mutableMapOf<String, Boolean>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                m[k] = obj.optBoolean(k, false)
            }
            return m
        }
        val funcionesArr = o.optJSONArray("functions")
        val funcionesList: List<String>? = if (funcionesArr == null) null else {
            val out = mutableListOf<String>()
            for (i in 0 until funcionesArr.length()) {
                val s = funcionesArr.optString(i)
                if (s.isNotBlank()) out.add(s)
            }
            out
        }
        val secObj: org.json.JSONObject? = o.optJSONObject("secciones")
        var seccionesDec: UserSecciones? = null
        if (secObj != null) {
            val wrapper = org.json.JSONObject()
            wrapper.put("secciones", secObj as Any)
            seccionesDec = decodeSecciones(wrapper.toString())
        }
        CfgRol(
            dock = mapaDe(o.optJSONObject("dock")),
            modulos = mapaDe(o.optJSONObject("modulos")),
            funciones = funcionesList,
            secciones = seccionesDec
        )
    } catch (_: Exception) { null }
}

private fun parseCompanyFunctions(json: String?): List<Pair<String, String>> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        val out = mutableListOf<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i)
            if (o != null) {
                val nombre = o.optString("nombre").ifBlank { continue }
                out.add(Pair(nombre, o.optString("descripcion", "")))
            } else {
                val s = arr.optString(i)
                if (s.isNotBlank()) out.add(Pair(s, ""))
            }
        }
        out
    } catch (e: Exception) { emptyList() }
}

private fun rolDeFuncion(descripcion: String): String {
    val idx = descripcion.indexOf("[Rol:", ignoreCase = true)
    if (idx < 0) return ""
    val end = descripcion.indexOf("]", startIndex = idx)
    return if (end > idx) descripcion.substring(idx + 6, end).trim() else ""
}

/**
 * iOS-styled User Profile Modal Dialog ("Cuadro de información del perfil").
 * Recreates exact aesthetic matching iOS grouped list dialog specifications.
 */
@Composable
fun UserProfileModal(
    userName: String = "Brayam",
    userRole: String = "Administrador",
    userEmail: String = "brayam@kapta.ia",
    userPhone: String = "+57 300 123 4567",
    userLanguage: String = "Español",
    badgeLabel: String = "AI",
    isDarkMode: Boolean = false,
    companyUsers: List<CompanyUserEntity> = emptyList(),
    companies: List<CompanyEntity> = emptyList(),
    viewerIsSuperAdmin: Boolean = false,
    currentCompanyCode: String? = null,
    dynamicCode: String? = null,
    onDarkModeToggle: (Boolean) -> Unit = {},
    onDeleteUser: ((CompanyUserEntity) -> Unit)? = null,
    onSaveUser: ((CompanyUserEntity) -> Unit)? = null,
    onSavePhone: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val isDark = LocalIsDarkMode.current || isDarkMode
    var userToDeleteItem by remember { mutableStateOf<UserRoleItem?>(null) }

    // Usuarios SOLO desde la base de datos (sin datos de ejemplo)
    var showUserFormDialog by remember { mutableStateOf(false) }
    var editingUserItem by remember { mutableStateOf<UserRoleItem?>(null) }

    // Foto persistente por usuario (SharedPreferences, sobrevive reinicios)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("kapta_perfil", android.content.Context.MODE_PRIVATE) }
    var userPhotoUrl by remember { mutableStateOf(prefs.getString("foto_$userEmail", "") ?: "") }
    var showPhotoUploadDialog by remember { mutableStateOf(false) }
    var showPhoneEditDialog by remember { mutableStateOf(false) }
    var phoneEditValue by remember { mutableStateOf(userPhone) }
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { picked ->
        picked?.let {
            val saved = com.example.util.PhotoStore.save(context, it)
            if (saved != null) {
                userPhotoUrl = saved
                prefs.edit().putString("foto_$userEmail", saved).apply()
                showPhotoUploadDialog = false
            }
        }
    }

    // Filtrado de usuarios: superadmin ve solo usuarios con acceso al sistema superadmin;
    // admin de negocio ve solo los usuarios de su empresa.
    val visibleCompanyUsers = when {
        viewerIsSuperAdmin -> companyUsers.filter {
            it.role.contains("SUPER_ADMIN", ignoreCase = true) ||
                    it.email.endsWith("@kaptaia.com", ignoreCase = true)
        }
        !currentCompanyCode.isNullOrBlank() -> companyUsers.filter {
            it.companyCode.equals(currentCompanyCode, ignoreCase = true)
        }
        else -> companyUsers
    }

    val businessUsers = remember(visibleCompanyUsers) {
        mutableStateListOf(*visibleCompanyUsers.map { u ->
            UserRoleItem(
                id = u.id,
                name = u.name,
                email = u.email,
                role = u.role,
                pin = u.password,
                companyId = u.companyId,
                companyCode = u.companyCode,
                functionsJson = u.assignedFunctionsJson,
                secciones = decodeSecciones(u.assignedFunctionsJson) ?: seccionesPorRol(u.role),
                dockAccess = mapOf("Inicio" to true, "Ventas" to true, "Finanzas" to (u.role.contains("Admin", true) || u.role.contains("Líder", true)), "Inventario" to !u.role.contains("Cajero", true)),
                moduleAccess = mapOf(
                    "Reportes y Analytics" to u.role.contains("Admin", true),
                    "Control de Turnos y Caja" to true,
                    "Facturación Electrónica DIAN" to true,
                    "Happy Hour & Promociones" to true,
                    "Venta por Mesa & Comandero" to true,
                    "División de Cuentas (Split)" to true,
                    "Agente IA Kapta Assistant" to u.role.contains("Admin", true)
                )
            )
        }.toTypedArray())
    }

    // Dynamic color definitions for light / dark blue theme
    val profileCompany = remember(currentCompanyCode, companies) {
        companies.firstOrNull { !currentCompanyCode.isNullOrBlank() && it.code.equals(currentCompanyCode, ignoreCase = true) } ?: companies.firstOrNull()
    }
    val businessBorderColors = remember(profileCompany) {
        val primary = runCatching { Color(android.graphics.Color.parseColor(profileCompany?.primaryColorHex ?: "#38BDF8")) }.getOrDefault(Color(0xFF38BDF8))
        val secondary = runCatching { Color(android.graphics.Color.parseColor(profileCompany?.secondaryColorHex ?: "#60A5FA")) }.getOrDefault(Color(0xFF60A5FA))
        val tertiary = runCatching { Color(android.graphics.Color.parseColor(profileCompany?.tertiaryColorHex ?: "#818CF8")) }.getOrDefault(Color(0xFF818CF8))
        listOf(primary, secondary, tertiary)
    }
    val modalBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF2F2F7)
    val cardBgColor = if (isDark) Color(0xFF0F172A) else Color.White
    val primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val valueTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val dividerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0).copy(alpha = 0.8f)
    val okButtonColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF007AFF)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Frosted / Semi-transparent Backdrop Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Modal Floating Card Container
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = modalBgColor,
                shadowElevation = 24.dp,
                tonalElevation = 2.dp,
                border = BorderStroke(0.6.dp, if (isDark) Color(0xFF334155) else Color.White.copy(alpha = 0.8f)),
                modifier = Modifier
                    .widthIn(max = 390.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Header Bar: Title centered & "OK" action text button on the right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "OK",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = okButtonColor,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 0.8.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Avatar, Name & Role Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Circle with Cyan/Blue gradient border & "AI" badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable { showPhotoUploadDialog = true }
                        ) {
                            // Circle Avatar Container
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = businessBorderColors
                                        )
                                    )
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userPhotoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = userPhotoUrl,
                                        contentDescription = "Foto de perfil de $userName",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar de $userName",
                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF475569),
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            }

                            // Camera / Edit Overlay Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4F46E5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cargar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            // "AI" Badge on Bottom-Right of Avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color.White)
                                    .border(0.6.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1E293B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = userRole,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Grouped List Block 1: "Información de la cuenta"
                    GroupedSectionTitle(title = "Información de la cuenta", color = primaryTextColor)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBgColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shadowElevation = if (isDark) 0.dp else 0.5.dp
                    ) {
                        Column {
                            GroupedListRow(
                                label = "Correo Electrónico",
                                value = userEmail,
                                showChevron = true,
                                labelColor = primaryTextColor,
                                valueColor = valueTextColor
                            )
                            GroupedDivider(color = dividerColor)
                            GroupedListRow(
                                label = "Teléfono",
                                value = userPhone,
                                showChevron = true,
                                labelColor = primaryTextColor,
                                valueColor = valueTextColor,
                                onClick = {
                                    if (onSavePhone != null) {
                                        phoneEditValue = userPhone
                                        showPhoneEditDialog = true
                                    }
                                }
                            )
                            GroupedDivider(color = dividerColor)
                            GroupedListRow(
                                label = "Rol",
                                value = userRole,
                                showChevron = true,
                                labelColor = primaryTextColor,
                                valueColor = valueTextColor
                            )
                            if (!dynamicCode.isNullOrBlank() && (userRole.contains("Admin", ignoreCase = true) || userRole.contains("Administrador", ignoreCase = true))) {
                                GroupedDivider(color = dividerColor)
                                GroupedListRow(
                                    label = "Clave de Verificación",
                                    value = dynamicCode,
                                    showChevron = false,
                                    labelColor = primaryTextColor,
                                    valueColor = Color(0xFF34C759)
                                )
                            }
                        }
                    }

                    // 3.5. Grouped List Block: "Gestión de Usuarios y Roles" (Solo Administradores)
                    val isAdminUser = userRole.contains("Admin", ignoreCase = true) || userRole.contains("Administrador", ignoreCase = true)
                    if (isAdminUser) {
                        Spacer(modifier = Modifier.height(20.dp))
                        GroupedSectionTitle(title = "Gestión de Usuarios y Roles (${businessUsers.size})", color = primaryTextColor)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBgColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shadowElevation = if (isDark) 0.dp else 0.5.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (businessUsers.isEmpty()) {
                                    Text(
                                        text = "No hay usuarios registrados en la base de datos.",
                                        fontSize = 12.sp,
                                        color = secondaryTextColor,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                businessUsers.forEachIndexed { index, u ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editingUserItem = u
                                                showUserFormDialog = true
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = u.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryTextColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when (u.role) {
                                                        "Administrador" -> Color(0xFF4F46E5)
                                                        "Cajero" -> Color(0xFF10B981)
                                                        else -> Color(0xFFF59E0B)
                                                    }
                                                ) {
                                                    Text(
                                                        text = u.role,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${u.email} • Clave/PIN: ${u.pin}",
                                                fontSize = 11.sp,
                                                color = secondaryTextColor
                                            )
                                            Text(
                                                text = "Dock: ${u.dockAccess.filterValues { it }.keys.joinToString(", ")} • ${u.moduleAccess.count { it.value }} Módulos",
                                                fontSize = 10.sp,
                                                color = okButtonColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    editingUserItem = u
                                                    showUserFormDialog = true
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar",
                                                    tint = secondaryTextColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    userToDeleteItem = u
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar Usuario",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (index < businessUsers.size - 1) {
                                        GroupedDivider(color = dividerColor)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        editingUserItem = null
                                        showUserFormDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = okButtonColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Crear Usuario", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Grouped List Block 2: "Ajustes"
                    GroupedSectionTitle(title = "Ajustes", color = primaryTextColor)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBgColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shadowElevation = if (isDark) 0.dp else 0.5.dp
                    ) {
                        Column {
                            GroupedSwitchRow(
                                label = "Modo Oscuro",
                                checked = isDarkMode,
                                onCheckedChange = onDarkModeToggle,
                                labelColor = primaryTextColor
                            )
                            GroupedDivider(color = dividerColor)
                            GroupedListRow(
                                label = "Notificaciones",
                                value = "",
                                showChevron = true,
                                labelColor = primaryTextColor,
                                valueColor = valueTextColor
                            )
                            GroupedDivider(color = dividerColor)
                            GroupedListRow(
                                label = "Privacidad y Seguridad",
                                value = "",
                                showChevron = true,
                                labelColor = primaryTextColor,
                                valueColor = valueTextColor
                            )
                            GroupedDivider(color = dividerColor)
                            GroupedListRow(
                                label = "Idioma",
                                value = userLanguage,
                                showChevron = true,
                                labelColor = primaryTextColor,
                                valueColor = valueTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. Grouped List Block 3: "Cuenta" & "Cerrar Sesión"
                    GroupedSectionTitle(title = "Cuenta", color = primaryTextColor)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBgColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                onDismiss()
                                onLogout()
                            },
                        shadowElevation = if (isDark) 0.dp else 0.5.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cerrar Sesión",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFC53030)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showPhoneEditDialog && onSavePhone != null) {
        AlertDialog(
            onDismissRequest = { showPhoneEditDialog = false },
            title = { Text("Editar teléfono", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = phoneEditValue,
                    onValueChange = { phoneEditValue = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                    label = { Text("Número de celular") },
                    placeholder = { Text("Ej. +57 300 123 4567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneEditValue.isNotBlank()) {
                            onSavePhone.invoke(phoneEditValue.trim())
                            showPhoneEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneEditDialog = false }) { Text("Cancelar") }
            }
        )
    }

    val companyFunctions = parseCompanyFunctions(companies.firstOrNull { it.code == currentCompanyCode }?.customFunctionsJson)

    if (showUserFormDialog) {
        UserRoleFormDialog(
            userToEdit = editingUserItem,
            isDark = isDark,
            companies = companies,
            companyFunctions = companyFunctions,
            emailsExistentes = businessUsers
                .filter { it.id != editingUserItem?.id }
                .mapNotNull { it.email.trim().takeIf { e -> e.isNotBlank() } }
                .toSet(),
            onDismiss = { showUserFormDialog = false },
            onSave = { saved ->
                if (onSaveUser != null) {
                    onSaveUser.invoke(
                        CompanyUserEntity(
                            // Al editar se conserva el id para ACTUALIZAR; al crear va 0 para insertar.
                            id = if (editingUserItem != null) saved.id else 0,
                            companyId = saved.companyId,
                            companyCode = saved.companyCode,
                            name = saved.name,
                            role = saved.role,
                            email = saved.email,
                            username = saved.name.ifBlank { saved.email },
                            password = saved.pin,
                            isSynced = false,
                            assignedFunctionsJson = encodeUserPermissions(saved) ?: saved.functionsJson
                        )
                    )
                } else {
                    val idx = businessUsers.indexOfFirst { it.id == saved.id }
                    if (idx >= 0) {
                        businessUsers[idx] = saved
                    } else {
                        businessUsers.add(saved)
                    }
                }
                showUserFormDialog = false
            }
        )
    }

    if (userToDeleteItem != null) {
        val u = userToDeleteItem!!
        AlertDialog(
            onDismissRequest = { userToDeleteItem = null },
            title = { Text("Eliminar Usuario", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar al usuario '${u.name}' (${u.email})? Se removerá de la base de datos de Railway y de la base de datos local.") },
            confirmButton = {
                Button(
                    onClick = {
                        val matchingEntity = companyUsers.find { it.id == u.id || it.email.equals(u.email, ignoreCase = true) }
                        if (matchingEntity != null && onDeleteUser != null) {
                            onDeleteUser.invoke(matchingEntity)
                        } else {
                            businessUsers.removeAll { it.id == u.id || it.email.equals(u.email, ignoreCase = true) }
                        }
                        userToDeleteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDeleteItem = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPhotoUploadDialog) {
        var tempUrl by remember { mutableStateOf(userPhotoUrl) }
        val samplePhotos = listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300"
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPhotoUploadDialog = false },
            title = { Text("Actualizar Foto de Perfil", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryTextColor) },
            text = {
                Column {
                    Text("Ingresa la URL de tu foto o selecciona un avatar real:", fontSize = 12.sp, color = secondaryTextColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("URL de la Foto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subir desde el dispositivo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Avatares sugeridos:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        samplePhotos.forEach { url ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(if (tempUrl == url) 2.dp else 0.dp, okButtonColor, CircleShape)
                                    .clickable { tempUrl = url }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPhotoUrl = tempUrl.trim()
                        prefs.edit().putString("foto_$userEmail", userPhotoUrl).apply()
                        showPhotoUploadDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = okButtonColor)
                ) {
                    Text("Guardar Foto", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoUploadDialog = false }) {
                    Text("Cancelar", color = secondaryTextColor)
                }
            }
        )
    }
}

@Composable
private fun RequisitoClave(cumple: Boolean, texto: String, textSecondary: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 1.dp)
    ) {
        Icon(
            imageVector = if (cumple) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (cumple) Color(0xFF10B981) else textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = texto,
            fontSize = 11.sp,
            color = if (cumple) Color(0xFF10B981) else textSecondary
        )
    }
}

@Composable
private fun FilaSwitchPermiso(
    titulo: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    textPrimary: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(titulo, fontSize = fontSize, color = textPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun FilaCheckPermiso(
    titulo: String,
    checked: Boolean,
    onToggle: () -> Unit,
    textPrimary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(titulo, fontSize = 12.sp, color = textPrimary, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
private fun VistaExpandablePermiso(
    titulo: String,
    masterOn: Boolean,
    onMaster: (Boolean) -> Unit,
    expanded: Boolean,
    onExpand: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    contenido: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clickable { onExpand() }
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
            Switch(
                checked = masterOn,
                onCheckedChange = onMaster,
                modifier = Modifier.scale(0.8f)
            )
        }
        if (expanded && masterOn) {
            Column(modifier = Modifier.padding(start = 12.dp)) { contenido() }
        }
    }
}

@Composable
private fun GroupedSectionTitle(title: String, color: Color = Color(0xFF0F172A)) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 28.dp, bottom = 6.dp)
    )
}

@Composable
private fun GroupedDivider(color: Color = Color(0xFFE2E8F0).copy(alpha = 0.8f)) {
    HorizontalDivider(
        color = color,
        thickness = 0.8.dp,
        modifier = Modifier.padding(start = 16.dp)
    )
}

@Composable
private fun GroupedSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    labelColor: Color = Color(0xFF0F172A)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}

@Composable
private fun GroupedListRow(
    label: String,
    value: String = "",
    showChevron: Boolean = true,
    labelColor: Color = Color(0xFF0F172A),
    valueColor: Color = Color(0xFF334155),
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.padding(end = 12.dp)
        )

        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            if (showChevron) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = valueColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun UserRoleFormDialog(
    userToEdit: UserRoleItem?,
    isDark: Boolean,
    companies: List<CompanyEntity> = emptyList(),
    companyFunctions: List<Pair<String, String>> = emptyList(),
    emailsExistentes: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (UserRoleItem) -> Unit
) {
    var name by remember { mutableStateOf(userToEdit?.name ?: "") }
    var email by remember { mutableStateOf(userToEdit?.email ?: "") }
    var pin by remember { mutableStateOf(userToEdit?.pin ?: "") }
    var pinVisible by remember { mutableStateOf(false) }
    var confirmPin by remember { mutableStateOf(userToEdit?.pin ?: "") }
    var role by remember { mutableStateOf(userToEdit?.role ?: "Cajero") }
    var selectedCompanyId by remember {
        mutableStateOf(userToEdit?.companyId ?: companies.firstOrNull()?.id ?: 0)
    }

    val dlgContext = androidx.compose.ui.platform.LocalContext.current
    val empresaSeleccionada = companies.firstOrNull { it.id == selectedCompanyId }
    val codigoEmpresaDlg = empresaSeleccionada?.code ?: userToEdit?.companyCode.orEmpty()
    // El correo no puede repetirse en otro usuario (al editar se excluye el propio).
    val correoDuplicado = email.trim().isNotBlank() &&
        email.trim().lowercase() in emailsExistentes.map { it.lowercase() }.toSet()
    val puedeGuardar = name.isNotBlank() && emailValido(email) && !correoDuplicado &&
        pinCumpleRequisitos(pin) && confirmPin == pin &&
        (userToEdit != null || companies.isEmpty() || empresaSeleccionada != null)

    val rolesBase = listOf("Administrador", "Cajero", "Mesero", "Barman", "Supervisor")
    var rolesCustom by remember(codigoEmpresaDlg) { mutableStateOf(cargarRolesPersonalizados(dlgContext, codigoEmpresaDlg)) }
    val roles = rolesBase + rolesCustom.filter { it !in rolesBase }
    var showAddRole by remember { mutableStateOf(false) }
    var newRoleName by remember { mutableStateOf("") }

    val funcionesPorRol: (String) -> Set<String> = { r ->
        companyFunctions.filter {
            rolDeFuncion(it.second).equals(r, ignoreCase = true) || rolDeFuncion(it.second).isEmpty()
        }.map { it.first }.toSet()
    }
    var selectedFunctions by remember {
        mutableStateOf(
            if (userToEdit?.functionsJson?.isNotBlank() == true)
                parseCompanyFunctions(userToEdit.functionsJson).map { it.first }.toSet()
            else funcionesPorRol(userToEdit?.role ?: "Cajero")
        )
    }

    var dockAccess by remember {
        mutableStateOf(
            userToEdit?.dockAccess ?: mapOf("Inicio" to true, "Ventas" to true, "Finanzas" to true, "Inventario" to false)
        )
    }

    var moduleAccess by remember {
        mutableStateOf(
            userToEdit?.moduleAccess ?: mapOf(
                "Reportes y Analytics" to false,
                "Control de Turnos y Caja" to true,
                "Facturación Electrónica DIAN" to true,
                "Happy Hour & Promociones" to true,
                "Venta por Mesa & Comandero" to true,
                "División de Cuentas (Split)" to true,
                "Agente IA Kapta Assistant" to false
            )
        )
    }

    // Granularidad por vista/sección ("Funciones").
    val seccionesInit = userToEdit?.secciones ?: seccionesPorRol(userToEdit?.role ?: "Cajero")
    var resumenSel by remember { mutableStateOf(seccionesInit.resumen) }
    var accionesSel by remember { mutableStateOf(seccionesInit.acciones) }
    var alertasOn by remember { mutableStateOf(seccionesInit.alertas) }
    var ventasResumenSel by remember { mutableStateOf(seccionesInit.ventasResumen) }
    var rankingOn by remember { mutableStateOf(seccionesInit.ventasRanking) }
    var verMasOn by remember { mutableStateOf(seccionesInit.ventasVerMas) }
    var verInvOn by remember { mutableStateOf(seccionesInit.ventasVerInventario) }
    var pdfOn by remember { mutableStateOf(seccionesInit.finPdf) }
    var filtrosSel by remember { mutableStateOf(seccionesInit.finFiltros) }
    var finVentasOn by remember { mutableStateOf(seccionesInit.finVentas) }
    var finGastosOn by remember { mutableStateOf(seccionesInit.finGastos) }
    var finRegistrarOn by remember { mutableStateOf(seccionesInit.finRegistrar) }
    var invCarga by remember { mutableStateOf(seccionesInit.invCarga) }
    var invMov by remember { mutableStateOf(seccionesInit.invMovimientos) }
    var invCrear by remember { mutableStateOf(seccionesInit.invCrear) }
    var invEditar by remember { mutableStateOf(seccionesInit.invEditar) }
    var invEliminar by remember { mutableStateOf(seccionesInit.invEliminar) }
    var invGuardar by remember { mutableStateOf(seccionesInit.invGuardar) }
    var invHacer by remember { mutableStateOf(seccionesInit.invHacer) }
    var invLectura by remember { mutableStateOf(seccionesInit.invLectura) }
    var expandedVista by remember { mutableStateOf<String?>(null) }

    fun seccionesActuales() = UserSecciones(
        resumen = resumenSel, acciones = accionesSel, alertas = alertasOn,
        ventasResumen = ventasResumenSel, ventasRanking = rankingOn,
        ventasVerMas = verMasOn, ventasVerInventario = verInvOn,
        finPdf = pdfOn, finFiltros = filtrosSel, finVentas = finVentasOn,
        finGastos = finGastosOn, finRegistrar = finRegistrarOn,
        invCarga = invCarga, invMovimientos = invMov, invCrear = invCrear,
        invEditar = invEditar, invEliminar = invEliminar, invGuardar = invGuardar,
        invHacer = invHacer, invLectura = invLectura
    )

    fun aplicarSecciones(s: UserSecciones) {
        resumenSel = s.resumen; accionesSel = s.acciones; alertasOn = s.alertas
        ventasResumenSel = s.ventasResumen; rankingOn = s.ventasRanking
        verMasOn = s.ventasVerMas; verInvOn = s.ventasVerInventario
        pdfOn = s.finPdf; filtrosSel = s.finFiltros; finVentasOn = s.finVentas
        finGastosOn = s.finGastos; finRegistrarOn = s.finRegistrar
        invCarga = s.invCarga; invMov = s.invMovimientos; invCrear = s.invCrear
        invEditar = s.invEditar; invEliminar = s.invEliminar; invGuardar = s.invGuardar
        invHacer = s.invHacer; invLectura = s.invLectura
    }

    fun dockPorRol(r: String): Map<String, Boolean> = when (r) {
        "Administrador", "Supervisor" -> mapOf("Inicio" to true, "Ventas" to true, "Finanzas" to true, "Inventario" to true)
        "Cajero" -> mapOf("Inicio" to true, "Ventas" to true, "Finanzas" to true, "Inventario" to false)
        else -> mapOf("Inicio" to true, "Ventas" to true, "Finanzas" to false, "Inventario" to false)
    }

    fun modulosPorRol(r: String): Map<String, Boolean> = when (r) {
        "Administrador", "Supervisor" -> mapOf(
            "Reportes y Analytics" to true, "Control de Turnos y Caja" to true,
            "Facturación Electrónica DIAN" to true, "Happy Hour & Promociones" to true,
            "Venta por Mesa & Comandero" to true, "División de Cuentas (Split)" to true,
            "Agente IA Kapta Assistant" to true
        )
        "Cajero" -> mapOf(
            "Reportes y Analytics" to false, "Control de Turnos y Caja" to true,
            "Facturación Electrónica DIAN" to true, "Happy Hour & Promociones" to true,
            "Venta por Mesa & Comandero" to true, "División de Cuentas (Split)" to true,
            "Agente IA Kapta Assistant" to false
        )
        else -> mapOf(
            "Reportes y Analytics" to false, "Control de Turnos y Caja" to false,
            "Facturación Electrónica DIAN" to false, "Happy Hour & Promociones" to true,
            "Venta por Mesa & Comandero" to true, "División de Cuentas (Split)" to true,
            "Agente IA Kapta Assistant" to false
        )
    }

    /** Aplica un rol: configuración guardada si existe, si no los valores por defecto del rol. */
    fun aplicarRol(r: String) {
        role = r
        selectedFunctions = funcionesPorRol(r)
        val cfg = cargarCfgRol(dlgContext, codigoEmpresaDlg, r)
        if (cfg != null && (cfg.dock != null || cfg.modulos != null || cfg.secciones != null || cfg.funciones != null)) {
            cfg.dock?.let { dockAccess = it }
            cfg.modulos?.let { moduleAccess = it }
            cfg.funciones?.let { selectedFunctions = it.toSet() }
            cfg.secciones?.let { aplicarSecciones(it) }
        } else {
            dockAccess = dockPorRol(r)
            moduleAccess = modulosPorRol(r)
            aplicarSecciones(seccionesPorRol(r))
        }
    }

    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shadowElevation = 16.dp
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
                    Text(
                        text = if (userToEdit == null) "Crear Usuario" else "Permisos de Usuario",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de empresa (solo al crear y si hay varias)
                if (userToEdit == null && companies.isNotEmpty()) {
                    Text("Empresa del Usuario *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        companies.forEach { c ->
                            val isSel = selectedCompanyId == c.id
                            Surface(
                                onClick = { selectedCompanyId = c.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFF4F46E5) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = c.name.ifBlank { c.code },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Empleado *") },
                    supportingText = { Text("Será también el Usuario para iniciar sesión", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val correoOk = email.isBlank() || emailValido(email)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico *") },
                    singleLine = true,
                    isError = !correoOk || correoDuplicado,
                    supportingText = {
                        if (correoDuplicado) Text("Este correo ya está registrado en otro usuario", fontSize = 11.sp, color = Color(0xFFEF4444))
                        else if (!correoOk) Text("El correo no es válido (ej. usuario@empresa.com)", fontSize = 11.sp, color = Color(0xFFEF4444))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Contraseña *") },
                    placeholder = { Text("Ej. Clave123*") },
                    singleLine = true,
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pinVisible) "Ocultar contraseña" else "Ver contraseña",
                                tint = textSecondary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                RequisitoClave(cumple = pinTieneMayus(pin), texto = "Mínimo una letra mayúscula", textSecondary = textSecondary)
                RequisitoClave(cumple = pinTieneMinus(pin), texto = "Mínimo una letra minúscula", textSecondary = textSecondary)
                RequisitoClave(cumple = pinTieneNumero(pin), texto = "Mínimo un número", textSecondary = textSecondary)
                RequisitoClave(cumple = pinTieneEspecial(pin), texto = "Mínimo un carácter especial", textSecondary = textSecondary)

                Spacer(modifier = Modifier.height(10.dp))

                val coincide = confirmPin == pin
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Verificación de contraseña *") },
                    singleLine = true,
                    isError = confirmPin.isNotBlank() && !coincide,
                    supportingText = {
                        if (confirmPin.isNotBlank() && !coincide) Text("Las contraseñas no coinciden", fontSize = 11.sp, color = Color(0xFFEF4444))
                    },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Asignar Rol de Acceso", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        roles.forEach { r ->
                            val isSel = role == r
                            Surface(
                                onClick = { aplicarRol(r) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFF4F46E5) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = r,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = { newRoleName = ""; showAddRole = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                if (showAddRole) {
                    AlertDialog(
                        onDismissRequest = { showAddRole = false },
                        title = { Text("Nuevo rol personalizado", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        text = {
                            OutlinedTextField(
                                value = newRoleName,
                                onValueChange = { newRoleName = it },
                                label = { Text("Nombre del rol") },
                                placeholder = { Text("Ej. Bartender nocturno") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val limpio = newRoleName.trim()
                                    if (limpio.isNotBlank()) {
                                        guardarRolPersonalizado(dlgContext, codigoEmpresaDlg, limpio)
                                        rolesCustom = cargarRolesPersonalizados(dlgContext, codigoEmpresaDlg)
                                        showAddRole = false
                                        aplicarRol(limpio)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                                enabled = newRoleName.trim().isNotBlank()
                            ) { Text("Guardar rol", color = Color.White, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddRole = false }) { Text("Cancelar") }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Funciones", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(
                    "Activa la vista y despliega para escoger tarjetas y botones una a una.",
                    fontSize = 11.sp, color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // INICIO
                VistaExpandablePermiso(
                    titulo = "Inicio",
                    masterOn = dockAccess["Inicio"] == true,
                    onMaster = { dockAccess = dockAccess.toMutableMap().apply { put("Inicio", it) } },
                    expanded = expandedVista == "Inicio",
                    onExpand = { expandedVista = if (expandedVista == "Inicio") null else "Inicio" },
                    textPrimary = textPrimary, textSecondary = textSecondary
                ) {
                    Text("Resumen general", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    RESUMEN_INICIO_ITEMS.forEach { item ->
                        FilaCheckPermiso(
                            titulo = item.titulo,
                            checked = item.key in resumenSel,
                            onToggle = {
                                resumenSel = if (item.key in resumenSel) resumenSel - item.key else resumenSel + item.key
                            },
                            textPrimary = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Acciones rápidas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    ACCIONES_INICIO_ITEMS.forEach { item ->
                        FilaCheckPermiso(
                            titulo = item.titulo,
                            checked = item.key in accionesSel,
                            onToggle = {
                                accionesSel = if (item.key in accionesSel) accionesSel - item.key else accionesSel + item.key
                            },
                            textPrimary = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FilaSwitchPermiso(
                        titulo = "Alertas de stock",
                        checked = alertasOn,
                        onChange = { alertasOn = it },
                        textPrimary = textPrimary,
                        fontSize = 12.sp
                    )
                }

                // VENTAS
                VistaExpandablePermiso(
                    titulo = "Ventas",
                    masterOn = dockAccess["Ventas"] == true,
                    onMaster = { dockAccess = dockAccess.toMutableMap().apply { put("Ventas", it) } },
                    expanded = expandedVista == "Ventas",
                    onExpand = { expandedVista = if (expandedVista == "Ventas") null else "Ventas" },
                    textPrimary = textPrimary, textSecondary = textSecondary
                ) {
                    Text("Resumen financiero", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    RESUMEN_VENTAS_ITEMS.forEach { item ->
                        FilaCheckPermiso(
                            titulo = item.titulo,
                            checked = item.key in ventasResumenSel,
                            onToggle = {
                                ventasResumenSel = if (item.key in ventasResumenSel) ventasResumenSel - item.key else ventasResumenSel + item.key
                            },
                            textPrimary = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FilaSwitchPermiso(titulo = "Ranking de productos más vendidos", checked = rankingOn, onChange = { rankingOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Ver más", checked = verMasOn, onChange = { verMasOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Ver Inventario Completo", checked = verInvOn, onChange = { verInvOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                }

                // FINANZAS
                VistaExpandablePermiso(
                    titulo = "Finanzas",
                    masterOn = dockAccess["Finanzas"] == true,
                    onMaster = { dockAccess = dockAccess.toMutableMap().apply { put("Finanzas", it) } },
                    expanded = expandedVista == "Finanzas",
                    onExpand = { expandedVista = if (expandedVista == "Finanzas") null else "Finanzas" },
                    textPrimary = textPrimary, textSecondary = textSecondary
                ) {
                    FilaSwitchPermiso(titulo = "Botón Exportar a PDF", checked = pdfOn, onChange = { pdfOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Filtros", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    FILTROS_FINANZAS_ITEMS.forEach { item ->
                        FilaCheckPermiso(
                            titulo = item.titulo,
                            checked = item.key in filtrosSel,
                            onToggle = {
                                filtrosSel = if (item.key in filtrosSel) filtrosSel - item.key else filtrosSel + item.key
                            },
                            textPrimary = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FilaSwitchPermiso(titulo = "Ventas", checked = finVentasOn, onChange = { finVentasOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Gastos", checked = finGastosOn, onChange = { finGastosOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Registrar Gasto", checked = finRegistrarOn, onChange = { finRegistrarOn = it }, textPrimary = textPrimary, fontSize = 12.sp)
                }

                // INVENTARIO
                VistaExpandablePermiso(
                    titulo = "Inventario",
                    masterOn = dockAccess["Inventario"] == true,
                    onMaster = { dockAccess = dockAccess.toMutableMap().apply { put("Inventario", it) } },
                    expanded = expandedVista == "Inventario",
                    onExpand = { expandedVista = if (expandedVista == "Inventario") null else "Inventario" },
                    textPrimary = textPrimary, textSecondary = textSecondary
                ) {
                    fun activarInv(fn: (Boolean) -> Unit): (Boolean) -> Unit = { v ->
                        fn(v)
                        if (v) invLectura = false
                    }
                    FilaSwitchPermiso(titulo = "Botón Carga masiva", checked = invCarga, onChange = activarInv { invCarga = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Movimientos", checked = invMov, onChange = activarInv { invMov = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Crear producto", checked = invCrear, onChange = activarInv { invCrear = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Editar productos", checked = invEditar, onChange = activarInv { invEditar = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Eliminar productos", checked = invEliminar, onChange = activarInv { invEliminar = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Guardar inventario", checked = invGuardar, onChange = activarInv { invGuardar = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    FilaSwitchPermiso(titulo = "Botón Hacer inventario", checked = invHacer, onChange = activarInv { invHacer = it }, textPrimary = textPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    FilaSwitchPermiso(
                        titulo = "Modo lectura (solo ver)",
                        checked = invLectura,
                        onChange = { v ->
                            invLectura = v
                            if (v) {
                                invCarga = false; invMov = false; invCrear = false
                                invEditar = false; invEliminar = false
                                invGuardar = false; invHacer = false
                            }
                        },
                        textPrimary = textPrimary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Funciones avanzadas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                moduleAccess.forEach { (mod, enabled) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mod, fontSize = 12.sp, color = textPrimary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { newVal ->
                                moduleAccess = moduleAccess.toMutableMap().apply { put(mod, newVal) }
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (companyFunctions.isNotEmpty()) {
                    Text("Funciones personalizadas (IA)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Asignadas según el rol del usuario. Puedes ajustarlas manualmente.",
                        fontSize = 11.sp, color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    companyFunctions.forEach { (nombre, desc) ->
                        val rol = rolDeFuncion(desc)
                        val checked = selectedFunctions.contains(nombre)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedFunctions = selectedFunctions.toMutableSet().apply { if (checked) remove(nombre) else add(nombre) } },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(nombre, fontSize = 12.sp, color = textPrimary)
                                if (rol.isNotBlank()) {
                                    Text("Rol: $rol", fontSize = 10.sp, color = textSecondary)
                                }
                            }
                            Checkbox(checked = checked, onCheckedChange = {
                                selectedFunctions = selectedFunctions.toMutableSet().apply { if (it) add(nombre) else remove(nombre) }
                            })
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (puedeGuardar) {
                            val item = UserRoleItem(
                                id = userToEdit?.id ?: (100..999).random(),
                                name = name.trim(),
                                email = email.trim(),
                                role = role,
                                pin = pin.trim(),
                                companyId = empresaSeleccionada?.id ?: userToEdit?.companyId ?: 0,
                                companyCode = empresaSeleccionada?.code ?: userToEdit?.companyCode.orEmpty(),
                                dockAccess = dockAccess,
                                moduleAccess = moduleAccess,
                                functionsJson = org.json.JSONArray(selectedFunctions.toList()).toString(),
                                secciones = seccionesActuales()
                            )
                            // Guarda el tipo de rol con su configuración para reusarlo.
                            val codigoCfg = item.companyCode.ifBlank { codigoEmpresaDlg }
                            if (codigoCfg.isNotBlank()) {
                                guardarCfgRol(
                                    context = dlgContext,
                                    companyCode = codigoCfg,
                                    rol = role,
                                    dock = dockAccess,
                                    modulos = moduleAccess,
                                    funciones = selectedFunctions.toList(),
                                    secciones = seccionesActuales()
                                )
                            }
                            onSave(item)
                        }
                    },
                    enabled = puedeGuardar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Guardar Usuario y Permisos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

