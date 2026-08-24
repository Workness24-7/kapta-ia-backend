package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Layout centralizado del Super Admin.
 *
 * - [topBar]: bloque flotante superior (logo + saludo + dock). Se renderiza como overlay
 *   arriba y se MIDE su altura real; el contenido recibe un top-padding igual a esa altura
 *   (+ margen), de modo que el dock flotante nunca tape títulos, tarjetas ni listas, sin
 *   importar la pantalla ni la orientación. Al colapsar el saludo, la altura cambia y el
 *   padding se ajusta solo.
 * - [content]: recibe [PaddingValues] (top = altura del topBar medida, bottom = SafeArea).
 *
 * El dock mantiene su estilo flotante; solo se corrige el sistema de layout.
 */
@Composable
fun SuperAdminScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topBarHeight + 8.dp,
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                content(innerPadding)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .statusBarsPadding()
                    .onSizeChanged { size ->
                        topBarHeight = with(density) { size.height.toDp() }
                    }
            ) {
                topBar()
            }
        }
    }
}
