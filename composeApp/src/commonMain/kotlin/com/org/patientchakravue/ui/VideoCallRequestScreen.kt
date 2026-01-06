package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallRequestScreen(
    onBack: () -> Unit,
    onRequestSent: () -> Unit = {}
) {
    var reason by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Request Video Call") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for video call") },
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
                    isLoading = true
                    scope.launch {
                        // Simulate sending request to backend
                        delay(1500)
                        isLoading = false
                        requestSent = true
                        snackbarHostState.showSnackbar("Video call request sent to your doctor!")
                        delay(1000)
                        onRequestSent()
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
                    Text("Send Video Call Request")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Note: Your doctor will receive your request and may call you back when available.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
