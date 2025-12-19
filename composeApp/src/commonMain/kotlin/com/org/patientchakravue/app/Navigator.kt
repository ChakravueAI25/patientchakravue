package com.org.patientchakravue.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

