package com.org.patientchakravue.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
}
