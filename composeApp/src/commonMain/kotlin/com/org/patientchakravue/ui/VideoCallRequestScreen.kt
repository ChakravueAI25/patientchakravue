package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoCallRequestScreen(
    onBack: () -> Unit,
    onRequestSent: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues()
) {
    var reason by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionManager = remember { SessionManager() }
    val apiRepository = remember { ApiRepository() }
    val patient = sessionManager.getPatient()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Header row: back arrow + title/subtitle (same line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1A3B5D),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Request Call",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A3B5D)
                    )
                    Text(
                        "Explain your concern to your doctor",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                // Spacer to keep title centered (balances IconButton width)
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for call") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                enabled = !isLoading
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (reason.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a reason for the call request")
                        }
                        return@Button
                    }
                    if (patient == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please login to send a call request")
                        }
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        // Send video call request to backend
                        val success = apiRepository.sendVideoCallRequest(patient.id, reason)
                        isLoading = false
                        if (success) {
                            requestSent = true
                            snackbarHostState.showSnackbar("Call request sent to your doctor!")
                            delay(1000)
                            onRequestSent()
                        } else {
                            snackbarHostState.showSnackbar("Failed to send request. Please try again.")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = !isLoading && !requestSent
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (requestSent) {
                    Text("Request Sent ✓")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Send Call Request")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Note: Your doctor will receive your request and may call you back when available.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
