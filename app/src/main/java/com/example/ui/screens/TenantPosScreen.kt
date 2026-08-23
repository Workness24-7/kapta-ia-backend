package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween              
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height               
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.platform.LocalContext
import com.example.util.PdfReportGenerator
import com.example.util.PdfSaleItem
import com.example.util.PdfExpenseItem
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.data.local.entity.FinancialTransactionEntity
import com.example.data.local.entity.PosProductEntity
import com.example.data.local.entity.PosSaleEntity
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSButton
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSPill
import com.example.ui.components.iOSSectionHeader
import com.example.ui.components.iOSSegmented
import com.example.ui.components.UserProfileModal
import com.example.ui.theme.LocalIsDarkMode

// Dynamic Stock Row model for the +Stock Bottom Sheet
private data class StockRowItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var selectedProduct: PosProductEntity? = null,
    var searchQuery: String = "",
    var quantityText: String = "10"
)

// Expense Item model
private data class BusinessExpense(
    val id: Int,
    val concept: String,
    val amount: Double,
    val type: String, // "Administrativo" vs "Recurrente"
    val category: String,
    val date: String
)

// Debtor Item model
private data class DebtorRecord(
    val id: Int,
    val name: String,
    val phone: String = "",
    val amountOwed: Double,
    val concept: String = "",
    val date: String,
    val abonoAmount: Double = 0.0
)

private data class DebtorGroupedProduct(
    val quantity: Int,
    val name: String,
    val subtotal: Double
)

private data class DebtorRawOrderItem(
    val quantity: Int,
    val productName: String,
    val unitPrice: Double,
    val timeStr: String
)

private fun getGroupedProductsForDebtor(name: String, amountOwed: Double): List<DebtorGroupedProduct> {
    return when {
        name.contains("Carlos", ignoreCase = true) -> listOf(
            DebtorGroupedProduct(20, "Cerveza Poker 330ml", 160000.0),
            DebtorGroupedProduct(2, "Aguardiente Tapa Roja 750ml", 215000.0)
        )
        name.contains("Laura", ignoreCase = true) -> listOf(
            DebtorGroupedProduct(3, "Coctel Mojito ClÃ¡sico", 54000.0),
            DebtorGroupedProduct(1, "Tabla de Quesos & Jamones", 31000.0)
        )
        name.contains("AndrÃ©s", ignoreCase = true) -> listOf(
            DebtorGroupedProduct(1, "Botella Ron Viejo de Caldas", 180000.0),
            DebtorGroupedProduct(5, "Cerveza Corona 355ml", 75000.0)
        )
        else -> listOf(
            DebtorGroupedProduct(1, "Consumo General / Productos POS", amountOwed)
        )
    }
}

private fun getRawOrdersForDebtor(name: String, amountOwed: Double, dateStr: String): List<DebtorRawOrderItem> {
    return when {
        name.contains("Carlos", ignoreCase = true) -> listOf(
            DebtorRawOrderItem(10, "Cerveza Poker 330ml", 8000.0, "18:30"),
            DebtorRawOrderItem(10, "Cerveza Poker 330ml", 8000.0, "20:15"),
            DebtorRawOrderItem(1, "Aguardiente Tapa Roja 750ml", 107500.0, "21:00"),
            DebtorRawOrderItem(1, "Aguardiente Tapa Roja 750ml", 107500.0, "22:45")
        )
        name.contains("Laura", ignoreCase = true) -> listOf(
            DebtorRawOrderItem(2, "Coctel Mojito ClÃ¡sico", 18000.0, "19:10"),
            DebtorRawOrderItem(1, "Coctel Mojito ClÃ¡sico", 18000.0, "20:40"),
            DebtorRawOrderItem(1, "Tabla de Quesos & Jamones", 31000.0, "21:00")
        )
        name.contains("AndrÃ©s", ignoreCase = true) -> listOf(
            DebtorRawOrderItem(1, "Botella Ron Viejo de Caldas", 180000.0, "20:00"),
            DebtorRawOrderItem(3, "Cerveza Corona 355ml", 15000.0, "21:30"),
            DebtorRawOrderItem(2, "Cerveza Corona 355ml", 15000.0, "23:00")
        )
        else -> listOf(
            DebtorRawOrderItem(1, "Consumo Registrado", amountOwed, "16:32")
        )
    }
}

data class DebtorHistoryItem(
    val quantity: Int,
    val productName: String,
    val subtotal: Double
)

