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
