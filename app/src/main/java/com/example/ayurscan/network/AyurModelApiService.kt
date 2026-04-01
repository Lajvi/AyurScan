package com.example.ayurscan.network

import com.example.ayurscan.model.MedicineRecommendationResponse
import com.example.ayurscan.model.RecommendationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AyurModelApiService {
    @GET("api/recommend")
    suspend fun getFoodRecommendations(
        @Query("body_type") bodyType: String,
        @Query("limit") limit: Int = 12
    ): Response<RecommendationResponse>

    @GET("api/recommend/medicine")
    suspend fun getMedicineRecommendations(
        @Query("body_type") bodyType: String,
        @Query("disease") disease: String,
        @Query("limit") limit: Int = 8
    ): Response<MedicineRecommendationResponse>
}
