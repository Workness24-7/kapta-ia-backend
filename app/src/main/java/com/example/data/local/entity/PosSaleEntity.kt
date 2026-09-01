package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos_sales")
data class PosSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyCode: String,
    val clientName: String = "Cliente General",
    val productName: String = "Producto Varios",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String,
    val transferAmount: Double = 0.0,
    val cashAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val itemCount: Int = 1,
    val isSynced: Boolean = false,
    val tipoVenta: String = "Normal"
)

