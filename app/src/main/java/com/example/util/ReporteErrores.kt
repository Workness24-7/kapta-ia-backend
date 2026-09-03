package com.example.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Reporte temprano de errores: guarda el stacktrace de cualquier crash no
 * capturado y permite ofrecer su envío como solicitud de soporte al reabrir.
 */
object ReporteErrores {
    private const val ARCHIVO = "ultimo_error.txt"

    fun instalar(context: Context) {
        val previo = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            try {
                File(context.cacheDir, ARCHIVO)
                    .writeText(Log.getStackTraceString(error).take(6000))
            } catch (_: Exception) { }
            previo?.uncaughtException(hilo, error)
        }
    }

    fun pendiente(context: Context): String? = try {
        val f = File(context.cacheDir, ARCHIVO)
        if (f.exists()) f.readText().ifBlank { null } else null
    } catch (_: Exception) { null }

    fun limpiar(context: Context) {
        try { File(context.cacheDir, ARCHIVO).delete() } catch (_: Exception) { }
    }
}
