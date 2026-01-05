package com.org.patientchakravue.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Navigator(initialScreen: Screen) {

    var currentScreen by mutableStateOf(initialScreen)
        private set

    private val backStack = mutableListOf<Screen>()

    /**
     * Used for inner navigation inside a pillar
     * Example: Vision -> AmslerGrid
     */
    fun navigateForward(screen: Screen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    /**
     * Used for bottom navigation clicks
     * Always resets to Dashboard or a pillar
     */
    fun navigateAsPillar(screen: Screen) {
        backStack.clear()
        currentScreen = screen
    }

    fun canGoBack(): Boolean = backStack.isNotEmpty()

    fun goBack() {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.lastIndex)
        } else {
            // fallback safety
            currentScreen = Screen.Dashboard
        }
    }
}
