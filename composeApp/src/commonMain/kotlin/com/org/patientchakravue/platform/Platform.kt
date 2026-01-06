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

/**
 * Saves a byte array as a PDF file and shows a local notification on completion.
 */
expect fun saveAndNotifyDownload(fileName: String, data: ByteArray)

