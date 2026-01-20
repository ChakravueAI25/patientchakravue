package com.org.patientchakravue.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.ui.*
import kotlinx.coroutines.launch

// Added imports for animation, graphicsLayer and pointer input
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import com.org.patientchakravue.ui.language.AppLocalizationProvider
import com.org.patientchakravue.ui.theme.AppTheme

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
                        { navigator.goBack() },
                        { navigator.goBack() },
                        paddingValues
                    )

                    is Screen.VideoCall -> VideoCallScreen(
                        screen.channelName,
                        screen.doctorId,
                        { navigator.goBack() }
                    )

                    is Screen.FeedbackDetail -> FeedbackDetailScreen(
                        note = screen.note,
                        onBack = { navigator.goBack() })

                    is Screen.MedicineList -> Text(
                        "Medicine List Screen",
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navigator: Navigator) {
    Box {
        // Keep the navigation surface as a simple white surface (no clear cutout)
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                 // Dashboard - Left
                 NavigationBarItem(
                     icon = { Icon(Icons.Default.Dashboard, null) },
                     label = { Text("Home") },
                     selected = navigator.currentScreen == Screen.Dashboard,
                     onClick = { navigator.navigateAsPillar(Screen.Dashboard) },
                     colors = NavigationBarItemDefaults.colors(
                         selectedIconColor = Color(0xFF4CAF50),
                         unselectedIconColor = Color.Black,
                         indicatorColor = Color.Transparent
                     )
                 )

                 // AfterCare - Left-Center
                 NavigationBarItem(
                     icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                     label = { Text("Care") },
                     selected = navigator.currentScreen == Screen.AfterCare,
                     onClick = { navigator.navigateAsPillar(Screen.AfterCare) },
                     colors = NavigationBarItemDefaults.colors(
                         selectedIconColor = Color(0xFF4CAF50),
                         unselectedIconColor = Color.Black,
                         indicatorColor = Color.Transparent
                     )
                 )

                 // Empty spacer for center FAB - use Spacer weight instead of disabled item
                 Spacer(modifier = Modifier.weight(1f))

                 // Vision - Right-Center
                 NavigationBarItem(
                     icon = { Icon(Icons.Default.RemoveRedEye, null) },
                     label = { Text("Vision") },
                     selected = navigator.currentScreen == Screen.Vision,
                     onClick = { navigator.navigateAsPillar(Screen.Vision) },
                     colors = NavigationBarItemDefaults.colors(
                         selectedIconColor = Color(0xFF4CAF50),
                         unselectedIconColor = Color.Black,
                         indicatorColor = Color.Transparent
                     )
                 )

                 // Notifications - Right
                 NavigationBarItem(
                     icon = { Icon(Icons.Default.Notifications, null) },
                     label = { Text("Alerts") },
                     selected = navigator.currentScreen == Screen.Notifications,
                     onClick = { navigator.navigateAsPillar(Screen.Notifications) },
                     colors = NavigationBarItemDefaults.colors(
                         selectedIconColor = Color(0xFF4CAF50),
                         unselectedIconColor = Color.Black,
                         indicatorColor = Color.Transparent
                     )
                 )

            }
        }

        // Place the FAB so it sits on the nav bar (no border/shadow)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp),
            contentAlignment = Alignment.Center
        ) {

            // Press state
            var pressed by remember { mutableStateOf(false) }

            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = tween(durationMillis = 120),
                label = "fab-scale"
            )

            // Green action button (no shadow, no border)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                                navigator.navigateForward(Screen.VideoCallRequest)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
 }
