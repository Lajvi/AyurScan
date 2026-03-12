package com.example.ayurscan

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ayurscan.viewmodel.AuthState
import com.example.ayurscan.viewmodel.AuthViewModel
import com.example.ayurscan.viewmodel.FoodScannerViewModel
import com.example.ayurscan.data.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AyurScanApp()
        }
    }
}

@Composable
fun AyurScanApp(
    authViewModel: AuthViewModel = viewModel(),
    foodScannerViewModel: FoodScannerViewModel = viewModel()
) {

    val navController = rememberNavController()

    var doshaResult by remember { mutableStateOf("Vata") }

    val authState by authViewModel.authState.collectAsState()

    val repository = remember { FirestoreRepository() }

    val scope = rememberCoroutineScope()

    // Fetch user profile when logged in
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {

            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->

                repository.getUserProfile(uid)?.let { profile ->

                    doshaResult = profile.primaryDosha
                }
            }
        }
    }

    val startDest =
        if (authState is AuthState.Authenticated) "home" else "slide"

    val onBackToHome: () -> Unit = {

        navController.popBackStack("home", inclusive = false)
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {

        composable("slide") {

            com.example.ayurscan.ui.screens.SlideScreen(
                onStartJourneyClick = {

                    Log.d("AyurScanApp", "Button clicked")

                    navController.navigate("loginpage")
                }
            )
        }

        composable("loginpage") {

            com.example.ayurscan.ui.screens.LoginScreen(

                authViewModel = authViewModel,

                onSignInClick = {

                    navController.navigate("home") {

                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("home") {

            com.example.ayurscan.ui.screens.HomeScreen(

                onHomeClick = {},

                onHeartClick = {

                    navController.navigate("details")
                },

                onSmileClick = {

                    navController.navigate("score")
                },

                onBellClick = {

                    navController.navigate("quiz")
                },

                onScanClick = {

                    navController.navigate("scanner")
                },

                onLogout = {

                    authViewModel.logout()
                }
            )
        }

        composable("details") {

            com.example.ayurscan.ui.screens.DetailsScreen(
                onBack = onBackToHome
            )
        }

        composable("score") {

            com.example.ayurscan.ui.screens.ScoreScreen(

                doshaResult = doshaResult,

                onReattempt = {

                    navController.navigate("quiz") {

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

                    scope.launch {

                        FirebaseAuth.getInstance()
                            .currentUser
                            ?.uid
                            ?.let { uid ->

                                repository.updateUserDosha(
                                    uid,
                                    result
                                )
                            }
                    }

                    navController.navigate("score")
                },

                onBack = onBackToHome
            )
        }

        composable("scanner") {

            com.example.ayurscan.ui.screens.ScannerScreen(

                userDosha = doshaResult,

                viewModel = foodScannerViewModel,

                onBack = onBackToHome
            )
        }
    }
}