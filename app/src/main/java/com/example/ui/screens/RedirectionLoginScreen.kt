package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.R
import com.example.ui.KaptaViewModel
import com.example.ui.components.DynamicCodeDock
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.KaptaLogoHeader

data class CountryItem(val name: String, val flagRes: Int)

@Composable
fun RedirectionLoginScreen(
    viewModel: KaptaViewModel,
    onNavigateToAdminLogin: () -> Unit,
    onNavigateToCompanyLogin: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsSesion = remember { context.getSharedPreferences("kapta_sesion", android.content.Context.MODE_PRIVATE) }
    val dynamicClave by viewModel.dynamicClave.collectAsState()
    val companies by viewModel.companies.collectAsState()
    var selectedCountry by remember { mutableStateOf(CountryItem("Colombia", R.drawable.flag_colombia)) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var companyCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val countries = listOf(
        CountryItem("Colombia", R.drawable.flag_colombia),
        CountryItem("México", R.drawable.flag_mexico),
        CountryItem("Perú", R.drawable.flag_peru),
        CountryItem("Chile", R.drawable.flag_chile),
        CountryItem("Argentina", R.drawable.flag_argentina),
        CountryItem("Ecuador", R.drawable.flag_ecuador)
    )

    val empresaUpper = companyCodeInput.trim().uppercase()
    val adminIniciado = if (empresaUpper.isNotBlank()) prefsSesion.getBoolean("admin_iniciado_$empresaUpper", false) else false
    LaunchedEffect(empresaUpper, adminIniciado) {
        if (adminIniciado && empresaUpper.isNotBlank()) {
            viewModel.iniciarClaveParaEmpresa(empresaUpper)
        }
    }

    EtherealBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (adminIniciado && empresaUpper.isNotBlank() && dynamicClave != null) {
                val compForDock = companies.find { it.code.equals(empresaUpper, ignoreCase = true) }
                val dockPrimary = try { Color(android.graphics.Color.parseColor(compForDock?.primaryColorHex ?: "#4F46E5")) } catch (_: Exception) { Color(0xFF4F46E5) }
                val dockSecondary = try { Color(android.graphics.Color.parseColor(compForDock?.secondaryColorHex ?: "#3B82F6")) } catch (_: Exception) { Color(0xFF3B82F6) }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                    DynamicCodeDock(clave = dynamicClave, primaryColor = dockPrimary, secondaryColor = dockSecondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Header Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KaptaLogoHeader(showSlogan = true, fontSize = 42)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Central Glassmorphism Container Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                backgroundColor = Color.White.copy(alpha = 0.35f),
                borderColor = Color.White.copy(alpha = 0.80f),
                borderWidth = 1.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 1. Country Selection Section
                    Text(
                        text = "Selecciona el país de tu negocio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .border(0.6.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(18.dp))
                                    .clickable { countryDropdownExpanded = !countryDropdownExpanded }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(selectedCountry.flagRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = selectedCountry.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (countryDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Desplegar países",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = countryDropdownExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White)
                                        .border(0.6.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                ) {
                                    countries.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCountry = item
                                                    countryDropdownExpanded = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(item.flagRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (item.name == selectedCountry.name) FontWeight.Bold else FontWeight.Medium,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        if (index < countries.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Color(0xFFE2E8F0))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 2. Company Identifier Section
                    Text(
                        text = "Ingresa el identificador de tu negocio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User text input area
                            Box(modifier = Modifier.weight(1f)) {
                                if (companyCodeInput.isEmpty()) {
                                    Text(
                                        text = "tunegocio",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 15.sp
                                    )
                                }
                                BasicTextField(
                                    value = companyCodeInput,
                                    onValueChange = {
                                        companyCodeInput = it
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color(0xFFCBD5E1))
                            )

                            // Fixed domain text
                            Text(
                                text = ".kaptaia.com",
                                color = Color(0xFF475569),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(0.6.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFDC2626),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 3. Action Button (Pill button with Blue-Purple gradient)
                    Button(
                        onClick = {
                            val code = companyCodeInput.trim()
                            if (code.equals("aptAdmin", ignoreCase = true)) {
                                errorMessage = null
                                onNavigateToAdminLogin()
                            } else if (code.isNotBlank()) {
                                coroutineScope.launch {
                                    val exists = viewModel.checkCompanyExists(code)
                                    if (exists) {
                                        errorMessage = null
                                        context
                                            .getSharedPreferences("kapta_session", android.content.Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("ultimo_codigo_negocio", code.uppercase())
                                            .apply()
                                        onNavigateToCompanyLogin(code)
                                    } else {
                                        errorMessage = "⚠️ Negocio no encontrado. El identificador no existe en el sistema."
                                    }
                                }
                            } else {
                                errorMessage = "Por favor ingresa un identificador de negocio"
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFF818CF8), spotColor = Color(0xFF60A5FA))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF60A5FA), // Light bright blue
                                            Color(0xFF818CF8), // Soft indigo
                                            Color(0xFFC084FC)  // Soft purple
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Continuar",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Continuar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Form Footer Divider with Central Circle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(8.dp)
                                .border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Link Text
                    TextButton(
                        onClick = {
                            // Quick prefill "la14" demo for convenience
                            companyCodeInput = "la14"
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "¿No encuentras tu empresa?",
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "KAPTA AI © 2026. Todos los derechos reservados.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

