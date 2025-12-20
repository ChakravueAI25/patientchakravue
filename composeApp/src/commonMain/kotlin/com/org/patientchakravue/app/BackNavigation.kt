package com.org.patientchakravue.app

fun Navigator.handleBackIntent() {
    navigateTo(Screen.Dashboard, clearBackStack = true)
}
