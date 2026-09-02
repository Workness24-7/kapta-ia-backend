package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.FuncionLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

const val APPS_SCRIPT_WEB_APP_URL = "https://kapta-ia-backend-production.up.railway.app"

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(
        val message: String = "Sincronizando...",
        val isRetrying: Boolean = false,
        val attempt: Int = 1
    ) : SyncState()
    data class Success(val lastSyncedAt: Long = System.currentTimeMillis()) : SyncState()
    data class Error(val message: String) : SyncState()
}

data class SheetDataResult(
    val range: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val rawValues: List<List<String>>
)

data class RemoteCompany(
    val codigo: String = "",
    val pais: String = "",
    val nombre: String = "",
    val idEmpresa: String = "",
    val correoAdmin: String = "",
    val passwordAdmin: String = "\n",
    val estado: String = "",
    val plan: String = "",
    val tiempo: String = "",
    val fechaVencimiento: String = "")

data class LoginResultado(
    val idEmpresa: String?,
    val mensajeError: String?
)

data class FinanzaKapta(
    val id: Int = 0,
    val fecha: String = "",
    val tipo: String = "",
    val categoria: String = "",
    val concepto: String = "",
    val monto: Double = 0.0,
    val metodoPago: String = "",
    val referencia: String = "",
    val usuario: String = ""
)

data class FinanzasKaptaResumen(
    val registros: List<FinanzaKapta> = emptyList(),
    val totalIngresos: Double = 0.0,
    val totalEgresos: Double = 0.0,
    val balance: Double = 0.0
)

// TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
data class SheetsTableDiagnostic(    val tableName: String = "",
    val sheetName: String = "",
    val rangeParam: String = "",
    val sanitizedUrl: String = "",
    val httpMethod: String = "GET",
    val httpStatusCode: Int? = null,
    val isSuccessful: Boolean = false,
    val rowsReceived: Int = 0,
    val columnCount: Int = 0,
    val isNullResponse: Boolean = true,
    val hasException: Boolean = false,
    val exceptionType: String = "",
    val exceptionMessage: String = "",
    val fallbackUsed: Boolean = false,
    val fallbackHttpStatusCode: Int? = null,
    val fallbackEmptyResponse: Boolean = true,
    val sheetsFetchDiagnosticStatus: String = "EMPTY_RESPONSE\n")

