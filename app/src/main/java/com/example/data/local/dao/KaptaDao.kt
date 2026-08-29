package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.CompanyUserEntity
import com.example.data.local.entity.FinancialTransactionEntity
import com.example.data.local.entity.IaFunctionEntity
import com.example.data.local.entity.PosProductEntity
import com.example.data.local.entity.PosSaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KaptaDao {
    @Query("SELECT * FROM companies ORDER BY id DESC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies ORDER BY id DESC")
    suspend fun getAllCompaniesSync(): List<CompanyEntity>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyById(id: Int): CompanyEntity?

    @Query("SELECT * FROM companies WHERE LOWER(code) = LOWER(:code) LIMIT 1")
    suspend fun getCompanyByCode(code: String): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Update
    suspend fun updateCompany(company: CompanyEntity)

    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteCompany(id: Int)

    @Query("DELETE FROM companies")
    suspend fun deleteAllCompanies()

    // Users
    @Query("SELECT * FROM company_users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<CompanyUserEntity>>

    @Query("SELECT * FROM company_users")
    suspend fun getAllUsersSync(): List<CompanyUserEntity>

    @Query("DELETE FROM company_users WHERE LOWER(companyCode) = LOWER(:companyCode)")
    suspend fun deleteUsersByCompanyCode(companyCode: String)

    @Query("SELECT * FROM company_users WHERE companyId = :companyId")
    fun getUsersByCompany(companyId: Int): Flow<List<CompanyUserEntity>>

    @Query("SELECT * FROM company_users WHERE companyId = :companyId AND role = 'Administrador' LIMIT 1")
    suspend fun getAdminUserByCompanyId(companyId: Int): CompanyUserEntity?

    @Query("SELECT * FROM company_users WHERE LOWER(companyCode) = LOWER(:companyCode) OR (companyId = :companyId AND companyId > 0) ORDER BY id ASC")
    fun getUsersByCompanyCodeAndId(companyCode: String, companyId: Int): Flow<List<CompanyUserEntity>>

    @Query("SELECT * FROM company_users WHERE LOWER(companyCode) = LOWER(:companyCode) ORDER BY id ASC")
    fun getUsersByCompanyCode(companyCode: String): Flow<List<CompanyUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CompanyUserEntity)

    @Update
    suspend fun updateUser(user: CompanyUserEntity)

    @Query("DELETE FROM company_users WHERE id = :id")
    suspend fun deleteUser(id: Int)

    @Query("SELECT * FROM company_users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<CompanyUserEntity>

    @Query("UPDATE company_users SET isSynced = 1 WHERE id = :id")
    suspend fun markUserSynced(id: Int)

    // POS Products
    @Query("SELECT * FROM pos_products WHERE :companyCode != '' AND LOWER(companyCode) = LOWER(:companyCode)")
    fun getProductsByCompany(companyCode: String): Flow<List<PosProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: PosProductEntity)

    @Update
    suspend fun updateProduct(product: PosProductEntity)

    @Query("DELETE FROM pos_products WHERE id = :id")
    suspend fun deleteProduct(id: Int)

    @Query("SELECT * FROM pos_products WHERE id = :id")
    suspend fun getProductById(id: Int): PosProductEntity?

    @Query("SELECT * FROM pos_products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<PosProductEntity>

    @Query("UPDATE pos_products SET isSynced = 1 WHERE id = :id")
    suspend fun markProductSynced(id: Int)

    // POS Sales
    @Query("SELECT * FROM pos_sales WHERE :companyCode != '' AND (LOWER(companyCode) = LOWER(:companyCode) OR (LENGTH(:companyCode) >= 3 AND LOWER(companyCode) LIKE '%' || LOWER(:companyCode) || '%')) ORDER BY timestamp DESC")
    fun getSalesByCompany(companyCode: String): Flow<List<PosSaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: PosSaleEntity)

    @Query("SELECT * FROM pos_sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<PosSaleEntity>

    @Query("UPDATE pos_sales SET isSynced = 1 WHERE id = :id")
    suspend fun markSaleSynced(id: Int)

    // Financial Transactions (Expenses/Income)
    @Query("SELECT * FROM financial_transactions WHERE :companyCode != '' AND (LOWER(companyCode) = LOWER(:companyCode) OR (LENGTH(:companyCode) >= 3 AND LOWER(companyCode) LIKE '%' || LOWER(:companyCode) || '%')) ORDER BY id DESC")
    fun getTransactionsByCompany(companyCode: String): Flow<List<FinancialTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransactionEntity)

    @Query("SELECT * FROM financial_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<FinancialTransactionEntity>

    @Query("UPDATE financial_transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markTransactionSynced(id: Int)

    // IA Functions Library (global, owned by SuperAdmin)
    @Query("SELECT * FROM ia_functions ORDER BY id ASC")
    fun getAllIaFunctions(): Flow<List<IaFunctionEntity>>

    @Query("SELECT * FROM ia_functions")
    suspend fun getAllIaFunctionsSync(): List<IaFunctionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIaFunction(f: IaFunctionEntity)

    @Query("DELETE FROM ia_functions WHERE nombre = :nombre")
    suspend fun deleteIaFunctionByNombre(nombre: String)

    @Query("SELECT * FROM ia_functions WHERE nombre = :nombre LIMIT 1")
    suspend fun getIaFunctionByNombre(nombre: String): IaFunctionEntity?
}
