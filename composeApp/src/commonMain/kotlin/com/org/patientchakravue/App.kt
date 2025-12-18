package com.org.patientchakravue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import patientchakravue.composeapp.generated.resources.Res
import patientchakravue.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
        MaterialTheme {
            val sessionManager = remember { SessionManager() }
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            // Check session
            var isLoggedIn by remember { mutableStateOf(sessionManager.getPatient() != null) }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) {
                if (isLoggedIn) {
                    val patient = sessionManager.getPatient()
                    if (patient != null) {
                        DashboardScreen(
                            patient = patient,
                            onLogout = {
                                sessionManager.clearSession()
                                isLoggedIn = false
                            },
                            showSnackbar = { msg ->
                                // Helper to show snackbar
                                // In real app, launch this in a coroutine
                            }
                        )
                    } else {
                        // Error state
                        Button(onClick = { isLoggedIn = false }) { Text("Session Error. Logout") }
                    }
                } else {
                    LoginScreen(
                        onLoginSuccess = { isLoggedIn = true },
                        showSnackbar = { msg ->
                            // scope.launch { snackbarHostState.showSnackbar(msg) }
                            // Note: Coroutine launch needed here
                        }
                    )
                }
            }
        }
    }
