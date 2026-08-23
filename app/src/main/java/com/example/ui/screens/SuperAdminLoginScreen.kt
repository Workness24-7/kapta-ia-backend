package com.example.ui.screens

import com.example.util.hayConexion
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EtherealBackground
import com.example.ui.components.KaptaLogoHeader

@Composable
fun SuperAdminLoginScreen(
    onLoginSuccess: () -> Unit,
    onBackToRedirection: () -> Unit
) {
    var emailInput by remember { mutableStateOf("AdminMauricio@kaptaia.com") }
    var passwordInput by remember { mutableStateOf("M4ur1C10*") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Header Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KaptaLogoHeader(showSlogan = true, fontSize = 42)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Glassmorphism Main Floating Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                        spotColor = Color(0xFF38BDF8).copy(alpha = 0.10f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.45f))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.90f),
                                Color(0xFFE0E7FF).copy(alpha = 0.70f),
                                Color(0xFFFBCFE8).copy(alpha = 0.70f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // User Avatar Icon with thin blue border
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF))
                            .border(1.5.dp, Color(0xFF60A5FA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar de Usuario",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Bienvenido de nuevo",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "Inicia sesión en tu cuenta de administrador",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Correo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            },
                            placeholder = { Text("Introduce tu correo electrónico", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White.copy(alpha = 0.85f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                                cursorColor = Color(0xFF2563EB)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Contraseña",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Ver contraseña",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            placeholder = { Text("Introduce tu contraseña", color = Color(0xFF94A3B8)) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White.copy(alpha = 0.85f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                                cursorColor = Color(0xFF2563EB)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Options Row: Remember Me & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2563EB),
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                            Text(
                                text = "Recordarme",
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        TextButton(
                            onClick = {},
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Action Button (Sign In - Intense Blue Pill)
                    Button(
                        onClick = {
                            if (!hayConexion(context)) {
                                errorMessage = "Se requiere conexión a internet para acceder al panel de administración."
                            } else {
                                // ponytail: credencial local; mover a servidor cuando exista login de superadmin
                                val emailValido = emailInput.trim()
                                    .equals("AdminMauricio@kaptaia.com", ignoreCase = true)
                                val passValido = passwordInput == "M4ur1C10*"
                                if (!emailValido || !passValido) {
                                    errorMessage = "Usuario o Contraseña incorrecta."
                                } else {
                                    onLoginSuccess()
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFF2563EB), spotColor = Color(0xFF1D4ED8))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF2563EB), // Intense Blue
                                            Color(0xFF1D4ED8)  // Deep Royal Blue
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Iniciar sesión",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Iniciar sesión",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Card Footer Divider with Central Dot
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
                                .padding(horizontal = 10.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF94A3B8))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Security Info Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEFF6FF).copy(alpha = 0.80f))
                            .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Acceso seguro y protegido.\nTus datos están en buenas manos.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation back link
            TextButton(onClick = onBackToRedirection) {
                Text(
                    text = "← Volver a selección de empresa",
                    color = Color(0xFF475569),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bottom Footer
            Text(
                text = "© 2026 KAPTA IA. Todos los derechos reservados.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

