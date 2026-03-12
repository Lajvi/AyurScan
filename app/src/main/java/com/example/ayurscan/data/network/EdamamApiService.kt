package com.example.ayurscan.data.network

import com.example.ayurscan.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

interface EdamamApiService {
    @GET("api/recipes/v2")
    suspend fun searchRecipes(
        @Query("type") type: String = "public",
        @Query("q") query: String,
        @Query("app_id") appId: String = BuildConfig.EDAMAM_APP_ID,
        @Query("app_key") appKey: String = BuildConfig.EDAMAM_APP_KEY,
        @Query("diet") diet: String? = null,
        @Query("health") health: String? = null,
        @Query("mealType") mealType: String? = null,
        @Query("random") random: Boolean = true
    ): RecipeResponse
}
