package com.example.tocomple

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val BUSINESS_PREFS_NAME = "business_preferences"
private const val BUSINESS_STATE_KEY = "business_state"
const val UNSAVED_TEMPLATE_ID = "sin_plantilla"
const val UNSAVED_TEMPLATE_NAME = "Sin plantilla"
private const val DEFAULT_TEMPLATE_ID = "precio_normal"
private const val DEFAULT_TEMPLATE_NAME = "Precio normal"
private const val DEFAULT_BREAD_UNITS_PER_PACKAGE = "6"

private val defaultProteinUnitsPerPackage = mapOf(
    "Vienesas" to "8",
    "Churrascos" to "10"
)

class BusinessPreferencesStorage(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(BUSINESS_PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): BusinessPreferencesState {
        val rawState = sharedPreferences.getString(BUSINESS_STATE_KEY, null)
        val parsedState = rawState?.let(::parseState)

        return normalizeState(parsedState)
    }

    fun saveState(state: BusinessPreferencesState) {
        sharedPreferences.edit()
            .putString(BUSINESS_STATE_KEY, serializeState(normalizeState(state)).toString())
            .apply()
    }

    private fun normalizeState(state: BusinessPreferencesState?): BusinessPreferencesState {
        val normalizedTemplates = state?.templates
            ?.map(::normalizeTemplate)
            ?.distinctBy { it.id }
            ?.toMutableList()
            ?: mutableListOf()

        if (normalizedTemplates.none { it.id == DEFAULT_TEMPLATE_ID }) {
            normalizedTemplates.add(0, defaultBusinessTemplate())
        }

        val rawSelectedTemplateId = state?.selectedTemplateId
            ?.takeIf { id -> id == UNSAVED_TEMPLATE_ID || normalizedTemplates.any { it.id == id } }
            ?: DEFAULT_TEMPLATE_ID
        val normalizedCurrentPricing = normalizePricing(
            state?.currentPricing
                ?: normalizedTemplates.firstOrNull { it.id == rawSelectedTemplateId }?.pricing
                ?: defaultBusinessPricingConfig()
        )
        val selectedTemplateId = when {
            rawSelectedTemplateId != UNSAVED_TEMPLATE_ID -> rawSelectedTemplateId
            else -> normalizedTemplates.firstOrNull { it.pricing == normalizedCurrentPricing }?.id
                ?: UNSAVED_TEMPLATE_ID
        }

        return BusinessPreferencesState(
            plannedQuantities = normalizePlannedQuantities(state?.plannedQuantities.orEmpty()),
            templates = normalizedTemplates,
            selectedTemplateId = selectedTemplateId,
            currentPricing = normalizedCurrentPricing
        )
    }

    private fun normalizeTemplate(template: BusinessTemplate): BusinessTemplate {
        val safeName = template.name.ifBlank { DEFAULT_TEMPLATE_NAME }
        val normalizedPricing = normalizePricing(template.pricing)
        val pricing = if (template.id == DEFAULT_TEMPLATE_ID) {
            if (isPricingEmpty(normalizedPricing)) {
                mergePricingWithDefaults(normalizedPricing)
            } else {
                normalizedPricing
            }
        } else {
            normalizedPricing
        }

        return template.copy(
            name = safeName,
            pricing = pricing
        )
    }

    private fun normalizePricing(pricing: BusinessPricingConfig): BusinessPricingConfig {
        val proteinLabels = completoTypes.map { it.proteinLabel }.distinct()
        val ingredientNames = completoTypes.flatMap { it.ingredients }.map { it.name }.distinct()
        val typeIds = completoTypes.map { it.id }

        return BusinessPricingConfig(
            breadPackagePrice = pricing.breadPackagePrice.filter(Char::isDigit),
            breadUnitsPerPackage = pricing.breadUnitsPerPackage.filter(Char::isDigit),
            proteinPackagePrices = proteinLabels.associateWith { label ->
                pricing.proteinPackagePrices[label].orEmpty().filter(Char::isDigit)
            },
            proteinUnitsPerPackage = proteinLabels.associateWith { label ->
                pricing.proteinUnitsPerPackage[label].orEmpty().filter(Char::isDigit)
            },
            ingredientCosts = ingredientNames.associateWith { name ->
                pricing.ingredientCosts[name].orEmpty().filter(Char::isDigit)
            },
            salePrices = typeIds.associateWith { typeId ->
                pricing.salePrices[typeId].orEmpty().filter(Char::isDigit)
            }
        )
    }

    private fun normalizePlannedQuantities(values: Map<String, String>): Map<String, String> =
        completoTypes.associate { type ->
            type.id to values[type.id].orEmpty().filter(Char::isDigit)
        }

    private fun mergePricingWithDefaults(pricing: BusinessPricingConfig): BusinessPricingConfig {
        val defaults = defaultBusinessPricingConfig()

        return BusinessPricingConfig(
            breadPackagePrice = pricing.breadPackagePrice.ifBlank { defaults.breadPackagePrice },
            breadUnitsPerPackage = pricing.breadUnitsPerPackage.ifBlank { defaults.breadUnitsPerPackage },
            proteinPackagePrices = defaults.proteinPackagePrices.mapValues { (label, defaultValue) ->
                pricing.proteinPackagePrices[label].orEmpty().ifBlank { defaultValue }
            },
            proteinUnitsPerPackage = defaults.proteinUnitsPerPackage.mapValues { (label, defaultValue) ->
                pricing.proteinUnitsPerPackage[label].orEmpty().ifBlank { defaultValue }
            },
            ingredientCosts = defaults.ingredientCosts.mapValues { (name, defaultValue) ->
                pricing.ingredientCosts[name].orEmpty().ifBlank { defaultValue }
            },
            salePrices = defaults.salePrices.mapValues { (typeId, defaultValue) ->
                pricing.salePrices[typeId].orEmpty().ifBlank { defaultValue }
            }
        )
    }

    private fun isPricingEmpty(pricing: BusinessPricingConfig): Boolean {
        return pricing.breadPackagePrice.isBlank() &&
            pricing.breadUnitsPerPackage.isBlank() &&
            pricing.proteinPackagePrices.values.all { it.isBlank() } &&
            pricing.proteinUnitsPerPackage.values.all { it.isBlank() } &&
            pricing.ingredientCosts.values.all { it.isBlank() } &&
            pricing.salePrices.values.all { it.isBlank() }
    }

    private fun serializeState(state: BusinessPreferencesState): JSONObject {
        return JSONObject().apply {
            put("selectedTemplateId", state.selectedTemplateId)
            put("plannedQuantities", mapToJson(state.plannedQuantities))
            put("currentPricing", serializePricing(state.currentPricing))
            put(
                "templates",
                JSONArray().apply {
                    state.templates.forEach { template ->
                        put(
                            JSONObject().apply {
                                put("id", template.id)
                                put("name", template.name)
                                put("pricing", serializePricing(template.pricing))
                            }
                        )
                    }
                }
            )
        }
    }

    private fun serializePricing(pricing: BusinessPricingConfig): JSONObject {
        return JSONObject().apply {
            put("breadPackagePrice", pricing.breadPackagePrice)
            put("breadUnitsPerPackage", pricing.breadUnitsPerPackage)
            put("proteinPackagePrices", mapToJson(pricing.proteinPackagePrices))
            put("proteinUnitsPerPackage", mapToJson(pricing.proteinUnitsPerPackage))
            put("ingredientCosts", mapToJson(pricing.ingredientCosts))
            put("salePrices", mapToJson(pricing.salePrices))
        }
    }

    private fun mapToJson(values: Map<String, String>): JSONObject {
        return JSONObject().apply {
            values.forEach { (key, value) -> put(key, value) }
        }
    }

    private fun parseState(rawState: String): BusinessPreferencesState? {
        return runCatching {
            val json = JSONObject(rawState)
            val templates = json.optJSONArray("templates")?.let(::parseTemplates).orEmpty()

            BusinessPreferencesState(
                plannedQuantities = jsonObjectToMap(json.optJSONObject("plannedQuantities")),
                templates = templates,
                selectedTemplateId = json.optString("selectedTemplateId"),
                currentPricing = parsePricing(json.optJSONObject("currentPricing"))
            )
        }.getOrNull()
    }

    private fun parseTemplates(array: JSONArray): List<BusinessTemplate> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    BusinessTemplate(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        pricing = parsePricing(item.optJSONObject("pricing"))
                    )
                )
            }
        }
    }

    private fun parsePricing(json: JSONObject?): BusinessPricingConfig {
        val legacyBreadUnitCost = json?.optString("breadUnitCost").orEmpty()
        val legacyProteinUnitCosts = jsonObjectToMap(json?.optJSONObject("proteinUnitCosts"))

        return BusinessPricingConfig(
            breadPackagePrice = json?.optString("breadPackagePrice")
                ?.takeIf { it.isNotBlank() }
                ?: legacyPackagePrice(
                    unitCost = legacyBreadUnitCost,
                    defaultUnits = DEFAULT_BREAD_UNITS_PER_PACKAGE
                ),
            breadUnitsPerPackage = json?.optString("breadUnitsPerPackage")
                ?.takeIf { it.isNotBlank() }
                ?: if (legacyBreadUnitCost.isNotBlank()) DEFAULT_BREAD_UNITS_PER_PACKAGE else "",
            proteinPackagePrices = completoTypes.map { it.proteinLabel }.distinct().associateWith { label ->
                jsonObjectToMap(json?.optJSONObject("proteinPackagePrices"))[label]
                    ?.takeIf { it.isNotBlank() }
                    ?: legacyPackagePrice(
                        unitCost = legacyProteinUnitCosts[label].orEmpty(),
                        defaultUnits = defaultProteinUnitsPerPackage[label].orEmpty()
                    )
            },
            proteinUnitsPerPackage = completoTypes.map { it.proteinLabel }.distinct().associateWith { label ->
                jsonObjectToMap(json?.optJSONObject("proteinUnitsPerPackage"))[label]
                    ?.takeIf { it.isNotBlank() }
                    ?: if (legacyProteinUnitCosts[label].orEmpty().isNotBlank()) {
                        defaultProteinUnitsPerPackage[label].orEmpty()
                    } else {
                        ""
                    }
            },
            ingredientCosts = jsonObjectToMap(json?.optJSONObject("ingredientCosts")),
            salePrices = jsonObjectToMap(json?.optJSONObject("salePrices"))
        )
    }

    private fun jsonObjectToMap(json: JSONObject?): Map<String, String> {
        if (json == null) return emptyMap()

        return buildMap {
            json.keys().forEach { key ->
                put(key, json.optString(key))
            }
        }
    }
}

