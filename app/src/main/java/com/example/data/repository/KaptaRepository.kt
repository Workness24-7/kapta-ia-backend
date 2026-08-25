package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.KaptaDao
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.data.local.entity.FinancialTransactionEntity
import com.example.data.local.entity.PosProductEntity
import com.example.data.local.entity.PosSaleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class KaptaRepository(private val dao: KaptaDao) {
    val allCompanies: Flow<List<CompanyEntity>> = dao.getAllCompanies()
    val allUsers: Flow<List<CompanyUserEntity>> = dao.getAllUsers()

    suspend fun getCompanyById(id: Int): CompanyEntity? = dao.getCompanyById(id)
    suspend fun getCompanyByCode(code: String): CompanyEntity? = dao.getCompanyByCode(code)
    
    suspend fun insertCompany(company: CompanyEntity): Long = dao.insertCompany(company)
    suspend fun updateCompany(company: CompanyEntity) = dao.updateCompany(company)
    suspend fun deleteCompany(id: Int) = dao.deleteCompany(id)

    fun getUsersByCompany(companyId: Int): Flow<List<CompanyUserEntity>> = dao.getUsersByCompany(companyId)
    fun getUsersByCompanyCode(code: String): Flow<List<CompanyUserEntity>> = dao.getUsersByCompanyCode(code)
    fun getUsersByCompanyCodeAndId(code: String, companyId: Int): Flow<List<CompanyUserEntity>> = dao.getUsersByCompanyCodeAndId(code, companyId)
    suspend fun insertUser(user: CompanyUserEntity) = dao.insertUser(user)
    suspend fun updateUser(user: CompanyUserEntity) = dao.updateUser(user)
    suspend fun deleteUser(id: Int) = dao.deleteUser(id)

    suspend fun getUnsyncedUsers(): List<CompanyUserEntity> = dao.getUnsyncedUsers()
    suspend fun markUserSynced(id: Int) = dao.markUserSynced(id)

    fun getProductsByCompany(companyCode: String): Flow<List<PosProductEntity>> = dao.getProductsByCompany(companyCode)
    suspend fun insertProduct(product: PosProductEntity) = dao.insertProduct(product)
    suspend fun updateProduct(product: PosProductEntity) = dao.updateProduct(product)
    suspend fun deleteProduct(id: Int) = dao.deleteProduct(id)

    suspend fun getProductById(id: Int): PosProductEntity? = dao.getProductById(id)

    suspend fun getUnsyncedProducts(): List<PosProductEntity> = dao.getUnsyncedProducts()
    suspend fun markProductSynced(id: Int) = dao.markProductSynced(id)

    fun getSalesByCompany(companyCode: String): Flow<List<PosSaleEntity>> {
        if (companyCode.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        android.util.Log.d("KAPTA_DIAG_DAO", "DAO query execute getSalesByCompany: companyCode=$companyCode")
        return dao.getSalesByCompany(companyCode).onEach { sales ->
            val v00001 = sales.find { it.clientName.contains("Juan", true) || it.productName.contains("coca", true) }
            val sumTotal = sales.sumOf { it.totalAmount }
            android.util.Log.d("KAPTA_DIAG_ROOM", "ROOM pos_sales fetched: count=${sales.size}, V-00001 totalAmount=${v00001?.totalAmount ?: 0.0}, sum=$sumTotal")
            android.util.Log.d("KAPTA_DIAG_REPOSITORY", "REPOSITORY sales flow emitted: count=${sales.size}, sum=$sumTotal")
        }
    }
    suspend fun insertSale(sale: PosSaleEntity) = dao.insertSale(sale)

    suspend fun getUnsyncedSales(): List<PosSaleEntity> = dao.getUnsyncedSales()
    suspend fun markSaleSynced(id: Int) = dao.markSaleSynced(id)

    fun getTransactionsByCompany(companyCode: String): Flow<List<FinancialTransactionEntity>> = dao.getTransactionsByCompany(companyCode)
    suspend fun insertTransaction(transaction: FinancialTransactionEntity) = dao.insertTransaction(transaction)
    suspend fun getUnsyncedTransactions(): List<FinancialTransactionEntity> = dao.getUnsyncedTransactions()
    suspend fun markTransactionSynced(id: Int) = dao.markTransactionSynced(id)

    // ponytail: sin fecha válida se asume membresía activa lejana (10 años)
    private fun diasHastaVencimiento(fechaIso: String): Int {
        if (fechaIso.isBlank()) return 3650
        return runCatching {
            val f = java.time.LocalDate.parse(fechaIso.substring(0, 10))
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), f).toInt()
        }.getOrDefault(3650)
    }

    suspend fun syncCompaniesFromSheets(sheetsService: com.example.data.remote.SheetsDatabaseService): List<CompanyEntity> {        try {
            val list = sheetsService.listarEmpresas()
            if (list.isEmpty()) {
                Log.w("KaptaRepository", "Lista remota de empresas vacía desde listarEmpresas(). Conservando datos locales en Room.")
                return dao.getAllCompaniesSync()
            }

            for (rComp in list) {
                val codeToUse = rComp.codigo.ifBlank { rComp.idEmpresa }
                if (codeToUse.isBlank()) continue

                val dias = diasHastaVencimiento(rComp.fechaVencimiento)
                val existing = dao.getCompanyByCode(codeToUse)
                if (existing == null) {
                    val newCompany = CompanyEntity(
                        code = codeToUse,
                        name = rComp.nombre.ifBlank { codeToUse },
                        country = rComp.pais.ifBlank { "Colombia" },
                        adminEmail = rComp.correoAdmin.ifBlank { "admin@$codeToUse.com" },
                        status = rComp.estado.ifBlank { "Activo" },
                        plan = rComp.plan.ifBlank { "Básico" },
                        durationTime = rComp.tiempo.ifBlank { "Mensual" },
                        expirationDays = dias
                    )
                    dao.insertCompany(newCompany)
                } else {
                    val updatedCompany = existing.copy(
                        name = if (rComp.nombre.isNotBlank()) rComp.nombre else existing.name,
                        country = if (rComp.pais.isNotBlank()) rComp.pais else existing.country,
                        adminEmail = if (rComp.correoAdmin.isNotBlank()) rComp.correoAdmin else existing.adminEmail,
                        status = if (rComp.estado.isNotBlank()) rComp.estado else existing.status,
                        plan = if (rComp.plan.isNotBlank()) rComp.plan else existing.plan,
                        durationTime = if (rComp.tiempo.isNotBlank()) rComp.tiempo else existing.durationTime,
                        expirationDays = dias
                    )
                    dao.updateCompany(updatedCompany)
                }
            }
            // Room es caché del backend: borrar locales que ya no existen en el remoto
            val remoteCodes = list.mapNotNull { r ->
                (r.codigo.ifBlank { r.idEmpresa }).takeIf { it.isNotBlank() }
            }.map { it.lowercase() }.toSet()
            dao.getAllCompaniesSync().filter { it.code.lowercase() !in remoteCodes }.forEach { c ->
                dao.deleteCompany(c.id)
                dao.deleteUsersByCompanyCode(c.code)
            }
            // usuarios huérfanos (empresas que ya no existen en el backend, ej. era Google Sheets)
            dao.getAllUsersSync().filter {
                it.companyCode.isNotBlank() && it.companyCode.lowercase() !in remoteCodes
            }.forEach { dao.deleteUser(it.id) }

            Log.d("KaptaRepository", "Sincronizadas ${list.size} empresas exitosamente en Room desde listarEmpresas.")
            return dao.getAllCompaniesSync()
        } catch (e: Exception) {
            Log.e("KaptaRepository", "Error durante la sincronización de empresas: ${e.message}", e)
            return dao.getAllCompaniesSync()
        }
    }

    suspend fun solicitarReporte(
        sheetsService: com.example.data.remote.SheetsDatabaseService,
        idEmpresa: String,
        tipoReporte: String = "general",
        extraParams: Map<String, String> = emptyMap()
    ): String? {
        return sheetsService.solicitarReporte(idEmpresa, tipoReporte, extraParams)
    }
}
