package com.example.util

import java.text.NumberFormat
import java.text.Normalizer
import java.util.Locale

/**
 * Moneda del negocio: se toma el PAIS de la tabla config_negocio y de ahí
 * sale el símbolo/formato. Default Colombia ("$") si no hay país configurado.
 */
object KaptaCurrency {
    @Volatile
    var pais: String = ""

    private val paises = mapOf(
        "colombia" to "CO", "mexico" to "MX", "argentina" to "AR",
        "chile" to "CL", "peru" to "PE", "ecuador" to "EC",
        "uruguay" to "UY", "bolivia" to "BO", "venezuela" to "VE",
        "paraguay" to "PY", "panama" to "PA", "costa rica" to "CR",
        "guatemala" to "GT", "honduras" to "HN", "el salvador" to "SV",
        "nicaragua" to "NI", "republica dominicana" to "DO", "cuba" to "CU", "espana" to "ES", "estados unidos" to "US",
        "estadosunidos" to "US", "usa" to "US", "eeuu" to "US"
    )

    fun locale(): Locale {
        val codigo = paises[normalizar(pais)] ?: "CO"
        return if (codigo == "US") Locale("en", "US") else Locale("es", codigo)
    }

    private fun normalizar(s: String): String {
        val n = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
        return Regex("[\\p{InCombiningDiacriticalMarks}]").replace(n, "")
    }

    fun format(amount: Double): String {
        val nf = NumberFormat.getCurrencyInstance(locale())
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        return nf.format(amount)
    }
}
