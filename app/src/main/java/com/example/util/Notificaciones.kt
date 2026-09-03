package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import java.util.Calendar

/**
 * Notificaciones de reenganche: canal, alertas contextuales (stock bajo,
 * membresía por vencer) y recordatorio diario aunque la app esté cerrada.
 */
object Notificaciones {
    const val CANAL = "kapta_recordatorios"
    private const val PREFS = "kapta_notif"

    fun crearCanal(context: Context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CANAL) != null) return
            mgr.createNotificationChannel(
                NotificationChannel(CANAL, "Recordatorios del negocio", NotificationManager.IMPORTANCE_DEFAULT)
            )
        } catch (_: Exception) { }
    }

    private fun haceHoras(context: Context, clave: String, horas: Long): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return System.currentTimeMillis() - p.getLong(clave, 0L) > horas * 60 * 60 * 1000L
    }

    private fun marcar(context: Context, clave: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(clave, System.currentTimeMillis()).apply()
    }

    private fun mostrar(context: Context, id: Int, titulo: String, texto: String) {
        try {
            crearCanal(context)
            val apertura = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val notif = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setAutoCancel(true)
                .setContentIntent(apertura)
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notif)
        } catch (_: Exception) { }
    }

    fun alertarStockBajo(context: Context, cantidad: Int) {
        if (cantidad <= 0 || !haceHoras(context, "stock_fecha", 24)) return
        marcar(context, "stock_fecha")
        mostrar(context, 11, "Stock bajo en tu negocio", "Tienes $cantidad producto(s) que requieren atención. Ábrelos en Alertas de Stock.")
    }

    fun alertarMembresia(context: Context, dias: Int) {
        if (dias !in 1..5) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getInt("memb_dias", -1) == dias) return
        p.edit().putInt("memb_dias", dias).apply()
        mostrar(context, 12, "Membresía por vencer", "Tu acceso total se suspende en $dias ${if (dias == 1) "día" else "días"}. Renueva a tiempo.")
    }

    /** Recordatorio diario 9am aunque la app esté cerrada (alarma inexacta, sin permisos especiales). */
    fun programarRecordatorioDiario(context: Context) {
        try {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (p.getBoolean("recordatorio_ok", false)) return
            val intent = Intent(context, RecordatorioReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 21, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val proxima = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }.timeInMillis
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .setInexactRepeating(AlarmManager.RTC_WAKEUP, proxima, AlarmManager.INTERVAL_DAY, pi)
            p.edit().putBoolean("recordatorio_ok", true).apply()
        } catch (_: Exception) { }
    }
}

/** Dispara el recordatorio diario de reenganche. */
class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Notificaciones.crearCanal(context)
            val apertura = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
                PendingIntent.getActivity(context, 22, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val notif = NotificationCompat.Builder(context, Notificaciones.CANAL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("¿Cómo va tu negocio hoy?")
                .setContentText("Revisa tus ventas, inventario y cuentas por cobrar en Kapta IA.")
                .setAutoCancel(true)
                .setContentIntent(apertura)
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(22, notif)
        } catch (_: Exception) { }
    }
}
