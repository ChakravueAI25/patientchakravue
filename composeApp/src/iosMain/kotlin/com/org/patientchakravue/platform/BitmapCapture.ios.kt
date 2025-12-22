package com.org.patientchakravue.platform

import androidx.compose.ui.graphics.Path

/**
 * iOS implementation of bitmap capture for Amsler Grid drawings
 * Note: This is a stub - full iOS implementation would use UIKit/CoreGraphics
 */
actual fun captureAmslerBitmap(paths: List<Path>, sizePx: Int): ByteArray {
    // TODO: Implement iOS-specific bitmap capture using UIKit/CoreGraphics
    // For now, return empty bytes - iOS implementation pending
    return ByteArray(0)
}

