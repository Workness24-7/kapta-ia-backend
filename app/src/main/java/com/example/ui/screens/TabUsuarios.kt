package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CompanyUserEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSSectionHeader
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.iOSSecondaryGroupedBackgroundLight
import com.example.ui.theme.iOSSecondaryGroupedBackgroundDark
import com.example.ui.theme.iOSSeparatorLight
import com.example.ui.theme.iOSSeparatorDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabUsuarios(viewModel: KaptaViewModel) {
    val companies by viewModel.companies.collectAsState()
    val users by viewModel.users.collectAsState()
    var userSearchQuery by remember { mutableStateOf("") }
    var passwordsVisible by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }
    var userToResetPassword by remember { mutableStateOf<CompanyUserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<CompanyUserEntity?>(null) }
    var userForDetail by remember { mutableStateOf<CompanyUserEntity?>(null) }
    var newPasswordInput by remember { mutableStateOf("") }

    val distinctUsers = remember(users) {
        users.distinctBy { user ->
            Pair(user.companyCode.lowercase(), user.email.lowercase().ifBlank { user.username.lowercase() })
        }
    }

    val filteredUsers = distinctUsers.filter { user ->
        val matchingComp = companies.find { it.id == user.companyId || it.code.equals(user.companyCode, ignoreCase = true) }
        val compName = matchingComp?.name ?: user.companyCode
        val compStatus = matchingComp?.getEffectiveStatus() ?: ""

        userSearchQuery.isEmpty() ||
                user.name.contains(userSearchQuery, ignoreCase = true) ||
                user.username.contains(userSearchQuery, ignoreCase = true) ||
                user.email.contains(userSearchQuery, ignoreCase = true) ||
                user.companyCode.contains(userSearchQuery, ignoreCase = true) ||
                compName.contains(userSearchQuery, ignoreCase = true) ||
                compStatus.contains(userSearchQuery, ignoreCase = true)
    }

    EtherealBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                iOSLargeTitle(
                    title = "Usuarios",
                    subtitle = "Administradores, Líderes y Empleados por empresa"
                )

                Spacer(modifier = Modifier.height(14.dp))

                IOSSearchField(
                    query = userSearchQuery,
                    onQueryChange = { userSearchQuery = it }
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    iOSSectionHeader("Seguridad")
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Recomendación de Seguridad",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cada empresa debe tener al menos 1 usuario Administrador activo.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    iOSSectionHeader("Gestión")
                }

                items(filteredUsers, key = { it.id }) { user ->
                    val isPassVisible = passwordsVisible[user.id] ?: false
                    val matchingCompany = companies.find { it.id == user.companyId || it.code.equals(user.companyCode, ignoreCase = true) }
                    val companyName = matchingCompany?.name ?: user.companyCode
                    val companyStatus = matchingCompany?.getEffectiveStatus() ?: "Activo"

                    val companyStatusColor = when {
                        companyStatus.contains("Acti", ignoreCase = true) -> Color(0xFF34C759)
                        companyStatus.contains("Venc", ignoreCase = true) -> Color(0xFFFF9F0A)
                        companyStatus.contains("Suspen", ignoreCase = true) -> Color(0xFFFF453A)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { userForDetail = user },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        UserCell(
                            user = user,
                            companyName = companyName,
                            companyStatus = companyStatus,
                            companyStatusColor = companyStatusColor,
                            isPassVisible = isPassVisible,
                            onTogglePass = {
                                val current = passwordsVisible.toMutableMap()
                                current[user.id] = !isPassVisible
                                passwordsVisible = current
                            },
                            onResetPassword = { userToResetPassword = user },
                            onDelete = { userToDelete = user }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Vista detallada del usuario
    userForDetail?.let { detailUser ->
        ModalBottomSheet(
            onDismissRequest = { userForDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            UserDetailContent(user = detailUser, companyName = companies.find { it.id == detailUser.companyId || it.code.equals(detailUser.companyCode, ignoreCase = true) }?.name ?: detailUser.companyCode)
        }
    }

    // Delete User Confirmation Dialog
    if (userToDelete != null) {        val user = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Eliminar Usuario", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar al usuario '${user.name}' (${user.email.ifBlank { user.username }}) de Google Sheets? Esta acción removerá el acceso al sistema.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Reset Password Dialog
    if (userToResetPassword != null) {
        val user = userToResetPassword!!
        AlertDialog(
            onDismissRequest = { userToResetPassword = null },
            title = { Text("Restablecer Contraseña") },
            text = {
                Column {
                    Text("Ingresa la nueva contraseña para el usuario ${user.username} (${user.name}):")
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
                            viewModel.resetUserPassword(user, newPasswordInput)
                            userToResetPassword = null
                            newPasswordInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToResetPassword = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun UserDetailContent(user: CompanyUserEntity, companyName: String) {
    val isDark = LocalIsDarkMode.current
    val separator = if (isDark) iOSSeparatorDark else iOSSeparatorLight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text(
            text = "Detalle del Usuario",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Empresa", companyName, separator)
                DetailRow("ID de Usuario", user.idUsuario.ifBlank { "—" }, separator)
                DetailRow("Nombre", user.name, separator)
                DetailRow("Usuario", user.username, separator)
                DetailRow("Correo", user.email, separator)
                DetailRow("Contraseña", user.password, separator)
                DetailRow("Rol", user.role, separator, isLast = true)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Estado", user.status, separator)
                DetailRow("Fecha de Creación", user.creationDate.ifBlank { "—" }, separator)
                DetailRow("Último Acceso", user.lastAccess.ifBlank { "—" }, separator)
                DetailRow("Cambio de Estado", user.fechaCambioEstado.ifBlank { "—" }, separator)
                DetailRow("Motivo del Cambio", user.motivoCambio.ifBlank { "—" }, separator)
                DetailRow("Cambiado Por", user.cambiadoPor.ifBlank { "—" }, separator, isLast = true)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    separatorColor: Color,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(separatorColor)
            )
        }
    }
}

@Composable
private fun IOSSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val container = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE9E9EB)
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (query.isEmpty()) "Buscar usuario, correo, empresa..." else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            maxLines = 1
        )
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
                shape = RoundedCornerShape(0.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UserCell(
    user: CompanyUserEntity,
    companyName: String,
    companyStatus: String,
    companyStatusColor: Color,
    isPassVisible: Boolean,
    onTogglePass: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    Column(modifier = Modifier.padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = companyStatusColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = companyName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = companyStatusColor
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "• $companyStatus",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = companyStatusColor.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = user.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (user.role) {
                            "Administrador" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            "Líder" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = user.role,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (user.role) {
                        "Administrador" -> MaterialTheme.colorScheme.primary
                        "Líder" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Usuario: ${user.username}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Correo: ${user.email}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "Contraseña: " + if (isPassVisible) user.password else "••••••••",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider(
            color = if (isDark) iOSSeparatorDark else iOSSeparatorLight,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onTogglePass) {
                Icon(
                    imageVector = if (isPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Pass",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onResetPassword,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restablecer", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar Usuario",
                    tint = Color(0xFFFF453A)
                )
            }
        }
    }
}

@Composable
private fun HorizontalDivider(
    color: Color,
    thickness: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}