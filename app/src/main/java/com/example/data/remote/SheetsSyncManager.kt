package com.example.data.remote

import android.util.Log
import com.example.data.repository.KaptaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SheetsSyncManager(
    private val sheetsService: SheetsDatabaseService,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "SheetsSyncManager"
        private const val POLLING_INTERVAL_MS = 10_000L // 10 segundos short polling
    }

    private var pollingJob: Job? = null
    private val _isPollingActive = MutableStateFlow(false)
    val isPollingActive: StateFlow<Boolean> = _isPollingActive.asStateFlow()

    val syncState: StateFlow<SyncState> = sheetsService.syncState

    /**
     * Sincroniza en segundo plano todos los registros no sincronizados de Room (isSynced = false)
     * enviándolos vía HTTP POST a Apps Script.
     * Si responde con status "success" (o HTTP 200), actualiza el registro en Room a isSynced = true.
     * Si falla, se mantiene en false para reintentar silenciosamente cuando haya conexión.
     */
    suspend fun syncPendingRecords(repository: KaptaRepository) {
        try {
            val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

            // 1. Unsynced Sales & Deudores
            val unsyncedSales = repository.getUnsyncedSales()
            for (sale in unsyncedSales) {
                val company = repository.getCompanyByCode(sale.companyCode)
                val sheetName = company?.name ?: sale.companyCode

                val isCreditSale = sale.paymentMethod.lowercase().contains("crédito") ||
                        sale.paymentMethod.lowercase().contains("credito") ||
                        sale.paymentMethod.lowercase().contains("debe") ||
                        sale.paymentMethod.lowercase().contains("deudor") ||
                        sale.paymentMethod.lowercase().contains("fiado")

                val dateStr = dateFormatter.format(java.util.Date(sale.timestamp))
                val dateParts = dateStr.split(" ")
                val fecha = dateParts.getOrElse(0) { dateStr }
                val hora = dateParts.getOrElse(1) { "" }
                val qty = if (sale.quantity > 0) sale.quantity else sale.itemCount
                val minPriceFlag = if (sale.paymentMethod.lowercase().contains("mínimo") || sale.paymentMethod.lowercase().contains("minimo")) "SI" else ""
                val unitPrice = if (sale.unitPrice > 0) sale.unitPrice else if (qty > 0) sale.totalAmount / qty else 0.0
                val subtotal = qty * unitPrice
                val usuario = company?.adminName ?: ""

                val success = if (isCreditSale) {
                    // Deudores (AJ:AQ - 8 cols): Fecha_Registro(AJ), Nom_Cliente(AK), Producto(AL), Cantidad(AM), Minimo(AN), Transferencia(AO), Efectivo(AP), Total_Pendiente(AQ)
                    val client = sale.clientName.ifBlank { "Cliente Crédito" }
                    val prodName = sale.productName.ifBlank { "Venta a Crédito" }
                    val abonoTransfer = if (sale.paymentMethod.lowercase().contains("transfer") || sale.paymentMethod.lowercase().contains("nequi") || sale.paymentMethod.lowercase().contains("daviplata") || sale.paymentMethod.lowercase().contains("tarjeta")) sale.totalAmount else 0.0
                    val abonoCash = if (sale.transferAmount > 0.0 || sale.cashAmount > 0.0) sale.cashAmount else if (!sale.paymentMethod.lowercase().contains("transfer") && !sale.paymentMethod.lowercase().contains("nequi") && !sale.paymentMethod.lowercase().contains("daviplata") && !sale.paymentMethod.lowercase().contains("tarjeta")) sale.totalAmount else 0.0
                    val deudorPayload = listOf(fecha, client, prodName, qty, minPriceFlag, abonoTransfer, abonoCash, sale.totalAmount, sale.tipoVenta)
                    sheetsService.registrarDeudor(sheetName, deudorPayload)
                } else {
                    // Ventas (N:AH - 21 cols): Id_Venta, Fecha, Hora, Cliente, Id_Producto, Producto, Cantidad, Precio_Unitario, Subtotal, Descuento, Transferencia, Efectivo, Total, Usuario, Estado
                    val client = sale.clientName.ifBlank { "Cliente General" }
                    val prodName = sale.productName.ifBlank { "Producto" }
                    val transferVal = if (sale.transferAmount > 0.0 || sale.cashAmount > 0.0) {
                        sale.transferAmount
                    } else if (sale.paymentMethod.lowercase().contains("transfer") || sale.paymentMethod.lowercase().contains("nequi") || sale.paymentMethod.lowercase().contains("daviplata") || sale.paymentMethod.lowercase().contains("tarjeta")) {
                        sale.totalAmount
                    } else 0.0

                    val cashVal = if (sale.transferAmount > 0.0 || sale.cashAmount > 0.0) {
                        sale.cashAmount
                    } else if (sale.paymentMethod.lowercase().contains("efectivo") || sale.paymentMethod.isBlank()) {
                        sale.totalAmount
                    } else 0.0

                    val ventaPayload = listOf(
                        "", // Id_Venta (lo genera el backend)
                        fecha,          // Fecha
                        hora,           // Hora
                        client,         // Cliente
                        "",             // Id_Producto
                        prodName,       // Producto
                        qty,            // Cantidad
                        unitPrice,      // Precio_Unitario
                        subtotal,       // Subtotal
                        "",             // Descuento
                        transferVal,    // Transferencia
                        cashVal,        // Efectivo
                        sale.totalAmount, // Total
                        usuario,        // Usuario
                        "Activo",       // Estado
                        "", "", "", "", "", "",
                        sale.tipoVenta  // Tipo
                    )
                    sheetsService.registrarVenta(sheetName, ventaPayload)
                }
                if (success) {
                    repository.markSaleSynced(sale.id)
                    Log.d(TAG, "Venta/Deudor #${sale.id} sincronizada exitosamente")
                }
            }

            // 2. Unsynced Users
            val unsyncedUsers = repository.getUnsyncedUsers()
            for (user in unsyncedUsers) {
                val company = repository.getCompanyByCode(user.companyCode)
                val sheetName = company?.name ?: user.companyCode
                // Usuarios (BQ:CA - 12 cols): Id_Usuario, Nombre, Correo, Contraseña, Rol, Estado, Fecha_Creacion, Ultimo_Acceso, Fecha_Cambio_Estado, Motivo_Cambio, Cambiado_Por, Funciones
                val payload = listOf("", user.name.ifBlank { user.username }, user.email, user.password, user.role, "Activo", user.creationDate, user.lastAccess, "", "", "", user.assignedFunctionsJson)
                val success = sheetsService.crearUsuario(sheetName, payload)
                if (success) {
                    repository.markUserSynced(user.id)
                    Log.d(TAG, "Usuario #${user.id} (${user.username}) sincronizado exitosamente")
                }
            }

            // 3. Unsynced Financial Transactions (Gastos)
            val unsyncedTx = repository.getUnsyncedTransactions()
            for (tx in unsyncedTx) {
                val company = repository.getCompanyByCode(tx.companyCode)
                val sheetName = company?.name ?: tx.companyCode
                // Gastos (AS:BF - 14 cols): Id_Gasto, Fecha, Hora, Categoría, Concepto, Descripción, Proveedor, Monto, Método_Pago, Referencia, Usuario, Estado, Fecha_Modificación, Modificado_Por
                val payload = listOf("", tx.dateString, "", tx.category.ifBlank { "Operativo" }, tx.subtitle.ifBlank { tx.title }, "", "", tx.amount, "", "", "", "Activo", "", "")
                val success = sheetsService.registrarGasto(sheetName, payload)
                if (success) {
                    repository.markTransactionSynced(tx.id)
                    Log.d(TAG, "Transacción #${tx.id} sincronizada exitosamente")
                }
            }

            // 4. Unsynced Products / Stock Updates (Inventario)
            val unsyncedProducts = repository.getUnsyncedProducts()
            for (prod in unsyncedProducts) {
                val company = repository.getCompanyByCode(prod.companyCode)
                val sheetName = company?.name ?: prod.companyCode
                // Inventario (A:M - 13 cols): Id_Producto, Codigo_Barras, Nom_Producto, Categoria, Cantidad, Costo, Precio_Venta, Precio_Minimo, Alerta_Stock, Estado, Fecha_Creacion, Ultima_Modificacion, Imagen
                val payload = listOf("", prod.barcode, prod.name, prod.category, prod.stock, prod.costPrice, prod.price, prod.minPrice, prod.minStockAlert, "Activo", "", "", prod.imageUrl)
                val success = sheetsService.registrarInventario(sheetName, payload)
                if (success) {
                    repository.markProductSynced(prod.id)
                    Log.d(TAG, "Producto #${prod.id} (${prod.name}) sincronizado exitosamente")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando sincronización en segundo plano: ${e.message}")
        }
    }

    /**
     * Inicia el Short Polling de 10 segundos en segundo plano.
     */
    fun startShortPolling(
        companyCode: String,
        repository: KaptaRepository? = null,
        onRefetchCriticalTables: suspend (sheetName: String) -> Unit
    ) {
        stopShortPolling()
        _isPollingActive.value = true

        pollingJob = coroutineScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Short Polling iniciado para empresa: $companyCode")
            delay(POLLING_INTERVAL_MS)
            while (isActive) {
                try {
                    val sheetName = companyCode.uppercase()
                    if (repository != null) {
                        syncPendingRecords(repository)
                    }
                    onRefetchCriticalTables(sheetName)
                } catch (e: Exception) {
                    Log.e(TAG, "Error durante short polling: ${e.message}")
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    /**
     * Detiene el polling de sincronización en segundo plano.
     */
    fun stopShortPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _isPollingActive.value = false
    }

    /**
     * Disparador manual para cuando la vista/pantalla recupera el foco.
     */
    fun refetchOnWindowFocus(
        companyCode: String,
        repository: KaptaRepository? = null,
        onRefetchCriticalTables: suspend (sheetName: String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (repository != null) {
                    syncPendingRecords(repository)
                }
                val sheetName = companyCode.uppercase()
                onRefetchCriticalTables(sheetName)
            } catch (e: Exception) {
                Log.e(TAG, "Error en refetch por foco: ${e.message}")
            }
        }
    }

    fun triggerImmediateSync(repository: KaptaRepository) {
        coroutineScope.launch(Dispatchers.IO) {
            syncPendingRecords(repository)
        }
    }
}
