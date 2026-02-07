package com.example.ayurscan.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ayurscan.R

@Composable
fun DetailsScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FBE7)) // Match background
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp),
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Icon(
                     imageVector = Icons.Filled.Favorite, 
                     contentDescription = "Favorite",
                     tint = Color(0xFFFF5252), // Red heart
                     modifier = Modifier
                         .size(40.dp)
                         .background(Color.White, CircleShape)
                         .padding(8.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FBE7)) // Light cream
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image
            Card(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF2196F3)), // Blue Border per mockup
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ayurfirstimage),
                    contentDescription = "Healthy Food",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append("Ayurveda ")
                        }
                        withStyle(style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)) {
                            append("(/ˌɑːjʊərˈveɪdə, -ˈviː-/; ")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                            append("IAST: āyurveda")
                        }
                        append(") is a traditional system of medicine with historical roots in the Indian subcontinent. It is based on the idea that disease is due to an imbalance or stress in a person's consciousness.")
                    },
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Authentic Principles",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E4E28)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ayurveda encourages certain lifestyle interventions and natural therapies to regain a balance between the body, mind, spirit, and the environment. It defines three fundamental bio-energies or Doshas: Vata, Pitta, and Kapha, which govern all physical and mental processes.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Key Takeaways / Authentic Info
                InfoCard(title = "Health & Balance", content = "Health is not merely the absence of disease, but a state of vibrant balance among the Doshas, Dhatus (tissues), and Malas (waste products).")
                Spacer(modifier = Modifier.height(8.dp))
                InfoCard(title = "Natural Healing", content = "Treatment focuses on removing the root cause of illness through diet, herbal remedies, and detoxification procedures like Panchakarma.")
                
                 Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InfoCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF8DA358))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = content, fontSize = 14.sp, color = Color.Gray)
        }
    }
}
