package com.example.ayurscan.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ayurscan.data.network.Recipe
import com.example.ayurscan.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Fetch default recommendations
        fetchRecipes("Ayurvedic")
    }

    fun fetchRecipes(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // To get diverse results, we will modify the query based on dosha selected
                val searchParam = if (query == "Ayurvedic") "Ayurvedic" else "Ayurvedic $query"
                val response = RetrofitClient.instance.searchRecipes(query = searchParam)
                _recipes.value = response.hits.map { it.recipe }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch recipes: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
