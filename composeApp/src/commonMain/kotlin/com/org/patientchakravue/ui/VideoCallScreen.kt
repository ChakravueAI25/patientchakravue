package com.org.patientchakravue.ui

import androidx.compose.runtime.Composable

/**
 * Platform-specific Video Call Screen.
 * Android uses Agora SDK for real-time video.
 * iOS can provide its own implementation or a placeholder.
 */
@Composable
expect fun VideoCallScreen(
    channelName: String,
    doctorId: String,
    onCallEnded: () -> Unit
)

