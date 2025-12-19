package com.org.patientchakravue.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import patientchakravue.composeapp.generated.resources.Login_bg
import patientchakravue.composeapp.generated.resources.Res

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager() }
    val api = remember { ApiRepository() }

    // State variables
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // THEME COLORS
    val ashColor = Color(0xFFC0C0C0)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. BACKGROUND IMAGE
        Image(
            painter = painterResource(Res.drawable.Login_bg), // KMP Resource
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        // 2. TRANSLUCENT LOGIN BOX
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.4f)
            ),
            border = BorderStroke(2.dp, ashColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Patient Login",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ashColor,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ashColor,
                        unfocusedBorderColor = ashColor.copy(alpha = 0.7f),
                        focusedLabelColor = ashColor,
                        unfocusedLabelColor = ashColor.copy(alpha = 0.7f),
                        cursorColor = ashColor,
                        focusedTextColor = ashColor,
                        unfocusedTextColor = ashColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = ashColor)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ashColor,
                        unfocusedBorderColor = ashColor.copy(alpha = 0.7f),
                        focusedLabelColor = ashColor,
                        unfocusedLabelColor = ashColor.copy(alpha = 0.7f),
                        cursorColor = ashColor,
                        focusedTextColor = ashColor,
                        unfocusedTextColor = ashColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Login Button
                if (isLoading) {
                    CircularProgressIndicator(color = ashColor)
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                showSnackbar("Fill all fields")
                                return@Button
                            }

                            isLoading = true
                            scope.launch {
                                try {
                                    val patient = api.login(email.trim(), password.trim())
                                    if (patient != null) {
                                        sessionManager.savePatient(patient)
                                        showSnackbar("Welcome back!")
                                        onLoginSuccess()
                                    } else {
                                        showSnackbar("Invalid credentials")
                                    }
                                } catch (e: Exception) {
                                    showSnackbar("Connection Error")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = ashColor
                        ),
                        border = BorderStroke(2.dp, ashColor),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
