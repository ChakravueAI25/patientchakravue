package com.org.patientchakravue.platform

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

/**
 * iOS implementation: No-op for now.
 * iOS uses APNS which would be configured separately.
 */
actual fun registerFcmTokenAfterLogin(patientId: String) {
    // iOS push notifications would be handled via APNS
    // This is a placeholder for future iOS push notification implementation
    println("iOS FCM registration placeholder for patient: $patientId")
}

/**
 * iOS implementation: Save PDF and notify user.
 * This is a placeholder - iOS file handling would need native implementation.
 */
actual fun saveAndNotifyDownload(fileName: String, data: ByteArray) {
    // iOS file saving would require native implementation using NSFileManager
    // and local notifications using UNUserNotificationCenter
    println("iOS saveAndNotifyDownload placeholder for file: $fileName")
}

