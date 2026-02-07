package com.example.ayurscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Question(
    val id: Int,
    val text: String,
    val options: List<String>
)

@Composable
fun QuizScreen(
    onQuizComplete: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val questions = remember {
        listOf(
            Question(1, "Body Frame", listOf("Slim, light, find it hard to gain weight", "Medium build, gain/lose weight easily", "Broad, stocky, gain weight easily")),
            Question(2, "Skin Type", listOf("Dry, rough, cold", "Warm, oily, prone to rashes", "Smooth, soft, oily, cool")),
            Question(3, "Hair", listOf("Dry, frizzy, brittle", "Fine, straight, tends to gray early", "Thick, wavy, oily")),
            Question(4, "Appetite", listOf("Variable; sometimes skip meals", "Strong hunger; get irritable if not fed", "Slow digestion; can skip meals without discomfort")),
            Question(5, "Sleep Pattern", listOf("Light sleeper, wake up easily", "Moderate sleeper", "Deep sleeper, hard to wake up")),
            Question(6, "Energy Levels", listOf("Burst of energy, then fatigue", "Steady and intense energy", "Slow and steady, lasting energy")),
            Question(7, "Climate Preference", listOf("Prefer warmth, dislike cold", "Prefer cool climate, dislike heat", "Prefer warmth, dislike damp/cold"))
        )
    }

    // State to track selected options (QuestionId -> OptionIndex)
    val selectedOptions = remember { mutableStateMapOf<Int, Int>() }

    fun calculateResult(): String {
        var vataScore = 0
        var pittaScore = 0
        var kaphaScore = 0

        selectedOptions.values.forEach { optionIndex ->
            when (optionIndex) {
                0 -> vataScore++
                1 -> pittaScore++
                2 -> kaphaScore++
            }
        }

        return when {
            vataScore >= pittaScore && vataScore >= kaphaScore -> {
                if (vataScore == pittaScore) "Vata-Pitta"
                else if (vataScore == kaphaScore) "Vata-Kapha"
                else "Vata"
            }
            pittaScore >= vataScore && pittaScore >= kaphaScore -> {
                if (pittaScore == kaphaScore) "Pitta-Kapha"
                else "Pitta"
            }
            else -> "Kapha"
        }
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { selectedOptions.clear() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), // Blue
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
            ) {
                Text("Re-attempt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FBE7)) // Light cream
        ) {
             // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(30.dp))
                }
                Text("Quiz", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(30.dp))
            }

            Text(
                text = "Find your Ayurvedic Dosha",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E4E28),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(questions) { question ->
                    QuestionCard(
                        question = question,
                        selectedOptionIndex = selectedOptions[question.id],
                        onOptionSelected = { index -> selectedOptions[question.id] = index }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedOptions.size == questions.size) {
                                val result = calculateResult()
                                onQuizComplete(result)
                            }
                        },
                        enabled = selectedOptions.size == questions.size,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4E7040),
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun QuestionCard(
    question: Question,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${question.id}. ${question.text}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF2E4E28)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val optionsLabels = listOf("A", "B", "C")
            question.options.forEachIndexed { index, optionText ->
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(vertical = 4.dp)
                       .background(
                           if (selectedOptionIndex == index) Color(0xFFE8F5E9) else Color.Transparent,
                           RoundedCornerShape(8.dp)
                       )
                       .clickable { onOptionSelected(index) }
                       .padding(8.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedOptionIndex == index),
                        onClick = { onOptionSelected(index) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4E7040))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${optionsLabels[index]}. $optionText",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
