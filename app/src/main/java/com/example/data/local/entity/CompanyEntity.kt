package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val name: String,
    val logoUrl: String = "",
    val listIconUrl: String = "",
    val status: String = "Activo",
    val plan: String = "Premium",
    val durationTime: String = "Mensual",
    val nit: String = "",
    val country: String = "Colombia",
    val city: String = "Cali, Valle del Cauca",
    val address: String = "Carrera 1 # 64-60",
    val phone: String = "(602) 485 1414",
    val phone2: String = "",
    val email: String = "admin@la14.com.co",
    val creationDate: String = "12/05/2024",
    val lastAccess: String = "Hoy, 8:35 a. m.",
    val adminName: String = "Carlos Pérez",
    val adminEmail: String = "admin@la14.com.co",
    val businessType: String = "Supermercado",
    val expirationDays: Int = 28,
    val notes: String = "Empresa principal de cadena de supermercados con 4 sucursales activas en el Valle del Cauca.",
    val selectedModulesJson: String = "[\"Inventario\", \"Registrar venta\", \"Cuentas por cobrar\", \"Registrar pago recurrente\", \"Pago administrativo\", \"Módulo de reportes\", \"Hacer inventario\"]",
    val customFunctionsJson: String = "[]",
    val primaryColorHex: String = "#4F46E5",
    val secondaryColorHex: String = "#3B82F6",
    val tertiaryColorHex: String = "#10B981",
    val neutralColorHex: String = "#0F172A",
    val fontType: String = "Inter"
) {
    fun getEffectiveStatus(): String {
        if (status.equals("Suspendido", ignoreCase = true) || status.contains("Retirad", ignoreCase = true) || status.contains("Inactiv", ignoreCase = true)) {
            return "Suspendido"
        }
        if (expirationDays in 0..5 || status.contains("Vencer", ignoreCase = true)) {
            return "Por vencer"
        }
        if (expirationDays < 0) {
            return "Vencido"
        }
        return status
    }

    // Suspendidas no cuentan dias restantes (no pagaron) sino dias de mora desde fecha_vencimiento
    fun diasEnVencido(): Int = maxOf(0, -expirationDays)
}
