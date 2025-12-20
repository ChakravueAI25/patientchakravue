package com.org.patientchakravue.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.ui.*
import kotlinx.coroutines.launch

@Composable
fun App() {
    MaterialTheme {
        val sessionManager = remember { SessionManager() }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val initialScreen = if (sessionManager.getPatient() != null) Screen.Dashboard else Screen.Login
        val navigator = remember { Navigator(initialScreen) }

        val bottomNavScreens = listOf(Screen.Dashboard, Screen.AfterCare, Screen.Vision, Screen.Notifications)

        AppBackHandler {
            navigator.handleBackIntent()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (navigator.currentScreen in bottomNavScreens) {
                    BottomNavigationBar(navigator)
                }
            }
        ) { paddingValues ->
            when (navigator.currentScreen) {
                is Screen.Login -> {
                    LoginScreen(
                        onLoginSuccess = {
                            navigator.navigateTo(Screen.Dashboard, clearBackStack = true)
                        },
                        showSnackbar = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
                is Screen.Dashboard -> {
                    val patient = sessionManager.getPatient()
                    if (patient != null) {
                        DashboardScreen(
                            patient = patient,
                            onNavigateToProfile = { navigator.navigateTo(Screen.Profile) },
                            onNavigateToAdherence = { navigator.navigateTo(Screen.AdherenceGraph) },
                            onNavigateToMedicineList = { navigator.navigateTo(Screen.MedicineList) },
                            bottomBar = { BottomNavigationBar(navigator) }
                        )
                    } else {
                        sessionManager.clearSession()
                        navigator.navigateTo(Screen.Login, clearBackStack = true)
                    }
                }
                is Screen.Profile -> {
                    ProfileScreen(onBack = { navigator.handleBackIntent() })
                }
                is Screen.AdherenceGraph -> {
                    AdherenceGraphScreen(onBack = { navigator.handleBackIntent() })
                }
                is Screen.AfterCare -> {
                    val patient = sessionManager.getPatient()
                    if (patient != null) {
                        AfterCareScreen(
                            patient = patient,
                            onBack = { navigator.handleBackIntent() },
                            showSnackbar = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                            contentPadding = paddingValues
                        )
                    } else {
                        sessionManager.clearSession()
                        navigator.navigateTo(Screen.Login, clearBackStack = true)
                    }
                }
                is Screen.Vision -> {
                    VisionScreen()
                }
                is Screen.Notifications -> {
                    val patient = sessionManager.getPatient()
                    if (patient != null) {
                        NotificationsScreen(
                            patient = patient,
                            contentPadding = paddingValues,
                            onNoteClick = { note ->
                                navigator.navigateTo(Screen.FeedbackDetail(note))
                            }
                        )
                    } else {
                        sessionManager.clearSession()
                        navigator.navigateTo(Screen.Login, clearBackStack = true)
                    }
                }
                is Screen.MedicineList -> {
                    Text("Medicine List Screen", modifier = Modifier.padding(paddingValues))
                }
                is Screen.FeedbackDetail -> {
                    // Cast the screen state to get the note
                    val screen = navigator.currentScreen as Screen.FeedbackDetail
                    // Extract submission ID from the note
                    val subId = screen.note.submissionId ?: ""

                    if (subId.isNotEmpty()) {
                        ChatScreen(
                            submissionId = subId,
                            onBack = { navigator.handleBackIntent() }
                        )
                    } else {
                        // Fallback for old notes without submission links
                        FeedbackDetailScreen(
                            note = screen.note,
                            onBack = { navigator.handleBackIntent() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navigator: Navigator) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = navigator.currentScreen == Screen.Dashboard,
            onClick = { navigator.navigateTo(Screen.Dashboard) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "AfterCare") },
            label = { Text("AfterCare") },
            selected = navigator.currentScreen == Screen.AfterCare,
            onClick = { navigator.navigateTo(Screen.AfterCare) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.RemoveRedEye, contentDescription = "Vision") },
            label = { Text("Vision") },
            selected = navigator.currentScreen == Screen.Vision,
            onClick = { navigator.navigateTo(Screen.Vision) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
            label = { Text("Notifications") },
            selected = navigator.currentScreen == Screen.Notifications,
            onClick = { navigator.navigateTo(Screen.Notifications) }
        )
    }
}
