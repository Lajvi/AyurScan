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
import com.google.firebase.auth.FirebaseAuth

sealed class ScannerState {
    object Idle : ScannerState()
    object Loading : ScannerState()
    data class Success(val result: String) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

class FoodScannerViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
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
            1. Identify the food.
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
                    _scannerState.value = ScannerState.Success(responseText)
                    // Save to Firestore
                    auth.currentUser?.uid?.let { uid ->
                        val record = FoodScanRecord(
                            userId = uid,
                            foodName = textQuery ?: "Unknown Food",
                            doshicAnalysis = responseText
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

    fun resetState() {
        _scannerState.value = ScannerState.Idle
    }
}
