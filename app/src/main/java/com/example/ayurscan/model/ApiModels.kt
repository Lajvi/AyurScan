package com.example.ayurscan.model

data class FoodItem(
    val food_name: String,
    val food_type: String,
    val category: String,
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double,
    val fiber_g: Double,
    val calories_kcal: Double,
    val vitamins: String,
    val minerals: String,
    val health_tags: String,
    val vaat_suitable: String?,
    val pit_suitable: String?,
    val kapha_suitable: String?,
    val dosha_rating: Int?,
    val dosha_reason: String?,
    val recommendation_type: String?
)

data class RecommendationResponse(
    val body_type: String,
    val recommended_foods: List<FoodItem>,
    val total_found: Int,
    val message: String
)

data class MedicineItem(
    val medicine_name: String,
    val formulation_type: String,
    val rasa_taste: String,
    val virya_potency: String,
    val dosage: String,
    val anupana: String,
    val dosha_match_score: Int,
    val disease_relevance: Int,
    val therapeutic_uses: String,
    val reason: String,
    val fallback_note: String?
)

data class MedicineRecommendationResponse(
    val body_type: String,
    val disease: String,
    val recommended_medicines: List<MedicineItem>,
    val total_found: Int,
    val message: String
)
