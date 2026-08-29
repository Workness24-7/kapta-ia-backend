package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ia_functions", indices = [Index(value = ["nombre"], unique = true)])
data class IaFunctionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val rol: String = "",
    val planTier: String = "Basico",
    val tipoNegocio: String = "",
    val modulo: String = "",
    val creadoPor: String = "SuperAdmin"
)

/**
 * Funcion de la biblioteca global, enriquecida con plan, tipo de negocio y modulo (plantilla reusable).
 * planTier: Basico | Premium | MaxIA. modulo: ventas|gastos|deudores|inventario|facturacion|reporte|caja|usuario|cliente|custom
 */
data class FuncionLib(
    val nombre: String,
    val descripcion: String = "",
    val rol: String = "",
    val planTier: String = "Basico",
    val tipoNegocio: String = "",
    val modulo: String = ""
)
