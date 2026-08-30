package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.KaptaViewModel
import com.example.ui.components.EtherealBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.iOSButton
import com.example.ui.components.iOSLargeTitle
import com.example.ui.components.iOSPill
import com.example.ui.components.iOSSectionHeader
import com.example.ui.components.iOSSegmented

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansMembershipsScreen(
    viewModel: KaptaViewModel,
    onBack: () -> Unit,
    onNavigateToCreateCompany: () -> Unit
) {
    var activePlanTab by remember { mutableStateOf("Básico") } // "Básico", "Premium", "MAX IA"
    var isAnnualSelected by remember { mutableStateOf(true) }
    var selectedPlanForSubscribe by remember { mutableStateOf<String?>(null) }

    val accent = MaterialTheme.colorScheme.primary
    val planOptions = listOf("Básico", "Premium", "MAX IA")
    val subtitleText = when (activePlanTab) {
        "Premium" -> "Para negocios que quieren crecer sin límites."
        "MAX IA" -> "La experiencia completa con Inteligencia Artificial."
        else -> "Todo lo esencial para operar tu negocio."
    }
    val monthlyPrice = when (activePlanTab) {
        "MAX IA" -> "$339.900"
        "Premium" -> "$249.900"
        else -> "$149.900"
    }
    val annualPrice = when (activePlanTab) {
        "MAX IA" -> "$3.999.000"
        "Premium" -> "$2.499.000"
        else -> "$1.499.000"
    }
    val savingsText = when (activePlanTab) {
        "MAX IA" -> "Ahorra $499.800 (2 meses gratis)"
        "Premium" -> "Ahorra $499.800 (2 meses gratis)"
        else -> "Ahorra $299.800 (2 meses gratis)"
    }
    val featuresTitle = when (activePlanTab) {
        "Premium", "MAX IA" -> "Incluye todo lo del plan Básico, más:"
        else -> "Incluye:"
    }
    val featuresList = when (activePlanTab) {
        "MAX IA" -> listOf(
            "IA para crear funciones personalizadas",
            "Agentes de IA",
            "Automatizaciones inteligentes",
            "Predicción de ventas",
            "Recomendaciones automáticas",
            "Análisis financiero avanzado",
            "Múltiples empresas",
            "API e integraciones",
            "Branding personalizado",
            "Soporte VIP 24/7"
        )
        "Premium" -> listOf(
            "Usuarios ilimitados",
            "Varias sucursales",
            "Reportes avanzados",
            "Dashboard inteligente",
            "Facturación electrónica",
            "Pagos recurrentes",
            "Membresías y planes",
            "Backups automáticos",
            "Estadísticas en tiempo real",
            "Acceso de administradores",
            "Soporte prioritario"
        )
        else -> listOf(
            "POS desde el celular",
            "Inventario",
            "Registrar ventas",
            "Clientes",
            "Cuentas por cobrar",
            "Compras",
            "Caja",
            "Reportes básicos",
            "1 sucursal incluida",
            "Hasta 5 usuarios",
            "Soporte estándar"
        )
    }
    val actionText = when (activePlanTab) {
        "MAX IA" -> "Cambiar a MAX IA"
        "Premium" -> "Cambiar a Premium"
        else -> "Seleccionar Plan Básico"
    }
    val cardFooterText = when (activePlanTab) {
        "MAX IA" -> "Para negocios que quieren liderar"
        "Premium" -> "Perfecto para empresas en crecimiento"
        else -> "Ideal para pequeños negocios"
    }
    val bottomText = when (activePlanTab) {
        "MAX IA" -> "Para negocios que quieren liderar"
        "Premium" -> "Perfecto para empresas en crecimiento"
        else -> "Para negocios que quieren comenzar y crecer"
    }

    EtherealBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Planes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accent
                    )
                }
                Spacer(modifier = Modifier.width(70.dp))
            }

            iOSLargeTitle(
                title = "Planes",
                subtitle = subtitleText,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Plan Switcher Tabs (Básico / Premium / MAX IA)
            iOSSegmented(
                options = planOptions,
                selectedIndex = planOptions.indexOf(activePlanTab),
                onSelect = { activePlanTab = planOptions[it] },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Scrollable Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 18.dp, bottom = 12.dp)
            ) {
                // Plan Header Card
                GlassCard(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activePlanTab == "MAX IA") {
                                    Icons.Default.Diamond
                                } else if (activePlanTab == "Básico") {
                                    Icons.Default.ShoppingBag
                                } else {
                                    Icons.Default.WorkspacePremium
                                },
                                contentDescription = "Plan Icon",
                                tint = accent,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (activePlanTab == "MAX IA") "MAX IA" else "Plan $activePlanTab",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Billing Selector (Pago mensual / Pago anual)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                borderColor = if (!isAnnualSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                                borderWidth = if (!isAnnualSelected) 2.dp else 1.dp,
                                onClick = { isAnnualSelected = false }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Pago mensual",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = monthlyPrice,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = if (!isAnnualSelected) accent else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "COP / mes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            GlassCard(
                                modifier = Modifier.weight(1.15f),
                                borderColor = if (isAnnualSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                                borderWidth = if (isAnnualSelected) 2.dp else 1.dp,
                                onClick = { isAnnualSelected = true }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Pago anual",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = annualPrice,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = if (isAnnualSelected) accent else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "COP / año",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    iOSPill(
                                        text = savingsText,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        iOSButton(
                            text = actionText,
                            onClick = { selectedPlanForSubscribe = activePlanTab },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Features Section
                iOSSectionHeader(text = featuresTitle)
                GlassCard(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        featuresList.forEach { feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (activePlanTab == "MAX IA") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Sparkles",
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nuevas funciones con IA",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = accent
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Accede antes que nadie a las nuevas herramientas de IA.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cardFooterText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
            }

            // Sticky Bottom Subscription Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    iOSButton(
                        text = "Suscribirse",
                        onClick = { selectedPlanForSubscribe = activePlanTab },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bottomText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Modal: Subscription Flow Option
    if (selectedPlanForSubscribe != null) {
        val planName = selectedPlanForSubscribe!!
        ModalBottomSheet(onDismissRequest = { selectedPlanForSubscribe = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Suscripción al Plan $planName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¿Deseas registrar una nueva empresa o renovar la membresía de un negocio existente?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                iOSButton(
                    text = "Registrar Nueva Empresa",
                    onClick = {
                        selectedPlanForSubscribe = null
                        onNavigateToCreateCompany()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                iOSButton(
                    text = "Renovar Empresa Existente",
                    onClick = {
                        selectedPlanForSubscribe = null
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tinted = true
                )
            }
        }
    }
}
