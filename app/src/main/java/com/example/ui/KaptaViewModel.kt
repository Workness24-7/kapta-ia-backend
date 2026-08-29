package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.data.local.entity.IaFunctionEntity
import com.example.data.local.entity.FuncionLib
import com.example.data.local.entity.FinancialTransaction
import com.example.data.local.entity.FinancialTransactionEntity
import com.example.data.local.entity.PosProductEntity
import com.example.data.local.entity.PosSaleEntity
import com.example.data.repository.KaptaRepository
import com.example.data.remote.SheetsDatabaseService
import com.example.data.remote.SheetsSyncManager
import com.example.data.remote.SyncState
import com.example.data.remote.SheetDataResult
import com.example.data.remote.RemoteCompany
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.remote.SheetsTableDiagnostic
import com.example.util.KaptaCurrency

// TEMPORAL - AUDIT_EXECUTION UI
data class AuditTableMetadata(
    val tableName: String,
    val range: String,
    val rowsReceived: Int,
    val columnCount: Int,
    val rowsPostHeader: Int
)

// TEMPORAL - AUDIT_EXECUTION UI
data class AuditRowDetail(
    val rowIndex: Int,
    val field1: String = "",
    val field2: String = "",
    val field3: String = "",
    val field4: String = "",
    val rawField3: String = "",
    val parsedField3: Double = 0.0,
    val rawField4: String = "",
    val parsedField4: Double = 0.0,
    val rawField5: String = "",
    val parsedField5: Double = 0.0,
    val rawField6: String = "",
    val rawField7: String = "",
    val parsedField7: Double = 0.0,
    val result: String = "",
    val reason: String = ""
)

// TEMPORAL - AUDIT_EXECUTION UI
data class AuditReport(
    val companyName: String = "",
    val companyCodeOriginal: String = "",
    val sheetName: String = "",
    val cleanCode: String = "",
    val tablesMetadata: List<AuditTableMetadata> = emptyList(),
    val inventoryRows: List<AuditRowDetail> = emptyList(),
    val salesRows: List<AuditRowDetail> = emptyList(),
    val expensesRows: List<AuditRowDetail> = emptyList(),
    val roomProductCount: Int = 0,
    val roomSaleCount: Int = 0,
    val roomTransactionCount: Int = 0,
    val roomUserCount: Int = 0,
    val roomSalesSum: Double = 0.0,
    val roomExpensesSum: Double = 0.0,
    val activeCompanyCode: String = "",
    val selectedCompanyInfo: String = "",
    val selectedRemoteCompanyInfo: String = "",
    val currentUserCompanyCode: String = "",
    val uiTotalIncome: Double = 0.0,
    val uiTotalExpenses: Double = 0.0,
    val uiBalance: Double = 0.0,
    val uiProductCount: Int = 0,
    val uiSaleCount: Int = 0,
    val errorMessage: String? = null,
    // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
    val sheetsDiagnostics: List<SheetsTableDiagnostic> = emptyList()
)

class KaptaViewModel(application: Application) : AndroidViewModel(application) {
    private val _isSavingCompany = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSavingCompany: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSavingCompany.asStateFlow()

    private val repository: KaptaRepository
    val sheetsService = SheetsDatabaseService()
    val syncManager = SheetsSyncManager(sheetsService, viewModelScope)
    val syncState: StateFlow<SyncState> = sheetsService.syncState

    // TEMPORAL - AUDIT_EXECUTION UI
    private val _auditReport = MutableStateFlow<AuditReport?>(null)
    // TEMPORAL - AUDIT_EXECUTION UI
    val auditReport: StateFlow<AuditReport?> = _auditReport.asStateFlow()

    // TEMPORAL - AUDIT_EXECUTION UI
    fun dismissAuditReport() {
        _auditReport.value = null
    }
    
    init {
        val db = AppDatabase.getDatabase(application)
        val dao = db.kaptaDao()
        repository = KaptaRepository(dao)

        viewModelScope.launch(Dispatchers.IO) {
            syncCompaniesFromSheets()
        }
    }

