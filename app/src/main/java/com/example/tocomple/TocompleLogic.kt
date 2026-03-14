package com.example.tocomple

import java.util.Locale
import kotlin.math.ceil

fun calculatePlan(
    people: Int,
    completosPerPerson: Int,
    smallAvocados: Boolean,
    extraCompletos: Int,
    completoType: CompletoType
): CompletoCalculation? {
    if (people <= 0 || completosPerPerson <= 0) return null

    val baseCompletos = people * completosPerPerson
    val totalCompletos = baseCompletos + extraCompletos
    val ingredients = completoType.ingredients.map { ingredient ->
        val averageWeight = when {
            ingredient.name == "Paltas" && smallAvocados -> SMALL_AVOCADO_WEIGHT
            else -> ingredient.averageUnitWeightKg
        }
        val totalKg = totalCompletos * ingredient.kgPerCompleto
        IngredientCalculation(
            name = ingredient.name,
            totalKg = totalKg,
            unitEstimate = ceil(totalKg / averageWeight).toInt(),
            unitLabelSingular = ingredient.unitLabelSingular,
            unitLabelPlural = ingredient.unitLabelPlural
        )
    }

    return CompletoCalculation(
        completoType = completoType,
        people = people,
        completosPerPerson = completosPerPerson,
        baseCompletos = baseCompletos,
        extraCompletos = extraCompletos,
        totalCompletos = totalCompletos,
        ingredients = ingredients
    )
}

fun formatWeight(valueKg: Double): String {
    return if (valueKg >= 1.0) {
        "${String.format(Locale.US, "%.2f", valueKg)} kg"
    } else {
        "${(valueKg * 1000).toInt()} g"
    }
}

fun formatCount(value: Int, singular: String, plural: String): String =
    "$value ${if (value == 1) singular else plural}"

fun formatCurrency(value: Double): String = "$" + String.format(Locale.US, "%,.0f", value)

fun formatCompleteLabel(value: Int): String = formatCount(value, "completo", "completos")

fun formatPersonLabel(value: Int): String = formatCount(value, "persona", "personas")

fun buildAssumptionsText(
    calculation: CompletoCalculation,
    smallAvocados: Boolean
): String {
    val produceAssumptions = calculation.completoType.ingredients.joinToString(" • ") { ingredient ->
        val average = when {
            ingredient.name == "Paltas" && smallAvocados -> SMALL_AVOCADO_WEIGHT
            else -> ingredient.averageUnitWeightKg
        }
        "${ingredient.name}: base ${formatWeight(average)} por unidad"
    }

    val recipeAssumptions = calculation.completoType.ingredients.joinToString(" • ") { ingredient ->
        "${ingredient.name}: usa ${formatWeight(ingredient.kgPerCompleto)} por completo"
    }

    return "Peso base:\n$produceAssumptions\n\nUso por completo:\n$recipeAssumptions"
}

fun calculateBusinessSummary(
    plannedQuantities: Map<String, String>,
    smallAvocados: Boolean,
    breadUnitCostInput: String,
    proteinUnitCosts: Map<String, String>,
    ingredientCostInputs: Map<String, String>,
    salePriceInputs: Map<String, String>
): BusinessSummary? {
    val plannedTypes = completoTypes.mapNotNull { completoType ->
        val quantity = plannedQuantities[completoType.id]?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        completoType to quantity
    }

    if (plannedTypes.isEmpty()) return null

    val totalProducts = plannedTypes.sumOf { it.second }

    val proteins = plannedTypes
        .groupBy(
            keySelector = { Triple(it.first.proteinLabel, it.first.proteinSingular, it.first.proteinPlural) },
            valueTransform = { it.second }
        )
        .map { (protein, counts) ->
            ProteinTotal(
                label = protein.first,
                count = counts.sum(),
                singular = protein.second,
                plural = protein.third
            )
        }

    val ingredientBuckets = linkedMapOf<String, MutableAggregatedIngredient>()
    plannedTypes.forEach { (completoType, quantity) ->
        completoType.ingredients.forEach { ingredient ->
            val averageWeight = when {
                ingredient.name == "Paltas" && smallAvocados -> SMALL_AVOCADO_WEIGHT
                else -> ingredient.averageUnitWeightKg
            }
            val bucket = ingredientBuckets.getOrPut(ingredient.name) {
                MutableAggregatedIngredient(
                    name = ingredient.name,
                    totalKg = 0.0,
                    averageUnitWeightKg = averageWeight,
                    unitLabelSingular = ingredient.unitLabelSingular,
                    unitLabelPlural = ingredient.unitLabelPlural
                )
            }
            bucket.totalKg += ingredient.kgPerCompleto * quantity
        }
    }

    val aggregatedIngredients = ingredientBuckets.values.map { ingredient ->
        AggregatedIngredient(
            name = ingredient.name,
            totalKg = ingredient.totalKg,
            unitEstimate = ceil(ingredient.totalKg / ingredient.averageUnitWeightKg).toInt(),
            unitLabelSingular = ingredient.unitLabelSingular,
            unitLabelPlural = ingredient.unitLabelPlural
        )
    }

    val breadUnitCost = breadUnitCostInput.toDoubleOrNull() ?: 0.0
    val ingredientCostMap = ingredientCostInputs.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
    val proteinCostMap = proteinUnitCosts.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
    val salePriceMap = salePriceInputs.mapValues { it.value.toDoubleOrNull() ?: 0.0 }

    val typeCosts = plannedTypes.map { (completoType, quantity) ->
        val breadCost = breadUnitCost * quantity
        val proteinCost = (proteinCostMap[completoType.proteinLabel] ?: 0.0) * quantity
        val ingredientsCost = completoType.ingredients.sumOf { ingredient ->
            (ingredientCostMap[ingredient.name] ?: 0.0) * (ingredient.kgPerCompleto * quantity)
        }
        val totalCost = breadCost + proteinCost + ingredientsCost
        val salePrice = salePriceMap[completoType.id] ?: 0.0
        val revenue = salePrice * quantity
        BusinessTypeCost(
            name = completoType.name,
            quantity = quantity,
            totalCost = totalCost,
            unitCost = if (quantity > 0) totalCost / quantity else 0.0,
            salePrice = salePrice,
            revenue = revenue,
            profit = revenue - totalCost
        )
    }

    return BusinessSummary(
        totalProducts = totalProducts,
        breads = totalProducts,
        proteins = proteins,
        ingredients = aggregatedIngredients,
        totalCost = typeCosts.sumOf { it.totalCost },
        totalRevenue = typeCosts.sumOf { it.revenue },
        totalProfit = typeCosts.sumOf { it.profit },
        typeCosts = typeCosts
    )
}

private data class MutableAggregatedIngredient(
    val name: String,
    var totalKg: Double,
    val averageUnitWeightKg: Double,
    val unitLabelSingular: String,
    val unitLabelPlural: String
)
