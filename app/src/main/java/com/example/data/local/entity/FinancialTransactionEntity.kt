package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_transactions")
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyCode: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dateString: String,
    val isExpense: Boolean,
    val category: String = "",
    val isSynced: Boolean = false,
    val stockAnterior: Int = -1,
    val stockNuevo: Int = -1,
    val usuario: String = ""
)
