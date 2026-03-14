package com.example.tocomple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tocomple.ui.theme.AvocadoGreenAlt

@Composable
fun BusinessSection() {
    var plannedQuantities by rememberSaveable {
        mutableStateOf(completoTypes.associate { it.id to "" })
    }
    val proteinLabels = remember { completoTypes.map { it.proteinLabel }.distinct() }
    val ingredientNames = remember { completoTypes.flatMap { it.ingredients }.map { it.name }.distinct() }
    var breadUnitCost by rememberSaveable { mutableStateOf("") }
    var proteinUnitCosts by rememberSaveable {
        mutableStateOf(proteinLabels.associateWith { "" })
    }
    var ingredientCosts by rememberSaveable {
        mutableStateOf(ingredientNames.associateWith { "" })
    }
    var salePrices by rememberSaveable {
        mutableStateOf(completoTypes.associate { it.id to "" })
    }

    val businessSummary = remember(
        plannedQuantities,
        breadUnitCost,
        proteinUnitCosts,
        ingredientCosts,
        salePrices
    ) {
        calculateBusinessSummary(
            plannedQuantities = plannedQuantities,
            smallAvocados = false,
            breadUnitCostInput = breadUnitCost,
            proteinUnitCosts = proteinUnitCosts,
            ingredientCostInputs = ingredientCosts,
            salePriceInputs = salePrices
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BusinessHeaderCard()
        BusinessPlanningCard(
            plannedQuantities = plannedQuantities,
            onQuantityChange = { typeId, value ->
                plannedQuantities = plannedQuantities + (typeId to value.filter(Char::isDigit).take(5))
            }
        )
        BusinessCostsCard(
            breadUnitCost = breadUnitCost,
            onBreadUnitCostChange = { breadUnitCost = it.filter(Char::isDigit) },
            proteinUnitCosts = proteinUnitCosts,
            onProteinCostChange = { label, value ->
                proteinUnitCosts = proteinUnitCosts + (label to value.filter(Char::isDigit))
            },
            ingredientCosts = ingredientCosts,
            onIngredientCostChange = { name, value ->
                ingredientCosts = ingredientCosts + (name to value.filter(Char::isDigit))
            }
        )
        BusinessSalesCard(
            salePrices = salePrices,
            onSalePriceChange = { typeId, value ->
                salePrices = salePrices + (typeId to value.filter(Char::isDigit))
            }
        )

        if (businessSummary == null) {
            BusinessEmptyStateCard()
        } else {
            BusinessOverviewCard(businessSummary)
            BusinessIngredientsCard(businessSummary)
            BusinessTypeCostsCard(businessSummary)
        }
    }
}

@Composable
private fun BusinessHeaderCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AvocadoGreenAlt)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Planifica producción y compra total del día",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
            Text(
                text = "Negocio",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Ingresa cuántas unidades planeas vender de cada tipo y te consolidamos la compra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessPlanningCard(
    plannedQuantities: Map<String, String>,
    onQuantityChange: (String, String) -> Unit
) {
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
                text = "Producción del día",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            completoTypes.forEach { completoType ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = completoType.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = completoType.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = plannedQuantities[completoType.id].orEmpty(),
                        onValueChange = { onQuantityChange(completoType.id, it) },
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BusinessOverviewCard(summary: BusinessSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen del negocio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricChip(
                    label = "Total",
                    value = formatCompleteLabel(summary.totalProducts)
                )
                SummaryMetricChip(
                    label = "Panes",
                    value = formatCount(summary.breads, "pan", "panes")
                )
                summary.proteins.forEach { protein ->
                    SummaryMetricChip(
                        label = protein.label,
                        value = formatCount(protein.count, protein.singular, protein.plural)
                    )
                }
                SummaryMetricChip(
                    label = "Costo total",
                    value = formatCurrency(summary.totalCost)
                )
                SummaryMetricChip(
                    label = "Ingreso esperado",
                    value = formatCurrency(summary.totalRevenue)
                )
                SummaryMetricChip(
                    label = "Ganancia",
                    value = formatCurrency(summary.totalProfit)
                )
            }
        }
    }
}

@Composable
private fun BusinessCostsCard(
    breadUnitCost: String,
    onBreadUnitCostChange: (String) -> Unit,
    proteinUnitCosts: Map<String, String>,
    onProteinCostChange: (String, String) -> Unit,
    ingredientCosts: Map<String, String>,
    onIngredientCostChange: (String, String) -> Unit
) {
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
                text = "Costos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ingresa valores referenciales para estimar costo total y costo por tipo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = breadUnitCost,
                onValueChange = onBreadUnitCostChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Costo por pan") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            proteinUnitCosts.forEach { (label, value) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { onProteinCostChange(label, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Costo por $label") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            ingredientCosts.forEach { (name, value) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { onIngredientCostChange(name, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Costo por kg de $name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun BusinessSalesCard(
    salePrices: Map<String, String>,
    onSalePriceChange: (String, String) -> Unit
) {
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
                text = "Precios de venta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ingresa el valor de venta por unidad para estimar ingreso y ganancia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            completoTypes.forEach { completoType ->
                OutlinedTextField(
                    value = salePrices[completoType.id].orEmpty(),
                    onValueChange = { onSalePriceChange(completoType.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Precio de venta ${completoType.name}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricChip(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun BusinessIngredientsCard(summary: BusinessSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Compra total estimada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            summary.ingredients.forEach { ingredient ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ingredient.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatCount(
                                ingredient.unitEstimate,
                                ingredient.unitLabelSingular,
                                ingredient.unitLabelPlural
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatWeight(ingredient.totalKg),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessTypeCostsCard(summary: BusinessSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Rentabilidad por tipo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            summary.typeCosts.forEach { typeCost ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = typeCost.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = buildString {
                                append(formatCompleteLabel(typeCost.quantity))
                                append(" • Costo ")
                                append(formatCurrency(typeCost.unitCost))
                                append(" c/u")
                                if (typeCost.salePrice > 0) {
                                    append(" • Venta ")
                                    append(formatCurrency(typeCost.salePrice))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(typeCost.profit),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ingreso ${formatCurrency(typeCost.revenue)}",
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
private fun BusinessEmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = "Ingresa al menos una cantidad en producción del día para generar la compra total.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