    fun syncCompaniesFromSheets() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = sheetsService.listarEmpresas()
                if (list.isNotEmpty()) {
                    _remoteCompanies.value = list
                }
                repository.syncCompaniesFromSheets(sheetsService)
            } catch (e: Exception) {
                Log.e("KaptaViewModel", "Error al sincronizar empresas desde Google Sheets: ${e.message}")
            }
        }
    }

    // ============================================================
    // FINANZAS KAPTA IA: ingresos por planes + gastos propios
    // ============================================================
    private val _finanzasKapta = MutableStateFlow<com.example.data.remote.FinanzasKaptaResumen?>(null)
    val finanzasKapta: StateFlow<com.example.data.remote.FinanzasKaptaResumen?> = _finanzasKapta.asStateFlow()

    fun refreshFinanzasKapta() {
        viewModelScope.launch(Dispatchers.IO) {
            _finanzasKapta.value = sheetsService.listarFinanzasKapta()
        }
    }

    fun registrarGastoKapta(
        concepto: String,
        monto: String,
        categoria: String,
        metodoPago: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = sheetsService.registrarFinanzaKapta(
                tipo = "Egreso",
                concepto = concepto,
                monto = monto,
                categoria = categoria,
                metodoPago = metodoPago,
                usuario = currentUser.value?.name ?: "SuperAdmin"
            )
            if (ok) refreshFinanzasKapta()
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    /**
     * Compra/renovación de plan de una empresa. Actualiza Room local con la
     * fechaVencimiento que devuelve el backend y refresca finanzas.
     */
    fun comprarPlan(
        company: CompanyEntity,
        plan: String,
        tiempo: String,
        monto: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val fv = sheetsService.comprarPlan(
                codigo = company.code,
                plan = plan,
                tiempo = tiempo,
                monto = monto,
                usuario = currentUser.value?.name ?: "SuperAdmin"
            )
            if (fv != null) {
                val dias = runCatching {
                    java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(),
                        java.time.LocalDate.parse(fv.substring(0, 10))
                    ).toInt()
                }.getOrDefault(30)
                repository.updateCompany(
                    company.copy(plan = plan, durationTime = tiempo, expirationDays = dias, status = "Activo")
                )
                refreshFinanzasKapta()
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(fv != null) }
        }
    }

    /**
     * 3. Enrutamiento Dinámico de Hojas (Multi-tenant): fetchBusinessData(sheetName, tableName)
     */
    suspend fun fetchBusinessData(sheetName: String, tableName: String? = null): SheetDataResult? {
        return sheetsService.fetchBusinessData(sheetName, tableName)
    }

    private fun parseMoney(value: String?): Double {
        val raw = value?.trim() ?: return 0.0
        if (raw.isBlank()) return 0.0
        
        var cleaned = raw.replace("$", "")
            .replace("USD", "", ignoreCase = true)
            .replace("COP", "", ignoreCase = true)
            .replace(" ", "")
            .replace(" ", "")
            .trim()
            
        if (cleaned.isBlank()) return 0.0

        return try {
            if (cleaned.contains(",") && cleaned.contains(".")) {
                if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) {
                    cleaned = cleaned.replace(".", "").replace(",", ".")
                } else {
                    cleaned = cleaned.replace(",", "")
                }
            } else if (cleaned.contains(",")) {
                val parts = cleaned.split(",")
                if (parts.size == 2 && parts[1].length == 3 && parts[0].length <= 3) {
                    cleaned = cleaned.replace(",", "")
                } else {
                    cleaned = cleaned.replace(",", ".")
                }
            } else if (cleaned.contains(".")) {
                val parts = cleaned.split(".")
                if (parts.size > 2) {
                    cleaned = cleaned.replace(".", "")
                } else if (parts.size == 2 && parts[1].length == 3) {
                    cleaned = cleaned.replace(".", "")
                }
            }
            cleaned.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Sincroniza y descarga desde Google Sheets hacia la BD de Room (Gastos, Deudores, Ventas, Inventario)
     */
    suspend fun syncTenantBusinessDataFromSheets(sheetName: String, companyCode: String) {
        withContext(Dispatchers.IO) {
            // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
            sheetsService.clearFetchDiagnostics()

            // TEMPORAL - AUDIT_EXECUTION UI
            val auditTablesMeta = mutableListOf<AuditTableMetadata>()
            val auditInvRows = mutableListOf<AuditRowDetail>()
            val auditSalesRows = mutableListOf<AuditRowDetail>()
            val auditExpensesRows = mutableListOf<AuditRowDetail>()

            try {
                val cleanCode = companyCode.trim().uppercase()

                // [A] Empresa Seleccionada Diagnostic Log
                val selCompany = _selectedCompany.value
                val selRemoteCompany = _selectedRemoteCompany.value
                val currUser = _currentUser.value
                Log.d("AUDIT_EXECUTION", "==================================================")
                Log.d("AUDIT_EXECUTION", "=== [A] EMPRESA SELECCIONADA DIAGNÓSTICO ===")
                Log.d("AUDIT_EXECUTION", "Nombre empresa seleccionada: '${selCompany?.name ?: selRemoteCompany?.nombre ?: sheetName}'")
                Log.d("AUDIT_EXECUTION", "Code original recibido: '$companyCode'")
                Log.d("AUDIT_EXECUTION", "SheetName utilizado: '$sheetName'")
                Log.d("AUDIT_EXECUTION", "CompanyCode final utilizado (cleanCode): '$cleanCode'")
                Log.d("AUDIT_EXECUTION", "--------------------------------------------------")

                fun logTableMetadata(tableName: String, res: SheetDataResult?) {
                    Log.d("AUDIT_EXECUTION", "=== [B] METADATOS TABLA: $tableName ===")
                    if (res == null) {
                        Log.d("AUDIT_EXECUTION", "Tabla: $tableName | Resultado: NULL")
                        // TEMPORAL - AUDIT_EXECUTION UI
                        auditTablesMeta.add(
                            AuditTableMetadata(
                                tableName = tableName,
                                range = "N/A",
                                rowsReceived = 0,
                                columnCount = 0,
                                rowsPostHeader = 0
                            )
                        )
                    } else {
                        Log.d("AUDIT_EXECUTION", "Nombre de Tabla: $tableName")
                        Log.d("AUDIT_EXECUTION", "Rango solicitado: '${res.range}'")
                        Log.d("AUDIT_EXECUTION", "Cantidad de filas recibidas: ${res.rawValues.size}")
                        Log.d("AUDIT_EXECUTION", "Cantidad de columnas 1ra fila: ${res.rawValues.firstOrNull()?.size ?: 0}")
                        Log.d("AUDIT_EXECUTION", "Filas después de eliminar encabezado: ${res.rows.size}")
                        // TEMPORAL - AUDIT_EXECUTION UI
                        auditTablesMeta.add(
                            AuditTableMetadata(
                                tableName = tableName,
                                range = res.range,
                                rowsReceived = res.rawValues.size,
                                columnCount = res.rawValues.firstOrNull()?.size ?: 0,
                                rowsPostHeader = res.rows.size
                            )
                        )
                    }
                    Log.d("AUDIT_EXECUTION", "--------------------------------------------------")
                }

                // 1. Download Gastos (AS:BF) -> FinancialTransactionEntity
                val gastosRes = sheetsService.fetchBusinessData(sheetName, "Gastos")
                logTableMetadata("Gastos", gastosRes)
                if (gastosRes != null && gastosRes.rows.isNotEmpty()) {
                    val currentTx = repository.getTransactionsByCompany(cleanCode).firstOrNull() ?: emptyList()
                    Log.d("AUDIT_EXECUTION", "=== [E] PROCESANDO FILAS DE GASTOS (Filas: ${gastosRes.rows.size}) ===")
                    for ((index, row) in gastosRes.rows.withIndex()) {
                        val rawDate = row.getOrNull(1)
                        val dateStr = rawDate?.trim() ?: ""
                        val category = row.getOrNull(3)?.trim() ?: "Operativo"
                        val rawConcept4 = row.getOrNull(4)?.trim()
                        val rawConcept5 = row.getOrNull(5)?.trim()
                        val concept = rawConcept4?.ifBlank { null } ?: rawConcept5 ?: "Gasto"
                        val rawMonto = row.getOrNull(7)
                        val amount = parseMoney(rawMonto)

                        val isValid = dateStr.isNotBlank() && concept.isNotBlank() && amount > 0
                        var isInserted = false
                        var isDiscarded = false
                        var discardReason = ""

                        if (isValid) {
                            val exists = currentTx.any {
                                it.subtitle.equals(concept, ignoreCase = true) &&
                                abs(it.amount - amount) < 0.01 &&
                                it.dateString.equals(dateStr, ignoreCase = true)
                            }
                            if (!exists) {
                                isInserted = true
                                repository.insertTransaction(
                                    FinancialTransactionEntity(
                                        companyCode = cleanCode,
                                        title = cleanCode,
                                        subtitle = concept,
                                        amount = amount,
                                        dateString = dateStr,
                                        isExpense = true,
                                        category = category,
                                        isSynced = true
                                    )
                                )
                            } else {
                                isDiscarded = true
                                discardReason = "Ya existe transacción equivalente en Room local (subtitle='$concept', amount=$amount, date='$dateStr')"
                            }
                        } else {
                            isDiscarded = true
                            discardReason = buildString {
                                if (dateStr.isBlank()) append("row[1] Fecha vacía. ")
                                if (concept.isBlank()) append("row[4]/row[5] Concepto vacío. ")
                                if (amount <= 0) append("row[7] Monto parseMoney <= 0 (original='$rawMonto', parsed=$amount). ")
                            }
                        }

                        Log.d("AUDIT_EXECUTION", "[E GASTOS Fila #$index] row[1] Fecha='$rawDate', row[4] Concepto='$rawConcept4', row[7] Monto original='$rawMonto' -> parseMoney=$amount | RESULTADO: ${if (isInserted) "INSERTADA" else "DESCARTADA"} | MOTIVO: ${if (isInserted) "N/A" else discardReason}")

                        // TEMPORAL - AUDIT_EXECUTION UI
                        auditExpensesRows.add(
                            AuditRowDetail(
                                rowIndex = index + 1,
                                field1 = dateStr,
                                field2 = concept,
                                rawField3 = rawMonto ?: "",
                                parsedField3 = amount,
                                result = if (isInserted) "INSERTADA" else "DESCARTADA",
                                reason = if (isInserted) "N/A" else discardReason
                            )
                        )
                    }
                }

                // 2. Download Deudores (AJ:AQ) -> PosSaleEntity (Fiado)
                val deudoresRes = sheetsService.fetchBusinessData(sheetName, "Deudores")
                logTableMetadata("Deudores", deudoresRes)
                if (deudoresRes != null && deudoresRes.rows.isNotEmpty()) {
                    val currentSales = repository.getSalesByCompany(cleanCode).firstOrNull() ?: emptyList()
                    for (row in deudoresRes.rows) {
                        val dateStr = row.getOrNull(0)?.trim() ?: ""
                        val clientName = row.getOrNull(1)?.trim() ?: ""
                        val productName = row.getOrNull(2)?.trim() ?: ""
                        val quantity = row.getOrNull(3)?.trim()?.toIntOrNull() ?: 1
                        val totalAmount = parseMoney(row.getOrNull(7))

                        if (dateStr.isNotBlank() && (clientName.isNotBlank() || productName.isNotBlank()) && totalAmount > 0) {
                            val exists = currentSales.any {
                                it.clientName.equals(clientName, ignoreCase = true) &&
                                it.productName.equals(productName, ignoreCase = true) &&
                                abs(it.totalAmount - totalAmount) < 0.01
                            }
                            if (!exists) {
                                repository.insertSale(
                                    PosSaleEntity(
                                        companyCode = cleanCode,
                                        clientName = clientName.ifBlank { "Cliente Fiado" },
                                        productName = productName.ifBlank { "Venta a Crédito" },
                                        quantity = quantity,
                                        unitPrice = if (quantity > 0) totalAmount / quantity else totalAmount,
                                        totalAmount = totalAmount,
                                        paymentMethod = "Fiado",
                                        itemCount = quantity,
                                        isSynced = true
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Download Ventas (N:AH) -> PosSaleEntity
                val ventasRes = sheetsService.fetchBusinessData(sheetName, "Ventas")
                logTableMetadata("Ventas", ventasRes)
                if (ventasRes != null && ventasRes.rows.isNotEmpty()) {
                    val currentSales = repository.getSalesByCompany(cleanCode).firstOrNull() ?: emptyList()
                    val matchedSalesIds = mutableSetOf<Int>()
                    Log.d("AUDIT_EXECUTION", "=== [D] PROCESANDO FILAS DE VENTAS (Filas: ${ventasRes.rows.size}) ===")
                    for ((index, row) in ventasRes.rows.withIndex()) {
                        val rawIdVenta = row.getOrNull(0)?.trim() ?: ""
                        val rawDate = row.getOrNull(1)
                        val dateStr = rawDate?.trim() ?: ""

                        // FASE 1 — TRAZAR LA VENTA REAL (Diagnóstico temporal para V-00001)
                        if (rawIdVenta.equals("V-00001", ignoreCase = true) || rawIdVenta.isNotBlank()) {
                            Log.d("AUDIT_EXECUTION", "VENTA ${if (rawIdVenta.isNotBlank()) rawIdVenta else "V-00001"}")
                            for (i in 0..12) {
                                Log.d("AUDIT_EXECUTION", "row[$i] = ${row.getOrNull(i) ?: ""}")
                            }
                        }

                        // Omite encabezado si estuviera presente
                        if (rawIdVenta.equals("Id_Venta", ignoreCase = true) || dateStr.equals("Fecha", ignoreCase = true)) {
                            continue
                        }

                        val effectiveDateStr = if (dateStr.isBlank()) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) else dateStr

                        val rawClient = row.getOrNull(3)
                        val clientName = rawClient?.trim() ?: ""
                        val effectiveClientName = if (clientName.isBlank()) "Cliente General" else clientName

                        val rawProduct = row.getOrNull(5)
                        val productName = rawProduct?.trim() ?: ""
                        val effectiveProductName = when {
                            productName.isNotBlank() -> productName
                            clientName.isNotBlank() -> "Venta - $clientName"
                            else -> row.getOrNull(4)?.trim()?.ifBlank { null } ?: "Venta POS"
                        }

                        val rawQuantity = row.getOrNull(6)
                        val quantity = rawQuantity?.trim()?.toIntOrNull() ?: 1
                        val rawTransfer = row.getOrNull(10)
                        val transferencia = parseMoney(rawTransfer)
                        val rawCash = row.getOrNull(11)
                        val efectivo = parseMoney(rawCash)
                        val rawTotal = row.getOrNull(12)
                        var totalAmount = parseMoney(rawTotal)

                        // Fallback para totalAmount si row[12] está vacío o <= 0
                        if (totalAmount <= 0) {
                            if (transferencia + efectivo > 0) {
                                totalAmount = transferencia + efectivo
                            } else {
                                val rawUnitPrice = row.getOrNull(7)
                                val unitPrice = parseMoney(rawUnitPrice)
                                if (unitPrice > 0 && quantity > 0) {
                                    totalAmount = unitPrice * quantity
                                } else {
                                    totalAmount = parseMoney(row.getOrNull(8)) // Subtotal
                                }
                            }
                        }

                        val paymentMethod = when {
                            transferencia > 0 && efectivo > 0 -> "Mixto"
                            transferencia > 0 -> "Transferencia"
                            else -> "Efectivo"
                        }

                        val isValid = totalAmount > 0
                        var isInserted = false
                        var isDiscarded = false
                        var discardReason = ""

                        if (isValid) {
                            val rowDateObj = parseDateStr(effectiveDateStr)
                            val rowTimestamp = rowDateObj?.time ?: System.currentTimeMillis()
                            val rowFormattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(rowTimestamp))

                            val matchedSale = currentSales.find { sale ->
                                sale.id !in matchedSalesIds &&
                                (sale.clientName.equals(effectiveClientName, ignoreCase = true) || sale.clientName.equals(clientName, ignoreCase = true)) &&
                                (sale.productName.equals(effectiveProductName, ignoreCase = true) || sale.productName.equals(productName, ignoreCase = true)) &&
                                kotlin.math.abs(sale.totalAmount - totalAmount) < 0.01 &&
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(sale.timestamp)) == rowFormattedDate
                            }

                            if (matchedSale != null) {
                                matchedSalesIds.add(matchedSale.id)
                                isDiscarded = true
                                discardReason = "Ya existe venta equivalente en Room local (ID=${matchedSale.id}, fecha=$rowFormattedDate, cliente='$effectiveClientName', producto='$effectiveProductName', total=$totalAmount)"
                            } else {
                                isInserted = true
                                val saleEntity = PosSaleEntity(
                                    companyCode = cleanCode,
                                    clientName = effectiveClientName,
                                    productName = effectiveProductName,
                                    quantity = quantity,
                                    unitPrice = if (quantity > 0) totalAmount / quantity else totalAmount,
                                    totalAmount = totalAmount,
                                    paymentMethod = paymentMethod,
                                    transferAmount = transferencia,
                                    cashAmount = efectivo,
                                    timestamp = rowTimestamp,
                                    itemCount = quantity,
                                    isSynced = true
                                )

                                // FASE 2 — TRAZAR EL MODELO
                                Log.d("AUDIT_EXECUTION", "V-00001 MODEL")
                                Log.d("AUDIT_EXECUTION", "clientName = ${saleEntity.clientName}")
                                Log.d("AUDIT_EXECUTION", "productName = ${saleEntity.productName}")
                                Log.d("AUDIT_EXECUTION", "quantity = ${saleEntity.quantity}")
                                Log.d("AUDIT_EXECUTION", "unitPrice = ${saleEntity.unitPrice}")
                                Log.d("AUDIT_EXECUTION", "subtotal = ${saleEntity.quantity * saleEntity.unitPrice}")
                                Log.d("AUDIT_EXECUTION", "discount = 0%")
                                Log.d("AUDIT_EXECUTION", "transferAmount = ${saleEntity.transferAmount}")
                                Log.d("AUDIT_EXECUTION", "cashAmount = ${saleEntity.cashAmount}")
                                Log.d("AUDIT_EXECUTION", "totalAmount = ${saleEntity.totalAmount}")
                                Log.d("AUDIT_EXECUTION", "timestamp = ${saleEntity.timestamp}")

                                repository.insertSale(saleEntity)

                                // FASE 4 — COMPROBAR ROOM
                                val salesInRoom = repository.getSalesByCompany(cleanCode).firstOrNull() ?: emptyList()
                                val roomSale = salesInRoom.lastOrNull()
                                if (roomSale != null) {
                                    Log.d("AUDIT_EXECUTION", "ROOM V-00001")
                                    Log.d("AUDIT_EXECUTION", "quantity = ${roomSale.quantity}")
                                    Log.d("AUDIT_EXECUTION", "unitPrice = ${roomSale.unitPrice}")
                                    Log.d("AUDIT_EXECUTION", "subtotal = ${roomSale.quantity * roomSale.unitPrice}")
                                    Log.d("AUDIT_EXECUTION", "transferAmount = ${roomSale.transferAmount}")
                                    Log.d("AUDIT_EXECUTION", "cashAmount = ${roomSale.cashAmount}")
                                    Log.d("AUDIT_EXECUTION", "totalAmount = ${roomSale.totalAmount}")
                                    Log.d("AUDIT_EXECUTION", "timestamp = ${roomSale.timestamp}")
                                }
                            }
                        } else {
                            isDiscarded = true
                            discardReason = buildString {
                                if (totalAmount <= 0) append("row[12] Total parseMoney <= 0 (original='$rawTotal', parsed=$totalAmount). ")
                            }
                        }

                        Log.d("AUDIT_EXECUTION", "[D VENTAS Fila #$index] row[1] Fecha='$rawDate', row[3] Cliente='$rawClient', row[5] Producto='$rawProduct', row[6] Cantidad='$rawQuantity', row[10] Transferencia='$rawTransfer', row[11] Efectivo='$rawCash', row[12] Total original='$rawTotal' -> parseMoney=$totalAmount | RESULTADO: ${if (isInserted) "INSERTADA" else "DESCARTADA"} | MOTIVO: ${if (isInserted) "N/A" else discardReason}")

                        // TEMPORAL - AUDIT_EXECUTION UI
                        auditSalesRows.add(
                            AuditRowDetail(
                                rowIndex = index + 1,
                                field1 = effectiveDateStr,
                                field2 = effectiveClientName,
                                field3 = effectiveProductName,
                                field4 = quantity.toString(),
                                rawField5 = rawTransfer ?: "",
                                rawField6 = rawCash ?: "",
                                rawField7 = rawTotal ?: "",
                                parsedField7 = totalAmount,
                                result = if (isInserted) "INSERTADA" else "DESCARTADA",
                                reason = if (isInserted) "N/A" else discardReason
                            )
                        )
                    }
                }

                // 4. Download Inventario (A:L) -> PosProductEntity
                val invRes = sheetsService.fetchBusinessData(sheetName, "Inventario")
                logTableMetadata("Inventario", invRes)
                if (invRes != null && invRes.rows.isNotEmpty()) {
                    val currentProds = repository.getProductsByCompany(cleanCode).firstOrNull() ?: emptyList()
                    Log.d("AUDIT_EXECUTION", "=== [C] PROCESANDO FILAS DE INVENTARIO (Filas: ${invRes.rows.size}) ===")
                    for ((index, row) in invRes.rows.withIndex()) {
                        val rawName = row.getOrNull(2)
                        val name = rawName?.trim() ?: ""
                        val category = row.getOrNull(3)?.trim() ?: "General"
                        val imageUrl = row.getOrNull(12)?.trim() ?: ""
                        val rawStock = row.getOrNull(4)
                        val stock = rawStock?.trim()?.toIntOrNull() ?: 0
                        val rawCost = row.getOrNull(5)
                        val cost = parseMoney(rawCost)
                        val rawPrice = row.getOrNull(6)
                        val price = parseMoney(rawPrice)
                        val rawMinPrice = row.getOrNull(7)
                        val minPrice = parseMoney(rawMinPrice)
                        val rawMinAlert = row.getOrNull(8)
                        val minAlert = rawMinAlert?.trim()?.toIntOrNull() ?: 5

                        val isValid = name.isNotBlank() && price > 0
                        var isInserted = false
                        var isUpdated = false
                        var isDiscarded = false
                        var actionReason = ""

                        if (isValid) {
                            val existing = currentProds.find { it.name.equals(name, ignoreCase = true) }
                            if (existing != null) {
                                isUpdated = true
                                actionReason = "Producto existente en Room (ID=${existing.id}), actualizado"
                                repository.updateProduct(
                                    existing.copy(
                                        companyCode = cleanCode,
                                        category = category,
                                        imageUrl = imageUrl,
                                        stock = stock,
                                        costPrice = cost,
                                        price = price,
                                        minPrice = minPrice,
                                        minStockAlert = minAlert,
                                        isSynced = true
                                    )
                                )
                            } else {
                                isInserted = true
                                actionReason = "Producto nuevo insertado en Room"
                                repository.insertProduct(
                                    PosProductEntity(
                                        companyCode = cleanCode,
                                        name = name,
                                        category = category,
                                        imageUrl = imageUrl,
                                        stock = stock,
                                        costPrice = cost,
                                        price = price,
                                        minPrice = minPrice,
                                        minStockAlert = minAlert,
                                        isSynced = true
                                    )
                                )
                            }
                        } else {
                            isDiscarded = true
                            actionReason = buildString {
                                if (name.isBlank()) append("row[2] Nom_Producto en blanco. ")
                                if (price <= 0) append("row[6] Precio_Venta parseMoney <= 0 (original='$rawPrice', parsed=$price). ")
                            }
                        }

                        val resultTag = when {
                            isInserted -> "INSERTADA"
                            isUpdated -> "ACTUALIZADA"
                            else -> "DESCARTADA"
                        }

                        Log.d("AUDIT_EXECUTION", "[C INVENTARIO Fila #$index] row[2] Nom_Producto='$rawName', row[4] Cantidad='$rawStock', row[5] Costo='$rawCost', row[6] Precio_Venta original='$rawPrice' -> parseMoney=$price, row[7] Precio_Minimo='$rawMinPrice' -> parseMoney=$minPrice | RESULTADO: $resultTag | MOTIVO: $actionReason")

                        // TEMPORAL - AUDIT_EXECUTION UI
                        auditInvRows.add(
                            AuditRowDetail(
                                rowIndex = index + 1,
                                field1 = name,
                                field2 = stock.toString(),
                                field3 = cost.toString(),
                                rawField4 = rawPrice ?: "",
                                parsedField4 = price,
                                rawField5 = rawMinPrice ?: "",
                                parsedField5 = minPrice,
                                result = resultTag,
                                reason = actionReason
                            )
                        )
                    }
                }

                // 5. Download Usuarios (BQ:CA) -> CompanyUserEntity
                val usersRes = sheetsService.fetchBusinessData(sheetName, "Usuarios")
                logTableMetadata("Usuarios", usersRes)
                if (usersRes != null && usersRes.rows.isNotEmpty()) {
                    val matchingCompany = repository.getCompanyByCode(cleanCode)
                    val cId = matchingCompany?.id ?: 0
                    val currentUsers = repository.getUsersByCompanyCode(cleanCode).firstOrNull() ?: emptyList()
                    for (row in usersRes.rows) {
                        val idUsuario = row.getOrNull(0)?.trim() ?: ""
                        val name = row.getOrNull(1)?.trim() ?: ""
                        val email = row.getOrNull(2)?.trim() ?: ""
                        val password = row.getOrNull(3)?.trim() ?: "1234"
                        val role = row.getOrNull(4)?.trim() ?: "Cajero"
                        val status = row.getOrNull(5)?.trim() ?: "Activo"
                        val creationDate = row.getOrNull(6)?.trim() ?: "Hoy"
                        val lastAccess = row.getOrNull(7)?.trim() ?: "Hoy"
                        val fechaCambioEstado = row.getOrNull(8)?.trim() ?: ""
                        val motivoCambio = row.getOrNull(9)?.trim() ?: ""
                        val cambiadoPor = row.getOrNull(10)?.trim() ?: ""

                        if (name.isNotBlank() || email.isNotBlank()) {
                            val exists = currentUsers.any {
                                (email.isNotBlank() && it.email.equals(email, ignoreCase = true)) ||
                                (name.isNotBlank() && it.name.equals(name, ignoreCase = true))
                            }
                            if (!exists) {
                                repository.insertUser(
                                    CompanyUserEntity(
                                        companyId = cId,
                                        companyCode = cleanCode,
                                        name = name.ifBlank { "Usuario" },
                                        role = role,
                                        email = email.ifBlank { "user@${cleanCode.lowercase()}.com" },
                                        username = email.ifBlank { name },
                                        password = password,
                                        status = status,
                                        creationDate = creationDate,
                                        lastAccess = lastAccess,
                                        idUsuario = idUsuario,
                                        fechaCambioEstado = fechaCambioEstado,
                                        motivoCambio = motivoCambio,
                                        cambiadoPor = cambiadoPor,
                                        isSynced = true
                                    )
                                )
                            }
                        }
                    }
                }

                // 6. Config_Negocio: el país define la moneda mostrada
                val configRes = sheetsService.fetchBusinessData(sheetName, "Config_Negocio")
                configRes?.rows?.firstOrNull {
                    it.getOrNull(0)?.trim().equals("PAIS", ignoreCase = true)
                }?.let { KaptaCurrency.pais = it.getOrNull(1)?.trim() ?: "" }

                // [F] ROOM REGISTROS POST-SINCRONIZACIÓN
                val dbProds = repository.getProductsByCompany(cleanCode).firstOrNull() ?: emptyList()
                val dbSales = repository.getSalesByCompany(cleanCode).firstOrNull() ?: emptyList()
                val dbTxs = repository.getTransactionsByCompany(cleanCode).firstOrNull() ?: emptyList()
                val dbUsers = repository.getUsersByCompanyCode(cleanCode).firstOrNull() ?: emptyList()
                val sumaVentas = dbSales.sumOf { it.totalAmount }
                val sumaGastos = dbTxs.filter { it.isExpense }.sumOf { it.amount }

                Log.d("AUDIT_EXECUTION", "=== [F] REGISTROS DE ROOM PARA COMPANY_CODE '$cleanCode' ===")
                Log.d("AUDIT_EXECUTION", "Cantidad de productos en Room: ${dbProds.size}")
                Log.d("AUDIT_EXECUTION", "Cantidad de ventas en Room: ${dbSales.size}")
                Log.d("AUDIT_EXECUTION", "Cantidad de transacciones en Room: ${dbTxs.size}")
                Log.d("AUDIT_EXECUTION", "Cantidad de usuarios en Room: ${dbUsers.size}")
                Log.d("AUDIT_EXECUTION", "Suma de ventas en Room: $sumaVentas")
                Log.d("AUDIT_EXECUTION", "Suma de gastos en Room: $sumaGastos")
                Log.d("AUDIT_EXECUTION", "--------------------------------------------------")

                // [G] ACTIVE COMPANY
                Log.d("AUDIT_EXECUTION", "=== [G] ACTIVE COMPANY EN VIEWMODEL ===")
                Log.d("AUDIT_EXECUTION", "activeCompanyCode StateFlow: '${activeCompanyCode.value}'")
                Log.d("AUDIT_EXECUTION", "selectedCompany: code='${_selectedCompany.value?.code}', name='${_selectedCompany.value?.name}'")
                Log.d("AUDIT_EXECUTION", "selectedRemoteCompany: codigo='${_selectedRemoteCompany.value?.codigo}', nombre='${_selectedRemoteCompany.value?.nombre}'")
                Log.d("AUDIT_EXECUTION", "currentUser.companyCode: '${_currentUser.value?.companyCode}'")
                Log.d("AUDIT_EXECUTION", "--------------------------------------------------")

                // [H] UI VALORES FINALES
                val inc = totalIncome.value
                val exp = totalExpenses.value
                val bal = inc - exp
                Log.d("AUDIT_EXECUTION", "=== [H] UI VALORES FINALES ===")
                Log.d("AUDIT_EXECUTION", "totalIncome: $inc")
                Log.d("AUDIT_EXECUTION", "totalExpenses: $exp")
                Log.d("AUDIT_EXECUTION", "balance (income - expenses): $bal")
                Log.d("AUDIT_EXECUTION", "Cantidad de productos mostrados: ${dbProds.size}")
                Log.d("AUDIT_EXECUTION", "Cantidad de ventas mostradas: ${dbSales.size}")
                Log.d("AUDIT_EXECUTION", "==================================================")

                // Limpiar reporte modal automático en sincronización exitosa
                _auditReport.value = null

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KaptaViewModel", "Error sincronizando datos desde Google Sheets: ${e.message}")
                // TEMPORAL - AUDIT_EXECUTION UI: PUBLICACIÓN DE ERROR EN REPORTE
                _auditReport.value = AuditReport(
                    companyName = sheetName,
                    companyCodeOriginal = companyCode,
                    sheetName = sheetName,
                    cleanCode = companyCode.trim().uppercase(),
                    tablesMetadata = auditTablesMeta,
                    inventoryRows = auditInvRows,
                    salesRows = auditSalesRows,
                    expensesRows = auditExpensesRows,
                    errorMessage = e.message ?: e.toString(),
                    // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
                    sheetsDiagnostics = sheetsService.lastFetchDiagnostics.toList()
                )
            }
        }
    }

    /**
     * Short Polling (10s) para refetch de tablas críticas y sync de pendientes
     */
    fun startPollingForTenant(companyCode: String) {
        syncManager.startShortPolling(companyCode, repository) { sheetName ->
            syncTenantBusinessDataFromSheets(sheetName, companyCode)
        }
    }

    /**
     * Refetch silencioso al recuperar foco de ventana
     */
    fun refetchOnWindowFocus(companyCode: String) {
        syncManager.refetchOnWindowFocus(companyCode, repository) { sheetName ->
            syncTenantBusinessDataFromSheets(sheetName, companyCode)
        }
    }

    /**
     * Registra una venta utilizando Arquitectura de Actualización Optimista (Optimistic UI)
     */
    fun recordSaleOptimistic(companyCode: String, sale: PosSaleEntity) {
        viewModelScope.launch {
            val unsyncedSale = sale.copy(isSynced = false)
            repository.insertSale(unsyncedSale)
            syncManager.triggerImmediateSync(repository)
        }
    }

    /**
     * Registra un gasto utilizando Arquitectura de Actualización Optimista (Optimistic UI)
     */
    fun recordExpenseOptimistic(companyCode: String, concept: String, amount: Double, category: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val txEntity = FinancialTransactionEntity(
                companyCode = companyCode,
                title = companyCode,
                subtitle = concept,
                amount = amount,
                dateString = dateStr,
                isExpense = true,
                category = category,
                isSynced = false
            )
            repository.insertTransaction(txEntity)
            syncManager.triggerImmediateSync(repository)
        }
    }

    val companies: StateFlow<List<CompanyEntity>> = repository.allCompanies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<CompanyUserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Todos") // "Todos", "Recientes", "Nuevo", "Por vencer", "Vencido", "Prueba", "Activo", "Suspendidos", "Por suspender"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _financeSubTab = MutableStateFlow("Resumen") // "Resumen", "Ingresos", "Egresos", "Cartera", "Suscripciones", "Retiro", "Reportes"
    val financeSubTab: StateFlow<String> = _financeSubTab.asStateFlow()

    private val _selectedCompany = MutableStateFlow<CompanyEntity?>(null)
    val selectedCompany: StateFlow<CompanyEntity?> = _selectedCompany.asStateFlow()

    private val _currentUser = MutableStateFlow<CompanyUserEntity?>(null)
    val currentUser: StateFlow<CompanyUserEntity?> = _currentUser.asStateFlow()

    private val _remoteCompanies = MutableStateFlow<List<RemoteCompany>>(emptyList())
    val remoteCompanies: StateFlow<List<RemoteCompany>> = _remoteCompanies.asStateFlow()

    // Selected Remote Company State
    private val _selectedRemoteCompany = MutableStateFlow<RemoteCompany?>(null)
    val selectedRemoteCompany: StateFlow<RemoteCompany?> = _selectedRemoteCompany.asStateFlow()

    fun selectRemoteCompany(remote: RemoteCompany?) {
        _selectedRemoteCompany.value = remote
        if (remote != null) {
            viewModelScope.launch {
                val code = remote.codigo.ifBlank { remote.idEmpresa }
                val company = repository.getCompanyByCode(code.lowercase())
                if (company != null) {
                    _selectedCompany.value = company
                }
                val sheetName = remote.nombre.ifBlank { code }
                syncTenantBusinessDataFromSheets(sheetName, code)
            }
        }
    }

    private val _isSuperAdminSession = MutableStateFlow(false)
    val isSuperAdminSession: StateFlow<Boolean> = _isSuperAdminSession.asStateFlow()

    fun setSuperAdminSession(isSuperAdmin: Boolean) {
        _isSuperAdminSession.value = isSuperAdmin
    }

    // Active Company Code Flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeCompanyCode: StateFlow<String> = combine(
        _selectedRemoteCompany,
        _selectedCompany,
        _currentUser,
        _remoteCompanies,
        _isSuperAdminSession
    ) { remote, company, user, remotes, isSuperAdmin ->
        val userIsSuperAdmin = isSuperAdmin ||
                user?.role.equals("superadmin", ignoreCase = true) ||
                user?.role.equals("SuperAdmin", ignoreCase = true)

        val code = if (userIsSuperAdmin) {
            // SI role == SUPER_ADMIN Y no existe una empresa seleccionada explícitamente para modo soporte:
            // activeCompanyCode = ""
            remote?.codigo?.ifBlank { null }
                ?: company?.code?.ifBlank { null }
                ?: ""
        } else {
            remote?.codigo?.ifBlank { null }
                ?: company?.code?.ifBlank { null }
                ?: user?.companyCode?.ifBlank { null }
                ?: ""
        }

        val logRole = if (userIsSuperAdmin) "SUPER_ADMIN" else "BUSINESS"
        android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] activeCompanyCode updated: role=$logRole, activeCompanyCode='$code', selectedCompany='${company?.code ?: remote?.codigo ?: ""}'")
        code
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Reactive Room Flows based on activeCompanyCode
    @OptIn(ExperimentalCoroutinesApi::class)
    val rawSalesFromRoom: Flow<List<PosSaleEntity>> = activeCompanyCode.flatMapLatest { code ->
        if (code.isBlank()) flowOf(emptyList())
        else repository.getSalesByCompany(code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawTransactionsFromRoom: Flow<List<FinancialTransactionEntity>> = activeCompanyCode.flatMapLatest { code ->
        if (code.isBlank()) flowOf(emptyList())
        else repository.getTransactionsByCompany(code)
    }

    // Mapped Financial Transactions for UI
    val financialTransactions: StateFlow<List<FinancialTransaction>> = combine(
        rawSalesFromRoom,
        rawTransactionsFromRoom
    ) { sales, txs ->
        val salesAsTx = sales.map { sale ->
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(sale.timestamp))
            FinancialTransaction(
                id = sale.id,
                title = if (sale.productName.isNotBlank()) sale.productName else "Venta POS",
                subtitle = if (sale.clientName.isNotBlank()) sale.clientName else sale.paymentMethod,
                amount = sale.totalAmount,
                dateString = dateStr,
                isExpense = false,
                category = "Ventas"
            )
        }
        val expensesAsTx = txs.map { tx ->
            FinancialTransaction(
                id = tx.id,
                title = if (tx.title.isNotBlank()) tx.title else "Gasto",
                subtitle = if (tx.subtitle.isNotBlank()) tx.subtitle else tx.category,
                amount = tx.amount,
                dateString = tx.dateString,
                isExpense = tx.isExpense,
                category = if (tx.category.isNotBlank()) tx.category else "Gastos"
            )
        }
        (salesAsTx + expensesAsTx).sortedByDescending { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date Filters
    private val _financeStartDate = MutableStateFlow("")
    val financeStartDate: StateFlow<String> = _financeStartDate.asStateFlow()

    private val _financeEndDate = MutableStateFlow("")
    val financeEndDate: StateFlow<String> = _financeEndDate.asStateFlow()

    fun setFinanceStartDate(date: String) {
        _financeStartDate.value = date
    }

    fun setFinanceEndDate(date: String) {
        _financeEndDate.value = date
    }

    fun clearFinanceDateFilter() {
        _financeStartDate.value = ""
        _financeEndDate.value = ""
    }

    private fun parseDateStr(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val cleanStr = dateStr.trim().split(" ")[0].split("T")[0]
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        )
        for (fmt in formats) {
            try {
                fmt.isLenient = false
                val parsed = fmt.parse(cleanStr)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    val filteredTransactions: StateFlow<List<FinancialTransaction>> = combine(
        financialTransactions,
        _financeStartDate,
        _financeEndDate
    ) { list, startStr, endStr ->
        val startDate = parseDateStr(startStr)
        val endDate = parseDateStr(endStr)?.let {
            val cal = java.util.Calendar.getInstance()
            cal.time = it
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            cal.time
        }

        list.filter { item ->
            val itemDate = item.parseDate()
            if (itemDate != null) {
                val afterStart = startDate == null || !itemDate.before(startDate)
                val beforeEnd = endDate == null || !itemDate.after(endDate)
                afterStart && beforeEnd
            } else {
                true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { !it.isExpense }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpenses: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.isExpense }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unfilteredTotalIncome: StateFlow<Double> = rawSalesFromRoom.map { sales ->
        sales.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unfilteredTotalExpenses: StateFlow<Double> = rawTransactionsFromRoom.map { txList ->
        txList.filter { it.isExpense }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val filteredCompanies: StateFlow<List<CompanyEntity>> = combine(companies, _searchQuery, _selectedFilter) { list, query, filter ->
        list.filter { company ->
            if (company.status.equals("Eliminado", ignoreCase = true)) return@filter false
            val matchesQuery = query.isEmpty() ||
                    company.name.contains(query, ignoreCase = true) ||
                    company.code.contains(query, ignoreCase = true) ||
                    company.nit.contains(query, ignoreCase = true) ||
                    company.adminName.contains(query, ignoreCase = true) ||
                    company.adminEmail.contains(query, ignoreCase = true)

            val effStatus = company.getEffectiveStatus()
            val matchesFilter = when (filter) {
                "Recientes" -> company.id in list.take(5).map { it.id }
                "Nuevo" -> company.creationDate.contains("2024") || company.creationDate.contains("2026")
                "Por vencer" -> effStatus.equals("Por vencer", ignoreCase = true)
                "Vencido" -> effStatus.equals("Vencido", ignoreCase = true)
                "Prueba", "De prueba" -> company.status.contains("Prueba", ignoreCase = true) || company.plan.contains("Prueba", ignoreCase = true)
                "Activo", "Activas" -> effStatus.equals("Activo", ignoreCase = true)
                "Suspendidos", "Suspendido", "Por suspender" -> effStatus.equals("Suspendido", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected company for detail / POS
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun setCurrentUser(user: CompanyUserEntity?) {
        _currentUser.value = user
        val isSuperAdmin = _isSuperAdminSession.value || user?.role.equals("superadmin", ignoreCase = true) || user?.role.equals("SuperAdmin", ignoreCase = true)
        val roleLabel = if (isSuperAdmin) "SUPER_ADMIN" else "BUSINESS"
        android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] Login/UserChanged: role=$roleLabel, activeCompanyCode='${activeCompanyCode.value}', selectedCompany='${_selectedCompany.value?.code ?: ""}'")

        if (user != null && user.companyCode.isNotBlank()) {
            viewModelScope.launch {
                val company = repository.getCompanyByCode(user.companyCode)
                    ?: repository.getCompanyById(user.companyId)
                if (company != null) {
                    syncObtenerTodo(company)
                }
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _selectedCompany.value = null
        _selectedRemoteCompany.value = null
        _isSuperAdminSession.value = false
        android.util.Log.d("KAPTA_ISOLATION", "[KAPTA_ISOLATION] Logout executed: role=None, activeCompanyCode='', selectedCompany=null")
    }

    suspend fun loginWithServer(pais: String, codigo: String, correo: String, password: String): com.example.data.remote.LoginResultado {
        return sheetsService.login(pais, codigo, correo, password)
    }

    private val _reportResult = MutableStateFlow<String?>(null)
    val reportResult: StateFlow<String?> = _reportResult.asStateFlow()

    fun solicitarReporte(idEmpresa: String, tipoReporte: String = "general", extraParams: Map<String, String> = emptyMap()) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.solicitarReporte(sheetsService, idEmpresa, tipoReporte, extraParams)
            _reportResult.value = result
            Log.d("KaptaViewModel", "solicitarReporte result: $result")
        }
    }

    fun fetchRemoteCompanies() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = sheetsService.listarEmpresas()
                if (list.isNotEmpty()) {
                    _remoteCompanies.value = list
                }
                repository.syncCompaniesFromSheets(sheetsService)
            } catch (e: Exception) {
                Log.e("KaptaViewModel", "Error al listar empresas desde servidor: ${e.message}")
            }
        }
    }

    suspend fun authenticateCompanyUser(companyCode: String, input: String, pass: String): CompanyUserEntity? {
        val uInput = input.trim()
        val pInput = pass.trim()
        if (uInput.isBlank() || pInput.isBlank()) return null

        // Paso 1: Identificación (Hoja Data Maestra - Empresas)
        val company = repository.getCompanyByCode(companyCode.lowercase())
            ?: companies.value.find { it.code.equals(companyCode, ignoreCase = true) }

        // Si el negocio no existe o su estado está inactivo o suspendido, rechazar la conexión inmediatamente
        if (company == null) return null
        val effStatus = company.status
        if (effStatus.equals("Inactivo", ignoreCase = true) || effStatus.equals("Suspendido", ignoreCase = true)) {
            return null
        }

        // Excepción Super Admin: Credenciales internas de Kapta IA
        if ((uInput.equals("AdminMauricio@kaptaia.com", ignoreCase = true) || uInput.equals("superadmin", ignoreCase = true)) &&
            (pInput == "M4ur1C10*" || pInput == "admin123")) {
            setSuperAdminSession(true)
            return CompanyUserEntity(
                companyId = company.id,
                companyCode = companyCode,
                name = "SuperAdmin (Kapta IA)",
                role = "Administrador",
                email = "AdminMauricio@kaptaia.com",
                username = "superadmin",
                password = ""
            )
        }

        // Paso 2: Validación (Hoja de Negocio - Tabla Usuarios específica)
        val dbUsers = repository.getUsersByCompanyCodeAndId(companyCode, company.id).firstOrNull() ?: emptyList()
        var match = dbUsers.find { 
            (it.username.equals(uInput, ignoreCase = true) || it.email.equals(uInput, ignoreCase = true)) &&
            verificarContrasena(pInput, it.password)
        }

        // Si no hay usuarios registrados aún para la empresa, validar contra las credenciales por defecto del negocio
        if (match == null && dbUsers.isEmpty()) {
            if ((uInput.equals(company.adminEmail, ignoreCase = true) || uInput.equals("admin", ignoreCase = true) || uInput.equals(company.code, ignoreCase = true)) &&
                (pInput == "1234" || pInput == "admin")) {
                match = CompanyUserEntity(
                    companyCode = companyCode,
                    companyId = company.id,
                    username = uInput,
                    name = company.adminName.ifBlank { "Administrador General" },
                    email = company.adminEmail.ifBlank { "admin@${companyCode}.com" },
                    password = pInput,
                    role = "Administrador"
                )
            } else if (uInput.equals("cajero", ignoreCase = true) && (pInput == "1234" || pInput == "0000")) {
                match = CompanyUserEntity(
                    companyCode = companyCode,
                    companyId = company.id,
                    username = "cajero",
                    name = "Cajero Turno 1",
                    email = "cajero@${companyCode}.com",
                    password = pInput,
                    role = "Cajero"
                )
            }
        }

        return match
    }

    // ponytail: espejo del hash PBKDF2 del backend; cachés locales con texto plano legado siguen validando
    private fun verificarContrasena(input: String, stored: String): Boolean {
        if (!stored.startsWith("pbkdf2$")) return stored == input
        val parts = stored.split("$")
        if (parts.size != 4) return false
        return try {
            val iter = parts[1].toInt()
            val salt = parts[2].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val expected = parts[3].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val spec = javax.crypto.spec.PBEKeySpec(input.toCharArray(), salt, iter, 256)
            val computed = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            java.security.MessageDigest.isEqual(computed, expected)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkCompanyExists(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.equals("aptAdmin", ignoreCase = true)) return true
        val list = companies.value
        val found = list.find { it.code.equals(trimmed, ignoreCase = true) }
        if (found != null) return true
        val dbComp = repository.getCompanyByCode(trimmed.lowercase())
        return dbComp != null
    }

    // Nombre real desde Room (el StateFlow puede aún no estar cargado al abrir el login)
    suspend fun empresaPorCodigo(code: String): CompanyEntity? =
        repository.getCompanyByCode(code.trim().lowercase())

    fun selectCompany(company: CompanyEntity?) {
        _selectedCompany.value = company
        if (company != null) {
            syncObtenerTodo(company)
        }
    }

    fun syncObtenerTodo(company: CompanyEntity) {
        viewModelScope.launch {
            val sheetName = company.name.ifBlank { company.code }
            val json = sheetsService.obtenerTodo(sheetName)
            if (json != null) {
                Log.d("KaptaViewModel", "obtener_todo data received for $sheetName")
            }
        }
    }

    /**
     * Comprime una imagen local (máx 512px, JPEG 80) y la sube al backend.
     * Devuelve URL pública; si falla, devuelve el path local como fallback.
     */
    suspend fun subirImagen(uriString: String): String {
        if (uriString.isBlank() || uriString.startsWith("http")) return uriString
        return try {
            withContext(Dispatchers.IO) {
                val resolver = getApplication<Application>().contentResolver
                val uri = android.net.Uri.parse(uriString)
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
                val bmp = resolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: return@withContext uriString
                val bos = java.io.ByteArrayOutputStream()
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos)
                val b64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
                sheetsService.subirFoto(b64) ?: uriString
            }
        } catch (e: Exception) {
            Log.e("KaptaViewModel", "Error subiendo imagen: ${e.message}")
            uriString
        }
    }

    /** Estadísticas reales del negocio desde el servidor (obtener_todo). */
    fun cargarEstadisticasEmpresa(
        codigo: String,
        onResult: (ventasHoy: Double?, ventasMes: Double?, productos: Int?, clientes: Int?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val json = sheetsService.obtenerTodo(codigo)
                if (json == null) {
                    onResult(null, null, null, null)
                    return@launch
                }
                val data = json.optJSONObject("data") ?: json

                fun filas(tabla: String): List<List<String>> =
                    data?.optJSONArray(tabla)?.let { arr ->
                        (0 until arr.length()).map { i ->
                            val row = arr.optJSONArray(i)
                            (0 until row.length()).map { row.optString(it) }
                        }
                    } ?: emptyList()

                val fmtHoyIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val fmtHoyEs = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val prefijoMes = fmtHoyIso.substring(0, 7)

                var ventasHoy = 0.0
                var ventasMes = 0.0
                for (v in filas("ventas").drop(1)) {
                    if (v.size < 13) continue
                    val dia = v.getOrNull(1)?.trim()?.split(" ")?.first() ?: continue
                    val total = v.getOrNull(12)?.toDoubleOrNull() ?: 0.0
                    if (dia == fmtHoyIso || dia == fmtHoyEs) ventasHoy += total
                    if (dia.startsWith(prefijoMes)) ventasMes += total
                }

                val productos = filas("inventario").drop(1)
                    .count { it.getOrNull(2)?.isNotBlank() == true }
                val clientes = filas("deudores").drop(1)
                    .mapNotNull { it.getOrNull(1)?.takeIf(String::isNotBlank) }
                    .distinct().size

                onResult(ventasHoy, ventasMes, productos, clientes)
            } catch (_: Exception) {
                onResult(null, null, null, null)
            }
        }
    }

    fun selectCompanyById(companyId: Int) {
        viewModelScope.launch {
            _selectedCompany.value = repository.getCompanyById(companyId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setFinanceSubTab(subTab: String) {
        _financeSubTab.value = subTab
    }

    // Actions on company
    fun renewMembership(company: CompanyEntity) {
        viewModelScope.launch {
            val updated = company.copy(
                status = "Activo",
                expirationDays = 30,
                lastAccess = "Hoy, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            )
            repository.updateCompany(updated)
            if (_selectedCompany.value?.id == company.id) {
                _selectedCompany.value = updated
            }

            val amount = when {
                company.plan.contains("MAX", ignoreCase = true) -> 399900.0
                company.plan.contains("Premium", ignoreCase = true) -> 249900.0
                else -> 149900.0
            }
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val txEntity = FinancialTransactionEntity(
                companyCode = company.code,
                title = company.name,
                subtitle = "Renovación Plan ${company.plan}",
                amount = amount,
                dateString = todayStr,
                isExpense = false,
                category = "Membresía",
                isSynced = false
            )
            repository.insertTransaction(txEntity)

            showToast("Membresía renovada con éxito para ${company.name}")
        }
    }

    fun suspendCompany(company: CompanyEntity) {
        viewModelScope.launch {
            val newStatus = if (company.status == "Suspendido") "Activo" else "Suspendido"
            val updated = company.copy(status = newStatus)
            repository.updateCompany(updated)
            if (_selectedCompany.value?.id == company.id) {
                _selectedCompany.value = updated
            }
            showToast("Empresa ${company.name} cambiada a estado: $newStatus")
        }
    }

    fun deleteCompany(company: CompanyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Borrado suave: la empresa sale de la vista superadmin de inmediato;
            // el servidor purga usuarios y toda su información 2 días después.
            val success = try {
                sheetsService.eliminarEmpresa(company.name)
            } catch (e: Exception) {
                Log.e("KaptaViewModel", "Error al eliminar empresa en el servidor: ${e.message}")
                false
            }

            if (success) {
                val updated = company.copy(status = "Eliminado")
                repository.updateCompany(updated)
                if (_selectedCompany.value?.id == company.id) {
                    _selectedCompany.value = null
                }
                syncCompaniesFromSheets()
                withContext(Dispatchers.Main) {
                    showToast("Empresa '${company.name}' eliminada. Sus datos se borrarán definitivamente en 2 días.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("Error al eliminar '${company.name}'. Verifique su conexión o permisos.")
                }
            }
        }
    }

    fun changeCompanyPlan(company: CompanyEntity, newPlan: String) {
        viewModelScope.launch {
            val updated = company.copy(plan = newPlan)
            repository.updateCompany(updated)
            if (_selectedCompany.value?.id == company.id) {
                _selectedCompany.value = updated
            }
            showToast("Plan actualizado a $newPlan para ${company.name}")
        }
    }

    fun resetUserPassword(user: CompanyUserEntity, newPass: String) {
        viewModelScope.launch {
            val updated = user.copy(password = newPass)
            repository.updateUser(updated)
            showToast("Contraseña restablecida para el usuario ${user.username}")
        }
    }

    fun copyCredentials(company: CompanyEntity) {
        val text = "Empresa: ${company.name}\nCódigo: ${company.code}\nAdmin: ${company.adminEmail}\nCorreo: ${company.email}"
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Credenciales ${company.name}", text)
        clipboard.setPrimaryClip(clip)
        showToast("Credenciales de ${company.name} copiadas al portapapeles")
    }

    // Company Creation & AI Custom Functions
    private val _createdAiFunctions = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Name, Description
    val createdAiFunctions: StateFlow<List<Pair<String, String>>> = _createdAiFunctions.asStateFlow()

    fun addCustomAiFunction(name: String, description: String) {
        val current = _createdAiFunctions.value.toMutableList()
        current.add(Pair(name, description))
        _createdAiFunctions.value = current
        showToast("Función con IA '$name' creada exitosamente")
    }

    fun removeCustomAiFunction(index: Int) {
        val current = _createdAiFunctions.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _createdAiFunctions.value = current
        }
    }

    fun toggleCustomAiFunction(nombre: String, descripcion: String) {
        val current = _createdAiFunctions.value.toMutableList()
        val i = current.indexOfFirst { it.first == nombre }
        if (i >= 0) current.removeAt(i) else current.add(Pair(nombre, descripcion))
        _createdAiFunctions.value = current
    }

    private val aiHttpClient = OkHttpClient()

    /**
     * Llama a OpenRouter (modelo gratuito) para generar funciones de negocio a partir de un prompt.
     * Devuelve lista de FuncionLib (con planTier y modulo). Sin key o fallo: vacío.
     */
    suspend fun generarFuncionesConIA(prompt: String): List<FuncionLib> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        if (apiKey.isBlank() || apiKey.startsWith("REEMPLAZA")) return@withContext emptyList()
        val model = "mistralai/mistral-7b-instruct:free"
        val system = """Eres un arquitecto de funciones para un POS multi-tenant. Diseñas funciones REUTILIZABLES (plantillas) para cualquier negocio.
Responde SOLO con un arreglo JSON (sin texto ni markdown) de este formato:
[{"nombre":"...","descripcion":"...","rol":"...","plan":"Basico|Premium|MaxIA","modulo":"ventas|gastos|deudores|inventario|facturacion|reporte|caja|usuario|cliente|custom"}]
"plan" = nivel minimo del negocio que la obtiene: Basico=vitales, Premium=basicas+utiles, MaxIA=todas+avanzadas a gran escala.
"modulo" = plantilla de UI que la renderiza (igual para todos los negocios). Crea entre 3 y 8 funciones coherentes."""
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", system) })
                put(JSONObject().apply { put("role", "user"); put("content", prompt) })
            })
        }
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        try {
            aiHttpClient.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("KaptaViewModel", "OpenRouter error ${response.code}: $str")
                    return@withContext emptyList()
                }
                val json = JSONObject(str)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content") ?: ""
                parseFuncionesIA(content)
            }
        } catch (e: Exception) {
            Log.e("KaptaViewModel", "Error IA: ${e.message}", e)
            emptyList()
        }
    }

    /** Genera el set completo de funciones para un tipo de negocio, clasificadas por plan. */
    suspend fun generarFuncionesPorTipoNegocio(tipoNegocio: String): List<FuncionLib> {
        // ponytail: si el tipo ya tiene funciones en la biblioteca, reusarlas y no gastar IA
        val existentes = _functionLibrary.value.filter { it.tipoNegocio.equals(tipoNegocio, ignoreCase = true) }
        if (existentes.isNotEmpty()) return existentes
        return generarFuncionesConIA(
            "Tipo de negocio: $tipoNegocio. Crea TODAS sus funciones clasificadas por plan: " +
            "Basico = vitales; Premium = basicas + utiles; MaxIA = todas + avanzadas a gran escala. " +
            "Cada funcion mapea a un modulo existente (ventas, gastos, deudores, inventario, facturacion, reporte, caja, usuario, cliente)."
        )
    }

    private fun parseFuncionesIA(content: String): List<FuncionLib> {
        val text = content.trim()
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return try {
            val arr = JSONArray(text.substring(start, end + 1))
            val out = mutableListOf<FuncionLib>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val nombre = o.optString("nombre").ifBlank { continue }
                val descripcion = o.optString("descripcion", "")
                val rol = o.optString("rol", "")
                val plan = o.optString("plan", "Basico").ifBlank { "Basico" }
                val modulo = o.optString("modulo", "custom").ifBlank { "custom" }
                out.add(FuncionLib(nombre = nombre, descripcion = descripcion, rol = rol, planTier = plan, modulo = modulo))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun rolDeDescripcion(descripcion: String): String {
        val idx = descripcion.indexOf("[Rol:", ignoreCase = true)
        if (idx < 0) return ""
        val end = descripcion.indexOf("]", startIndex = idx)
        return if (end > idx) descripcion.substring(idx + 6, end).trim() else ""
    }

    fun parseCustomFunctions(json: String?): List<Pair<String, String>> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
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

    // Biblioteca global de funciones IA (propiedad de SuperAdmin)
    private val _functionLibrary = MutableStateFlow<List<FuncionLib>>(emptyList())
    val functionLibrary: StateFlow<List<FuncionLib>> = _functionLibrary.asStateFlow()

    fun loadFunctionLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val remoto = runCatching { sheetsService.listarFunciones() }.getOrDefault(emptyList())
            val local = runCatching { repository.getAllIaFunctionsSync() }.getOrDefault(emptyList())
                .map { FuncionLib(nombre = it.nombre, descripcion = it.descripcion, rol = it.rol, planTier = it.planTier, tipoNegocio = it.tipoNegocio, modulo = it.modulo) }
            val merged = (remoto + local).distinctBy { it.nombre }
            _functionLibrary.value = merged
            merged.forEach {
                runCatching { repository.insertIaFunction(IaFunctionEntity(nombre = it.nombre, descripcion = it.descripcion, rol = it.rol, planTier = it.planTier, tipoNegocio = it.tipoNegocio, modulo = it.modulo)) }
            }
        }
    }

    suspend fun addFunctionToLibrary(f: FuncionLib) {
        runCatching { sheetsService.crearFuncion(f.nombre, f.descripcion, f.rol, f.planTier, f.tipoNegocio, f.modulo) }
        runCatching { repository.insertIaFunction(IaFunctionEntity(nombre = f.nombre, descripcion = f.descripcion, rol = f.rol, planTier = f.planTier, tipoNegocio = f.tipoNegocio, modulo = f.modulo)) }
        _functionLibrary.value = (_functionLibrary.value + f).distinctBy { it.nombre }
    }

    suspend fun removeFunctionFromLibrary(nombre: String) {
        runCatching { sheetsService.eliminarFuncion(nombre) }
        runCatching { repository.deleteIaFunctionByNombre(nombre) }
        _functionLibrary.value = _functionLibrary.value.filter { it.nombre != nombre }
    }

    fun setCustomAiFunctionsFromCompany(json: String?) {
        _createdAiFunctions.value = parseCustomFunctions(json)
    }

    fun saveOrUpdateCompany(
        companyId: Int? = null,
        name: String,
        code: String,
        logoUrl: String = "",
        listIconUrl: String = "",
        status: String,
        plan: String,
        durationTime: String = "Mensual",
        nit: String = "",
        country: String = "Colombia",
        city: String,
        address: String,
        phone: String,
        phone2: String = "",
        email: String,
        businessType: String,
        adminName: String,
        adminEmail: String,
        adminPass: String,
        selectedModules: List<String>,
        primaryColor: String = "#4F46E5",
        secondaryColor: String = "#3B82F6",
        tertiaryColor: String = "#10B981",
        neutralColor: String = "#0F172A",
        fontType: String = "Inter",
        onSuccess: () -> Unit
    ) {
        if (_isSavingCompany.value) return
        _isSavingCompany.value = true

        // Validación NIT único (solo al crear; el backend también lo valida)
        val nitLimpio = nit.trim()
        if (companyId == null && nitLimpio.isNotBlank() &&
            companies.value.any { it.nit.trim() == nitLimpio }
        ) {
            showToast("El NIT $nitLimpio ya está registrado para otra empresa")
            _isSavingCompany.value = false
            return
        }

        viewModelScope.launch {
            try {
                val modulesJson = JSONArray(selectedModules).toString()
                val createdFunctionsJson = JSONArray().apply {
                    _createdAiFunctions.value.forEach {
                        put(JSONObject().apply { put("nombre", it.first); put("descripcion", it.second) })
                    }
                }.toString()
                val cleanCode = if (code.isBlank()) name.lowercase().replace(" ", "").replace("[^a-zA-Z0-9]".toRegex(), "") else code.lowercase().replace(" ", "")

                val isTrial = durationTime.contains("Prueba", ignoreCase = true) || status.contains("Prueba", ignoreCase = true)
                val cleanPlanInput = plan.replace(Regex("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ]"), "").trim().ifBlank { "Básico" }
                val cleanStatusInput = status.replace(Regex("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ]"), "").trim().ifBlank { "Activa" }
                val finalPlan = if (isTrial) "MAX IA" else cleanPlanInput
                val finalStatus = if (isTrial) "Prueba" else cleanStatusInput
                val calcExpirationDays = when {
                    isTrial -> 15
                    durationTime.contains("Anual", ignoreCase = true) -> 365
                    durationTime.contains("Permanente", ignoreCase = true) -> 3650
                    else -> 30
                }

                val company = CompanyEntity(
                    id = companyId ?: 0,
                    code = cleanCode,
                    name = name,
                    logoUrl = logoUrl,
                    listIconUrl = listIconUrl,
                    status = finalStatus,
                    plan = finalPlan,
                    durationTime = durationTime,
                    nit = nit,
                    country = country,
                    city = city,
                    address = address,
                    phone = phone,
                    phone2 = phone2,
                    email = email,
                    businessType = businessType,
                    adminName = adminName,
                    adminEmail = adminEmail,
                    expirationDays = calcExpirationDays,
                    selectedModulesJson = modulesJson,
                    customFunctionsJson = createdFunctionsJson,
                    primaryColorHex = primaryColor,
                    secondaryColorHex = secondaryColor,
                    tertiaryColorHex = tertiaryColor,
                    neutralColorHex = neutralColor,
                    fontType = fontType
                )

                if (companyId != null && companyId > 0) {
                    val updatePayload = mapOf(
                        "codigo" to company.code,
                        "nombre" to name,
                        "estado" to finalStatus,
                        "plan" to finalPlan,
                        "logoUrl" to logoUrl,
                        "listIconUrl" to listIconUrl,
                        "colorPrimario" to primaryColor,
                        "colorSecundario" to secondaryColor,
                        "colorTerciario" to tertiaryColor,
                        "colorNeutro" to neutralColor,
                        "tipoFuente" to fontType,
                        "funciones" to createdFunctionsJson
                    )
                    try {
                        sheetsService.actualizarEmpresa(updatePayload)
                    } catch (e: Exception) {
                        Log.e("KaptaViewModel", "No se sincronizó la actualización en la nube: ${e.message}")
                    }
                    repository.updateCompany(company)
                    showToast("Empresa '$name' actualizada con éxito")
                    _createdAiFunctions.value = emptyList()
                    onSuccess()
                } else {
                    val passToUse = if (adminPass.isBlank()) "admin123" else adminPass
                    val payload = mapOf(
                        "nombre" to name,
                        "nombreComercial" to name,
                        "nit" to nit,
                        "codigo" to cleanCode,
                        "tipo" to businessType,
                        "pais" to country,
                        "ciudad" to city,
                        "direccion" to address,
                        "correo" to email,
                        "celular1" to phone,
                        "celular2" to phone2,
                        "plan" to finalPlan,
                        "tiempo" to durationTime,
                        "estado" to finalStatus,
                        "adminNombre" to adminName,
                        "adminCorreo" to adminEmail,
                        "adminPassword" to passToUse,
                        "registradoPor" to "SuperAdmin",
                        "observaciones" to buildString {
                            append("Empresa ").append(name)
                            if (businessType.isNotBlank()) append(" ").append(businessType)
                            if (city.isNotBlank() || country.isNotBlank()) {
                                append(" en ")
                                append(listOf(city, country).filter { it.isNotBlank() }.joinToString(", "))
                            }
                        },
                        "logoUrl" to logoUrl,
                        "listIconUrl" to listIconUrl,
                        "colorPrimario" to primaryColor,
                        "colorSecundario" to secondaryColor,
                        "colorTerciario" to tertiaryColor,
                        "colorNeutro" to neutralColor,
                        "tipoFuente" to fontType,
                        "funciones" to createdFunctionsJson
                    )

                    val responseJson = sheetsService.registrarEmpresa(payload)
                    
                    if (responseJson != null) {
                        val finalCode = responseJson.optString("codigo").ifBlank {
                            responseJson.optString("idEmpresa").ifBlank { cleanCode }
                        }
                        val companyToInsert = company.copy(code = finalCode)
                        val newId = repository.insertCompany(companyToInsert).toInt()
                        
                        repository.insertUser(
                            CompanyUserEntity(
                                companyId = newId,
                                companyCode = finalCode,
                                name = adminName,
                                role = "Administrador",
                                email = adminEmail,
                                username = "admin",
                                password = passToUse,
                                isSynced = true
                            )
                        )
                        showToast("Empresa creada correctamente")
                        _createdAiFunctions.value = emptyList()
                        onSuccess()
                    } else {
                        showToast("No se pudo crear la empresa. Revisa tu conexión e inténtalo nuevamente.")
                    }
                }
            } catch (e: Exception) {
                showToast("No se pudo crear la empresa. Revisa tu conexión e inténtalo nuevamente.")
            } finally {
                _isSavingCompany.value = false
            }
        }
    }

    // Tenant POS Operations
    fun getUsersByCompanyCode(code: String, companyId: Int = 0): Flow<List<CompanyUserEntity>> {
        val cleanCode = code.trim().uppercase()
        return repository.getUsersByCompanyCodeAndId(cleanCode, companyId)
    }

    fun ensureDefaultUsersForCompany(code: String, companyId: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = code.trim().uppercase()
            val list = repository.getUsersByCompanyCodeAndId(cleanCode, companyId).first()
            if (list.isEmpty()) {
                val company = repository.getCompanyByCode(cleanCode)
                val cId = company?.id ?: companyId
                val adminName = company?.adminName?.ifBlank { "Administrador" } ?: "Administrador"
                val adminEmail = company?.adminEmail?.ifBlank { "admin@$cleanCode.com" } ?: "admin@$cleanCode.com"

                repository.insertUser(
                    CompanyUserEntity(
                        companyId = cId,
                        companyCode = cleanCode,
                        name = adminName,
                        role = "Administrador",
                        email = adminEmail,
                        username = "admin",
                        password = "adminPassword123"
                    )
                )
            }
        }
    }

    fun getProductsForCompany(code: String): Flow<List<PosProductEntity>> {
        val cleanCode = code.trim().uppercase()
        return repository.getProductsByCompany(cleanCode)
    }

    fun ensureDefaultProductsForCompany(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = code.trim().uppercase()
            if (!cleanCode.equals("aptAdmin", ignoreCase = true) && !cleanCode.equals("demo", ignoreCase = true)) return@launch
            val list = repository.getProductsByCompany(cleanCode).first()
            if (list.isEmpty()) {
                val sampleProducts = listOf(
                    PosProductEntity(companyCode = cleanCode, name = "Cerveza Poker 330ml", price = 6000.0, costPrice = 3000.0, category = "Cerveza", stock = 184, aliases = "poker, cerveza, lata"),
                    PosProductEntity(companyCode = cleanCode, name = "Aguardiente Tapa Roja 750ml", price = 75000.0, costPrice = 45000.0, category = "Licores", stock = 92, aliases = "guaro, aguardiente, tapa roja"),
                    PosProductEntity(companyCode = cleanCode, name = "Ron Caldas 750ml", price = 85000.0, costPrice = 50000.0, category = "Licores", stock = 45, aliases = "ron, caldas, botella"),
                    PosProductEntity(companyCode = cleanCode, name = "Mojito Clásico Especial", price = 18000.0, costPrice = 7000.0, category = "Cocteles", stock = 38, aliases = "mojito, coctel"),
                    PosProductEntity(companyCode = cleanCode, name = "Agua Mineral 600ml", price = 3000.0, costPrice = 1200.0, category = "Bebidas", stock = 110, aliases = "agua, mineral"),
                    PosProductEntity(companyCode = cleanCode, name = "Cerveza Club Colombia 330ml", price = 7000.0, costPrice = 3500.0, category = "Cerveza", stock = 120, aliases = "club colombia, dorada"),
                    PosProductEntity(companyCode = cleanCode, name = "Coctel Gin Tonic Premium", price = 22000.0, costPrice = 9000.0, category = "Cocteles", stock = 35, aliases = "gin, tonic"),
                    PosProductEntity(companyCode = cleanCode, name = "Whisky Buchanans 12 Años", price = 180000.0, costPrice = 110000.0, category = "Licores", stock = 18, aliases = "whisky, buchanans"),
                    PosProductEntity(companyCode = cleanCode, name = "Tabla de Jamones & Quesos", price = 35000.0, costPrice = 18000.0, category = "Pasabocas", stock = 25, aliases = "tabla, jamon, queso"),
                    PosProductEntity(companyCode = cleanCode, name = "Papas Fritas Gourmet", price = 12000.0, costPrice = 5000.0, category = "Pasabocas", stock = 40, aliases = "papas, fritas")
                )
                sampleProducts.forEach { repository.insertProduct(it) }
            }
        }
    }

    fun getSalesForCompany(code: String): Flow<List<PosSaleEntity>> {
        val cleanCode = code.trim().uppercase()
        return repository.getSalesByCompany(cleanCode).onEach { sales ->
            val v00001 = sales.find { it.clientName.contains("Juan", true) || it.productName.contains("coca", true) }
            android.util.Log.d("KAPTA_DIAG_VIEWMODEL", "VIEWMODEL getSalesForCompany($cleanCode): count=${sales.size}, V-00001 totalAmount=${v00001?.totalAmount ?: 0.0}, sum=${sales.sumOf { it.totalAmount }}")
        }
    }

    fun getTransactionsForCompany(code: String): Flow<List<FinancialTransactionEntity>> {
        val cleanCode = code.trim().uppercase()
        return repository.getTransactionsByCompany(cleanCode)
    }

    fun saveOrUpdateProduct(product: PosProductEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val nombre = product.name.trim()
            if (nombre.isNotBlank()) {
                val existentes = repository.getProductsByCompany(product.companyCode).first()
                val duplicado = existentes.find {
                    it.id != product.id && it.name.trim().equals(nombre, ignoreCase = true)
                }
                if (duplicado != null) {
                    showToast("Ya existe un producto con el nombre '$nombre'")
                    return@launch
                }
            }
            val finalImageUrl = if (product.imageUrl.isNotBlank() && !product.imageUrl.startsWith("http")) {
                subirImagen(product.imageUrl)
            } else product.imageUrl
            val unsynced = product.copy(imageUrl = finalImageUrl, isSynced = false)
            if (unsynced.id > 0) {
                repository.updateProduct(unsynced)
                showToast("Producto '${unsynced.name}' actualizado")
            } else {
                repository.insertProduct(unsynced)
                showToast("Producto '${unsynced.name}' registrado en inventario")
            }
            syncManager.triggerImmediateSync(repository)
            onSuccess()
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            val product = repository.getProductById(productId) ?: return@launch
            val nombre = product.name.trim()
            val ok = try {
                sheetsService.eliminarProducto(product.companyCode, nombre)
            } catch (e: Exception) {
                android.util.Log.e("KaptaViewModel", "Error eliminando producto en servidor: ${e.message}")
                false
            }
            if (!ok) {
                showToast("No se pudo eliminar en el servidor. Intente de nuevo.")
                return@launch
            }
            repository.deleteProduct(productId)
            showToast("Producto '$nombre' eliminado")
        }
    }

    fun registerPosSale(
        companyCode: String,
        clientName: String = "Cliente General",
        productName: String = "Producto Varios",
        quantity: Int = 1,
        unitPrice: Double = 0.0,
        totalAmount: Double = 0.0,
        paymentMethod: String = "Efectivo",
        transferAmount: Double = 0.0,
        cashAmount: Double = 0.0,
        itemCount: Int = quantity,
        timestamp: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val calcTotal = if (totalAmount > 0) totalAmount else (quantity * unitPrice)
            val finalTransfer = if (transferAmount > 0.0 || cashAmount > 0.0) transferAmount else if (paymentMethod.lowercase().contains("transfer") || paymentMethod.lowercase().contains("nequi") || paymentMethod.lowercase().contains("daviplata") || paymentMethod.lowercase().contains("tarjeta")) calcTotal else 0.0
            val finalCash = if (transferAmount > 0.0 || cashAmount > 0.0) cashAmount else if (paymentMethod.lowercase().contains("efectivo") || paymentMethod.isBlank()) calcTotal else 0.0

            val sale = PosSaleEntity(
                companyCode = companyCode,
                clientName = clientName,
                productName = productName,
                quantity = quantity,
                unitPrice = unitPrice,
                totalAmount = calcTotal,
                paymentMethod = paymentMethod,
                transferAmount = finalTransfer,
                cashAmount = finalCash,
                timestamp = timestamp,
                itemCount = itemCount,
                isSynced = false
            )
            repository.insertSale(sale)
            syncManager.triggerImmediateSync(repository)
            onSuccess()
        }
    }

    fun pagarDeudor(
        companyCode: String,
        clientName: String,
        productName: String = "",
        paymentMethod: String = "Efectivo",
        transferAmount: Double = 0.0,
        cashAmount: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val company = repository.getCompanyByCode(companyCode)
            val sheetName = company?.name ?: companyCode
            val success = sheetsService.pagarDeudor(
                sheetName = sheetName,
                clientName = clientName,
                productName = productName,
                paymentMethod = paymentMethod,
                transferAmount = transferAmount,
                cashAmount = cashAmount
            )
            if (success) {
                showToast("¡Deudor $clientName pagado y movido a Ventas!")
                onSuccess()
            } else {
                showToast("Deuda liquidada localmente para $clientName")
                onSuccess()
            }
        }
    }

    fun enviarSoporte(
        companyCode: String,
        tipoSolicitud: String,
        observaciones: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val company = repository.getCompanyByCode(companyCode)
            val sheetName = company?.name ?: companyCode
            val codigo = company?.code ?: companyCode
            val success = sheetsService.registrarSoporte(
                sheetName = sheetName,
                tipoSolicitud = tipoSolicitud,
                observaciones = observaciones,
                solicitante = codigo
            )
            if (success) {
                showToast("Solicitud de soporte enviada. Te contactaremos pronto.")
            } else {
                showToast("No se pudo enviar la solicitud. Intenta de nuevo.")
            }
            onSuccess()
        }
    }

    val soportes = mutableStateOf<List<Map<String, Any>>>(emptyList())

    fun cargarSoportes() {
        viewModelScope.launch {
            val lista = sheetsService.listarSoportes()
            if (lista != null) soportes.value = lista
        }
    }

    fun importarInventario(
        companyCode: String,
        csv: String,
        onResult: (insertados: Int, errores: List<String>) -> Unit
    ) {
        viewModelScope.launch {
            val resp = sheetsService.importarInventario(companyCode, csv)
            if (resp != null && resp.optString("status") == "success") {
                val data = resp.optJSONObject("data")
                val insertados = data?.optInt("insertados", 0) ?: 0
                val errores = mutableListOf<String>()
                val arr = data?.optJSONArray("errores")
                if (arr != null) for (i in 0 until arr.length()) errores.add(arr.optString(i))
                syncTenantBusinessDataFromSheets(companyCode, companyCode)
                onResult(insertados, errores)
            } else {
                onResult(0, listOf(resp?.optString("message") ?: "Error desconocido"))
            }
        }
    }

    fun registrarDeudorDirecto(
        companyCode: String,
        clientName: String,
        productName: String,
        quantity: Int,
        isMinPrice: Boolean = false,
        abonoAmount: Double = 0.0,
        abonoMethod: String = "",
        pendingTotal: Double,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val company = repository.getCompanyByCode(companyCode)
            val sheetName = company?.name ?: companyCode
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val minFlag = if (isMinPrice) "SI" else ""
            val abonoEsTransferencia = abonoMethod.lowercase().contains("transfer") || abonoMethod.lowercase().contains("nequi") || abonoMethod.lowercase().contains("daviplata") || abonoMethod.lowercase().contains("tarjeta")
            val abonoTransfer = if (abonoEsTransferencia) abonoAmount else 0.0
            val abonoCash = if (!abonoEsTransferencia) abonoAmount else 0.0
            val payload = listOf(dateStr, clientName, productName, quantity, minFlag, abonoTransfer, abonoCash, pendingTotal)
            sheetsService.registrarDeudor(sheetName, payload)
            onSuccess()
        }
    }

    fun createOrUpdateUser(user: CompanyUserEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val unsynced = user.copy(isSynced = false)
            if (unsynced.id > 0) {
                repository.updateUser(unsynced)
                showToast("Usuario '${unsynced.name}' actualizado")
            } else {
                repository.insertUser(unsynced)
                showToast("Usuario '${unsynced.name}' registrado exitosamente")
            }
            syncManager.triggerImmediateSync(repository)
            onSuccess()
        }
    }

    fun deleteUser(user: CompanyUserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchingComp = repository.getCompanyByCode(user.companyCode) ?: repository.getCompanyById(user.companyId)
            val companyName = matchingComp?.name ?: user.companyCode

            val userEmailToDelete = user.email.ifBlank { user.username }
            val success = try {
                sheetsService.eliminarUsuario(
                    empresaNombre = companyName,
                    userEmail = userEmailToDelete
                )
            } catch (e: Exception) {
                Log.e("KaptaViewModel", "Error al eliminar usuario en Google Sheets: ${e.message}")
                false
            }

            if (success) {
                repository.deleteUser(user.id)
                withContext(Dispatchers.Main) {
                    showToast("Usuario '${user.name}' eliminado exitosamente de Google Sheets")
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("Error al eliminar '${user.name}' en Google Sheets. Intente de nuevo.")
                }
            }
        }
    }

    private suspend fun ensureCompanyDataCompleted(dao: com.example.data.local.dao.KaptaDao) {
        val list = dao.getAllCompaniesSync()
        for (company in list) {
            val adminUser = dao.getAdminUserByCompanyId(company.id)
            if (adminUser == null && company.code.isNotBlank()) {
                dao.insertUser(
                    CompanyUserEntity(
                        companyId = company.id,
                        companyCode = company.code,
                        name = company.adminName.ifBlank { "Administrador General" },
                        role = "Administrador",
                        email = company.adminEmail.ifBlank { "admin@${company.code}.com" },
                        username = "admin",
                        password = "adminPassword123"
                    )
                )
            }
        }
    }

    fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }
}
