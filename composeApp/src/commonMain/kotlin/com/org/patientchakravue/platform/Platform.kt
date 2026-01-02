package com.org.patientchakravue.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Platform-specific function to register FCM token after login.
 * On Android: Fetches FCM token and sends to backend.
 * On iOS: No-op (iOS uses APNS handled separately).
 */
expect fun registerFcmTokenAfterLogin(patientId: String)
