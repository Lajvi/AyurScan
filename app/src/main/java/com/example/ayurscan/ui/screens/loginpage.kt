package com.example.ayurscan.ui.screens

import android.widget.Toast
import androidx.compose.ui.platform.LocalContextimport androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ayurscan.R
import com.example.ayurscan.viewmodel.AuthState
import com.example.ayurscan.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onSignInClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    
    val authState = authViewModel.authState.collectAsState()
    val context = LocalContext.current
    
    // Navigate on successful auth
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Authenticated -> onSignInClick()
            is AuthState.Error -> {
                Toast.makeText(context, (authState.value as AuthState.Error).message, Toast.LENGTH_LONG).show()
                // Avoid repeated showing if recomposed, although typically state moves back to unauth or stays error
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background Image - Using a placeholder or the provided image if available
        // Since we don't have the exact background image from the user as a file yet, we use a color/gradient
        // that matches the green theme from the screenshot.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4E7040)) // Dark Greenish background like the top of the image
        ) {
            // Optional: If you have the bg image resource, use Image(...) here
            // For now, let's use a blurred or solid overlay to match text visibility
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSignUp) "SIGN UP" else "LOGIN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black, // Or Dark Green/Black based on contrast
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username/ E-mail") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Remember Me & Forgot Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox placeholder (using a simple Box for visual)
                    // In a real app use Checkbox
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.dp, Color.Black, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remember Me", fontSize = 12.sp, color = Color.White)
                }
                Text(
                    "Forgot Password...",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign In Button
            Button(
                onClick = {
                    if (isSignUp) {
                        authViewModel.signup(username, password)
                    } else {
                        authViewModel.login(username, password)
                    }
                },
                enabled = authState.value != AuthState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E7040)), // Dark Green
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(50.dp)
                    .width(150.dp)
                    .border(1.dp, Color.White, RoundedCornerShape(8.dp))
            ) {
                if (authState.value == AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isSignUp) "Sign Up" else "Sign In", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OR Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White)
                Text(" or ", color = Color.White)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Login Buttons
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                // Icon would be here
                Text("Continue with Google", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                // Icon would be here
                Text("Continue with Apple", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                if (isSignUp) "Already have an account?" else "Don't have an account?",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                if (isSignUp) "Log in" else "Sign up",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { isSignUp = !isSignUp }
            )
        }
    }
}
