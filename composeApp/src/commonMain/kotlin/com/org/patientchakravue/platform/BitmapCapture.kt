package com.org.patientchakravue.platform

import androidx.compose.ui.graphics.Path

/**
 * Platform-specific bitmap capture for Amsler Grid drawings
 */
expect fun captureAmslerBitmap(paths: List<Path>, sizePx: Int): ByteArray

