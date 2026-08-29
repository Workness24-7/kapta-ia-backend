package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_users")
data class CompanyUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: Int,
    val companyCode: String,
    val name: String,
    val role: String,
    val email: String,
    val username: String,
    val password: String,
    val status: String = "Activo",
    val creationDate: String = "12/05/2024",
    val lastAccess: String = "Hoy, 8:35 a. m.",
    val idUsuario: String = "",
    val fechaCambioEstado: String = "",
    val motivoCambio: String = "",
    val cambiadoPor: String = "",
    val isSynced: Boolean = false,
    val assignedFunctionsJson: String = ""
)
