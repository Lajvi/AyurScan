package com.example.ayurscan.data.network

data class RecipeResponse(
    val from: Int,
    val to: Int,
    val count: Int,
    val hits: List<Hit>
)

data class Hit(
    val recipe: Recipe
)

data class Recipe(
    val uri: String,
    val label: String,
    val image: String,
    val source: String,
    val url: String,
    val yield: Float,
    val dietLabels: List<String>,
    val healthLabels: List<String>,
    val cautions: List<String>,
    val ingredientLines: List<String>,
    val ingredients: List<Ingredient>,
    val calories: Double,
    val totalWeight: Double,
    val cuisineType: List<String>?,
    val mealType: List<String>?,
    val dishType: List<String>?,
    val totalNutrients: Map<String, NutrientInfo>
)

data class Ingredient(
    val text: String,
    val quantity: Double,
    val measure: String?,
    val food: String,
    val weight: Double,
    val foodCategory: String?,
    val foodId: String,
    val image: String?
)

data class NutrientInfo(
    val label: String,
    val quantity: Double,
    val unit: String
)
