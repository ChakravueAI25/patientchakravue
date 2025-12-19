package com.org.patientchakravue.app

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
}
