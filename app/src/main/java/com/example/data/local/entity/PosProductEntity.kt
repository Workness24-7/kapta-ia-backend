package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos_products")
data class PosProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyCode: String,
    val name: String,
    val aliases: String = "",
    val price: Double,
    val costPrice: Double = 0.0,
    val hasMinPrice: Boolean = false,
    val minPrice: Double = 0.0,
    val category: String,
    val stock: Int = 10,
    val minStockAlert: Int = 10,
    val imageUrl: String = "",
    val barcode: String = "",
    val isSynced: Boolean = false
)
