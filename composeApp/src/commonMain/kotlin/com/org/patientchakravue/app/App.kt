package com.org.patientchakravue.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.ui.*
import kotlinx.coroutines.launch

@Composable
fun App(initialCallData: Pair<String, String>? = null) {
    MaterialTheme {
        AppLocalizationProvider {
            val sessionManager = remember { SessionManager() }
            val initialScreen = when {
                initialCallData != null -> Screen.VideoCall(initialCallData.first, initialCallData.second)
                sessionManager.getPatient() != null -> Screen.Dashboard
                else -> Screen.Login
            }
            val navigator = remember { Navigator(initialScreen) }
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            // Show back handler on any screen that is NOT a root screen
            if (navigator.currentScreen !in listOf(Screen.Dashboard, Screen.Login)) {
                AppBackHandler { navigator.goBack() }
            }

            val bottomNavScreens = listOf(Screen.Dashboard, Screen.AfterCare, Screen.Vision, Screen.Notifications)

            Scaffold(
                modifier = Modifier.statusBarsPadding(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (navigator.currentScreen in bottomNavScreens) {
                        BottomNavigationBar(navigator)
                    }
                }
            ) { paddingValues ->
                when (val screen = navigator.currentScreen) {
                    is Screen.Login -> LoginScreen(
                        onLoginSuccess = { navigator.navigateAsPillar(Screen.Dashboard) },
                        showSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                    is Screen.Dashboard -> {
                        val patient = sessionManager.getPatient()
                        if (patient != null) {
                            DashboardScreen(
                                patient = patient,
                                onNavigateToProfile = { navigator.navigateForward(Screen.Profile) },
                                onNavigateToAdherence = { navigator.navigateForward(Screen.AdherenceGraph) },
                                onNavigateToMedicineList = { navigator.navigateForward(Screen.MedicineList) },
                                bottomBar = { BottomNavigationBar(navigator) }
                            )
                        } else {
                            sessionManager.clearSession()
                            navigator.navigateAsPillar(Screen.Login)
                        }
                    }
                    is Screen.Profile -> ProfileScreen(
                        sessionManager = sessionManager,
                        onBack = { navigator.goBack() },
                        onLogout = {
                            sessionManager.clearSession()
                            navigator.navigateAsPillar(Screen.Login)
                        }
                    )
                    is Screen.AdherenceGraph -> AdherenceGraphScreen(onBack = { navigator.goBack() })
                    is Screen.AfterCare -> {
                         val patient = sessionManager.getPatient()
                         if (patient != null) {
                            AfterCareScreen(
                                patient = patient,
                                onBack = { navigator.goBack() },
                                showSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                contentPadding = paddingValues
                            )
                         }
                    }
                    is Screen.Vision -> {
                        val patient = sessionManager.getPatient()
                        if (patient != null) {
                            VisionScreen(
                                patient = patient,
                                contentPadding = paddingValues,
                                onNavigateToAmsler = { navigator.navigateForward(Screen.AmslerGrid) },
                                onNavigateToTumblingE = { navigator.navigateForward(Screen.TumblingE) }
                            )
                        }
                    }
                    is Screen.AmslerGrid -> {
                        val patient = sessionManager.getPatient()
                        if (patient != null) {
                            AmslerTestScreen(
                                patient = patient,
                                onBack = { navigator.goBack() },
                                showSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    }
                    is Screen.TumblingE -> {
                         val patient = sessionManager.getPatient()
                         if (patient != null) {
                            TumblingETestScreen(
                                patient = patient,
                                onBack = { navigator.goBack() },
                                showSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                         }
                    }
                    is Screen.Notifications -> {
                        val patient = sessionManager.getPatient()
                        if (patient != null) {
                            NotificationsScreen(
                                patient = patient,
                                onNavigateToChat = { doctorId, doctorName, submissionIds ->
                                    navigator.navigateForward(Screen.Chat(doctorId, doctorName, submissionIds))
                                },
                                bottomBar = { BottomNavigationBar(navigator) }
                            )
                        }
                    }
                    is Screen.Chat -> ChatScreen(
                        doctorName = screen.doctorName,
                        submissionIds = screen.submissionIds,
                        onBack = { navigator.goBack() }
                    )
                    is Screen.VideoCallRequest -> VideoCallRequestScreen(
                        onBack = { navigator.goBack() },
                        onStartCall = { channelName, doctorId ->
                            navigator.navigateForward(Screen.VideoCall(channelName, doctorId))
                        }
                    )
                    is Screen.VideoCall -> {
                        val callScreen = screen as Screen.VideoCall
                        VideoCallScreen(
                            channelName = callScreen.channelName,
                            doctorId = callScreen.doctorId,
                            onCallEnded = { navigator.goBack() }
                        )
                    }
                    is Screen.FeedbackDetail -> FeedbackDetailScreen(note = screen.note, onBack = { navigator.goBack() })
                    is Screen.MedicineList -> Text("Medicine List Screen", modifier = Modifier.padding(paddingValues))
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navigator: Navigator) {
    Box {
        NavigationBar(
            modifier = Modifier.height(80.dp)
        ) {
            // Dashboard - Left side
            NavigationBarItem(
                icon = { Icon(Icons.Default.Dashboard, null) },
                label = { Text("Dashboard", fontSize = 10.sp, maxLines = 1) },
                selected = navigator.currentScreen == Screen.Dashboard,
                onClick = { navigator.navigateAsPillar(Screen.Dashboard) },
                modifier = Modifier.weight(1f)
            )

            // AfterCare - Left of center
            NavigationBarItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                label = { Text("AfterCare", fontSize = 10.sp, maxLines = 1) },
                selected = navigator.currentScreen == Screen.AfterCare,
                onClick = { navigator.navigateAsPillar(Screen.AfterCare) },
                modifier = Modifier.weight(1f)
            )

            // Empty space for FAB in center
            Box(modifier = Modifier.weight(1f))

            // Vision - Right of center
            NavigationBarItem(
                icon = { Icon(Icons.Default.RemoveRedEye, null) },
                label = { Text("Vision", fontSize = 10.sp, maxLines = 1) },
                selected = navigator.currentScreen == Screen.Vision,
                onClick = { navigator.navigateAsPillar(Screen.Vision) },
                modifier = Modifier.weight(1f)
            )

            // Notifications - Right side
            NavigationBarItem(
                icon = { Icon(Icons.Default.Notifications, null) },
                label = { Text("Notifications", fontSize = 10.sp, maxLines = 1) },
                selected = navigator.currentScreen == Screen.Notifications,
                onClick = { navigator.navigateAsPillar(Screen.Notifications) },
                modifier = Modifier.weight(1f)
            )
        }

        // Floating Video Call Button in center
        FloatingActionButton(
            onClick = { navigator.navigateForward(Screen.VideoCallRequest) },
            containerColor = Color(0xFF4CAF50),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Videocam, null, tint = Color.White)
        }
    }
}
