package com.org.patientchakravue.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.org.patientchakravue.app.Navigator
import com.org.patientchakravue.app.Screen


@Composable
fun PatientBottomNavBar(navigator: Navigator) {
    val activeColor = Color(0xFF4CAF50)
    val inactiveColor = Color(0xFF000000)
    val navBarColor = Color(0xFFBFE6D3)
    val bubbleColor = Color.White

    Box {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = navBarColor,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            NavigationBar(
                modifier = Modifier
                    .height(88.dp)
                    .padding(horizontal = 12.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {

                // Dashboard - Home
                NavigationBarItem(
                    selected = navigator.currentScreen == Screen.Dashboard,
                    onClick = { navigator.navigateAsPillar(Screen.Dashboard) },
                    icon = {
                        CustomNavIcon(
                            icon = Icons.Default.Home,
                            label = "Home",
                            isSelected = navigator.currentScreen == Screen.Dashboard,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            bubbleColor = bubbleColor
                        )
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = Color.Transparent
                    ),
                    alwaysShowLabel = false,
                    interactionSource = remember { MutableInteractionSource() }
                )

                // AfterCare - Care
                NavigationBarItem(
                    selected = navigator.currentScreen == Screen.AfterCare,
                    onClick = { navigator.navigateAsPillar(Screen.AfterCare) },
                    icon = {
                        CustomNavIcon(
                            icon = Icons.AutoMirrored.Filled.List,
                            label = "Care",
                            isSelected = navigator.currentScreen == Screen.AfterCare,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            bubbleColor = bubbleColor
                        )
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = Color.Transparent
                    ),
                    alwaysShowLabel = false,
                    interactionSource = remember { MutableInteractionSource() }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Vision
                NavigationBarItem(
                    selected = navigator.currentScreen == Screen.Vision,
                    onClick = { navigator.navigateAsPillar(Screen.Vision) },
                    icon = {
                        CustomNavIcon(
                            icon = Icons.Default.RemoveRedEye,
                            label = "Vision",
                            isSelected = navigator.currentScreen == Screen.Vision,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            bubbleColor = bubbleColor
                        )
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = Color.Transparent
                    ),
                    alwaysShowLabel = false,
                    interactionSource = remember { MutableInteractionSource() }
                )

                // Notifications - Alerts
                NavigationBarItem(
                    selected = navigator.currentScreen == Screen.Notifications,
                    onClick = { navigator.navigateAsPillar(Screen.Notifications) },
                    icon = {
                        CustomNavIcon(
                            icon = Icons.Default.Notifications,
                            label = "Alerts",
                            isSelected = navigator.currentScreen == Screen.Notifications,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            bubbleColor = bubbleColor
                        )
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = Color.Transparent
                    ),
                    alwaysShowLabel = false,
                    interactionSource = remember { MutableInteractionSource() }
                )
            }
        }

        // Center FAB for Video Call Request
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp),
            contentAlignment = Alignment.Center
        ) {
            var pressed by remember { mutableStateOf(false) }

            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = tween(120),
                label = "fab-scale"
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(activeColor, RoundedCornerShape(32.dp))
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
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun CustomNavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    bubbleColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(300),
        label = "icon-scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp)
        ) {
            // White bubble behind icon when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(bubbleColor, RoundedCornerShape(10.dp))
                )
            }

            // Icon with scale animation
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = if (isSelected) activeColor else inactiveColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Show label only when selected
        if (isSelected) {
            Text(
                label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = activeColor
            )
        }
    }
}


