package com.org.patientchakravue.app

import androidx.compose.runtime.Composable

// iOS actual for AppBackHandler: iOS doesn't have a standard back button, so provide a no-op.
@Composable
actual fun AppBackHandler(onBack: () -> Unit) {
    // No-op on iOS; keep signature to satisfy expect/actual linkage.
}

