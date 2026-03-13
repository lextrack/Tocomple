package com.example.tocomple

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tocomple.ui.theme.AvocadoGreenAlt
import com.example.tocomple.ui.theme.TocompleTheme

@Composable
fun CompletosApp() {
    var peopleInput by rememberSaveable { mutableStateOf("1") }
    var completosPerPersonInput by rememberSaveable { mutableStateOf("1") }
    var smallAvocados by rememberSaveable { mutableStateOf(false) }
    var extraCompletos by rememberSaveable { mutableIntStateOf(0) }
    var selectedTypeId by rememberSaveable { mutableStateOf(completoTypes.first().id) }

    val people = peopleInput.toIntOrNull() ?: 0
    val completosPerPerson = completosPerPersonInput.toIntOrNull() ?: 0
    val selectedType = remember(selectedTypeId) {
        completoTypes.firstOrNull { it.id == selectedTypeId } ?: completoTypes.first()
    }
    val calculation = remember(
        people,
        completosPerPerson,
        smallAvocados,
        extraCompletos,
        selectedType
    ) {
        calculatePlan(
            people = people,
            completosPerPerson = completosPerPerson,
            smallAvocados = smallAvocados,
            extraCompletos = extraCompletos,
            completoType = selectedType
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(
                selectedType = selectedType,
                onTypeSelected = { selectedTypeId = it.id }
            )

            PurchaseSummaryCard(
                selectedType = selectedType,
                calculation = calculation
            )

            FormCard(
                selectedType = selectedType,
                peopleInput = peopleInput,
                onPeopleChange = { peopleInput = it.filter(Char::isDigit).take(5) },
                completosPerPersonInput = completosPerPersonInput,
                onCompletosChange = { completosPerPersonInput = it.filter(Char::isDigit).take(3) },
                smallAvocados = smallAvocados,
                onSmallAvocadosChange = { smallAvocados = it },
                extraCompletos = extraCompletos,
                onExtraSelected = { extraCompletos = it }
            )

            if (calculation == null) {
                EmptyStateCard()
            } else {
                DetailGrid(calculation)
                AssumptionsCard(calculation, smallAvocados)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HeaderCard(
    selectedType: CompletoType,
    onTypeSelected: (CompletoType) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                completoTypes.forEach { completoType ->
                    AnimatedSelectableChip(
                        selected = selectedType.id == completoType.id,
                        onClick = { onTypeSelected(completoType) },
                        label = completoType.name
                    )
                }
            }

            Crossfade(
                targetState = selectedType,
                animationSpec = tween(durationMillis = 220),
                label = "header_type_transition"
            ) { animatedType ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AvocadoGreenAlt)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = animatedType.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = animatedType.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tipo seleccionado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FormCard(
    selectedType: CompletoType,
    peopleInput: String,
    onPeopleChange: (String) -> Unit,
    completosPerPersonInput: String,
    onCompletosChange: (String) -> Unit,
    smallAvocados: Boolean,
    onSmallAvocadosChange: (Boolean) -> Unit,
    extraCompletos: Int,
    onExtraSelected: (Int) -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val compactTwoColumns = screenWidthDp >= 390

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (compactTwoColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = peopleInput,
                        onValueChange = onPeopleChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Personas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = completosPerPersonInput,
                        onValueChange = onCompletosChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Por persona") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else {
                OutlinedTextField(
                    value = peopleInput,
                    onValueChange = onPeopleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Personas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = completosPerPersonInput,
                    onValueChange = onCompletosChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Completos por persona") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (selectedType.usesAvocado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Las paltas son chicas",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = smallAvocados,
                        onCheckedChange = onSmallAvocadosChange
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Completos extra",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Agrega algunos por si alguien repite o para no quedarte corto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 2, 4, 6).forEach { option ->
                        AnimatedSelectableChip(
                            selected = extraCompletos == option,
                            onClick = { onExtraSelected(option) },
                            label = if (option == 0) "Sin extra" else "+$option"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = "Necesitamos al menos 1 persona y 1 completo por persona para calcular la compra.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
