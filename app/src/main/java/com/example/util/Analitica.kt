package com.example.util

import android.app.Activity
import android.content.Context

/**
 * Analítica básica local (sin SDK externo): contadores de eventos y logros
 * en SharedPreferences. Sirve para disparar review, upsell y diagnósticos.
 */
object Analitica {
    private fun prefs(context: Context) =
        context.getSharedPreferences("kapta_analitica", Context.MODE_PRIVATE)

    fun evento(context: Context, nombre: String) {
        val p = prefs(context)
        p.edit()
            .putInt("ev_$nombre", p.getInt("ev_$nombre", 0) + 1)
            .putInt("ev_logro", p.getInt("ev_logro", 0) + 1)
            .putLong("ultimo_uso", System.currentTimeMillis())
            .apply()
    }

    fun conteo(context: Context, nombre: String): Int =
        prefs(context).getInt("ev_$nombre", 0)

    fun logros(context: Context): Int = conteo(context, "logro")

    private fun haceDias(context: Context, clave: String, dias: Long): Boolean {
        val ultimo = prefs(context).getLong(clave, 0L)
        return System.currentTimeMillis() - ultimo > dias * 24 * 60 * 60 * 1000L
    }

    private fun marcar(context: Context, clave: String) {
        prefs(context).edit().putLong(clave, System.currentTimeMillis()).apply()
    }

    /**
     * Pide reseña en Play después de un logro (3+ logros, máx 1 vez cada 90 días).
     * Llamar desde la UI con el Activity visible.
     */
    fun pedirReviewSiCorresponde(activity: Activity) {
        try {
            val ctx = activity.applicationContext
            if (logros(ctx) < 3) return
            if (!haceDias(ctx, "review_fecha", 90)) return
            val manager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        manager.launchReviewFlow(activity, task.result)
                            .addOnCompleteListener { marcar(ctx, "review_fecha") }
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    /** Upsell post-valor: plan Básico con 5+ logros, máx 1 vez cada 7 días. */
    fun debeMostrarUpsell(context: Context, esPlanBasico: Boolean): Boolean {
        if (!esPlanBasico) return false
        if (logros(context) < 5) return false
        if (!haceDias(context, "upsell_fecha", 7)) return false
        marcar(context, "upsell_fecha")
        return true
    }
}
