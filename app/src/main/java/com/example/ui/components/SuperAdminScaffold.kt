package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Layout centralizado del Super Admin.
 *
 * - [dock]: barra de navegación global. Se ancla al fondo mediante el bottomBar del
 *   Scaffold, de modo que su altura real (incluido margen inferior y SafeArea) se
 *   descuenta automáticamente del padding inferior del [content]. Así el contenido
 *   nunca queda oculto detrás del dock, sin importar la pantalla ni la orientación.
 * - [content]: recibe [PaddingValues] con el bottom inset correcto. El header fijo
 *   (logo + saludo) se renderiza como overlay dentro de este content, igual que antes.
 *
 * El dock mantiene su estilo flotante; solo se corrige el sistema de layout.
 */
@Composable
fun SuperAdminScaffold(
    modifier: Modifier = Modifier,
    dock: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                dock()
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
