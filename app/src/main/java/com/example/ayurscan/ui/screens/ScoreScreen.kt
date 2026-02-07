package com.example.ayurscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreScreen(
    doshaResult: String = "Vata", 
    onReattempt: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val doshaInfo = getDoshaInfo(doshaResult)
    var points by remember { mutableStateOf(1) }
    var streak by remember { mutableStateOf(1) }
    
    // State for Dialog
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var tempValue by remember { mutableStateOf("") }
    var onDialogConfirm: (Int) -> Unit by remember { mutableStateOf({}) }

    if (showDialog) {
        EditValueDialog(
            title = dialogTitle,
            initialValue = tempValue,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onDialogConfirm(newValue)
                showDialog = false
            }
        )
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { /* Handle Achievements */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), // Blue
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
            ) {
                Text("Achievements", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FBE7)) // Light cream/greenish bg
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(30.dp))
                }
                // Exact title from image
                Text("Fit Check Score", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Points Card
            StatsCard(
                title = "Points Earned", 
                color = Color(0xFFFBC02D), // Yellow/Gold
                iconVector = Icons.Filled.Star,
                value = points,
                onIncrease = { points++ },
                onDecrease = { if (points > 0) points-- },
                onEdit = {
                    dialogTitle = "Edit Points"
                    tempValue = points.toString()
                    onDialogConfirm = { points = it }
                    showDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Streak Card
            StatsCard(
                title = "Streak Count", 
                color = Color(0xFFFFB74D), // Orange
                iconVector = Icons.Filled.ThumbUp, 
                value = streak,
                onIncrease = { streak++ },
                onDecrease = { if (streak > 0) streak-- },
                onEdit = {
                    dialogTitle = "Edit Streak"
                    tempValue = streak.toString()
                    onDialogConfirm = { streak = it }
                    showDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Suggestions Title (Green and decorative style)
            Text(
                text = "Suggestions",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF7D9E46) // Green
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filtered Suggestions based on Dosha
            doshaInfo.recommendations.forEach { recommendation ->
                SuggestionItem(text = recommendation)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Re-attempt link
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                 TextButton(onClick = onReattempt) {
                     Text("Retake Assessment", color = Color(0xFF7D9E46))
                 }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatsCard(
    title: String, 
    color: Color, 
    iconVector: androidx.compose.ui.graphics.vector.ImageVector, 
    value: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Counter & Edit
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                     Row(
                        modifier = Modifier
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDecrease, modifier = Modifier.size(20.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Decrease", tint = Color.Black)
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(value.toString(), fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        IconButton(onClick = onIncrease, modifier = Modifier.size(20.dp)) {
                             Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                         Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                         Spacer(modifier = Modifier.width(4.dp))
                         Text("Edit", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EditValueDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.all { char -> char.isDigit() }) text = it },
                label = { Text("Enter Value") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val number = text.toIntOrNull()
                if (number != null) {
                    onConfirm(number)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class DoshaResultInfo(
    val description: String,
    val recommendations: List<String>
)

fun getDoshaInfo(result: String): DoshaResultInfo {
    return when {
        result.contains("Vata") && result.contains("Pitta") -> DoshaResultInfo(
            "You have a dual constitution of Vata and Pitta. You are creative and energetic but can get easily burnt out.",
            listOf("Eat warm, grounding foods", "Avoid spicy foods", "Practice meditation", "Keep a regular routine")
        )
        result.contains("Vata") && result.contains("Kapha") -> DoshaResultInfo(
            "You have a dual constitution of Vata and Kapha.",
            listOf("Eat warm, light foods", "Avoid heavy dairy", "Exercise regularly", "Stay warm")
        )
        result.contains("Pitta") && result.contains("Kapha") -> DoshaResultInfo(
            "You have a dual constitution of Pitta and Kapha.",
            listOf("Eat cooling, light foods", "Avoid oily/fried foods", "Engage in moderate exercise", "Stay hydrated")
        )
        result.contains("Vata") -> DoshaResultInfo(
            "Vata is characterized by the properties of dry, cold, light, minute, and movement. Balances air and ether.",
            listOf("Favor warm soups and stews", "Eat cooked grains like OATS and RICE", "Use warming spices like GINGER", "Avoid dry/cold snacks like popcorn")
        )
        result.contains("Pitta") -> DoshaResultInfo(
            "Pitta governs metabolism and transformation. Associated with fire and water.",
            listOf("Favor cooling foods like MELONS", "Eat green leafy salads", "Avoid hot spices like CHILI", "Drink cool water, avoid alcohol")
        )
        result.contains("Kapha") -> DoshaResultInfo(
            "Kapha governs structure and fluid balance. Associated with earth and water.",
            listOf("Favor light, dry foods like BEANS", "Eat spicy foods to stimulate digestion", "Avoid heavy dairy and sweets", "Exercise vigorously")
        )
        else -> DoshaResultInfo("Unknown Dosha", emptyList())
    }
}

@Composable
fun SuggestionItem(text: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text, fontWeight = FontWeight.Medium, color = Color.Black, fontSize = 16.sp)
        }
    }
}
