package com.org.patientchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
actual fun VideoCallScreen(
    channelName: String,
    doctorId: String,
    onCallEnded: () -> Unit
) {
    // iOS placeholder - Video calling not yet implemented for iOS
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Video calling is not available on iOS yet",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCallEnded) {
                Text("Go Back")
            }
        }
    }
}

