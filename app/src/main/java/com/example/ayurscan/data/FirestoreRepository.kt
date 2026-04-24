package com.example.ayurscan.data

import com.example.ayurscan.model.FoodScanRecord
import com.example.ayurscan.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val scansCollection = db.collection("scans")

    suspend fun saveUserProfile(profile: UserProfile) {
        usersCollection.document(profile.uid).set(profile).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserDosha(uid: String, dosha: String) {
        try {
            usersCollection.document(uid).set(mapOf("primaryDosha" to dosha), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            // Ignore error so it doesn't crash the app
        }
    }

    suspend fun saveFoodScan(record: FoodScanRecord) {
        val docRef = scansCollection.document()
        val recordWithId = record.copy(scanId = docRef.id)
        docRef.set(recordWithId).await()
    }

    suspend fun getRecentScans(userId: String): List<FoodScanRecord> {
        return try {
            val snapshot = scansCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            snapshot.toObjects(FoodScanRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
