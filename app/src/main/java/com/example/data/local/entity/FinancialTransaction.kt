package com.example.data.local.entity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinancialTransaction(
    val id: Int,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dateString: String, // format: "dd/MM/yyyy"
    val isExpense: Boolean,
    val category: String = ""
) {
    fun parseDate(): Date? {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.isLenient = false
            format.parse(dateString)
        } catch (_: Exception) {
            try {
                val formatIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatIso.isLenient = false
                formatIso.parse(dateString)
            } catch (_: Exception) {
                null
            }
        }
    }
}
