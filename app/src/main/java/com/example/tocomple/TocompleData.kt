package com.example.tocomple

private const val TOMATO_WEIGHT_PER_COMPLETO = 0.06
private const val AVOCADO_WEIGHT_PER_COMPLETO = 0.075
const val AVERAGE_TOMATO_WEIGHT = 0.1
const val REGULAR_AVOCADO_WEIGHT = 0.165
const val SMALL_AVOCADO_WEIGHT = 0.12
private const val AVERAGE_CABBAGE_WEIGHT = 1.0
private const val AVERAGE_SAUCE_BOTTLE_KG = 0.5
private const val AVERAGE_ONION_WEIGHT = 0.1
private const val AVERAGE_EGG_WEIGHT = 0.06
private const val AVERAGE_FRIES_BAG_KG = 1.0
private const val AVERAGE_GREEN_BEAN_BUNCH_KG = 0.25
private const val AVERAGE_GREEN_CHILI_WEIGHT = 0.02

val completoTypes = listOf(
    CompletoType(
        id = "italiano",
        name = "Italiano",
        description = "Tomate, palta y mayo",
        ingredients = listOf(
            IngredientSpec("Tomates", TOMATO_WEIGHT_PER_COMPLETO, AVERAGE_TOMATO_WEIGHT),
            IngredientSpec("Paltas", AVOCADO_WEIGHT_PER_COMPLETO, REGULAR_AVOCADO_WEIGHT),
            IngredientSpec("Mayonesa", 0.02, AVERAGE_SAUCE_BOTTLE_KG, "envase", "envases")
        )
    ),
    CompletoType(
        id = "dinamico",
        name = "Dinámico",
        description = "Tomate, palta, chucrut, salsa americana y mayo",
        ingredients = listOf(
            IngredientSpec("Tomates", 0.045, AVERAGE_TOMATO_WEIGHT),
            IngredientSpec("Paltas", 0.06, REGULAR_AVOCADO_WEIGHT),
            IngredientSpec("Chucrut", 0.03, AVERAGE_CABBAGE_WEIGHT),
            IngredientSpec("Salsa americana", 0.03, AVERAGE_CABBAGE_WEIGHT),
            IngredientSpec("Mayonesa", 0.02, AVERAGE_SAUCE_BOTTLE_KG, "envase", "envases")
        )
    ),
    CompletoType(
        id = "al_pobre",
        name = "A lo pobre",
        description = "Cebolla caramelizada, huevo frito y papas fritas",
        ingredients = listOf(
            IngredientSpec("Cebolla", 0.05, AVERAGE_ONION_WEIGHT),
            IngredientSpec("Huevos", 0.06, AVERAGE_EGG_WEIGHT, "huevo", "huevos"),
            IngredientSpec("Papas fritas", 0.05, AVERAGE_FRIES_BAG_KG, "bolsa", "bolsas")
        )
    ),
    CompletoType(
        id = "chacarero",
        name = "Chacarero",
        description = "Tomate, porotos verdes, ají verde y mayo",
        ingredients = listOf(
            IngredientSpec("Tomates", 0.04, AVERAGE_TOMATO_WEIGHT),
            IngredientSpec("Porotos verdes", 0.04, AVERAGE_GREEN_BEAN_BUNCH_KG, "manojo", "manojos"),
            IngredientSpec("Ají verde", 0.015, AVERAGE_GREEN_CHILI_WEIGHT, "ají", "ajíes"),
            IngredientSpec("Mayonesa", 0.02, AVERAGE_SAUCE_BOTTLE_KG, "envase", "envases")
        )
    ),
    CompletoType(
        id = "as_luco",
        name = "As Luco",
        description = "Carne de vacuno y queso fundido",
        proteinLabel = "Churrascos",
        proteinSingular = "churrasco",
        proteinPlural = "churrascos",
        ingredients = listOf(
            IngredientSpec("Carne de vacuno", 0.08, 0.5, "bandeja", "bandejas"),
            IngredientSpec("Queso", 0.05, 0.25, "paquete", "paquetes")
        )
    )
)

enum class AppSection(val label: String) {
    CALCULATOR("Calculadora"),
    BUSINESS("Negocio")
}

data class CompletoCalculation(
    val completoType: CompletoType,
    val people: Int,
    val completosPerPerson: Int,
    val baseCompletos: Int,
    val extraCompletos: Int,
    val totalCompletos: Int,
    val ingredients: List<IngredientCalculation>
)

data class CompletoType(
    val id: String,
    val name: String,
    val description: String,
    val ingredients: List<IngredientSpec>,
    val proteinLabel: String = "Vienesas",
    val proteinSingular: String = "vienesa",
    val proteinPlural: String = "vienesas"
) {
    val usesAvocado: Boolean
        get() = ingredients.any { it.name == "Paltas" }
}

data class IngredientSpec(
    val name: String,
    val kgPerCompleto: Double,
    val averageUnitWeightKg: Double,
    val unitLabelSingular: String = "unidad",
    val unitLabelPlural: String = "unidades"
)

data class IngredientCalculation(
    val name: String,
    val totalKg: Double,
    val unitEstimate: Int,
    val unitLabelSingular: String,
    val unitLabelPlural: String
)

data class DetailItem(
    val title: String,
    val mainValue: String,
    val supporting: String,
    val accent: androidx.compose.ui.graphics.Color
)

data class BusinessSummary(
    val totalProducts: Int,
    val breads: Int,
    val proteins: List<ProteinTotal>,
    val ingredients: List<AggregatedIngredient>,
    val totalCost: Double,
    val totalRevenue: Double,
    val totalProfit: Double,
    val typeCosts: List<BusinessTypeCost>
)

data class ProteinTotal(
    val label: String,
    val count: Int,
    val singular: String,
    val plural: String
)

data class AggregatedIngredient(
    val name: String,
    val totalKg: Double,
    val unitEstimate: Int,
    val unitLabelSingular: String,
    val unitLabelPlural: String
)

data class BusinessTypeCost(
    val name: String,
    val quantity: Int,
    val totalCost: Double,
    val unitCost: Double,
    val salePrice: Double,
    val revenue: Double,
    val profit: Double
)
