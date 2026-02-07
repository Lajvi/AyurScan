package com.example.ayurscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ayurscan.R


@Composable
fun SlideScreen(
    onStartJourneyClick: () -> Unit
) {
    // Animation States
    var isVisible by remember { mutableStateOf(false) }
    
    // Trigger animation on launch
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Pulse Animation for Button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5D7052), // Deep Sage
                        Color(0xFFE6EAD6), // Lighter Sage/Cream
                        Color(0xFFF1F3E0)  // Soft Cream
                    )
                )
            )
    ) {
        // Decorative background elements (optional, kept simple for now)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Animation
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(animationSpec = tween(1000))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logoscan),
                    contentDescription = "AyurScan Logo",
                    modifier = Modifier
                        .size(140.dp) // Slightly larger
                        .padding(bottom = 16.dp)
                )
            }

            // Tagline Animation
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -20 }) + fadeIn(animationSpec = tween(1000, delayMillis = 300))
            ) {
                Text(
                    text = "Ancient Wisdom,\nModern Wellness.",
                    fontSize = 28.sp,
                    color = Color(0xFF1E331A), // Darker Green for contrast
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.1f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }

            // Herbs Image Animation
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(initialScale = 0.8f) + fadeIn(animationSpec = tween(1000, delayMillis = 600))
            ) {
                androidx.compose.material3.Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp) // Taller image
                        .padding(horizontal = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ayurfirstimage),
                        contentDescription = "Ayurvedic Herbs",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // Button Animation
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(animationSpec = tween(1000, delayMillis = 900))
            ) {
                Button(
                    onClick = {
                        onStartJourneyClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD68C45), // Premium Earthy Orange/Gold
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50), 
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp)
                        .scale(scale) // Apply pulse effect
                ) {
                    Text(
                        text = "Start Your Journey",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
