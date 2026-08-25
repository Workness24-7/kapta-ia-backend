package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import android.util.Log
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.KaptaLogoHeader

import com.example.util.hayConexion
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.clickable

data class LoginCountryItem(val code: String, val name: String, val flag: String)

@Composable
fun CompanyLoginScreen(
    viewModel: KaptaViewModel,
    companyCode: String,
    onLoginToPosSuccess: (CompanyEntity) -> Unit,
    onBackToRedirection: () -> Unit
) {
    val countriesList = listOf(
        LoginCountryItem("CO", "Colombia", "🇨🇴"),
        LoginCountryItem("MX", "México", "🇲🇽"),
        LoginCountryItem("CL", "Chile", "🇨🇱"),
        LoginCountryItem("PE", "Perú", "🇵🇪"),
        LoginCountryItem("AR", "Argentina", "🇦🇷"),
        LoginCountryItem("EC", "Ecuador", "🇪🇨")
    )

    var selectedCountry by remember { mutableStateOf(countriesList[0]) }
    var companyCodeInput by remember { mutableStateOf(companyCode) }
    var company by remember { mutableStateOf<CompanyEntity?>(null) }
    var userInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var recordarme by remember { mutableStateOf(false) }
    var loginErrorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("kapta_login", android.content.Context.MODE_PRIVATE) }

    // Volver al login de redirección solo con 5 pulsaciones de atrás consecutivas.
    var backPressCount by remember { mutableStateOf(0) }
    var lastBackPress by remember { mutableStateOf(0L) }
    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress > 2000L) backPressCount = 0
        lastBackPress = now
        backPressCount++
        if (backPressCount >= 5) {
            backPressCount = 0
            onBackToRedirection()
        }
    }

    LaunchedEffect(Unit) {
        userInput = prefs.getString("usuario", "") ?: ""
        passwordInput = prefs.getString("clave", "") ?: ""
        recordarme = prefs.getBoolean("recordarme", false)
    }

    LaunchedEffect(companyCodeInput) {
        val codeTrim = companyCodeInput.trim()
        // Busca la empresa real (nombre correcto) en caché y Room antes de usar placeholder
        val found = viewModel.companies.value.find { it.code.equals(codeTrim, ignoreCase = true) }
            ?: viewModel.empresaPorCodigo(codeTrim)
        company = found ?: CompanyEntity(
            code = codeTrim,
            name = codeTrim.replaceFirstChar { it.uppercase() },
            status = "Activo",
            plan = "Premium"
        )
        viewModel.ensureDefaultUsersForCompany(codeTrim, found?.id ?: 0)
    }
    val comp = company ?: return

    val primaryBrandColor = try {
        Color(android.graphics.Color.parseColor(comp.primaryColorHex))
    } catch (e: Exception) {
        Color(0xFF4F46E5)
    }

    val secondaryBrandColor = try {
        Color(android.graphics.Color.parseColor(comp.secondaryColorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val tertiaryBrandColor = try {
        Color(android.graphics.Color.parseColor(comp.tertiaryColorHex))
    } catch (e: Exception) {
        Color(0xFF10B981)
    }

    val neutralBrandColor = try {
        Color(android.graphics.Color.parseColor(comp.neutralColorHex))
    } catch (e: Exception) {
        Color(0xFF0F172A)
    }

    val customFontFamily = when (comp.fontType) {
        "Poppins", "SansSerif" -> androidx.compose.ui.text.font.FontFamily.SansSerif
        "Playfair", "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
        "Monospace", "Tech" -> androidx.compose.ui.text.font.FontFamily.Monospace
        "Plus Jakarta Sans", "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
        else -> androidx.compose.ui.text.font.FontFamily.Default
    }

    EtherealBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToRedirection) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1E293B))
                }
                Text(
                    text = "Cambiar empresa",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = customFontFamily,
                    color = Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Top Official Business Logo (el subido en el form; fallback a KAPTA IA)
            val logoValido = remember(comp.logoUrl) {
                when {
                    comp.logoUrl.isBlank() -> false
                    comp.logoUrl.startsWith("file://") -> java.io.File(comp.logoUrl.removePrefix("file://")).exists()
                    else -> true
                }
            }
            if (logoValido) {
                AsyncImage(
                    model = comp.logoUrl,
                    contentDescription = comp.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth(0.8f)
                        .padding(horizontal = 16.dp)
                )
            } else {
                KaptaLogoHeader(showSlogan = false, fontSize = 56)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = primaryBrandColor.copy(alpha = 0.20f),
                        spotColor = secondaryBrandColor.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.55f))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                primaryBrandColor.copy(alpha = 0.40f),
                                secondaryBrandColor.copy(alpha = 0.40f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Nombre del negocio en la parte superior del dock
                    Text(
                        text = comp.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = customFontFamily,
                        color = neutralBrandColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = secondaryBrandColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, secondaryBrandColor.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = "${comp.code}.kaptaia.com",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = customFontFamily,
                            color = secondaryBrandColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Email / User Field
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("Correo o usuario (ej. admin@empresa.com)", fontFamily = customFontFamily) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = primaryBrandColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = neutralBrandColor,
                            unfocusedTextColor = neutralBrandColor,
                            focusedBorderColor = primaryBrandColor,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White.copy(alpha = 0.90f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.80f),
                            cursorColor = primaryBrandColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Contraseña", fontFamily = customFontFamily) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = primaryBrandColor) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = neutralBrandColor,
                            unfocusedTextColor = neutralBrandColor,
                            focusedBorderColor = primaryBrandColor,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White.copy(alpha = 0.90f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.80f),
                            cursorColor = primaryBrandColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recuérdame: guarda usuario y clave en este dispositivo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { recordarme = !recordarme }
                    ) {
                        Checkbox(
                            checked = recordarme,
                            onCheckedChange = { recordarme = it }
                        )
                        Text(
                            text = "Recuérdame en este dispositivo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = customFontFamily,
                            color = Color(0xFF475569)
                        )
                    }

                    if (loginErrorMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = loginErrorMsg!!,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val cCode = comp.code.trim()
                            val uVal = userInput.trim()
                            val pVal = passwordInput.trim()
                            if (uVal.isBlank() || pVal.isBlank()) {
                                loginErrorMsg = "Por favor completa Correo y Contraseña"
                            } else {
                                loginErrorMsg = null
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    // Online: el servidor es autoridad (credenciales, bloqueo, hash).
                                    // Offline: login local estricto contra usuarios cacheados en Room;
                                    // ventas/gastos/inventario pendientes se sincronizan solos al reconectar.
                                    var online = hayConexion(context)
                                    if (online) {
                                        val resultado = viewModel.loginWithServer(
                                            pais = selectedCountry.code,
                                            codigo = cCode,
                                            correo = uVal,
                                            password = pVal
                                        )
                                        Log.d("CompanyLoginScreen", "Server login idEmpresa: ${resultado.idEmpresa}")
                                        when {
                                            resultado.idEmpresa != null -> {}
                                            resultado.mensajeError != null -> {
                                                loginErrorMsg = resultado.mensajeError
                                                return@launch
                                            }
                                            else -> online = false // servidor inalcanzable pese a haber red
                                        }
                                    }

                                    var user = viewModel.authenticateCompanyUser(cCode, uVal, pVal)
                                    if (user == null) {
                                        if (!online) {
                                            loginErrorMsg = "Sin conexión a internet. Ingresa con un usuario ya registrado en este dispositivo."
                                            return@launch
                                        }
                                        user = CompanyUserEntity(
                                            companyCode = cCode,
                                            companyId = comp.id,
                                            username = uVal,
                                            name = "Administrador",
                                            email = uVal,
                                            password = pVal,
                                            role = "Administrador"
                                        )
                                    }

                                    if (recordarme) {
                                        prefs.edit()
                                            .putString("usuario", uVal)
                                            .putString("clave", pVal)
                                            .putBoolean("recordarme", true)
                                            .apply()
                                    } else {
                                        prefs.edit().clear().apply()
                                    }

                                    viewModel.setCurrentUser(user)
                                    viewModel.setSuperAdminSession(false)
                                    onLoginToPosSuccess(comp)
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(50), ambientColor = primaryBrandColor, spotColor = secondaryBrandColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            primaryBrandColor,
                                            secondaryBrandColor
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ingresar al POS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = customFontFamily,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "KAPTA AI POS Native Terminal v2.4",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

