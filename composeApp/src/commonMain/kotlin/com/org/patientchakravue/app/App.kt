package com.org.patientchakravue.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.ui.*
import kotlinx.coroutines.launch
import com.org.patientchakravue.ui.language.AppLocalizationProvider
import com.org.patientchakravue.ui.theme.AppTheme
import com.org.patientchakravue.ui.theme.PatientBottomNavBar

@Composable
fun App(initialCallData: Pair<String, String>? = null) {
    AppTheme {
        AppLocalizationProvider {
            val sessionManager = remember { SessionManager() }
            val initialScreen = when {
                initialCallData != null -> Screen.VideoCall(
                    initialCallData.first,
                    initialCallData.second
                )

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

            // include VideoCall in bottomNavScreens so nav remains visible during a call
            // Use a predicate instead of a list because Screen.VideoCall is a data class (requires parameters)
            val isBottomNavScreen: (Screen) -> Boolean = { screen ->
                when (screen) {
                    is Screen.Dashboard,
                    is Screen.AfterCare,
                    is Screen.Vision,
                    is Screen.Notifications,
                    is Screen.VideoCall,
                    is Screen.VideoCallRequest -> true

                    else -> false
                }
            }

            Scaffold(
                modifier = Modifier.statusBarsPadding(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (isBottomNavScreen(navigator.currentScreen)) {
                        PatientBottomNavBar(navigator)
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
                                onNavigateToMedicineList = { navigator.navigateForward(Screen.MedicineList) }
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
                                showSnackbar = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            msg
                                        )
                                    }
                                },
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
                                showSnackbar = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            msg
                                        )
                                    }
                                }
                            )
                        }
                    }

                    is Screen.TumblingE -> {
                        val patient = sessionManager.getPatient()
                        if (patient != null) {
                            TumblingETestScreen(
                                patient = patient,
                                onBack = { navigator.goBack() },
                                showSnackbar = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            msg
                                        )
                                    }
                                }
                            )
                        }
                    }

                    is Screen.Notifications -> {
                        val patient = sessionManager.getPatient()
                        if (patient != null) {
                            NotificationsScreen(
                                patient = patient,
                                onNavigateToChat = { doctorId, doctorName, submissionIds ->
                                    navigator.navigateForward(
                                        Screen.Chat(
                                            doctorId,
                                            doctorName,
                                            submissionIds
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is Screen.Chat -> ChatScreen(
                        doctorName = screen.doctorName,
                        submissionIds = screen.submissionIds,
                        onBack = { navigator.goBack() }
                    )

                    is Screen.VideoCallRequest -> VideoCallRequestScreen(
                        { navigator.navigateAsPillar(Screen.Dashboard) },
                        { navigator.navigateAsPillar(Screen.Dashboard) },
                        paddingValues
                    )

                    is Screen.VideoCall -> VideoCallScreen(
                        screen.channelName,
                        screen.doctorId,
                        { navigator.navigateAsPillar(Screen.Dashboard) }
                    )

                    is Screen.FeedbackDetail -> FeedbackDetailScreen(
                        note = screen.note,
                        onBack = { navigator.navigateAsPillar(Screen.Dashboard) })

                    is Screen.MedicineList -> Text(
                        "Medicine List Screen",
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

