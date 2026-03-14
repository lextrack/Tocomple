package com.example.tocomple

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tocomple.ui.theme.AvocadoGreenAlt

@Composable
fun BusinessSection() {
    val context = LocalContext.current
    val storage = remember(context) { BusinessPreferencesStorage(context) }
    var plannedQuantities by rememberSaveable {
        mutableStateOf(completoTypes.associate { it.id to "" })
    }
    val proteinLabels = remember { completoTypes.map { it.proteinLabel }.distinct() }
    val ingredientNames = remember { completoTypes.flatMap { it.ingredients }.map { it.name }.distinct() }
    var breadPackagePrice by rememberSaveable { mutableStateOf("") }
    var breadUnitsPerPackage by rememberSaveable { mutableStateOf("") }
    var proteinPackagePrices by rememberSaveable {
        mutableStateOf(proteinLabels.associateWith { "" })
    }
    var proteinUnitsPerPackage by rememberSaveable {
        mutableStateOf(proteinLabels.associateWith { "" })
    }
    var ingredientCosts by rememberSaveable {
        mutableStateOf(ingredientNames.associateWith { "" })
    }
    var salePrices by rememberSaveable {
        mutableStateOf(completoTypes.associate { it.id to "" })
    }
    var templates by remember { mutableStateOf(listOf<BusinessTemplate>()) }
    var selectedTemplateId by rememberSaveable { mutableStateOf(defaultBusinessTemplate().id) }
    var newTemplateName by rememberSaveable { mutableStateOf("") }
    var hasLoadedBusinessState by remember { mutableStateOf(false) }
    var showTemplatePicker by rememberSaveable { mutableStateOf(false) }
    var productionExpanded by rememberSaveable { mutableStateOf(true) }
    var financeExpanded by rememberSaveable { mutableStateOf(false) }
    var purchaseExpanded by rememberSaveable { mutableStateOf(false) }
    var profitabilityExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(storage) {
        val savedState = storage.loadState()
        plannedQuantities = savedState.plannedQuantities
        templates = savedState.templates
        selectedTemplateId = savedState.selectedTemplateId

        breadPackagePrice = savedState.currentPricing.breadPackagePrice
        breadUnitsPerPackage = savedState.currentPricing.breadUnitsPerPackage
        proteinPackagePrices = savedState.currentPricing.proteinPackagePrices
        proteinUnitsPerPackage = savedState.currentPricing.proteinUnitsPerPackage
        ingredientCosts = savedState.currentPricing.ingredientCosts
        salePrices = savedState.currentPricing.salePrices
        hasLoadedBusinessState = true
    }

    val currentPricing = remember(
        breadPackagePrice,
        breadUnitsPerPackage,
        proteinPackagePrices,
        proteinUnitsPerPackage,
        ingredientCosts,
        salePrices
    ) {
        BusinessPricingConfig(
            breadPackagePrice = breadPackagePrice,
            breadUnitsPerPackage = breadUnitsPerPackage,
            proteinPackagePrices = proteinPackagePrices,
            proteinUnitsPerPackage = proteinUnitsPerPackage,
            ingredientCosts = ingredientCosts,
            salePrices = salePrices
        )
    }

    val selectedTemplate = remember(templates, selectedTemplateId) {
        templates.firstOrNull { it.id == selectedTemplateId }
    }

    LaunchedEffect(hasLoadedBusinessState, selectedTemplateId, selectedTemplate, currentPricing) {
        if (!hasLoadedBusinessState) return@LaunchedEffect
        if (selectedTemplateId == UNSAVED_TEMPLATE_ID) return@LaunchedEffect

        val templatePricing = selectedTemplate?.pricing ?: return@LaunchedEffect
        if (currentPricing != templatePricing) {
            selectedTemplateId = UNSAVED_TEMPLATE_ID
        }
    }

    LaunchedEffect(
        hasLoadedBusinessState,
        plannedQuantities,
        templates,
        selectedTemplateId,
        currentPricing
    ) {
        if (!hasLoadedBusinessState) return@LaunchedEffect

        storage.saveState(
            BusinessPreferencesState(
                plannedQuantities = plannedQuantities,
                templates = templates,
                selectedTemplateId = selectedTemplateId,
                currentPricing = currentPricing
            )
        )
    }

    if (!hasLoadedBusinessState) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BusinessHeaderCard()
            BusinessLoadingCard()
        }
        return
    }

    val plannedTypes = remember(plannedQuantities) {
        completoTypes.filter { type ->
            plannedQuantities[type.id]?.toIntOrNull()?.let { it > 0 } == true
        }
    }
    val visibleTypes = plannedTypes.ifEmpty { completoTypes }
    val visibleProteinLabels = remember(plannedTypes) {
        plannedTypes.map { it.proteinLabel }.distinct().ifEmpty { proteinLabels }
    }
    val visibleIngredientNames = remember(plannedTypes) {
        plannedTypes
            .flatMap { type -> type.ingredients.map { ingredient -> ingredient.name } }
            .distinct()
            .ifEmpty { ingredientNames }
    }

    val businessSummary = remember(
        plannedQuantities,
        breadPackagePrice,
        breadUnitsPerPackage,
        proteinPackagePrices,
        proteinUnitsPerPackage,
        ingredientCosts,
        salePrices
    ) {
        calculateBusinessSummary(
            plannedQuantities = plannedQuantities,
            smallAvocados = false,
            breadPackagePriceInput = breadPackagePrice,
            breadUnitsPerPackageInput = breadUnitsPerPackage,
            proteinPackagePrices = proteinPackagePrices,
            proteinUnitsPerPackage = proteinUnitsPerPackage,
            ingredientCostInputs = ingredientCosts,
            salePriceInputs = salePrices
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BusinessHeaderCard()
        BusinessSnapshotCard(summary = businessSummary)
        BusinessTemplatesCard(
            templates = templates,
            selectedTemplateId = selectedTemplateId,
            newTemplateName = newTemplateName,
            onNewTemplateNameChange = { newTemplateName = it.take(18) },
            onOpenTemplatePicker = { showTemplatePicker = true },
            onUnsavedSelected = { selectedTemplateId = UNSAVED_TEMPLATE_ID },
            onTemplateSelected = { template ->
                selectedTemplateId = template.id
                breadPackagePrice = template.pricing.breadPackagePrice
                breadUnitsPerPackage = template.pricing.breadUnitsPerPackage
                proteinPackagePrices = template.pricing.proteinPackagePrices
                proteinUnitsPerPackage = template.pricing.proteinUnitsPerPackage
                ingredientCosts = template.pricing.ingredientCosts
                salePrices = template.pricing.salePrices
            },
            onCreateTemplate = {
                val cleanName = newTemplateName.trim()
                if (cleanName.isBlank()) return@BusinessTemplatesCard

                val newTemplate = BusinessTemplate(
                    id = sanitizeTemplateId(cleanName, templates.map { it.id }),
                    name = sanitizeTemplateName(cleanName, templates.map { it.name }),
                    pricing = currentPricing
                )
                templates = templates + newTemplate
                selectedTemplateId = newTemplate.id
                newTemplateName = ""
            },
            onDeleteTemplate = { templateId ->
                if (templateId == defaultBusinessTemplate().id) return@BusinessTemplatesCard

                val remainingTemplates = templates.filterNot { it.id == templateId }
                templates = remainingTemplates
                val fallbackTemplate = remainingTemplates.firstOrNull { it.id == defaultBusinessTemplate().id }
                    ?: defaultBusinessTemplate()
                selectedTemplateId = fallbackTemplate.id
                breadPackagePrice = fallbackTemplate.pricing.breadPackagePrice
                breadUnitsPerPackage = fallbackTemplate.pricing.breadUnitsPerPackage
                proteinPackagePrices = fallbackTemplate.pricing.proteinPackagePrices
                proteinUnitsPerPackage = fallbackTemplate.pricing.proteinUnitsPerPackage
                ingredientCosts = fallbackTemplate.pricing.ingredientCosts
                salePrices = fallbackTemplate.pricing.salePrices
            }
        )

        if (showTemplatePicker) {
            TemplatePickerDialog(
                templates = templates,
                selectedTemplateId = selectedTemplateId,
                onDismiss = { showTemplatePicker = false },
                onUnsavedSelected = {
                    selectedTemplateId = UNSAVED_TEMPLATE_ID
                    showTemplatePicker = false
                },
                onTemplateSelected = { template ->
                    selectedTemplateId = template.id
                    breadPackagePrice = template.pricing.breadPackagePrice
                    breadUnitsPerPackage = template.pricing.breadUnitsPerPackage
                    proteinPackagePrices = template.pricing.proteinPackagePrices
                    proteinUnitsPerPackage = template.pricing.proteinUnitsPerPackage
                    ingredientCosts = template.pricing.ingredientCosts
                    salePrices = template.pricing.salePrices
                    showTemplatePicker = false
                }
            )
        }

        ExpandableBusinessCard(
            title = "Producción",
            subtitle = "Define cuántas unidades planeas vender por tipo.",
            expanded = productionExpanded,
            onExpandedChange = { productionExpanded = it }
        ) {
            BusinessPlanningContent(
                plannedQuantities = plannedQuantities,
                onQuantityChange = { typeId, value ->
                    plannedQuantities = plannedQuantities + (typeId to value.filter(Char::isDigit).take(5))
                }
            )
        }

        ExpandableBusinessCard(
            title = "Finanzas",
            subtitle = "Completa costos y precios de venta para estimar rentabilidad.",
            expanded = financeExpanded,
            onExpandedChange = { financeExpanded = it }
        ) {
            BusinessFinanceContent(
                visibleTypes = visibleTypes,
                visibleProteinLabels = visibleProteinLabels,
                visibleIngredientNames = visibleIngredientNames,
                breadPackagePrice = breadPackagePrice,
                onBreadPackagePriceChange = { breadPackagePrice = it.filter(Char::isDigit) },
                breadUnitsPerPackage = breadUnitsPerPackage,
                onBreadUnitsPerPackageChange = { breadUnitsPerPackage = it.filter(Char::isDigit).take(3) },
                proteinPackagePrices = proteinPackagePrices,
                onProteinPackagePriceChange = { label, value ->
                    proteinPackagePrices = proteinPackagePrices + (label to value.filter(Char::isDigit))
                },
                proteinUnitsPerPackage = proteinUnitsPerPackage,
                onProteinUnitsPerPackageChange = { label, value ->
                    proteinUnitsPerPackage = proteinUnitsPerPackage + (label to value.filter(Char::isDigit).take(3))
                },
                ingredientCosts = ingredientCosts,
                onIngredientCostChange = { name, value ->
                    ingredientCosts = ingredientCosts + (name to value.filter(Char::isDigit))
                },
                salePrices = salePrices,
                onSalePriceChange = { typeId, value ->
                    salePrices = salePrices + (typeId to value.filter(Char::isDigit))
                }
            )
        }

        if (businessSummary == null) {
            BusinessEmptyStateCard()
        } else {
            ExpandableBusinessCard(
                title = "Compra consolidada",
                subtitle = "Revisa cuánto necesitas comprar en total.",
                expanded = purchaseExpanded,
                onExpandedChange = { purchaseExpanded = it }
            ) {
                BusinessIngredientsContent(businessSummary)
            }

            ExpandableBusinessCard(
                title = "Rentabilidad por tipo",
                subtitle = "Compara costo, venta e ingreso por cada producto.",
                expanded = profitabilityExpanded,
                onExpandedChange = { profitabilityExpanded = it }
            ) {
                BusinessTypeProfitabilityContent(businessSummary)
            }
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
                    text = "Planifica producción, compra y rentabilidad del día",
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
                text = "Primero mira el resumen. Luego baja al detalle solo si necesitas ajustar producción o finanzas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessLoadingCard() {
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
                text = "Preparando negocio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cargando precios, plantilla activa y la última configuración guardada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessSnapshotCard(summary: BusinessSummary?) {
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
                text = "Resumen del día",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            if (summary == null) {
                Text(
                    text = "Completa la producción para ver costo, ingreso y ganancia estimada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            } else {
                BusinessOverviewContent(summary)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BusinessTemplatesCard(
    templates: List<BusinessTemplate>,
    selectedTemplateId: String,
    newTemplateName: String,
    onNewTemplateNameChange: (String) -> Unit,
    onOpenTemplatePicker: () -> Unit,
    onUnsavedSelected: () -> Unit,
    onTemplateSelected: (BusinessTemplate) -> Unit,
    onCreateTemplate: () -> Unit,
    onDeleteTemplate: (String) -> Unit
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
                text = "Plantillas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "La plantilla activa se va guardando sola mientras editas costos y precios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (selectedTemplateId) {
                            UNSAVED_TEMPLATE_ID -> UNSAVED_TEMPLATE_NAME
                            else -> templates.firstOrNull { it.id == selectedTemplateId }?.name
                                ?: defaultBusinessTemplate().name
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(onClick = onOpenTemplatePicker) {
                    Text("Elegir")
                }
            }

            OutlinedTextField(
                value = newTemplateName,
                onValueChange = onNewTemplateNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nueva plantilla") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateTemplate,
                    enabled = newTemplateName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar actual")
                }
                OutlinedButton(
                    onClick = { onDeleteTemplate(selectedTemplateId) },
                    enabled = selectedTemplateId != defaultBusinessTemplate().id &&
                        selectedTemplateId != UNSAVED_TEMPLATE_ID,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Eliminar activa")
                }
            }
        }
    }
}

@Composable
private fun TemplatePickerDialog(
    templates: List<BusinessTemplate>,
    selectedTemplateId: String,
    onDismiss: () -> Unit,
    onUnsavedSelected: () -> Unit,
    onTemplateSelected: (BusinessTemplate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = {
            Text(
                text = "Elegir plantilla",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedSelectableChip(
                    selected = selectedTemplateId == UNSAVED_TEMPLATE_ID,
                    onClick = onUnsavedSelected,
                    label = UNSAVED_TEMPLATE_NAME
                )
                templates.forEach { template ->
                    AnimatedSelectableChip(
                        selected = selectedTemplateId == template.id,
                        onClick = { onTemplateSelected(template) },
                        label = template.name
                    )
                }
            }
        }
    )
}

@Composable
private fun ExpandableBusinessCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (expanded) "Ocultar" else "Ver",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun BusinessPlanningContent(
    plannedQuantities: Map<String, String>,
    onQuantityChange: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun BusinessFinanceContent(
    visibleTypes: List<CompletoType>,
    visibleProteinLabels: List<String>,
    visibleIngredientNames: List<String>,
    breadPackagePrice: String,
    onBreadPackagePriceChange: (String) -> Unit,
    breadUnitsPerPackage: String,
    onBreadUnitsPerPackageChange: (String) -> Unit,
    proteinPackagePrices: Map<String, String>,
    onProteinPackagePriceChange: (String, String) -> Unit,
    proteinUnitsPerPackage: Map<String, String>,
    onProteinUnitsPerPackageChange: (String, String) -> Unit,
    ingredientCosts: Map<String, String>,
    onIngredientCostChange: (String, String) -> Unit,
    salePrices: Map<String, String>,
    onSalePriceChange: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BusinessFieldGroup(
            title = "Costos base",
            subtitle = "Ingresa cómo compras panes y proteínas: paquete y cuántas unidades trae."
        ) {
            PackageCostInputs(
                title = "Panes",
                packagePrice = breadPackagePrice,
                onPackagePriceChange = onBreadPackagePriceChange,
                unitsPerPackage = breadUnitsPerPackage,
                onUnitsPerPackageChange = onBreadUnitsPerPackageChange,
                packagePriceLabel = "Precio bolsa o paquete",
                unitsLabel = "Trae"
            )

            visibleProteinLabels.forEach { label ->
                PackageCostInputs(
                    title = label,
                    packagePrice = proteinPackagePrices[label].orEmpty(),
                    onPackagePriceChange = { onProteinPackagePriceChange(label, it) },
                    unitsPerPackage = proteinUnitsPerPackage[label].orEmpty(),
                    onUnitsPerPackageChange = { onProteinUnitsPerPackageChange(label, it) },
                    packagePriceLabel = "Precio paquete o bandeja",
                    unitsLabel = "Trae"
                )
            }
        }

        BusinessFieldGroup(
            title = "Ingredientes",
            subtitle = "Solo se muestran los ingredientes usados hoy."
        ) {
            visibleIngredientNames.forEach { name ->
                OutlinedTextField(
                    value = ingredientCosts[name].orEmpty(),
                    onValueChange = { onIngredientCostChange(name, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Costo por kg de $name") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ChileanCurrencyVisualTransformation()
                )
            }
        }

        BusinessFieldGroup(
            title = "Precios de venta",
            subtitle = "Solo se muestran los productos planificados."
        ) {
            visibleTypes.forEach { completoType ->
                OutlinedTextField(
                    value = salePrices[completoType.id].orEmpty(),
                    onValueChange = { onSalePriceChange(completoType.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Precio de venta ${completoType.name}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ChileanCurrencyVisualTransformation()
                )
            }
        }
    }
}

private fun sanitizeTemplateId(name: String, existingIds: List<String>): String {
    val baseId = name
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "plantilla" }

    var candidate = baseId
    var suffix = 2
    while (candidate in existingIds) {
        candidate = "${baseId}_$suffix"
        suffix += 1
    }
    return candidate
}

private fun sanitizeTemplateName(name: String, existingNames: List<String>): String {
    val baseName = name.trim().ifBlank { "Plantilla" }
    val normalizedExisting = existingNames.map { it.trim().lowercase() }.toSet()

    if (baseName.lowercase() !in normalizedExisting) return baseName

    var suffix = 2
    var candidate = "$baseName $suffix"
    while (candidate.trim().lowercase() in normalizedExisting) {
        suffix += 1
        candidate = "$baseName $suffix"
    }
    return candidate
}

private class ChileanCurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter(Char::isDigit)
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = raw
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, raw.length)
                val digitsToRight = raw.length - safeOffset
                val separatorsToRight = if (digitsToRight <= 0) 0 else (digitsToRight - 1) / 3
                return formatted.length - digitsToRight - separatorsToRight
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, formatted.length)
                val digitsBefore = formatted.take(safeOffset).count(Char::isDigit)
                return digitsBefore.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
private fun PackageCostInputs(
    title: String,
    packagePrice: String,
    onPackagePriceChange: (String) -> Unit,
    unitsPerPackage: String,
    onUnitsPerPackageChange: (String) -> Unit,
    packagePriceLabel: String,
    unitsLabel: String
) {
    val derivedUnitCost = remember(packagePrice, unitsPerPackage) {
        val price = packagePrice.toDoubleOrNull() ?: 0.0
        val units = unitsPerPackage.toDoubleOrNull() ?: 0.0
        if (price > 0.0 && units > 0.0) price / units else 0.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = packagePrice,
                onValueChange = onPackagePriceChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(packagePriceLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ChileanCurrencyVisualTransformation()
            )
            OutlinedTextField(
                value = unitsPerPackage,
                onValueChange = onUnitsPerPackageChange,
                modifier = Modifier.width(110.dp),
                singleLine = true,
                label = { Text(unitsLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        if (derivedUnitCost > 0.0) {
            Text(
                text = "Costo estimado por unidad: ${formatCurrency(derivedUnitCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusinessFieldGroup(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BusinessOverviewContent(summary: BusinessSummary) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryMetricChip(
            label = "Total",
            value = formatCompleteLabel(summary.totalProducts)
        )
        SummaryMetricChip(
            label = "Costo",
            value = formatCurrency(summary.totalCost)
        )
        SummaryMetricChip(
            label = "Ingreso",
            value = formatCurrency(summary.totalRevenue)
        )
        SummaryMetricChip(
            label = "Ganancia",
            value = formatCurrency(summary.totalProfit)
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
private fun BusinessIngredientsContent(summary: BusinessSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
private fun BusinessTypeProfitabilityContent(summary: BusinessSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
private fun BusinessEmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = "Ingresa cantidades en Producción para activar la compra consolidada y la rentabilidad.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
