package com.org.patientchakravue.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.org.patientchakravue.app.Navigator
import com.org.patientchakravue.app.Screen

data class PatientBottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun PatientBottomNavBar(navigator: Navigator) {
    val bottomNavItems = listOf(
        PatientBottomNavItem(Screen.Dashboard, "Home", Icons.Default.Dashboard),
        PatientBottomNavItem(Screen.AfterCare, "Care", Icons.AutoMirrored.Filled.List),
        PatientBottomNavItem(Screen.Vision, "Vision", Icons.Default.RemoveRedEye),
        PatientBottomNavItem(Screen.Notifications, "Alerts", Icons.Default.Notifications),
    )

    Box {
        // Keep the navigation surface as a simple white surface
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(90.dp)
            ) {
                // Dashboard - Left
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
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

                // Empty spacer for center FAB
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

        // FAB for Video Call Request
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp),
            contentAlignment = Alignment.Center
        ) {
            var pressed by remember { mutableStateOf(false) }

            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = tween(durationMillis = 120),
                label = "fab-scale"
            )

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
                                navigator.navigateAsPillar(Screen.VideoCallRequest)
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
