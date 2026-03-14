package com.example.tocomple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GuideSection() {
    val context = LocalContext.current
    val storage = remember(context) { BusinessPreferencesStorage(context) }
    var businessState by remember { mutableStateOf<BusinessPreferencesState?>(null) }

    LaunchedEffect(storage) {
        businessState = storage.loadState()
    }

    if (businessState == null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GuideHeaderCard(templateName = null)
            GuideLoadingCard()
        }
        return
    }

    val state = businessState ?: return
    val summary = calculateBusinessSummary(
        plannedQuantities = state.plannedQuantities,
        smallAvocados = false,
        breadPackagePriceInput = state.currentPricing.breadPackagePrice,
        breadUnitsPerPackageInput = state.currentPricing.breadUnitsPerPackage,
        proteinPackagePrices = state.currentPricing.proteinPackagePrices,
        proteinUnitsPerPackage = state.currentPricing.proteinUnitsPerPackage,
        ingredientCostInputs = state.currentPricing.ingredientCosts,
        salePriceInputs = state.currentPricing.salePrices
    )
    val activeTemplateName = when (state.selectedTemplateId) {
        UNSAVED_TEMPLATE_ID -> UNSAVED_TEMPLATE_NAME
        else -> state.templates.firstOrNull { it.id == state.selectedTemplateId }?.name
            ?: defaultBusinessTemplate().name
    }
    val plannedTypes = completoTypes.mapNotNull { type ->
        val quantity = state.plannedQuantities[type.id]?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        type to quantity
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideHeaderCard(templateName = activeTemplateName)

        if (summary == null) {
            GuideEmptyStateCard()
        } else {
            GuideSummaryCard(summary = summary)
            GuideProductionCard(plannedTypes = plannedTypes, summary = summary)
            GuideFinancialCard(summary = summary)
            GuideDispatchCard(summary = summary)
            GuidePurchaseCard(summary = summary)
            GuidePdfButton(
                templateName = activeTemplateName,
                summary = summary,
                plannedTypes = plannedTypes
            )
        }
    }
}

@Composable
private fun GuideHeaderCard(templateName: String?) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale.forLanguageTag("es-CL")) }
    val todayLabel = remember { LocalDate.now().format(formatter) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Guía del día",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Vista general para fabricar, despachar y revisar resultados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Fecha: $todayLabel",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (templateName != null) {
                Text(
                    text = "Base usada: $templateName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideLoadingCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(
            text = "Cargando la última configuración de negocio...",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuideSummaryCard(summary: BusinessSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Resumen general",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            GuideMetricLine("Total a fabricar", formatCompleteLabel(summary.totalProducts), true)
            GuideMetricLine("Panes", formatCount(summary.breads, "pan", "panes"), false)
            summary.proteins.forEach { protein ->
                GuideMetricLine(
                    protein.label,
                    formatCount(protein.count, protein.singular, protein.plural),
                    false
                )
            }
            GuideMetricLine("Costo estimado", formatCurrency(summary.totalCost), false)
            GuideMetricLine("Ingreso esperado", formatCurrency(summary.totalRevenue), false)
            GuideMetricLine("Ganancia estimada", formatCurrency(summary.totalProfit), true)
        }
    }
}

@Composable
private fun GuideMetricLine(label: String, value: String, emphasize: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f)
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun GuideProductionCard(
    plannedTypes: List<Pair<CompletoType, Int>>,
    summary: BusinessSummary
) {
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
                text = "Detalle a fabricar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            plannedTypes.forEach { (type, quantity) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCompleteLabel(quantity),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Total del día: ${formatCompleteLabel(summary.totalProducts)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideFinancialCard(summary: BusinessSummary) {
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
                text = "Costo y venta por tipo",
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
                            text = "${formatCompleteLabel(typeCost.quantity)} • Costo ${formatCurrency(typeCost.unitCost)} c/u • Venta ${formatCurrency(typeCost.salePrice)}",
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
private fun GuideDispatchCard(summary: BusinessSummary) {
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
                text = "Despacho rápido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            GuideDispatchLine(
                label = "Panes",
                value = formatCount(summary.breads, "pan", "panes")
            )
            summary.proteins.forEach { protein ->
                GuideDispatchLine(
                    label = protein.label,
                    value = formatCount(protein.count, protein.singular, protein.plural)
                )
            }
        }
    }
}

@Composable
private fun GuideDispatchLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GuidePurchaseCard(summary: BusinessSummary) {
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
                text = "Compra y despacho",
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
private fun GuideEmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(
            text = "Todavía no hay producción definida en Negocio. Completa esa sección y aquí aparecerá la guía general del día.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuidePdfButton(
    templateName: String,
    summary: BusinessSummary,
    plannedTypes: List<Pair<CompletoType, Int>>
) {
    val context = LocalContext.current

    Button(
        onClick = {
            runCatching {
                val pdfUri = exportGuidePdf(
                    context = context,
                    templateName = templateName,
                    summary = summary,
                    plannedTypes = plannedTypes
                )
                shareGuidePdf(context, pdfUri)
            }.onFailure {
                Toast.makeText(
                    context,
                    "No pudimos generar el PDF.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Generar PDF")
    }
}