fun defaultBusinessPricingConfig(): BusinessPricingConfig {
    return BusinessPricingConfig(
        breadPackagePrice = "2550",
        breadUnitsPerPackage = DEFAULT_BREAD_UNITS_PER_PACKAGE,
        proteinPackagePrices = mapOf(
            "Vienesas" to "2312",
            "Churrascos" to "9900"
        ),
        proteinUnitsPerPackage = defaultProteinUnitsPerPackage,
        ingredientCosts = mapOf(
            "Tomates" to "1190",
            "Paltas" to "3990",
            "Mayonesa" to "3557",
            "Chucrut" to "1690",
            "Salsa americana" to "1690",
            "Cebolla" to "800",
            "Huevos" to "5800",
            "Papas fritas" to "4000",
            "Porotos verdes" to "4000",
            "Ají verde" to "1000",
            "Carne de vacuno" to "8250",
            "Queso" to "8062"
        ),
        salePrices = mapOf(
            "italiano" to "3200",
            "dinamico" to "3500",
            "al_pobre" to "4900",
            "chacarero" to "3900",
            "as_luco" to "5500"
        )
    )
}

fun defaultBusinessTemplate(): BusinessTemplate =
    BusinessTemplate(
        id = DEFAULT_TEMPLATE_ID,
        name = DEFAULT_TEMPLATE_NAME,
        pricing = defaultBusinessPricingConfig()
    )

private fun legacyPackagePrice(unitCost: String, defaultUnits: String): String {
    val unitCostValue = unitCost.toIntOrNull() ?: return ""
    val unitsValue = defaultUnits.toIntOrNull() ?: return ""
    return (unitCostValue * unitsValue).toString()
}
