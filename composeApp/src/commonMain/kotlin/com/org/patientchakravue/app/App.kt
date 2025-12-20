package com.org.patientchakravue.app

import androidx.activity.compose.BackHandler
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

        // Global back handler to navigate to Dashboard from any other screen
        BackHandler(enabled = navigator.currentScreen != Screen.Dashboard) {
            navigator.navigateTo(Screen.Dashboard, true)
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
                    ProfileScreen(onBack = { navigator.navigateTo(Screen.Dashboard, true) })
                }
                is Screen.AdherenceGraph -> {
                    AdherenceGraphScreen(onBack = { navigator.navigateTo(Screen.Dashboard, true) })
                }
                is Screen.AfterCare -> {
                    val patient = sessionManager.getPatient()
                    if (patient != null) {
                        AfterCareScreen(
                            patient = patient,
                            onBack = { navigator.navigateTo(Screen.Dashboard, true) },
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
                    Text("Notifications Screen", modifier = Modifier.padding(paddingValues))
                }
                is Screen.MedicineList -> {
                    Text("Medicine List Screen", modifier = Modifier.padding(paddingValues))
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