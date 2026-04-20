package com.example.ayurscan.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ayurscan.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.ayurscan.data.FirestoreRepository
import com.example.ayurscan.model.FoodScanRecord
import com.example.ayurscan.model.FoodItem
import com.example.ayurscan.model.MedicineItem
import com.example.ayurscan.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth

sealed class ScannerState {
    object Idle : ScannerState()
    object Loading : ScannerState()
    data class Success(val result: String) : ScannerState()
    data class FoodRecommendationSuccess(val foods: List<FoodItem>) : ScannerState()
    data class MedicineRecommendationSuccess(val medicines: List<MedicineItem>) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

class FoodScannerViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val repository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val scannerState: StateFlow<ScannerState> = _scannerState

    fun analyzeFood(userDosha: String, textQuery: String?, image: Bitmap?) {
        if (textQuery.isNullOrBlank() && image == null) {
            _scannerState.value = ScannerState.Error("Please provide an image or type a food name.")
            return
        }

        _scannerState.value = ScannerState.Loading

        val prompt = """
            You are an expert Ayurvedic practitioner. A user whose primary Dosha is **$userDosha** is asking about a specific food.
            
            Based on the provided ${if (image != null) "image and/or " else ""}text:
            1. Identify the food. MANDATORY: At the very beginning of your response, output ONLY the exact, simple name of the food enclosed in brackets like this: [FOOD_NAME: Apple] or [FOOD_NAME: Almonds].
            2. What are the doshic qualities of this food (does it aggravate or pacify Vata, Pitta, and Kapha)?
            3. Is it good for this specific user's Dosha ($userDosha)? 
            4. Provide dietary advice: how much should they consume, and are there specific preparation methods (e.g., add warming spices, eat raw vs. cooked) to make it better for them?
            
            Keep your response concise, structured with bullet points, and highly encouraging.
        """.trimIndent()

        viewModelScope.launch {
            try {
                val inputContent = content {
                    if (image != null) {
                        image(image)
                    }
                    if (!textQuery.isNullOrBlank()) {
                        text("User provided context/name: $textQuery")
                    }
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text
                if (responseText != null) {
                    var finalResponse = responseText

                    // Extract food name
                    val foodNameRegex = "\\[FOOD_NAME:\\s*(.*?)\\]".toRegex()
                    val matchResult = foodNameRegex.find(responseText)
                    var queryFoodName = textQuery
                    if (matchResult != null) {
                        val foodName = matchResult.groupValues[1].trim()
                        queryFoodName = foodName
                        // Strip the tag from the final response to keep it clean
                        finalResponse = finalResponse.replace(matchResult.value, "").trim()

                        // Fetch detailed dosha info from our Ayur model API
                        try {
                            val apiResponse = RetrofitClient.apiService.getFoodDetails(foodName)
                            if (apiResponse.isSuccessful) {
                                val foodDetails = apiResponse.body()
                                if (foodDetails != null) {
                                    val apiInsights = "\n\n**Integration Insights from Ayur Model for $foodName:**\n" +
                                            "• **Vata Suitable:** ${foodDetails.vaat_suitable ?: "Not specified"}\n" +
                                            "• **Pitta Suitable:** ${foodDetails.pit_suitable ?: "Not specified"}\n" +
                                            "• **Kapha Suitable:** ${foodDetails.kapha_suitable ?: "Not specified"}\n"
                                    finalResponse += apiInsights
                                }
                            }
                        } catch (e: Exception) {
                            // Backend might be unavailable or food not found in DB
                            // We gracefully fallback to just showing the Gemini analysis
                        }
                    }

                    _scannerState.value = ScannerState.Success(finalResponse)
                    // Save to Firestore
                    auth.currentUser?.uid?.let { uid ->
                        val record = FoodScanRecord(
                            userId = uid,
                            foodName = queryFoodName ?: "Unknown Food",
                            doshicAnalysis = finalResponse
                        )
                        repository.saveFoodScan(record)
                    }
                } else {
                    _scannerState.value = ScannerState.Error("AI returned an empty response.")
                }
            } catch (e: Exception) {
               _scannerState.value = ScannerState.Error("Analysis failed: ${e.localizedMessage}")
            }
        }
    }

    fun fetchFoodRecommendations(userDosha: String) {
        _scannerState.value = ScannerState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFoodRecommendations(bodyType = userDosha.lowercase())
                if (response.isSuccessful && response.body() != null) {
                    _scannerState.value = ScannerState.FoodRecommendationSuccess(response.body()!!.recommended_foods)
                } else {
                    _scannerState.value = ScannerState.Error("Failed to fetch food recommendations.")
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    fun fetchMedicineRecommendations(userDosha: String, disease: String) {
        _scannerState.value = ScannerState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMedicineRecommendations(bodyType = userDosha.lowercase(), disease = disease)
                if (response.isSuccessful && response.body() != null) {
                    _scannerState.value = ScannerState.MedicineRecommendationSuccess(response.body()!!.recommended_medicines)
                } else {
                    _scannerState.value = ScannerState.Error("Failed to fetch medicine recommendations.")
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _scannerState.value = ScannerState.Idle
    }
}
