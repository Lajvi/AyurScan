package com.example.ayurscan.model

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val primaryDosha: String = "Vata",
    val createdAt: Timestamp = Timestamp.now()
)

data class FoodScanRecord(
    val scanId: String = "",
    val userId: String = "",
    val foodName: String = "",
    val doshicAnalysis: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class DailyActivity(
    val activityId: String = "",
    val userId: String = "",
    val type: String = "",
    val notes: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