fun formatCurrency(amount: Double): String = com.example.util.KaptaCurrency.format(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtorsManagementModal(
    debtorsList: MutableList<DebtorRecord>,
    totalDebtorsAmount: Double,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit,
    onPayDebtor: (clientName: String, method: String, amount: Double) -> Unit = { _, _, _ -> }
) {
    var selectedDebtor by remember { mutableStateOf<DebtorRecord?>(null) }
    var showAddDebtorDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GestiÃ³n de Cuentas por Cobrar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${debtorsList.size} deudores â€¢ Total pendiente: ${formatCurrency(totalDebtorsAmount)}",
                        fontSize = 12.sp,
                        color = Color(0xFFFF453A),
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DEBTORS LIST
            if (debtorsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay cuentas por cobrar pendientes.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(debtorsList) { debtor ->
                        val pendingBalance = (debtor.amountOwed - debtor.abonoAmount).coerceAtLeast(0.0)

                        Surface(
                            onClick = { selectedDebtor = debtor },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Arriba a la izquierda: Nombre
                                // Abajo a la izquierda: Fecha
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = debtor.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Fecha: ${debtor.date}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // A la derecha (centrado verticalmente)
                                Column(horizontalAlignment = Alignment.End) {
                                    if (debtor.abonoAmount > 0.0) {
                                        Text(
                                            text = "-${formatCurrency(debtor.abonoAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formatCurrency(pendingBalance),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    } else {
                                        Text(
                                            text = formatCurrency(debtor.amountOwed),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAddDebtorDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Registrar Nuevo Deudor", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // FLOATING DEBTOR DETAIL MODAL
    if (selectedDebtor != null) {
        DebtorDetailFloatingModal(
            debtor = selectedDebtor!!,
            onDismiss = { selectedDebtor = null },
            onPaymentDone = { debtorPaid, paymentMethod ->
                onPayDebtor(debtorPaid.name, paymentMethod, debtorPaid.amountOwed)
                debtorsList.remove(debtorPaid)
                selectedDebtor = null
                onShowToast("Deuda liquidada completamente para ${debtorPaid.name}")
            },
            onAbonoDone = { debtorUpdated, newAbonoValue ->
                val idx = debtorsList.indexOfFirst { it.id == debtorUpdated.id }
                if (idx != -1) {
                    val updatedTotalAbono = debtorUpdated.abonoAmount + newAbonoValue
                    if (updatedTotalAbono >= debtorUpdated.amountOwed) {
                        debtorsList.removeAt(idx)
                        selectedDebtor = null
                        onShowToast("Â¡Abono liquidÃ³ la totalidad de la deuda de ${debtorUpdated.name}!")
                    } else {
                        val newRecord = debtorUpdated.copy(abonoAmount = updatedTotalAbono)
                        debtorsList[idx] = newRecord
                        selectedDebtor = newRecord
                        onShowToast("Abono de ${formatCurrency(newAbonoValue)} registrado para ${debtorUpdated.name}")
                    }
                }
            }
        )
    }

    // ADD NEW DEBTOR DIALOG
    if (showAddDebtorDialog) {
        var newName by remember { mutableStateOf("") }
        var newAmountText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDebtorDialog = false },
            title = { Text("Registrar Nuevo Deudor", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre del Cliente *") },
                        placeholder = { Text("Ej. Carlos Mendoza") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAmountText,
                        onValueChange = { newAmountText = it },
                        label = { Text("Monto Adeudado ($) *") },
                        placeholder = { Text("375000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newAmountText.toDoubleOrNull() ?: 0.0
                        if (newName.isNotBlank() && amount > 0) {
                            val currentDate = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()).format(java.util.Date())
                            debtorsList.add(
                                DebtorRecord(
                                    id = (debtorsList.maxOfOrNull { it.id } ?: 0) + 1,
                                    name = newName.trim(),
                                    phone = "3000000000",
                                    amountOwed = amount,
                                    concept = "Consumo directo",
                                    date = currentDate,
                                    abonoAmount = 0.0
                                )
                            )
                            showAddDebtorDialog = false
                            onShowToast("Deudor $newName registrado con Ã©xito")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtorDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DebtorDetailFloatingModal(
    debtor: DebtorRecord,
    onDismiss: () -> Unit,
    onPaymentDone: (DebtorRecord, String) -> Unit,
    onAbonoDone: (debtor: DebtorRecord, abonoValue: Double) -> Unit
) {
    var isHistoryExpanded by remember { mutableStateOf(false) }

    var activeFlowMode by remember { mutableStateOf("NONE") }
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var abonoInputText by remember { mutableStateOf("") }

    val pendingBalance = (debtor.amountOwed - debtor.abonoAmount).coerceAtLeast(0.0)

    val groupedProducts = remember(debtor) {
        getGroupedProductsForDebtor(debtor.name, debtor.amountOwed)
    }

    val rawOrders = remember(debtor) {
        getRawOrdersForDebtor(debtor.name, debtor.amountOwed, debtor.date)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // 1. CABECERA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = debtor.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = { isHistoryExpanded = !isHistoryExpanded },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = if (isHistoryExpanded) "âŒ" else "ðŸ”„",
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // 2. CUERPO PRINCIPAL
                Text(
                    text = "Resumen de Productos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                groupedProducts.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = formatCurrency(item.subtotal),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // 3. TOTALES
                if (debtor.abonoAmount > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abono",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${formatCurrency(debtor.abonoAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pendiente",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = formatCurrency(pendingBalance),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = formatCurrency(debtor.amountOwed),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. ACCIONES DINÃMICAS
                when (activeFlowMode) {
                    "NONE" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    activeFlowMode = "PAGO"
                                    selectedMethod = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Pago", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    activeFlowMode = "ABONO"
                                    selectedMethod = null
                                    abonoInputText = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Abono", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }

                    "PAGO" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "MÃ©todo de Pago",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { selectedMethod = "Efectivo" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMethod == "Efectivo") Color(0xFF34C759) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("ðŸ’¸", fontSize = 22.sp)
                                }

                                Button(
                                    onClick = { selectedMethod = "Transferencia" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMethod == "Transferencia") Color(0xFF34C759) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("ðŸª™", fontSize = 22.sp)
                                }
                            }

                            if (selectedMethod != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onPaymentDone(debtor, selectedMethod ?: "Efectivo") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Text("Pago Realizado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { activeFlowMode = "NONE" },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }

                    "ABONO" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Valor de Abono",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = abonoInputText,
                                onValueChange = { abonoInputText = it },
                                placeholder = { Text("Escribe la cantidad (ej. 300000)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "MÃ©todo de Abono",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { selectedMethod = "Efectivo" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMethod == "Efectivo") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("ðŸ’¸", fontSize = 22.sp)
                                }

                                Button(
                                    onClick = { selectedMethod = "Transferencia" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMethod == "Transferencia") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("ðŸª™", fontSize = 22.sp)
                                }
                            }

                            val abonoVal = abonoInputText.toDoubleOrNull() ?: 0.0
                            if (selectedMethod != null && abonoVal > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onAbonoDone(debtor, abonoVal)
                                        activeFlowMode = "NONE"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Text("Realizado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { activeFlowMode = "NONE" },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 5. HISTORIAL EXPANDIBLE
                if (isHistoryExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Historial",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Registro crudo pedido por pedido",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    rawOrders.forEach { rawItem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "${rawItem.quantity}x ${rawItem.productName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${rawItem.timeStr} - ${formatCurrency(rawItem.unitPrice)} c/u",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatCurrency(rawItem.quantity * rawItem.unitPrice),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TopDockItem(val index: Int, val label: String, val icon: ImageVector)

private data class BusinessModule(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val minPlan: String, // "BÃ¡sico", "Premium", "MAX IA"
    val allowedBusinessTypes: List<String>? = null
)

private fun getModulesForCompany(businessType: String, plan: String, userRole: String = "Administrador"): List<BusinessModule> {
    val isCajero = userRole.equals("Cajero", ignoreCase = true) || userRole.equals("Empleado", ignoreCase = true) || userRole.equals("Mesero", ignoreCase = true)

    val allModules = listOf(
        BusinessModule(
            title = "Reportes y Analytics Avanzados",
            description = "MÃ©tricas en tiempo real e histogramas de rentabilidad",
            icon = Icons.Default.BarChart,
            color = Color(0xFF4F46E5),
            minPlan = "Premium"
        ),
        BusinessModule(
            title = "Control de Turnos y Caja",
            description = "Apertura, arqueos ciegos y cierre de caja",
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF34C759),
            minPlan = "BÃ¡sico"
        ),
        BusinessModule(
            title = "FacturaciÃ³n ElectrÃ³nica DIAN",
            description = "EmisiÃ³n directa de facturas y documentos equivalentes",
            icon = Icons.Default.Receipt,
            color = Color(0xFF0284C7),
            minPlan = "Premium"
        ),
        BusinessModule(
            title = "Happy Hour & Promociones",
            description = "Reglas automatizadas de descuento por horario",
            icon = Icons.Default.Star,
            color = Color(0xFFFF9F0A),
            minPlan = "Premium",
            allowedBusinessTypes = listOf("Bar", "Discoteca", "Restaurante", "CafÃ©", "LicorerÃ­a")
        ),
        BusinessModule(
            title = "Venta por Mesa & Comandero",
            description = "AtenciÃ³n tÃ¡ctil a mesas, barras y zonas",
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF8B5CF6),
            minPlan = "Premium",
            allowedBusinessTypes = listOf("Bar", "Discoteca", "Restaurante", "CafÃ©")
        ),
        BusinessModule(
            title = "DivisiÃ³n de Cuentas (Split)",
            description = "Divide la cuenta por personas de forma Ã¡gil",
            icon = Icons.Default.CreditCard,
            color = Color(0xFFEC4899),
            minPlan = "Premium",
            allowedBusinessTypes = listOf("Bar", "Discoteca", "Restaurante", "CafÃ©")
        ),
        BusinessModule(
            title = "Control de Lotes y Vencimientos",
            description = "GestiÃ³n y alertas de rotaciÃ³n de productos perecederos",
            icon = Icons.Default.Inventory2,
            color = Color(0xFF059669),
            minPlan = "BÃ¡sico",
            allowedBusinessTypes = listOf("Supermercado", "Minimercado", "FerreterÃ­a", "Abarrotes", "LicorerÃ­a", "Boutique")
        ),
        BusinessModule(
            title = "Agente IA Kapta Assistant",
            description = "Asistente predictivo de compras e inventarios con IA",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFF7C3AED),
            minPlan = "MAX IA"
        ),
        BusinessModule(
            title = "PredicciÃ³n de Ventas Inteligente",
            description = "ProyecciÃ³n de demanda y sugerencias automÃ¡ticas con IA",
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF6366F1),
            minPlan = "MAX IA"
        )
    )

    val isMaxIa = plan.contains("MAX", ignoreCase = true) || plan.contains("IA", ignoreCase = true)
    val isPremium = plan.contains("Premium", ignoreCase = true)

    return allModules.filter { mod ->
        val roleAllowed = if (isCajero) {
            mod.title.contains("Caja", ignoreCase = true) || mod.title.contains("Mesa", ignoreCase = true) || mod.title.contains("DivisiÃ³n", ignoreCase = true)
        } else true

        val planAllowed = when {
            isMaxIa -> true
            isPremium -> mod.minPlan == "BÃ¡sico" || mod.minPlan == "Premium"
            else -> mod.minPlan == "BÃ¡sico"
        }

        val typeAllowed = if (mod.allowedBusinessTypes == null) {
            true
        } else {
            mod.allowedBusinessTypes.any {
                it.equals(businessType, ignoreCase = true) ||
                businessType.contains(it, ignoreCase = true) ||
                it.contains(businessType, ignoreCase = true)
            }
        }

        roleAllowed && planAllowed && typeAllowed
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantPosScreen(
    viewModel: KaptaViewModel,
    company: CompanyEntity,
    onExitPos: () -> Unit
) {
    val productsFlow: List<PosProductEntity> by viewModel.getProductsForCompany(company.code).collectAsState(initial = emptyList())
    val salesFlow: List<PosSaleEntity> by viewModel.getSalesForCompany(company.code).collectAsState(initial = emptyList())
    val transactionsFlow: List<FinancialTransactionEntity> by viewModel.getTransactionsForCompany(company.code).collectAsState(initial = emptyList())
    val companyUsers: List<CompanyUserEntity> by viewModel.getUsersByCompanyCode(company.code, company.id).collectAsState(initial = emptyList())
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSuperAdminSession by viewModel.isSuperAdminSession.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val userRole = currentUser?.role ?: if (isSuperAdminSession) "Administrador" else "Cajero"
    val isCajero = userRole.equals("Cajero", ignoreCase = true) || userRole.equals("Empleado", ignoreCase = true) || userRole.equals("Mesero", ignoreCase = true)

    androidx.compose.runtime.LaunchedEffect(company.code, company.id) {
        viewModel.ensureDefaultProductsForCompany(company.code)
        viewModel.ensureDefaultUsersForCompany(company.code, company.id)
    }

    // Fixed Top Dock Navigation Index (0: Inicio, 1: Ventas, 2: Finanzas, 3: Inventario, 4: MenÃº Adicional)
    var selectedDockTab by remember { mutableIntStateOf(if (isCajero) 1 else 0) }

    var searchPosQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    LaunchedEffect(company.code) {
        viewModel.startPollingForTenant(company.code)
    }

    // Cart items for POS
    val cart = remember { mutableStateMapOf<Int, Int>() }

    // UI Dialog & BottomSheet Controls
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showReceiptModal by remember { mutableStateOf(false) }
    var showUserProfileModal by remember { mutableStateOf(false) }
    var showProductFormModal by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<PosProductEntity?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("Efectivo") }

    // Modals for requested Quick Actions
    var showNuevaVentaView by remember { mutableStateOf(false) }
    var showExpenseModal by remember { mutableStateOf(false) }
    var showAddStockBottomSheet by remember { mutableStateOf(false) }
    var showStockConfirmationDialog by remember { mutableStateOf(false) }
    var showDebtorsModal by remember { mutableStateOf(false) }
    var selectedDebtorForHistory by remember { mutableStateOf<DebtorRecord?>(null) }
    var showAdditionalMenuSheet by remember { mutableStateOf(false) }
    var showHacerInventarioModal by remember { mutableStateOf(false) }

    // Inventory baseline snapshot state
    var inventorySavedDate by remember { mutableStateOf("31/07/2026 08:00 AM") }
    val savedInventoryBaselines = remember { mutableStateMapOf<Int, Pair<Int, Int>>() } // ProductId -> (Baseline, Added)

    // Finance filter states
    val todayFormatted = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    var financesFilter by remember { mutableStateOf("Mes") } // "DÃ­a", "Mes", "Rango de fechas"
    var financesStartDate by remember { mutableStateOf(todayFormatted) }
    var financesEndDate by remember { mutableStateOf(todayFormatted) }

    val isDemoTenant = company.code.equals("aptAdmin", ignoreCase = true) || company.code.equals("demo", ignoreCase = true)

    val cashSalesFlow = remember(salesFlow) {
        salesFlow.filter { sale ->
            val pm = sale.paymentMethod.lowercase()
            !pm.contains("fiado") && !pm.contains("debe") && !pm.contains("deudor") && !pm.contains("credito") && !pm.contains("crÃ©dito")
        }
    }

    val salesDetailList = remember(company.code, cashSalesFlow) {
        val list = mutableStateListOf<PdfSaleItem>()
        if (cashSalesFlow.isNotEmpty()) {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale("es", "CO"))
            cashSalesFlow.forEach { sale ->
                val dateStr = try { sdf.format(java.util.Date(sale.timestamp)) } catch (e: Exception) { "Hoy" }
                val qty = if (sale.quantity > 0) sale.quantity else sale.itemCount
                val unitP = if (sale.unitPrice > 0) sale.unitPrice else (if (qty > 0) sale.totalAmount / qty else sale.totalAmount)
                list.add(PdfSaleItem(dateStr, qty, sale.productName.ifBlank { "Venta" }, unitP))
            }
        }
        list
    }

    // Expense items state
    val expensesList = remember(company.code, transactionsFlow) {
        val list = mutableStateListOf<BusinessExpense>()
        val expenses = transactionsFlow.filter { it.isExpense }
        if (expenses.isNotEmpty()) {
            expenses.forEach { tx ->
                list.add(
                    BusinessExpense(
                        id = tx.id,
                        concept = tx.subtitle.ifBlank { tx.title },
                        amount = tx.amount,
                        type = tx.category.ifBlank { "Operativo" },
                        category = tx.category.ifBlank { "General" },
                        date = tx.dateString
                    )
                )
            }
        }
        list
    }

    // Debtors list state
    val debtorsList = remember(company.code) {
        mutableStateListOf<DebtorRecord>()
    }

    // Stock addition rows inside the +Stock BottomSheet
    val stockRows = remember { mutableStateListOf(StockRowItem()) }

    val dynamicCategories = remember(productsFlow) {
        listOf("Todos") + productsFlow.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val lowStockList = productsFlow.filter { it.stock <= it.minStockAlert }
    var showNotificacionesDialog by remember { mutableStateOf(false) }

    val filteredProducts = productsFlow.filter { prod ->
        val matchesSearch = searchPosQuery.isEmpty() ||
                prod.name.contains(searchPosQuery, ignoreCase = true) ||
                prod.aliases.contains(searchPosQuery, ignoreCase = true) ||
                prod.category.contains(searchPosQuery, ignoreCase = true)
        val matchesCat = selectedCategory == "Todos" || prod.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCat
    }

    val totalItems = cart.values.sum()
    val totalPrice = cart.entries.sumOf { (prodId, qty) ->
        val prod = productsFlow.find { it.id == prodId }
        (prod?.price ?: 0.0) * qty
    }

        val todayCalForPos = java.util.Calendar.getInstance()
        val todayYear = todayCalForPos.get(java.util.Calendar.YEAR)
        val todayDay = todayCalForPos.get(java.util.Calendar.DAY_OF_YEAR)

        val todaySalesAmount = cashSalesFlow.filter { sale ->
            val c = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            c.get(java.util.Calendar.YEAR) == todayYear && c.get(java.util.Calendar.DAY_OF_YEAR) == todayDay
        }.sumOf { it.totalAmount }

        val totalSalesAmount = cashSalesFlow.sumOf { it.totalAmount }
        val totalTransactionCount = cashSalesFlow.size
        val totalExpensesAmount = expensesList.sumOf { it.amount }
        val totalDebtorsAmount = debtorsList.sumOf { (it.amountOwed - it.abonoAmount).coerceAtLeast(0.0) }
        val activeClientsCount = if (totalItems > 0 || totalTransactionCount > 0 || debtorsList.isNotEmpty()) (cart.size + debtorsList.size) else 0

        val logRole = if (isSuperAdminSession || userRole.contains("superadmin", true)) "SUPER_ADMIN" else "BUSINESS"
        android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] TenantPosScreen state: role=$logRole, companyCode=${company.code}, salesCount=${cashSalesFlow.size}, salesTotal=$totalSalesAmount")
        android.util.Log.d("KAPTA_UI_TOTAL", "[KAPTA_UI_TOTAL] role=$logRole, screen=TenantPosDashboard, salesCount=${cashSalesFlow.size}, salesTotal=$todaySalesAmount, displayedTotal=$todaySalesAmount")

    // Neutral de la paleta de la empresa = tono del fondo del sistema (solo si es claro)
    val parsedNeutral = try {
        Color(android.graphics.Color.parseColor(company.neutralColorHex))
    } catch (e: Exception) {
        null
    }
    // ponytail: umbral de luminancia fijo; un neutral oscuro de la paleta cae al gris claro por defecto
    val neutralBgTint = parsedNeutral?.takeIf { it.luminance() > 0.5f }
    EtherealBackground(tintColor = neutralBgTint) {
        Column(modifier = Modifier.fillMaxSize()) {
            MembershipAlertBanner(company = company)
            // Header Bar flotante estilo SuperAdmin: logo + circulo de plan + campana + perfil
            val isMaxIaPlan = company.plan.contains("MAX", ignoreCase = true) || company.plan.contains("IA", ignoreCase = true)
            val isPremiumPlan = company.plan.contains("Premium", ignoreCase = true)

            // Circulo indicador del plan: BÃ¡sico verde, Premium morado, MAX IA degradado morado->naranja
            val planCircleColor: Color = when {
                isMaxIaPlan -> Color(0xFFA855F7)
                isPremiumPlan -> Color(0xFFA855F7)
                else -> Color(0xFF34C759)
            }

            // Dock superior flotante estilo SuperAdmin (redondeado, vidrio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo del negocio + circulo de plan + nombre
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val logoUrlToUse = company.logoUrl.ifBlank { company.listIconUrl }
                            val logoDisponible = when {
                                logoUrlToUse.isBlank() -> false
                                logoUrlToUse.startsWith("file://") -> java.io.File(logoUrlToUse.removePrefix("file://")).exists()
                                else -> true
                            }
                            if (logoDisponible) {
                                AsyncImage(
                                    model = logoUrlToUse,
                                    contentDescription = company.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Circulo del color del plan (MAX IA con degradado morado->naranja)
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isMaxIaPlan) Modifier.background(
                                        Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFF97316)))
                                    ) else Modifier.background(planCircleColor)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = company.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Acciones: campana de notificaciones + perfil (salir vive en el perfil)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable { showNotificacionesDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Notificaciones",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (lowStockList.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEF4444))
                                        .border(1.dp, Color.White, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${lowStockList.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showUserProfileModal = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // MAIN CONTENT BODY (Switches dynamically based on selectedDockTab)
            val activeDockTab = if (isCajero && selectedDockTab != 1 && selectedDockTab != 3) 1 else selectedDockTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeDockTab) {
                    // BOTON 1: Inicio (Dashboard, Resumen General, Acciones RÃ¡pidas & Alertas)
                    0 -> InicioDashboardView(
                        salesToday = todaySalesAmount,
                        monthlyExpenses = totalExpensesAmount,
                        debtorsCount = debtorsList.size,
                        debtorsAmount = totalDebtorsAmount,
                        activeClientsCount = activeClientsCount,
                        lowStockProducts = lowStockList,
                        onQuickActionVenta = { showNuevaVentaView = true },
                        onQuickActionGasto = { showExpenseModal = true },
                        onQuickActionAddStock = {
                            stockRows.clear()
                            stockRows.add(StockRowItem())
                            showAddStockBottomSheet = true
                        },
                        onQuickActionDeudores = { showDebtorsModal = true },
                        onGoToFinanzasDia = {
                            financesFilter = "DÃ­a"
                            selectedDockTab = 2
                        }
                    )

                    // BOTON 2: Ventas (Resumen comparativo & Ranking por categorÃ­a)
                    1 -> VentasSectionView(
                        products = filteredProducts,
                        searchQuery = searchPosQuery,
                        onSearchChange = { searchPosQuery = it },
                        selectedCategory = selectedCategory,
                        categories = dynamicCategories,
                        onCategorySelect = { selectedCategory = it },
                        cart = cart,
                        totalItems = totalItems,
                        totalPrice = totalPrice,
                        onAddToCart = { prodId -> cart[prodId] = (cart[prodId] ?: 0) + 1 },
                        onRemoveFromCart = { prodId ->
                            val current = cart[prodId] ?: 0
                            if (current > 1) cart[prodId] = current - 1 else cart.remove(prodId)
                        },
                        onCheckout = { showCheckoutDialog = true },
                        onGoToInventory = { selectedDockTab = 3 },
                        onQuickActionVenta = { showNuevaVentaView = true },
                        onQuickActionAddStock = {
                            stockRows.clear()
                            stockRows.add(StockRowItem())
                            showAddStockBottomSheet = true
                        },
                        onQuickActionGasto = { showExpenseModal = true },
                        isCajero = isCajero,
                        isDemoTenant = isDemoTenant,
                        salesToday = totalSalesAmount,
                        salesFlow = salesFlow
                    )

                    // BOTON 3: Finanzas (InformaciÃ³n financiera y control de gastos detallados)
                    2 -> FinanzasSectionView(
                        company = company,
                        activeFilter = financesFilter,
                        onFilterChange = { financesFilter = it },
                        startDateStr = financesStartDate,
                        onStartDateChange = { financesStartDate = it },
                        endDateStr = financesEndDate,
                        onEndDateChange = { financesEndDate = it },
                        salesList = cashSalesFlow,
                        expensesList = expensesList,
                        onAddExpenseClick = { showExpenseModal = true }
                    )

                    // BOTON 4: Inventario (Lista por categorÃ­a, Guardar Inventario & Hacer Inventario)
                    3 -> InventarioSectionView(
                        products = productsFlow,
                        savedInventoryBaselines = savedInventoryBaselines,
                        inventorySavedDate = inventorySavedDate,
                        onNewProduct = {
                            editingProduct = null
                            showProductFormModal = true
                        },
                        onEditProduct = { prod ->
                            editingProduct = prod
                            showProductFormModal = true
                        },
                        onDeleteProduct = { prodId -> viewModel.deleteProduct(prodId) },
                        onSaveInventoryBaseline = {
                            productsFlow.forEach { prod ->
                                savedInventoryBaselines[prod.id] = Pair(prod.stock, 0)
                            }
                            inventorySavedDate = "Hoy ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
                            viewModel.showToast("Base de inventario guardada exitosamente")
                        },
                        onHacerInventario = { showHacerInventarioModal = true },
                        isCajero = isCajero
                    )
                }
            }
        }

        // -------------------------------------------------------------------------------------
        // DOCK DE NAVEGACIÃ“N FLOTANTE (OPACIDAD 80%, EXCLUSIVAMENTE ICONOS VECTORIALES SIN TEXTO)
        // -------------------------------------------------------------------------------------
        val dockBg = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)
        val dockBorder = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.90f)

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(32.dp),
            color = dockBg,
            border = BorderStroke(1.dp, dockBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val topDockItems = if (isCajero) {
                    listOf(
                        TopDockItem(1, "Ventas", Icons.Default.TrendingUp),
                        TopDockItem(3, "Inventario", Icons.Default.Inventory2)
                    )
                } else {
                    listOf(
                        TopDockItem(0, "Inicio", Icons.Default.Home),
                        TopDockItem(1, "Ventas", Icons.Default.TrendingUp),
                        TopDockItem(2, "Finanzas", Icons.Default.AccountBalanceWallet),
                        TopDockItem(3, "Inventario", Icons.Default.Inventory2),
                        TopDockItem(4, "MenÃº", Icons.Default.MoreHoriz)
                    )
                }

                topDockItems.forEach { item ->
                    val isSelected = selectedDockTab == item.index && item.index != 4
                    val iconColor = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        onClick = {
                            if (item.index == 4) {
                                showAdditionalMenuSheet = true
                            } else {
                                selectedDockTab = item.index
                            }
                        },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // NOTIFICACIONES DEL NEGOCIO (stock bajo + deudores)
    // -------------------------------------------------------------------------------------
    if (showNotificacionesDialog) {
        AlertDialog(
            onDismissRequest = { showNotificacionesDialog = false },
            title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (lowStockList.isEmpty() && debtorsList.isEmpty()) {
                        Text("Sin novedades por ahora.")
                    } else {
                        lowStockList.take(6).forEach { prod ->
                            Text("• Stock bajo: ${prod.name} (${prod.stock} disponibles)")
                        }
                        if (lowStockList.size > 6) {
                            Text("• ...y ${lowStockList.size - 6} productos más con stock bajo")
                        }
                        if (debtorsList.isNotEmpty()) {
                            Text(
                                "• ${debtorsList.size} clientes con saldo pendiente",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificacionesDialog = false }) { Text("Cerrar") }
            }
        )
    }

    // -------------------------------------------------------------------------------------
    // NUEVA VENTA FULL SCREEN OVERLAY VIEW
    // -------------------------------------------------------------------------------------
    if (showNuevaVentaView) {
        NuevaVentaView(
            products = productsFlow,
            debtorsList = debtorsList,
            onClose = { showNuevaVentaView = false },
            onPaymentSuccess = { customerName, ticketItems, totalAmount, paymentMethod, transferAmount, cashAmount ->
                val finalClient = if (customerName.isBlank()) "Cliente Mostrador" else customerName.trim()
                val sharedTimestamp = System.currentTimeMillis()
                val grouped = ticketItems.entries.groupBy { it.key.id }
                grouped.forEach { (_, entries) ->
                    val prod = entries.first().key
                    val qty = entries.sumOf { it.value }
                    if (qty > 0) {
                        val newStock = (prod.stock - qty).coerceAtLeast(0)
                        viewModel.saveOrUpdateProduct(prod.copy(stock = newStock)) {}
                        val itemTotal = qty * prod.price
                        val ratio = if (totalAmount > 0) itemTotal / totalAmount else 0.0
                        val itemTransfer = transferAmount * ratio
                        val itemCash = cashAmount * ratio
                        viewModel.registerPosSale(
                            companyCode = company.code,
                            clientName = finalClient,
                            productName = prod.name,
                            quantity = qty,
                            unitPrice = prod.price,
                            totalAmount = itemTotal,
                            paymentMethod = paymentMethod,
                            transferAmount = itemTransfer,
                            cashAmount = itemCash,
                            itemCount = qty,
                            timestamp = sharedTimestamp
                        )
                    }
                }
                viewModel.showToast("Â¡Venta de ${formatCurrency(totalAmount)} pagada y registrada!")
                showNuevaVentaView = false
            },
            onDebeSuccess = { customerName, ticketItems, totalAmount, abonoAmount, abonoMethod ->
                val finalCustomer = if (customerName.isBlank()) "Cliente Fiado" else customerName.trim()
                val grouped = ticketItems.entries.groupBy { it.key.id }
                grouped.forEach { (_, entries) ->
                    val prod = entries.first().key
                    val qty = entries.sumOf { it.value }
                    if (qty > 0) {
                        val newStock = (prod.stock - qty).coerceAtLeast(0)
                        viewModel.saveOrUpdateProduct(prod.copy(stock = newStock)) {}
                        val itemTotal = qty * prod.price
                        val isMinPriceSelected = entries.any { it.key.minPrice > 0 || it.key.hasMinPrice }
                        viewModel.registrarDeudorDirecto(
                            companyCode = company.code,
                            clientName = finalCustomer,
                            productName = prod.name,
                            quantity = qty,
                            isMinPrice = isMinPriceSelected,
                            abonoAmount = abonoAmount,
                            abonoMethod = abonoMethod,
                            pendingTotal = itemTotal
                        )
                    }
                }
                val existingDebtor = debtorsList.find { it.name.equals(finalCustomer, ignoreCase = true) }
                val currentDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val pendingAmount = (totalAmount - abonoAmount).coerceAtLeast(0.0)
                if (existingDebtor != null) {
                    val idx = debtorsList.indexOf(existingDebtor)
                    debtorsList[idx] = existingDebtor.copy(
                        amountOwed = existingDebtor.amountOwed + pendingAmount,
                        abonoAmount = existingDebtor.abonoAmount + abonoAmount,
                        date = currentDate
                    )
                } else {
                    debtorsList.add(
                        DebtorRecord(
                            id = (debtorsList.maxOfOrNull { it.id } ?: 0) + 1,
                            name = finalCustomer,
                            phone = "3000000000",
                            amountOwed = totalAmount,
                            abonoAmount = abonoAmount,
                            concept = "Consumo fiado en Nueva Venta",
                            date = currentDate
                        )
                    )
                }
                viewModel.showToast("Venta fiada a $finalCustomer registrada. Stock actualizado.")
                showNuevaVentaView = false
            }
        )
    }

    // -------------------------------------------------------------------------------------
    // QUICK ACTION MODAL 1: REGISTRAR GASTO (Administrativo vs Recurrente)
    // -------------------------------------------------------------------------------------
    if (showExpenseModal) {
        ModalBottomSheet(
            onDismissRequest = { showExpenseModal = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            var expenseType by remember { mutableStateOf("Administrativo") }
            var conceptText by remember { mutableStateOf("") }
            var amountText by remember { mutableStateOf("") }
            var selectedSubcategory by remember { mutableStateOf("General") }

            val categoriesForAdmin = listOf("Recibos & Servicios", "NÃ³mina & Sueldos", "Arriendo", "Mantenimiento", "Eventos", "General")
            val categoriesForRecurrente = listOf("Pedidos de MercancÃ­a", "Bebidas & Licores", "Insumos Minuciosos", "Frutas & Desechables", "Empaques")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Registrar Nuevo Gasto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Clasifica el gasto segÃºn su tipo operativo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // Type Selector Switch
                Text("Tipo de Gasto *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { expenseType = "Administrativo" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (expenseType == "Administrativo") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gasto Administrativo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (expenseType == "Administrativo") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Recibos, nÃ³mina, eventos", fontSize = 10.sp, color = if (expenseType == "Administrativo") Color(0xFFDBEAFE) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(
                        onClick = { expenseType = "Recurrente" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (expenseType == "Recurrente") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gasto Recurrente", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (expenseType == "Recurrente") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("MercancÃ­a, insumos minuciosos", fontSize = 10.sp, color = if (expenseType == "Recurrente") Color(0xFFEDE9FE) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Concept & Description
                OutlinedTextField(
                    value = conceptText,
                    onValueChange = { conceptText = it },
                    label = { Text("Concepto del Gasto *") },
                    placeholder = { Text(if (expenseType == "Administrativo") "Ej. Pago recibo de energÃ­a elÃ©ctrica" else "Ej. Compra de limones y servilletas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount text
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor Total ($) *") },
                    placeholder = { Text("150000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (conceptText.isNotBlank() && amount > 0) {
                            expensesList.add(
                                0,
                                BusinessExpense(
                                    id = expensesList.size + 1,
                                    concept = conceptText.trim(),
                                    amount = amount,
                                    type = expenseType,
                                    category = selectedSubcategory,
                                    date = "Hoy"
                                )
                            )
                            showExpenseModal = false
                            viewModel.showToast("Gasto registrado exitosamente")
                        }
                    },
                    enabled = conceptText.isNotBlank() && amountText.toDoubleOrNull() != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Gasto", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // QUICK ACTION MODAL 2: + STOCK (Flotante Bottom Sheet)
    // -------------------------------------------------------------------------------------
    if (showAddStockBottomSheet) {
        val sheetBg = MaterialTheme.colorScheme.surface
        val cardBg = MaterialTheme.colorScheme.surfaceContainer
        val cardBorder = if (isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant
        val titleColor = MaterialTheme.colorScheme.onBackground

        ModalBottomSheet(
            onDismissRequest = { showAddStockBottomSheet = false },
            containerColor = sheetBg,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Ingreso de Stock y MercancÃ­a", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
                            Text("BÃºsqueda predictiva & adiciÃ³n mÃºltiple", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = { showAddStockBottomSheet = false }) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic list of stock addition rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    stockRows.forEachIndexed { index, rowItem ->
                        var dropdownExpanded by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Numeric Quantity input (Compact)
                                    OutlinedTextField(
                                        value = rowItem.quantityText,
                                        onValueChange = { rowItem.quantityText = it },
                                        placeholder = { Text("Cant.", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(75.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    // Product Predictive Search Field
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = rowItem.selectedProduct?.name ?: rowItem.searchQuery,
                                            onValueChange = {
                                                rowItem.searchQuery = it
                                                rowItem.selectedProduct = null
                                                dropdownExpanded = true
                                            },
                                            placeholder = { Text("Buscar producto...", fontSize = 12.sp) },
                                            trailingIcon = {
                                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        val matchingProducts = productsFlow.filter { prod ->
                                            rowItem.searchQuery.isBlank() ||
                                                    prod.name.contains(rowItem.searchQuery, ignoreCase = true) ||
                                                    prod.aliases.contains(rowItem.searchQuery, ignoreCase = true)
                                        }

                                        DropdownMenu(
                                            expanded = dropdownExpanded && matchingProducts.isNotEmpty(),
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.80f)
                                        ) {
                                            matchingProducts.take(6).forEach { prod ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("(Stock: ${prod.stock})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    },
                                                    onClick = {
                                                        rowItem.selectedProduct = prod
                                                        rowItem.searchQuery = prod.name
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (stockRows.size > 1) {
                                        IconButton(
                                            onClick = { stockRows.removeAt(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF453A))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Icon-only (+) button to add more dynamic rows
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { stockRows.add(StockRowItem()) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "AÃ±adir fila", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button: "Agregar Stock"
                Button(
                    onClick = {
                        val validRows = stockRows.filter { it.selectedProduct != null && (it.quantityText.toIntOrNull() ?: 0) > 0 }
                        if (validRows.isNotEmpty()) {
                            showStockConfirmationDialog = true
                        } else {
                            viewModel.showToast("Selecciona al menos un producto vÃ¡lido y su cantidad")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar Stock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // CENTRAL CONFIRMATION DIALOG (Emergente Central de ConfirmaciÃ³n)
    // -------------------------------------------------------------------------------------
    if (showStockConfirmationDialog) {
        val validRows = stockRows.filter { it.selectedProduct != null && (it.quantityText.toIntOrNull() ?: 0) > 0 }

        AlertDialog(
            onDismissRequest = { showStockConfirmationDialog = false },
            title = {
                Text("Confirmar Ingreso de MercancÃ­a", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            },
            text = {
                Column {
                    Text("Verifica el listado exacto de productos y cantidades a ingresar a inventario:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    validRows.forEach { row ->
                        val prod = row.selectedProduct!!
                        val qty = row.quantityText.toIntOrNull() ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("â€¢ ${prod.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("+ $qty unidades", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF34C759))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        validRows.forEach { row ->
                            val prod = row.selectedProduct!!
                            val addedQty = row.quantityText.toIntOrNull() ?: 0
                            val updatedProduct = prod.copy(stock = prod.stock + addedQty)

                            // Accumulate into baseline added count
                            val currentBase = savedInventoryBaselines[prod.id]
                            val newAdded = (currentBase?.second ?: 0) + addedQty
                            savedInventoryBaselines[prod.id] = Pair(currentBase?.first ?: prod.stock, newAdded)

                            viewModel.saveOrUpdateProduct(updatedProduct) {}
                        }
                        showStockConfirmationDialog = false
                        showAddStockBottomSheet = false
                        viewModel.showToast("Â¡Stock actualizado e ingresado correctamente!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                ) {
                    Text("Confirmar Ingreso", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStockConfirmationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // -------------------------------------------------------------------------------------
    // QUICK ACTION MODAL 4: DEUDORES
    // -------------------------------------------------------------------------------------
    if (showDebtorsModal) {
        DebtorsManagementModal(
            debtorsList = debtorsList,
            totalDebtorsAmount = totalDebtorsAmount,
            onDismiss = { showDebtorsModal = false },
            onShowToast = { msg -> viewModel.showToast(msg) },
            onPayDebtor = { clientName, method, amount ->
                val transfer = if (method.contains("Transfer", ignoreCase = true)) amount else 0.0
                val cash = if (method.contains("Efectivo", ignoreCase = true)) amount else 0.0
                viewModel.pagarDeudor(
                    companyCode = company.code,
                    clientName = clientName,
                    paymentMethod = method,
                    transferAmount = transfer,
                    cashAmount = cash
                )
            }
        )
    }

    // -------------------------------------------------------------------------------------
    // BOTON 5: (â€¢â€¢â€¢) MENÃš ADICIONAL (MÃ³dulos Avanzados del Plan)
    // -------------------------------------------------------------------------------------
    if (showAdditionalMenuSheet) {
        val availableModules = getModulesForCompany(company.businessType, company.plan, userRole)

        ModalBottomSheet(
            onDismissRequest = { showAdditionalMenuSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MÃ³dulos Avanzados del Plan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("${company.name} (${company.businessType} â€¢ Plan ${company.plan})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showAdditionalMenuSheet = false }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF34C759))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                    items(availableModules) { mod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    showAdditionalMenuSheet = false
                                    viewModel.showToast("Accediendo a: ${mod.title}")
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(mod.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = mod.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(mod.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                    Text(mod.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // INVENTORY MODAL: HACER INVENTARIO (FÃ³rmula: [Guardado + AÃ±adidos - Actual] * Precio C/U)
    // -------------------------------------------------------------------------------------
    if (showHacerInventarioModal) {
        ModalBottomSheet(
            onDismissRequest = { showHacerInventarioModal = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AuditorÃ­a / Hacer Inventario", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Comparativa vs Guardado ($inventorySavedDate)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showHacerInventarioModal = false }) {
                        Text("Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                var totalMerma = 0.0

                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(productsFlow) { prod ->
                        val (baseStock, addedStock) = savedInventoryBaselines[prod.id] ?: Pair(prod.stock, 0)
                        val expectedStock = baseStock + addedStock
                        val currentStock = prod.stock
                        val diffQty = expectedStock - currentStock
                        val diffMoney = diffQty * prod.price
                        totalMerma += diffMoney

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Guardado: $baseStock | AÃ±adido: +$addedStock | Actual: $currentStock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (diffQty == 0) "Cuadre Exacto" else "Diferencia: $diffQty C/U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (diffQty == 0) Color(0xFF34C759) else Color(0xFFFF453A)
                                    )
                                    Text(
                                        text = "$${diffMoney.toInt()} COP",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = if (diffMoney == 0.0) Color(0xFF34C759) else Color(0xFFFF453A)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Diferencia Monetaria Total / Merma:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        Text("$${totalMerma.toInt()} COP", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet for Product Registration & Editing
    if (showProductFormModal) {
        ProductFormBottomSheet(
            companyCode = company.code,
            productToEdit = editingProduct,
            existingCategories = productsFlow.map { it.category }.filter { it.isNotBlank() },
            onDismiss = { showProductFormModal = false },
            onSave = { productToSave ->
                viewModel.saveOrUpdateProduct(productToSave) {
                    showProductFormModal = false
                }
            }
        )
    }

    // Checkout Dialog
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Cobrar Venta - $${totalPrice.toInt()}") },
            text = {
                Column {
                    Text("Selecciona el mÃ©todo de pago:")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Efectivo" to "ðŸ’µ",
                            "Transferencia Bancaria" to "ðŸ“²"
                        ).forEach { (method, iconStr) ->
                            val isSelected = selectedPaymentMethod == method
                            Button(
                                onClick = { selectedPaymentMethod = method },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = iconStr,
                                    fontSize = 22.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sharedTimestamp = System.currentTimeMillis()
                        cart.forEach { (prodId, qty) ->
                            if (qty > 0) {
                                val prod = productsFlow.find { it.id == prodId }
                                if (prod != null) {
                                    val newStock = (prod.stock - qty).coerceAtLeast(0)
                                    viewModel.saveOrUpdateProduct(prod.copy(stock = newStock)) {}
                                    val itemTotal = qty * prod.price
                                    viewModel.registerPosSale(
                                        companyCode = company.code,
                                        clientName = "Cliente Mostrador",
                                        productName = prod.name,
                                        quantity = qty,
                                        unitPrice = prod.price,
                                        totalAmount = itemTotal,
                                        paymentMethod = selectedPaymentMethod,
                                        itemCount = qty,
                                        timestamp = sharedTimestamp
                                    )
                                }
                            }
                        }
                        showCheckoutDialog = false
                        cart.clear()
                        showReceiptModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                ) {
                    Text("Confirmar Pago", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // User Profile Modal
    if (showUserProfileModal) {
        UserProfileModal(
            userName = currentUser?.name ?: company.adminName.ifBlank { "Administrador" },
            userRole = userRole,
            userEmail = currentUser?.email ?: company.adminEmail.ifBlank { "admin@${company.code}.com" },
            userPhone = company.phone,
            userLanguage = "EspaÃ±ol",
            isDarkMode = isDarkMode,
            companyUsers = companyUsers,
            companies = listOf(company),
            currentCompanyCode = company.code,
            onDarkModeToggle = { viewModel.toggleDarkMode(it) },
            onDeleteUser = { userToDelete -> viewModel.deleteUser(userToDelete) },
            onSaveUser = { nuevo -> viewModel.createOrUpdateUser(nuevo) },
            onDismiss = { showUserProfileModal = false },
            onLogout = {
                viewModel.setCurrentUser(null)
                onExitPos()
            }
        )
    }
}

/**
 * Alerta de membresÃ­a segÃºn dÃ­as restantes:
 * - 0-2 dÃ­as: banner rojo fijo (suspensiÃ³n inminente).
 * - 3-5 dÃ­as: banner amarillo/naranja; tocarlo lo colapsa a una pÃ­ldora compacta.
 */
// ponytail: el colapso se hace tocando el propio banner, no detectando clics fuera de Ã©l
@Composable
private fun MembershipAlertBanner(company: CompanyEntity) {
    if (company.getEffectiveStatus().equals("Suspendido", ignoreCase = true)) return
    val dias = company.expirationDays
    when {
        dias in 0..2 -> MembershipBannerRow(
            container = Color(0xFFFEE2E2),
            border = Color(0xFFDC2626),
            content = Color(0xFF991B1B),
            iconTint = Color(0xFFDC2626),
            text = "MembresÃ­a PrÃ³xima a Vencer: en $dias ${if (dias == 1) "dÃ­a" else "dÃ­as"} se suspenderÃ¡ el Acceso Total. Para renovar contÃ¡ctanos."
        )
        dias in 3..5 -> {
            var expanded by remember { mutableStateOf(true) }
            AnimatedVisibility(visible = expanded) {
                Column {
                    MembershipBannerRow(
                        container = Color(0xFFFFF7E0),
                        border = Color(0xFFF59E0B),
                        content = Color(0xFF92400E),
                        iconTint = Color(0xFFF59E0B),
                        text = "MembresÃ­a PrÃ³xima a Vencer. Para renovar contÃ¡ctanos.",
                        onClick = { expanded = false }
                    )
                }
            }
            AnimatedVisibility(visible = !expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF7E0))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MembresÃ­a prÃ³xima a vencer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }
    }
}

@Composable
private fun MembershipBannerRow(
    container: Color,
    border: Color,
    content: Color,
    iconTint: Color,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = content,
            lineHeight = 16.sp
        )
    }
}

// -------------------------------------------------------------------------------------
// BOTON 1: ðŸ  INICIO (Dashboard Principal, Resumen General & Alertas)
// -------------------------------------------------------------------------------------
// Acción rápida del dashboard de inicio (tarjeta degradada premium)
private data class AccionRapida(
    val titulo: String,
    val icono: ImageVector,
    val desde: Color,
    val hasta: Color,
    val accion: () -> Unit
)

@Composable
private fun InicioDashboardView(
    salesToday: Double,
    monthlyExpenses: Double,
    debtorsCount: Int,
    debtorsAmount: Double,
    activeClientsCount: Int,
    lowStockProducts: List<PosProductEntity>,
    onQuickActionVenta: () -> Unit,
    onQuickActionGasto: () -> Unit,
    onQuickActionAddStock: () -> Unit,
    onQuickActionDeudores: () -> Unit,
    onGoToFinanzasDia: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 90.dp)
    ) {
        // RESUMEN GENERAL (4 KPI Cards)
        iOSLargeTitle(title = "Resumen General del Negocio")
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardKpiCard(
                title = "Ventas del DÃ­a",
                value = formatCurrency(salesToday),
                subtitle = "En tiempo real (Clic)",
                accentColor = Color(0xFF34C759),
                icon = Icons.Default.TrendingUp,
                onClick = onGoToFinanzasDia,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiCard(
                title = "Gastos del Mes",
                value = formatCurrency(monthlyExpenses),
                subtitle = "Total acumulado",
                accentColor = Color(0xFFFF453A),
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardKpiCard(
                title = "Deudores",
                value = "$debtorsCount personas",
                subtitle = "Total: ${formatCurrency(debtorsAmount)} (Clic)",
                accentColor = Color(0xFFFF9F0A),
                icon = Icons.Default.Person,
                onClick = onQuickActionDeudores,
                modifier = Modifier.weight(1f)
            )
            DashboardKpiCard(
                title = "Clientes Activos",
                value = "$activeClientsCount personas",
                subtitle = "En establecimiento",
                accentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ACCIONES RÁPIDAS EN INICIO (tarjetas premium degradadas)
        iOSSectionHeader(text = "Acciones Rápidas")
        Spacer(modifier = Modifier.height(10.dp))

        val accionesRapidas = listOf(
            AccionRapida("Compras", Icons.Default.ShoppingCart, Color(0xFF315AA8), Color(0xFF416FC2), onQuickActionVenta),
            AccionRapida("Billetera", Icons.Default.AccountBalanceWallet, Color(0xFF5428B8), Color(0xFF7046D4), onQuickActionGasto),
            AccionRapida("Agregar", Icons.Default.Add, Color(0xFF18A94F), Color(0xFF32C96A), onQuickActionAddStock),
            AccionRapida("Perfil", Icons.Default.Person, Color(0xFFE58A05), Color(0xFFF2A01A), onQuickActionDeudores)
        )

        // Fila única 1x4 en cualquier ancho
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            accionesRapidas.forEach { accion ->
                QuickActionCard(
                    title = accion.titulo,
                    icon = accion.icono,
                    gradient = listOf(accion.desde, accion.hasta),
                    onClick = accion.accion,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ALERTAS EN INICIO: tarjetas de producto en grid responsive
        iOSSectionHeader(text = "Alertas de Stock")
        Text(
            text = "Productos con inventario bajo o que requieren atenciÃ³n",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 18.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (lowStockProducts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF34C759))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Â¡Todo en orden! El inventario cuenta con suficiente stock.", fontSize = 13.sp, color = Color(0xFF166534))
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 900.dp -> 4
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lowStockProducts.chunked(columns).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { prod ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ProductAlertCard(product = prod, onViewInventory = onQuickActionAddStock)
                                }
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductAlertCard(product: PosProductEntity, onViewInventory: () -> Unit) {
    val isDark = LocalIsDarkMode.current
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorder = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.28f) else Color(0xFFE5E7EB)
    val nameColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val thumbBg = if (isDark) Color(0xFF26262B) else Color(0xFFEEF0FA)

    // Estados derivados de los mismos datos de siempre (sin cambiar lÃ³gica)
    val sinStock = product.stock <= 0
    val critico = !sinStock && product.stock <= (product.minStockAlert / 2)

    data class Estado(val texto: String, val detalle: String, val chipBg: Color, val chipFg: Color, val icono: ImageVector?)
    val estado = when {
        sinStock -> Estado("Sin stock", "Producto agotado", if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E7EB), if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A), Icons.Default.Inventory2)
        critico -> Estado("Stock crÃ­tico", "Quedan ${product.stock} unidades", if (isDark) Color(0xFF3B1D1D) else Color(0xFFFEECEC), if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C), Icons.Default.Warning)
        else -> Estado("Stock bajo", "Quedan ${product.stock} unidades", if (isDark) Color(0xFF3A2E10) else Color(0xFFFEF3C7), if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E), Icons.Default.Warning)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x120F172A),
                spotColor = Color(0x120F172A)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            // Imagen del producto arriba
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(thumbBg),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(shape = RoundedCornerShape(50), color = estado.chipBg) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    estado.icono?.let {
                        Icon(imageVector = it, contentDescription = null, tint = estado.chipFg, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(text = estado.texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = estado.chipFg)
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "${estado.detalle} â¢ LÃ­mite: ${product.minStockAlert}",
                fontSize = 12.sp,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewInventory,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver inventario", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// BOTON 2: â‡ï¸ VENTAS (Resumen Comparativo & Ranking por CategorÃ­a)
// -------------------------------------------------------------------------------------
@Composable
private fun VentasSectionView(
    products: List<PosProductEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelect: (String) -> Unit,
    cart: Map<Int, Int>,
    totalItems: Int,
    totalPrice: Double,
    onAddToCart: (Int) -> Unit,
    onRemoveFromCart: (Int) -> Unit,
    onCheckout: () -> Unit,
    onGoToInventory: () -> Unit,
    onQuickActionVenta: () -> Unit = {},
    onQuickActionAddStock: () -> Unit = {},
    onQuickActionGasto: () -> Unit = {},
    isCajero: Boolean = false,
    isDemoTenant: Boolean = false,
    salesToday: Double = 0.0,
    salesFlow: List<PosSaleEntity> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 90.dp)
    ) {
        // Acciones Operativas RÃ¡pidas para Venta, Stock y Gasto
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Acciones Operativas Autorizadas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onQuickActionVenta,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("âš¡ Nueva Venta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onQuickActionAddStock,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ðŸ“¦ AÃ±adir Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onQuickActionGasto,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ðŸ’¸ Reg. Gasto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        iOSLargeTitle(title = "Resumen Financiero de Ventas")
        Spacer(modifier = Modifier.height(10.dp))

        // Comparativas de Ventas
        val todayCal = java.util.Calendar.getInstance()
        val curYear = todayCal.get(java.util.Calendar.YEAR)
        val curDay = todayCal.get(java.util.Calendar.DAY_OF_YEAR)
        val curMonth = todayCal.get(java.util.Calendar.MONTH)

        val startOfWeek = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -7)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cashSales = remember(salesFlow) {
            salesFlow.filter { sale ->
                val pm = sale.paymentMethod.lowercase()
                val pn = sale.productName.lowercase()
                !pm.contains("fiado") && !pm.contains("debe") && !pm.contains("deudor") && !pm.contains("credito") && !pm.contains("crÃ©dito") &&
                !pm.contains("anulad") && !pm.contains("cancelad") && !pn.contains("anulad") && !pn.contains("cancelad")
            }
        }

        val salesTodayVal = cashSales.filter { sale ->
            val c = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            c.get(java.util.Calendar.YEAR) == curYear && c.get(java.util.Calendar.DAY_OF_YEAR) == curDay
        }.sumOf { it.totalAmount }

        val weekVal = cashSales.filter { it.timestamp >= startOfWeek }.sumOf { it.totalAmount }

        val monthVal = cashSales.filter { sale ->
            val c = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            c.get(java.util.Calendar.YEAR) == curYear && c.get(java.util.Calendar.MONTH) == curMonth
        }.sumOf { it.totalAmount }

        val todayFormattedStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(todayCal.time)
        android.util.Log.d("KAPTA_DIAG", "DASHBOARD: fecha_actual=$todayFormattedStr, cantidad_ventas=${cashSales.size}, ventas_del_dia=$salesTodayVal")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SalesComparisonCard("Ventas Hoy", formatCurrency(salesTodayVal), "Real Hoy", if (salesTodayVal > 0) "+100%" else "0%", true, Modifier.weight(1f))
            SalesComparisonCard("Esta Semana", formatCurrency(weekVal), "Ãšltimos 7 dÃ­as", if (weekVal > 0) "+100%" else "0%", true, Modifier.weight(1f))
            SalesComparisonCard("Este Mes", formatCurrency(monthVal), "Acumulado Mes", if (monthVal > 0) "+100%" else "0%", monthVal >= 0, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ranking de Productos mÃ¡s Vendidos por CategorÃ­a
        iOSSectionHeader(text = "Ranking de Productos MÃ¡s Vendidos")
        Spacer(modifier = Modifier.height(15.dp))

        // Category Filter Pills (activo morado intenso, inactivo gris claro)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { onCategorySelect(cat) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F4F9)
                ) {
                    Text(
                        text = cat,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        val filteredForRanking = remember(products, selectedCategory) {
            if (selectedCategory == "Todos") products else products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        val rankingList = remember(filteredForRanking, salesFlow) {
            filteredForRanking.map { prod ->
                val matches = salesFlow.filter {
                    it.productName.equals(prod.name, ignoreCase = true) || it.productName.contains(prod.name, ignoreCase = true)
                }
                Triple(
                    prod,
                    matches.sumOf { if (it.quantity > 0) it.quantity else it.itemCount },
                    matches.sumOf { it.totalAmount }
                )
            }.sortedByDescending { it.second }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rankingList.forEachIndexed { index, (prod, units, revenue) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0x0F0F172A),
                            spotColor = Color(0x0F0F172A)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // NÃºmero del ranking
                        Text(
                            text = "${rankingList.indexOfFirst { it.first.id == prod.id } + 1}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Imagen real del producto (o placeholder elegante)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (prod.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = prod.imageUrl,
                                    contentDescription = prod.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(5.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Nombre + informaciÃ³n de ventas
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prod.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "$units Unidades â¢ ${formatCurrency(revenue)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Indicador de tendencia (cÃ­rculo verde claro)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BotÃ³n Inferior: "Ver Inventario" (navy oscuro premium)
        Button(
            onClick = onGoToInventory,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Inventario Completo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SalesComparisonCard(
    title: String,
    value: String,
    comparison: String,
    percentage: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF34C759) else Color(0xFFFF453A),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = percentage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF34C759) else Color(0xFFFF453A)
                )
            }
            Text(comparison, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------------------------------
// BOTON 3: ðŸ’¹ FINANZAS (Control de Gastos Detallados & Estado Financiero)
// -------------------------------------------------------------------------------------
@Composable
private fun FinanzasSectionView(
    company: CompanyEntity,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    startDateStr: String,
    onStartDateChange: (String) -> Unit,
    endDateStr: String,
    onEndDateChange: (String) -> Unit,
    salesList: List<PosSaleEntity>,
    expensesList: List<BusinessExpense>,
    onAddExpenseClick: () -> Unit
) {
    val context = LocalContext.current

    val todayCal = java.util.Calendar.getInstance()
    val todayShortFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale("es", "CO")).format(todayCal.time)
    val todayDateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(todayCal.time)
    val currentMonthFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "CO")).format(todayCal.time).replaceFirstChar { it.uppercase() }

    val periodLabel = when (activeFilter) {
        "DÃ­a" -> java.text.SimpleDateFormat("d 'de' MMMM yyyy", java.util.Locale("es", "CO")).format(todayCal.time).replaceFirstChar { it.uppercase() }
        "Mes" -> currentMonthFormat
        else -> if (startDateStr.isNotBlank() && endDateStr.isNotBlank()) "$startDateStr hasta $endDateStr" else "Rango Personalizado"
    }

    // Filter Sales Items
    val filteredSales = remember(activeFilter, startDateStr, endDateStr, salesList) {
        val todayCal = java.util.Calendar.getInstance()
        val curYear = todayCal.get(java.util.Calendar.YEAR)
        val curDay = todayCal.get(java.util.Calendar.DAY_OF_YEAR)
        val curMonth = todayCal.get(java.util.Calendar.MONTH)

        val list = when (activeFilter) {
            "DÃ­a" -> salesList.filter { sale ->
                val saleCal = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                saleCal.get(java.util.Calendar.YEAR) == curYear &&
                saleCal.get(java.util.Calendar.DAY_OF_YEAR) == curDay
            }
            "Mes" -> salesList.filter { sale ->
                val saleCal = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                saleCal.get(java.util.Calendar.YEAR) == curYear &&
                saleCal.get(java.util.Calendar.MONTH) == curMonth
            }
            else -> {
                if (startDateStr.isBlank() || endDateStr.isBlank()) {
                    salesList
                } else {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val startMs = try { sdf.parse(startDateStr)?.time ?: 0L } catch (_: Exception) { 0L }
                    val endMs = try {
                        val parsed = sdf.parse(endDateStr)
                        if (parsed != null) {
                            java.util.Calendar.getInstance().apply {
                                timeInMillis = parsed.time
                                set(java.util.Calendar.HOUR_OF_DAY, 23)
                                set(java.util.Calendar.MINUTE, 59)
                                set(java.util.Calendar.SECOND, 59)
                                set(java.util.Calendar.MILLISECOND, 999)
                            }.timeInMillis
                        } else Long.MAX_VALUE
                    } catch (_: Exception) { Long.MAX_VALUE }

                    if (startMs > 0 && endMs > 0) {
                        salesList.filter { it.timestamp in startMs..endMs }
                    } else {
                        salesList
                    }
                }
            }
        }

        list.forEach { sale ->
            android.util.Log.d("KAPTA_DIAG_FINANZAS", "FINANZAS ITEM: id=${sale.id}, timestamp=${sale.timestamp}, client=${sale.clientName}, product=${sale.productName}, qty=${sale.quantity}, unitPrice=${sale.unitPrice}, totalAmount=${sale.totalAmount}, transfer=${sale.transferAmount}, cash=${sale.cashAmount}")
        }
        val v00001 = list.find { it.clientName.contains("Juan", true) || it.productName.contains("coca", true) } ?: list.firstOrNull()
        android.util.Log.d("KAPTA_DIAG_FINANZAS", "FINANZAS TOTALES: activeFilter=$activeFilter, count=${list.size}, V-00001 totalAmount=${v00001?.totalAmount ?: 0.0}, sum=${list.sumOf { it.totalAmount }}")

        list
    }

    // Filter Expense Items
    val filteredExpenses = remember(activeFilter, startDateStr, endDateStr, expensesList) {
        when (activeFilter) {
            "DÃ­a" -> expensesList.filter {
                it.date.equals(todayDateFormat, ignoreCase = true) ||
                it.date.contains(todayShortFormat, ignoreCase = true)
            }
            "Mes" -> expensesList
            else -> expensesList
        }
    }

    val totalSales = filteredSales.sumOf { it.totalAmount }
    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val netProfit = totalSales - totalExpenses

    val logRoleFinanzas = if (company.code.isBlank()) "SUPER_ADMIN" else "BUSINESS"
    android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] FinanzasSectionView: role=$logRoleFinanzas, companyCode=${company.code}, salesCount=${filteredSales.size}, salesTotal=$totalSales")
    android.util.Log.d("KAPTA_UI_TOTAL", "[KAPTA_UI_TOTAL] role=$logRoleFinanzas, screen=FinanzasSectionView, salesCount=${filteredSales.size}, salesTotal=$totalSales, displayedTotal=$totalSales")
    android.util.Log.d("KAPTA_DIAG", "INGRESOS: fecha_seleccionada=$activeFilter ($periodLabel), cantidad_ventas=${filteredSales.size}, suma_totalAmount=$totalSales")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 90.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                iOSLargeTitle(title = "Estado Financiero", subtitle = "Periodo: $periodLabel")
            }

            // BOTÃ“N DESTACADO EXPORTAR A PDF
            Button(
                onClick = {
                    val pdfSales = filteredSales.map { sale ->
                        val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale("es", "CO"))
                        val dateStr = try { sdf.format(java.util.Date(sale.timestamp)) } catch (_: Exception) { "Hoy" }
                        val qty = if (sale.quantity > 0) sale.quantity else sale.itemCount
                        val unitP = if (sale.unitPrice > 0) sale.unitPrice else (if (qty > 0) sale.totalAmount / qty else sale.totalAmount)
                        PdfSaleItem(dateStr, qty, sale.productName.ifBlank { "Venta" }, unitP)
                    }
                    val pdfExpenses = filteredExpenses.map { exp ->
                        PdfExpenseItem(
                            dateStr = exp.date,
                            concept = exp.concept,
                            type = if (exp.type.contains("Admin", ignoreCase = true)) "Administrativo" else "Recurrente",
                            amount = exp.amount
                        )
                    }
                    val pdfFile = PdfReportGenerator.generateFinancialReportPdf(
                        context = context,
                        company = company,
                        periodLabel = periodLabel,
                        salesList = pdfSales,
                        expensesList = pdfExpenses
                    )
                    PdfReportGenerator.openOrSharePdf(context, pdfFile)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar a PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // FILTROS MENÃš: "DÃ­a", "Mes", "Rango de fechas"
        iOSSegmented(
            options = listOf("DÃ­a", "Mes", "Rango de fechas"),
            selectedIndex = listOf("DÃ­a", "Mes", "Rango de fechas").indexOf(activeFilter),
            onSelect = { idx -> onFilterChange(listOf("DÃ­a", "Mes", "Rango de fechas")[idx]) }
        )

        if (activeFilter == "Rango de fechas") {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startDateStr,
                    onValueChange = onStartDateChange,
                    label = { Text("Fecha Inicio", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDateStr,
                    onValueChange = onEndDateChange,
                    label = { Text("Fecha Fin", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Financial KPI Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardKpiCard("Ventas Total", formatCurrency(totalSales), "Ingresos en periodo", Color(0xFF34C759), Icons.Default.TrendingUp, modifier = Modifier.weight(1f))
            DashboardKpiCard("Gastos Total", formatCurrency(totalExpenses), "Egresos en periodo", Color(0xFFFF453A), Icons.Default.AccountBalanceWallet, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card Net Profit
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Utilidad Neta (Neto)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCurrency(netProfit), fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (netProfit >= 0) Color(0xFF34C759) else Color(0xFFFF453A))
                Text("CÃ¡lculo: Ventas (${formatCurrency(totalSales)}) - Gastos (${formatCurrency(totalExpenses)})", fontSize = 11.sp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECCIÃ“N 1: DETALLE DE VENTAS
        iOSSectionHeader(text = "1. Ventas en el Periodo")
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSales.isEmpty()) {
            Text("Sin ventas en el periodo seleccionado", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val scrollState = androidx.compose.foundation.rememberScrollState()

                    Box(modifier = Modifier.horizontalScroll(scrollState)) {
                        Column(modifier = Modifier.width(640.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fecha", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(85.dp))
                                Text("Cantidad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(65.dp))
                                Text("Producto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(115.dp))
                                Text("P. C/U", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                Text("Subtotal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                Text("Transf.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                Text("Efectivo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            filteredSales.forEach { sale ->
                                val dateFormatted = try { sdf.format(java.util.Date(sale.timestamp)) } catch (_: Exception) { "-" }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dateFormatted, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(85.dp))
                                    Text("${sale.quantity}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(65.dp))
                                    Text(sale.productName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(115.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatCurrency(sale.unitPrice), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                    Text(formatCurrency(sale.totalAmount), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                    Text(formatCurrency(sale.transferAmount), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                    Text(formatCurrency(sale.cashAmount), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(75.dp))
                                    Text(formatCurrency(sale.totalAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759), modifier = Modifier.width(75.dp), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECCIÃ“N 2: DETALLE DE GASTOS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            iOSSectionHeader(text = "2. Gastos en el Periodo")
            Button(
                onClick = onAddExpenseClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("+ Registrar Gasto", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredExpenses.isEmpty()) {
            Text("Sin gastos en el periodo seleccionado", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filteredExpenses.forEach { expense ->
                val strictType = if (expense.type.contains("Admin", ignoreCase = true)) "Administrativo" else "Recurrente"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (strictType == "Administrativo") Color(0xFFDBEAFE) else Color(0xFFEDE9FE)
                                ) {
                                    Text(
                                        text = strictType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (strictType == "Administrativo") Color(0xFF1E40AF) else Color(0xFF6B21A8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(expense.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(expense.concept, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text("Fecha: ${expense.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("-${formatCurrency(expense.amount)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFFF453A))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// BOTON 4: ðŸ“‹ INVENTARIO (Lista por CategorÃ­a, Guardar Inventario & Hacer Inventario)
// -------------------------------------------------------------------------------------
@Composable
private fun InventarioSectionView(
    products: List<PosProductEntity>,
    savedInventoryBaselines: Map<Int, Pair<Int, Int>>,
    inventorySavedDate: String,
    onNewProduct: () -> Unit,
    onEditProduct: (PosProductEntity) -> Unit,
    onDeleteProduct: (Int) -> Unit,
    onSaveInventoryBaseline: () -> Unit,
    onHacerInventario: () -> Unit,
    isCajero: Boolean = false
) {
    var selectedCatFilter by remember { mutableStateOf("Todos") }
    val categories = remember(products) {
        listOf("Todos") + products.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filtered = if (selectedCatFilter == "Todos") products else products.filter { it.category.equals(selectedCatFilter, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 90.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                iOSLargeTitle(
                    title = if (isCajero) "Inventario de Productos (Solo Lectura)" else "Inventario de Productos",
                    subtitle = "Stock Actual y Precio C/U"
                )
            }

            if (!isCajero) {
                Button(
                    onClick = onNewProduct,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Producto", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat ->
                val isSelected = selectedCatFilter == cat
                Surface(
                    onClick = { selectedCatFilter = cat },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Product Items List grouped by Category
        filtered.forEach { prod ->
            val baseline = savedInventoryBaselines[prod.id]
            val formatText = if (baseline != null) {
                val (base, added) = baseline
                "$base (+${added})"
            } else null

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (prod.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = prod.imageUrl,
                                contentDescription = prod.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "CategorÃ­a: ${prod.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stock: ${prod.stock} C/U",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (prod.stock <= 15) Color(0xFFFF453A) else Color(0xFF34C759)
                            )
                            if (formatText != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Base: $formatText",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        iOSPill(text = "${formatCurrency(prod.price)} C/U")
                        if (!isCajero) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                IconButton(onClick = { onEditProduct(prod) }, modifier = Modifier.size(28.dp)) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteProduct(prod.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF453A))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isCajero) {
            Spacer(modifier = Modifier.height(24.dp))

            // DOS BOTONES INFERIORES: "Guardar inventario" & "Hacer Inventario"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // BotÃ³n 1: "Guardar inventario"
                Button(
                    onClick = onSaveInventoryBaseline,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar Inventario", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // BotÃ³n 2: "Hacer Inventario"
                Button(
                    onClick = onHacerInventario,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hacer Inventario", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Helper Cards
@Composable
private fun DashboardKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.shadow(
            elevation = 6.dp,
            shape = cardShape,
            spotColor = accentColor.copy(alpha = 0.2f),
            ambientColor = Color.Black.copy(alpha = 0.04f)
        ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White,
                    accentColor.copy(alpha = 0.20f),
                    MaterialTheme.colorScheme.outlineVariant
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.20f),
                                    accentColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val liftPx by animateFloatAsState(
        targetValue = if (hovered || pressed) -8f else 0f,
        animationSpec = tween(220),
        label = "liftQuickAction"
    )
    val scale = if (pressed) 0.98f else 1f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                translationY = liftPx
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (hovered) 14.dp else 10.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(interactionSource = interaction, indication = LocalIndication.current) { onClick() }
            .padding(12.dp)
    ) {
        // Icono blanco arriba a la izquierda
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(26.dp)
        )
        // Nombre de la acción abajo a la izquierda
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun DetailRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormBottomSheet(
    companyCode: String,
    productToEdit: PosProductEntity?,
    existingCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (PosProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var aliases by remember { mutableStateOf(productToEdit?.aliases ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "") }
    var priceText by remember { mutableStateOf(productToEdit?.price?.toInt()?.toString() ?: "") }
    var costText by remember { mutableStateOf(productToEdit?.costPrice?.toInt()?.toString() ?: "") }
    var imageUrl by remember { mutableStateOf(productToEdit?.imageUrl ?: "") }
    var stockText by remember { mutableStateOf(productToEdit?.stock?.toString() ?: "50") }
    var minStockAlertText by remember { mutableStateOf(productToEdit?.minStockAlert?.toString() ?: "10") }
    var hasMinPrice by remember { mutableStateOf(productToEdit?.hasMinPrice ?: false) }
    var minPriceText by remember { mutableStateOf(productToEdit?.minPrice?.toInt()?.toString() ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    val dynamicCategories = remember(existingCategories) {
        existingCategories.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUrl = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (productToEdit == null) "Nuevo Producto" else "Editar Producto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Preview & Native Gallery Picker
            Text("Imagen del Producto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (imageUrl.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Foto del producto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Agregar foto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+ Foto",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (imageUrl.isNotBlank()) "Cambiar Foto" else "Seleccionar de GalerÃ­a",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        TextButton(
                            onClick = { imageUrl = "" },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Quitar Foto", color = Color(0xFFFF453A), fontSize = 11.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toca para elegir una foto de tu galerÃ­a",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Producto *") },
                placeholder = { Text("Ej. Cerveza Poker 330ml") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Aliases / Search tags
            OutlinedTextField(
                value = aliases,
                onValueChange = { aliases = it },
                label = { Text("Apodos / BÃºsqueda RÃ¡pida") },
                placeholder = { Text("Ej. (Polas) (Cerveza frÃ­a) (Birra)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Category Input Field
            Text("CategorÃ­a", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        categoryExpanded = true
                    },
                    label = { Text("Escribe o selecciona categorÃ­a") },
                    placeholder = { Text("Ej. Cocteles, Entradas, Bebidas...") },
                    trailingIcon = {
                        IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                            Icon(
                                imageVector = if (categoryExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Sugerencias de categorÃ­a"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                val filteredCats = dynamicCategories.filter { cat ->
                    cat.contains(category.trim(), ignoreCase = true)
                }

                if (categoryExpanded && filteredCats.isNotEmpty()) {
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        filteredCats.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontSize = 13.sp) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cost & Sale Price Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Costo ($)") },
                    placeholder = { Text("3000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Precio Venta ($) *") },
                    placeholder = { Text("6000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Has Min Price Switch & Min Price Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Precio MÃ­nimo de Venta", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Restringe el precio lÃ­mite autorizado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = hasMinPrice,
                    onCheckedChange = { hasMinPrice = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                )
            }

            if (hasMinPrice) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = minPriceText,
                    onValueChange = { minPriceText = it },
                    label = { Text("Precio MÃ­nimo Autorizado ($)") },
                    placeholder = { Text("5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Inicial & LÃ­mite de Alerta de Stock Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Stock Actual") },
                    placeholder = { Text("50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = minStockAlertText,
                    onValueChange = { minStockAlertText = it },
                    label = { Text("LÃ­mite Alerta Stock") },
                    placeholder = { Text("10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Text(
                text = "Notificar cuando el inventario caiga a este nÃºmero de unidades o menos",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val priceVal = priceText.toDoubleOrNull() ?: 0.0
                    val costVal = costText.toDoubleOrNull() ?: 0.0
                    val minPriceVal = minPriceText.toDoubleOrNull() ?: 0.0
                    val stockVal = stockText.toIntOrNull() ?: 50
                    val minStockVal = minStockAlertText.toIntOrNull() ?: 10

                    if (name.isNotBlank() && priceVal > 0) {
                        val product = PosProductEntity(
                            id = productToEdit?.id ?: 0,
                            companyCode = companyCode,
                            name = name.trim(),
                            aliases = aliases.trim(),
                            category = category.trim().ifBlank { "General" },
                            price = priceVal,
                            costPrice = costVal,
                            imageUrl = imageUrl.trim(),
                            stock = stockVal,
                            minStockAlert = minStockVal,
                            hasMinPrice = hasMinPrice,
                            minPrice = if (hasMinPrice) minPriceVal else 0.0
                        )
                        onSave(product)
                    }
                },
                enabled = name.isNotBlank() && priceText.toDoubleOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Producto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// -------------------------------------------------------------------------------------
// NUEVA VENTA VIEW COMPOSABLE
// -------------------------------------------------------------------------------------
private class CartItem(
    val product: PosProductEntity,
    initialQuantity: Int = 1,
    initialIsMinPrice: Boolean = false
) {
    var quantity by mutableIntStateOf(initialQuantity)
    var isMinPrice by mutableStateOf(initialIsMinPrice)

    val unitPrice: Double
        get() = if (isMinPrice && (product.minPrice > 0 || product.hasMinPrice)) {
            if (product.minPrice > 0) product.minPrice else product.price
        } else {
            product.price
        }

    val subtotal: Double
        get() = unitPrice * quantity
}

@Composable
private fun CartProductCard(
    item: CartItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prod = item.product
    val hasMin = prod.minPrice > 0 || prod.hasMinPrice

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // 1. Cabecera: FotografÃ­a del producto (ProporciÃ³n 3:1) con estrella (â­/ðŸŒŸ) en la esquina superior derecha
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (prod.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = prod.imageUrl,
                        contentDescription = prod.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prod.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Ãcono de Estrella de Favoritos en la esquina superior derecha
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                ) {
                    Text(
                        text = if (isFavorite) "ðŸŒŸ" else "â­",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Cuerpo: Nombre del producto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = prod.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isFavorite && item.quantity == 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Quitar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Condicional: Control [ MÃ­nimo (Off/On) ] (Solo si el producto tiene precio mÃ­nimo > 0)
            if (hasMin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.isMinPrice) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MÃ­nimo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isMinPrice) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = item.isMinPrice,
                        onCheckedChange = { item.isMinPrice = it },
                        modifier = Modifier.scale(0.75f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF9F0A)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 4. Precio: Precio unitario ($ Valor c/u)
            iOSPill(
                text = "${formatCurrency(item.unitPrice)} c/u",
                color = if (item.isMinPrice) Color(0xFFFF9F0A) else Color(0xFF34C759)
            )

            // Espaciador flexible para asegurar alineaciÃ³n inferior idÃ©ntica entre tarjetas
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))

            // 5. Pie (Siempre anclado al fondo): Selector de cantidad [ âž–  0  âž• ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.quantity > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (item.quantity > 0) {
                            item.quantity--
                            if (item.quantity == 0 && !isFavorite) {
                                onRemove()
                            }
                        }
                    },
                    enabled = item.quantity > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Restar",
                        tint = if (item.quantity > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (item.quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        if (item.quantity < prod.stock) {
                            item.quantity++
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Sumar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaVentaView(
    products: List<PosProductEntity>,
    debtorsList: List<DebtorRecord>,
    onClose: () -> Unit,
    onPaymentSuccess: (customerName: String, ticketItems: Map<PosProductEntity, Int>, totalAmount: Double, paymentMethod: String, transferAmount: Double, cashAmount: Double) -> Unit,
    onDebeSuccess: (customerName: String, ticketItems: Map<PosProductEntity, Int>, totalAmount: Double, abonoAmount: Double, abonoMethod: String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    var productSearchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var showPaymentModal by remember { mutableStateOf(false) }
    var showDebeModal by remember { mutableStateOf(false) }

    var paymentTab by remember { mutableStateOf("COMPLETO") }
    var transferInput by remember { mutableStateOf("") }
    var cashInput by remember { mutableStateOf("") }

    var abonoInput by remember { mutableStateOf("") }
    var abonoMethodSelection by remember { mutableStateOf("Efectivo") }

    val favoriteProductIds = remember { mutableStateListOf<Int>() }
    val cartItems = remember { mutableStateListOf<CartItem>() }

    // Pre-cargar productos estrella/favoritos en la cuadrÃ­cula de "Productos Seleccionados" inicializados con cantidad 0
    LaunchedEffect(products) {
        if (favoriteProductIds.isEmpty() && products.isNotEmpty()) {
            val defaultFavs = products.filter { it.minPrice > 0 || it.hasMinPrice }.map { it.id }.ifEmpty { products.take(6).map { it.id } }
            favoriteProductIds.addAll(defaultFavs)
        }
        if (cartItems.isEmpty() && products.isNotEmpty()) {
            val favs = products.filter { favoriteProductIds.contains(it.id) }
            favs.forEach { prod ->
                cartItems.add(CartItem(product = prod, initialQuantity = 0, initialIsMinPrice = false))
            }
        }
    }

    // Debounce de bÃºsqueda (300ms) para evitar pÃ©rdida de foco en la caja de texto
    LaunchedEffect(productSearchQuery) {
        delay(300)
        debouncedSearchQuery = productSearchQuery
    }

    // Active debtor matching logic
    val matchedDebtor = remember(customerName, debtorsList) {
        if (customerName.trim().length >= 2) {
            debtorsList.find { it.name.contains(customerName.trim(), ignoreCase = true) }
        } else null
    }

    // Customer suggestions
    val customerSuggestions = remember(customerName, debtorsList) {
        if (customerName.isNotBlank()) {
            debtorsList.filter { it.name.contains(customerName.trim(), ignoreCase = true) }
        } else emptyList()
    }

    // Product suggestions based on debounced query
    val productSuggestions = remember(debouncedSearchQuery, products) {
        if (debouncedSearchQuery.isNotBlank()) {
            products.filter {
                it.name.contains(debouncedSearchQuery, ignoreCase = true) ||
                it.aliases.contains(debouncedSearchQuery, ignoreCase = true) ||
                it.category.contains(debouncedSearchQuery, ignoreCase = true)
            }
        } else emptyList()
    }

    val totalAmount = cartItems.sumOf { it.subtotal }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP BAR HEADER
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Nueva Venta",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Punto de Venta RÃ¡pido & Control de Stock",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    iOSPill(text = "POS Activo")
                }
            }

            // MAIN SCROLLABLE AREA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .padding(bottom = 120.dp)
            ) {
                // 1. CAMPO DE CLIENTE
                Text(
                    text = "Cliente",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {
                            customerName = it
                            customerDropdownExpanded = true
                        },
                        placeholder = { Text("Nombre del cliente (ej. Carlos Mendoza)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (customerName.isNotEmpty()) {
                                IconButton(onClick = { customerName = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    DropdownMenu(
                        expanded = customerDropdownExpanded && customerSuggestions.isNotEmpty(),
                        onDismissRequest = { customerDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.90f).background(Color.White)
                    ) {
                        customerSuggestions.forEach { debtor ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(debtor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                            Text("Tel: ${debtor.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Debe: ${formatCurrency(debtor.amountOwed)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9F0A))
                                    }
                                },
                                onClick = {
                                    customerName = debtor.name
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // SUBTLE DEBTOR WARNING INDICATOR
                if (matchedDebtor != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9F0A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "âš ï¸ Cliente deudor activo: ${matchedDebtor.name} registra un saldo pendiente de ${formatCurrency(matchedDebtor.amountOwed)}.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. BUSCADOR DE PRODUCTOS
                Text(
                    text = "Buscador de Productos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = {
                            productSearchQuery = it
                            productDropdownExpanded = true
                        },
                        placeholder = { Text("Escribe para buscar (ej. Aguila, Poker, Guaro...)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (productSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    productSearchQuery = ""
                                    debouncedSearchQuery = ""
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Overlay flotante de sugerencias de bÃºsqueda
                    if (debouncedSearchQuery.isNotBlank() && productDropdownExpanded) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 56.dp)
                                .zIndex(10f)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                if (productSuggestions.isEmpty()) {
                                    Text(
                                        text = "No se encontraron productos coincidentes",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                } else {
                                    productSuggestions.take(6).forEach { prod ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val existing = cartItems.find { it.product.id == prod.id }
                                                    if (existing != null) {
                                                        existing.quantity = maxOf(1, existing.quantity + 1)
                                                    } else {
                                                        cartItems.add(CartItem(product = prod, initialQuantity = 1, initialIsMinPrice = false))
                                                    }
                                                    productSearchQuery = ""
                                                    debouncedSearchQuery = ""
                                                    productDropdownExpanded = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                                Text("CategorÃ­a: ${prod.category} â€¢ Stock: ${prod.stock}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(formatCurrency(prod.price), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF34C759))
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. PRODUCTOS SELECCIONADOS (GRID 2 COLUMNAS CON PRE-CARGA EN ZERO-STATE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Productos Seleccionados",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val activeItemCount = cartItems.filter { it.quantity > 0 }.sumOf { it.quantity }
                    if (activeItemCount > 0) {
                        iOSPill(text = "$activeItemCount Ã­tems en orden")
                    } else {
                        Text(
                            text = "Pre-cargados (0 Ã­tems)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (cartItems.isEmpty()) {
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sin productos seleccionados",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Busca arriba y selecciona los productos para agregarlos.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    cartItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                val isFav = favoriteProductIds.contains(item.product.id)
                                CartProductCard(
                                    item = item,
                                    isFavorite = isFav,
                                    onToggleFavorite = {
                                        if (isFav) {
                                            favoriteProductIds.remove(item.product.id)
                                            if (item.quantity == 0) {
                                                cartItems.remove(item)
                                            }
                                        } else {
                                            favoriteProductIds.add(item.product.id)
                                        }
                                    },
                                    onRemove = { cartItems.remove(item) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 4. BARRA FIJA INFERIOR (RESUMEN Y ACCIONES)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                // TOTAL GRANDE Y EN NEGRITA ALINEADO A LA DERECHA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = formatCurrency(totalAmount),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BOTONES LADO A LADO: [ Pago ] y [ Debe ]
                val hasActiveItems = cartItems.any { it.quantity > 0 }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    iOSButton(
                        text = "Pago",
                        onClick = { showPaymentModal = true },
                        enabled = hasActiveItems,
                        modifier = Modifier.weight(1f).height(50.dp)
                    )

                    iOSButton(
                        text = "Debe",
                        onClick = { showDebeModal = true },
                        enabled = hasActiveItems,
                        modifier = Modifier.weight(1f).height(50.dp)
                    )
                }
            }
        }
    }

    // DIÃLOGO DE SELECCIÃ“N DE MÃ‰TODO DE PAGO ("Pago")
    if (showPaymentModal) {
        val activeMap = remember(cartItems) { cartItems.filter { it.quantity > 0 }.associate { it.product to it.quantity } }
        AlertDialog(
            onDismissRequest = { showPaymentModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Seleccionar Forma de Pago",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Total a Cobrar: ${formatCurrency(totalAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    iOSSegmented(
                        options = listOf("CompletÃ³", "Dividido"),
                        selectedIndex = if (paymentTab == "COMPLETO") 0 else 1,
                        onSelect = { idx -> paymentTab = if (idx == 0) "COMPLETO" else "DIVIDIDO" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (paymentTab == "COMPLETO") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    showPaymentModal = false
                                    onPaymentSuccess(customerName, activeMap, totalAmount, "Efectivo", 0.0, totalAmount)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("ðŸ’µ", fontSize = 22.sp)
                            }

                            Button(
                                onClick = {
                                    showPaymentModal = false
                                    onPaymentSuccess(customerName, activeMap, totalAmount, "Transferencia", totalAmount, 0.0)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("ðŸ“²", fontSize = 22.sp)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = transferInput,
                            onValueChange = { transferInput = it },
                            label = { Text("Monto en Transferencia ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = cashInput,
                            onValueChange = { cashInput = it },
                            label = { Text("Monto en Efectivo ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (paymentTab == "DIVIDIDO") {
                    val activeMap = remember(cartItems) { cartItems.filter { it.quantity > 0 }.associate { it.product to it.quantity } }
                    val tVal = transferInput.toDoubleOrNull() ?: 0.0
                    val cVal = cashInput.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            showPaymentModal = false
                            onPaymentSuccess(customerName, activeMap, totalAmount, "Dividido", tVal, cVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirmar Dividido", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÃLOGO DE REGISTRO DE DEUDOR ("Debe")
    if (showDebeModal) {
        val activeMap = remember(cartItems) { cartItems.filter { it.quantity > 0 }.associate { it.product to it.quantity } }
        AlertDialog(
            onDismissRequest = { showDebeModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Registrar Deudor (Fiado)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Total Venta: ${formatCurrency(totalAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Abono Inicial (Opcional):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = abonoInput,
                        onValueChange = { abonoInput = it },
                        placeholder = { Text("Escribe monto abonado (ej. 10000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    val abonoVal = abonoInput.toDoubleOrNull() ?: 0.0
                    if (abonoVal > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("MÃ©todo de Abono:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Efectivo" to "ðŸ’µ", "Transferencia" to "ðŸ“²").forEach { (method, iconStr) ->
                                val isSelected = abonoMethodSelection == method
                                Button(
                                    onClick = { abonoMethodSelection = method },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = iconStr,
                                        fontSize = 20.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deuda Restante: ${formatCurrency((totalAmount - abonoVal).coerceAtLeast(0.0))}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF453A)
                        )
                    }
                }
            },
            confirmButton = {
                val abonoVal = abonoInput.toDoubleOrNull() ?: 0.0
                val method = if (abonoVal > 0) abonoMethodSelection else ""
                Button(
                    onClick = {
                        showDebeModal = false
                        onDebeSuccess(customerName, activeMap, totalAmount, abonoVal, method)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirmar Deuda", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebeModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
