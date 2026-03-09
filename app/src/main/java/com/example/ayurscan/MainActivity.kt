package com.example.ayurscan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ayurscan.viewmodel.AuthState
import com.example.ayurscan.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AyurScanApp() // Call the composable that handles navigation
        }
    }
}

@Composable
fun AyurScanApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    // State to hold the result of the quiz
    var doshaResult by remember { mutableStateOf("Vata") } // Default value
    val authState by authViewModel.authState.collectAsState()

    // Determine the start destination based on Auth State
    val startDest = if (authState is AuthState.Authenticated) "home" else "slide"

    // Common Back Navigation Logic
    val onBackToHome: () -> Unit = {
        navController.popBackStack("home", inclusive = false)
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("slide") {
            com.example.ayurscan.ui.screens.SlideScreen(onStartJourneyClick = {
                Log.d("AyurScanApp", "Button clicked")
                navController.navigate("loginpage")
            })
        }
        composable("loginpage") {
            com.example.ayurscan.ui.screens.LoginScreen(
                authViewModel = authViewModel,
                onSignInClick = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true } // Clear back stack on login
                    }
                }
            )
        }
        composable("home") {
            com.example.ayurscan.ui.screens.HomeScreen(
                onHomeClick = { /* Already on Home */ },
                onHeartClick = { navController.navigate("details") },
                onSmileClick = { navController.navigate("score") },
                onBellClick = { navController.navigate("quiz") }
            )
        }
        composable("details") {
            com.example.ayurscan.ui.screens.DetailsScreen(onBack = onBackToHome)
        }
        composable("score") {
             // Pass the stored result and navigation callback
            com.example.ayurscan.ui.screens.ScoreScreen(
                doshaResult = doshaResult,
                onReattempt = {
                    navController.navigate("quiz") {
                        // Pop up to home to avoid back stack loop accumulation
                        popUpTo("home") { saveState = true }
                    }
                },
                onBack = onBackToHome
            )
        }
        composable("quiz") {
            com.example.ayurscan.ui.screens.QuizScreen(
                onQuizComplete = { result ->
                    doshaResult = result
                    navController.navigate("score")
                },
                onBack = onBackToHome
            )
        }
    }
}
