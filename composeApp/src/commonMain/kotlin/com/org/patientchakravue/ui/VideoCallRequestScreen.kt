package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallRequestScreen(
    onBack: () -> Unit,
    onStartCall: (channelName: String, doctorId: String) -> Unit = { _, _ -> }
) {
    var reason by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
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
                    // Generate a unique channel name based on timestamp
                    val channelName = "patient_call_${System.currentTimeMillis()}"
                    val doctorId = "default_doctor" // This would come from your backend in a real scenario
                    onStartCall(channelName, doctorId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.Videocam, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Video Call")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Note: Make sure you have a stable internet connection and your camera/microphone permissions are enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