class SheetsDatabaseService(
    private val spreadsheetId: String = DEFAULT_SPREADSHEET_ID,
    private val webAppScriptUrl: String? = null
) {
    companion object {
        const val DEFAULT_SPREADSHEET_ID = "1HY--pHW5xQ79VO8Ip_Go4g0vKSRPX5Blv_aTUfcPpgw"
        private const val TAG = "SheetsDatabaseService"
        private const val BASE_URL = "https://sheets.googleapis.com/v4/spreadsheets"

        // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
        fun sanitizeSecrets(text: String?): String {
            if (text == null) return ""
            return text
                .replace(Regex("key=[^&]+"), "key=***")
                .replace(Regex("AKfycb[a-zA-Z0-9_-]+"), "AKfycb***")
                .replace(Regex("AIzaSy[a-zA-Z0-9_-]+"), "AIzaSy***")
        }
    }

    // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
    val lastFetchDiagnostics = java.util.Collections.synchronizedList(mutableListOf<SheetsTableDiagnostic>())

    // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
    fun clearFetchDiagnostics() {
        lastFetchDiagnostics.clear()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    @Volatile
    var currentIdEmpresa: String? = null

    private fun getApiKey(): String {
        return try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val valStr = keyField.get(null) as? String
            if (valStr != null && valStr.trim().isNotEmpty() && !valStr.contains("Placeholder", ignoreCase = true) && valStr.trim().length > 20) {
                valStr.trim()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Convierte cualquier estructura JSON válida retornada por Google Sheets API v4 o Apps Script
     * en una lista bidimensional de cadenas (List<List<String>>).
     */
    fun parseJsonTo2DList(jsonStr: String, tableName: String? = null): MutableList<List<String>>? {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty()) return null

        try {
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                return parseJsonArrayTo2DList(jsonArray)
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)

                // 1. Probar en la raíz del objeto
                var arrayToParse = findDataArrayInObject(jsonObject, tableName)

                // 2. Probar dentro del campo "data" si es un JSONObject
                if (arrayToParse == null && jsonObject.has("data")) {
                    val dataObj = jsonObject.optJSONObject("data")
                    if (dataObj != null) {
                        arrayToParse = findDataArrayInObject(dataObj, tableName)
                    }
                }

                // 3. Probar dentro del campo "result" si es un JSONObject
                if (arrayToParse == null && jsonObject.has("result")) {
                    val resultObj = jsonObject.optJSONObject("result")
                    if (resultObj != null) {
                        arrayToParse = findDataArrayInObject(resultObj, tableName)
                    }
                }

                if (arrayToParse != null) {
                    return parseJsonArrayTo2DList(arrayToParse)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing JSON to 2D List: ${e.message}")
        }
        return null
    }

    private fun findDataArrayInObject(obj: JSONObject, tableName: String?): JSONArray? {
        // A. Si se especificó tableName, buscar por el nombre de la tabla primero
        if (!tableName.isNullOrBlank()) {
            val cleanTable = tableName.trim().lowercase()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.trim().lowercase() == cleanTable) {
                    val arr = obj.optJSONArray(key)
                    if (arr != null) return arr
                }
            }
        }

        // B. Buscar claves estándar de arreglos
        val standardKeys = listOf("values", "data", "rows", "result", "items", "records", "dataRows", "empresas", "companies")
        for (key in standardKeys) {
            val arr = obj.optJSONArray(key)
            if (arr != null) return arr
        }

        // C. Si no coincide ninguna clave estándar, buscar el primer JSONArray dentro del objeto
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val arr = obj.optJSONArray(key)
            if (arr != null) return arr
        }

        return null
    }

    private fun parseJsonArrayTo2DList(jsonArray: JSONArray): MutableList<List<String>> {
        val rows = mutableListOf<List<String>>()
        if (jsonArray.length() == 0) return rows

        val firstItem = jsonArray.opt(0)

        if (firstItem is JSONArray) {
            // Arreglo 2D de arreglos: [ ["a", "b"], ["c", "d"] ]
            for (i in 0 until jsonArray.length()) {
                val rowArray = jsonArray.optJSONArray(i) ?: continue
                val rowList = mutableListOf<String>()
                for (j in 0 until rowArray.length()) {
                    rowList.add(rowArray.optString(j, ""))
                }
                rows.add(rowList)
            }
        } else if (firstItem is JSONObject) {
            // Arreglo de objetos JSON: [ {"col1": "val1", "col2": "val2"}, ... ]
            val keysList = mutableListOf<String>()
            val keysIterator = firstItem.keys()
            while (keysIterator.hasNext()) {
                keysList.add(keysIterator.next())
            }
            val numericKeys = keysList.filter { it.trim().toIntOrNull() != null }
            val isNumericArray = numericKeys.isNotEmpty() && numericKeys.size >= keysList.size / 2

            val finalKeys = if (isNumericArray) {
                numericKeys.sortedBy { it.trim().toInt() }
            } else {
                rows.add(keysList)
                keysList
            }

            for (i in 0 until jsonArray.length()) {
                val itemObj = jsonArray.optJSONObject(i) ?: continue
                val rowList = mutableListOf<String>()
                for (key in finalKeys) {
                    rowList.add(itemObj.optString(key, ""))
                }
                rows.add(rowList)
            }
        } else {
            // Arreglo plano de cadenas/valores
            val flatRow = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                flatRow.add(jsonArray.optString(i, ""))
            }
            rows.add(flatRow)
        }

        return rows
    }

    /**
     * 3. Enrutamiento Dinámico de Hojas (Multi-tenant)
     * Consultas dinámicas según el nombre de la empresa y la tabla requerida.
     * Ej: "CAFÉ CENTRAL!A:Z" o "Data Maestra!A:Z"
     */
    suspend fun fetchBusinessData(
        sheetName: String,
        tableName: String? = null
    ): SheetDataResult? = withContext(Dispatchers.IO) {
        val formattedSheet = sheetName.trim()
        val rangeParam = when (tableName?.trim()?.lowercase()) {
            "inventario" -> "$formattedSheet!A:M"
            "ventas", "venta" -> "$formattedSheet!N:AH"
            "deudores", "deudor" -> "$formattedSheet!AJ:AQ"
            "gastos", "gasto" -> "$formattedSheet!AS:BF"
            "auditoria_gastos", "auditoria_gasto" -> "$formattedSheet!AK:AN"
            "usuarios", "usuario" -> "$formattedSheet!BQ:CA"
            "movimientos", "movimiento" -> "$formattedSheet!AZ:BE"
            "config_negocio", "config" -> "$formattedSheet!A:F"
            else -> null
        } ?: return@withContext null

        executeWithRetry {
            var allRows: MutableList<List<String>>? = null

            // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
            var diagStatusCode: Int? = null
            var diagHasException = false
            var diagExceptionType = ""
            var diagExceptionMsg = ""
            var diagFallbackUsed = false
            var diagFallbackStatusCode: Int? = null
            var diagFallbackEmptyResponse = true
            var diagSanitizedUrl = ""

            // 1. Fallback Apps Script Método A: GET request con parámetros completos
            if (allRows.isNullOrEmpty()) {
                diagFallbackUsed = true
                val targetUrl = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val cleanTableName = tableName ?: ""
                val scriptUrl = "$targetUrl?action=read&sheetName=${URLEncoder.encode(formattedSheet, "UTF-8")}&tableName=${URLEncoder.encode(cleanTableName, "UTF-8")}&range=${URLEncoder.encode(rangeParam, "UTF-8")}&idEmpresa=${URLEncoder.encode(currentIdEmpresa ?: "", "UTF-8")}"
                
                if (diagSanitizedUrl.isBlank()) {
                    diagSanitizedUrl = sanitizeSecrets(scriptUrl)
                } else {
                    diagSanitizedUrl += " | Fallback GET: " + sanitizeSecrets(scriptUrl)
                }

                val request = Request.Builder().url(scriptUrl).get().build()
                try {
                    client.newCall(request).execute().use { response ->
                        diagFallbackStatusCode = response.code
                        if (response.code == 429) throw RateLimitException("429 Rate Limit")
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string() ?: ""
                            val parsed = parseJsonTo2DList(responseBody, tableName)
                            if (!parsed.isNullOrEmpty()) {
                                allRows = parsed
                                diagFallbackEmptyResponse = false
                            }
                        }
                    }
                } catch (e: RateLimitException) {
                    throw e
                } catch (e: Exception) {
                    diagHasException = true
                    diagExceptionType = e.javaClass.simpleName
                    diagExceptionMsg = sanitizeSecrets(e.message)
                    Log.w(TAG, "Apps Script GET fallback error: ${e.message}")
                }
            }

            // 2. Fallback Apps Script Método B: POST request con payload JSON si GET retornó vacío
            if (allRows.isNullOrEmpty()) {
                val targetUrl = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                diagSanitizedUrl += " | Fallback POST"

                val jsonPayload = JSONObject().apply {
                    val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else formattedSheet
                    put("action", "read")
                    put("sheetName", effectiveSheet)
                    if (!tableName.isNullOrBlank()) {
                        put("tableName", tableName)
                        put("targetTable", tableName)
                    }
                    put("range", rangeParam)
                    if (!currentIdEmpresa.isNullOrBlank()) {
                        put("idEmpresa", currentIdEmpresa)
                    }
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(targetUrl).post(body).build()

                try {
                    client.newCall(request).execute().use { response ->
                        diagFallbackStatusCode = response.code
                        if (response.code == 429) throw RateLimitException("429 Rate Limit")
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string() ?: ""
                            val parsed = parseJsonTo2DList(responseBody, tableName)
                            if (!parsed.isNullOrEmpty()) {
                                allRows = parsed
                                diagFallbackEmptyResponse = false
                            }
                        }
                    }
                } catch (e: RateLimitException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Apps Script POST fallback error: ${e.message}")
                }
            }

            // 3. Fallback Apps Script Método C: POST 'obtener_todo' si sigue vacío
            if (allRows.isNullOrEmpty()) {
                try {
                    val todoObj = obtenerTodo(formattedSheet)
                    if (todoObj != null) {
                        val parsed = parseJsonTo2DList(todoObj.toString(), tableName)
                        if (!parsed.isNullOrEmpty()) {
                            allRows = parsed
                            diagFallbackEmptyResponse = false
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Apps Script obtenerTodo fallback error: ${e.message}")
                }
            }

            // TEMPORAL - AUDIT_EXECUTION SHEETS DIAGNOSTIC
            val finalStatus = when {
                !allRows.isNullOrEmpty() -> if (diagFallbackUsed) "FALLBACK_USED" else "SUCCESS"
                allRows != null && allRows!!.isEmpty() -> "EMPTY_RESPONSE"
                diagHasException -> if (diagFallbackUsed) "FALLBACK_ERROR" else "EXCEPTION"
                diagStatusCode != null && diagStatusCode != 200 -> if (diagFallbackUsed) "FALLBACK_ERROR" else "HTTP_ERROR"
                else -> "EMPTY_RESPONSE"
            }

            val tableDiag = SheetsTableDiagnostic(
                tableName = tableName ?: "General",
                sheetName = formattedSheet,
                rangeParam = rangeParam,
                sanitizedUrl = diagSanitizedUrl,
                httpMethod = "GET/POST",
                httpStatusCode = diagStatusCode ?: diagFallbackStatusCode,
                isSuccessful = !allRows.isNullOrEmpty(),
                rowsReceived = allRows?.size ?: 0,
                columnCount = allRows?.firstOrNull()?.size ?: 0,
                isNullResponse = (allRows == null),
                hasException = diagHasException,
                exceptionType = diagExceptionType,
                exceptionMessage = diagExceptionMsg,
                fallbackUsed = diagFallbackUsed,
                fallbackHttpStatusCode = diagFallbackStatusCode,
                fallbackEmptyResponse = diagFallbackEmptyResponse,
                sheetsFetchDiagnosticStatus = finalStatus
            )
            lastFetchDiagnostics.add(tableDiag)

            if (allRows.isNullOrEmpty()) return@executeWithRetry null

            val rowsList = allRows!!
            val headers = if (rowsList.isNotEmpty()) rowsList[0] else emptyList()
            val dataRows = if (rowsList.size > 1) rowsList.subList(1, rowsList.size) else emptyList()

            SheetDataResult(
                range = rangeParam,
                headers = headers,
                rows = dataRows,
                rawValues = rowsList
            )
        }
    }

    /**
     * Inicio de Sesión Aislado (action: "login"):
     * Payload: {"action": "login", "pais": "CO", "codigo": "1001", "correo": "admin@empresa.com", "password": "1234"}
     * Responderá con un identificador de sesión único (idEmpresa), ej: CO_1001_LicoresDonPedro.
     */
    suspend fun login(
        pais: String,
        codigo: String,
        correo: String,
        password: String
    ): LoginResultado = withContext(Dispatchers.IO) {
        val resultado = try {
            executeWithRetry {
                val jsonPayload = JSONObject().apply {
                    put("action", "login")
                    put("pais", pais)
                    put("codigo", codigo)
                    put("correo", correo)
                    put("password", password)
                    // Keys mapping from Base Maestra
                    put("Pais", pais)
                    put("Codigo_Negocio", codigo)
                    put("Correo_Admin", correo)
                    put("Contraseña_Admin", password)
                    put("Contrasena_Admin", password)
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val responseStr = response.body?.string() ?: ""
                    Log.d(TAG, "login response code ${response.code}")
                    val json = JSONObject(responseStr)
                    if (json.optString("status") == "success") {
                        val idEmpresa = json.optString("idEmpresa")
                            .ifBlank { json.optString("id_empresa") }
                            .ifBlank { json.optString("Id_Negocio") }
                            .ifBlank { json.optString("id_negocio") }
                            .ifBlank { json.optString("idEmpresaSesion") }
                            .ifBlank { "${pais}_${codigo}" }
                        currentIdEmpresa = idEmpresa
                        LoginResultado(idEmpresa, null)
                    } else {
                        LoginResultado(null, json.optString("message").ifBlank { null })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en llamada login: ${e.message}", e)
            LoginResultado(null, null)
        }
        resultado ?: LoginResultado(null, null)
    }

    /**
     * Acción para Listar Empresas:
     * Al cargar la pantalla principal de selección de negocios, realiza una petición POST con {"action": "listar_empresas"}.
     * Retorna un arreglo con todas las empresas registradas (código, país, nombre, idEmpresa, correoAdmin, passwordAdmin).
     */
    suspend fun listarEmpresas(): List<RemoteCompany> = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val requestUrl = if (targetUrl.contains("?")) "$targetUrl&action=listar_empresas" else "$targetUrl?action=listar_empresas"
            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .build()

            val list = mutableListOf<RemoteCompany>()
            client.newCall(request).execute().use { response ->
                val responseCode = response.code
                val contentType = response.header("Content-Type") ?: "N/A"
                val locationHeader = response.header("Location") ?: "N/A"
                val responseStr = response.body?.string() ?: ""

                Log.d(TAG, "1. URL exacta utilizada: $requestUrl")
                Log.d(TAG, "2. Código HTTP final: $responseCode")
                Log.d(TAG, "3. Content-Type: $contentType (Location: $locationHeader)")
                Log.d(TAG, "4. Longitud de response body: ${responseStr.length}")
                Log.d(TAG, "5. Primeros 2000 caracteres del response body: ${responseStr.take(2000)}")

                if (response.isSuccessful && responseStr.isNotBlank()) {
                    try {
                        val rootObj = try { JSONObject(responseStr) } catch (e: Exception) { null }
                        val rootStatus = rootObj?.optString("status") ?: "N/A"
                        Log.d(TAG, "6. Si el JSON raíz tiene status=\"success\": status = '$rootStatus'")

                        val dataObj = rootObj?.optJSONObject("data")
                        val dataArray = rootObj?.optJSONArray("data")
                        Log.d(TAG, "7. Si existe el objeto \"data\": dataIsObj=${dataObj != null}, dataIsArray=${dataArray != null}")

                        val empresasArrayDirect = rootObj?.optJSONArray("empresas")
                        val empresasArrayNested = dataObj?.optJSONArray("empresas")
                        Log.d(TAG, "8. Si existe el JSONArray \"empresas\": direct=${empresasArrayDirect != null}, nestedInObj=${empresasArrayNested != null}")

                        val jsonArray = if (responseStr.trim().startsWith("[")) {
                            JSONArray(responseStr)
                        } else {
                            val jsonObj = JSONObject(responseStr)
                            jsonObj.optJSONObject("data")?.optJSONArray("empresas")
                                ?: jsonObj.optJSONArray("empresas")
                                ?: jsonObj.optJSONArray("companies")
                                ?: JSONArray()
                        }

                        Log.d(TAG, "9. empresas.length() (en jsonArray actual): ${jsonArray.length()}")

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val codigo = item.optString("codigo")
                                .ifBlank { item.optString("code") }
                                .ifBlank { item.optString("Codigo_Negocio") }
                                .ifBlank { item.optString("codigo_negocio") }
                                .ifBlank { item.optString("Codigo") }

                            val pais = item.optString("pais")
                                .ifBlank { item.optString("country") }
                                .ifBlank { item.optString("Pais") }

                            val nombre = item.optString("nombre")
                                .ifBlank { item.optString("name") }
                                .ifBlank { item.optString("Nombre_Negocio") }
                                .ifBlank { item.optString("nombre_negocio") }
                                .ifBlank { item.optString("Nombre") }

                            val idEmpresa = item.optString("idEmpresa")
                                .ifBlank { item.optString("id_empresa") }
                                .ifBlank { item.optString("Id_Negocio") }
                                .ifBlank { item.optString("id_negocio") }
                                .ifBlank { item.optString("IdEmpresa") }

                            val correoAdmin = item.optString("Correo_Admin")
                                .ifBlank { item.optString("correo_admin") }
                                .ifBlank { item.optString("adminEmail") }
                                .ifBlank { item.optString("correo") }

                            val passwordAdmin = item.optString("Contraseña_Admin")
                                .ifBlank { item.optString("Contrasena_Admin") }
                                .ifBlank { item.optString("contrasena_admin") }
                                .ifBlank { item.optString("adminPassword") }
                                .ifBlank { item.optString("password") }

                            val estado = item.optString("estado")
                                .ifBlank { item.optString("Estado") }
                            val plan = item.optString("plan")
                                .ifBlank { item.optString("Plan") }
                            val tiempo = item.optString("tiempo")
                                .ifBlank { item.optString("Tiempo") }
                            val fechaVencimiento = item.optString("fechaVencimiento")
                                .ifBlank { item.optString("fecha_vencimiento") }

                            Log.d(TAG, "10. Elemento [$i]: id=$idEmpresa, codigo=$codigo, nombre=$nombre")

                            list.add(
                                RemoteCompany(
                                    codigo = codigo,
                                    pais = pais,
                                    nombre = nombre,
                                    idEmpresa = idEmpresa,
                                    correoAdmin = correoAdmin,
                                    passwordAdmin = passwordAdmin,
                                    estado = estado,
                                    plan = plan,
                                    tiempo = tiempo,
                                    fechaVencimiento = fechaVencimiento
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing listarEmpresas JSON: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "11. Cantidad final de RemoteCompany creados: ${list.size}")
            Log.d(TAG, "DIAGNOSTIC listarEmpresas FINAL COUNT = ${list.size}")
            list
        }
        result ?: emptyList()
    }

    /**
     * Petición HTTP POST a Apps Script con payload exacto:
     * {"sheetName": "NOMBRE_DE_LA_HOJA_DEL_NEGOCIO", "idEmpresa": "CO_1001_LicoresDonPedro", "tableName": "TABLA", "action": "ACTION", "data": [...]}
     */
    suspend fun postRowToAppsScript(
        sheetName: String,
        data: List<Any>,
        tableName: String? = null,
        action: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val flatData = flattenAndNormalizeData(data, tableName, action)
            val jsonPayload = JSONObject().apply {
                val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else sheetName
                put("sheetName", effectiveSheet)
                if (!currentIdEmpresa.isNullOrBlank()) {
                    put("idEmpresa", currentIdEmpresa)
                }
                if (!action.isNullOrBlank()) {
                    put("action", action)
                }
                if (!tableName.isNullOrBlank()) {
                    put("tableName", tableName)
                    put("targetTable", tableName)
                }
                put("data", JSONArray(flatData))
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "postRowToAppsScript action=$action sheetName=$sheetName response code ${response.code}: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    val resultStr = json.optString("result")
                    val message = json.optString("message").lowercase()
                    status.equals("success", ignoreCase = true) ||
                            isSuccess ||
                            resultStr.equals("success", ignoreCase = true) ||
                            message.contains("exito") ||
                            message.contains("registrad") ||
                            message.contains("guardad") ||
                            message.contains("ok")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success") || responseStr.lowercase().contains("exito") || responseStr.lowercase().contains("ok")
                }
                response.isSuccessful && (isSuccessJson || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun registrarInventario(sheetName: String, stockData: List<Any>): Boolean =
        postRowToAppsScript(sheetName = sheetName, data = stockData, tableName = "Inventario", action = "registrar_inventario")

    suspend fun registrarVenta(sheetName: String, saleData: List<Any>): Boolean =
        postRowToAppsScript(sheetName = sheetName, data = saleData, tableName = "Ventas", action = "registrar_venta")

    suspend fun registrarDeudor(sheetName: String, deudorData: List<Any>): Boolean =
        postRowToAppsScript(sheetName = sheetName, data = deudorData, tableName = "Deudores", action = "registrar_deudor")

    suspend fun registrarGasto(sheetName: String, expenseData: List<Any>): Boolean =
        postRowToAppsScript(sheetName = sheetName, data = expenseData, tableName = "Gastos", action = "registrar_gasto")

    suspend fun crearUsuario(sheetName: String, userData: List<Any>): Boolean =
        postRowToAppsScript(sheetName = sheetName, data = userData, tableName = "Usuarios", action = "crear_usuario")

    suspend fun pagarDeudor(
        sheetName: String,
        clientName: String,
        productName: String = "",
        paymentMethod: String = "Efectivo",
        transferAmount: Double = 0.0,
        cashAmount: Double = 0.0,
        usuario: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else sheetName
                put("action", "pagar_deudor")
                put("sheetName", effectiveSheet)
                if (!currentIdEmpresa.isNullOrBlank()) {
                    put("idEmpresa", currentIdEmpresa)
                }
                put("clienteNombre", clientName)
                put("productoNombre", productName)
                put("clientName", clientName)
                put("productName", productName)
                put("paymentMethod", paymentMethod)
                put("metodoPago", paymentMethod)
                put("transferAmount", transferAmount)
                put("cashAmount", cashAmount)
                put("usuario", usuario)
                put("data", JSONArray(listOf(clientName, productName, paymentMethod, transferAmount, cashAmount)))
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "pagarDeudor response code ${response.code}: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    val message = json.optString("message").lowercase()
                    status.equals("success", ignoreCase = true) || isSuccess || message.contains("exito") || message.contains("pagad") || message.contains("ok")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success") || responseStr.lowercase().contains("ok") || responseStr.lowercase().contains("exito")
                }
                response.isSuccessful && (isSuccessJson || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun asignarPerdedor(
        sheetName: String,
        clientName: String,
        productName: String = "",
        perdedor: String
    ): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else sheetName
                put("action", "asignar_perdedor")
                put("sheetName", effectiveSheet)
                if (!currentIdEmpresa.isNullOrBlank()) {
                    put("idEmpresa", currentIdEmpresa)
                }
                put("clienteNombre", clientName)
                put("clientName", clientName)
                put("productoNombre", productName)
                put("productName", productName)
                put("perdedor", perdedor)
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "asignarPerdedor response code ${response.code}: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    val message = json.optString("message").lowercase()
                    status.equals("success", ignoreCase = true) || isSuccess ||
                            message.contains("exito") || message.contains("guardad") || message.contains("ok")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success") || responseStr.lowercase().contains("ok") || responseStr.lowercase().contains("exito")
                }
                response.isSuccessful && (isSuccessJson || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun dividirChico(
        sheetName: String,
        bolirranaName: String,
        chico: Int,
        personas: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else sheetName
                put("action", "dividir_chico")
                put("sheetName", effectiveSheet)
                if (!currentIdEmpresa.isNullOrBlank()) {
                    put("idEmpresa", currentIdEmpresa)
                }
                put("clienteNombre", bolirranaName)
                put("clientName", bolirranaName)
                put("chico", chico)
                put("partes", JSONArray(personas))
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "dividirChico response code ${response.code}: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    val message = json.optString("message").lowercase()
                    status.equals("success", ignoreCase = true) || isSuccess ||
                            message.contains("exito") || message.contains("ok")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success") || responseStr.lowercase().contains("ok") || responseStr.lowercase().contains("exito")
                }
                response.isSuccessful && (isSuccessJson || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun registrarSoporte(
        sheetName: String,
        tipoSolicitud: String,
        observaciones: String,
        solicitante: String
    ): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else sheetName
                put("action", "registrar_soporte")
                put("sheetName", effectiveSheet)
                if (!currentIdEmpresa.isNullOrBlank()) put("idEmpresa", currentIdEmpresa)
                put("tipo_solicitud", tipoSolicitud)
                put("observaciones", observaciones)
                put("solicitante", solicitante)
            }
            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder().url(targetUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "registrarSoporte response code ${response.code}: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    val message = json.optString("message").lowercase()
                    status.equals("success", ignoreCase = true) || isSuccess ||
                            message.contains("exito") || message.contains("registrad") || message.contains("ok")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success") || responseStr.lowercase().contains("ok") || responseStr.lowercase().contains("exito")
                }
                response.isSuccessful && (isSuccessJson || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun obtenerTodo(sheetName: String): JSONObject? = withContext(Dispatchers.IO) {
        executeWithRetry {
            val jsonPayload = JSONObject().apply {
                val effectiveSheet = if (!currentIdEmpresa.isNullOrBlank()) currentIdEmpresa!! else sheetName
                put("action", "obtener_todo")
                put("sheetName", effectiveSheet)
                if (!currentIdEmpresa.isNullOrBlank()) {
                    put("idEmpresa", currentIdEmpresa)
                }
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "obtenerTodo response code ${response.code}: $responseStr")
                if (response.isSuccessful && responseStr.isNotBlank()) {
                    try {
                        JSONObject(responseStr)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing obtenerTodo JSON: ${e.message}")
                        null
                    }
                } else null
            }
        }
    }

    suspend fun listarSoportes(): List<Map<String, Any>>? = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("action", "listar_soportes")
            }
            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder().url(targetUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                val json = JSONObject(responseStr)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                val list = mutableListOf<Map<String, Any>>()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val map = mutableMapOf<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = obj.get(k)
                    }
                    list.add(map)
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "listarSoportes error: ${e.message}")
            null
        }
    }

    suspend fun actualizarStock(sheetName: String, stockData: List<Any>): Boolean =
        registrarInventario(sheetName, stockData)

    /**
     * Registro de nueva empresa en Apps Script con action "registrar_empresa".
     * Payload: {"action": "registrar_empresa", "sheetName": "Data Maestra", "data": [...], "adminEmail": "...", "adminPassword": "..."}
     */
    suspend fun registrarEmpresa(
        payload: Map<String, Any>
    ): JSONObject? = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject(payload).apply {
                put("action", "registrar_empresa")
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            
            android.util.Log.d("KAPTA_DIAG_CREATE", "1. [POST URL]: $targetUrl")
            android.util.Log.d("KAPTA_DIAG_CREATE", "2. [PAYLOAD]: ${jsonPayload.toString()}")
            
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    val rawResponseText = if (responseStr.isEmpty()) "<EMPTY>" else responseStr

                    val diagBuilder = java.lang.StringBuilder()
                    diagBuilder.append("DIAGNÓSTICO CREAR EMPRESA\n\n")
                    diagBuilder.append("URL:\n$targetUrl\n\n")
                    diagBuilder.append("PAYLOAD:\n${jsonPayload.toString(2)}\n\n")
                    diagBuilder.append("HTTP CODE:\n${response.code}\n\n")
                    diagBuilder.append("HTTP MESSAGE:\n${response.message}\n\n")
                    diagBuilder.append("isSuccessful:\n${response.isSuccessful}\n\n")
                    diagBuilder.append("RESPUESTA RAW:\n$rawResponseText\n\n")

                    DiagStorage.lastDiagnostic = diagBuilder.toString()

                    var finalResult: JSONObject? = null
                    try {
                        diagBuilder.append("PARSE JSON:\n")
                        val json = JSONObject(responseStr)
                        diagBuilder.append("Exitoso\n\n")
                        
                        val statusVal = json.optString("status", "N/A")
                        val successVal = if (json.has("success")) json.getBoolean("success").toString() else "N/A"
                        
                        diagBuilder.append("STATUS:\n$statusVal\n\n")
                        diagBuilder.append("SUCCESS:\n$successVal\n\n")
                        
                        val dataObj = json.optJSONObject("data")
                        if (dataObj != null) {
                            diagBuilder.append("DATA:\n${dataObj.toString(2)}\n\n")
                        } else {
                            diagBuilder.append("DATA:\nN/A (o no es objeto)\n\n")
                        }
                        
                        if (statusVal == "success" || json.optBoolean("success", false)) {
                            finalResult = dataObj ?: json
                        } else {
                            diagBuilder.append("ERROR:\nEl estado no fue success.\n")
                            finalResult = null
                        }
                        
                    } catch (e: Exception) {
                        diagBuilder.append("Fallo (${e.message})\n\n")
                        diagBuilder.append("ERROR:\nNo se pudo parsear la respuesta como JSON.\n")
                        finalResult = null
                    }
                    
                    DiagStorage.lastDiagnostic = diagBuilder.toString()
                    return@use finalResult
                }
            } catch (e: Exception) {
                val failDiag = "DIAGNÓSTICO CREAR EMPRESA\n\nURL:\n$targetUrl\n\nEXCEPTION:\n${e.javaClass.simpleName} - ${e.message}"
                DiagStorage.lastDiagnostic = failDiag
                throw e
            }
        }
        result
    }

    /**
     * Actualiza campos de la empresa en Apps Script (action "actualizar_empresa").
     * El backend debe localizar la fila por "codigo" y escribir los campos recibidos
     * (logoUrl, listIconUrl, colorPrimario, colorSecundario, colorTerciario, colorNeutro, tipoFuente).
     */
    suspend fun actualizarEmpresa(payload: Map<String, Any>): JSONObject? = withContext(Dispatchers.IO) {
        try {
            executeWithRetry {
                val jsonPayload = JSONObject(payload).apply { put("action", "actualizar_empresa") }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder().url(targetUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    val str = response.body?.string() ?: ""
                    val json = JSONObject(str)
                    if (json.optString("status") == "success" || json.optBoolean("success", false)) json else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando empresa: ${e.message}", e)
            null
        }
    }

    /**
     * Lista la biblioteca global de funciones IA (action "listar_funciones").
     */
    suspend fun listarFunciones(): List<FuncionLib> = withContext(Dispatchers.IO) {
        try {
            executeWithRetry {
                val json = JSONObject().apply { put("action", "listar_funciones") }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder().url(targetUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    val str = response.body?.string() ?: ""
                    val root = JSONObject(str)
                    val arr = root.optJSONArray("data") ?: root.optJSONArray("funciones") ?: JSONArray()
                    val out = mutableListOf<FuncionLib>()
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val nombre = o.optString("nombre").orEmpty()
                        if (nombre.isBlank()) continue
                        out.add(
                            FuncionLib(
                                nombre = nombre,
                                descripcion = o.optString("descripcion", ""),
                                rol = o.optString("rol", ""),
                                planTier = o.optString("planTier", "Basico"),
                                tipoNegocio = o.optString("tipoNegocio", ""),
                                modulo = o.optString("modulo", "")
                            )
                        )
                    }
                    out
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error listando funciones: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun crearFuncion(
        nombre: String,
        descripcion: String,
        rol: String = "",
        planTier: String = "Basico",
        tipoNegocio: String = "",
        modulo: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            executeWithRetry {
                val json = JSONObject().apply {
                    put("action", "crear_funcion")
                    put("nombre", nombre)
                    put("descripcion", descripcion)
                    put("rol", rol)
                    put("planTier", planTier)
                    put("tipoNegocio", tipoNegocio)
                    put("modulo", modulo)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder().url(targetUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    val str = response.body?.string() ?: ""
                    val root = JSONObject(str)
                    root.optString("status") == "success" || root.optBoolean("success", false)
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error creando funcion: ${e.message}", e)
            false
        }
    }

    suspend fun eliminarFuncion(nombre: String): Boolean = withContext(Dispatchers.IO) {
        try {
            executeWithRetry {
                val json = JSONObject().apply { put("action", "eliminar_funcion"); put("nombre", nombre) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder().url(targetUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    val str = response.body?.string() ?: ""
                    val root = JSONObject(str)
                    root.optString("status") == "success" || root.optBoolean("success", false)
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando funcion: ${e.message}", e)
            false
        }
    }

    /**
     * Eliminación en cascada de empresa en Apps Script con action "eliminar_empresa".
     * Payload: {"action": "eliminar_empresa", "empresaNombre": "Nombre de la Empresa"}
     */
    suspend fun eliminarEmpresa(empresaNombre: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = executeWithRetry {
                val jsonPayload = JSONObject().apply {
                    put("action", "eliminar_empresa")
                    put("empresaNombre", empresaNombre)
                    put("sheetName", "Data Maestra")
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val responseStr = response.body?.string() ?: ""
                    Log.d(TAG, "eliminarEmpresa response code ${response.code}: $responseStr")
                    val isSuccessJson = try {
                        val json = JSONObject(responseStr)
                        val status = json.optString("status")
                        val isSuccess = json.optBoolean("success", false)
                        val resultStr = json.optString("result")
                        val message = json.optString("message").lowercase()
                        status.equals("success", ignoreCase = true) ||
                                isSuccess ||
                                resultStr.equals("success", ignoreCase = true) ||
                                message.contains("exito") ||
                                message.contains("eliminad") ||
                                message.contains("ok")
                    } catch (_: Exception) {
                        responseStr.lowercase().contains("success") || responseStr.lowercase().contains("eliminad")
                    }
                    response.isSuccessful && (isSuccessJson || responseStr.isBlank())
                }
            }
            result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error en llamada eliminarEmpresa para '$empresaNombre': ${e.message}", e)
            false
        }
    }

    /**
     * Sube una imagen (base64 JPEG) al backend y devuelve su URL pública /foto/{id}.
     */
    suspend fun subirFoto(base64: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = executeWithRetry {
                val jsonPayload = JSONObject().apply {
                    put("action", "subir_foto")
                    put("datos", base64)
                    if (!currentIdEmpresa.isNullOrBlank()) put("idEmpresa", currentIdEmpresa)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val responseStr = response.body?.string() ?: ""
                    Log.d(TAG, "subirFoto response code ${response.code}")
                    val json = JSONObject(responseStr)
                    if (json.optString("status") == "success") {
                        val url = json.optJSONObject("data")?.optString("url").orEmpty()
                        if (url.isNotBlank()) APPS_SCRIPT_WEB_APP_URL + url else ""
                    } else ""
                }
            }
            result?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo foto: ${e.message}", e)
            null
        }
    }

    /**
     * Carga masiva de inventario desde CSV/TXT. El backend autogenera Id_Producto.
     * Devuelve el JSONObject de respuesta (status, data.insertados, data.errores).
     */
    suspend fun importarInventario(companyCode: String, csv: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            executeWithRetry {
                val jsonPayload = JSONObject().apply {
                    put("action", "importar_inventario")
                    put("sheetName", companyCode)
                    put("csv", csv)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val str = response.body?.string() ?: ""
                    JSONObject(str)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importarInventario: ${e.message}", e)
            null
        }
    }

    /**
     * Eliminación de usuario en Apps Script con action "eliminar_usuario".
     * Payload: {"action": "eliminar_usuario", "empresaNombre": "NOMBRE_DE_LA_EMPRESA", "userEmail": "EMAIL_DEL_USUARIO"}
     */
    suspend fun eliminarProducto(sheetName: String, nombre: String): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                put("action", "eliminar_producto")
                put("sheetName", sheetName)
                put("producto", nombre)
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                val ok = try {
                    val json = JSONObject(responseStr)
                    json.optString("status").equals("success", ignoreCase = true) ||
                            json.optBoolean("success", false) ||
                            json.optString("message", "").lowercase().contains("eliminad")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("eliminad") || responseStr.lowercase().contains("success")
                }
                response.isSuccessful && (ok || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun eliminarUsuario(empresaNombre: String, userEmail: String): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                put("action", "eliminar_usuario")
                put("empresaNombre", empresaNombre)
                put("userEmail", userEmail)
                put("sheetName", empresaNombre)
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "eliminarUsuario response: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    val resultStr = json.optString("result")
                    val message = json.optString("message").lowercase()
                    status.equals("success", ignoreCase = true) ||
                            isSuccess ||
                            resultStr.equals("success", ignoreCase = true) ||
                            message.contains("exito") ||
                            message.contains("eliminad")
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success") || responseStr.lowercase().contains("eliminad")
                }
                response.isSuccessful && (isSuccessJson || responseStr.isBlank())
            }
        }
        result ?: false
    }

    suspend fun resetPassword(empresaNombre: String, userEmail: String, newPass: String): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                put("action", "actualizar_contrasena")
                put("empresaNombre", empresaNombre)
                put("userEmail", userEmail)
                put("sheetName", empresaNombre)
                put("clave", newPass)
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "resetPassword response: $responseStr")
                val isSuccessJson = try {
                    val json = JSONObject(responseStr)
                    val status = json.optString("status")
                    val isSuccess = json.optBoolean("success", false)
                    status.equals("success", ignoreCase = true) || isSuccess
                } catch (_: Exception) {
                    responseStr.lowercase().contains("success")
                }
                response.isSuccessful && isSuccessJson
            }
        }
        result ?: false
    }

    /**
      * Solicitud de Reportes en Google Apps Script con action "reportes".
     * Payload: {"action": "reportes", "idEmpresa": idEmpresa, "tipo": tipoReporte}
     */
    suspend fun solicitarReporte(
        idEmpresa: String,
        tipoReporte: String = "general",
        extraParams: Map<String, String> = emptyMap()
    ): String? = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val jsonPayload = JSONObject().apply {
                put("action", "reportes")
                put("idEmpresa", idEmpresa)
                put("tipo", tipoReporte)
                extraParams.forEach { (k, v) ->
                    put(k, v)
                }
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw RateLimitException("429 Rate Limit")
                val responseStr = response.body?.string() ?: ""
                Log.d(TAG, "solicitarReporte response code ${response.code}: $responseStr")
                if (response.isSuccessful) responseStr else null
            }
        }
        result
    }

    /**
     * Escritura de datos en Google Sheets (POST/PUT via Sheets API Append o Web App Script).
     */
    suspend fun writeBusinessData(
        sheetName: String,
        values: List<List<Any>>
    ): Boolean = withContext(Dispatchers.IO) {
        val result = executeWithRetry {
            val apiKey = getApiKey()
            val encodedRange = URLEncoder.encode("$sheetName!A:Z", "UTF-8").replace("+", "%20")

            if (webAppScriptUrl != null && webAppScriptUrl.isNotBlank()) {
                // Post via Google Apps Script Web App
                val jsonPayload = JSONObject().apply {
                    put("action", "append")
                    put("sheetName", sheetName)
                    put("values", JSONArray(values.map { JSONArray(it) }))
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(webAppScriptUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    response.isSuccessful
                }
            } else {
                // Direct Sheets API Append
                val url = "$BASE_URL/$spreadsheetId/values/$encodedRange:append?valueInputOption=USER_ENTERED&key=$apiKey"
                val jsonPayload = JSONObject().apply {
                    put("range", "$sheetName!A:Z")
                    put("majorDimension", "ROWS")
                    put("values", JSONArray(values.map { JSONArray(it) }))
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    response.isSuccessful
                }
            }
        }
        result ?: false
    }

    /**
     * 2. Actualización Optimista (Optimistic UI) + Escritura en segundo plano
     */
    suspend fun writeBusinessDataOptimistic(
        sheetName: String,
        tableName: String,
        values: List<List<Any>>,
        onOptimisticLocalUpdate: suspend () -> Unit
    ): Boolean {
        // 1. Ejecución local instantánea
        onOptimisticLocalUpdate()

        // 2. Persistencia remota en segundo plano con reintentos
        return writeBusinessData(sheetName, values)
    }

    /**
     * 4. Lógica de reintentos (Retry logic) con Backoff Exponencial (Soporte Error 429)
     */
    /**
     * Compra/renovación de plan: action "comprar_plan".
     * Retorna la fechaVencimiento ISO entregada por el backend, o null si falló.
     */
    suspend fun comprarPlan(
        codigo: String,
        plan: String,
        tiempo: String,
        monto: String,
        usuario: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val result = executeWithRetry {
                val jsonPayload = JSONObject().apply {
                    put("action", "comprar_plan")
                    put("codigo", codigo)
                    put("plan", plan)
                    put("tiempo", tiempo)
                    put("monto", monto)
                    put("usuario", usuario)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder().url(targetUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val responseStr = response.body?.string() ?: ""
                    Log.d(TAG, "comprarPlan response code ${response.code}: $responseStr")
                    val json = JSONObject(responseStr)
                    if (json.optString("status") == "success") {
                        json.optJSONObject("data")?.optString("fechaVencimiento").orEmpty()
                    } else null
                }
            }
            result?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Error en comprarPlan para '$codigo': ${e.message}", e)
            null
        }
    }

    /**
     * Finanzas de KAPTA IA (ingresos por planes + gastos propios): action "listar_finanzas_kapta".
     */
    suspend fun listarFinanzasKapta(): FinanzasKaptaResumen? = withContext(Dispatchers.IO) {
        try {
            val result = executeWithRetry {
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val requestUrl = if (targetUrl.contains("?")) "$targetUrl&action=listar_finanzas_kapta" else "$targetUrl?action=listar_finanzas_kapta"
                val request = Request.Builder().url(requestUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val responseStr = response.body?.string() ?: ""
                    val json = JSONObject(responseStr)
                    if (json.optString("status") != "success") return@use null
                    val data = json.optJSONObject("data") ?: return@use null
                    val registros = mutableListOf<FinanzaKapta>()
                    val arr = data.optJSONArray("registros")
                    for (i in 0 until (arr?.length() ?: 0)) {
                        val item = arr!!.getJSONObject(i)
                        registros.add(
                            FinanzaKapta(
                                id = item.optInt("id"),
                                fecha = item.optString("fecha"),
                                tipo = item.optString("tipo"),
                                categoria = item.optString("categoria"),
                                concepto = item.optString("concepto"),
                                monto = item.optString("monto").toDoubleOrNull() ?: 0.0,
                                metodoPago = item.optString("metodo_pago"),
                                referencia = item.optString("referencia"),
                                usuario = item.optString("usuario")
                            )
                        )
                    }
                    FinanzasKaptaResumen(
                        registros = registros,
                        totalIngresos = data.optDouble("totalIngresos", 0.0),
                        totalEgresos = data.optDouble("totalEgresos", 0.0),
                        balance = data.optDouble("balance", 0.0)
                    )
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error en listarFinanzasKapta: ${e.message}", e)
            null
        }
    }

    /**
     * Registro de ingreso/gasto propio de KAPTA: action "registrar_finanza_kapta".
     */
    suspend fun registrarFinanzaKapta(
        tipo: String,
        concepto: String,
        monto: String,
        categoria: String = "",
        metodoPago: String = "",
        referencia: String = "",
        usuario: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = executeWithRetry {
                val jsonPayload = JSONObject().apply {
                    put("action", "registrar_finanza_kapta")
                    put("tipo", tipo)
                    put("concepto", concepto)
                    put("monto", monto)
                    put("categoria", categoria)
                    put("metodoPago", metodoPago)
                    put("referencia", referencia)
                    put("usuario", usuario)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val targetUrl: String = if (!webAppScriptUrl.isNullOrBlank()) webAppScriptUrl!! else APPS_SCRIPT_WEB_APP_URL
                val request = Request.Builder().url(targetUrl).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) throw RateLimitException("429 Rate Limit")
                    val responseStr = response.body?.string() ?: ""
                    Log.d(TAG, "registrarFinanzaKapta response code ${response.code}: $responseStr")
                    JSONObject(responseStr).optString("status") == "success"
                }
            }
            result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error en registrarFinanzaKapta: ${e.message}", e)
            false
        }
    }

    private suspend fun <T> executeWithRetry(        maxAttempts: Int = 4,
        initialDelayMs: Long = 1000L,
        block: suspend () -> T?
    ): T? {
        var currentDelay = initialDelayMs
        for (attempt in 1..maxAttempts) {
            try {
                if (attempt > 1) {
                    _syncState.value = SyncState.Syncing(
                        message = "Sincronizando...",
                        isRetrying = true,
                        attempt = attempt
                    )
                } else {
                    _syncState.value = SyncState.Syncing(message = "Sincronizando...", isRetrying = false, attempt = 1)
                }

                val res = block()
                _syncState.value = SyncState.Success(System.currentTimeMillis())
                return res
            } catch (e: RateLimitException) {
                Log.w(TAG, "Attempt $attempt failed due to rate limit 429: ${e.message}")
                if (attempt == maxAttempts) {
                    _syncState.value = SyncState.Error("Límite de cuota alcanzado. Reintentando...")
                    return null
                }
                _syncState.value = SyncState.Syncing(
                    message = "Sincronizando... (Pausado por cuota, reintentando en ${currentDelay / 1000}s)",
                    isRetrying = true,
                    attempt = attempt
                )
                delay(currentDelay)
                currentDelay *= 2
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Attempt $attempt failed: ${e.localizedMessage}")
                if (attempt == maxAttempts) {
                    _syncState.value = SyncState.Error("Error de sincronización con Google Sheets")
                    return null
                }
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        _syncState.value = SyncState.Idle
        return null
    }

    /**
     * Asegura que el arreglo 'data' sea plano (una sola dimensión List<Any>)
     * y que la cantidad de elementos coincida exactamente con las columnas esperadas
     * de cada tabla según el rango de destino:
     * - Inventario: 6 columnas (A:F)
     * - Ventas: 6 columnas (H:M)
     * - Deudores: 6 columnas (O:T)
     * - Gastos: 4 columnas (AA:AD)
     */
    private fun flattenAndNormalizeData(
        rawData: List<Any>,
        tableName: String? = null,
        action: String? = null
    ): List<Any> {
        val flatList = mutableListOf<Any>()

        fun extract(item: Any?) {
            when (item) {
                null -> {}
                is Iterable<*> -> item.forEach { extract(it) }
                is Array<*> -> item.forEach { extract(it) }
                is JSONArray -> {
                    for (i in 0 until item.length()) {
                        extract(item.opt(i))
                    }
                }
                else -> flatList.add(item)
            }
        }

        rawData.forEach { extract(it) }

        val targetKey = "${action ?: ""} ${tableName ?: ""}".lowercase()
        val expectedColumns = when {
            targetKey.contains("inventario") -> 13 // A:M (Col 1 a 13)
            targetKey.contains("venta") -> 21      // N:AH (Col 14 a 34)
            targetKey.contains("deudor") -> 8     // AJ:AQ (Col 36 a 43)
            targetKey.contains("gasto") -> 14     // AS:BF (Col 45 a 58)
            targetKey.contains("usuario") -> 11   // BQ:CA (Col 69 a 79)
            else -> 0
        }

        if (expectedColumns <= 0 || flatList.isEmpty()) {
            return flatList
        }

        return when {
            flatList.size == expectedColumns -> flatList
            flatList.size > expectedColumns -> flatList.take(expectedColumns)
            else -> {
                val padded = flatList.toMutableList()
                while (padded.size < expectedColumns) {
                    padded.add("")
                }
                padded
            }
        }
    }

    class RateLimitException(message: String) : Exception(message)
}


object DiagStorage {
    var lastDiagnostic: String? = null
}
