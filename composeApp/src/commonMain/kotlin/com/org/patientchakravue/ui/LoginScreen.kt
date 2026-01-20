package com.org.patientchakravue.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.platform.registerFcmTokenAfterLogin
import kotlinx.coroutines.launch

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

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dark Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )

            // 2. LANGUAGE SWITCHER - TOP RIGHT CORNER
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 44.dp, end = 16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    LanguageSwitcherIcon(tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            // 2. TRANSLUCENT LOGIN BOX
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f) // 20% translucent white
                    ),
                    border = BorderStroke(2.dp, Color(0xFF00D25B)), // Brand green border (from image)
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = localizedString("login_title"),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2979FF), // Blue from image
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = localizedString("login_caption"),
                            color = Color(0xFF757575), // Subtle gray for caption
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(localizedString("email_label")) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                cursorColor = MaterialTheme.colorScheme.onBackground,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(localizedString("password_label")) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                cursorColor = MaterialTheme.colorScheme.onBackground,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Login Button
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            val loginErrorEmpty = localizedString("login_error_empty")
                            val loginSuccess = localizedString("login_success")
                            val loginErrorInvalid = localizedString("login_error_invalid")
                            val errorConnection = localizedString("error_connection")
                            val loginButtonText = localizedString("login_button")

                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        showSnackbar(loginErrorEmpty)
                                        return@Button
                                    }

                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val patient = api.login(email.trim(), password.trim())
                                            if (patient != null) {
                                                sessionManager.savePatient(patient)
                                                registerFcmTokenAfterLogin(patient.id)
                                                showSnackbar(loginSuccess)
                                                onLoginSuccess()
                                            } else {
                                                showSnackbar(loginErrorInvalid)
                                            }
                                        } catch (e: Exception) {
                                            showSnackbar(errorConnection)
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00D25B), // Green from image
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp) // Fully rounded corners as in image
                            ) {
                                Text(loginButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
