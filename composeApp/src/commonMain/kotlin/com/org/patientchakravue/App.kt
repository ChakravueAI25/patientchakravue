package com.org.patientchakravue

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
}

class Navigator(initialScreen: Screen) {
    var currentScreen by mutableStateOf(initialScreen)
        private set

    private val backStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen, clearBackStack: Boolean = false) {
        if (clearBackStack) {
            backStack.clear()
        } else {
            backStack.add(currentScreen)
        }
        currentScreen = screen
    }

    fun canGoBack(): Boolean = backStack.isNotEmpty()

    fun goBack() {
        if (canGoBack()) {
            currentScreen = backStack.removeLast()
        }
    }
}

@Composable
fun App() {
    MaterialTheme {
        val sessionManager = remember { SessionManager() }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val initialScreen = if (sessionManager.getPatient() != null) Screen.Dashboard else Screen.Login
        val navigator = remember { Navigator(initialScreen) }

        // This is where platform-specific back handling would be connected to navigator.goBack()
        // For now, we will just manage the screen state.

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
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
                            onLogout = {
                                sessionManager.clearSession()
                                navigator.navigateTo(Screen.Login, clearBackStack = true)
                            },
                            showSnackbar = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    } else {
                        // This case should ideally not happen if logic is correct.
                        // If it does, we force a logout.
                        sessionManager.clearSession()
                        navigator.navigateTo(Screen.Login, clearBackStack = true)
                    }
                }
            }
        }
    }
}
