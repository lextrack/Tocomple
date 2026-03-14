package com.example.tocomple

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tocomple.ui.theme.BusinessBlue
import com.example.tocomple.ui.theme.BusinessBlueSoft
import com.example.tocomple.ui.theme.TomatoRed
import com.example.tocomple.ui.theme.TocompleTheme
import kotlinx.coroutines.launch

@Composable
fun CompletosApp() {
    var selectedSectionName by rememberSaveable { mutableStateOf(AppSection.CALCULATOR.name) }
    val selectedSection = remember(selectedSectionName) { AppSection.valueOf(selectedSectionName) }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Tocomple",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Selecciona el modo de la app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppSection.entries.forEach { section ->
                                NavigationDrawerItem(
                                    label = { Text(section.label) },
                                    selected = selectedSection == section,
                                    onClick = {
                                        selectedSectionName = section.name
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (selectedSection == AppSection.CALCULATOR) {
                            "Modo para calcular ingredientes y cantidades por pedido."
                        } else {
                            "Modo para planificar producción, compra y rentabilidad del día."
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = screenBackgroundBrush(selectedSection)
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeTopBar(
                    selectedSection = selectedSection,
                    onMenuClick = { coroutineScope.launch { drawerState.open() } }
                )

                AnimatedContent(
                    targetState = selectedSection,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 240)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 240),
                                initialOffsetY = { it / 12 }
                            ) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 180)) +
                            slideOutVertically(
                                animationSpec = tween(durationMillis = 180),
                                targetOffsetY = { -it / 18 }
                            )
                    },
                    label = "app_section"
                ) { section ->
                    when (section) {
                        AppSection.CALCULATOR -> CalculatorSection()
                        AppSection.BUSINESS -> BusinessSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun screenBackgroundBrush(section: AppSection): Brush {
    val topColor = when (section) {
        AppSection.CALCULATOR -> TomatoRed.copy(alpha = 0.9f)
        AppSection.BUSINESS -> BusinessBlue.copy(alpha = 0.9f)
    }
    val middleColor = when (section) {
        AppSection.CALCULATOR -> MaterialTheme.colorScheme.background
        AppSection.BUSINESS -> BusinessBlueSoft.copy(alpha = 0.32f)
    }

    return Brush.verticalGradient(
        listOf(
            topColor,
            middleColor,
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ModeTopBar(
    selectedSection: AppSection,
    onMenuClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HamburgerButton(onClick = onMenuClick)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = selectedSection.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cambia de modo desde el menú lateral",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HamburgerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompletosPreview() {
    TocompleTheme(dynamicColor = false) {
        Surface {
            CompletosApp()
        }
    }
}
