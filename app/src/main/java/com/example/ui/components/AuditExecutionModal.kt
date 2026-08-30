package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AuditReport

// TEMPORAL - AUDIT_EXECUTION UI
@Composable
fun AuditExecutionModal(
    report: AuditReport,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auditoría de Sincronización",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "TEMPORAL - AUDIT_EXECUTION UI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cerrar auditoría")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
                    // [0] DIAGNÓSTICO DE CONEXIÓN GOOGLE SHEETS
                    item {
                        AuditSectionHeader("[0] DIAGNÓSTICO DE CONEXIÓN GOOGLE SHEETS")
                        val globalStatus = when {
                            report.errorMessage != null -> "EXCEPTION"
                            report.sheetsDiagnostics.any { it.sheetsFetchDiagnosticStatus == "FALLBACK_ERROR" } -> "FALLBACK_ERROR"
                            report.sheetsDiagnostics.any { it.sheetsFetchDiagnosticStatus == "HTTP_ERROR" } -> "HTTP_ERROR"
                            report.sheetsDiagnostics.any { it.sheetsFetchDiagnosticStatus == "EXCEPTION" } -> "EXCEPTION"
                            report.sheetsDiagnostics.any { it.sheetsFetchDiagnosticStatus == "FALLBACK_USED" } -> "FALLBACK_USED"
                            report.sheetsDiagnostics.any { it.sheetsFetchDiagnosticStatus == "SUCCESS" } -> "SUCCESS"
                            else -> "EMPTY_RESPONSE"
                        }
                        val lastError = report.errorMessage 
                            ?: report.sheetsDiagnostics.firstOrNull { it.exceptionMessage.isNotBlank() }?.exceptionMessage
                            ?: "Ninguno"
                        val lastHttpStatus = report.sheetsDiagnostics.firstOrNull { it.httpStatusCode != null }?.httpStatusCode?.toString() ?: "N/A"
                        val fallbackUsedStr = if (report.sheetsDiagnostics.any { it.fallbackUsed }) "SÍ (Apps Script)" else "NO (API v4 directo)"
                        val responseDetail = if (report.sheetsDiagnostics.isEmpty()) "Sin peticiones registradas" 
                            else "Peticiones realizadas: ${report.sheetsDiagnostics.size} | Exitosas: ${report.sheetsDiagnostics.count { it.isSuccessful || !it.fallbackEmptyResponse }}"

                        AuditCard {
                            Text("Estado Google Sheets: $globalStatus", fontWeight = FontWeight.Bold, color = if (globalStatus == "SUCCESS" || globalStatus == "FALLBACK_USED") Color(0xFF2E7D32) else Color(0xFFC62828))
                            Text("Último error: $lastError", color = if (lastError != "Ninguno") Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface)
                            Text("HTTP status: $lastHttpStatus")
                            Text("Fallback Apps Script: $fallbackUsedStr")
                            Text("Detalle de respuesta: $responseDetail")
                        }

                        if (report.sheetsDiagnostics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("[AUDIT SHEETS REQUEST]", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            report.sheetsDiagnostics.forEach { diag ->
                                AuditCard {
                                    Text("Tabla: ${diag.tableName} | Status: ${diag.sheetsFetchDiagnosticStatus}", fontWeight = FontWeight.Bold)
                                    Text("SheetName: '${diag.sheetName}' | Rango exacto: '${diag.rangeParam}'")
                                    Text("URL/Base: ${diag.sanitizedUrl}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("Método: ${diag.httpMethod} | HTTP Status: ${diag.httpStatusCode ?: "N/A"} | Respuesta Exitosa: ${diag.isSuccessful}")
                                    Text("Filas recibidas: ${diag.rowsReceived} | Columnas: ${diag.columnCount} | Respuesta NULL: ${diag.isNullResponse}")
                                    if (diag.hasException) {
                                        Text("Excepción: [${diag.exceptionType}] ${diag.exceptionMessage}", color = Color(0xFFC62828), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    }
                                    Text("Fallback Apps Script utilizado: ${diag.fallbackUsed} | Fallback HTTP Status: ${diag.fallbackHttpStatusCode ?: "N/A"} | Fallback Respuesta Vacía: ${diag.fallbackEmptyResponse}")
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // [A] EMPRESA
                    item {
                        AuditSectionHeader("[A] EMPRESA SELECCIONADA")
                        AuditCard {
                            Text("Empresa Nombre: ${report.companyName}", fontWeight = FontWeight.SemiBold)
                            Text("CompanyCode Original: '${report.companyCodeOriginal}'")
                            Text("SheetName Utilizado: '${report.sheetName}'")
                            Text("CleanCode (Final): '${report.cleanCode}'", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // [B] GOOGLE SHEETS METADATA
                    item {
                        AuditSectionHeader("[B] GOOGLE SHEETS (METADATOS DE TABLAS)")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.tablesMetadata.forEach { meta ->
                                AuditCard {
                                    Text("Tabla: ${meta.tableName}", fontWeight = FontWeight.Bold)
                                    Text("Rango solicitado: '${meta.range}'")
                                    Text("Filas recibidas raw: ${meta.rowsReceived}")
                                    Text("Columnas 1ra fila: ${meta.columnCount}")
                                    Text("Filas post-encabezado: ${meta.rowsPostHeader}", color = if (meta.rowsPostHeader > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                                }
                            }
                        }
                    }

                    // [C] INVENTARIO
                    item {
                        AuditSectionHeader("[C] INVENTARIO FILA POR FILA (${report.inventoryRows.size})")
                    }
                    if (report.inventoryRows.isEmpty()) {
                        item { AuditEmptyState("No hay filas registradas o la respuesta fue vacía.") }
                    } else {
                        items(report.inventoryRows) { row ->
                            AuditRowCard(
                                title = "Fila #${row.rowIndex} | Nom_Producto: '${row.field1}'",
                                status = row.result,
                                statusColor = when(row.result) {
                                    "INSERTADA" -> Color(0xFF2E7D32)
                                    "ACTUALIZADA" -> Color(0xFF0288D1)
                                    else -> Color(0xFFC62828)
                                }
                            ) {
                                Text("Cantidad: ${row.field2} | Costo: ${row.field3}")
                                Text("Precio_Venta original: '${row.rawField4}' -> parseMoney: ${row.parsedField4}")
                                Text("Precio_Minimo: '${row.rawField5}' -> parseMoney: ${row.parsedField5}")
                                Text("Motivo/Detalle: ${row.reason}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }

                    // [D] VENTAS
                    item {
                        AuditSectionHeader("[D] VENTAS FILA POR FILA (${report.salesRows.size})")
                    }
                    if (report.salesRows.isEmpty()) {
                        item { AuditEmptyState("No hay filas de ventas registradas.") }
                    } else {
                        items(report.salesRows) { row ->
                            AuditRowCard(
                                title = "Fila #${row.rowIndex} | Producto: '${row.field3}'",
                                status = row.result,
                                statusColor = if (row.result == "INSERTADA") Color(0xFF2E7D32) else Color(0xFFC62828)
                            ) {
                                Text("Fecha: '${row.field1}' | Cliente: '${row.field2}' | Cantidad: ${row.field4}")
                                Text("Transferencia: '${row.rawField5}' | Efectivo: '${row.rawField6}'")
                                Text("Total original: '${row.rawField7}' -> parseMoney: ${row.parsedField7}")
                                Text("Motivo/Detalle: ${row.reason}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }

                    // [E] GASTOS
                    item {
                        AuditSectionHeader("[E] GASTOS FILA POR FILA (${report.expensesRows.size})")
                    }
                    if (report.expensesRows.isEmpty()) {
                        item { AuditEmptyState("No hay filas de gastos registradas.") }
                    } else {
                        items(report.expensesRows) { row ->
                            AuditRowCard(
                                title = "Fila #${row.rowIndex} | Concepto: '${row.field2}'",
                                status = row.result,
                                statusColor = if (row.result == "INSERTADA") Color(0xFF2E7D32) else Color(0xFFC62828)
                            ) {
                                Text("Fecha: '${row.field1}'")
                                Text("Monto original: '${row.rawField3}' -> parseMoney: ${row.parsedField3}")
                                Text("Motivo/Detalle: ${row.reason}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }

                    // [F] ROOM
                    item {
                        AuditSectionHeader("[F] ROOM (REGISTROS POST-SINCRONIZACIÓN)")
                        AuditCard {
                            Text("Productos en Room: ${report.roomProductCount}")
                            Text("Ventas en Room: ${report.roomSaleCount}")
                            Text("Transacciones en Room: ${report.roomTransactionCount}")
                            Text("Usuarios en Room: ${report.roomUserCount}")
                            Text("Suma Total Ventas: $${report.roomSalesSum}")
                            Text("Suma Total Gastos: $${report.roomExpensesSum}")
                        }
                    }

                    // [G] ACTIVE COMPANY
                    item {
                        AuditSectionHeader("[G] ACTIVE COMPANY EN VIEWMODEL")
                        AuditCard {
                            Text("activeCompanyCode: '${report.activeCompanyCode}'")
                            Text("selectedCompany: '${report.selectedCompanyInfo}'")
                            Text("selectedRemoteCompany: '${report.selectedRemoteCompanyInfo}'")
                            Text("currentUser.companyCode: '${report.currentUserCompanyCode}'")
                        }
                    }

                    // [H] UI
                    item {
                        AuditSectionHeader("[H] UI VALORES FINALES CALCULADOS")
                        AuditCard {
                            Text("totalIncome: $${report.uiTotalIncome}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("totalExpenses: $${report.uiTotalExpenses}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("balance (Neto): $${report.uiBalance}", fontWeight = FontWeight.Bold)
                            Text("Cantidad de productos mostrados UI: ${report.uiProductCount}")
                            Text("Cantidad de ventas mostradas UI: ${report.uiSaleCount}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun AuditCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun AuditRowCard(
    title: String,
    status: String,
    statusColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun AuditEmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 8.dp)
    )
}
