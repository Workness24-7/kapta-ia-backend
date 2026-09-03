package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val PRIVACIDAD_RESUMEN = listOf(
    "1. Responsable" to "Tus datos los administra tu negocio; Kapta IA POS solo los guarda y procesa por cuenta del negocio para operar el sistema.",
    "2. Qué recogemos" to "Datos de tu cuenta (nombre, correo, rol y permisos), datos de la operación (ventas, inventario, deudores, gastos) y datos técnicos del equipo para sincronizar entre dispositivos.",
    "3. Para qué se usan" to "Operar el punto de venta, sincronizar tus equipos, darte soporte, prevenir fraudes y cumplir obligaciones legales del negocio.",
    "4. Inteligencia artificial" to "El sistema usa IA solo como ayuda en funciones específicas (asistente, reportes y sugerencias). La IA no toma decisiones legales por sí sola; quien decide es el comerciante.",
    "5. Biometría" to "Si entras con huella o Face ID, esa verificación la hace tu propio teléfono. Nunca recibimos ni guardamos tus datos biométricos.",
    "6. Con quién se comparte" to "Solo con proveedores técnicos indispensables (nube, base de datos e IA), bajo confidencialidad. Nunca vendemos tus datos.",
    "7. Seguridad" to "Conexiones cifradas, contraseñas protegidas con hash, acceso por roles y claves dinámicas para operaciones sensibles.",
    "8. Tus derechos" to "Puedes conocer, corregir, actualizar y pedir la eliminación de tus datos escribiendo al correo de contacto de tu negocio."
)

private val TERMINOS_RESUMEN = listOf(
    "1. El servicio" to "Kapta IA POS es la herramienta de tu negocio para ventas, inventario, deudores y finanzas, según el plan contratado.",
    "2. Tu cuenta" to "Cuida tu usuario y contraseña y no los compartas. Lo que se haga con tus credenciales se presume hecho por ti.",
    "3. Permisos" to "Solo verás las funciones que el administrador te activó. Si necesitas algo más, pídelo al administrador.",
    "4. Uso correcto" to "Usa el sistema solo para la operación lícita del negocio. Está prohibido registrar datos falsos, alterar registros o usar datos de terceros sin su autorización.",
    "5. Datos de clientes" to "Si registras clientes (fiados, mesas), el negocio declara tener su autorización; el sistema solo los guarda por cuenta del negocio.",
    "6. Inteligencia artificial" to "La IA sugiere y ayuda, pero no decide por ti: verifica precios, cierres y decisiones importantes antes de actuar.",
    "7. Disponibilidad" to "Procuramos un servicio continuo, pero puede haber mantenimientos o fallas de internet. Verifica cierres e inventarios físicos.",
    "8. Aceptación" to "Al pulsar Aceptar confirmas que leíste este resumen y aceptas la Política de Privacidad y los Términos de Uso completos."
)

/**
 * Consentimiento legal para usuarios nuevos (o sin registro previo).
 * Muestra el resumen, permite scroll libre, el botón Continuar baja por
 * la lectura y solo al llegar al final aparece Aceptar.
 */@Composable
fun ConsentimientoScreen(
    nombreNegocio: String,
    nombreUsuario: String,
    onAceptar: () -> Unit
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val alFinal by remember {
        derivedStateOf { scroll.maxValue == 0 || scroll.value >= scroll.maxValue - 8 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Bienvenido, $nombreUsuario",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$nombreNegocio • Antes de entrar, lee y acepta las reglas",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Política de Privacidad (resumen)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    PRIVACIDAD_RESUMEN.forEach { (titulo, texto) ->
                        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(texto, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Términos de Uso (resumen)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TERMINOS_RESUMEN.forEach { (titulo, texto) ->
                        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(texto, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (alFinal) {
            Button(
                onClick = onAceptar,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Aceptar y entrar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desliza para leer todo el documento",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        scope.launch {
                            val destino = (scroll.value + 900).coerceAtMost(scroll.maxValue)
                            scroll.animateScrollTo(destino)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Continuar ↓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

/** Registro de aceptación por usuario+negocio (vale una vez; nuevos usuarios siempre la ven). */
fun tieneConsentimiento(context: android.content.Context, companyCode: String, userKey: String): Boolean {
    if (companyCode.isBlank() || userKey.isBlank()) return true
    return context.getSharedPreferences("kapta_consent", android.content.Context.MODE_PRIVATE)
        .getBoolean("ok_${companyCode.uppercase()}_${userKey.lowercase()}", false)
}

fun guardarConsentimiento(context: android.content.Context, companyCode: String, userKey: String) {
    if (companyCode.isBlank() || userKey.isBlank()) return
    context.getSharedPreferences("kapta_consent", android.content.Context.MODE_PRIVATE)
        .edit()
        .putBoolean("ok_${companyCode.uppercase()}_${userKey.lowercase()}", true)
        .putString("fecha_${companyCode.uppercase()}_${userKey.lowercase()}", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
        .apply()
}

fun claveUsuarioConsentimiento(nombre: String, email: String, username: String): String =
    email.ifBlank { username.ifBlank { nombre } }
